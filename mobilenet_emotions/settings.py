import numpy as np

img_size = 128
dim = (img_size, img_size, 3)
random_state = 54
seed = 54
weights = 'imagenet'
user = '/home/volodymyr/AhegaoProject'
abs_path = '/home/volodymyr/'
path_to_data = '/home/volodymyr/AhegaoProject/data.csv'
img_path = '...'
classweights = True
space_params_fit_gen = [
    (0.01, 1),  # damp param
    (50, 100),  # epochs
    np.arange(32, 96, 8),  # batch_train
    np.arange(8, 40, 8),  # batch_valid
    (0.1, 0.8),  # dropout
    (0.1, 0.8),  # dropout_global
    (1e-5, 1e-3),  # eta
    [None, 5, 10, 15],  # layers to freeze
    (1e-3, 1),  # l2
    np.arange(1, 4, 1)  # layers to add

]
tune_lr = True
ncalls = 10
defined_params = []
