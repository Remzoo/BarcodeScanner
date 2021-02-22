package morawski.rafal.barcodescanner.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import morawski.rafal.barcodescanner.R
import java.io.IOException

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var mPreview: CameraSourcePreview
    private lateinit var mMediaPlayer: MediaPlayer
    private lateinit var mFlashImage: ImageView
    private lateinit var mCloseImage: ImageView

    private var mCameraSource: CameraSource? = null
    private var mScannerBoxOverlay: ScannerBoxOverlay? = null
    private var mFlashEnabled = true
    private var mBeepEnabled = true

    private var mBarcodeDetected = false
    private var mShouldTurnOnFlash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        intent.extras?.let { bundle ->
            mBeepEnabled = bundle.getBoolean(EXTRA_BEEP_ENABLED, true)
            mFlashEnabled = bundle.getBoolean(EXTRA_FLASH_ENABLED, true)
        }

        savedInstanceState?.let {
            mShouldTurnOnFlash = it.getBoolean(KEY_FLASH_ENABLED, false)
        }

        mPreview = findViewById(R.id.preview)
        mFlashImage = findViewById(R.id.flash)
        mCloseImage = findViewById(R.id.close)
        mScannerBoxOverlay = mPreview.getScanBoxOverlay()
        mFlashImage.visibility = if (mFlashEnabled) View.VISIBLE else View.GONE

        mMediaPlayer = MediaPlayer.create(this, R.raw.beep).apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setVolume(0.1f, 0.1f)
        }

        checkCameraPermission()
        if (mFlashEnabled) {
            checkFlashAvailability()
        }
        setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_FLASH_ENABLED, mShouldTurnOnFlash)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        mCameraSource?.let { mPreview.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraSource?.let {
            mPreview.release()
            mCameraSource = null
        }
    }

    override fun onBackPressed() {
        if (mPreview.isStopped()) {
            startCameraSource()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkCameraPermission() {
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RC_CAMERA_PERM)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == RC_CAMERA_PERM) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createCameraSource()
            } else {
                setResult(CommonStatusCodes.ERROR)
                finish()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun checkFlashAvailability() {
        val hasFlash = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        mFlashImage.visibility = if (hasFlash) View.VISIBLE else View.GONE

        if (hasFlash) {
            setupFlash()
        }
    }

    private fun setupListeners() {
        mCloseImage.setOnClickListener {
            setResult(CommonStatusCodes.CANCELED)
            finish()
        }

        mFlashImage.setOnClickListener {
            mShouldTurnOnFlash = !mShouldTurnOnFlash
            setupFlash()
        }
    }

    private fun setupFlash() {
        val flashRes = if (mShouldTurnOnFlash) R.drawable.ic_flash_off else R.drawable.ic_flash_on
        mFlashImage.setImageResource(flashRes)
        mPreview.setFlashEnabled(mShouldTurnOnFlash)
    }

    private fun createCameraSource() {
        val barcodeDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build()


        val barcodeProcessor = object : Detector.Processor<Barcode> {
            override fun release() { /* NO-OP */
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                detections?.let {
                    val barcodes = it.detectedItems
                    if (barcodes.size() == 1 && !mBarcodeDetected) {
                        mBarcodeDetected = true
                        runOnUiThread { onBarcodeDetected(barcodes.valueAt(0)) }
                    }
                }
            }
        }

        if (mScannerBoxOverlay != null) {
            val barcodeBoxDetector = BarcodeBoxDetector(barcodeDetector, mScannerBoxOverlay).apply {
                setProcessor(barcodeProcessor)
            }

            mCameraSource = CameraSource.Builder(this, barcodeBoxDetector)
                    .setRequestedPreviewSize(1920, 1080)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(25.0f)
                    .setAutoFocusEnabled(true)
                    .build()
        } else {
            barcodeDetector.setProcessor(barcodeProcessor)

            mCameraSource = CameraSource.Builder(this, barcodeDetector)
                    .setRequestedPreviewSize(1920, 1080)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(25.0f)
                    .setAutoFocusEnabled(true)
                    .build()
        }
    }

    private fun startCameraSource() {
        mCameraSource?.let {
            try {
                mPreview.start(it)
                mBarcodeDetected = false
            } catch (e: IOException) {
                Log.e(TAG, "startCameraSource: Couldn't start camera source.", e)
                it.release()
                mCameraSource = null
            }
        }
    }

    private fun onBarcodeDetected(barcode: Barcode) {
        mScannerBoxOverlay?.setViewfinderColor(Color.GREEN)
        if (mBeepEnabled) {
            mMediaPlayer.start()
        }
        mPreview.stop()

        Intent().apply {
            putExtra(EXTRA_BARCODE_OBJECT, barcode)
        }.also {
            setResult(CommonStatusCodes.SUCCESS, it)
            finish()
        }
    }


    companion object {
        private const val TAG = "ScannerActivity"

        private const val RC_CAMERA_PERM = 201

        private const val KEY_FLASH_ENABLED = "KEY_FLASH_ENABLED"
        const val EXTRA_BARCODE_OBJECT = "EXTRA_BARCODE_OBJECT"
        const val EXTRA_BEEP_ENABLED = "EXTRA_BEEP_ENABLED"
        const val EXTRA_FLASH_ENABLED = "EXTRA_FLASH_ENABLED"
    }
}