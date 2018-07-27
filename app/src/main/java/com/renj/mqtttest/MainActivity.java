package com.renj.mqtttest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.renj.rqottlibrary.RMqttServer;
import com.renj.rqottlibrary.RMqttServerAdapter;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;

    private EditText etSendContent;
    private Button btSendMessage;
    private Button btSubscribe;
    private Button btUnsubscribe;

    private RMqttServer rMqttServer;

    private String serverUrl = "tcp://156.23.4.8:1883";
    private String imei = "test_id";
    private String topic = "r_test";
    private int qo = 1;
    private String username = "your username";
    private String password = "your admin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
        }

        etSendContent = findViewById(R.id.et_send_content);
        btSendMessage = findViewById(R.id.bt_send_message);
        btSubscribe = findViewById(R.id.bt_subscribe);
        btUnsubscribe = findViewById(R.id.bt_unsubscribe);

        btSendMessage.setOnClickListener(this);
        btSubscribe.setOnClickListener(this);
        btUnsubscribe.setOnClickListener(this);

        initMqtt();
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();

        switch (vId) {
            case R.id.bt_send_message:
                String messageContent = etSendContent.getText().toString().trim();
                if (TextUtils.isEmpty(messageContent)) {
                    Toast.makeText(MainActivity.this, "请输入内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                rMqttServer.publish(messageContent);
                break;
            case R.id.bt_subscribe:
                rMqttServer.subscribe(topic, 1);
                break;
            case R.id.bt_unsubscribe:
                rMqttServer.unsubscribe(topic);
                break;
        }
    }

    private void initMqtt() {
        rMqttServer = new RMqttServer.Builder()
                // mqtt服务器地址 格式例如：tcp://10.0.261.159:1883
                .serverUrl(serverUrl)
                // 主题和策略
                .topic(topic, qo)
                // 设置自动重连
                .autoReconnect(true)
                // 设置不清除回话session 可收到服务器之前发出的推送消息
                .clearSession(false)
                // 唯一标识
                .clientId(imei)
                // 用户名、密码
                .userName(username)
                .passWord(password)
                // 心跳包默认的发送间隔
                .keepAliveInterval(20)
                // 构建出RMqttServer 建议用application的context
                .build(this.getApplicationContext());

        rMqttServer.setRMqttServerAdapter(new RMqttServerAdapter() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // 推送消息到达
                String result = new String(message.getPayload());
                Log.i("MainActivity", "推送消息到达 => " + result);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MY_PERMISSIONS_REQUEST_READ_PHONE_STATE == requestCode) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_PHONE_STATE.equals(permissions[i])) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // 获取手机IMEI号
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    imei = telephonyManager.getDeviceId();
                    return;
                } else {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        rMqttServer.disconnect();
//        rMqttServer.close();
    }
}
