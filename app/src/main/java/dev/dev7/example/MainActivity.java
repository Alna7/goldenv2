package dev.dev7.example;

import static dev.dev7.lib.v2ray.utils.V2rayConstants.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.*;
import java.util.*;

import android.util.Base64;

import dev.dev7.lib.v2ray.V2rayController;

public class MainActivity extends AppCompatActivity {

    private EditText keyInput;
    private Spinner serverSelector;
    private Button btnUpdateConfig, btnConnection;
    private List<String> configList = new ArrayList<>();
    private List<String> configNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final String BASE_URL = "https://www.speedur.org:2096/sub/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        V2rayController.init(this, R.drawable.ic_launcher, "Golden VPN");

        keyInput = findViewById(R.id.key_input);
        serverSelector = findViewById(R.id.server_selector);
        btnUpdateConfig = findViewById(R.id.btn_update_config);
        btnConnection = findViewById(R.id.btn_connection);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, configNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverSelector.setAdapter(adapter);

        btnUpdateConfig.setOnClickListener(view -> fetchConfigs());

        btnConnection.setOnClickListener(view -> {
    int selectedIndex = serverSelector.getSelectedItemPosition();
    if (selectedIndex >= 0 && selectedIndex < configList.size()) {
        String selectedConfig = configList.get(selectedIndex);
        ConnectionState state = V2rayController.getConnectionState();

        if (state == ConnectionState.DISCONNECTED) {
            V2rayController.startV2ray(this, "Golden", selectedConfig, null);
        } else {
            V2rayController.stopV2ray(this);
        }
    } else {
        Toast.makeText(this, "سروری انتخاب نشده", Toast.LENGTH_SHORT).show();
    }
});

    private void fetchConfigs() {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            Toast.makeText(this, "لطفاً کلید را وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + key);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                reader.close();

                List<String> fullConfigs = new ArrayList<>();
                List<String> names = new ArrayList<>();

                for (String rawLine : builder.toString().split("\n")) {
                    rawLine = rawLine.trim();
                    if (rawLine.startsWith("vmess://") || rawLine.startsWith("vless://")) {
                        String decodedJson = null;
                        if (rawLine.startsWith("vmess://")) {
                            try {
                                String jsonPart = rawLine.substring(8);
                                decodedJson = new String(Base64.decode(jsonPart, Base64.DEFAULT));
                                String ps = decodedJson.contains("\"ps\"") ? decodedJson.split("\"ps\"\\s*:\\s*\"")[1].split("\"")[0] : "بدون‌نام";
                                names.add(ps);
                            } catch (Exception e) {
                                names.add("VMess ناشناس");
                            }
                        } else {
                            // vless
                            String ps = "VLESS ناشناس";
                            if (rawLine.contains("#")) {
                                ps = URLDecoder.decode(rawLine.substring(rawLine.indexOf('#') + 1), "UTF-8");
                            }
                            names.add(ps);
                        }
                        fullConfigs.add(rawLine);
                    }
                }

                runOnUiThread(() -> {
                    configList.clear();
                    configList.addAll(fullConfigs);
                    configNames.clear();
                    configNames.addAll(names);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "کانفیگ‌ها بارگذاری شدند", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "خطا در دریافت لیست", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
