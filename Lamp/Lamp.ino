#include <WiFi.h>
#include <PubSubClient.h>
#include <WiFiClientSecure.h>

// ===== WIFI =====
const char* ssid = "Tang 4";
const char* password = "88888888";

// ===== MQTT (HiveMQ Cloud) =====
const char* mqtt_server = "b46a1e0912534437b2b78880fc3cf93a.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;
const char* mqtt_user = "Hungnguyen221104";
const char* mqtt_pass = "Hung221104@";

// ===== OBJECT =====
WiFiClientSecure espClient;
PubSubClient client(espClient);

// ===== LED =====
int ledPin = 4;

// ===== CALLBACK =====
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.println("\n===== CALLBACK RECEIVED =====");

  Serial.print("Topic: ");
  Serial.println(topic);

  String msg = "";
  for (int i = 0; i < length; i++) {
    msg += (char)payload[i];
  }

  msg.trim();

  Serial.print("Message: ");
  Serial.println(msg);

  if (msg == "ON") {
    Serial.println("===> TURN ON LED");
    digitalWrite(ledPin, HIGH);
    client.publish("esp32/lamp/status", "ON");
  } 
  else if (msg == "OFF") {
    Serial.println("===> TURN OFF LED");
    digitalWrite(ledPin, LOW);
    client.publish("esp32/lamp/status", "OFF");
  } 
  else {
    Serial.println("===> UNKNOWN COMMAND");
  }
}

// ===== CONNECT WIFI =====
void setup_wifi() {
  delay(10);
  Serial.println("\nConnecting to WiFi...");
  
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected!");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// ===== RECONNECT MQTT =====
void reconnect() {
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");

    if (client.connect("ESP32Client", mqtt_user, mqtt_pass)) {
      Serial.println(" SUCCESS!");

      client.subscribe("esp32/lamp");
      Serial.println("Subscribed to: esp32/lamp");

      client.publish("esp32/status", "ESP32 Connected");
    } else {
      Serial.print(" FAILED, rc=");
      Serial.print(client.state());
      Serial.println(" -> retry in 2s");
      delay(2000);
    }
  }
}

// ===== SETUP =====
void setup() {
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);

  Serial.println("ESP32 START");

  setup_wifi();

  // Bỏ verify SSL (cho dễ test)
  espClient.setInsecure();

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

// ===== LOOP =====
void loop() {
  if (!client.connected()) {
    reconnect();
  }

  client.loop();
  delay(10);
}