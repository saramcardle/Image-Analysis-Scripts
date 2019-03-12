threshold=8.2; %Threshold determined from molecular modelingm
coords=[]; %Initialize Paired coordinates variable

kimfile=csvread('C:\Users\smcardle\Downloads\Cropped647withZ uncertainty 31.1 nm depth 50 nm BC6.1nm_3nm z=0.csv',1,0); %Distance processed Kim127 molecules
mabfile=csvread('C:\Users\smcardle\Downloads\Cropped561withZ uncertainty 23.7 nm depth 50 nm BC7.3nm_3nm z=0.csv',1,0); %Distance prcoessed mAb24 molecules

kim=kimfile(:,2:3); %extract XY coordinates for kim127
mab=mabfile(:,2:3); %extract XY coordinates for mAb24

remainkim=kim; %remainkim are the kim molecules that have not yet been paired or eliminated
remainmab=mab;
shortened=1; %counter to ensure progress is continuing to be made
i=0;


while shortened>0 %continue searching while there continues to be new matches
    oldnum=length(remainkim); %number of kim molecules remaining
    objmab=KDTreeSearcher(remainmab); %efficient way to search all remaining mAb24
    objkim=KDTreeSearcher(remainkim); %efficient way to search all remaining Kim127
        
    [idxmab Dmab]=knnsearch(objkim,remainmab);  %Identity of the Kim molecule nearest each Mab molecule (1 entry for each Mab). Dmab is distance.
    [idxkim Dkim]=knnsearch(objmab,remainkim); %Identity of the Mab molecule nearest each kim molecule (1 entry for each Kim). Dkim is distance.

    
    match=(idxmab(idxkim)==[1:length(idxkim)]'); %Find mutual nearest neighbor pairs. Indexed into Kim list. 
    %IE- look up the Mab molecule that is closest to the 1st Kim molecule
    %(this is idxkim). Call this m. Look up the Kim molecule that is
    %closest to m (this is idxmab). If that entry says 1, than Kim 1 and Mab m are mutual nearest neighbor pairs. 
    %Repeat for every Kim molecule.
    
    far=(Dkim>threshold); %Find Kims that are too far from their nearest Mab to be on the same molecule
    matchclose=match&~far; %mutual nearest neighbor pairs that are near each other. 
    if sum(matchclose)<1 
        disp('break') %if there are none, stop looping
        break
    end
           
    mabpos=remainmab(idxkim(matchclose),:); %Find the coordinates of the Mab molecule in each pair
    kimpos=remainkim(matchclose,:); %Find the coordinates of the Kim molecule in each pair
    coords=cat(1,coords,[kimpos mabpos]); %Append these coordinates to the output variable
        
    remainkim=remainkim(~match|far,:); %Remove all Kim molecules that were added to coordinates, the rest are remainingkim
    
    farmab=(Dmab>threshold); %Find Mabs that are too far from their nearest Kim to be on the same molecule
    other=setdiff([1:length(remainmab)]',idxkim(match)); %find all Mabs that did not match
    other2=unique([other;find(farmab)]); %Find every Mab that is either unmatched or too far
    remainmab=remainmab(other2,:); %These are the remaining Mabs
    
    shortened=oldnum-length(remainkim); %Count how many molecules were added to coords
    i=i+1 %iteration just for display
end

%Coords have all Kim and Mab coordinates of the nearest neighbor pairs that
%are less than Threshold
%remainkim and remainmab are the coordinates of all other molecules in the
%input data

%Copy-pasted into Excel for further processing.




