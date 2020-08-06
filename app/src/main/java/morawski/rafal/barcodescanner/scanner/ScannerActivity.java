package morawski.rafal.barcodescanner.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import morawski.rafal.barcodescanner.R;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScannerActivity extends AppCompatActivity {
    private static final String TAG = "ScannerActivity";

    private static final int RC_CAMERA_PERM = 201;
    private static final String KEY_FLASH_ENABLED = "KEY_FLASH_ENABLED";

    public static final String BARCODE_OBJECT = "BARCODE_OBJECT";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private ScannerBoxOverlay mScannerBoxOverlay;
    private MediaPlayer mMediaPlayer;

    private ImageView mFlashImage;
    private ImageView mCloseImage;
    private boolean mFlashEnabled = false;
    private boolean mBarcodeDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        if (savedInstanceState != null) {
            mFlashEnabled = savedInstanceState.getBoolean(KEY_FLASH_ENABLED, false);
        }

        mPreview = findViewById(R.id.preview);
        mFlashImage = findViewById(R.id.flash);
        mCloseImage = findViewById(R.id.close);
        mScannerBoxOverlay = mPreview.getScannerOverlay();

        mMediaPlayer = MediaPlayer.create(this, R.raw.beep);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setVolume(0.1f, 0.1f);

        checkFlashAvailability();
        checkCameraPermission();
        setUpListeners();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_FLASH_ENABLED, mFlashEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraSource != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mPreview.release();
            mCameraSource = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mPreview.isStopped()) {
            startCameraSource();
        } else {
            super.onBackPressed();
        }
    }

    private void checkCameraPermission() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, RC_CAMERA_PERM);
        }
    }

    private void checkFlashAvailability() {
        boolean hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        mFlashImage.setVisibility(hasFlash ? View.VISIBLE : View.GONE);

        if (hasFlash) {
            setUpFlash();
        }
    }

    private void setUpListeners() {
        mCloseImage.setOnClickListener(v -> {
            setResult(CommonStatusCodes.CANCELED);
            finish();
        });

        mFlashImage.setOnClickListener(v -> {
            mFlashEnabled = !mFlashEnabled;
            setUpFlash();
        });
    }

    private void setUpFlash() {
        mFlashImage.setImageResource(mFlashEnabled ? R.drawable.ic_flash_off : R.drawable.ic_flash_on);
        mPreview.setFlashEnabled(mFlashEnabled);
    }


    private void startCameraSource() {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
                mBarcodeDetected = false;
            } catch (IOException e) {
                Log.e(TAG, "onResume: Could no start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void createCameraSource() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        Detector.Processor<Barcode> barcodeProcessor = new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() == 1 && !mBarcodeDetected) {
                    mBarcodeDetected = true;
                    runOnUiThread(() -> onBarcodeDetected(barcodes.valueAt(0)));
                }
            }
        };

        if (mScannerBoxOverlay != null) {
            BarcodeBoxDetector barcodeBoxDetector = new BarcodeBoxDetector(barcodeDetector, mScannerBoxOverlay);
            barcodeBoxDetector.setProcessor(barcodeProcessor);

            mCameraSource = new CameraSource.Builder(this, barcodeBoxDetector)
                    .setRequestedPreviewSize(1920, 1080)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(25.0f)
                    .setAutoFocusEnabled(true)
                    .build();
        } else {
            barcodeDetector.setProcessor(barcodeProcessor);

            mCameraSource = new CameraSource.Builder(this, barcodeDetector)
                    .setRequestedPreviewSize(1920, 1080)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(25.0f)
                    .setAutoFocusEnabled(true)
                    .build();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
            } else {
                setResult(CommonStatusCodes.ERROR);
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void onBarcodeDetected(Barcode barcode) {
        if (mScannerBoxOverlay != null) {
            mScannerBoxOverlay.setCrosshairColor(Color.GREEN);
        }
        mMediaPlayer.start();
        mPreview.stop();

        Intent resultIntent = new Intent();
        resultIntent.putExtra(BARCODE_OBJECT, barcode);
        setResult(CommonStatusCodes.SUCCESS, resultIntent);
        finish();
    }
}