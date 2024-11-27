rdists=[200:200:1000 1500:500:8000 9000:1000:20000]; %define the relevant distances
annotjson ='F:\\Dirk\\geojsons\\6089-06_CD45 - 2019-11-10 15.11.29.ndpi_Tissue.geojson'; %path to .json file with your annotation outline, exported from QuPath
pixelSize=0.2207;
%centroids=[]; %detection centroids. Can be exported from QuPath in many different ways. The easiest is to export detection measurements to csv and then copy-paste. Or you can read an excel table.

text=fileread(annotjson);
json=jsondecode(text);

%In my experience, the json from a complicated tissue outline can be
%formatted differently from different files. I don't know why and I don't
%see a pattern. This complicated code gets at all of the variations I've
%seen. Perhaps there are more?

try
    coord=json.geometry.coordinates;
catch
    coord = json.features.geometry.coordinates;
end

if size(coord,1)==1
    tissue=polyshape(squeeze(coord));
else
    tissue=polyshape;
    for i=1:length(coord)
        if isnumeric(coord{i})
            tissue=addboundary(tissue,squeeze(coord{i}));
        else
            cs=coord{i};
            for j=1:size(cs)
                tissue=addboundary(tissue,squeeze(cs{j}));
            end
        end
        
    end
end

xyum = centroids / pixelSize;

[modK, modH]=ripleyKBounded(xyum,rdists,tissue,false);



function [K, H] = ripleyKBounded(dataXY,xK,tissue,useVertex)
% Calculates Ripley's K function and H function for a set of points and a
% complex bounding geometry
%
% Derived from: https://github.com/alexandrovteam/spatial-corals/blob/master/ripleyK.m
% which is, itself, derived from https://github.com/aludnam/MATLAB/blob/master/PatternAnalysis/kfunction.m
%
% Edited by Sara McArdle to deal with region boundaries
%
% OUTPUT
%  K: average number of points found within a certain radius, WEIGHTED BY
%  TISSUE AREA. If the tissue only takes up 50% of the imaginary circle,
%  the number of points is multiplied by 2.
%  H: vector values of Ripley's H, also called L(r)-r
%
%
% INPUT
%  dataXY - N-by-2 vector where N is number of datapoints. Each row
%       corresponds to x and y coordinates of each datapoint
%
%  xK - corresponds to the distances where K function should be computed.
%       K is the same size as xK... (radii)
%
%  tissue - Matlab polyshape of region boundary
%
%  useVertex- boolean for whether use a simplifying assumption that you can
%  calculate the distance between a point and the boundary only with the
%  tissue vertices. True if the tissue is sufficiently complex, false if
%  there are long straight lines.


[N,k] = size(dataXY);

%input check
if k~=2 error('dataXY must have two columns'); end

%Distance from each point to every other point
DIST = squareform(pdist(dataXY,'euclidean'));% DIST = NxN matrix with 0s along major diagonal
%sort each column individually by distance
DIST = sort(DIST);

%initialize variables
K = nan(size(xK)); %will hold sum of point counts
Nk = length(K); %number of K values expected ( =  number of radii testing)


if useVertex
    %find the smallest distance from each point to the boundary
    %helps minimize number of circles we'll eventually need to calculate
    vts=nearestvertex(tissue,dataXY); %find nearest vertex indices
    txy=tissue.Vertices(vts,:); %find xy locations of each vertex
    boundDists=sum(sqrt((dataXY-txy).^2),2); %calculate distance
else
    boundDists=zeros(1,length(dataXY));
end

theta = (0:47)*(2*pi/48); %part of a definition of a polyshape circle

wb = waitbar(0,'Computing Ripley''s K-function...');

for k=1:Nk %for each RADIUS
    Npts = sum(DIST(2:end,:)<xK(k)); %how many points are within that radius from EVERY point individually
    %%NOTE: this does NOT include the point itself. Possibly this is wrong
    %%and leads to an off-by-one error below. My understanding is that if
    %%you have few enough points for this error to matter, you probably
    %%shouldn't be using Ripley anyway.
    
    %how big is a circle of that radius?
    circle = polyshape(xK(k)*cos(theta),xK(k)*sin(theta));
    circlearea = area(circle); %Maximum possible search area
    
    %we're now going to loop over every point and calculate how much tissue
    %area was actually searched. This is much slower than the original
    %Ripley K function.
    
    fracArea=[]; %initialize weighting factor
    for p=1:length(dataXY) %loop over every point
        
        %if the nearest tissue boundary is more than R away, a full circle
        %can fit inside, so there's no need to calculate.
        %if you have long boundaries with no vertices, compared to the search radii,
        %this might not be true
        if boundDists(p)>xK(k)
            fracArea(p)=1;
        elseif Npts(p)==0 %if there are no pts, don't bother calculating a weighting factor, the answer is 0
            fracArea(p)=1;
        else
            %create a circle of radius xK(k) centered at point po
            x = dataXY(p,1) + xK(k)*cos(theta);
            y = dataXY(p,2) + xK(k)*sin(theta);
            circle = polyshape(x,y);
            
            %calculate the area of the intersection of that circle and the
            %tissue
            fracArea(p)= area(intersect(circle,tissue)) / circlearea;
        end
    end
    
    %weight the number of points in the "circle" by the tissue area
    %actually searched. This increases the apparent number of points as if
    %a full circle was valid.
    K(k) = sum(Npts./fracArea,'omitnan')/N; %average over all points
    waitbar(k/Nk,wb)
    clear circle
end
close (wb);

lambda = N/area(tissue); %total tissue density

%for each circle-ified point count, take sqrt and subtract r to get to H
%function
H=sqrt(K/lambda/pi)-xK;
end

