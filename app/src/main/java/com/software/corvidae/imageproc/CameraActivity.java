package com.software.corvidae.imageproc;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import org.opencv.android.OpenCVLoader;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,
                                                                                Camera.PreviewCallback {
    private static final String TAG = "CameraActivity";

    private static final int ACCEPT_REJECT_PICTURE_REQUEST = 1;  // The request code

    private Camera.Size _previewSize;
    private AutoFitTextureView _textureView;
    private static Camera _camera;
    private ImageView _cameraPreview = null;
    private Bitmap _transformBitmap = null;
    private int[] _imagePixels = null;
    private byte[] _frameData = null;
    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;
    private OrientationEventListener _orientationListener;
    private static int _cameraOrientation;
    private static int _deviceOrientation;
    private int _seekBarValue;

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the camera instance early in app lifecycle
        try {
            _camera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "onCreate() - Camera could not be opened " + e);
        }

        _orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                _cameraOrientation = orientation;
            }
        };

        if (_orientationListener.canDetectOrientation()) {
            _orientationListener.enable();
        } else {
            _orientationListener.disable();
        }

        setContentView(R.layout.camera_layout);
        _textureView = (AutoFitTextureView) findViewById(R.id.previewTextureView);
        _cameraPreview = (ImageView) findViewById(R.id.imageView);

        // a seekbar to change canny threshold settings
        SeekBar _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _seekBarValue = _seekBar.getProgress();
        _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                _seekBarValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                showSeekBarToast();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Called after onCreate()
     * @param savedInstanceState Bundle containing saved instance values
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // set this up late after camera is opened
        _textureView.setSurfaceTextureListener(this);

        // default activity return value
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED, null);
        finish();
        super.onBackPressed();
    }

    /**
     * Used for handling the CANCEL and SAVE buttons in the Meet Summary Activity
     * These buttons show when the user has requested a new gymnast picture be added
     *
     * @param requestCode The request code coming back from the activity
     * @param resultCode The return code from the button press
     * @param data This will always be null
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ACCEPT_REJECT_PICTURE_REQUEST) {
            if (resultCode == RESULT_OK) {
                setResult(Activity.RESULT_OK, null);
            } else if (resultCode == RESULT_CANCELED) {
                    setResult(Activity.RESULT_CANCELED, null);
            }

            cleanup();
            finish();
        }
    }

    /**
     * Determines the device orientation and sets camera parameters accordingly
     */
    private static void setCameraDisplayOrientation() {

        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info);

        int degrees = 90;
        if (_cameraOrientation < 45 || _cameraOrientation > 315) degrees = 0;
        else if (_cameraOrientation < 135) degrees = 90;
        else if (_cameraOrientation < 225) degrees = 180;

        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            _deviceOrientation = (info.orientation + degrees) % 360;
            _deviceOrientation = (360 - _deviceOrientation) % 360;  // compensate for mirror
        } else {
            // back-facing
            _deviceOrientation = (info.orientation - degrees + 360) % 360;
        }

        if (_camera != null) {
            _camera.setDisplayOrientation(_deviceOrientation);
            _camera.getParameters().setRotation(_deviceOrientation);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable() called...");
        Log.d(TAG, "onSurfaceTextureAvailable() Width = " + width);
        Log.d(TAG, "onSurfaceTextureAvailable() Height = " + height);

        // can't do anything without camera instance
        if (_camera == null) {
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        setCameraDisplayOrientation();

        // start 'er up!
        try {
            _camera.setPreviewTexture(surface);
            _camera.setPreviewCallback(this);
            _camera.startPreview();

        } catch (Exception e) {
            Log.e(TAG, "Exception starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // ...
        configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        cleanup();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `_textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `_textureView` is fixed.
     *
     * @param viewWidth  The width of `_textureView`
     * @param viewHeight The height of `_textureView`
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void configureTransform(int viewWidth, int viewHeight) {

        if (null == _textureView || null == _previewSize) {
            return;
        }

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, _previewSize.height, _previewSize.width);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        int rotation = _deviceOrientation;
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / _previewSize.height,
                    (float) viewWidth / _previewSize.width);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        _textureView.setTransform(matrix);
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Log.d(TAG, "setUpCameraOutputs() called...");
        try {
            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = _deviceOrientation;
            int sensorOrientation = 90;
           // boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    //noinspection ConstantConditions,ConstantConditions
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        //swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    //noinspection ConstantConditions
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        //swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        // go for picture size that fits display size (we never need better resolution than this)
        try {
            DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
            List<Camera.Size> sizes = _camera.getParameters().getSupportedPictureSizes();
            for (Camera.Size size : sizes) {
                if (size.width <= metrics.widthPixels || size.height <= metrics.widthPixels) {
                    _camera.getParameters().setPictureSize(size.width, size.height);
                    Log.d(TAG, "Camera Picture Size Width = " + size.width);
                    Log.d(TAG, "Camera Picture Size Height = " + size.height);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        try {
            _previewSize = _camera.getParameters().getPreviewSize();
            Log.d(TAG, "Camera Preview Size Width = " + _previewSize.width);
            Log.d(TAG, "Camera Preview Size Height = " + _previewSize.height);

            // set up OpenCV variables
            PreviewSizeWidth = _previewSize.width;
            PreviewSizeHeight = _previewSize.height;
            _imagePixels = new int[PreviewSizeWidth * PreviewSizeHeight];
            _transformBitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);

            // fit the aspect ratio of TextureView to the size of preview
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                _textureView.setAspectRatio(_previewSize.width, _previewSize.height);
            } else {
                _textureView.setAspectRatio(_previewSize.height, _previewSize.width);
            }

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        imageFormat = _camera.getParameters().getPreviewFormat();
        Log.d(TAG, "Image Format = " + imageFormat);
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     */
    private void showSeekBarToast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Low Threshold: " + _seekBarValue, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    // release all the resources for GC
    private void cleanup() {

        _orientationListener.disable();
        _textureView.setSurfaceTextureListener(null);

        if (_camera != null && isCameraInUse()) {
            try {
                _camera.stopPreview();
                _camera.setPreviewCallback(null);
                _camera.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // check if the camera is in use
    private static boolean isCameraInUse() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (imageFormat == ImageFormat.NV21) {
            if ( !bProcessing )
            {
                _frameData = data;
                mHandler.post(DoImageProcessing);
            }
        } else {
            Log.d(TAG, "Wrong video format");
        }
    }

    // JNI...
    public native boolean ImageProcessing(int width, int height, int lowThreshold, byte[] NV21FrameData, int [] pixels);
    static
    {
        System.loadLibrary("ImageProcessing");
    }

    private Runnable DoImageProcessing = new Runnable() {
        public void run() {

            // set 'processing flag' true
            bProcessing = true;
            try {
                ImageProcessing(
                        PreviewSizeWidth,   //
                        PreviewSizeHeight,  //
                        _seekBarValue,
                        _frameData,         // IN - byte array from camera
                        _imagePixels);      // OUT - char array from jni code
            } catch (Exception e) {
                e.printStackTrace();
            }

            // load bitmap into image view, add some transparency so we can see the camera's preview and
            // rotate. A nicer method would be to return the canny image (_imagePixels) without black background
            _transformBitmap.setPixels(_imagePixels, 0, PreviewSizeWidth, 0, 0, PreviewSizeWidth, PreviewSizeHeight);
            _cameraPreview.setAlpha(.60f);
            _cameraPreview.setImageBitmap(RotateBitmap(_transformBitmap, 90));

            // done processing
            bProcessing = false;
        }
    };

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
