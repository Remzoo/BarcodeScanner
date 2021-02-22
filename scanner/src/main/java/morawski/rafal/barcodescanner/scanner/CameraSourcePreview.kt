package morawski.rafal.barcodescanner.scanner

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Camera
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.CameraSource
import morawski.rafal.barcodescanner.R
import java.io.IOException
import kotlin.math.round

class CameraSourcePreview(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {

    private val TAG = "CameraSourcePreview"

    private var mSurfaceAvailable = false
    private var mStartRequested = false
    private var mDrawScanBox = false
    private var mIsStopped = true
    private var mFlashEnabled = false

    private var mCamera: Camera? = null
    private var mCameraSource: CameraSource? = null
    private var mScanBoxOverlay: ScannerBoxOverlay? = null
    private var mSurfaceView: SurfaceView
    private var mScanBoxSize: Size

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(p0: SurfaceHolder) {
            mSurfaceAvailable = true

            try {
                startIfReady()
            } catch (se: SecurityException) {
                Log.e(TAG, "surfaceCreated: No Camera permissions.", se)
            } catch (e: IOException) {
                Log.e(TAG, "surfaceCreated: Could not create camera source.", e)
            }
        }

        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) { /* NO-OP */
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) { /* NO-OP */
        }
    }

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.CameraSourcePreview, 0, 0)
        try {
            mDrawScanBox = typedArray.getBoolean(R.styleable.CameraSourcePreview_drawBox, false)
            val boxWidth = typedArray.getInteger(R.styleable.CameraSourcePreview_boxWidth, 200)
            val boxHeight = typedArray.getInteger(R.styleable.CameraSourcePreview_boxHeight, 200)
            mScanBoxSize = Size(dpToPx(boxWidth), dpToPx(boxHeight))
        } finally {
            typedArray.recycle()
        }

        mSurfaceView = SurfaceView(context)
        mSurfaceView.holder.addCallback(surfaceCallback)
        addView(mSurfaceView)

        if (mDrawScanBox) {
            mSurfaceView.setBackgroundColor(Color.parseColor("#44000000"))

            mScanBoxOverlay = ScannerBoxOverlay(context).apply {
                layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT)
                setBoxSizePx(mScanBoxSize)
            }.also {
                addView(it)
            }
        }
    }

    fun getScanBoxOverlay() = mScanBoxOverlay

    @Throws(IOException::class, SecurityException::class)
    fun start(cameraSource: CameraSource?) {
        if (cameraSource == null) {
            stop()
        }

        mCameraSource = cameraSource
        if (mCameraSource != null) {
            mStartRequested = true
            startIfReady()
        }
    }

    fun stop() {
        mCameraSource?.stop()
        mIsStopped = true
    }

    fun release() {
        mCameraSource?.release()
        mCameraSource = null
    }

    fun isStopped() = mIsStopped

    fun setFlashEnabled(enabled: Boolean) {
        mFlashEnabled = enabled
        setupFlash()
    }

    fun isFlashEnabled() = mFlashEnabled

    @Throws(IOException::class, SecurityException::class)
    private fun startIfReady() {
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource?.start(mSurfaceView.holder)
            mCamera = getCameraObject()
            setupFlash()

            mStartRequested = false
            mIsStopped = false
        }
    }

    private fun setupFlash() {
        mCamera?.let { camera ->
            val params = camera.parameters
            params.flashMode = if (mFlashEnabled) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
            camera.parameters = params
        }
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val layoutWidth = right - left
        val layoutHeight = bottom - top

        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, layoutWidth, layoutHeight)
        }

        try {
            startIfReady()
        } catch (se: SecurityException) {
            Log.e(TAG, "onLayout: No Camera permissions.", se)
        } catch (e: IOException) {
            Log.e(TAG, "onLayout: Could not create camera source.", e)
        }
    }

    private fun getCameraObject(): Camera? {
        val fields = CameraSource::class.java.declaredFields

        for (field in fields) {
            if (field.type == Camera::class.java) {
                field.isAccessible = true

                try {
                    return field.get(mCameraSource) as Camera
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                break
            }
        }
        return null
    }

    private fun dpToPx(dp: Int): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        return round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }
}