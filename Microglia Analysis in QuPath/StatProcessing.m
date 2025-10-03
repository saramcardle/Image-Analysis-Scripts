%% Setup

%First, use uiimport to load the csv of cells exported from QuPath as a table
%Make sure you pay attention to the appropriate column categories
%Name it raw

load('grouped.mat') %contains genotype and sex of each mouse

%assign the gene and sex information to the table of cells
gene = categorical(height(raw),1);
sex = categorical(height(raw),1);
for i=1:height(raw)
    gene(i,1)=grouped{raw.Image(i)==grouped.Image,'Gene'};
    sex(i,1)=grouped{grouped.Image==raw.Image(i),'Sex'};
end
raw.Gene=gene;
raw.Sex=sex;

%% Median per mouse
medians = groupsummary(raw(:,[1,4,7:end]),{'Gene','Sex','Image'},'median'); %may have to adjust column number depending on how you processed the .csv file!


%% Decile Analysis
%create a variable that will assign each cell to a decile category
steps=floor(quantile([1:height(raw)],9));  %decile thresholds
idxs=categorical((10-sum(le(1:10490,steps')))'); %1 index per cell, based on which decile it's in

%choose a variable
Variable='BI'; %'Aream2' , 'Circularity'
sorted = sortrows(raw,Variable,'ascend'); %sort data table by that variable
sorted.grouping = idxs; %assign it to a column in the table

varmean=groupsummary(sorted,'grouping','mean',{Variable});  %get the average value of Variable per decile
%calculate the average CD68 and Iba intensity per decile per genotype
subsetmean=groupsummary(sorted,{'Gene','grouping'},{'mean','stdev'},{'CD68sum','ROI033mPerPixelC3Mean'}); 

%begin formatting to make it easy to copy-paste into Prism
%directly into a grouped sheet with mean - sem - n format 
reformat = eval(['varmean.mean_' Variable]);

sqrtN=sqrt(subsetmean.GroupCount); %to turn StDev into SEM

%to get CD68 curve
reformatCD68=[reformat subsetmean.mean_CD68sum(1:10) subsetmean.std_CD68sum(1:10)./sqrtN(1:10) subsetmean.GroupCount(1:10) ...
subsetmean.mean_CD68sum(11:20) subsetmean.std_CD68sum(11:20)./sqrtN(11:20) subsetmean.GroupCount(11:20)];

%to get Iba curve
reformatIba=[reformat subsetmean.mean_ROI033mPerPixelC3Mean(1:10) subsetmean.std_ROI033mPerPixelC3Mean(1:10)./sqrtN(1:10) subsetmean.GroupCount(1:10) ...
subsetmean.mean_ROI033mPerPixelC3Mean(11:20) subsetmean.std_ROI033mPerPixelC3Mean(11:20)./sqrtN(11:20) subsetmean.GroupCount(11:20)];

