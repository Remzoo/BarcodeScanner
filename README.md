# BarcodeScanner
BarcodeScanner is a sample application which uses Google Vision API. There is ability to limit detection area by setting parameters in CameraSourcePreview view. Works for portrait and landscape mode. Supporting flash and beep sound on barcode detection.

# Preview
<img src="https://user-images.githubusercontent.com/10036526/109422416-b7009700-79db-11eb-8321-d35792ccf610.gif" width="200" />

# Launching Scanner Activity
```
            Intent(this, BarcodeScannerActivity::class.java).apply {
                putExtra(BarcodeScannerActivity.EXTRA_BEEP_ENABLED, true)
                putExtra(BarcodeScannerActivity.EXTRA_FLASH_ENABLED, true)
            }.also {
                startActivityForResult(it, 9001)
            }
```
