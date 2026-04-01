#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <DHT.h>

// ===== DHT =====
#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== WiFi =====
const char* ssid = "TP-LINK_CC01B8";
const char* password = "99990000";

// ===== HiveMQ =====
const char* mqtt_server = "b46a1e0912534437b2b78880fc3cf93a.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;

// ===== Credentials (từ ảnh của bạn) =====
const char* mqtt_user = "Hungnguyen221104";
const char* mqtt_pass = "Hung221104@";

WiFiClientSecure espClient;
PubSubClient client(espClient);

// ===== Connect MQTT =====
void connectMQTT() {
  while (!client.connected()) {
    Serial.print("Connecting MQTT...");

    if (client.connect("ESP32Client", mqtt_user, mqtt_pass)) {
      Serial.println("Connected MQTT!");
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" -> retry...");
      delay(2000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  dht.begin();

  // ===== Connect WiFi =====
  WiFi.begin(ssid, password);
  Serial.print("Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected");
  Serial.println(WiFi.localIP());

  // ===== MQTT Setup =====
  espClient.setInsecure();   // 🔥 BẮT BUỘC
  client.setServer(mqtt_server, mqtt_port);
}

void loop() {
  if (!client.connected()) {
    connectMQTT();
  }
  client.loop();

  static unsigned long lastMsg = 0;

  if (millis() - lastMsg > 5000) {
    lastMsg = millis();

    float t = dht.readTemperature();
    float h = dht.readHumidity();

    if (isnan(t) || isnan(h)) {
      Serial.println("DHT read failed!");
      return;
    }

    String temp = String(t);
    String hum = String(h);

    // ===== Publish =====
    client.publish("esp32/dht11/temperature", temp.c_str());
    client.publish("esp32/dht11/humidity", hum.c_str());

    // ===== Debug =====
    Serial.println("===== MQTT SENT =====");
    Serial.println("Temp: " + temp + " °C");
    Serial.println("Hum : " + hum + " %");
  }
}