import tensorflow as tf
import keras.preprocessing.image as im_proc
import numpy as np
import argparse
import pandas as pd
from sklearn.metrics import accuracy_score, precision_score
from tqdm import tqdm
model = 'emotions.tflite'
tflite_interpreter = tf.lite.Interpreter(model_path=model)
input_details = tflite_interpreter.get_input_details()
output_details = tflite_interpreter.get_output_details()
tflite_interpreter.allocate_tensors()
classes_emo = {0: 'Ahegao', 1: 'Angry', 2: 'Happy', 3: 'Neutral', 4: 'Sad', 5: 'Surprise'}


def inference_img(img):

    im = im_proc.img_to_array(im_proc.load_img(img, target_size=(128, 128))) / 255
    im = np.expand_dims(im, 0)

    tflite_interpreter.set_tensor(input_details[0]['index'], im)
    tflite_interpreter.invoke()
    output_data = tflite_interpreter.get_tensor(output_details[0]['index'])
    print(classes_emo.get(np.argmax(output_data[0][:6])))

def alldata_inference(path,abs_pref):
    df = pd.read_csv(path)
    df['path'] = abs_pref + df['path']
    predicted = []
    ground_truth = df['label'].values
    for im in tqdm(df['path'].values):
        im = im_proc.img_to_array(im_proc.load_img(im, target_size=(128, 128))) / 255
        im = np.expand_dims(im, 0)
        tflite_interpreter.set_tensor(input_details[0]['index'], im)
        tflite_interpreter.invoke()
        predicted.append(classes_emo.get(np.argmax(tflite_interpreter.get_tensor(output_details[0]['index'])[0][:6])))
    print('Accuracy : {0}'.format(accuracy_score(y_true=ground_truth,y_pred=predicted)))
    print('Precision : {0}'.format(precision_score(y_pred=predicted,y_true=ground_truth, average='micro')))

if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--img', help='Path to image to recognize', type=str, default=None)
    ap.add_argument('--dataframe', help='Path to csv file with pathes to pictures and labels',
                    type=str, default=None)
    ap.add_argument('--abs_path', help='Absolute prefix for path to folder with pictures',default=None,
                    type=str)
    args = ap.parse_args()
    if args.img and not args.dataframe :
        inference_img(args.img)
    elif args.dataframe and args.abs_path and not args.img :
        alldata_inference(args.dataframe, args.abs_path)
    elif args.dataframe and not args.abs_path or args.abs_path and not args.dataframe :
        raise Exception('When --dataframe is specified, --abs_path should also be specified and vice versa')
    else:
        raise Exception('You should specify at least one parameter')
