#include <WiFi.h>
#include <PubSubClient.h>

const char* ssid = "YOUR_WIFI";
const char* password = "YOUR_PASS";

const char* mqtt_server = "b46a1e0912534437b2b78880fc3cf93a.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_user = "Hungnguyen221104";
const char* mqtt_pass = "Hung221104@";

WiFiClientSecure espClient;
PubSubClient client(espClient);

int ledPin = 2;

void callback(char* topic, byte* payload, unsigned int length) {
  String msg = "";

  for (int i = 0; i < length; i++) {
    msg += (char)payload[i];
  }

  if (msg == "ON") {
    digitalWrite(ledPin, HIGH);
    client.publish("esp32/lamp/status", "ON");
  } else {
    digitalWrite(ledPin, LOW);
    client.publish("esp32/lamp/status", "OFF");
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) delay(500);

  espClient.setInsecure(); // 🔥 cho HiveMQ Cloud

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void reconnect() {
  while (!client.connected()) {
    if (client.connect("ESP32Client", mqtt_user, mqtt_pass)) {
      client.subscribe("esp32/lamp");
    } else {
      delay(2000);
    }
  }
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop();
}