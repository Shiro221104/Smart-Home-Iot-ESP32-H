#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <WiFiClientSecure.h>

// ===== WIFI =====
const char* ssid = "Quang Thu";
const char* password = "1000000000";


// ===== Firebase =====
#define API_KEY "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"
// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ===== GPIO =====
#define LIVING_LED 4
#define KITCHEN_LED 5
#define BEDROOM_LED 18

// ===== WIFI =====
void setup_wifi() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);

  Serial.print("Connecting WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nConnected!");
}

// ===== SETUP =====
void setup() {

  pinMode(LIVING_LED, OUTPUT);
  pinMode(KITCHEN_LED, OUTPUT);
  pinMode(BEDROOM_LED, OUTPUT);

  setup_wifi();

  // Firebase config
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

// ===== HANDLE DEVICE =====
void handleDevice(String room, String type, String status) {

  if (type != "light") return;

  int pin = -1;

  if (room == "livingroom") pin = LIVING_LED;
  else if (room == "kitchen") pin = KITCHEN_LED;
  else if (room == "bedroom") pin = BEDROOM_LED;

  if (pin == -1) return;

  if (status == "ON") {
    digitalWrite(pin, HIGH);
  } else {
    digitalWrite(pin, LOW);
  }

  Serial.println("Room: " + room + " -> " + status);
}

// ===== LOOP =====
void loop() {

  if (Firebase.RTDB.getJSON(&fbdo, "/devices")) {

    FirebaseJson &json = fbdo.jsonObject();
    size_t len = json.iteratorBegin();

    String key, value;

    for (size_t i = 0; i < len; i++) {

      int type;
      json.iteratorGet(i, type, key, value);

      FirebaseJson device;
      device.setJsonData(value);

      String room, devType, status;

      device.get(room, "room");
      device.get(devType, "type");
      device.get(status, "status");

      handleDevice(room, devType, status);
    }

    json.iteratorEnd();
  }

  delay(1000);
}