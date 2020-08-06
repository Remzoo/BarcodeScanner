package morawski.rafal.barcodescanner.scanner;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.ByteArrayOutputStream;

public class BarcodeBoxDetector extends Detector<Barcode> {
    private static final String TAG = "BarcodeBoxDetector";

    private Detector<Barcode> mDelegate;
    private ScannerBoxOverlay mScannerBoxOverlay;

    public BarcodeBoxDetector(Detector<Barcode> delegate, ScannerBoxOverlay scannerBoxOverlay) {
        mDelegate = delegate;
        mScannerBoxOverlay = scannerBoxOverlay;
    }

    @Override
    public SparseArray<Barcode> detect(Frame frame) {
        if (mScannerBoxOverlay == null) {
            return mDelegate.detect(frame);
        }

        int frameWidth = frame.getMetadata().getWidth();
        int frameHeight = frame.getMetadata().getHeight();
        int frameRotation = frame.getMetadata().getRotation();

        int viewWidth = mScannerBoxOverlay.getWidth();
        int viewHeight = mScannerBoxOverlay.getHeight();

        float mWidthScaleFactor = 1.0f;
        float mHeightScaleFactor = 1.0f;
        if (isPortrait()) {
            mWidthScaleFactor = (float) frameHeight / (float) viewWidth;
            mHeightScaleFactor = (float) frameWidth / (float) viewHeight;
        } else {
            mWidthScaleFactor = (float) frameWidth / (float) viewWidth;
            mHeightScaleFactor = (float) frameHeight / (float) viewHeight;
        }

        Size boxSize = mScannerBoxOverlay.getBoxSize();

        int boxWidth = (int) (boxSize.getWidth() * mWidthScaleFactor);
        int boxHeight = (int) (boxSize.getHeight() * mHeightScaleFactor);

        Rect rect = getRectangle(frameWidth, frameHeight, boxWidth, boxHeight);

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, frameWidth, frameHeight, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(rect, 100, outputStream);
        byte[] jpegArray = outputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Frame croppedFrame = new Frame.Builder()
                .setBitmap(bitmap)
                .setRotation(frameRotation)
                .build();

        return mDelegate.detect(croppedFrame);
    }

    private Rect getRectangle(int frameWidth, int frameHeight, int boxWidth, int boxHeight) {
        int left = 0, top = 0, right = 0, bottom = 0;

        if (isPortrait()) {
            left = (int) (frameWidth / 2.0 - boxHeight / 2.0);
            top = (int) (frameHeight / 2.0 - boxWidth / 2.0);
            right = (int) (frameWidth / 2.0 + boxHeight / 2.0);
            bottom = (int) (frameHeight / 2.0 + boxWidth / 2.0);

        } else {
            left = (int) (frameWidth / 2.0 - boxWidth / 2.0);
            top = (int) (frameHeight / 2.0 - boxHeight / 2.0);
            right = (int) (frameWidth / 2.0 + boxWidth / 2.0);
            bottom = (int) (frameHeight / 2.0 + boxHeight / 2.0);
        }

        return new Rect(left, top, right, bottom);
    }

    private boolean isPortrait() {
        int orientation = Resources.getSystem().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }
}
