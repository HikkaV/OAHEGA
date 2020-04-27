from keras import Model, Input
from keras.applications import MobileNet
from keras.layers import Dropout, Dense, GlobalAveragePooling2D, Activation, SeparableConv2D
from keras.optimizers import Adam, SGD
from keras.regularizers import l2
from settings import *


class DeepMN:

    def __init__(self, classes=None, weights='imagenet', dropout_global=1e-3,
                 dropout=0.6, activation='relu', eta=0.001,
                 amsgrad=False, train_mode=False, l2_=0, layer_params=1,
                 trainable=None):
        self.regulizer = l2_
        self.train_mode = train_mode
        self.classes = len(classes)
        self.w = weights
        self.dropout_global = dropout_global
        self.dropout = dropout
        self.activation = activation
        self.eta = eta
        self.amsgrad = amsgrad
        self.trainable = trainable
        self.layer_params = layer_params

    def create_model(self):
        from helper import precision, recall, f1_score
        input = Input(shape=(dim[0], dim[1], 3), name='input')

        model = MobileNet(input_shape=dim, alpha=1, depth_multiplier=1, dropout=self.dropout_global,
                          include_top=False, weights=self.w, input_tensor=None)
        if self.trainable:
            for i in model.layers[:len(model.layers) - self.trainable]:
                i.trainable = False
            x = model.output
            input = model.input
            optimizer = SGD(learning_rate=self.eta)
        else:
            optimizer = Adam(lr=self.eta, amsgrad=self.amsgrad)
            x = model(input)

        x = GlobalAveragePooling2D()(x)
        nodes = 2048
        x = Dense(nodes, activation='relu', kernel_regularizer=l2(self.regulizer))(x)
        x = Dropout(self.dropout)(x)
        for i in range(self.layer_params):
            nodes = int(nodes / 2)
            x = Dense(nodes, activation='relu', kernel_regularizer=l2(self.regulizer))(x)
            x = Dropout(self.dropout)(x)
        z = Dense(units=self.classes, activation='softmax')(x)
        model = Model(inputs=input, outputs=z)
        if not self.train_mode:
            model.compile(optimizer=optimizer, loss='categorical_crossentropy', metrics=['accuracy',
                                                                                    precision, recall, f1_score])

        else:
            model.compile(optimizer=optimizer, loss='categorical_crossentropy', metrics=['accuracy'])

        print(model.summary())

        return model, optimizer
