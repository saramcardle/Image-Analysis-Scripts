%   <CustomTools>
%       <Menu>
%           <Submenu name="Surfaces Functions">
%               <Item name="Compensation_SmallData" icon="Matlab"
%                   tooltip="Separate 2 channels that bleed into each other">
%                   <Command>MatlabXT::Compensation_SmallData(%i)</Command>
%               </Item>
%           </Submenu>
%       </Menu>
%
%       <SurpassTab>
%           <SurpassComponent name="bpSurfaces">
%               <Item name="Compensation_SmallData" icon="Matlab"
%                   tooltip="Separate 2 channels that bleed into each other">
%                   <Command>MatlabXT::Compensation_SmallData(%i)</Command>
%               </Item>
%           </SurpassComponent>
%       </SurpassTab>
%   </CustomTools>



function varargout = Compensation_SmallData(varargin)
% COMPENSATION_SMALLDATA MATLAB code for Compensation_SmallData.fig
%      COMPENSATION_SMALLDATA, by itself, creates a new COMPENSATION_SMALLDATA or raises the existing
%      singleton*.
%
%      H = COMPENSATION_SMALLDATA returns the handle to a new COMPENSATION_SMALLDATA or the handle to
%      the existing singleton*.
%
%      COMPENSATION_SMALLDATA('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in COMPENSATION_SMALLDATA.M with the given input arguments.
%
%      COMPENSATION_SMALLDATA('Property','Value',...) creates a new COMPENSATION_SMALLDATA or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before Compensation_SmallData_OpeningFcn gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to Compensation_SmallData_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES

% Edit the above text to modify the response to help Compensation_SmallData

% Last Modified by GUIDE v2.5 14-Sep-2018 16:08:33

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
                   'gui_Singleton',  gui_Singleton, ...
                   'gui_OpeningFcn', @Compensation_SmallData_OpeningFcn, ...
                   'gui_OutputFcn',  @Compensation_SmallData_OutputFcn, ...
                   'gui_LayoutFcn',  [] , ...
                   'gui_Callback',   []);
if nargin && ischar(varargin{1})
    gui_State.gui_Callback = str2func(varargin{1});
end

if nargout
    [varargout{1:nargout}] = gui_mainfcn(gui_State, varargin{:});
else
    gui_mainfcn(gui_State, varargin{:});
end
% End initialization code - DO NOT EDIT


% --- Executes just before Compensation_SmallData is made visible.
function Compensation_SmallData_OpeningFcn(hObject, eventdata, handles, varargin)
% This function has no output args, see OutputFcn.
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)
% varargin   command line arguments to Compensation_SmallData (see VARARGIN)

% Choose default command line output for Compensation_SmallData
handles.output = hObject;

% Update handles structure
guidata(hObject, handles);

disp(varargin)
handles.data=varargin{1} %WHY? WHY DID I DO THIS?
guidata(hObject,handles)

Status=findobj('Tag','Status_text');
Status.String={'Connecting'};
drawnow
xapp=xtconnectimaris(handles.data);
DataSet=xapp.GetDataSet;
AllData(1)={xapp};


Surfs=xtgetsporfaces(xapp,'Surfaces');
List={Surfs.Name};
List={'Choose Surface', List{:}};

Surf_popup=findobj('Tag','Surf_popup');
Surf_popup.String=List;

handles.data=AllData;
guidata(hObject,handles)

Status.String={'Choose a Surface from List'};








% UIWAIT makes Compensation_SmallData wait for user response (see UIRESUME)
% uiwait(handles.figure1);




% --- Outputs from this function are returned to the command line.
function varargout = Compensation_SmallData_OutputFcn(hObject, eventdata, handles) 
varargout{1} = handles.output;


% --- Executes on button press in Start_button.
function Start_button_Callback(hObject, eventdata, handles)
% Get connected 
Status=findobj('Tag','Status_text');
Status.String={'Connecting'};
drawnow
xapp=xtconnectimaris(handles.data);
DataSet=xapp.GetDataSet;
AllData(1)={xapp};
xapp.GetCurrentFileName

Surfs=xtgetsporfaces(xapp,'Surfaces');
List={Surfs.Name};
List={'Choose Surface', List{:}};

Surf_popup=findobj('Tag','Surf_popup');
Surf_popup.String=List;

handles.data=AllData;
guidata(hObject,handles)

Status.String={'Choose a Surface from List'};






% --- Executes on selection change in XChannel_popup.
function XChannel_popup_Callback(hObject, eventdata, handles)
YChannel_popup=findobj('Tag', 'YChannel_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
axes1=findobj('Tag','axes1');
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

if  length(cellstr(hObject.String))<3 | YChannel_popup.Value==1 | XChannel_popup.Value==1

    %Select More
else 
    %Plot intensities
    RawData=handles.data(2);
    Xvals=RawData{1}{3,XChannel_popup.Value-1};
    Yvals=RawData{1}{3,YChannel_popup.Value-1};
    plot(handles.axes1, Xvals, Yvals,'.k')
    axis(handles.axes1,[xmin xmax ymin ymax])
    drawnow;
end
    




% --- Executes during object creation, after setting all properties.
function XChannel_popup_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes on selection change in YChannel_popup.
function YChannel_popup_Callback(hObject, eventdata, handles)

YChannel_popup=findobj('Tag', 'YChannel_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
axes1=findobj('Tag','axes1');
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

if  length(cellstr(hObject.String))<3 | YChannel_popup.Value==1 | XChannel_popup.Value==1

    %Select More
else 
    %Plot intensities
    RawData=handles.data(2);
    Xvals=RawData{1}{3,XChannel_popup.Value-1};
    Yvals=RawData{1}{3,YChannel_popup.Value-1};
    plot(handles.axes1, Xvals, Yvals,'.k')
    axis(handles.axes1,[xmin xmax ymin ymax])
    drawnow;
end
    



% --- Executes during object creation, after setting all properties.
function YChannel_popup_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes on button press in Finish_button.
function Finish_button_Callback(hObject, eventdata, handles)
Status=findobj('Tag','Status_text');
Status.String={'Reading Data'};
Status.BackgroundColor=[1 .6 .784];
drawnow

editTR=findobj('Tag','editTR');
editBL=findobj('Tag','editBL');
TR=str2num(editTR.String);
BL=str2num(editBL.String);
Trans=[1 TR; BL 1];
ITrans=inv(Trans);

YChannel_popup=findobj('Tag', 'YChannel_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
Chs=[XChannel_popup.Value-2 YChannel_popup.Value-2];

% Get connected 
AllData=handles.data;
xapp=AllData{1};
fname=char(xapp.GetCurrentFileName);
fileObj=ImarisReader(fname);
DataSet=fileObj.DataSet;
%DataSet=xapp.GetDataSet;
Instance=xapp.GetDataSet;

imData=zeros(DataSet.SizeX,DataSet.SizeY,DataSet.SizeZ,2,DataSet.SizeT,'uint8');
Comp=cell(DataSet.SizeZ,DataSet.SizeT);
Ch1=cell(DataSet.SizeZ,DataSet.SizeT);
Ch2=cell(DataSet.SizeZ,DataSet.SizeT);
disp('get imData')
for t=1:DataSet.SizeT
    for c=1:2
        imData(:,:,:,c,t)=DataSet.GetDataVolume(Chs(c),t-1);  %%call from file
    end% for c
end %fot t

disp('channel math')

for t=1:DataSet.SizeT
    for z=1:DataSet.SizeZ
        Ch1{z,t}=imData(:,:,z,1,t);
        Ch2{z,t}=imData(:,:,z,2,t);
        Combined=[Ch1{z,t}(:),Ch2{z,t}(:)];
        Comp{z,t}=double(Combined)*ITrans;
    end
end
disp('finished math')

Status.String={'Writing Data'};
drawnow

Type=Instance.GetType;
if ~(Type==Imaris.tType.eTypeFloat)
    Instance.SetType(Imaris.tType.eTypeFloat);
    xapp.SetDataSet(Instance)
end
disp('prep imaris file')

Instance.SetSizeC(Instance.GetSizeC+2);
XName=char(Instance.GetChannelName(XChannel_popup.Value-2));
YName=char(Instance.GetChannelName(YChannel_popup.Value-2));
XNameNew=[XName ' Compensated'];
YNameNew=[YName ' Compensated'];

Instance.SetChannelName(Instance.GetSizeC-2,XNameNew)
Instance.SetChannelColorRGBA(Instance.GetSizeC-2,Instance.GetChannelColorRGBA(XChannel_popup.Value-2));
Instance.SetChannelName(Instance.GetSizeC-1,YNameNew)
Instance.SetChannelColorRGBA(Instance.GetSizeC-1,Instance.GetChannelColorRGBA(YChannel_popup.Value-2));

disp('begin writing data')

for t=1:Instance.GetSizeT
    disp(['time: ' num2str(t)])
    for z=1:Instance.GetSizeZ
        Instance.SetDataSubVolumeAs1DArrayFloats(Comp{z,t}(:,1),...
            0,0,z-1,Instance.GetSizeC-2, t-1, ...
            Instance.GetSizeX,Instance.GetSizeY,1);
        Instance.SetDataSubVolumeAs1DArrayFloats(Comp{z,t}(:,2),...
            0,0,z-1,Instance.GetSizeC-1, t-1, ...
            Instance.GetSizeX,Instance.GetSizeY,1);
        disp(['Z slice: ' num2str(z)])
        drawnow
    end
    %     DataSet.SetDataVolumeAs1DArrayFloats(Comp{t}(:,1),DataSet.GetSizeC-2, t-1);
    %     DataSet.SetDataVolumeAs1DArrayFloats(Comp{t}(:,2), DataSet.GetSizeC-1, t-1);
end

Status.String={'Done'};
Status.BackgroundColor=[.757 .867 .776];
drawnow


function editTR_Callback(hObject, eventdata, handles)
editTR=findobj('Tag','editTR');
editBL=findobj('Tag','editBL');
YChannel_popup=findobj('Tag', 'YChannel_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
axes1=findobj('Tag','axes1');
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');


TR=str2num(editTR.String);
BL=str2num(editBL.String);
RawData=handles.data(2);
Xvals=RawData{1}{3,XChannel_popup.Value-1};
Yvals=RawData{1}{3,YChannel_popup.Value-1};
Combined=[Xvals(:), Yvals(:)];


Trans=[1 TR; BL 1];
ITrans=inv(Trans);
Comp=double(Combined)*ITrans;

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

plot(handles.axes1, Comp(:,1), Comp(:,2),'.k');
axis(handles.axes1,[xmin xmax ymin ymax]);
drawnow;


% --- Executes during object creation, after setting all properties.
function editTR_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function editBL_Callback(hObject, eventdata, handles)
editTR=findobj('Tag','editTR');
editBL=findobj('Tag','editBL');
YChannel_popup=findobj('Tag', 'YChannel_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
axes1=findobj('Tag','axes1');
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');


TR=str2num(editTR.String);
BL=str2num(editBL.String);
RawData=handles.data(2);
Xvals=RawData{1}{3,XChannel_popup.Value-1};
Yvals=RawData{1}{3,YChannel_popup.Value-1};
Combined=[Xvals(:), Yvals(:)];


Trans=[1 TR; BL 1];
ITrans=inv(Trans);
Comp=double(Combined)*ITrans;

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

plot(handles.axes1, Comp(:,1), Comp(:,2),'.k');
axis(handles.axes1,[xmin xmax ymin ymax]);
drawnow;


% --- Executes during object creation, after setting all properties.
function editBL_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes during object creation, after setting all properties.
function axes1_CreateFcn(hObject, eventdata, handles)
axis(hObject,[0 256 0 256]);


function xmin_edit_Callback(hObject, eventdata, handles)
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
axes1=findobj('Tag','axes1');

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

axis(handles.axes1,[xmin xmax ymin ymax])
drawnow;


% --- Executes during object creation, after setting all properties.
function xmin_edit_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function xmax_edit_Callback(hObject, eventdata, handles)
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
axes1=findobj('Tag','axes1');

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

axis(handles.axes1,[xmin xmax ymin ymax])
drawnow;


% --- Executes during object creation, after setting all properties.
function xmax_edit_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function ymin_edit_Callback(hObject, eventdata, handles)
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
axes1=findobj('Tag','axes1');

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

axis(handles.axes1,[xmin xmax ymin ymax])
drawnow;

% --- Executes during object creation, after setting all properties.
function ymin_edit_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function ymax_edit_Callback(hObject, eventdata, handles)
xmin_edit=findobj('Tag','xmin_edit');
xmax_edit=findobj('Tag','xmax_edit');
ymin_edit=findobj('Tag','ymin_edit');
ymax_edit=findobj('Tag','ymax_edit');
axes1=findobj('Tag','axes1');

xmin=str2num(xmin_edit.String);
xmax=str2num(xmax_edit.String);
ymin=str2num(ymin_edit.String);
ymax=str2num(ymax_edit.String);

axis(handles.axes1,[xmin xmax ymin ymax])
drawnow;



% --- Executes during object creation, after setting all properties.
function ymax_edit_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes on selection change in Surf_popup.
function Surf_popup_Callback(hObject, eventdata, handles)
AllData=handles.data;
xapp=AllData{1};
DataSet=xapp.GetDataSet;

Surf=xtgetsporfaces(xapp,'Surfaces');
if hObject.Value>1
    SPick=Surf(hObject.Value-1).ImarisObject;
end

aSurface=xapp.GetFactory.ToSurfaces(SPick);
surfNum=aSurface.GetNumberOfSurfaces;

%% Allocate Space

Ch1=cell(1,DataSet.GetSizeT);
Ch2=cell(1,DataSet.GetSizeT);
Medians=zeros(surfNum, DataSet.GetSizeC);

%% Get Surface Statistics   
Status=findobj('Tag','Status_text');
Status.String={'Getting Surface Data'};
Status.BackgroundColor=[1 .6 .784];
drawnow
statStruct=xtgetstats(xapp, aSurface, 'Singlet');
for i=1:length(statStruct)
    Mask(i)=~isempty(regexp(statStruct(i).Name,'Intensity Mean'));
end
IntensityMeans=statStruct(Mask);
IntensityMeans=struct2cell(IntensityMeans');
AllData(2)={IntensityMeans};

handles.data=AllData;
guidata(hObject,handles);
    
%% fill in X and Y pop ups
clear Names
Names=cell(1,DataSet.GetSizeC);

for i=1:DataSet.GetSizeC
    Names{i}=char(DataSet.GetChannelName(i-1));
end

XChannel_popup=findobj('Tag', 'XChannel_popup');
YChannel_popup=findobj('Tag', 'YChannel_popup');
 
XChannel_popup.String=[{'X Channel'}, Names];
YChannel_popup.String=[{'Y Channel'}, Names];
Status.String={'OK'};
Status.BackgroundColor=[.94 .94 .94];
drawnow

% --- Executes during object creation, after setting all properties.
function Surf_popup_CreateFcn(hObject, eventdata, handles)
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes on button press in Export_button.
function Export_button_Callback(hObject, eventdata, handles)
editTR=findobj('Tag','editTR');
editBL=findobj('Tag','editBL');
TR=str2num(editTR.String);
BL=str2num(editBL.String);
Surf_popup=findobj('Tag','Surf_popup');
XChannel_popup=findobj('Tag', 'XChannel_popup');
YChannel_popup=findobj('Tag', 'YChannel_popup');


Formatted=cell(6,3);
Formatted{1,1}=[cat(2,'Compensation Matrix for ',Surf_popup.String{Surf_popup.Value})];
Formatted{2,1}=['Row Header bleeds into column header by given fraction'];
Formatted{4,2}=XChannel_popup.String{XChannel_popup.Value};
Formatted{5,1}=XChannel_popup.String{XChannel_popup.Value};
Formatted{4,3}=YChannel_popup.String{YChannel_popup.Value};
Formatted{6,1}=YChannel_popup.String{YChannel_popup.Value};
Formatted{5,2}=1;
Formatted{6,3}=1;
Formatted{5,3}=TR;
Formatted{6,2}=BL;

[file, path]=uiputfile('.xls','Save Current Table');
full=fullfile(path, file);
xlswrite(full,Formatted);
