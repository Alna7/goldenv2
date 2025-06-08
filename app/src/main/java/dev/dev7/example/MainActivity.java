package dev.dev7.example;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dev.dev7.lib.v2ray.V2rayController;

public class MainActivity extends AppCompatActivity {

    private EditText uuidEditText;
    private Button connectButton, disconnectButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uuidEditText = findViewById(R.id.uuidEditText);
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedUuid = prefs.getString("uuid", "");
        uuidEditText.setText(savedUuid);

        connectButton.setOnClickListener(v -> {
            String uuid = uuidEditText.getText().toString().trim();
            if (uuid.isEmpty()) {
                Toast.makeText(this, "لطفاً UUID را وارد کنید", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putString("uuid", uuid).apply();

            String configUrl = "vless://" + uuid +
                    "@185.143.234.120:443?type=ws&host=app.alnafun.ir" +
                    "&path=/?ed%3D443&security=tls&sni=iau.ac.ir";

            V2rayController.startV2ray(this, "MyConfig", configUrl, null);
            Toast.makeText(this, "در حال اتصال...", Toast.LENGTH_SHORT).show();
        });

        disconnectButton.setOnClickListener(v -> {
            V2rayController.stopV2ray(this);
            Toast.makeText(this, "اتصال قطع شد", Toast.LENGTH_SHORT).show();
        });
    }
}
