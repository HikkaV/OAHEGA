# *******************************************************************
# Face detection using the YOLOv2-tiny algorithm + age and gender recognition
#
# Description : annotate_darknet.py
# The main code of the Face detection using the YOLOv3 algorithm
#
# *******************************************************************

# Usage example:  python annotate_darknet.py --image samples/outside_000001.jpg \
#                                    --output-dir outputs/
#                 python annotate_darknet.py --video samples/subway.mp4 \
#                                    --output-dir outputs/
#                 python annotate_darknet.py --src 1 --output-dir outputs/


import argparse
import random
import sys
import os

from utils import *
import numpy as np

#####################################################################
parser = argparse.ArgumentParser()

parser.add_argument('--model-cfg', type=str, default='/home/hikkav/ABdarknet/darknet/cfg/yolov3.cfg',
                    help='path to config file')
parser.add_argument('--model-weights', type=str,
                    default='/home/hikkav/Загрузки/yolov3.weights',
                    help='path to weights of model')

parser.add_argument('--video_dir', type=str, default=None,
                    help='path to video file')
parser.add_argument('--src', type=int, default=0,
                    help='source of the camera')
parser.add_argument('--skip', type=int, default=1,
                    help='how many frames to skip')
parser.add_argument('--image_dir', type=str, default='/home/hikkav/Yolo_mark/x64/Release/data/img',
                    help='path to image directory ')
args = parser.parse_args()

#####################################################################


# print the arguments
print('----- info -----')
print('[i] The config file: ', args.model_cfg)
print('[i] The weights of model file: ', args.model_weights)

print('###########################################################\n')

# Give the configuration and weight files for the model and load the network
# using them.


net = cv2.dnn.readNetFromDarknet(args.model_cfg, args.model_weights)

net.setPreferableBackend(cv2.dnn.DNN_BACKEND_OPENCV)
net.setPreferableTarget(cv2.dnn.DNN_TARGET_CPU)
counter=0



#######################################################################
# my part of code


def _convert(img_size, i):
    dw = 1. / img_size[0]
    dh = 1. / img_size[1]
    cx = i[0]
    cy=i[1]
    x = cx * dw
    w = i[2] * dw
    y = cy * dh
    h = i[3] * dh
    return max(0.005, x), max(0.005, y), min(0.995, w), min(0.995, h)

def make_inputs(faces, path, w, h):


    global counter
    shape=(w,h)
    f = open(path.split('.')[0]+'.txt', 'a')
    for i in range(len(faces)):

        x,y,w_,h_=_convert(shape,faces[i])
        if not w_+h_<0.065:
           box_info = " {} {} {} {} {}".format(1, x, y, w_,
                                                h_)

           if os.path.getsize(path.split('.')[0] + '.txt') > 0:
              f.write('\n'+box_info+'\n')
           else:
              f.write(box_info + '\n')
    counter+=1
    print('Done with {}'.format(counter))
    f.close()


####################################################################
def _main():

    wind_name = 'cropping faces using yolov3'
    video_list = []

    if args.video_dir is not None:

        if not os.path.exists(args.video_dir):
            print('The path {} does not exist '.format(args.video))
            sys.exit(1)
        for i in os.listdir(args.video_dir):
            if not os.path.isfile(args.video_dir + '/' + i):
                print("[!] ==> Input video file {} doesn't exist".format(args.video))
                sys.exit(1)
            video_list.append(args.video_dir + '/' + i)
    elif args.image_dir is not None:
        for i in os.listdir(args.image_dir):
           if not i.split('.')[1]=='txt':
               frame = cv2.imread(args.image_dir + '/' + i)
               img = Image.open(args.image_dir + '/' + i)
               width, height = img.size
               blob = cv2.dnn.blobFromImage(frame, 1 / 255, (IMG_WIDTH, IMG_HEIGHT),
                                            [0, 0, 0], 1, crop=False)
               net.setInput(blob)

               # Runs the forward pass to get output of the output layers

               outs = net.forward(get_outputs_names(net))

               # Remove the bounding boxes with low confidence
               people = post_process(frame, outs, CONF_THRESHOLD, NMS_THRESHOLD
                                                   )
               make_inputs(people, args.image_dir + '/' + i, width, height)


        print('Done')
        sys.exit(1)

    else:
        video_list.append(1)

    # Get the video writer initialized to save the output video

    cv2.namedWindow(wind_name, cv2.WINDOW_NORMAL)
    for i in video_list:

        if i != 1 :
            cap = cv2.VideoCapture(i)

        else:

            cap = cv2.VideoCapture(args.src)

        n = 0
        while True:

            has_frame, frame = cap.read()

            # Stop the program if reached end of video
            if not has_frame:

                print('[i] ==> Done processing!!!')
                cv2.waitKey(1000)
                break
            if args.skip != n:
                n += 1
                continue
            n = 0

            # Create a 4D blob from a frame.

            blob = cv2.dnn.blobFromImage(frame, 1 / 255, (IMG_WIDTH, IMG_HEIGHT),
                                         [0, 0, 0], 1, crop=False)

            # Sets the input to the network
            net.setInput(blob)

            # Runs the forward pass to get output of the output layers

            outs = net.forward(get_outputs_names(net))

            # Remove the bounding boxes with low confidence
            people, ids, indices = post_process(frame, outs, CONF_THRESHOLD, NMS_THRESHOLD
                                                )



            print('[i] ==> # detected  objects: {}'.format(len(people)))
            print('#' * 60)

            # initialize the set of information we'll displaying on the frame
            info = [
                ('number of objects detected', '{}'.format(len(people)))
            ]

            for (i, (txt, val)) in enumerate(info):
                text = '{}: {}'.format(txt, val)
                cv2.putText(frame, text, (10, (i * 20) + 20),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, COLOR_RED, 2)

            cv2.imshow(wind_name, frame)

            key = cv2.waitKey(1)
            if key == 27 or key == ord('q'):
                print('[i] ==> Interrupted by user!')
                break

        cap.release()
        cv2.destroyAllWindows()

        print('==> All done!')
        print('***********************************************************')


if __name__ == '__main__':
    _main()
