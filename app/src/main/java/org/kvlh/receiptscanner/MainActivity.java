package org.kvlh.receiptscanner;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_SCAN = 1001;
    private static final String FOLDER = Environment.DIRECTORY_PICTURES + "/Receipts";

    private TextView status;
    private Button scanButton;
    private int totalSaved = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        scanButton = new Button(this);
        scanButton.setText("Scan receipts");
        scanButton.setTextSize(22);
        scanButton.setOnClickListener(v -> startScan());

        status = new TextView(this);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 48, 0, 0);
        status.setText("Saved to Pictures/Receipts");

        Button openButton = new Button(this);
        openButton.setText("Open receipts folder");
        openButton.setOnClickListener(v -> openFolder());
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        openParams.topMargin = 24;

        root.addView(scanButton);
        root.addView(openButton, openParams);
        root.addView(status);
        setContentView(root);
    }

    private void openFolder() {
        Uri dir = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents", "primary:Pictures/Receipts");
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(dir, DocumentsContract.Document.MIME_TYPE_DIR);
        try {
            startActivity(intent);
        } catch (Exception e) {
            status.setText("No file manager could open the folder");
        }
    }

    private void startScan() {
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(100)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        GmsDocumentScanning.getClient(options)
                .getStartScanIntent(this)
                .addOnSuccessListener(sender -> {
                    try {
                        startIntentSenderForResult(sender, REQ_SCAN, null, 0, 0, 0);
                    } catch (Exception e) {
                        status.setText("Could not open scanner: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> status.setText("Scanner unavailable: " + e.getMessage()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SCAN || resultCode != RESULT_OK || data == null) {
            return;
        }
        GmsDocumentScanningResult result = GmsDocumentScanningResult.fromActivityResultIntent(data);
        if (result == null || result.getPages() == null) {
            return;
        }
        List<GmsDocumentScanningResult.Page> pages = result.getPages();
        scanButton.setEnabled(false);
        status.setText("Saving " + pages.size() + " page(s)...");
        new Thread(() -> {
            int saved = 0;
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            for (int i = 0; i < pages.size(); i++) {
                try {
                    save(pages.get(i).getImageUri(), "receipt_" + stamp + "_" + (i + 1) + ".jpg");
                    saved++;
                } catch (Exception ignored) {
                    // counted below as not saved
                }
            }
            final int ok = saved;
            final int total = pages.size();
            runOnUiThread(() -> {
                scanButton.setEnabled(true);
                totalSaved += ok;
                status.setText((ok == total ? "" : "Some failed. ")
                        + "Saved " + totalSaved + " receipt(s) to Pictures/Receipts");
            });
        }).start();
    }

    private void save(Uri source, String name) throws Exception {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, FOLDER);
        Uri target = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (target == null) {
            throw new IllegalStateException("MediaStore insert failed");
        }
        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = resolver.openOutputStream(target)) {
            if (in == null || out == null) {
                throw new IllegalStateException("stream null");
            }
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }
}
