% How the Jaccard indices (Intersection-Over-Union) were calculated
% This is more complicated than just division because I wanted to ensure
% that islets with a union area of 0 were excluded from the analysis instead of
% being averaged in as a Inf or a 0


jaccard=table('Size',[24578 3],'VariableTypes',{'string','string','categorical'},'VariableNames',{'pt','type','loc'});

%colocs and IOU
stains={'INS','GCG','IAPP','ProINSDark','ProINSTotal', 'PPY','SST','Chromogranin','GCGRb'};
colocnames={};
unionnames={};
strs={}
for i=1:length(stains)
    for j=i+1:length(stains)
        strs{end+1}=[stains{i} '_' stains{j}];
        colocnameum=[stains{i} '_' stains{j} 'AreaUm_2'];
        colocnames{end+1}=colocnameum;
        unionnames{end+1}=['UnionOf_' stains{i} '_' stains{j} 'AreaUm_2'];
        eval(['jaccard.' strs{end} 'intersection=nan(24578,1);']);
        eval(['jaccard.' strs{end} 'union=nan(24578,1);']);
        eval(['jaccard.' strs{end} 'iou=nan(24578,1);']);
    end
end
    
   
r=0;                   
missingfiles=0;
for f=1:length(files)
    if endsWith(files(f).name,'.txt')
        %data=struct2table(tdfread(fullfile(folder,files(f).name)));
        data = readtable(fullfile(folder,files(f).name), opts);

        areacol=find(strcmp(data.Properties.VariableDescriptions,'Union of: INS: GCG: IAPP: ProINS Dark: ProINS Total: PPY: SST: Chromogranin: GCG Rb area um^2'));
        pctcol=find(strcmp(data.Properties.VariableDescriptions,'Union of: INS: GCG: IAPP: ProINS Dark: ProINS Total: PPY: SST: Chromogranin: GCG Rb area %'));
        data=renamevars(data,[data.Properties.VariableNames(1,[areacol;pctcol])],["UnionEndocrineAreaUm_2","UnionEndocrineArea_"]);
        %data=renamevars(data,["x_CD45Pos","Peri_CD45Pos"],["CD45Pct","PeriCD45Pct"]);
        data=renamevars(data,["x_CD45Pos","Peri_CD45Pos","PeriCD45Pos"],["CD45Pct","TotalCD45Pct","TotalCD45Pos"]);
        
        pRow=find(strcmp(patients.CD45name,char(data{1,1})));
        imgnameparts=split(char(data{1,1}),'_');
        patientnum=imgnameparts{1};
        %patientnum=imagename(1:7);
        
        % pRow=find(strcmp(patients.CaseIDBlock,patientnum),1);
        type=patients.DonorType(pRow);
        location=patients.PancreasHeadTail(pRow);

        islets=data(data.Class=="Islet",:);
        validvars=islets.Properties.VariableNames;
        
        jaccard.pt(r+1:r+height(islets))=repmat(patientnum,[height(islets),1]);
        jaccard.type(r+1:r+height(islets))=repmat(type,[height(islets),1]);
        jaccard.loc(r+1:r+height(islets))=repmat(location,[height(islets),1]);
        jaccard.CD45Pos(r+1:r+height(islets))=islets.CD45Pos;
        jaccard.IsletArea(r+1:r+height(islets))=islets.Area_m_2;
        jaccard.EndocrineAreaUm_2(r+1:r+height(islets))=islets.UnionEndocrineAreaUm_2;
        
        for i=1:9
           str = [stains{i} 'AreaUm_2'];
           if sum(strcmp(str,validvars)>0)
               eval(['jaccard.' str '(r+1:r+height(islets))=islets.' str ';']);
           else 
               eval(['jaccard.' str '(r+1:r+height(islets))=zeros(height(islets),1);']);
          end
        end

        for i=1:36
            str=colocnames{i}(1:end-8);
                parts=split(str,'_');
                idx1=find(strcmp([parts{1} 'AreaUm_2'],validvars));
                idx2=find(strcmp([parts{2} 'AreaUm_2'],validvars));
            if sum(strcmp(colocnames{i},validvars))>0 & sum(strcmp(unionnames{i},validvars))>0
                eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=islets.' colocnames{i} ';']);
                eval(['union=islets.' unionnames{i} ';'])
                eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=union;']);
                eval(['iou=islets.' colocnames{i} './union;'])
                iou(union./islets.Area_m_2<.01)=nan;
                eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=iou*100;']);
            elseif sum(strcmp(colocnames{i},validvars))>0
                if ~isempty(idx1)&&~isempty(idx2)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=islets.' colocnames{i} ';']);
                    eval(['union=islets.' parts{1} 'AreaUm_2+islets.' parts{2} 'AreaUm_2 - islets.' colocnames{i} ';']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=union;']);
                    eval(['iou=islets.' colocnames{i} './union;']);
                    iou(union./islets.Area_m_2<.01)=nan;
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=iou*100;']);
                elseif ~isempty(idx1)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=islets.' parts{1} 'AreaUm_2;']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=0./islets.'  parts{1} 'AreaUm_2;']);
                elseif ~isempty(idx2)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=islets.' parts{2} 'AreaUm_2;']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=0./islets.'  parts{2} 'AreaUm_2;']);
                else
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=nan(height(islets),1);']);
                end
            else
                 if ~isempty(idx1)&&~isempty(idx2)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['union=islets.' parts{1} 'AreaUm_2+islets.' parts{2} 'AreaUm_2;'])
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=union;']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=zeros(height(islets),1);']);
                elseif ~isempty(idx1)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=islets.' parts{1} 'AreaUm_2;']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=0./islets.'  parts{1} 'AreaUm_2;']);
                elseif ~isempty(idx2)
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=islets.' parts{2} 'AreaUm_2;']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=0./islets.'  parts{2} 'AreaUm_2;']);
                else
                    eval(['jaccard.' strs{i} 'intersection(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'union(r+1:r+height(islets))=zeros(height(islets),1);']);
                    eval(['jaccard.' strs{i} 'iou(r+1:r+height(islets))=nan(height(islets),1);']);
                end
                disp(['no coloc' strs{i}])
                missingfiles=missingfiles+1;
                             
            end
        end
            r=r+height(islets);
    end
    
end

% This part is for calculating "expected" IOU based on the union area and 2
% individual hormone areas. 

jaccard.INSpct_ProINSTotalunion=jaccard.INSAreaUm_2./jaccard.INS_ProINSTotalunion;
jaccard.ProINSTotalpct_INSunion=jaccard.ProINSTotalAreaUm_2./jaccard.INS_ProINSTotalunion;

jaccard.IAPPpct_ProINSTotalunion=jaccard.IAPPAreaUm_2./jaccard.IAPP_ProINSTotalunion;
jaccard.ProINSTotalpct_IAPPunion=jaccard.ProINSTotalAreaUm_2./jaccard.IAPP_ProINSTotalunion;

jaccard.IAPPpct_INSunion=jaccard.IAPPAreaUm_2./jaccard.INS_IAPPunion;
jaccard.INSpct_IAPPunion=jaccard.INSAreaUm_2./jaccard.INS_IAPPunion;

jaccard.GCGpct_Chromograninunion=jaccard.GCGAreaUm_2./jaccard.GCG_Chromograninunion;
jaccard.Chromograninpct_GCGunion=jaccard.ChromograninAreaUm_2./jaccard.GCG_Chromograninunion;

jaccard.GCGpct_GCGRbunion=jaccard.GCGAreaUm_2./jaccard.GCG_GCGRbunion;
jaccard.GCGRbpct_GCGunion=jaccard.GCGRbAreaUm_2./jaccard.GCG_GCGRbunion;

jaccard.GCGRbpct_Chromograninunion=jaccard.GCGRbAreaUm_2./jaccard.Chromogranin_GCGRbunion;
jaccard.Chromograninpct_GCGRbunion=jaccard.ChromograninAreaUm_2./jaccard.Chromogranin_GCGRbunion;

jaccard.INSpct_Chromograninunion=jaccard.INSAreaUm_2./jaccard.INS_Chromograninunion;
jaccard.Chromograninpct_INSunion=jaccard.ChromograninAreaUm_2./jaccard.INS_Chromograninunion;

for i=1:9
    eval(['jaccard.' stains{i} 'pct=jaccard.' stains{i} 'AreaUm_2;']);
end
    

%arrayfun(@isfinite,jaccard.iou)
summaries={};
for i=1:36
    eval(['rows=find(arrayfun(@isfinite,jaccard.' strs{i} 'iou));']);
    eval(['summaries{i}=groupsummary(jaccard(rows,{''pt'',''loc'',''' strs{i} 'iou''}),["pt","loc"],''mean'');']);
    summaries{i}=renamevars(summaries{i},'GroupCount',[strs{i} 'Count']);
end

unions={'INSpct_ProINSTotalunion','ProINSTotalpct_INSunion','IAPPpct_ProINSTotalunion','ProINSTotalpct_IAPPunion','IAPPpct_INSunion','INSpct_IAPPunion','GCGpct_Chromograninunion','Chromograninpct_GCGunion','GCGpct_GCGRbunion','GCGRbpct_GCGunion','GCGRbpct_Chromograninunion','Chromograninpct_GCGRbunion','INSpct_Chromograninunion','Chromograninpct_INSunion'};
for i=1:14
    eval(['rows=find(arrayfun(@isfinite,jaccard.' unions{i} '));']);
    eval(['summaries{i+36}=groupsummary(jaccard(rows,{''pt'',''loc'',''' unions{i} '''}),["pt","loc"],''mean'');']);
end

output=groupsummary(jaccard,["pt","loc","type"]);

output{output.type=="AutoAb+ (2)","type"}="AAb";
output{output.type=="AutoAb+ (1)","type"}="AAb";

for i=1:(14+36)
    output=outerjoin(summaries{i},output,'Keys',{'pt','loc'},'MergeKeys',true);
end
    
names=[output.Properties.VariableNames];
cols= contains(names,"iou")|contains(names,"pct")| contains(names,"union");
doublesummary=groupsummary(output,{'type','loc'},"mean",cols);

