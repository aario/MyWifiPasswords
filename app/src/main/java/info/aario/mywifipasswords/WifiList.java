package info.aario.mywifipasswords;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class WifiList extends AppCompatActivity {
    public static final String EXTRA_SSID = "info.aario.mywifipasswords.SSID";
    public static final String EXTRA_PSK = "info.aario.mywifipasswords.PSK";
    public static final String DEFAULT_WPA_SUPPLICANT_PATH = "/data/misc/wifi/wpa_supplicant.conf";
    public static final String DEFAULT_WIFI_CONFIG_STORE_PATH = "/data/misc/wifi/WifiConfigStore.xml";

    CoordinatorLayout layout;
    ListView lvWifiConnections;
    Intent ShowWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_list);
        layout = (CoordinatorLayout) findViewById(R.id.list_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lvWifiConnections = findViewById(R.id.wifiList);
        ShowWifi = new Intent(this, ShowWifi.class);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_popup_sync));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _reloadWifiList("");
            }
        });

        SearchView svSearch = findViewById(R.id.search_view);
        svSearch.setQueryHint("Search...");
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                _search(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                _search(newText);
                return true;
            }
        });

        lvWifiConnections.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String Ssid = ((TextView) arg1.findViewById(android.R.id.text1)).getText().toString();
                String Psk = ((TextView) arg1.findViewById(android.R.id.text2)).getText().toString();
                ShowWifi.putExtra(EXTRA_SSID, Ssid);
                ShowWifi.putExtra(EXTRA_PSK, Psk);
                startActivity(ShowWifi);
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        _reloadWifiList("");
    }

    private String _findLineContaining(String[] Lines, String text) {
        for (String Line : Lines) {
            if (Line.contains(text)) {
                return Line;
            }
        }
        return "";
    }

    private String _getValueFromConfigLine(String Line) {
        return Line.split("=")[1].replace("\"", "");
    }

    private String _getConfigValueByKey(String[] Lines, String Key) {
        String Line = _findLineContaining(Lines, Key + '=');
        if (Line.isEmpty()) {
            return "";
        }

        return _getValueFromConfigLine(Line);
    }

    private String _getSsidFromNetworkSectionLines(String[] NetworkSectionLines) {
        return _getConfigValueByKey(NetworkSectionLines, "ssid");
    }

    private String _getPskFromNetworkSectionLines(String[] NetworkSectionLines) {
        return _getConfigValueByKey(NetworkSectionLines, "psk");
    }

    private Map<String, String> _readWpaSupplicant(String FileContent) {
        String[] NetworkSections = FileContent.split("network[=][{]");
        Map<String, String> Connections = new HashMap<String, String>();
        for (String NetworkSection : NetworkSections) {
            String[] NetworkSectionLines = NetworkSection.split("[}]")[0].split("\n");
            String Ssid = _getSsidFromNetworkSectionLines(NetworkSectionLines);
            if (Ssid.isEmpty()) {
                continue;
            }
            String Psk = _getPskFromNetworkSectionLines(NetworkSectionLines);
            if (Psk.isEmpty()) {
                continue;
            }
            Connections.put(Ssid, Psk);
        }
        return Connections;
    }


    private Map<String, String> _readWifiConfigStore(String FileContent) {
        Map<String, String> Connections = new HashMap<String, String>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(FileContent)));
            XPath xpather = XPathFactory.newInstance().newXPath();
            NodeList wificonfs = (NodeList) xpather.evaluate("/WifiConfigStoreData/NetworkList/Network/WifiConfiguration", doc, XPathConstants.NODESET);
            for (int i = 0; i < wificonfs.getLength(); i++) {
                Node conf = wificonfs.item(i);
                String ssid = (String) xpather.evaluate("./string[@name='SSID']/text()", conf, XPathConstants.STRING);
                String psk = (String) xpather.evaluate("./string[@name='PreSharedKey']/text()", conf, XPathConstants.STRING);
                if (ssid.length() > 0 && psk.length() > 0) {
                    Connections.put(ssid.substring(1, ssid.length() - 1), psk.substring(1, psk.length() - 1));
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            Log.e("PARSE_WIFICONFIG", "WifiConfigStore.xml parse error:\n" + e.getStackTrace());
        }
        return Connections;
    }


    private void _populateWifiList(Map<String, String> Connections, String SearchText) {
        SortedSet<String> Ssids = new TreeSet<>(Connections.keySet());
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (String Ssid : Ssids) {
            if ((!SearchText.isEmpty()) && (!Ssid.toLowerCase().contains(SearchText.toLowerCase()))) {
                continue;
            }
            String Psk = Connections.get(Ssid);
            Map<String, String> item = new HashMap<String, String>(2);
            item.put("ssid", Ssid);
            item.put("psk", Psk);
            data.add(item);
        }
        // Initialize an array adapter
        SimpleAdapter Adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[]{"ssid", "psk"},
                new int[]{
                        android.R.id.text1,
                        android.R.id.text2
                }
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View ItemView = super.getView(position, convertView, parent);
                ((TextView) ItemView.findViewById(android.R.id.text1)).setTextColor(Color.GRAY);
                ((TextView) ItemView.findViewById(android.R.id.text2)).setTextColor(Color.WHITE);
                return ItemView;
            }
        };

        lvWifiConnections.setAdapter(Adapter);
    }


    private String sucat(String filepath) {
        try {
            SimpleSuexec cat_exec = new SimpleSuexec(new String[]{"cat", filepath});
            if (cat_exec.retval == 0 && cat_exec.stdout != null) {
                return cat_exec.stdout;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            // No worries. File might not be there.
        }
        return null;
    }


    private Map<String, String> _getNetworks() {
        // Networks in the Android 8+ wifi config store take precedence over a leftover wpa_supplicant file.
        Map<String, String> connmap = new HashMap<String, String>();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String wpa_supplicant = sucat(sharedPref.getString(SettingsActivity.KEY_PREF_WPA_SUPPLICANT_PATH, DEFAULT_WPA_SUPPLICANT_PATH));
        String wifi_config_store = sucat(sharedPref.getString(SettingsActivity.KEY_PREF_WIFI_CONFIG_STORE_PATH, DEFAULT_WIFI_CONFIG_STORE_PATH));
        if (wpa_supplicant != null) connmap.putAll(_readWpaSupplicant(wpa_supplicant));
        if (wifi_config_store != null) connmap.putAll(_readWifiConfigStore(wifi_config_store));
        return connmap;
    }

    private void _reloadWifiList(String SearchText) {
        Map<String, String> Connections = _getNetworks();
        _populateWifiList(Connections, SearchText);
        Snackbar.make(layout, "Wifi list reloaded.", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, About.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void _search(String SearchText) {
        _reloadWifiList(SearchText);
    }
}
