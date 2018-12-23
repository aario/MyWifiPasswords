package info.aario.mywifipasswords;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ShowWifi extends AppCompatActivity {
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_wifi);
        layout = findViewById(R.id.show_layout);
        Intent intent = getIntent();
        String Ssid = intent.getStringExtra(WifiList.EXTRA_SSID);
        String Psk = intent.getStringExtra(WifiList.EXTRA_PSK);
        TextView tvSsid = findViewById(R.id.ssid);
        tvSsid.setText(Ssid);
        tvSsid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _copyToClipboard("SSID", ((TextView) v).getText().toString());
            }
        });
        TextView tvPsk = findViewById(R.id.psk);
        tvPsk.setText(Psk);
        tvPsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _copyToClipboard("PSK", ((TextView) v).getText().toString());
            }
        });
    }

    private void _copyToClipboard(String Label, String Text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(Label, Text);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(layout, Label + " copied to clipboard.", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}
