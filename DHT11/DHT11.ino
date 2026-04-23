#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <DHT.h>

// WIFI
#define WIFI_SSID "Tang 4"
#define WIFI_PASSWORD "88888888"

// FIREBASE
#define FIREBASE_HOST "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app"

// SENSOR
#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

#define MQ2_A0 5
#define MQ2_D0 2

void setup() {
  Serial.begin(115200);
  dht.begin();
  pinMode(MQ2_D0, INPUT);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\n✅ WiFi OK");
}

void loop() {

  float t = dht.readTemperature();
  float h = dht.readHumidity();

  int gas = analogRead(MQ2_A0);
  int gasAlert = digitalRead(MQ2_D0);

  String gasStatus;
  if (gas < 300) gasStatus = "SAFE";
  else if (gas < 600) gasStatus = "WARNING";
  else gasStatus = "DANGER";

  String json = "{";
  json += "\"temperature\":" + String(t) + ",";
  json += "\"humidity\":" + String(h) + ",";
  json += "\"gas_value\":" + String(gas) + ",";
  json += "\"gas_detected\":" + String(gasAlert) + ",";
  json += "\"gas_status\":\"" + gasStatus + "\"";
  json += "}";

  String url = String(FIREBASE_HOST) + "/sensors/device1/current.json";

  // 🔥 FIX SSL
  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  http.begin(client, url);
  http.addHeader("Content-Type", "application/json");

  int httpResponseCode = http.PUT(json);

  Serial.println("HTTP Code: " + String(httpResponseCode));

  if (httpResponseCode > 0) {
    Serial.println("✅ Firebase OK");
  } else {
    Serial.println("❌ Firebase FAIL");
  }

  http.end();

  delay(3000);
}