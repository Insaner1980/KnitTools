package com.finnvek.knittools.audit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Process;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** A separate-UID recipient with no dependency on the target APK runtime. */
public class AuditPart16ShareReceiver extends Activity {
    private InputStream input;
    private BroadcastReceiver continuation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Uri uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? getIntent().getParcelableExtra(Intent.EXTRA_STREAM, Uri.class)
                : getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            input = getContentResolver().openInputStream(uri);
            if (getIntent().getBooleanExtra("hold", false)) {
                continuation = new BroadcastReceiver() {
                    @Override public void onReceive(Context context, Intent intent) { readAndFinish(); }
                };
                IntentFilter filter = new IntentFilter(getPackageName() + ".CONTINUE");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(continuation, filter, RECEIVER_EXPORTED);
                } else {
                    registerReceiver(continuation, filter);
                }
                reply("opened", null, null, false, false);
            } else {
                readAndFinish();
            }
        } catch (Exception failure) {
            reply("failed", null, failure.getClass().getSimpleName(), false, false);
            finish();
        }
    }

    private void readAndFinish() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            input.close();
            input = null;
            boolean denied = false;
            try (InputStream other = getContentResolver().openInputStream(Uri.parse(getIntent().getStringExtra("other")))) {
                if (other != null) other.read();
            } catch (SecurityException expected) { denied = true; }
            boolean writeDenied = false;
            Uri shared = getIntent().getClipData().getItemAt(0).getUri();
            try (android.os.ParcelFileDescriptor ignored = getContentResolver().openFileDescriptor(shared, "rw")) {
                // Opening is sufficient to detect an unintended write grant.
            } catch (SecurityException expected) { writeDenied = true; }
            reply("read", bytes.toByteArray(), null, denied, writeDenied);
        } catch (Exception failure) {
            reply("failed", null, failure.getClass().getSimpleName(), false, false);
        } finally { finish(); }
    }

    private void reply(String stage, byte[] bytes, String failure, boolean otherDenied, boolean writeDenied) {
        sendBroadcast(new Intent(getIntent().getStringExtra("replyAction"))
            .setPackage(getIntent().getStringExtra("owner"))
            .putExtra("stage", stage).putExtra("bytes", bytes).putExtra("failure", failure)
            .putExtra("uid", Process.myUid()).putExtra("otherDenied", otherDenied).putExtra("writeDenied", writeDenied));
    }

    @Override public void onDestroy() {
        if (continuation != null) unregisterReceiver(continuation);
        try { if (input != null) input.close(); } catch (Exception ignored) {
            // Best-effort teardown must not replace the share result already reported to the test.
        }
        super.onDestroy();
    }
}
