package morawski.rafal.barcodescanner.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;

import com.google.android.gms.common.images.Size;

public class ScannerBoxOverlay extends View {
    private static final String TAG = "ScannerBoxOverlay";

    private int mLeft;
    private int mTop;
    private int mRectWidth = 200;
    private int mRectHeight = 200;
    private float mStrokeWidth = 10.0f;
    private float mCornerRadius = 20.0f;
    private int mCrosshairColor;

    private Paint mEraser;
    private Paint mCrosshairPaint;

    public ScannerBoxOverlay(Context context) {
        super(context);
        init();
    }

    private void init() {
        mEraser = new Paint();
        mEraser.setAntiAlias(true);
        mEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mEraser.setStrokeWidth(mStrokeWidth);

        mCrosshairColor = Color.WHITE;

        mCrosshairPaint = new Paint();
        mCrosshairPaint.setColor(mCrosshairColor);
        mCrosshairPaint.setStrokeWidth(mStrokeWidth);
        mCrosshairPaint.setStyle(Paint.Style.STROKE);
    }

    public void setScanBoxSizePx(Size size) {
        mRectWidth = size.getWidth();
        mRectHeight = size.getHeight();
        invalidate();
    }

    public Size getBoxSize() {
        return new Size(mRectWidth, mRectHeight);
    }

    public void setCrosshairColor(int color) {
        mCrosshairColor = color;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = l + r;
        int height = t + b;

        mLeft = (width - mRectWidth) / 2;
        mTop = (height - mRectHeight) / 2;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw transparent rect;
        canvas.drawRect(mLeft, mTop, mLeft + mRectWidth, mTop + mRectHeight, mEraser);

        // Draw crosshair
        drawCrosshair(canvas, mCrosshairColor);
    }

    private void drawCrosshair(Canvas canvas, int color) {
        int left = (int) (mLeft - mStrokeWidth / 2);
        int top = (int) (mTop - mStrokeWidth / 2);
        int right = (int) (mLeft + mRectWidth + mStrokeWidth / 2);
        int bottom = (int) (mTop + mRectHeight + mStrokeWidth / 2);

        mCrosshairPaint.setColor(color);
        canvas.drawRoundRect(left, top, right, bottom, mCornerRadius, mCornerRadius, mCrosshairPaint);

        int widthOffset = (int) (mRectWidth * 0.3 / 2.0);
        int heightOffset = (int) (mRectHeight * 0.3 / 2.0);
        int offset = Math.min(widthOffset, heightOffset);

        mEraser.setStrokeWidth(mCrosshairPaint.getStrokeWidth());
        canvas.drawLine(left + offset, top, right - offset, top, mEraser);
        canvas.drawLine(left + offset, bottom, right - offset, bottom, mEraser);
        canvas.drawLine(left, top + offset, left, bottom - offset, mEraser);
        canvas.drawLine(right, top + offset, right, bottom - offset, mEraser);
    }
}
