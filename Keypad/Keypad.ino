#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <Keypad.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <U8g2lib.h>

// =====================================================
// WIFI
// =====================================================
#define WIFI_SSID "ESP32_Hotspot"
#define WIFI_PASSWORD "12345678"

// =====================================================
// FIREBASE
// =====================================================
#define API_KEY "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"

// =====================================================
// DEVICE ID
// =====================================================
String deviceId = "-OrJEzAul-5Cr1Hm-ptf";

// =====================================================
// FIREBASE OBJECTS
// =====================================================
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// =====================================================
// OLED SH1106
// =====================================================
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// =====================================================
// KEYPAD
// =====================================================
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

Keypad keypad = Keypad(makeKeymap(keys),
                       pin_rows,
                       pin_column,
                       ROW_NUM,
                       COLUMN_NUM);

// =====================================================
// PASSWORD
// =====================================================
String correctPassword = "1234";
String inputPassword = "";
int maxLength = 4;

// =====================================================
// SERVO
// =====================================================
Servo myServo;
int servoPin = 18;

// =====================================================
// STATE
// =====================================================
String lastStatus = "";

// =====================================================
// OLED FUNCTION
// =====================================================
void showMessage(String line1, String line2 = "") {

  u8g2.clearBuffer();

  u8g2.setFont(u8g2_font_ncenB08_tr);

  u8g2.drawStr(0, 20, line1.c_str());
  u8g2.drawStr(0, 40, line2.c_str());

  u8g2.sendBuffer();
}

// =====================================================
// FIREBASE UPDATE STATUS
// =====================================================
void updateStatus(String status) {

  String path = "/devices/" + deviceId + "/status";

  if (Firebase.RTDB.setString(&fbdo, path, status)) {

    Serial.print("✅ Status -> ");
    Serial.println(status);

  } else {

    Serial.println("❌ Firebase Update FAILED");
    Serial.println(fbdo.errorReason());
  }
}

// =====================================================
// OPEN DOOR
// =====================================================
void openDoor(String source) {

  Serial.println("================================");
  Serial.println("🚪 OPEN DOOR");
  Serial.println("📌 Source: " + source);

  // ===== OLED =====
  showMessage(source, "Door OPEN");

  // ===== Firebase status =====
  updateStatus("ON");

  // ===== Open Servo =====
  myServo.write(90);

  Serial.println("🔓 Door Opened");

  // ===== Keep door open =====
  delay(5000);

  // ===== Closing =====
  showMessage("Closing Door");

  Serial.println("🔒 Closing Door");

  myServo.write(0);

  delay(1000);

  // ===== Reset status =====
  updateStatus("OFF");

  // ===== OLED =====
  showMessage("Enter Password");

  Serial.println("✅ Door Closed");
  Serial.println("================================");
}

// =====================================================
// SETUP
// =====================================================
void setup() {

  Serial.begin(115200);

  // =====================================================
  // WIFI
  // =====================================================
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("🔄 Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {

    Serial.print(".");
    delay(500);
  }

  Serial.println();
  Serial.println("✅ WiFi Connected");

  // =====================================================
  // FIREBASE CONFIG
  // =====================================================
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  // =====================================================
  // ANONYMOUS LOGIN
  // =====================================================
  if (Firebase.signUp(&config, &auth, "", "")) {

    Serial.println("✅ Firebase Anonymous Login OK");

  } else {

    Serial.println("❌ Firebase Login FAILED");
    Serial.println(config.signer.signupError.message.c_str());
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  delay(1000);

  if (Firebase.ready()) {

    Serial.println("🔥 Firebase READY");

  } else {

    Serial.println("❌ Firebase NOT READY");
  }

  // =====================================================
  // OLED
  // =====================================================
  u8g2.begin();

  showMessage("DOOR LOCK", "Enter Password");

  // =====================================================
  // SERVO
  // =====================================================
  myServo.attach(servoPin);

  myServo.write(0);

  // =====================================================
  // INIT STATUS
  // =====================================================
  updateStatus("OFF");
}

// =====================================================
// LOOP
// =====================================================
void loop() {

  // =====================================================
  // READ FIREBASE STATUS
  // =====================================================
  String path = "/devices/" + deviceId + "/status";

  if (Firebase.RTDB.getString(&fbdo, path)) {

    String status = fbdo.stringData();

    Serial.print("📡 Firebase Status: ");
    Serial.println(status);

    // ===== Detect ON =====
    if (status == "ON" && lastStatus != "ON") {

      openDoor("App");
    }

    lastStatus = status;

  } else {

    Serial.println("❌ Firebase Read FAILED");
    Serial.println(fbdo.errorReason());
  }

  // =====================================================
  // KEYPAD
  // =====================================================
  char key = keypad.getKey();

  if (key != NO_KEY) {

    Serial.print("⌨ Key: ");
    Serial.println(key);

    // ===== Clear =====
    if (key == '*') {

      inputPassword = "";

      showMessage("Cleared", "Enter Again");

      delay(1000);

      showMessage("Enter Password");

      return;
    }

    // ===== Add character =====
    if (inputPassword.length() < maxLength) {

      inputPassword += key;

      String stars = "";

      for (int i = 0; i < inputPassword.length(); i++) {
        stars += "*";
      }

      showMessage("Input:", stars);
    }

    // ===== Check password =====
    if (inputPassword.length() == maxLength) {

      if (inputPassword == correctPassword) {

        Serial.println("✅ Correct Password");

        openDoor("Password");

      } else {

        Serial.println("❌ Wrong Password");

        showMessage("Wrong Password");

        delay(2000);
      }

      // ===== Reset input =====
      inputPassword = "";

      showMessage("Enter Password");
    }
  }

  delay(300);
}