# -*- coding: utf-8 -*-
"""
Created on Fri Nov 16 14:46:26 2018

@author: smcardle
"""
#import matplotlib.pyplot as plt
import numpy as np
from scipy import ndimage
#import argparse, pickle
import pickle

#open pickle file from IronPython tile-by-tile processing
tempsavefile=open(r'D:\Users\zeiss\Documents\FlatteningMacro\focusvars2.pkl','rb')
out = pickle.load(tempsavefile)
tempsavefile.close()


"""

parser = argparse.ArgumentParser(description="test argparse thingy", prefix_chars='@')
parser.add_argument('input1',nargs='+')
args = parser.parse_args()
combined="".join(x for x in args.input1)
out=eval(combined)"""



#Parse input
focusblockTiles=np.array(out[0])
Xlist=out[1]
Ylist=out[2]
Zpos=np.array(out[3])
varsblockTiles=np.array(out[4])

#generate blank map of appropriate size
Xoptions=np.array(sorted(set(Xlist)))
Yoptions=np.array(sorted(set(Ylist)))
focusmap=np.zeros((len(Yoptions)*4,len(Xoptions)*4))
focusmap[focusmap==0]=np.nan
tilt=np.zeros((4,4)) #average relative focus of each of the 16 blocks

#put in best focus positions into map
for tilenum in range(focusblockTiles.shape[0]):
    tilex=np.where(Xoptions==Xlist[tilenum])
    tiley=np.where(Yoptions==Ylist[tilenum])
    tilefocus=focusblockTiles[tilenum,:,:]
    focusmap[int(tiley[0]*4):int((tiley[0]+1)*4),int(tilex[0]*4):int((tilex[0]+1)*4)]=tilefocus
    corrected=[b-tilefocus[0,0] for b in tilefocus] #normalize tilefocus to top left corner
    tilt+=corrected #sum focal position of each tile
    

tilt=tilt/focusblockTiles.shape[0] #calculate average tilt over the entire image
#gives an approximate measurement of slide tilt
#correcting this makes the averaging more accurate
tiltmat=np.tile(tilt,(Yoptions.size,Xoptions.size)) #repeat tilt for shape of entire image
focusmapTilt=focusmap-tiltmat #remove effect of tilt (causes sharp edges in focus map)

#3x3 median filtering to remove incorrect focus choices due to noise,dirt, etc
#assumes correct position of 1 tile cannot be very far from the position of its neighbors
#generic filter chosen to properly handle NAN values (where properly = ignore them and just use real values. This also fixes the edge cases to be correct)
filteredFocus=ndimage.generic_filter(focusmapTilt,np.nanmedian,size=3,mode='constant',cval=np.NaN)

#above filter puts data where there is no data. Remove any location with fake data. 
filteredFocus[np.isnan(focusmap)]=np.nan #shouldn't actually matter, is here for completeness
filteredTilt=filteredFocus+tiltmat #put the removed tilt back


Xidx=[]
Yidx=[]

#converts between focal planes in microns and actual Z steps
#converts between numpy map and basic python-readable list of lists 
bestTiles=[]
for tilenum in range(focusblockTiles.shape[0]):
    tilex=np.where(Xoptions==Xlist[tilenum])
    tiley=np.where(Yoptions==Ylist[tilenum])
    smoothtile=filteredTilt[int(tiley[0]*4):int((tiley[0]+1)*4),int(tilex[0]*4):int((tilex[0]+1)*4)]
    beststep=np.zeros((4,4))
    for xstep in range(4):
        for ystep in range(4):
            #find best tiles
            if sum(smoothtile[xstep,ystep]==Zpos[tilenum])>0:
                #if best slice is an exact slice
                beststep[xstep,ystep]=np.where(smoothtile[xstep,ystep]==Zpos[tilenum])[0]
            else: #otherwise find best focus in 2 nearest steps
                splittiles=np.absolute(smoothtile[xstep,ystep]-Zpos[tilenum]).argsort()[:2]
                beststep[xstep,ystep]=splittiles[np.argmax(varsblockTiles[tilenum,xstep,ystep,splittiles])]
    bestTiles.append(beststep.tolist())
    Xidx.append(np.asscalar(tilex[0]))
    Yidx.append(np.asscalar(tiley[0]))

#resets origin of the X,Y indicies to thaet of the first tile
Xidx[:]=[(i - Xidx[0]) for i in Xidx]
Yidx[:]=[(i - Yidx[0]) for i in Yidx]

output=(bestTiles,Xidx,Yidx)


print(output) #returns values to System.Diagnostics.Process in Zen macro