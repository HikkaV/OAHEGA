import csv
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
import os
import tensorflow_hub as hub
from settings import abs_path,model
module = hub.Module(model)
height, width = hub.get_expected_image_size(module)
filename = tf.placeholder(tf.string)
image_bytes = tf.read_file(filename)
image = tf.image.decode_image(image_bytes, channels=3)
image = tf.image.resize_bilinear([image], [height, width])
features = module(image)
images_list = os.listdir(abs_path)
with open('feature_vecs.tsv', 'w') as fw:
     csv_writer = csv.writer(fw, delimiter='\t')
     with tf.Session() as sess:
         sess.run(tf.global_variables_initializer())

         for image_path in images_list:
             fvecs = sess.run(features, feed_dict={filename: os.path.join(abs_path,image_path)})
             csv_writer.writerows(fvecs)