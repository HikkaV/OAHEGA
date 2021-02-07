import os
with open('train.txt','w') as f:
     for i in os.listdir('/home/hikkav/Yolo_mark/x64/Release/data/img'):
         if i.split('.')[1]!='txt':
            f.write('/home/hikkav/Yolo_mark/x64/Release/data/img/'+i)
            f.write('\n')
f.close()