%% Converts a series of .txt files with Islet and Tissue measurements into a more usable table form
% Also includes merging the image analysis data with clinical data
% It's unlikely anyone will use this exact code (there are more efficient methods!), it's just uploaded as an
% example of how to work with these opaque files. 

%% clinical info sheet
opts = spreadsheetImportOptions("NumVariables", 6);

% Specify sheet and range
opts.Sheet = "overview";
opts.DataRange = "A9:F58";

% Specify column names and types
opts.VariableNames = ["CaseIDBlock", "CD45name","PancreasHeadTail", "Var3", "Var4", "DonorType", "T1Dyears"];
opts.SelectedVariableNames = ["CaseIDBlock", "CD45name","PancreasHeadTail", "DonorType", "T1Dyears"];
opts.VariableTypes = ["string", "string","categorical", "char", "char", "string", "double"];

% Specify variable properties
opts = setvaropts(opts, ["CaseIDBlock", "Var3", "Var4", "DonorType"], "WhitespaceRule", "preserve");
opts = setvaropts(opts, ["CaseIDBlock", "PancreasHeadTail", "Var3", "Var4", "DonorType"], "EmptyFieldRule", "auto");

% Import the data
patients = readtable("F:\Dirk\nPOD slides 122018 edited.xlsx", opts, "UseExcel", false);

% Clear temporary variables
clear opts


%% qupath output

%Location of all the exported .csv files with islet and tissue measurements
folder='F:\Dirk\Results NewCD45 030623';
files=dir(folder);

opts = delimitedTextImportOptions("NumVariables", 523);
opts.VariableNamesLine=1;

% Specify range and delimiter
opts.DataLines = [2, Inf];
opts.Delimiter = "\t";

% Specify column names and types
%opts.VariableNames = ["Image", "Name", "Class", "Parent", "ROI", "CentroidXm", "CentroidYm", "CD45Neg", "CD45Pos", "CD45Pos1", "Aream2", "Circularity", "Solidity", "MaxDiameterm", "MinDiameterm", "PeriCD45Neg", "PerCD45Pos", "PeriCD45Pos", "CHGAAreaUm2", "CHGAArea", "GCGAreaUm2", "GCGArea", "IAPPAreaUm2", "IAPPArea", "PPYAreaUm2", "PPYArea", "ProINSTotalAreaUm2", "ProINSTotalArea", "SSTAreaUm2", "SSTArea", "GCGCHGAAreaUm2", "GCGCHGAArea", "GCGIAPPAreaUm2", "GCGIAPPArea", "GCGProINSTotalAreaUm2", "GCGProINSTotalArea", "GCGPPYAreaUm2", "GCGPPYArea", "GCGSSTAreaUm2", "GCGSSTArea", "CHGAIAPPAreaUm2", "CHGAIAPPArea", "CHGAProINSTotalAreaUm2", "CHGAProINSTotalArea", "CHGAPPYAreaUm2", "CHGAPPYArea", "CHGASSTAreaUm2", "CHGASSTArea", "IAPPSSTAreaUm2", "IAPPSSTArea", "ProINSTotalPPYAreaUm2", "ProINSTotalPPYArea", "ProINSTotalSSTAreaUm2", "ProINSTotalSSTArea", "PPYSSTAreaUm2", "PPYSSTArea", "UnionOfINSGCGCHGAIAPPProINSDarkProINSTotalPPYSSTAreaUm2", "UnionOfINSGCGCHGAIAPPProINSDarkProINSTotalPPYSSTArea", "INSGCGCHGAIAPPProINSDarkProINSTotalPPYSSTIOU", "GCGCHGAIOU", "GCGIAPPIOU", "GCGProINSTotalIOU", "GCGPPYIOU", "GCGSSTIOU", "CHGAIAPPIOU", "CHGAProINSTotalIOU", "CHGAPPYIOU", "CHGASSTIOU", "IAPPProINSTotalIOU", "IAPPPPYIOU", "IAPPSSTIOU", "ProINSTotalPPYIOU", "ProINSTotalSSTIOU", "PPYSSTIOU", "CHGAIsletAreaUm2", "CHGAIsletArea", "GCGIsletAreaUm2", "GCGIsletArea", "IAPPIsletAreaUm2", "IAPPIsletArea", "PPYIsletAreaUm2", "PPYIsletArea", "ProINSTotalIsletAreaUm2", "ProINSTotalIsletArea", "SSTIsletAreaUm2", "SSTIsletArea", "GCGCHGAIsletAreaUm2", "GCGCHGAIsletArea", "GCGIAPPIsletAreaUm2", "GCGIAPPIsletArea", "GCGProINSTotalIsletAreaUm2", "GCGProINSTotalIsletArea", "GCGPPYIsletAreaUm2", "GCGPPYIsletArea", "GCGSSTIsletAreaUm2", "GCGSSTIsletArea", "CHGAIAPPIsletAreaUm2", "CHGAIAPPIsletArea", "CHGAProINSTotalIsletAreaUm2", "CHGAProINSTotalIsletArea", "CHGAPPYIsletAreaUm2", "CHGAPPYIsletArea", "CHGASSTIsletAreaUm2", "CHGASSTIsletArea", "IAPPSSTIsletAreaUm2", "IAPPSSTIsletArea", "ProINSTotalPPYIsletAreaUm2", "ProINSTotalPPYIsletArea", "ProINSTotalSSTIsletAreaUm2", "ProINSTotalSSTIsletArea", "PPYSSTIsletAreaUm2", "PPYSSTIsletArea", "UnionOfINSGCGCHGAIAPPProINSDarkProINSTotalPPYSSTIsletAreaUm2", "UnionOfINSGCGCHGAIAPPProINSDarkProINSTotalPPYSSTIsletArea", "TotalIsletAreaUm2", "TotalIsletArea", "PeriCD45Pos1", "NumDetections", "NumCD45", "NumCHGA", "NumCHGAIAPP", "NumCHGAPPY", "NumCHGAProINSTotal", "NumCHGASST", "NumGCG", "NumGCGCHGA", "NumGCGIAPP", "NumGCGPPY", "NumGCGProINSTotal", "NumGCGSST", "NumIAPP", "NumIAPPSST", "NumIslet", "NumPPY", "NumPPYSST", "NumPeriIslet", "NumProINSTotal", "NumProINSTotalPPY", "NumProINSTotalSST", "NumSST", "NumUnionOfCHGAIAPP", "NumUnionOfCHGAPPY", "NumUnionOfCHGAProINSTotal", "NumUnionOfCHGASST", "NumUnionOfGCGCHGA", "NumUnionOfGCGIAPP", "NumUnionOfGCGPPY", "NumUnionOfGCGProINSTotal", "NumUnionOfGCGSST", "NumUnionOfIAPPPPY", "NumUnionOfIAPPProINSTotal", "NumUnionOfIAPPSST", "NumUnionOfINSGCGCHGAIAPPProINSDarkProINSTotalPPYSST", "NumUnionOfPPYSST", "NumUnionOfProINSTotalPPY", "NumUnionOfProINSTotalSST", "Perimeterm"];
opts.VariableTypes = ["string", "string", "categorical", "categorical", "categorical", "categorical", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double","double", "double", "double", "double", "double", "double", "double","double","double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double" "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double" "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double", "double", "double", "double", "double", "double","double", "double", "double", "double", "double"];

% Specify file level properties
opts.ExtraColumnsRule = "ignore";
opts.EmptyLineRule = "read";


%% generate measurement names
%single areas
variables={'Area_m_2','Perimeter_m','Circularity','Solidity','MaxDiameter_m','MinDiameter_m','TotalCellNum','TotalCD45Pct'};

%colocs and IOU
stains={'INS','GCG','IAPP','ProINSTotal','PPY','SST','Chromogranin','GCGRb','ProINSDark'};
colocnames={};
for i=1:length(stains)
    variables{end+1}=[stains{i} 'Area_'];
    variables{end+1}=[stains{i} 'AreaUm_2'];
    variables{end+1}=[stains{i} 'Nuclear_'];
    for j=i+1:length(stains)
        iouname=[stains{i} '_' stains{j} 'IOU_'];
        variables{end+1}=iouname;
        
        colocnamepct=[stains{i} '_' stains{j} 'Area_'];
        variables{end+1}=colocnamepct;
        colocnames{end+1}=colocnamepct;
        colocnameum=[stains{i} '_' stains{j} 'AreaUm_2'];
        variables{end+1}=colocnameum;
        
    end 
end

%add additional one-off measurements (cd45s, delaunays, etc)
variables=[variables, {'CD45Pos','CD45Pct','PeriCD45Pos','PeriCD45Pct','UnionEndocrineAreaUm_2','UnionEndocrineArea_','Delaunay_MeanDistance','Delaunay_MeanTriangleArea','TotalCD45Pos','RingCD45Pct','RingCD45Pos'}];

%% Read the .txt files and concatenate all information
cytomap=[];
for f=1:length(files)
    if endsWith(files(f).name,'.txt')
        
        data = readtable(fullfile(folder,files(f).name), opts);
        
        %rename variables that are too long / unwieldy
        areacol=find(strcmp(data.Properties.VariableDescriptions,'Union of: INS: GCG: IAPP: ProINS Dark: ProINS Total: PPY: SST: Chromogranin: GCG Rb area um^2'));
        pctcol=find(strcmp(data.Properties.VariableDescriptions,'Union of: INS: GCG: IAPP: ProINS Dark: ProINS Total: PPY: SST: Chromogranin: GCG Rb area %'));
        data=renamevars(data,[data.Properties.VariableNames(1,[areacol;pctcol])],["UnionEndocrineAreaUm_2","UnionEndocrineArea_"]);

        %to correct a mistake in my original code 
        %that called total islet cells Peri-islet cells. 
        data=renamevars(data,["x_CD45Pos","Peri_CD45Pos","PeriCD45Pos","PeriCD45Neg"],["CD45Pct","TotalCD45Pct","TotalCD45Pos","TotalCD45Neg"]);
        
        %find the patient in the clinical spreadsheet
        
        %get patient ID num
        pRow=find(strcmp(patients.CD45name,char(data{1,1})));
        imgnameparts=split(char(data{1,1}),'_');
        patientnum=imgnameparts{1};
        
        %get their T1D type
        type=patients.DonorType(pRow);
        
        %get location of this biopsy (head or tail)
        location=patients.PancreasHeadTail(pRow);
        
        %remove Tissue measurements
        islets=data(data.Class=="Islet",:);
        
        %calculate missing CD45 nums
        %Ring = Peri only, not including intra-islet cells
        islets.RingCD45Pos=islets.TotalCD45Pos-islets.CD45Pos;
        islets.RingCD45Pct=islets.RingCD45Pos./(islets.TotalCD45Neg+islets.TotalCD45Pos-islets.CD45Neg-islets.CD45Pos);
        islets.TotalCellNum = islets.TotalCD45Neg+islets.TotalCD45Pos;
        
        validvars=islets.Properties.VariableNames;

        %append each varialbe to islets table
        for v=1:length(variables)
            if sum(strcmp(validvars,variables{v}))==0
                eval(['islets.' variables{v} '=zeros(height(islets),1);'])
            end
        end
               
        %append matching clinical information as a table column
        islets.patientnum=repmat(string(patientnum),height(islets),1);
        islets.type=repmat(type,height(islets),1);
        islets.location=repmat(location,height(islets),1);       
        
        %%reformat in the most convenient order for cytomap
        exportable=islets(:,[{'Image','Name','patientnum','type','location','CentroidX_m','CentroidY_m'},variables]);      
        
        %concatenate to overall table with all samples
        cytomap=[cytomap; exportable];
    end
end
%% Cytomap export

%variables I plan to use for UMAP dimensionality reduction
tsnevars={'Area_m_2','Circularity','INSAreaUm_2','GCGAreaUm_2','IAPPAreaUm_2','ProINSTotalAreaUm_2','GCGRbAreaUm_2','PPYAreaUm_2','SSTAreaUm_2','ChromograninAreaUm_2','UnionEndocrineAreaUm_2','Delaunay_MeanTriangleArea','TotalCD45Pos'};

%prep variables to hold normalized data
normvars=cellfun(@(x) [x '_log'],tsnevars,'UniformOutput',false);
cytomapnorm=cytomap(:,{'Image','Name','patientnum','type','location','CentroidX_m','CentroidY_m'});

for v=1:length(tsnevars)
    %for all data in units of area, log transform 
    %area variables tend to be log-normal
    if (contains(tsnevars{v},'Um_2') || contains(tsnevars{v},'_m') || contains(tsnevars{v},'_Mean') || contains(tsnevars{v},'CD45')) %lognormal, +min/2
        plotdata=cytomap{:,tsnevars{v}};
        minval=min(plotdata(plotdata>0));
        cytomapnorm=addvars(cytomapnorm,log(plotdata+minval/2),'NewVariableNames',normvars{v});
    else
        %shape variables don't get logged
        plotdata=cytomap{:,tsnevars{v}};
        cytomapnorm=addvars(cytomapnorm,plotdata,'NewVariableNames',normvars{v});
    end
    
    %error checking
    if sum(isnan(plotdata))>0
        disp(tsnevars{v})
    end
end

%merge normalized values for dimensionality reduction with raw values for
%plotting
cytomapnorm=[cytomapnorm cytomap(:,tsnevars) cytomap(:,[{'CD45Pos','RingCD45Pos','INSArea_','GCGArea_','IAPPArea_','ProINSTotalArea_','GCGRbArea_','PPYArea_','SSTArea_','ChromograninArea_','UnionEndocrineArea_','CD45Pct','RingCD45Pct','TotalCellNum'}, colocnames])];

delaunay=cytomapnorm.Delaunay_MeanTriangleArea;
%1 pt out of ~25000 had a NaN for it's delaunay area. Set it to the max
%possible Delaunay area
cytomapnorm.Delaunay_MeanTriangleArea(isnan(delaunay))=pi*4000^2;
cytomapnorm.Delaunay_MeanTriangleArea_log(isnan(delaunay))=log(pi*4000^2+min(delaunay)/2);

%write output
writetable(cytomapnorm,fullfile('F:\Dirk\IsletResults','cytomapNormimport.csv'))

%see also the file for converting this (plus cytomap results) to a .fcs     file