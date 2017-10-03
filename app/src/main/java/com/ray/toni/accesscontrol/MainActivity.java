package com.ray.toni.accesscontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;


public class MainActivity extends Activity {

    private NfcAdapter myNfcAdapter;
    private TextView myTextView;
    private PendingIntent pendingIntent;

    @SuppressLint("SetTextI18n")
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // field declarations
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        myTextView = (TextView) findViewById(R.id.result);

        // check for Nfc support
        if (this.myNfcAdapter == null) {
            Toast.makeText(this, "This device doesnt support NFC.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // check if Nfc is enabled
        if (this.myNfcAdapter.isEnabled()) {
            this.myTextView.setText("Ready to Scan");
        } else {
            this.myTextView.setText("NFC is disabled.");
        }

        // look for tags
        handleTag(getIntent());
    }

    public void onResume() {
        super.onResume();
        myNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    protected void onPause() {
        myNfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleTag(intent);
    }

    public static String bytesToHexString(byte[] bytes) {
        char[] hexArray = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v / 16];
            hexChars[(j * 2) + 1] = hexArray[v % 16];
        }
        return new String(hexChars);
    }

    @SuppressLint("SetTextI18n")
    private void handleTag(Intent intent) {
        if (intent == null) {
            return;
        }
        if ("android.nfc.action.TAG_DISCOVERED".equals(intent.getAction()) || "android.nfc.action.TECH_DISCOVERED".equals(intent.getAction())) {
            this.myTextView.setText("Tag discovered");
            IsoDep tag = IsoDep.get((Tag) intent.getParcelableExtra("android.nfc.extra.TAG"));
            byte[] SELECT = new byte[]{
                    (byte) 0,    //CLA
                    (byte) 0xA4, //INS
                    (byte) 0x04, //P1
                    (byte) 0x00, //P2
                    (byte) 0x08, //Length
                    (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00 //AID
            };
            byte[] CMMD = new byte[]{
                    (byte) 0x80, //CLA
                    (byte) 0xCA, //INS
                    (byte) 0x9F, //P1
                    (byte) 0x7F, //P2
                    (byte) 0x2D //Length of response
            };
            if (tag != null) {
                byte[] result = null;
                try {
                    tag.connect();
                    try {
                        result = tag.transceive(SELECT);
                    } catch (IOException e) {
                        Toast.makeText(this, "Oops! Something went wrong.", Toast.LENGTH_SHORT).show();
                    }
                    if (result != null) {
                        if (!(result[0] == (byte) 0x69 && result[1] == (byte) 0x99)) {
                            try {
                                result = tag.transceive(CMMD);
                            } catch (IOException e2) {
                                Toast.makeText(this, "Oops! Something went wrong.", Toast.LENGTH_SHORT).show();
                            }
                            int len = result.length;
                            if (result[len - 2] == (byte) 0x90 && result[len - 1] == (byte) 0) {
                                byte[] data = new byte[(len - 2)];
                                System.arraycopy(result, 0, data, 0, len - 2);
                                this.myTextView.setText(bytesToHexString(data).substring(0, 70));
                            } else {
                                Toast.makeText(this, "Could not process result", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    try {
                        tag.close();
                    } catch (IOException e3) {
                        Toast.makeText(this, "Error closing connection", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e4) {
                    Toast.makeText(this, "Error connecting", Toast.LENGTH_SHORT).show();
                } catch (Throwable th) {
                    try {
                        tag.close();
                    } catch (IOException e5) {
                        Toast.makeText(this, "Error closing connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}