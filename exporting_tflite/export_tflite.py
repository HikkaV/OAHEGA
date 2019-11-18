import tensorflow as tf
import sys
import argparse

if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--type', default='age_gender', type=str, help='Type of model to convert')
    args = ap.parse_args()
    if args.type == 'age_gender':

        input_arrays = ["the_input"]
        output_arrays = ["output_node0", "output_node1"]
        converter = tf.compat.v1.lite.TFLiteConverter.from_frozen_graph('/home/volodymyr/AhegaoProject/exporting_tflite'
                                                                        '/tensorflow_model/age_gender.pb',
                                                                        input_arrays=input_arrays,
                                                                        output_arrays=output_arrays)
        converter.optimizations = [tf.compat.v1.lite.Optimize.DEFAULT]
        tfmodel = converter.convert()
        open("age_gender.tflite", "wb").write(tfmodel)
    else:

        model = tf.keras.models.load_model('/home/volodymyr/AhegaoProject/ahegao_tracker/distilled_model.h5',
                                           compile=False)
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        tflite_model = converter.convert()
        open("emotions.tflite", "wb").write(tflite_model)
