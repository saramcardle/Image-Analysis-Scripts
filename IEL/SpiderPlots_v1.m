%choose and open a file
uiopen('*.csv');

%rename auto-named variables
x=who;
raw=eval(x{1});

%just for convenience because the extra 9 0s are annoying
raw.TrackID=raw.TrackID-1000000000;

%find each track
tracks=unique(raw.TrackID);

%plot
figure
hold on

for i=1:length(tracks)
    %find location of each cell in each valid timepoint
    rows=raw.TrackID==tracks(i);
    
    %move starting location to 0,0,0
    disp=raw(rows,1:3);    
    disp{:,1}=disp{:,1}-disp{1,1};
	disp{:,2}=disp{:,2}-disp{1,2};
	disp{:,3}=disp{:,3}-disp{1,3};
    
    %actually plot
    plot(disp{:,1},disp{:,2},'-','Color',rand([1,3]))
end

%set labels
xlabel('X Distance from Origin, \mum')
ylabel('Y Distance from Origin, \mum')
axis equal