#include <Wire.h>
#include <U8g2lib.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <DHT.h>

// ===== OLED =====
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ===== WIFI =====
#define WIFI_SSID "Tang 4"
#define WIFI_PASSWORD "88888888"

// ===== FIREBASE =====
#define FIREBASE_HOST "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app"

// ===== DHT =====
#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== MQ2 =====
#define MQ2_A0 5
#define MQ2_D0 2

void setup() {
  Serial.begin(115200);

  // OLED I2C (ESP32-S3)
  Wire.begin(8, 9);
  u8g2.begin();

  // SENSOR
  dht.begin();
  pinMode(MQ2_D0, INPUT);

  // WIFI
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi OK");
}

void loop() {

  // ===== READ SENSOR =====
  float t = dht.readTemperature();
  float h = dht.readHumidity();

  int gas = analogRead(MQ2_A0);
  int gasAlert = digitalRead(MQ2_D0);

  String gasStatus;
  if (gas < 300) gasStatus = "SAFE";
  else if (gas < 600) gasStatus = "WARNING";
  else gasStatus = "DANGER";

  // ===== OLED DISPLAY =====
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);

  u8g2.drawStr(0, 15, "ESP32-S3 SENSOR");

  String tempStr = "Temp: " + String(t) + " C";
  String humStr  = "Hum: " + String(h) + " %";
  String gasStr  = "Gas: " + String(gasStatus);

  u8g2.drawStr(0, 35, tempStr.c_str());
  u8g2.drawStr(0, 50, humStr.c_str());
  u8g2.drawStr(0, 64, gasStr.c_str());

  u8g2.sendBuffer();

  // ===== FIREBASE =====
  String json = "{";
  json += "\"temperature\":" + String(t) + ",";
  json += "\"humidity\":" + String(h) + ",";
  json += "\"gas_value\":" + String(gas) + ",";
  json += "\"gas_detected\":" + String(gasAlert) + ",";
  json += "\"gas_status\":\"" + gasStatus + "\"";
  json += "}";

  String url = String(FIREBASE_HOST) + "/sensors/device1/current.json";

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  http.begin(client, url);
  http.addHeader("Content-Type", "application/json");

  int httpResponseCode = http.PUT(json);

  Serial.println("HTTP Code: " + String(httpResponseCode));

  http.end();

  delay(3000);
}