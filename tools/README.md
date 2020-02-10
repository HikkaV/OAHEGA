***A tool for visualizing images in dataset***
* Make virtualenv and install requirements.
* Install imagemagic if not installed and use it to create a sprite of dataset :
 <pre> montage images/img_*.jpg -tile 50x50 -geometry 50x50! sprite.jpg</pre>
* Change settings.py and use visualize dataset.py script to create vectors.
* Run tensorboard in the same dir : 
<pre>tensorboard --logdir .</pre>