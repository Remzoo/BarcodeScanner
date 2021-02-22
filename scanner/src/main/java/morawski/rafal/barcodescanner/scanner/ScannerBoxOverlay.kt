package morawski.rafal.barcodescanner.scanner

import android.content.Context
import android.graphics.*
import android.view.View
import com.google.android.gms.common.images.Size
import kotlin.math.min

class ScannerBoxOverlay(context: Context) : View(context) {

    private var mLeft: Int = 0
    private var mTop: Int = 0
    private var mRectWidth: Int = 200
    private var mRectHeight: Int = 200
    private var mStrokeWidth: Float = 10.0f
    private var mCornerRadius: Float = 20.0f
    private var mViewfinderColor: Int = Color.WHITE

    private lateinit var mEraser: Paint
    private lateinit var mViewfinderPaint: Paint

    init {
        initialize()
    }

    private fun initialize() {
        mEraser = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            strokeWidth = mStrokeWidth
        }

        mViewfinderColor = Color.WHITE

        mViewfinderPaint = Paint().apply {
            color = mViewfinderColor
            strokeWidth = mStrokeWidth
            style = Paint.Style.STROKE
        }
    }

    fun setBoxSizePx(size: Size) {
        mRectWidth = size.width
        mRectHeight = size.height
        invalidate()
    }

    fun getBoxSizePx() = Size(mRectWidth, mRectHeight)

    fun setViewfinderColor(color: Int) {
        mViewfinderColor = color
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = left + right
        val height = top + bottom

        mLeft = (width - mRectWidth) / 2
        mTop = (height - mRectHeight) / 2
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val canv = canvas ?: return
        // Draw transparent rect
        canv.drawRect(mLeft.toFloat(), mTop.toFloat(), (mLeft + mRectWidth).toFloat(), (mTop + mRectHeight).toFloat(), mEraser)

        // Draw viewfinder
        drawViewfinder(canv, mViewfinderColor)
    }

    private fun drawViewfinder(canvas: Canvas, color: Int) {
        val left = mLeft - mStrokeWidth / 2
        val top = mTop - mStrokeWidth / 2
        val right = mLeft + mRectWidth + mStrokeWidth / 2
        val bottom = mTop + mRectHeight + mStrokeWidth / 2

        mViewfinderPaint.color = color
        canvas.drawRoundRect(left, top, right, bottom, mCornerRadius, mCornerRadius, mViewfinderPaint)

        val widthOffset = (mRectWidth * 0.3 / 2.0).toInt()
        val heightOffset = (mRectHeight * 0.3 / 2.0).toInt()
        val offset = min(widthOffset, heightOffset)

        mEraser.strokeWidth = mViewfinderPaint.strokeWidth
        canvas.drawLine(left + offset, top, right - offset, top, mEraser)
        canvas.drawLine(left + offset, bottom, right - offset, bottom, mEraser)
        canvas.drawLine(left, top + offset, left, bottom - offset, mEraser)
        canvas.drawLine(right, top + offset, right, bottom - offset, mEraser)
    }
}