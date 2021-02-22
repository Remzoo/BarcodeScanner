package morawski.rafal.barcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.barcode.Barcode
import morawski.rafal.barcodescanner.scanner.BarcodeScannerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mStart: Button
    private lateinit var mResult: TextView
    private lateinit var mBeepEnabled: CheckBox
    private lateinit var mFlashEnabled: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mStart = findViewById(R.id.start)
        mResult = findViewById(R.id.result)
        mBeepEnabled = findViewById(R.id.cbBeepEnable)
        mFlashEnabled = findViewById(R.id.cbFlashEnabled)

        mStart.setOnClickListener {
            Intent(this, BarcodeScannerActivity::class.java).apply {
                putExtra(BarcodeScannerActivity.EXTRA_BEEP_ENABLED, mBeepEnabled.isChecked)
                putExtra(BarcodeScannerActivity.EXTRA_FLASH_ENABLED, mFlashEnabled.isChecked)
            }.also {
                startActivityForResult(it, RC_BARCODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_BARCODE) {
            when (resultCode) {
                CommonStatusCodes.SUCCESS -> {
                    if (data != null) {
                        val barcode = data.getParcelableExtra<Barcode>(BarcodeScannerActivity.EXTRA_BARCODE_OBJECT)
                        if (barcode != null) {
                            mResult.text = barcode.displayValue
                        } else {
                            mResult.setText(R.string.nullObject)
                        }
                    } else {
                        mResult.setText(R.string.noData)
                    }
                }
                CommonStatusCodes.CANCELED -> {
                    mResult.setText(R.string.canceledScan)
                }
                CommonStatusCodes.ERROR -> {
                    mResult.setText(R.string.error)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val RC_BARCODE = 9001
    }
}