package morawski.rafal.barcodescanner.scanner

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import java.io.ByteArrayOutputStream

class BarcodeBoxDetector(
        private val mDelegate: Detector<Barcode>,
        private val mScannerBoxOverlay: ScannerBoxOverlay? = null
) : Detector<Barcode>() {

    override fun detect(frame: Frame): SparseArray<Barcode> {
        if (mScannerBoxOverlay == null) {
            // There is no box, so nothing to do because we don't need to crop
            // pass work to delegate
            return mDelegate.detect(frame)
        }

        val frameWidth = frame.metadata.width
        val frameHeight = frame.metadata.height
        val frameRotation = frame.metadata.rotation

        val viewWidth = mScannerBoxOverlay.width
        val viewHeight = mScannerBoxOverlay.height

        val widthScaleFactor: Float
        val heightScaleFactor: Float
        if (isPortrait) {
            widthScaleFactor = frameHeight.toFloat() / viewWidth.toFloat()
            heightScaleFactor = frameWidth.toFloat() / viewHeight.toFloat()
        } else {
            widthScaleFactor = frameWidth.toFloat() / viewWidth.toFloat()
            heightScaleFactor = frameHeight.toFloat() / viewHeight.toFloat()
        }

        val scanBoxSize = mScannerBoxOverlay.getBoxSizePx()
        val boxWidth = (scanBoxSize.width * widthScaleFactor).toInt()
        val boxHeight = (scanBoxSize.height * heightScaleFactor).toInt()

        val cropRect = getRectangle(frameWidth, frameHeight, boxWidth, boxHeight)

        val yuvImage = YuvImage(frame.grayscaleImageData?.array(), ImageFormat.NV21, frameWidth, frameHeight, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(cropRect, 100, outputStream)
        val jpegArray = outputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)

        val croppedFrame = Frame.Builder()
                .setBitmap(bitmap)
                .setRotation(frameRotation)
                .build()

        return mDelegate.detect(croppedFrame)
    }

    private fun getRectangle(frameWidth: Int, frameHeight: Int, boxWidth: Int, boxHeight: Int): Rect {
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int

        if (isPortrait) {
            left = (frameWidth / 2.0 - boxHeight / 2.0).toInt()
            top = (frameHeight / 2.0 - boxWidth / 2.0).toInt()
            right = (frameWidth / 2.0 + boxHeight / 2.0).toInt()
            bottom = (frameHeight / 2.0 + boxWidth / 2.0).toInt()
        } else {
            left = (frameWidth / 2.0 - boxWidth / 2.0).toInt()
            top = (frameHeight / 2.0 - boxHeight / 2.0).toInt()
            right = (frameWidth / 2.0 + boxWidth / 2.0).toInt()
            bottom = (frameHeight / 2.0 + boxHeight / 2.0).toInt()
        }
        return Rect(left, top, right, bottom)
    }

    private val isPortrait
        get() = Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}