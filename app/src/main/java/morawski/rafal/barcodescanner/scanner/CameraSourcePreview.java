package morawski.rafal.barcodescanner.scanner;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import morawski.rafal.barcodescanner.R;
import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;
import java.lang.reflect.Field;

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private Context mContext;
    private CameraSource mCameraSource;
    private SurfaceView mSurfaceView;
    private ScannerBoxOverlay mScannerOverlay;
    private Camera mCamera;

    private boolean mSurfaceAvailable;
    private boolean mStartRequested;
    private boolean mDrawScanBox;
    private boolean mIsStopped;
    private boolean mFlashEnabled;

    private Size mScanBoxSize;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSurfaceAvailable = false;
        mStartRequested = false;
        mDrawScanBox = false;
        mIsStopped = true;
        mFlashEnabled = false;

        // Obtain style settings
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CameraSourcePreview, 0, 0);
        try {
            mDrawScanBox = typedArray.getBoolean(R.styleable.CameraSourcePreview_drawBox, false);
            int boxWidth = typedArray.getInteger(R.styleable.CameraSourcePreview_boxWidth, 200);
            int boxHeight = typedArray.getInteger(R.styleable.CameraSourcePreview_boxHeight, 200);
            mScanBoxSize = new Size(dpToPx(boxWidth), dpToPx(boxHeight));

        } finally {
            typedArray.recycle();
        }

        mSurfaceView = new SurfaceView(mContext);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);

        if (mDrawScanBox) {
            mSurfaceView.setBackgroundColor(Color.parseColor("#44000000"));

            mScannerOverlay = new ScannerBoxOverlay(mContext);
            mScannerOverlay.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            mScannerOverlay.setScanBoxSizePx(mScanBoxSize);
            addView(mScannerOverlay);
        }
    }

    public ScannerBoxOverlay getScannerOverlay() {
        return mScannerOverlay;
    }

    public void start(CameraSource cameraSource) throws IOException, SecurityException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;
        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
        mIsStopped = true;
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    public boolean isStopped() {
        return mIsStopped;
    }

    public void setFlashEnabled(boolean enabled) {
        mFlashEnabled = enabled;
        setUpFlash();
    }

    public boolean isFlashEnabled() {
        return mFlashEnabled;
    }

    private void startIfReady() throws IOException, SecurityException {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mSurfaceView.getHolder());
            mCamera = getCameraObject();
            setUpFlash();

            mStartRequested = false;
            mIsStopped = false;
        }
    }

    private void setUpFlash() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(mFlashEnabled ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(params);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(0, 0, layoutWidth, layoutHeight);
        }

        try {
            startIfReady();
        } catch (SecurityException se) {
            Log.e(TAG, "surfaceCreated: No camera permissions.", se);
        } catch (IOException e) {
            Log.e(TAG, "surfaceCreated: Could not create camera source.", e);
        }
    }

    class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceAvailable = true;

            try {
                startIfReady();
            } catch (SecurityException se) {
                Log.e(TAG, "surfaceCreated: No camera permissions.", se);
            } catch (IOException e) {
                Log.e(TAG, "surfaceCreated: Could not create camera source.", e);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceAvailable = false;
        }
    }

    private Camera getCameraObject() {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);

                try {
                    Camera camera = (Camera) field.get(mCameraSource);
                    return camera;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
