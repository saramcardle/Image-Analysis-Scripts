%The TIRF image of a cell must be registered to the STORM data.  
%First, the CD16 channel was  drift corrected in Nikon NIS elements, and then the
%data was input into Thunderstorm (ImageJ). Thunderstorm produced a
%Gaussian rendering of the data. The TIRF image was registered to the
%rendering. 

I=imread('C:\Users\smcardle\Documents\Zhichao\registered TIRF.tif','tif'); %Load the registered TIRF image. 

I2=I-100;  %Subtract camera baseline value
I2(I2<0)=0; %Remove negative values
crop=I2(95:164,143:205); %Crop to the cell of interest

enlarge=400; %Enlargement factor for smoothing

cropLarge=imresize(gpuArray(crop),enlarge); %Resize cropped portion

%Convert intensity to distance from coverslip. See Sundd, et al., Nature, 2010.
d=488/(4*pi)*((((1.52*sin(80*pi/180))^2)-(1.33^2))^(-0.5));  %TIRF angle was 80 deg. 

L=max(max(crop));
if L<1
    L=1;
end
Imax=double(L);
h=25+d*log(Imax./cropLarge); %Brightest point is 25 nm from coverslip. 

%Load the localizations from the 3 channels. These were converted from the Nikon format to the Thunderstorm format.  

raw488=csvread('C:\Users\smcardle\Documents\Zhichao\Neu CD16-G MAB24-R KIM FR-2_405_488_N2T.csv',1,0); %Read all data
xy488=raw488(:,2:3); %Get just the XY positions
shift488=(xy488-(min(xy488)-60))/(160/400)+.5;  %Convert the nm positions into pixels. 
%Each raw pixel is 160 nm, but is enlarged 400x. 
%The gaussian rendered image starts 60 nm before the first CD16 point
%(3*sigma of rendering, where sigma = 20 mn)
%Matlab calls the center of the first pixel 1, therefore the edge is value
%0.5 and the data must be shifted 0.5 pixels.  

%Repeat for other 2 channels. 
raw561=csvread('C:\Users\smcardle\Documents\Zhichao\Neu CD16-G MAB24-R KIM FR-2_405_561_N2T.csv',1,0);
xy561=raw561(:,2:3);
shift561=(xy561-(min(xy488)-60))/.4+.5;

raw647=csvread('C:\Users\smcardle\Documents\Zhichao\Neu CD16-G MAB24-R KIM FR-2_405_647_N2T.csv',1,0);
xy647=raw647(:,2:3);
shift647=(xy647-(min(xy488)-60))/.4+.5;

for c=1:3  %For each channel individually
    switch c
        case 1
            dots=shift488;
        case 2
            dots=shift561;
        case 3
            dots=shift647;
    end
    
        cropped=round(dots(:,2))>=94.5*enlarge&round(dots(:,2))<=164.5*enlarge&round(dots(:,1))>=142.5*enlarge&round(dots(:,1))<=205.5*enlarge;  %Only process the dots inside the cell of interest (enlarged region)
    %Images are indexed as X,Y
    %Matrices are indexed as row,column
    croppeddots=dots(cropped,:); %Find the positions

    Z=0;
    for i=1:length(croppeddots)
          Z(i)=gather(h(round(croppeddots(i,2))-94.5*enlarge+1,round(croppeddots(i,1))-142.5*enlarge+1));  %find the intensity value at each XY location
    end
    
    %For each color
    switch c
        case 1
            cropped488=raw488(cropped,:); %use only the points in the cell of interest
            cropped488(:,4)=Z'; %Add the Z value into the matrix
        case 2
            cropped561 = raw561(cropped,:);
            cropped561(:,4)=Z';
        case 3
            cropped647 = raw647(cropped,:);
            cropped647(:,4) = Z';
    end
end
        
%Column names, Thunderstorm (ImageJ) format
header={'frame',	'x [nm]',	'y [nm]',	'z [nm]',	'sigma [nm]',	'intensity [photons]',	'offset [photons]',	'bkgstd [photons]',	'chi2',	'uncertainty [nm]'};

%Write a new file for each channel 

xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped488withZ2.xlsx',header)
xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped488withZ2.xlsx',cropped488,1,'A2')


xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped561withZ2.xlsx',header)
xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped561withZ2.xlsx',cropped561,1,'A2')


xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped647withZ2.xlsx',header)
xlswrite('C:\Users\smcardle\Documents\Zhichao\Cropped647withZ2.xlsx',cropped647,1,'A2')




