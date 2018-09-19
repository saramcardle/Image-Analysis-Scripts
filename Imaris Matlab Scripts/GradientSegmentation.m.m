%% Segmentation algorithm for microscopy data. 
% Segments cells based on 2 channels simulataneously, based on the
% gradients in both channels. Good for images where there are adjacent
% double-positive cells that are difficult to separate with a single-color
% intensity thresholds. Does not assume that cells are round, so is
% especially well adapted for oddly shaped cells, such as macrophages.

%Largely based on:
%"Unsupervised color image segmentation using a dynamic color gradient thresholding algorithm", Balasubramanian, et al., Human Vision and Electronic Imaging, 2008
%Algorithm was adapted to fluorescent imaging data
%Major changes:
%1) Performs 3D segmentation
%2) Uses 2 channels only (written for GFP/YFP, though any colors will work)
%3) Assumes you have set all background pixels to 0. Fluorescent images
%typically have a high (>50%) number of background pixels and it is
%inefficient to process them. The script will still work if you have not
%done this, but it will be slower. 
%4) Additional region merging based on some reasonable assumptions about
%cells. No final region is allowed to be smaller than a user-defined size, 
%and no cell can be "landlocked" (fully contained within other cells). 
%5) Reads data from an .ims file, and outputs calculated cell surfaces to Imaris for
%further processing/tracking
%6) Multiple parameters are tweakable

%Dependencies:
%1) Imaris Reader- Library by Pete Beemiller than can be found on the Imaris XTensions page
%2) Imaris 7 (newer versions do not work with Imaris Reader)
%3) Matlab Parallel Processing Toolbox
%4) adjacentRegionsGraph - function by Steve Eddins that can be found on the Matlab File Exchange

%This script is very slow, even with parallel computing. It takes ~12-24 hours
%to process a large data set (512x512x50x30 XYZT). It is highly recommended
%that you adjust parameters based on a small cropped region.
%This can be run as a straight script, but running it block by block and
%then using imshow() to look at the output is helpful for tuning the
%parameters. 

%Written by Sara McArdle of the La Jolla Institute Microscopy Core, 2018.


%% Declare Variables, Get connected to imaris
maxT=0;    %maximum number of timepoints you wish to segment. Set to 0 to use entire dataset. 
fileObj=ImarisReader('D:\Sara\Konrad Explant Tracking\080915 No15814_1_Series007_t1_crop.ims'); %insert path to file
channels = [4 5]; %Channels to use in the Imaris file. 0-based.
BorderThresh=.05; %Smallest gradient that represents a border that should be kept. Paper suggest .25, empirically smaller is better. 
minSeedSize=5; %minimum number of pixels in a seed point; discard regions smaller than this
flatThresh=.02; %multiplication factor to decide a region is smooth enough to be a seed. 0.1 suggested in paper, but empirically smaller seems to work better. Smaller = slower. 
minNewRegion=5; %Minimum size for a region detected in the region growing step to be labeled a new seed
minOverlap =3; %Minumum number of neighboring pixels to define a "neighbor"
simcolor = 1.1; %Threshold to determine if new region should be merged to a seed. Ratio of channel intensities in seed vs new area.
RMThresh = .05:.1:.55; %Region Merging Threshold steps. Smaller steps = slower, more accurate. Results are *highly sensitive* to these values. Lower max value = more segmentation. Higher max value = more merging. 
minCellSize =  200; %Minimum number of voxels a valid cell can be. Depends on your data. 


DataSet=fileObj.DataSet;
if maxT==0
    maxT=DataSet.SizeT;
end

imData=zeros(DataSet.SizeX,DataSet.SizeY,DataSet.SizeZ,2,maxT,'uint8');
javaaddpath ImarisLib.jar;  %C:\Program Files\Bitplane\Imaris x64 7.7.2\XT\rtmatlab\ImarisLib.jar
vImarisLib = ImarisLib;
aImarisApplication = vImarisLib.GetApplication(0);
aFactory = aImarisApplication.GetFactory;
%% Get Data


for t=1:maxT   
    for c=1:2 
        imData(:,:,:,c,t)=DataSet.GetDataVolume(channels(c),t-1);
    end
end

GFP=cell(1,maxT);
YFP=cell(1,maxT);


for t=1:maxT
    GFP{t}=imData(:,:,:,2,t); %loop for time eventually
    YFP{t}=imData(:,:,:,1,t);
end
disp('data retrieved')

sizeX=size(GFP{1},1);
sizeY=size(GFP{1},2);

%% Calculate Gradients
%Unsupervised color image segmentation using a dynamic color gradient thresholding algorithm
%Guru Prashanth Balasubramanian, et al., Human Vision and Electronic Imaging XIII


maxZ=DataSet.SizeZ;
Gx=cell(maxZ,maxT); 
Gy=cell(maxZ,maxT);
Yx=cell(maxZ,maxT);
Yy=cell(maxZ,maxT);
GradMat=cell(maxZ,maxT);
thresh1=zeros(maxZ,maxT);
background=cell(maxZ,maxT);
erodeback = cell(maxZ,maxT);
seback=[1 1 1; 1 1 1; 1 1 1];


for t=1:maxT
    for z=1:maxZ
        background{z,t}=((GFP{t}(:,:,z)+YFP{t}(:,:,z))==0);
        erodeback{z,t}=imerode(background{z,t},seback);
        [Gx{z,t}, Gy{z,t}]=gradient(medfilt2(double(GFP{t}(:,:,z)),[3,3]));
        [Yx{z,t}, Yy{z,t}]=gradient(medfilt2(double(YFP{t}(:,:,z)),[3,3]));
    end
end

tic
parfor t=1:maxT
    for z=1:maxZ
        for i=1:sizeX
            for j=1:sizeY
                if erodeback{z,t}(i,j)==1
                    GradMat{z,t}(i,j)=0;
                else
                    D = [Yx{z,t}(i,j), Yy{z,t}(i,j); Gx{z,t}(i,j) Gy{z,t}(i,j)];
                    Prod = transpose(D)*D;
                    GradMat{z,t}(i,j)=sqrt(max(max(eig(Prod))));
                end
            end
        end
        
        thresh1(z,t)=graythresh(GradMat{z,t});
    end
end
toc
disp('Calculating Gradients')

%% Vector Based Color Gradient
EnGrad=cell(maxZ,maxT);
TgMask = cell(maxZ,maxT);

parfor t=1:maxT
    for z=1:maxZ
        EnGrad{z,t}=zeros(size(GradMat{z,t}));
        MaxVal=max(max(GradMat{z,t}));
        for i=1:size(GradMat{z,t},1)
            for j=1:size(GradMat{z,t},2)
                if (GradMat{z,t}(i,j)>= .05*MaxVal) &&(GradMat{z,t}(i,j) < .95 * MaxVal)
                    EnGrad{z,t}(i,j) = (GradMat{z,t}(i,j) - (0.05*MaxVal))/(.9 * MaxVal);
                elseif GradMat{z,t}(i,j) >= .95 * MaxVal
                    EnGrad{z,t}(i,j)=1;
                end
            end
        end
        TgMask{z,t}=EnGrad{z,t}>(BorderThresh * thresh1(z,t));
        
    end
end

%% Line Field Map
Th= .1;
Tl = .01;
LineField = cell(maxZ,maxT);
WeightGrad = cell(maxZ,maxT);
WGNoBack = cell(maxZ,maxT);
thresh2=zeros(maxZ,maxT);

parfor t=1:maxT
    for z=1:maxZ
        LineField{z,t}=zeros(size(GradMat{z,t}));
        for i=1:size(GradMat{z,t},1)
            for j=1:size(GradMat{z,t},2)
                if EnGrad{z,t}(i,j) > Th
                    LineField{z,t}(i,j) = 1;
                elseif EnGrad{z,t}(i,j) > Tl
                    if (i>2) && (j>2) && (i<size(GradMat{z,t},1)-1)  && (j<size(GradMat{z,t},2)-1)
                        if  EnGrad{z,t}(i+1,j) > Th || EnGrad{z,t}(i-1,j) > Th || ...
                                EnGrad{z,t}(i,j+1) > Th || EnGrad{z,t}(i,j-1) > Th ||...
                                EnGrad{z,t}(i+2,j) > Th || EnGrad{z,t}(i-2,j) > Th || ...
                                EnGrad{z,t}(i,j+2) > Th || EnGrad{z,t}(i,j-2) > Th
                            LineField{z,t}(i,j) = 1;
                        end
                    elseif (i>1) && (j>1) && (i<size(GradMat{z,t},1))  && (j<size(GradMat{z,t},2))
                        if  EnGrad{z,t}(i+1,j) > Th || EnGrad{z,t}(i-1,j) > Th || ...
                                EnGrad{z,t}(i,j+1) > Th || EnGrad{z,t}(i,j-1) > Th
                            LineField{z,t}(i,j) = 1;
                        end
                    elseif i==1 || j==1
                        if  EnGrad{z,t}(i+1,j) > Th || EnGrad{z,t}(i,j+1) > Th
                            LineField{z,t}(i,j) = 1;
                        end
                    elseif i== size(GradMat{z,t},1) || j == size(GradMat{z,t},2)
                        if  EnGrad{z,t}(i-1,j) > Th || EnGrad{z,t}(i,j-1) > Th
                            LineField{z,t}(i,j) = 1;
                        end
                    end
                end
            end
        end
        combo=(logical(LineField{z,t}) & TgMask{z,t});
        WeightGrad{z,t}=EnGrad{z,t}.*combo;
        WGNoBack{z,t}=WeightGrad{z,t};
        WGNoBack{z,t}(background{z,t})=2;
        
        thresh2(z,t)=graythresh(WeightGrad{z,t}); 
    end
end
disp('Gradient Map Finished')
imshow(WeightGrad{30,1},[])
%% Seed Generation
CC=cell(maxZ,maxT);
%minSeedSize=6;
seeds=cell(maxZ,maxT);
LM=cell(maxZ,maxT,2);
%se=[0 1 0; 1 1 1; 0 1 0];

parfor t=1:maxT
    for z=1:maxZ
        flat=WGNoBack{z,t}<flatThresh*thresh2(z,t);  
        %flat2=imopen(flat,se);
        CC{z,t}=bwconncomp(flat,4);
        stat=regionprops(CC{z,t},'Area');
        
        seedmin=[stat.Area]>minSeedSize;
        %    seedmax = [stat.Area]<max([stat.Area]);
        seeds{z,t}=seedmin; %& seedmax;
        %      CCPix{z,t}=CC{z,t}.PixelIdxList(seeds{z,t});
        
        LM{z,t,1}=labelmatrix(CC{z,t});
        black=cat(1,CC{z,t}.PixelIdxList{~seeds{z,t}});
        LM{z,t,1}(black)=0;
        k=1;
        for j=1:max(max(LM{z,t,1}))
            if ~isempty(find(LM{z,t,1}==j,1))
                if j~=k
                  LM{z,t,1}((LM{z,t,1}==j))=k;
                end
                k=k+1;
            end
        end
    end
end
disp('seeds found')



%% Region Growing
thresh3=zeros(maxZ,maxT,10);
%minNewRegion=8;
%minnum=4; %Delete
%simcolor = 1.2;
%OtsuRG=0; %Delete
edge = 0;
dilstruct= ones(3,3); %[0 1 0; 1 1 1; 0 1 0];
LMlarge = uint16(zeros(size(LM{1,1,1})));
LMnoover = uint16(zeros(size(LM{1,1,1})));
fig=false(size(LM{1,1,1}));

xyzsize=size(LM{1,1,1});

parfor t=1:maxT
    thresh4=zeros(maxZ,10); %start out assuming 10 updates, may change in the loops
    
    LMpar=LM(:,t,1); %Pull out 1 timepoint for parallelization
    for z=1:maxZ %Each Z step independently for now
        i=1;
        change = 1;
        updatenum=0;
        edge = 0;
        while edge <2 
            while change == 1 %until stopping point is reached
                updatenum = updatenum+1;
                if edge == 0 
                    uncat=(LMpar{z,i}==0)&(~background{z,t}); %Which pixels do not have a label but are not background
                    %Z is z step, i is related to update number. The entire
                    %i mechanism can likely be cleaned up. Use 2 variables,
                    %LMcurrent, LMold, and don't keep every update. It was
                    %useful for debugging. 
                    thresh4(z,updatenum)=graythresh(WeightGrad{z,t}(uncat)); %calculate threshold on jush those pixels in WeightGrad image
                    step=.5:.5:1; %step thresholds
                elseif edge == 1
                    thresh4(z, updatenum) = 1.01;%MAX VALUE
                    step=1;
                else
                    disp('????') %should never happen
                    break
                end
                change = 0; %as of this moment, no change has occured
                
                % Calculate New Regions
                for s=step
                    
                    U = s*thresh4(z,updatenum); %calculate current threshold
                    i=i+1; %counter
                    
                    LMpar{z,i}=LMpar{z,i-1}; %start Label Matrix as previous labels
                    
                    LargeNew = bwconncomp(WGNoBack{z,t}<U,4); %find connect regions with WeightGrad smaller than current threshold
                    LMlarge = uint16(labelmatrix(LargeNew));
                    LMnoover = LMlarge;
                    LMnoover(LMpar{z,i}>0)=0; %if those pixels already had a label, igonore them
                    SmallNew = bwconncomp(LMnoover>0,4); %calculate label matrix of just the newly found pixels
                    %last 6 lines can be consensed and made more efficient
                    %WGNoBack{z,t}<U & LMpar{z,i}==0
                    stat = regionprops(SmallNew,'Area'); %calculate area of newly identified pixel regions
                    
                    %    new=bwconncomp(fig,4); %new foreground
                    %    stat=regionprops(new,'Area');
                    for j=1:SmallNew.NumObjects  %for each new cluster
                        overlapPix=zeros(1,SmallNew.NumObjects); 
                        largeID=unique(LMlarge(SmallNew.PixelIdxList{j})); %check to make sure each new small region came from 1 large region
                        if length(largeID)>1
                            disp('WRONG') %should never happen
                        end
                        
                        
                        fig = false(xyzsize);
                        fig(SmallNew.PixelIdxList{j})=true; %for the pixels in this new region
                        Dilated=imdilate(fig, dilstruct); %dilate by 1 in all direction
                        
                        if sum(sum(Dilated & (LMpar{z,i-1}>0)))<minOverlap  %if there's not enough overlap
                            %Growing Region
                            %check if it's actually background
                            if stat(j).Area>minNewRegion %if it's large enough
                                LMpar{z,i}(SmallNew.PixelIdxList{j})=max(max(LMpar{z,i}))+1; %make it a new region
                                change = 1;
                            end
                        else  %if there is overlap
                            neighborlist=unique(LMpar{z,i-1}(Dilated)); %Find which original regions this new region is overlapping with
                            neighborlist = neighborlist(neighborlist ~=0); %background does not count
                            
                            for k=[neighborlist'] %for each previous region
                                %    reg(k) = sum(ismembc(new.PixelIdxList{j},find(LM{z,t,i-1}==k)));
                                overlapPix(k) = sum(sum(Dilated & (LMpar{z,i-1}==k))); %determine how much overlap with each 
                            end
                            
                            %better way of determining neighbor
                            neighbor=find(overlapPix==max(overlapPix));
                            if length(neighbor)>1
                                neighbor=neighbor(1); %in case of tie, just pick the first for code clarity
                            end
                            %Remove overlapping pixels
                            %                growing = setdiff(new.PixelIdxList{j},find(LM{z,t,i-1}>0));
                            %               overlap = intersect(new.PixelIdxList{j},find(LM{z,t,i-1}==neighbor));
                            %                      if ~isempty(growing)
                            if stat(j).Area<=minNewRegion %if new region is small
                                LMpar{z,i}(SmallNew.PixelIdxList{j}) = neighbor; %merge it with the neighbor it has the most overlap with
                                change = 1; %a change occured!
                            else %if new region is large, determine amount of similarity to most overlapping neighbor
                                g=GFP{t}(:,:,z);
                                y=YFP{t}(:,:,z);
                                
                                
                                grat=median(median(g(SmallNew.PixelIdxList{j})))...
                                    /median(median(g(LMpar{z,i-1}==neighbor)));
                                yrat=median(median(y(SmallNew.PixelIdxList{j})))...
                                    /median(median(y(LMpar{z,i-1}==neighbor)));
                                if (grat < simcolor || grat > 1/simcolor) && ...
                                        (yrat < simcolor || yrat > 1/simcolor) %if similar enough
                                    LMpar{z,i}(SmallNew.PixelIdxList{j}) = neighbor; %merge regions
                                    change = 1;
                                    
                                else %if not similar to most overlapping neighbor, it becomes it's own region
                                    %check is it's actually background
                                    LMpar{z,i}(growing) = max(max(LMpar{z,i}))+1;
                                    change = 1;
                                    
                                end
                                
                            end
                            %                         end
                        end
                    end
                end
                if edge == 1
                    change =0; %only repeat edge while loop once,
                end
            end %repeat while loop until no changes occur 
            edge = edge + 1; %then deal with edges of WeightGrad to include them into cells
            change = 1; %reset change
        end
        edge = 0; %reset edge for next time point
    end
    LMcombined{t}=LMpar; 
end
%Change LMcombined
disp('regions grown')
LM=LMcombined;
%% Region Merging
% Convert LM to Regions
clear NewReg
Regions = cell(1,maxT);

%minSeedSize=5; %delete
% RMThresh = .1:.1:1; 
newthresh=0;


%Clean Up Label Numbers
parfor t=1:maxT
    Regions{t}=double(zeros(xyzsize(1),xyzsize(2), maxZ));
    LMtime=LMcombined{t};
    for z=1:maxZ
        for i=size(LMtime,2):-1:1 %not knowing how many updates were required to finish growing regions, work backwards until you find a non-blank LM
            if ~isempty(LMtime{z,i})
                if z==1
                    Regions{t}(:,:,z)=double(LMtime{z,i});
                else
                    im=double(LMtime{z,i})+max(max(max(Regions{t}))); %each z step get greater (unique) label numbers
                    im(LMtime{z,i}==0)=0;
                    Regions{t}(:,:,z)=im;
                end
                break
            end
        end
    end
end

NewReg = Regions;

parfor t=1:maxT
    S=0;
    num=max(max(max(NewReg{t})));
    Greg=cell(1,num);
    Yreg=cell(1,num);
    for R=RMThresh
disp(R)
disp(num)
        newthresh = 0;
        
        while newthresh == 0
            R
            [sorted, sidx] = sort(NewReg{t}(:));
            sortedno0 = sorted(sorted>0);
            sidx=sidx(sorted>0);
            
            for i=1:num
                pix=sidx(ismembc(sortedno0,i));
                Greg{i}=GFP{t}(pix);
                Yreg{i}=YFP{t}(pix);
            end
            
            
            
            g=adjacentRegionsGraph(NewReg{t},6);
            pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
            S=zeros(1,length(pairlist));
            
            for i=1:length(pairlist)
                mu=([mean(Greg{pairlist(i,1)});mean(Yreg{pairlist(i,1)})]...
                    -[mean(Greg{pairlist(i,2)});mean(Yreg{pairlist(i,2)})]);
                invcov=cov(double(Greg{pairlist(i,1)}),double(Yreg{pairlist(i,1)}))...
                    +cov(double(Greg{pairlist(i,2)}),double(Yreg{pairlist(i,2)}));
                S(i)=transpose(mu)/invcov*mu;
            end
            
            remaining = true(1,length(S));
            remaining(isnan(S))=false;
            pairnum=sum(remaining);
            
            while sum(remaining)>0
                if min(S(remaining))<R
                   % min(S(remaining))
                    pair=pairlist(find(S==min(S(remaining)),1),:);
                    NewReg{t}(NewReg{t}==pair(2))=pair(1);
                    removed=((pairlist(:,1)==pair(1))|(pairlist(:,2)==pair(1))|...
                        (pairlist(:,1)==pair(2))|(pairlist(:,1)==pair(2)));
                    %         sum(removed)
                    remaining(removed)=false;
                else
                    break
                end
            end
            
            if sum(remaining)/pairnum>.95
                newthresh = 1;
            else
                disp(sum(remaining))
            end
        end
        
        
        [sorted, sidx] = sort(NewReg{t}(:));
            sortedno0 = sorted(sorted>0);
            sidx=sidx(sorted>0);
        
            
            k=1;  %replace with ranking function
            for j=1:max(max(max(sortedno0)))
                if ~isempty(find(sortedno0==j,1))
                    if j~=k
                        pix=sidx(sortedno0==j);
                        NewReg{t}(pix)=k;
                    end
                    k=k+1;
                    
                end
                
                %      RegTest{find(RMThresh == R)}=NewReg{t};
            end
            num=max(max(max(NewReg{t})));
            disp(num);
    end
end
disp('Merged')


%% Clean Up Merged Regions
CleanReg=NewReg;
clean = 0;
clear Greg Yreg

parfor t=1:maxT
    
    num=max(max(max(CleanReg{t})));
    Merged=regionprops(CleanReg{t},'Area','PixelIdxList');
    g=adjacentRegionsGraph(CleanReg{t},6);
    pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
    coastal=g.Edges.Labels((g.Edges.Labels(:,1)==0),:);
    landlocked=setdiff(1:num,coastal(:,2));
    
    while ~isempty(landlocked)
        
        Greg=cell(1,num);
        Yreg=cell(1,num);
        LLpairlist = false(size(pairlist));
        num=max(max(max(CleanReg{t})))
        
        for i=1:num
            pix=Merged(i).PixelIdxList;
            Greg{i}=GFP{t}(pix);
            Yreg{i}=YFP{t}(pix);
            if ~isempty(find(landlocked==i,1))
                LLpairlist(pairlist==i)=true;
            end
        end
        
        LLpairs=pairlist((LLpairlist(:,1)|LLpairlist(:,2)),:);
        
        for k=landlocked
            
            kpairs=(LLpairs==k);
            kpairs=find(kpairs(:,1)|kpairs(:,2));
            S=zeros(length(kpairs),1);
            for j=1:length(kpairs)
                
                i=kpairs(j);
                mu=([mean(Greg{LLpairs(i,1)});mean(Yreg{LLpairs(i,1)})]...
                    -[mean(Greg{LLpairs(i,2)});mean(Yreg{LLpairs(i,2)})]);
                invcov=cov(double(Greg{LLpairs(i,1)}),double(Yreg{LLpairs(i,1)}))...
                    +cov(double(Greg{LLpairs(i,2)}),double(Yreg{LLpairs(i,2)}));
                S(j)=transpose(mu)/invcov*mu;
            end
            pair=LLpairs(kpairs(find(S==min(S))),:);
            CleanReg{t}(CleanReg{t}==pair(2))=pair(1);
        end
        
        k=1;
        for j=1:max(max(max(CleanReg{t})))
            if ~isempty(find(CleanReg{t}==j,1))
                if j~=k
                CleanReg{t}((CleanReg{t}==j))=k;
                end
                k=k+1;
            end
        end
        num=max(max(max(CleanReg{t})));
        %  disp('recleaned')
        
        
        Merged=regionprops(CleanReg{t},'Area','PixelIdxList');
        g=adjacentRegionsGraph(CleanReg{t},6);
        pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
        coastal=g.Edges.Labels((g.Edges.Labels(:,1)==0),:);
        landlocked=setdiff(1:num,coastal(:,2));
        
    end
    disp('Removed Landlocked')
    
    %Get rid of small
    MergedArea = [Merged.Area];
    small = find(MergedArea < minCellSize);
    
    while ~isempty(small)
        
        g=adjacentRegionsGraph(CleanReg{t},6);
        pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
        
        %Get rid of small unconnected
        for i=small
            if isempty(find(pairlist(:)==i,1))
                CleanReg{t}(CleanReg{t}==i)=0;
                small=small(small~=i);
            end
        end
        
        %Clean Up Label Numbers again
        k=1;
        for j=1:max(max(max(CleanReg{t})))
            if ~isempty(find(CleanReg{t}==j,1))
                if j~=k
                CleanReg{t}((CleanReg{t}==j))=k;
                end
                k=k+1;
            end
        end
        num=max(max(max(CleanReg{t})));
  %      disp('recleaned')
        Merged=regionprops(CleanReg{t},'Area','PixelIdxList');
        MergedArea = [Merged.Area];
        small = find(MergedArea < minCellSize);
        g=adjacentRegionsGraph(CleanReg{t},6);
        pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
        
        
        Greg=cell(1,num);
        Yreg=cell(1,num);
        Spairlist = false(size(pairlist));
        
        
        for i=1:num
            pix=Merged(i).PixelIdxList;
            Greg{i}=GFP{t}(pix);
            Yreg{i}=YFP{t}(pix);
            if ~isempty(find(small==i,1))
                Spairlist(pairlist==i)=true;
            end
        end
        
        Spairs=pairlist((Spairlist(:,1)|Spairlist(:,2)),:);
        
        for k=small
            kpairs=(Spairs==k);
            kpairs=find(kpairs(:,1)|kpairs(:,2));
            S=zeros(length(kpairs),1);
            for j=1:length(kpairs)
                try
                    i=kpairs(j);
                    mu=([mean(Greg{Spairs(i,1)});mean(Yreg{Spairs(i,1)})]...
                        -[mean(Greg{Spairs(i,2)});mean(Yreg{Spairs(i,2)})]);
                    invcov=cov(double(Greg{Spairs(i,1)}),double(Yreg{Spairs(i,1)}))...
                        +cov(double(Greg{Spairs(i,2)}),double(Yreg{Spairs(i,2)}));
                    S(j)=transpose(mu)/invcov*mu;
                catch
                    disp('error')
                end
            end
            pair=Spairs(kpairs(find(S==min(S))),:);
            CleanReg{t}(CleanReg{t}==pair(2))=pair(1);
        end
        
        k=1;
        for j=1:max(max(max(CleanReg{t})))
            if ~isempty(find(CleanReg{t}==j,1))
                if j~=k
                CleanReg{t}((CleanReg{t}==j))=k;
                end
                k=k+1;
            end
        end
        num=max(max(max(CleanReg{t})));
        %disp('recleaned')
        
        Merged=regionprops(CleanReg{t},'Area','PixelIdxList','BoundingBox');
        g=adjacentRegionsGraph(CleanReg{t},6);
        pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
        coastal=g.Edges.Labels((g.Edges.Labels(:,1)==0),:);
        landlocked=setdiff(1:num,coastal(:,2));
        MergedArea = [Merged.Area];
        small = find(MergedArea < minCellSize);
    end
    disp('Removed Small')
end


%%  More Cleaning???
%Repeat Merging Step for areas that were forced cleaned
CleanReg2=CleanReg;
parfor t=1:maxT
    S=0;
    num=max(max(max(CleanReg2{t})));
    Greg=cell(1,num);
    Yreg=cell(1,num);
    for R=RMThresh(end)
        newthresh = 0;
        R
        while newthresh == 0
            
            [sorted, sidx] = sort(CleanReg2{t}(:));
            sortedno0 = sorted(sorted>0);
            sidx=sidx(sorted>0);
            
            for i=1:num
                pix=sidx(ismembc(sortedno0,i));
                Greg{i}=GFP{t}(pix);
                Yreg{i}=YFP{t}(pix);
            end
            
            
            g=adjacentRegionsGraph(CleanReg2{t},6);
            pairlist=g.Edges.Labels((g.Edges.Labels(:,1)>0),:);
            S=zeros(1,length(pairlist));
            
            for i=1:length(pairlist)
                mu=([mean(Greg{pairlist(i,1)});mean(Yreg{pairlist(i,1)})]...
                    -[mean(Greg{pairlist(i,2)});mean(Yreg{pairlist(i,2)})]);
                invcov=cov(double(Greg{pairlist(i,1)}),double(Yreg{pairlist(i,1)}))...
                    +cov(double(Greg{pairlist(i,2)}),double(Yreg{pairlist(i,2)}));
                S(i)=transpose(mu)/invcov*mu;
            end
            
            remaining = true(1,length(S));
            remaining(isnan(S))=false;
            pairnum=sum(remaining);
            
            while sum(remaining)>0
                if min(S(remaining))<R
                    pair=pairlist(find(S==min(S(remaining)),1),:);
                    CleanReg2{t}(CleanReg2{t}==pair(2))=pair(1);
                    removed=((pairlist(:,1)==pair(1))|(pairlist(:,2)==pair(1))|...
                        (pairlist(:,1)==pair(2))|(pairlist(:,1)==pair(2)));
                    %         sum(removed)
                    remaining(removed)=false;
                else
                    break
                end
            end
            
            if sum(remaining)/pairnum>.95
                newthresh = 1;
            end
        end
        
        k=1;
        for j=1:max(max(max(CleanReg2{t})))
            if ~isempty(find(CleanReg2{t}==j,1))
                if j~=k
                CleanReg2{t}((CleanReg2{t}==j))=k;
                end
                k=k+1;
            end
            
        end
        num=max(max(max(CleanReg2{t})));
        disp(num)
    end
end
disp('ReMerged')

se2=cat(3,[0 0 0; 0 1 0; 0 0 0],[0 1 0; 1 1 1; 0 1 0],[0 0 0; 0 1 0; 0 0 0]);
for t=1:maxT
    CleanReg2{t}=imdilate(CleanReg2{t},se2);
end

%% Surfaces to Imaris
XPixel=(DataSet.ExtendMaxX-DataSet.ExtendMinX)/(DataSet.SizeX-1);
YPixel=(DataSet.ExtendMaxY-DataSet.ExtendMinY)/(DataSet.SizeY-1);
ZPixel=(DataSet.ExtendMaxZ-DataSet.ExtendMinZ)/(DataSet.SizeZ-1);

objlist=[];
for time=1:maxT
   time
   clear aObject
   objlist=[];
   aObject = aFactory.CreateSurfaces;
   aObject.SetName({'Gradient Surfaces'});
    LMFinal=CleanReg2{time};
    overlapPix=regionprops3(LMFinal,'VoxelList','Volume','VoxelIdxList');
    real=overlapPix(overlapPix.Volume>0,:);
    for j=1:height(real)
       AS=alphaShape(real.VoxelList{j},1);
       [BF P]=boundaryFacets(AS);
       Pscale=[P(:,2)*XPixel P(:,1)*YPixel P(:,3)*ZPixel]+[DataSet.ExtendMinX DataSet.ExtendMinY DataSet.ExtendMinZ];
       tri=triangulation(BF,Pscale);
       aObject.AddSurface(single(Pscale),int32(BF-1),single(vertexNormal(tri)),time-1)
       objlist=[objlist; mode(LMFinal(real.VoxelIdxList{j})) time-1];
       pause(0.5)
    end
    vParent = aImarisApplication.GetSurpassScene;
    vParent.AddChild(aObject, -1); 
end









