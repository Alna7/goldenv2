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
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;

public class MainActivity extends AppCompatActivity {

    private static final String SHARED_PREFS_NAME = "v2ray_prefs";
    private static final String CONFIG_KEY = "saved_config";
    private static final String UUID_KEY = "user_uuid";
    private static final String CONFIG_URL = "http://109.94.171.5/get_config.php";
    private static final String AES_KEY = "n9v6Qw2sD8e3L1b0";

    private Button connection;
    private TextView connection_speed, connection_traffic, connection_time, server_delay, connected_server_delay, connection_mode, core_version;
    private EditText uuid_input;
    private BroadcastReceiver v2rayBroadCastReceiver;

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button subscribeButton = findViewById(R.id.btn_subscribe);
        Button updateConfigButton = findViewById(R.id.btn_update_config);
        connection = findViewById(R.id.btn_connection);

        connection_speed = findViewById(R.id.connection_speed);
        connection_time = findViewById(R.id.connection_duration);
        connection_traffic = findViewById(R.id.connection_traffic);
        server_delay = findViewById(R.id.server_delay);
        connected_server_delay = findViewById(R.id.connected_server_delay);
        connection_mode = findViewById(R.id.connection_mode);
        uuid_input = findViewById(R.id.uuid_input);
        core_version = findViewById(R.id.core_version);

        V2rayController.init(this, R.drawable.ic_launcher, "V2ray Android");

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        uuid_input.setText(prefs.getString(UUID_KEY, ""));
        core_version.setText(V2rayController.getCoreVersion());

        subscribeButton.setOnClickListener(view -> {
            Intent telegramIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/goldenvpn0"));
            startActivity(telegramIntent);
        });

        updateConfigButton.setOnClickListener(view -> fetchAndSaveRemoteConfig());

        connection.setOnClickListener(view -> {
            String userUUID = uuid_input.getText().toString().trim();

            if (!userUUID.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
                Toast.makeText(this, "UUID نامعتبر است", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs1 = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
            String config = prefs1.getString(CONFIG_KEY, null);
            prefs1.edit().putString(UUID_KEY, userUUID).apply();

            if (config == null || config.isEmpty()) {
                Toast.makeText(this, "ابتدا دکمه آپدیت را بزنید", Toast.LENGTH_SHORT).show();
                return;
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

        // وضعیت اتصال فعلی
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
                String uuid = uuid_input.getText().toString().trim();
                if (!uuid.matches("^[0-9a-fA-F\\-]{36}$")) {
                    runOnUiThread(() -> Toast.makeText(this, "UUID نامعتبر است", Toast.LENGTH_SHORT).show());
                    return;
                }

                URL url = new URL(CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String postData = "uuid=" + URLEncoder.encode(uuid, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String encryptedConfig = responseBuilder.toString();
                String decryptedConfig = decryptAES(encryptedConfig, AES_KEY);

                SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
                prefs.edit()
                        .putString(CONFIG_KEY, decryptedConfig)
                        .putString(UUID_KEY, uuid)
                        .apply();

                runOnUiThread(() -> Toast.makeText(this, "کانفیگ با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "خطا در دریافت یا رمزگشایی کانفیگ", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String decryptAES(String encryptedData, String key) throws Exception {
        byte[] encryptedBytes = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
        }
    }
}
