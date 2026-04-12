package com.example.myapplication.MQTT;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

public class MQTTHandler {

    private MqttAndroidClient client;
    private MQTTCallback callback;

    public interface MQTTCallback {
        void onMessageReceived(String topic, String message);
    }

    public void setCallback(MQTTCallback callback) {
        this.callback = callback;
    }

    public void connect(Context context) {
        String serverUri = "ssl://b46a1e0912534437b2b78880fc3cf93a.s1.eu.hivemq.cloud:8883";
        String clientId = MqttClient.generateClientId();

        client = new MqttAndroidClient(context, serverUri, clientId);

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("Hungnguyen221104");
            options.setPassword("Hung221104@".toCharArray());
            options.setCleanSession(true);

            IMqttToken token = client.connect(options);

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Connected Cloud");

                    // 🔥 subscribe trạng thái đèn
                    subscribe("esp32/lamp/status");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Failed: " + exception);
                }
            });

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());

                    Log.d("MQTT", "Received: " + msg);

                    if (callback != null) {
                        callback.onMessageReceived(topic, msg);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic, 1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 🔥 gửi dữ liệu
    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                mqttMessage.setQos(1);

                client.publish(topic, mqttMessage);

                Log.d("MQTT", "Sent: " + message);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 🔥 bật/tắt đèn
    public void controlLamp(boolean isOn) {
        String topic = "esp32/lamp";
        String message = isOn ? "ON" : "OFF";

        publish(topic, message);
    }
}