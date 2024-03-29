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

package org.oahega.com;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.oahega.com.customview.OverlayView;
import org.oahega.com.preference.Preference;
import org.oahega.com.tflite.Recognition;
import org.oahega.com.tflite.detection.Classifier;
import org.oahega.com.tflite.detection.TFLiteObjectDetectionAPIModel;
import org.oahega.com.tracking.MultiBoxTracker;
import org.oahega.com.utils.Settings;
import org.oahega.com.env.BorderedText;
import org.oahega.com.env.ImageUtils;
import org.oahega.com.env.Logger;
import org.oahega.com.utils.DataHelper;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

    private static final Logger LOGGER = new Logger("DetectorActivity");
    private static final String TAG = "DetectorActivity";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "face_detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/detection_lables.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 320);
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;
    long startTime;
    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private org.oahega.com.tflite.clasification.Classifier classifier;

    private boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void doTest() {
        int pass = 0;
        int fail = 0;
        long beginTime = System.currentTimeMillis();
        File file = new File("SD-карта/oahega photo/1");
        if (file != null) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < file.listFiles().length; i += 1) {
                File image = file.listFiles()[i];
                try {
                    Bitmap bitmap =
                            BitmapFactory.decodeStream(new FileInputStream(image.toString()));
                    ArrayList<Recognition> recognitions = classifier.recognizeImage(bitmap, 0);
                    Recognition r = getRec(recognitions);
                    String input = image.getName().split("_")[0];
                    stringBuilder.append(image.getName().toString());
                    stringBuilder.append(":");
                    stringBuilder.append(r.getTitle());
                    stringBuilder.append(":");
                    stringBuilder.append(r.getConfidence() * 100);
                    stringBuilder.append("\n");
//                    Log.d("===", "\n input: " + input + "\t output: " + r.getTitle() + " \tresult: " + input.equals(r.getTitle()));
//                    for (Recognition recognition : recognitions) {
//                        Log.d("===", recognition.getTitle() + " : " + recognition.getConfidence() * 100);
//                    }
//                    for (Recognition recognition : recognitions) {
//                        Log.d("===", recognition.getTitle() + " : " + recognition.getConfidence() * 100);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            long time = System.currentTimeMillis() - beginTime;
            stringBuilder.append("\n");
            stringBuilder.append("pass=").append(pass).append("\n");
            stringBuilder.append("fail=").append(fail).append("\n");
            stringBuilder.append("time= ").append(time);
            String stringResult = stringBuilder.toString();
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("SD-карта/oahega photo/result.txt")));
                writer.write(stringBuilder.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {
                    ex.printStackTrace();/*ignore*/
                }
            }
        } else {
//            Log.d("===", "file == null");
        }
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        LOGGER.d("onPreviewSizeChosen()");
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
            LOGGER.d("detector created");
        } catch (final IOException e) {
            LOGGER.d("detector is not created");
            e.printStackTrace();
            LOGGER.d("Exception initializing classifier!" + e.getMessage());
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        try {
            classifier = org.oahega.com.tflite.clasification.Classifier
                .create(this, org.oahega.com.tflite.clasification.Classifier.Model.QUANTIZED,
                    org.oahega.com.tflite.clasification.Classifier.Device.CPU, 4);
            LOGGER.d("classifier created");
        } catch (IOException e) {
            LOGGER.d("classifier is not created");
            e.printStackTrace();
        }
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.d("Camera orientation relative to screen canvas: " + sensorOrientation);

        LOGGER.d("Initializing at size " + previewWidth + "х" + previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(Canvas canvas) {
                        tracker.draw(canvas);
                        if (DetectorActivity.this.isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    private Recognition getRec(List<Recognition> clasifResult) {
        Recognition rec = clasifResult.get(0);

        for (Recognition res : clasifResult) {
            if (res.getConfidence() > rec.getConfidence()) {
                rec = res;
            }
        }
        return rec;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
      return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    private List<Recognition> detectFaces(Bitmap bitmap) { // return list of faces
        return detector.recognizeImage(bitmap);

    }

    @Override
    public void startProcess() {
//        if (firstRun) {
//            doTest();
//            firstRun = false;
//        }
        LOGGER.d("startProcess()");
        startTime = SystemClock.uptimeMillis();
        ArrayList<Bitmap> inputBitmaps = DataHelper.getInstance().getListOne();
        ArrayList<Recognition> outputs = new ArrayList<>();
        ArrayList<ArrayList<Recognition>> resultsForAvarageOnManyBitmaps = new ArrayList<>();
        for (Bitmap bitmap : inputBitmaps) {
            List<Recognition> detects = detectFaces(cropBitmapDetection(bitmap));
            ArrayList<Recognition> resultClassiofOnOneBitmap = new ArrayList<>();
            for (Recognition detect : detects) {
                if (detect.getConfidence() * 100 > Preference.getInstance().getMinPercentDetect()) {
                    if ("face".equals(detect.getTitle())) {
                        Matrix matrix = new Matrix();

                        matrix.postRotate(sensorOrientation);

                        Bitmap imageForClassif = cropBitmapClassification(detect, bitmap);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(imageForClassif, 0, 0, imageForClassif.getWidth(), imageForClassif.getHeight(), matrix, true);

                        List<Recognition> classifs = classifier.recognizeImage(imageForClassif, 0);
                        Recognition bestClassif = getRec(classifs);
                        bestClassif.setLocation(detect.getLocation());
//                        Log.d("===", "new 2" + bestClassif.getTitle() + " : " + getRec(classifs).getConfidence() * 100);
                        if (bestClassif.getConfidence() * 100 > Preference.getInstance()
                            .getMinPercentClassif()) {
                            resultClassiofOnOneBitmap.add(bestClassif);
                        }
                    } else {
//                        Log.d("===", "new person " + detect.getConfidence() * 100);
                        outputs.add(detect);
                    }
                }
            }
            resultsForAvarageOnManyBitmaps.add(resultClassiofOnOneBitmap);
        }
//        Log.d("===", "detects and classifs ended");
        int minDetectsOnMitmap = resultsForAvarageOnManyBitmaps.get(0).size();

        for (int i = 0; i < resultsForAvarageOnManyBitmaps.size(); i++) {
            if (resultsForAvarageOnManyBitmaps.get(i).size() < minDetectsOnMitmap) {
                minDetectsOnMitmap = resultsForAvarageOnManyBitmaps.get(i).size();
            }
        }
        for (int detectIndex = 0; detectIndex < minDetectsOnMitmap; detectIndex++) {
            ArrayList<Recognition> results = new ArrayList<>();
            for (int bitmapIndex = 0; bitmapIndex < resultsForAvarageOnManyBitmaps.size(); bitmapIndex++) {
                results.add(resultsForAvarageOnManyBitmaps.get(bitmapIndex).get(detectIndex));
            }
//            Log.d("===", "new Face " + getRec(results).getConfidence() * 100);
            outputs.add(getRec(results));
        }

      if (!useCamera2API && Settings.getInstance().isIsfront()) {
        for (Recognition output : outputs) {
          changeOutput(output);
        }
      }
      for (Recognition recognition : outputs) {
        recognition.doubleValueWeight(Settings.getInstance().getWidth());
        recognition.doubleValueHeight(Settings.getInstance().getWidth());
      }

        showResults(outputs, inputBitmaps.get(0));
    }

    public static Point getScreenSize() {
        int pxWidth;
        int pxHeight;
        DisplayMetrics outMetrics = Resources.getSystem().getDisplayMetrics();
        if (outMetrics.widthPixels < outMetrics.heightPixels
            || outMetrics.widthPixels > outMetrics.heightPixels) {
            pxWidth = outMetrics.widthPixels;
            pxHeight = outMetrics.heightPixels;
        } else {
            pxWidth = outMetrics.heightPixels;
            pxHeight = outMetrics.widthPixels;
        }
        return new Point(pxWidth, pxHeight);
    }

    private void changeOutput(Recognition recognition) {
        RectF inputLocation = recognition.getLocation();
        RectF outputLocation = new RectF();
        outputLocation.top = TF_OD_API_INPUT_SIZE - inputLocation.bottom;
        outputLocation.bottom = TF_OD_API_INPUT_SIZE - inputLocation.top;
        outputLocation.left = TF_OD_API_INPUT_SIZE - inputLocation.right;
        outputLocation.right = TF_OD_API_INPUT_SIZE - inputLocation.left;
        recognition.setLocation(outputLocation);
    }

    private void showResults(ArrayList<Recognition> results, Bitmap bitmap) {

//        Log.d("===", "showResults");
//        for (Recognition recognition : results) {
//            Log.d("===", "title " + recognition.getTitle() + " coeficient: " + recognition.getConfidence());
//        }
        bitmap = Bitmap.createBitmap(bitmap);
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Recognition> mappedRecognitions =
                new LinkedList<>();

        for (final Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }

        tracker.trackResults(mappedRecognitions, System.currentTimeMillis());
        trackingOverlay.postInvalidate();

        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        readyForNextImage();
        isProcessingFrame = false;
        DataHelper.getInstance().getListOne().clear();
//        Log.d("===", "end procees");
    }

    private ArrayList<Recognition> getResultsForShow(ArrayList<ArrayList<Recognition>> mainList) {
        ArrayList<Recognition> results = new ArrayList<>();
        if (mainList.isEmpty()) {
            return results;
        }
        int maxDetectionsOnBitmap = mainList.get(0).size();
        for (int i = 0; i < mainList.size(); i++) {
            if (maxDetectionsOnBitmap < mainList.get(i).size()) {
                maxDetectionsOnBitmap = mainList.get(i).size();
            }
        }
        for (int i = 0; i < maxDetectionsOnBitmap; i++) {
            HashMap<String, Float> detects = new HashMap<>();
            if (!mainList.get(0).isEmpty()) {
                Recognition recognition = mainList.get(0).get(0);

                for (ArrayList<Recognition> recognitions : mainList)
                    if (recognitions.size() > i) {
                        recognition = recognitions.get(i);
                        if (detects.containsKey(recognition.getTitle())) {
                            detects.put(recognition.getTitle(), detects.get(recognition.getTitle()) + recognition.getConfidence() * 100);
                        } else {
                            detects.put(recognition.getTitle(), recognition.getConfidence());
                        }
                    }
                String maxKey = "";
                float maxValue = 0.0F;
                for (
                        String key : detects.keySet()) {
                    if (maxValue < ((detects.get(key)) / mainList.size())) {
                        maxValue = (detects.get(key)) / mainList.size();
                        maxKey = key;
                    }

                    recognition.setConfidence(maxValue / 100);
                    recognition.setTitle(maxKey);
                    results.add(recognition);
                }
            }
        }
        return results;
    }

    private Bitmap cropBitmapClassification(Recognition detectResult, Bitmap startBitmape) {
        Bitmap startBitmap;

        if (Settings.getInstance().isIsfront()) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1, 1);
            startBitmap = Bitmap.createBitmap(startBitmape, 0, 0, startBitmape.getWidth(),
                    startBitmape.getHeight(), matrix, false);
        } else {
            startBitmap = startBitmape;
        }

        RectF coordinates = detectResult.getLocation();
        if (coordinates.top < 0) {
            coordinates.top = 0;
        }
        if (coordinates.left < 0) {
            coordinates.left = 0;
        }
        if (coordinates.bottom > startBitmap.getHeight()) {
            coordinates.bottom = startBitmap.getHeight();
        }
        if (coordinates.right > startBitmap.getWidth()) {
            coordinates.right = startBitmap.getWidth();
        }
        int width = Math.round(coordinates.right - coordinates.left) < 0 ? 1 : Math.round(coordinates.right - coordinates.left);
        int height = Math.round(coordinates.bottom - coordinates.top) < 0 ? 1 : Math.round(coordinates.bottom - coordinates.top);

        int[] pixels = new int[width * height];
        Bitmap bitmapPart = Bitmap.createBitmap(width == 0 ? 1 : width,
                height == 0 ? 1 : height,
                Bitmap.Config.ARGB_8888);

        startBitmap.getPixels(
                pixels, 0, width, Math.round(coordinates.left), Math.round(coordinates.top),
                width,
                height
        );
        bitmapPart
                .setPixels(pixels, 0, width,
                        0, 0,
                        width,
                        height);

        return bitmapPart;
    }

    private Bitmap cropBitmapDetection(Bitmap startBitmap) {
        if (Settings.getInstance().isIsfront()) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1, 1);
            return Bitmap.createBitmap(startBitmap, 0, 0, startBitmap.getWidth(),
                    startBitmap.getHeight(), matrix, false);
        } else {
            return startBitmap;
        }
    }

    @Override
    protected void newBitmap() {
        int cropSize = TF_OD_API_INPUT_SIZE;
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        DataHelper.getInstance().getListOne().add(croppedBitmap);
        if (DataHelper.getInstance().getListOne().size() >= Preference.getInstance().getAverage()
            && DataHelper.getInstance().getListOne().size() > 0) {
            isProcessingFrame = true;

            runInBackground(
                    () -> {

                        Log.d(TAG, "start process");
                        startProcess();
                    });
        }
    }
}
