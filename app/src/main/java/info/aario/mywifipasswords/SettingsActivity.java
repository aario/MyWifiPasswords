package info.aario.mywifipasswords;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_WPA_SUPPLICANT_PATH = "wpa_supplicant_path";
    public static final String KEY_PREF_WIFI_CONFIG_STORE_PATH = "wifi_config_store_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        setTheme(android.R.style.Theme_Black_NoTitleBar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
