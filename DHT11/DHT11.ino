#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// ===== DHT =====
#define DHTPIN 4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== OLED =====
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// ===== WiFi =====
const char* ssid = "Tang 4";
const char* password = "88888888";

// ===== MQTT =====
const char* mqtt_server = "b46a1e0912534437b2b78880fc3cf93a.s1.eu.hivemq.cloud";
const int mqtt_port = 8883;

const char* mqtt_user = "Hungnguyen221104";
const char* mqtt_pass = "Hung221104@";

WiFiClientSecure espClient;
PubSubClient client(espClient);

// ===== Connect MQTT =====
void connectMQTT() {
  while (!client.connected()) {
    Serial.print("Connecting MQTT...");

    if (client.connect("ESP32S3Client", mqtt_user, mqtt_pass)) {
      Serial.println("Connected MQTT!");
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" -> retry...");
      delay(2000);
    }
  }
}

// ===== SETUP =====
void setup() {
  Serial.begin(115200);

  // ===== DHT =====
  dht.begin();

  // ===== OLED =====
  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println("OLED not found");
    while (true);
  }

  display.clearDisplay();
  display.setTextColor(WHITE);

  // ===== WiFi =====
  WiFi.begin(ssid, password);
  Serial.print("Connecting WiFi");

  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("Connecting WiFi...");
  display.display();

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nWiFi connected");
  Serial.println(WiFi.localIP());

  // ===== MQTT =====
  espClient.setInsecure();   
  client.setServer(mqtt_server, mqtt_port);
}

// ===== LOOP =====
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

    // ===== MQTT Publish =====
    client.publish("esp32/dht11/temperature", temp.c_str());
    client.publish("esp32/dht11/humidity", hum.c_str());

    // ===== OLED DISPLAY =====
    display.clearDisplay();

    display.setTextSize(1);
    display.setCursor(0, 0);
    display.println("ESP32-S3 MQTT");

    display.setCursor(0, 10);
    display.print("IP: ");
    display.println(WiFi.localIP());

    display.setTextSize(2);
    display.setCursor(0, 25);
    display.print(t);
    display.println(" C");

    display.setCursor(0, 48);
    display.print(h);
    display.println(" %");

    display.display();

    // ===== Serial Debug =====
    Serial.println("===== MQTT SENT =====");
    Serial.println("Temp: " + temp + " C");
    Serial.println("Hum : " + hum + " %");
  }
}