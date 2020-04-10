/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.oahega.com.customview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import org.oahega.com.utils.Settings;

/** A {@link TextureView} that can be adjusted to a specified aspect ratio. */
public class AutoFitTextureView extends TextureView {
  private int ratioWidth = 0;
  private int ratioHeight = 0;

  public AutoFitTextureView(final Context context) {
    this(context, null);
  }

  public AutoFitTextureView(final Context context, final AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AutoFitTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
   * calculated from the parameters. Note that the actual sizes of parameters don't matter, that is,
   * calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
   *
   * @param width Relative horizontal size
   * @param height Relative vertical size
   */
  public void setAspectRatio(final int width, final int height) {
    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Size cannot be negative.");
    }
    ratioWidth = width;
    ratioHeight = height;
    requestLayout();
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    final int width = MeasureSpec.getSize(widthMeasureSpec);
    final int height = MeasureSpec.getSize(heightMeasureSpec);
    if (0 == ratioWidth || 0 == ratioHeight) {
      Log.d("+++1", "ratioWidth: " + ratioWidth);
      Log.d("+++1", "ratioHeight: " + ratioHeight);
      Log.d("+++1", "width: " + width);
      Log.d("+++1", "Height: " + height);
      Log.d("+++1", "ratio : 1.0");
      Log.d("+++1", "width: " + width + " height: " + height);
      Settings.getInstance().setHeight(height);
      Settings.getInstance().setWidth(width);
      Settings.getInstance().setRatio(1.0);
      setMeasuredDimension(width, height);
    } else {
      Log.d("+++", "===========");
      Log.d("+++", "if (width > height * ratioWidth / ratioHeight) {");
      Log.d("+++2", "width: " + width + " height: " + width * ratioHeight / ratioWidth);
      Log.d("+++", "else");
      Log.d("+++3", "width: " + height * ratioWidth / ratioHeight + " height: " + height);
      Log.d("+++", "===========");
      if (width > height * ratioWidth / ratioHeight) {
        Log.d("+++2", "ratioWidth: " + ratioWidth);
        Log.d("+++2", "ratioHeight: " + ratioHeight);
        Log.d("+++2", "width: " + width);
        Log.d("+++2", "Height: " + height);
        Log.d("+++2", "ration: " + ((double) width * ratioHeight / ratioWidth) / height);
        Log.d("+++2", "width: " + width + " height: " + width * ratioHeight / ratioWidth);
        Settings.getInstance()
            .setWidth((double) ((double) width * ratioHeight / ratioWidth) / height);
        Settings.getInstance().setHeight(height / (double) width);
        Settings.getInstance().setRatio(width / (double) height);
        setMeasuredDimension(width, width * ratioHeight / ratioWidth);
      } else {
        Log.d("+++3", "ratioWidth: " + ratioWidth);
        Log.d("+++3", "ratioHeight: " + ratioHeight);
        Log.d("+++3", "width: " + width);
        Log.d("+++3", "Height: " + height);
        Log.d("+++3", "ratio: " + (double) (height * ratioWidth / ratioHeight) / width);
        Log.d("+++3", "width: " + height * ratioWidth / ratioHeight + " height: " + height);

        Settings.getInstance().setWidth((double) (height * ratioWidth / ratioHeight) / width);
        Settings.getInstance().setHeight(height / (double) width);

        Settings.getInstance().setRatio(height / (double) width);
        setMeasuredDimension(height * ratioWidth / ratioHeight, height);
      }
    }
  }
}
