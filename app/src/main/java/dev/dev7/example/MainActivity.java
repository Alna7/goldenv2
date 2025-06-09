package dev.dev7.example;

import static dev.dev7.lib.v2ray.utils.V2rayConstants.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.V2rayConfigs;
import dev.dev7.lib.v2ray.utils.V2rayConstants;

public class MainActivity extends AppCompatActivity {

    private Button connection;
    private TextView connection_speed, connection_traffic, connection_time, server_delay, connected_server_delay, connection_mode, core_version;
    private EditText uuid_input;
    private BroadcastReceiver v2rayBroadCastReceiver;

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Toast.makeText(this, "UUID is invalid!", Toast.LENGTH_SHORT).show();
                return;
            }

            String config = "[{\\\"v\\\":\\\"2\\\",\\\"ps\\\":\\\"Turkey\\\",\\\"add\\\":\\\"185.143.234.120\\\",\\\"port\\\":\\\"443\\\",\\\"id\\\":\\\"" + userUUID + "\\\",\\\"aid\\\":\\\"0\\\",\\\"net\\\":\\\"ws\\\",\\\"type\\\":\\\"none\\\",\\\"host\\\":\\\"app.alnafun.ir\\\",\\\"path\\\":\\\"/\\\",\\\"tls\\\":\\\"tls\\\",\\\"sni\\\":\\\"iau.ac.ir\\\"}]";

            if (V2rayController.getConnectionState() == CONNECTION_STATES.DISCONNECTED) {
                V2rayController.startV2ray(this, "Turkey", config, null);
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
            String userUUID = uuid_input.getText().toString().trim();

            if (userUUID.length() < 30 || !userUUID.matches("^[0-9a-fA-F\\-]{36}$")) {
                Toast.makeText(this, "UUID is invalid!", Toast.LENGTH_SHORT).show();
                return;
            }

            String config = "[{\\\"v\\\":\\\"2\\\",\\\"ps\\\":\\\"Turkey\\\",\\\"add\\\":\\\"185.143.234.120\\\",\\\"port\\\":\\\"443\\\",\\\"id\\\":\\\"" + userUUID + "\\\",\\\"aid\\\":\\\"0\\\",\\\"net\\\":\\\"ws\\\",\\\"type\\\":\\\"none\\\",\\\"host\\\":\\\"app.alnafun.ir\\\",\\\"path\\\":\\\"/\\\",\\\"tls\\\":\\\"tls\\\",\\\"sni\\\":\\\"iau.ac.ir\\\"}]";

            server_delay.setText("server delay : measuring...");
            new Handler().postDelayed(() ->
                    server_delay.setText("server delay : " + V2rayController.getV2rayServerDelay(config) + "ms"), 200);
        });

        connection_mode.setOnClickListener(view -> {
            V2rayController.toggleConnectionMode();
            connection_mode.setText("connection mode : " + V2rayConfigs.serviceMode.toString());
        });

        switch (V2rayController.getConnectionState()) {
            case CONNECTED:
                connection.setText("CONNECTED");
                connected_server_delay.callOnClick();
                break;
            case DISCONNECTED:
                connection.setText("DISCONNECTED");
                break;
            case CONNECTING:
                connection.setText("CONNECTING");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
        }
    }
}
