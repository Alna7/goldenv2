package dev.dev7.example;

import static dev.dev7.lib.v2ray.utils.V2rayConstants.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.*;
import java.util.Objects;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;

public class MainActivity extends AppCompatActivity {

    private Button connection;
    private TextView connection_speed, connection_traffic, connection_time, server_delay, connected_server_delay, connection_mode, core_version;
    private EditText uuid_input;
    private BroadcastReceiver v2rayBroadCastReceiver;
    private Spinner serverSelector;

    private String selectedHost = "app"; // دیفالت ترکیه
    private final String CONFIG_URL = "http://109.94.171.5/sub.txt"; // URL کانفیگ
    private final String PREFS_NAME = "AppPrefs";
    private final String CONFIG_KEY = "saved_config";

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button subscribeButton = findViewById(R.id.btn_subscribe);
        Button updateConfigButton = findViewById(R.id.btn_update_config);
        serverSelector = findViewById(R.id.server_selector);

        subscribeButton.setOnClickListener(view -> {
            Intent telegramIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/goldenvpn0"));
            startActivity(telegramIntent);
        });

        updateConfigButton.setOnClickListener(view -> fetchAndSaveRemoteConfig());

        serverSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedServer = parent.getItemAtPosition(position).toString();
                if (selectedServer.equals("آلمان")) {
                    selectedHost = "ali";
                } else {
                    selectedHost = "app";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // پیش‌فرض بماند
            }
        });

        if (savedInstanceState == null) {
            V2rayController.init(this, R.drawable.ic_launcher, "V2ray Android");
            connection = findViewById(R.id.btn_connection);
            connection_speed = findViewById(R.id.connection_speed);
            connection_time = findViewById(R.id.connection_duration);
            connection_traffic = findViewById(R.id.connection_traffic);
            server_delay = findViewById(R.id.server_delay);
            connection_mode = findViewById(R.id.connection_mode);
            connected_server_delay = findViewById(R.id.connected_server_delay);
            uuid_input = findViewById(R.id.uuid_input);
            core_version = findViewById(R.id.core_version);
        }

        core_version.setText(V2rayController.getCoreVersion());

        connection.setOnClickListener(view -> {
            String userUUID = uuid_input.getText().toString().trim();

            if (userUUID.length() < 30 || !userUUID.matches("^[0-9a-fA-F\\-]{36}$")) {
                Toast.makeText(this, "UUID is invalid", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String storedConfig = prefs.getString(CONFIG_KEY, null);

            String config;
            if (storedConfig != null) {
                config = storedConfig.replace("REPLACE_UUID", userUUID).replace("REPLACE_HOST", selectedHost);
            } else {
                config = "vless://" + userUUID +
                        "@185.143.234.120:443?type=ws&host=" + selectedHost +
                        ".alnafun.ir&path=/&security=tls&sni=iau.ac.ir#SelectedServer";
            }

            if (V2rayController.getConnectionState() == CONNECTION_STATES.DISCONNECTED) {
                V2rayController.startV2ray(this, "Dynamic", config, null);
            } else {
                V2rayController.stopV2ray(this);
            }
        });

        connected_server_delay.setOnClickListener(view -> {
            connected_server_delay.setText("connected server delay : measuring...");
            V2rayController.getConnectedV2rayServerDelay(this, delayResult -> runOnUiThread(() ->
                    connected_server_delay.setText("connected server delay : " + delayResult + "ms")));
        });

        server_delay.setOnClickListener(view -> {
            server_delay.setText("server delay : measuring...");
            new Handler().postDelayed(() ->
                    server_delay.setText("server delay : only available after fetching config"), 200);
        });

        connection_mode.setOnClickListener(view -> {
            V2rayController.toggleConnectionMode();
            connection_mode.setText("connection mode : " + V2rayConfigs.serviceMode.toString());
        });

        switch (V2rayController.getConnectionState()) {
            case CONNECTED:
                connection.setText("وصل شدید");
                connected_server_delay.callOnClick();
                break;
            case DISCONNECTED:
                connection.setText("برای اتصال کلیک کنید");
                break;
            case CONNECTING:
                connection.setText("در حال اتصال");
                break;
        }

        v2rayBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runOnUiThread(() -> {
                    connection_time.setText("connection time : " + Objects.requireNonNull(intent.getExtras()).getString(SERVICE_DURATION_BROADCAST_EXTRA));
                    connection_speed.setText("connection speed : " + intent.getExtras().getString(SERVICE_UPLOAD_SPEED_BROADCAST_EXTRA) + " | " + intent.getExtras().getString(SERVICE_DOWNLOAD_SPEED_BROADCAST_EXTRA));
                    connection_traffic.setText("connection traffic : " + intent.getExtras().getString(SERVICE_UPLOAD_TRAFFIC_BROADCAST_EXTRA) + " | " + intent.getExtras().getString(SERVICE_DOWNLOAD_TRAFFIC_BROADCAST_EXTRA));
                    connection_mode.setText("connection mode : " + V2rayConfigs.serviceMode.toString());
                    switch ((CONNECTION_STATES) Objects.requireNonNull(intent.getExtras().getSerializable(SERVICE_CONNECTION_STATE_BROADCAST_EXTRA))) {
                        case CONNECTED:
                            connection.setText("CONNECTED");
                            break;
                        case DISCONNECTED:
                            connection.setText("DISCONNECTED");
                            connected_server_delay.setText("connected server delay : wait for connection");
                            break;
                        case CONNECTING:
                            connection.setText("CONNECTING");
                            break;
                    }
                });
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT), RECEIVER_EXPORTED);
        } else {
            registerReceiver(v2rayBroadCastReceiver, new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT));
        }
    }

    private void fetchAndSaveRemoteConfig() {
        new Thread(() -> {
            try {
                URL url = new URL(CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder configBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    configBuilder.append(line).append("\n");
                }
                reader.close();

                String config = configBuilder.toString();

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(CONFIG_KEY, config).apply();

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "کانفیگ با موفقیت آپدیت شد", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "خطا در دریافت کانفیگ", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
        }
    }
}
