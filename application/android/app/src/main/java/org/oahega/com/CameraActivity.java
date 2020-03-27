package org.oahega.com;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;

import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;

import org.oahega.com.preference.PreferenceBottomSheetDialogFragment;
import org.oahega.com.utils.Settings;
import org.oahega.com.env.ImageUtils;
import org.oahega.com.env.Logger;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener {

  private static final Logger LOGGER = new Logger("CameraActivity");

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    protected boolean useCamera2API;
    protected volatile boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

  private long time = 3_000;
  private long lastTime = System.currentTimeMillis();
  private Handler handler2 = new Handler();
  private Runnable run = new Runnable() {
    @Override
    public void run() {
      if (System.currentTimeMillis() - lastTime > time) {
        finish();
        startActivity(intent);
      } else {
        handler2.postDelayed(this::run, 1_000);
      }
    }
  };
  private Intent intent = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
      intent = getIntent();
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

      findViewById(R.id.settings).setOnClickListener(this);
      handler2.postDelayed(run, 1_000);
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
      LOGGER.d("onPreviewFrame()");
        if (isProcessingFrame) {
          LOGGER.d("onPreviewFrame -> Dropping frame!");
          lastTime = System.currentTimeMillis();
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (!isProcessingFrame) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
          LOGGER.d("Exception!" + e.getMessage());
          e.printStackTrace();
            return;
        }

        yuvBytes[0] = bytes;
        yRowStride = previewWidth;
        // todo save  new ready bitmap


        imageConverter =
                () -> ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

        postInferenceCallback =
                () -> {
                    camera.addCallbackBuffer(bytes);
                    isProcessingFrame = false;
                };
        // todo save  new ready bitmap
//        processImage(); // todo remove
        newBitmap();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
      LOGGER.d("onImageAvailable ");

        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) { // todo
          LOGGER.d("onImageAvailable previewWidth == 0 || previewHeight == 0 ");
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();
            if (isProcessingFrame) {
                if (image != null) {
                    image.close();
                }
              LOGGER.d("onImageAvailable -> Dropping frame!");
              lastTime = System.currentTimeMillis();
                return;
            }
            if (image == null) {
              LOGGER.d("onImageAvailable image == null");
                return;
            }


            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    () -> ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);

            postInferenceCallback =
                    () -> {
                        image.close();
                        isProcessingFrame = false;
                    };

            image.close();
            newBitmap();

        } catch (final Exception e) {
          LOGGER.d("Exception! " + e.getMessage());
          e.printStackTrace();
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
          LOGGER.d("Exception! " + e.getMessage());
          e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        CameraActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera(boolean front) {
      LOGGER.d("chooseCamera()");
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT && front) {
                    continue;
                }
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK && !front) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
              LOGGER.d("Camera API enable: " + useCamera2API);
              LOGGER.d("camera id = " + cameraId);
                return cameraId;
            }
        } catch (CameraAccessException e) {
          e.printStackTrace();
          LOGGER.d(" Not allowed to access camera " + e.getMessage());
        }
      LOGGER.d("chooseCamera() return null");
        return null;
    }

  public void setFragment() {
    LOGGER.d("setFragment()");
        String cameraId = chooseCamera(!Settings.getInstance().isIsfront());

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(Size size, int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }
    LOGGER.d("start fragment transaction");
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
              LOGGER.d("Initializing buffer " + i + " at size " + buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }


    @Override
    public void onClick(View v) {
      if (v.getId() == R.id.settings) {
        PreferenceBottomSheetDialogFragment dialogFragment = new PreferenceBottomSheetDialogFragment();
        FragmentManager fm = getSupportFragmentManager();
        dialogFragment.show(fm, "test");
      }
        if (v.getId() == R.id.btn_change_camera) {
          LOGGER.d("on click: btn_change_camera");
            Settings.getInstance().setIsfront(!Settings.getInstance().isIsfront());
            setFragment();
            return;
        }
    }


    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(int numThreads);

    protected abstract void setUseNNAPI(boolean isChecked);

    protected abstract void startProcess();

    protected abstract void newBitmap();

}
