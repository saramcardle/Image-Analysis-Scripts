# -*- coding: utf-8 -*-
"""
Created on Mon Dec  3 10:04:16 2018

@author: smcardle
"""

#Select files for processing
#Using external python because tkinter has more options than ZenWindow
import tkinter as tk
from tkinter import filedialog

root=tk.Tk()
filename=tk.filedialog.askopenfilenames(parent=root, title="Choose Files to Flatten",filetypes=[('Zeiss Images', '*.czi')])
root.destroy()  
print(filename)
