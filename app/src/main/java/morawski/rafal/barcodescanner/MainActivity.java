package morawski.rafal.barcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import morawski.rafal.barcodescanner.scanner.ScannerActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

public class MainActivity extends AppCompatActivity {

    private static final int RC_BARCODE = 9001;

    private Button mStart;
    private TextView mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResult = findViewById(R.id.result);
        mStart = findViewById(R.id.start);

        mStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            startActivityForResult(intent, RC_BARCODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_BARCODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(ScannerActivity.BARCODE_OBJECT);
                    if (barcode != null) {
                        mResult.setText(barcode.displayValue);
                    } else {
                        mResult.setText(R.string.nullObject);
                    }
                } else {
                    mResult.setText(R.string.noData);
                }
            } else if (resultCode == CommonStatusCodes.CANCELED) {
                mResult.setText(R.string.canceledScan);
            } else if (resultCode == CommonStatusCodes.ERROR) {
                mResult.setText(R.string.error);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}