%Calculates shape and movement statistics for surface objects in Imaris.
%Designed for analysis of macrophage motion. 
%Finds standard metrics (Volume, Extent, Solidity, Axes lengths, ellipticity)
%Also calculates new metrics. 
%1) Various "Dancing on the Spot" metrics- How much a shape has changed
%between 2 time points. Based on which pixels are occupied.
%Dancing = pixels occupied newly in timepoint 2 / total pixels occupied in timepoints 1 or 2
%Change = pixels occupied only in timespoints 1 or 2 (not both) / total pixels occupied in timepoints 1 or 2
%Each of them are calculated with raw data, and when the centroid of
%timepoint 2 has been moved to the centroid of timepoint 1 (adjusted)
%
%2) Outside of sphere- how many protrusion does the cell have outside an
%imagined sphere of the same volume and centroid. This is a measure of how
%"dendritic" the shape looks. The angle, volume, and length of each protrusion is measured.
%It can differentiate a) very round cells (high sphicity, low protrusion number and volume) 
%b) cells that are shaped more like endothelial cells (elongated, with 
%1 or 2 protrusions of high volume) 
%c) cells shaped like dendritic cells (multiple protrusions of small volume). 
%The protrusions are tracked overtime to monitor shape changes.  

%Written by Sara McArdle of the La Jolla Institute Microscopy Core, 2017. 
%For questions, please contact smcardle@lji.org

fname='E:\Program Data\Bitplane\DataStorage\Compensated_new_Segmentation_rounded_v2_trackattempt6.ims'; %put in correct path
obj=ImarisReader(fname);
Surfs=obj.Surfaces;
Macs=Surfs(1); %put in correct surface number

trackIds=Macs.GetTrackIDs; 
tracks=unique(trackIds);
edges=Macs.GetTrackEdges;

for i=tracks' %for each tracked cell
    idNums=(trackIds==i);
    edgesind=edges(idNums,:);
    cellIds=[edgesind(:,1);edgesind(end,2)];
    masks=cell(1,length(cellIds));
    k=1;
    for j=cellIds' %for each cell surface along the track
        masks{k}=Macs.GetMask(j); %get the mask
        k=k+1;
    end
    masksAll{i-1e9+1}=masks; %put all masks into a cell array
end

sizedata=size(masksAll{1}{1});
masksFill=cell(1,length(tracks));

%fill holes in imaris cell masks
parfor i=1:length(masksAll) 
    l=length(masksAll{i})
    fill=cell(1,l);
    for j=1:l
        j
        white=ones(sizedata(1),sizedata(2),sizedata(3)+2);
        image=masksAll{i}{j};
        white(:,:,2:end-1)=image;
        filled=imfill(white,'holes');
        fill{j}=filled(:,:,2:end-1);
    end
    masksFill{i}=fill;
end


%%Generate Statistics
regioncents=[];
statsAll=cell(1,length(masksAll));
for a=1:length(masksAll) %a is counter for track
    a
    stats=[];
    pixelList=cell(1,length(masksAll{a}));
    for b=1:length(masksAll{a}) %b is counter for individual cell surface
        RP2=regionprops(uint8(masksFill{a}{b}),'Area','BoundingBox','Centroid','PixelIdxList','PixelList');  %Calculate standard statistics with regionprops3
        BB=RP2.BoundingBox;
        boxVolume=BB(4)*BB(5)*BB(6);
        RP2.Extent=RP2.Area/boxVolume; %Extent = filled volume / bounding box volume
        [~,v]=convhulln(RP2.PixelList);
        RP2.Solidity=RP2.Area/v; %Solidity = filled volume / convex hull volume
        RP3=regionprops3(uint8(masksFill{a}{b}),'MajorAxisLength','Eccentricity','AllAxes'); 
        fields3=fieldnames(RP3); %add 3D statistics to 2D lists
        for c=1:length(fields3)
            RP2=setfield(RP2,fields3{c},getfield(RP3,fields3{c})); 
        end
        %Calculate ellipsity (ratio of different axes lengths)
        RP2.Ellipsity1=RP3.SecondAxisLength/RP3.FirstAxisLength;
        RP2.Ellipsity2=RP3.ThirdAxisLength/RP3.FirstAxisLength;
        RP2.Ellipsity3=RP3.ThirdAxisLength/RP3.SecondAxisLength;
        
        %The following statistics are designed to 
        if b==1
            RP2.Dancing=[];
            RP2.Change=[];
            RP2.AdjDancing=[];
            RP2.AdjChange=[];
        else
            %Change In Pixels
            dance=setdiff(RP2.PixelIdxList,stats(b-1).PixelIdxList);
            
            change2=setdiff(stats(b-1).PixelIdxList,RP2.PixelIdxList);
            changeAll=union(dance,change2);
            pixelAll=union(stats(b-1).PixelIdxList,RP2.PixelIdxList);
            RP2.Dancing=length(dance)/length(RP2.PixelIdxList);
            RP2.Change=(length(changeAll)/length(pixelAll));
            
            %Centroid Adjusted Change in Pixels
            shift=round(RP2.Centroid-stats(b-1).Centroid);
            
            while max(stats(b-1).PixelList(:,3)+shift(3))>max(sizedata(3))
                shift(3)=shift(3)-1;
            end
            while min(stats(b-1).PixelList(:,3)+shift(3))<1
                shift(3)=shift(3)+1;
            end
            adjusted=[stats(b-1).PixelList(:,1)+shift(1) stats(b-1).PixelList(:,2)+shift(2) stats(b-1).PixelList(:,3)+shift(3)];
            adjIdxList=sub2ind(sizedata,adjusted(:,2),adjusted(:,1),adjusted(:,3));
            danceAdj=setdiff(RP2.PixelIdxList,adjIdxList);
            change2Adj=setdiff(adjIdxList,RP2.PixelIdxList);
            changeAllAdj=union(danceAdj,change2Adj);
            pixelAll=union(adjIdxList,RP2.PixelIdxList);
            
            
            RP2.AdjDancing=length(danceAdj)/length(RP2.PixelIdxList);
            RP2.AdjChange=(length(changeAllAdj)/length(pixelAll));
            
            
        end
        
        %%Outside of Sphere Stats
        zratio=3/.89; %put in pixel dimensions
        radius=(RP2.Area*zratio*3/4/pi)^(1/3);
        inside=false(1,length(RP2.PixelList));
        RP2.Radius=radius;
        
        %generate sphere with the same total volume and centroid position
        for i=1:length(RP2.PixelList)
            % if sqrt(sum((rp.PixelList(i,:)-rp.Centroid).^2))<=radius
            if sqrt((RP2.PixelList(i,1)-RP2.Centroid(1))^2 + (RP2.PixelList(i,2)-RP2.Centroid(2))^2 + (zratio*(RP2.PixelList(i,3)-RP2.Centroid(3)))^2)<=radius
                inside(i)=true;
            end
        end
        
        RP2.SphereRatio=sum(inside)/RP2.Area;
        
        %find regions of the cell outside that sphere
        imagesphere=uint8(masksFill{a}{b});
        imagesphere(RP2.PixelIdxList(inside))=0
        outside=regionprops(imagesphere>0,'Area','Centroid','PixelIdxList'); %generate basic statistics
        
        
        count=false(1,length(outside));
        for j=1:length(outside)
            if outside(j).Area>10 %Only look at protrusion with volume > 10 pixels. Adjustable. 
                count(j)=true;
                [th phi l]=cart2sph(outside(j).Centroid(1)-RP2.Centroid(1), outside(j).Centroid(2)-RP2.Centroid(2), zratio*(outside(j).Centroid(3)-RP2.Centroid(3))); %angle of protrusion
                %add to statistics structure
                outside(j).theta=th;
                outside(j).phi=phi;
                outside(j).length=l-radius;
                
                %find border pixels
                black=zeros(size(image));
                black(outside(j).PixelIdxList)=1;
                dilated=imdilate(black,ones(3,3,3));
                pil2=find(dilated>0);
                outside(j).SA=length(intersect(pil2,RP2.PixelIdxList)); 
                regioncents(end+1,:)=[outside(j).Centroid b];
            end
        end
        
        %calculate per-cell protrusion staistics
        RP2.RegionsOutside=sum(count);
        RP2.OutsideStats=outside(count);
        RP2.OutTheta=[outside(count).theta];
        RP2.OutPhi=[outside(count).phi];
        RP2.OutLength=[outside(count).length];
        RP2.OutSA=[outside(count).SA];
        RP2.OutVolume=[outside(count).Area]*zratio;
        
        stats=[stats RP2];
    end
    statsAll{a}=stats;
end

%Average all stats for each cell by track
TrackStats=struct([]);
for a=1:length(masksAll)
    TrackStats(a).Area=mean([statsAll{a}.Area]);
    TrackStats(a).Extent=mean([statsAll{a}.Extent]);
    TrackStats(a).Solidity=mean([statsAll{a}.Solidity]);
    TrackStats(a).MajorAxisLength=mean([statsAll{a}.MajorAxisLength]);
    TrackStats(a).MeridionalEccentricity=mean([statsAll{a}.MeridionalEccentricity]);
    TrackStats(a).EquatorialEccentricity=mean([statsAll{a}.EquatorialEccentricity]);
    TrackStats(a).Ellipsity1=mean([statsAll{a}.Ellipsity1]);
    TrackStats(a).Ellipsity2=mean([statsAll{a}.Ellipsity2]);
    TrackStats(a).Ellipsity3=mean([statsAll{a}.Ellipsity3]);
    DI=[statsAll{a}.Dancing];
    TrackStats(a).Dancing=median(DI(2:end));
    ADI=[statsAll{a}.AdjDancing];
    TrackStats(a).AdjDancing=median(ADI(2:end));
    CI=[statsAll{a}.Change];
    TrackStats(a).Change=median(CI(2:end));
    ACI=[statsAll{a}.AdjChange];
    TrackStats(a).AdjChange=median(ACI(2:end));
end

%save all data, including masks. Uses v7.3 because mask data is very large.
save('Statistics','annot','masksAll','statsAll','TrackStats','masksFill','-v7.3');
