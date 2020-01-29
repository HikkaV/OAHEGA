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

package org.tensorflow.lite.examples.detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Recognition;
import org.tensorflow.lite.examples.detection.tflite.detection.Classifier;
import org.tensorflow.lite.examples.detection.tflite.detection.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;
import org.tensorflow.lite.examples.detection.utils.DataHelper;
import org.tensorflow.lite.examples.detection.utils.Settings;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    private static final String TAG = "DetectorActivity";

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "face_detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/detection_lables.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
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

    private org.tensorflow.lite.examples.detection.tflite.clasification.Classifier classifier;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
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
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
        try {
            classifier = org.tensorflow.lite.examples.detection.tflite.clasification.Classifier.create(this, org.tensorflow.lite.examples.detection.tflite.clasification.Classifier.Model.QUANTIZED,
                    org.tensorflow.lite.examples.detection.tflite.clasification.Classifier.Device.CPU, 8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
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
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
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
        List<Recognition> detections = detector.recognizeImage(bitmap);
        List<Recognition> detectionsResult = new ArrayList<>();
        for (Recognition detection : detections) {
            if (detection.getTitle().equals("face")) {
                detectionsResult.add(detection);
            }
        }
        return detectionsResult;
    }

    @Override
    public void startProcess() {
        startTime = SystemClock.uptimeMillis();
        ArrayList<Bitmap> startBitmaps = DataHelper.getInstance().getListOne();
        ArrayList<ArrayList<Recognition>> mainList = new ArrayList<>();
        for (Bitmap startBitmap : startBitmaps) { // for every bitmap
            List<Recognition> detectResults = detectFaces(cropBitmapDetection(startBitmap));
            ArrayList<Recognition> detects = new ArrayList<>();

            for (Recognition detectResult : detectResults) { // for every detection
                if (detectResult.getConfidence() > Settings.getInstance().getMinDetectionPercentToShow() / 100.0) {
                    ArrayList<Recognition> classificationResults = classifier.recognizeImage(cropBitmapClassification(detectResult, startBitmap), sensorOrientation);
                    ArrayList<Recognition> classifs = new ArrayList<>();
                    for (Recognition clasiifResult : classificationResults) { // for every classification
                        if ((clasiifResult.getConfidence() * 100) > Settings.getInstance().getMinClassificationPercentToShow()) {
                            clasiifResult.setLocation(detectResult.getLocation());
                            classifs.add(clasiifResult);
                        }
                    }
                    if (!classifs.isEmpty()) {
                        detects.add(getRec(classifs));
                    }
                }
                mainList.add(detects);
            }
        }


        ArrayList<Recognition> results = getResultsForShow(mainList);
        if (results == null) {
            readyForNextImage();
            isProcessingFrame = false;
            DataHelper.getInstance().getListOne().clear();
            return;
        }
        showResults(results, startBitmaps.get(0));
    }

    private void showResults(ArrayList<Recognition> results, Bitmap bitmap) {
        bitmap = Bitmap.createBitmap(bitmap);
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Recognition> mappedRecognitions =
                new LinkedList<>();
        if (results.isEmpty()) {
            readyForNextImage();
            isProcessingFrame = false;
            DataHelper.getInstance().getListOne().clear();
            return;
        }
//        Recognition result = getRec(results);
        for (final Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null
                    && result.getConfidence() > (Settings.getInstance().getMinClassificationPercentToShow() / 100.0)) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
            }
        }

        tracker.trackResults(mappedRecognitions, System.currentTimeMillis());
        trackingOverlay.postInvalidate();

        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        Bitmap finalBitmap = bitmap;
        runOnUiThread(
                () -> {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(finalBitmap.getWidth() + "x" + finalBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                });
        readyForNextImage();
        isProcessingFrame = false;
        DataHelper.getInstance().getListOne().clear();
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

        return Bitmap.createScaledBitmap(bitmapPart, 128, 128, true);
    }

    private Bitmap cropBitmapDetection(Bitmap startBitmap) {
        if (Settings.getInstance().isIsfront()) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1, 1);
            return Bitmap.createScaledBitmap(Bitmap.createBitmap(startBitmap, 0, 0, startBitmap.getWidth(),
                    startBitmap.getHeight(), matrix, false), 300, 300, false);
        } else {
            return Bitmap.createScaledBitmap(startBitmap, 300, 300, false);
        }
    }

    @Override
    protected void newBitmap() {
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        DataHelper.getInstance().getListOne().add(croppedBitmap);
        if (DataHelper.getInstance().getListOne().size() >= Settings.getInstance().getNumOfAvarage() && DataHelper.getInstance().getListOne().size() > 0) {
            isProcessingFrame = true;

            runInBackground(
                    () -> {

                            Log.d(TAG, "start process");
                            startProcess();
                    });
        }
    }
}
