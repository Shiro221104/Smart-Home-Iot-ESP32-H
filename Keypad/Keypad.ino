#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <Keypad.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <U8g2lib.h>

// ===== WiFi =====
#define WIFI_SSID "Quang Thu"
#define WIFI_PASSWORD "1000000000"

// ===== Firebase =====
#define API_KEY "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"

// ===== Device ID =====
String deviceId = "-OrJEzAul-5Cr1Hm-ptf";

// Firebase objects
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ===== OLED =====
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ===== Keypad =====
#define ROW_NUM 4
#define COLUMN_NUM 4

char keys[ROW_NUM][COLUMN_NUM] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};

byte pin_rows[ROW_NUM] = {23, 14, 27, 26};
byte pin_column[COLUMN_NUM] = {25, 33, 32, 19};

Keypad keypad = Keypad(makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM);

// ===== Password =====
String correctPassword = "1234";
String inputPassword = "";
int maxLength = 4;

// ===== Servo =====
Servo myServo;
int servoPin = 18;

// ===== State =====
String lastStatus = "";

// ===== OLED =====
void showMessage(String l1, String l2 = "") {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(0, 20, l1.c_str());
  u8g2.drawStr(0, 40, l2.c_str());
  u8g2.sendBuffer();
}

// ===== OPEN DOOR =====
void openDoor(String source) {
  Serial.println("🚪 Opening door from: " + source);

  showMessage(source, "Opening...");

  myServo.write(90);
  delay(3000);
  myServo.write(0);

  // reset Firebase về OFF
  if (Firebase.RTDB.setString(&fbdo, "/devices/" + deviceId + "/status", "OFF")) {
    Serial.println("✅ Reset status OK");
  } else {
    Serial.println("❌ Reset FAILED");
    Serial.println(fbdo.errorReason());
  }
}

// ===== Setup =====
void setup() {
  Serial.begin(115200);

  // ===== WiFi =====
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("🔄 Connecting WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ WiFi Connected");

  // ===== Firebase config =====
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  // ===== 🔥 FIX QUAN TRỌNG (Anonymous login) =====
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("✅ Anonymous Login OK");
  } else {
    Serial.printf("❌ SignUp FAILED: %s\n", config.signer.signupError.message.c_str());
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  delay(1000);

  if (Firebase.ready()) {
    Serial.println("🔥 Firebase READY");
  } else {
    Serial.println("❌ Firebase NOT READY");
  }

  // ===== OLED =====
  u8g2.begin();
  showMessage("DOOR LOCK", "Enter Password");

  // ===== Servo =====
  myServo.attach(servoPin);
  myServo.write(0);
}

// ===== Loop =====
void loop() {

  // ===== Firebase đọc status =====
  if (Firebase.RTDB.getString(&fbdo, "/devices/" + deviceId + "/status")) {

    String status = fbdo.stringData();

    Serial.print("📡 Status: ");
    Serial.println(status);

    if (status != lastStatus) {
      lastStatus = status;

      if (status == "ON") {
        openDoor("App");
      }
    }

  } else {
    Serial.println("❌ Firebase FAILED");
    Serial.println(fbdo.errorReason());
  }

  // ===== Keypad =====
  char key = keypad.getKey();

  if (key != NO_KEY) {

    if (key == '*') {
      inputPassword = "";
      showMessage("Cleared", "Enter Again");
      return;
    }

    if (inputPassword.length() < maxLength) {
      inputPassword += key;

      String stars = "";
      for (int i = 0; i < inputPassword.length(); i++) {
        stars += "*";
      }

      showMessage("Input:", stars);
    }

    if (inputPassword.length() == maxLength) {

      if (inputPassword == correctPassword) {
        openDoor("Password");
      } else {
        showMessage("Wrong Password");
        delay(2000);
      }

      inputPassword = "";
      showMessage("Enter Password");
    }
  }

  delay(500);
}