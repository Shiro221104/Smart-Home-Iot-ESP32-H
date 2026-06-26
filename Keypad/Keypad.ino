// =====================================================
// SMART HOME - ESP32 MERGED
// Keypad Door Lock + Lamp Control + Notifications
// Firebase: users/{userId}/devices/{deviceId}/status
// =====================================================

#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <Keypad.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <U8g2lib.h>
#include <time.h>

// =====================================================
// WIFI
// =====================================================
#define WIFI_SSID     "ESP32_Hotspot"
#define WIFI_PASSWORD "12345678"

// =====================================================
// FIREBASE
// =====================================================
#define API_KEY       "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL  "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL    "Hungnguyen221104@gmail.com"
#define USER_PASSWORD "123456"

// =====================================================
// USER ID
// =====================================================
#define USER_ID "fWn2xoXCpDWoJyCV1yt3FXxLgMi2"

// =====================================================
// DEVICE IDs
// =====================================================
String doorID    = "-OvFphAM62NlYeQJ7agh";
String livingID  = "-OvKwCdcMamePNGjS0Ku";
String bedroomID = "-OvLCcClf9EkFZttj-FF";

// =====================================================
// FIREBASE OBJECTS
// =====================================================
FirebaseData fbdo;        // doc/ghi status
FirebaseData fbdo_noti;   // push notification (rieng tranh xung dot)
FirebaseAuth auth;
FirebaseConfig config;

// =====================================================
// HELPER: path device
// =====================================================
String devicePath(String deviceId) {
  return "/users/" + String(USER_ID) + "/devices/" + deviceId + "/status";
}

// =====================================================
// OLED SH1106
// =====================================================
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// =====================================================
// KEYPAD 4x4
// =====================================================
#define ROW_NUM    4
#define COLUMN_NUM 4

char keys[ROW_NUM][COLUMN_NUM] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};

byte pin_rows[ROW_NUM]      = {23, 14, 27, 26};
byte pin_column[COLUMN_NUM] = {25, 33, 32, 19};

Keypad keypad = Keypad(makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM);

// =====================================================
// PASSWORD (doc tu Firebase)
// =====================================================
String correctPassword = "";
String inputPassword   = "";
int    maxLength       = 4;

unsigned long lastPasswordFetch = 0;
#define PASSWORD_REFRESH_MS 30000

// =====================================================
// SERVO
// =====================================================
Servo myServo;
#define SERVO_PIN 18

// =====================================================
// LED LAMP
// =====================================================
#define LIVING_LED   4
#define BEDROOM_LED  5

// =====================================================
// BUTTON vat ly
// =====================================================
#define BTN_LIVING   13
#define BTN_BEDROOM  12   // doi sang 12 vi 27 trung voi keypad row

// =====================================================
// STATE
// =====================================================
String lastDoorStatus = "";
bool   livingState    = false;
bool   bedroomState   = false;

// =====================================================
// OLED: hien thi 2 dong
// =====================================================
void showMessage(String line1, String line2 = "") {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(0, 20, line1.c_str());
  if (line2 != "") u8g2.drawStr(0, 40, line2.c_str());
  u8g2.sendBuffer();
}

// =====================================================
// PUSH NOTIFICATION
// Path: /users/{uid}/notifications/{push_key}
// Cau truc: { title, message, time, userId }
// =====================================================
void pushNotification(String title, String message) {
  if (!Firebase.ready()) return;

  String notiPath = "/users/" + String(USER_ID) + "/notifications";

  FirebaseJson notiJson;
  notiJson.set("title",   title);
  notiJson.set("message", message);
  notiJson.set("userId",  String(USER_ID));

  time_t t_now; time(&t_now);
  if (t_now > 100000) notiJson.set("time", (int)t_now);

  if (Firebase.RTDB.pushJSON(&fbdo_noti, notiPath, &notiJson))
    Serial.println(">> Notification OK: [" + title + "] " + fbdo_noti.pushName());
  else
    Serial.println("!! Notification ERR: " + fbdo_noti.errorReason());
}

// =====================================================
// FIREBASE: ghi status
// =====================================================
void updateStatus(String deviceId, String status) {
  String path = devicePath(deviceId);
  if (Firebase.RTDB.setString(&fbdo, path, status))
    Serial.println(">> " + deviceId + " -> " + status);
  else
    Serial.println("!! Update failed: " + fbdo.errorReason());
}

// =====================================================
// FIREBASE: doc password cua
// Path: /users/{uid}/devices/{doorID}/password
// =====================================================
void loadDoorPassword() {
  if (!Firebase.ready()) return;
  String path = "/users/" + String(USER_ID) + "/devices/" + doorID + "/password";
  if (Firebase.RTDB.getString(&fbdo, path)) {
    correctPassword = fbdo.stringData();
    Serial.println(">> Password loaded: " + correctPassword);
  } else {
    Serial.println("!! Load password failed: " + fbdo.errorReason());
  }
}

// =====================================================
// DOOR: mo cua
// =====================================================
void openDoor(String source) {
  Serial.println(">> MO CUA | Source: " + source);
  showMessage(source, "Door OPEN");
  updateStatus(doorID, "ON");

  // Notification chi khi nhan mat khau thu cong
  if (source == "Password") {
    pushNotification("THIẾT BỊ BẬT", "Cửa chính đã được mở bằng mật khẩu.");
  }

  myServo.write(90);
  delay(5000);

  showMessage("Closing...");
  myServo.write(0);
  delay(1000);

  updateStatus(doorID, "OFF");
  showMessage("Enter Password");
  Serial.println(">> Cua dong");
}

// =====================================================
// LAMP: ap dung trang thai LED
// =====================================================
void applyLamp(String deviceId, int pin, bool &stateVar, String status) {
  bool on = (status == "ON");
  if (stateVar == on) return;
  stateVar = on;
  digitalWrite(pin, on ? HIGH : LOW);
  Serial.println(">> Den " + deviceId + " -> " + status);
}

// =====================================================
// FIREBASE: doc trang thai den
// =====================================================
void readLamps() {
  if (!Firebase.ready()) return;

  if (Firebase.RTDB.getString(&fbdo, devicePath(livingID)))
    applyLamp(livingID, LIVING_LED, livingState, fbdo.stringData());

  if (Firebase.RTDB.getString(&fbdo, devicePath(bedroomID)))
    applyLamp(bedroomID, BEDROOM_LED, bedroomState, fbdo.stringData());
}

// =====================================================
// BUTTON: bat/tat den + push notification
// =====================================================
void handleButtons() {

  // --- LIVING ROOM ---
  if (digitalRead(BTN_LIVING) == LOW) {
    delay(200);
    if (digitalRead(BTN_LIVING) == LOW) {
      livingState = !livingState;
      String s = livingState ? "ON" : "OFF";
      digitalWrite(LIVING_LED, livingState ? HIGH : LOW);
      updateStatus(livingID, s);

      // Push notification
      if (livingState)
        pushNotification("THIẾT BỊ BẬT", "Đèn phòng khách đã được bật.");
      else
        pushNotification("THIẾT BỊ TẮT", "Đèn phòng khách đã được tắt.");

      showMessage("Phong khach:", s);
      delay(800);
      showMessage("Enter Password");
      while (digitalRead(BTN_LIVING) == LOW);
    }
  }

  // --- BEDROOM ---
  if (digitalRead(BTN_BEDROOM) == LOW) {
    delay(200);
    if (digitalRead(BTN_BEDROOM) == LOW) {
      bedroomState = !bedroomState;
      String s = bedroomState ? "ON" : "OFF";
      digitalWrite(BEDROOM_LED, bedroomState ? HIGH : LOW);
      updateStatus(bedroomID, s);

      // Push notification
      if (bedroomState)
        pushNotification("THIẾT BỊ BẬT", "Đèn phòng ngủ đã được bật.");
      else
        pushNotification("THIẾT BỊ TẮT", "Đèn phòng ngủ đã được tắt.");

      showMessage("Phong ngu:", s);
      delay(800);
      showMessage("Enter Password");
      while (digitalRead(BTN_BEDROOM) == LOW);
    }
  }
}

// =====================================================
// FIREBASE: kiem tra lenh mo cua tu App
// =====================================================
void checkDoorCommand() {
  if (!Firebase.ready()) return;

  if (Firebase.RTDB.getString(&fbdo, devicePath(doorID))) {
    String status = fbdo.stringData();
    if (status == "ON" && lastDoorStatus != "ON") {
      openDoor("App");
    }
    lastDoorStatus = status;
  }
}

// =====================================================
// KEYPAD: xu ly nhap mat khau
// =====================================================
void handleKeypad() {
  char key = keypad.getKey();
  if (key == NO_KEY) return;

  Serial.println("Key: " + String(key));

  if (key == '*') {
    inputPassword = "";
    showMessage("Cleared", "Enter Again");
    delay(1000);
    showMessage("Enter Password");
    return;
  }

  if ((int)inputPassword.length() < maxLength) {
    inputPassword += key;
    String stars = "";
    for (int i = 0; i < (int)inputPassword.length(); i++) stars += "*";
    showMessage("Input:", stars);
  }

  if ((int)inputPassword.length() == maxLength) {
    if (correctPassword == "") {
      showMessage("No Password", "Check Firebase");
      delay(2000);
    } else if (inputPassword == correctPassword) {
      Serial.println(">> Mat khau dung!");
      openDoor("Password");
    } else {
      Serial.println(">> Mat khau sai!");
      showMessage("Wrong!", "Try again");
      delay(2000);
    }
    inputPassword = "";
    showMessage("Enter Password");
  }
}

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);

  // GPIO
  pinMode(LIVING_LED,  OUTPUT); digitalWrite(LIVING_LED,  LOW);
  pinMode(BEDROOM_LED, OUTPUT); digitalWrite(BEDROOM_LED, LOW);
  pinMode(BTN_LIVING,  INPUT_PULLUP);
  pinMode(BTN_BEDROOM, INPUT_PULLUP);

  // OLED
  Wire.begin();
  u8g2.begin();
  showMessage("Booting...");

  // Servo
  myServo.attach(SERVO_PIN);
  myServo.write(0);

  // WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  showMessage("Connecting", "WiFi...");
  Serial.print("Connecting WiFi");
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  Serial.println("\nWiFi OK: " + WiFi.localIP().toString());
  showMessage("WiFi OK", WiFi.localIP().toString());

  // NTP (can cho timestamp notification)
  configTime(7 * 3600, 0, "pool.ntp.org", "time.nist.gov");

  // Firebase
  config.api_key      = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email     = USER_EMAIL;
  auth.user.password  = USER_PASSWORD;
  config.token_status_callback = tokenStatusCallback;
  config.timeout.serverResponse = 10 * 1000;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  showMessage("Firebase", "Logging in...");
  Serial.print("Dang dang nhap");
  while (!Firebase.ready()) { delay(500); Serial.print("."); }
  Serial.println("\nFirebase OK!");
  showMessage("Firebase OK");
  delay(500);

  // Doc password cua tu Firebase
  loadDoorPassword();
  lastPasswordFetch = millis();

  // Reset trang thai cua
  updateStatus(doorID, "OFF");

  showMessage("SMART HOME", "Enter Password");
  Serial.println(">> System Ready | Password: " + correctPassword);
}

// =====================================================
// LOOP
// =====================================================
void loop() {
  unsigned long now = millis();

  // Lam moi password moi 30 giay
  if (now - lastPasswordFetch >= PASSWORD_REFRESH_MS) {
    loadDoorPassword();
    lastPasswordFetch = now;
  }

  // 1. Kiem tra lenh mo cua tu App
  checkDoorCommand();

  // 2. Xu ly keypad
  handleKeypad();

  // 3. Doc trang thai den tu Firebase
  readLamps();

  // 4. Xu ly nut nhan vat ly
  handleButtons();

  delay(300);
}