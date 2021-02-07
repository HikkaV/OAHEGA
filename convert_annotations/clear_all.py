import os
import shutil
path = '/home/hikkav/Yolo_mark/x64/Release/data/img'
for i in os.listdir(path):

        if i.split('.')[1]=='txt':
            os.remove(path+'/'+i)