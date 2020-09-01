txtfile='D:\SCANS\AxioScanZ1_usage.txt';
fid=fopen(txtfile,'r');
tscan=textscan(fid,'%s%s','whitespace','\b\t','Delimiter',{'        '},'MultipleDelimsAsOne',1);
fclose(fid);

folder=txtfile(1:find(txtfile=='\',1,'last'));
filename=[datestr(datetime,29) ' Axioscan Usage.csv'];
outfile=[folder filename];

filelist=unique(tscan{1});
outMat=cell(1,5);

for i=1:length(filelist)
    lines=find(strcmp(tscan{1},filelist{i}));
    if length(lines)>1
        
        dtlinenumber=lines(find(cellfun(@(x) contains(x,'DateAndTime'),tscan{2}(lines)),1));
        dateline=tscan{2}{dtlinenumber};
        split1=split(dateline,{'>','Z'});
        split2=split(split1{2},'T');
        
        durlinenumber=lines(find(cellfun(@(x) contains(x,'Duration'),tscan{2}(lines)),1));
        durationline=split(tscan{2}{durlinenumber},{'<','>'});
        
        if length(dtlinenumber)>0 & length(durlinenumber)>0
            username=split(filelist{i},'/');
            if length(username)>1
                outMat(end+1,:)={username{1},filelist{i},split2{1},split2{2},durationline{3}};
            else
                username=split(filelist{i},'_');
                if length(username)>1
                    outMat(end+1,:)={username{1},filelist{i},split2{1},split2{2},durationline{3}};
                else
                    outMat(end+1,:)={'UNKNOWN',filelist{i},split2{1},split2{2},durationline{3}};
                end
            end
        end
    end
end


minutes=cell2mat(cellfun(@str2num, outMat(2:end,end),'UniformOutput',false))/1000/60;
minCell=arrayfun(@(x) num2str(x,2), minutes, 'UniformOutput',false);
outMat(2:end,end+1)=minCell;
fid2=fopen(outfile,'w');
header={'Username','File Name','Acquisition Date','Acquisition Time','Duration (ms)','Duration (min)'};
fprintf(fid2,'%s,%s,%s,%s,%s,%s\n',header{:});
for j=2:size(outMat,1)
    fprintf(fid2,'%s,%s,%s,%s,%s,%s\n',outMat{j,:});
end
fclose(fid2);

