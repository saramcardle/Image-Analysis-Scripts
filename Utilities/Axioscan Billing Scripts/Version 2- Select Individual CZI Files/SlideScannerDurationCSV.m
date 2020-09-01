[files,pathname,idx]=uigetfile('*.czi',"Choose files from a single user",'MultiSelect','on');

cd(pathname)
[outfile,outpath]=uiputfile('.csv',"Where to output the data");


header={'File Name','Acquisition Date','Acquisition Time','Duration (ms)','Duration (min)'};
outMat={};
mins=[];
%cycle through all hand-chosen files
for f=1:length(files)
    filename=fullfile(pathname,files{f});
    clear acqduration date
    reader=bfGetReader(filename);
    globalmetadata=reader.getGlobalMetadata;
    
    metadataKeys = globalmetadata.keySet().iterator();
    for i=1:globalmetadata.size()
        key = metadataKeys.nextElement();
        if contains(key,'AcquisitionDuration')
            acqduration=globalmetadata.get(key); 
        elseif contains(key,'AcquisitionDateAndTime')
            date=globalmetadata.get(key);
        end
    end
    
    if exist('acqduration') && exist('date')
        
        dateSplit=split(date,'T');
        daySplit=split(dateSplit{1},'.');  
        timeSplit=split(dateSplit{2},'.');
        durationSplit=split(acqduration,'.');
        
        outMat{end+1,1}=files{f};
        outMat{end,2}=daySplit{1};
        outMat{end,3}=timeSplit{1};
        outMat{end,4}=durationSplit{1};
       % outMat{end,5}=num2str(mins(end),3);
       outMat{end,5}=str2num(acqduration)/1000/60;
        
        reader.close()
    end
end


outfile=fullfile(outpath, outfile);
fid2=fopen(outfile,'w');

fprintf(fid2,'%s,%s,%s,%s,%s\n',header{:});
for j=1:size(outMat,1)
    fprintf(fid2,'%s,%s,%s,%s,%.2f\n',outMat{j,:});
end

fprintf(fid2,'\n\n');

total=sum(cell2mat(outMat(:,5)));
fprintf(fid2,'%s,%6.2f','Total Acquisition Time (hours):',total/60);
fclose(fid2);