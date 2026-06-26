#include <Wire.h>
#include <U8g2lib.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <DHT.h>
#include <time.h>

// ===== OLED =====
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ===== WIFI =====
#define WIFI_SSID     "Tầng 3"
#define WIFI_PASSWORD "88888888"

// ===== FIREBASE =====
#define API_KEY      "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL    "Hungnguyen221104@gmail.com"
#define USER_PASSWORD "123456"

// ===== DHT =====
#define DHTPIN  4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== MQ2 =====
#define MQ2_A0 5
#define MQ2_D0 2

// ===== RELAY & NUT NHAN =====
#define FAN_RELAY_PIN  18
#define BUTTON_PIN     13

// ===== NGUONG GAS =====
#define GAS_DANGER_THRESHOLD  600
#define GAS_SAFE_THRESHOLD    600
#define GAS_WARNING_THRESHOLD 300

// ===== FIREBASE OBJECTS =====
FirebaseData fbdo;    // doc/ghi cam bien
FirebaseData fbdo2;   // notification gas
FirebaseData fbdo3;   // cap nhat trang thai quat
FirebaseData fbdo4;   // notification nut nhan thu cong
FirebaseAuth auth;
FirebaseConfig config;

String userUID       = "";
String lastGasStatus = "";
bool   firebaseReady = false;

const String fanDeviceId = "-OvLD7bW7bkRn_5Sr5uG";

// ===== TRANG THAI =====
bool fanIsOn = false;

// ===== CHONG DOI NUT =====
bool          lastBtnState  = HIGH;
bool          stableBtnState = HIGH;
unsigned long lastDebounce  = 0;
#define DEBOUNCE_MS 50

// ===== NHAP NHAY OLED =====
bool          blinkState = false;
unsigned long lastBlink  = 0;
#define BLINK_MS 500

// ===== KHAI BAO HAM =====
void showScreenNormal(float t, float h, int gas, String gasStatus);
void showScreenDanger(int gas);
void showOLED(const char* l1, const char* l2, const char* l3, const char* l4);
void setFan(bool on, bool sendNotif);
void updateFanFirebase(bool on);
void pushNotification(FirebaseData &fd, String title, String message, int gasVal = -1);

// =====================================================
void setup() {
  Serial.begin(115200);

  Wire.begin(41, 42);
  u8g2.begin();
  showOLED("Booting...", "", "", "");

  dht.begin();
  pinMode(MQ2_D0, INPUT);

  pinMode(FAN_RELAY_PIN, OUTPUT);
  digitalWrite(FAN_RELAY_PIN, LOW);
  fanIsOn = false;

  pinMode(BUTTON_PIN, INPUT_PULLUP);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  showOLED("Connecting", "WiFi...", "", "");
  while (WiFi.status() != WL_CONNECTED) { delay(500); }
  showOLED("WiFi OK", WiFi.localIP().toString().c_str(), "", "");

  configTime(7 * 3600, 0, "pool.ntp.org", "time.nist.gov");

  config.api_key      = API_KEY;
  config.database_url = DATABASE_URL;
  auth.user.email     = USER_EMAIL;
  auth.user.password  = USER_PASSWORD;
  config.token_status_callback = tokenStatusCallback;
  config.timeout.serverResponse = 10 * 1000;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  showOLED("Firebase", "Logging in...", "", "");
  while (auth.token.uid == "") { delay(500); }

  userUID       = auth.token.uid.c_str();
  firebaseReady = true;
  showOLED("Firebase OK", userUID.substring(0, 16).c_str(), "", "");
  delay(1000);

  updateFanFirebase(false);
}

// =====================================================
void loop() {
  unsigned long now = millis();

  // ===== XU LY NUT NHAN - Toggle ON/OFF don gian =====
  bool btnReading = digitalRead(BUTTON_PIN);
  if (btnReading != lastBtnState) lastDebounce = now;

  if ((now - lastDebounce) > DEBOUNCE_MS) {
    if (btnReading == LOW && stableBtnState == HIGH) {
      // Nhan nut -> dao trang thai quat
      bool newState = !fanIsOn;
      setFan(newState, true); // true = gui notification
      Serial.println(newState ? ">> NUT: Bat quat" : ">> NUT: Tat quat");
    }
    stableBtnState = btnReading;
  }
  lastBtnState = btnReading;

  // ===== DOC CAM BIEN =====
  float t      = dht.readTemperature();
  float h      = dht.readHumidity();
  int   gas    = analogRead(MQ2_A0);
  int   gasAlert = digitalRead(MQ2_D0);

  String gasStatus;
  if      (gas < GAS_WARNING_THRESHOLD) gasStatus = "SAFE";
  else if (gas < GAS_DANGER_THRESHOLD)  gasStatus = "WARNING";
  else                                  gasStatus = "DANGER";

  // ===== LOGIC TU DONG QUAT (theo gas) =====
  if (gas >= GAS_DANGER_THRESHOLD && !fanIsOn) {
    setFan(true, false); // false = khong gui notification nut nhan
    Serial.println(">> AUTO: Gas nguy hiem -> BAT QUAT");
  } else if (gas < GAS_SAFE_THRESHOLD && fanIsOn) {
    setFan(false, false);
    Serial.println(">> AUTO: Gas an toan -> TAT QUAT");
  }

  // ===== OLED =====
  if (gasStatus == "DANGER") {
    if (now - lastBlink >= BLINK_MS) { blinkState = !blinkState; lastBlink = now; }
    if (blinkState) showScreenDanger(gas);
    else            showScreenNormal(t, h, gas, gasStatus);
  } else {
    showScreenNormal(t, h, gas, gasStatus);
  }

  // ===== FIREBASE =====
  if (firebaseReady && Firebase.ready()) {

    String sensorPath = "/users/" + userUID + "/sensors/device1";
    FirebaseJson sensorJson;
    sensorJson.set("temperature",  t);
    sensorJson.set("humidity",     h);
    sensorJson.set("gas_value",    gas);
    sensorJson.set("gas_detected", gasAlert);
    sensorJson.set("gas_status",   gasStatus);
    sensorJson.set("userId",       userUID);

    if (!Firebase.RTDB.setJSON(&fbdo, sensorPath, &sensorJson))
      Serial.println("!! Cam bien ERR: " + fbdo.errorReason());

    // Notification khi gas chuyen sang DANGER
    if (gasStatus == "DANGER" && lastGasStatus != "DANGER") {
      pushNotification(fbdo2, "CẢNH BÁO GAS",
                       "Phát hiện khí gas nguy hiểm!", gas);
      Serial.println(">> Gas DANGER -> Notification");
    }
  }

  lastGasStatus = gasStatus;
  delay(500);
}

// =====================================================
// Bat/Tat relay + cap nhat Firebase + (tuy chon) notification
// =====================================================
void setFan(bool on, bool sendNotif) {
  if (fanIsOn == on) return;
  fanIsOn = on;
  digitalWrite(FAN_RELAY_PIN, on ? HIGH : LOW);
  updateFanFirebase(on);

  if (sendNotif) {
    if (on) pushNotification(fbdo4, "THIẾT BỊ BẬT", "Quạt đã được bật thủ công.");
    else    pushNotification(fbdo4, "THIẾT BỊ TẮT", "Quạt đã được tắt thủ công.");
  }
}

void updateFanFirebase(bool on) {
  if (!(firebaseReady && Firebase.ready())) return;
  String path = "/users/" + userUID + "/devices/" + fanDeviceId + "/status";
  if (!Firebase.RTDB.setString(&fbdo3, path, on ? "ON" : "OFF"))
    Serial.println("!! Fan status ERR: " + fbdo3.errorReason());
}

// =====================================================
// Push notification len Firebase
// =====================================================
void pushNotification(FirebaseData &fd, String title, String message, int gasVal) {
  if (!(firebaseReady && Firebase.ready())) return;

  time_t t_now; time(&t_now);
  String notiPath = "/users/" + userUID + "/notifications";

  FirebaseJson notiJson;
  notiJson.set("title",   title);
  notiJson.set("message", message);
  notiJson.set("time",    (int)t_now);
  notiJson.set("userId",  userUID);
  if (gasVal >= 0) notiJson.set("gas_value", gasVal);

  if (Firebase.RTDB.pushJSON(&fd, notiPath, &notiJson))
    Serial.println(">> Notification OK: " + title + " | " + fd.pushName());
  else
    Serial.println("!! Notification ERR: " + fd.errorReason());
}

// =====================================================
// MAN HINH BINH THUONG
// =====================================================
void showScreenNormal(float t, float h, int gas, String gasStatus) {
  u8g2.clearBuffer();
  char buf[32];

  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(0, 10, "Temperature:");

  u8g2.setFont(u8g2_font_ncenB14_tr);
  snprintf(buf, sizeof(buf), "%.1f C", t);
  u8g2.drawStr(0, 30, buf);

  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(0, 44, "Humidity:");

  u8g2.setFont(u8g2_font_ncenB14_tr);
  snprintf(buf, sizeof(buf), "%.1f %%", h);
  u8g2.drawStr(0, 63, buf);

  u8g2.sendBuffer();
}

// =====================================================
// MAN HINH CANH BAO DANGER
// =====================================================
void showScreenDanger(int gas) {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB14_tr);
  u8g2.drawStr(10, 18, "!! NGUY HIEM");
  u8g2.drawStr(15, 36, "KHI GAS!");
  u8g2.setFont(u8g2_font_7x13B_tr);
  char buf[24];
  snprintf(buf, sizeof(buf), "Gas: %d", gas);
  u8g2.drawStr(30, 52, buf);
  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(5, 64, "QUAT DA BAT TU DONG");
  u8g2.sendBuffer();
}

// =====================================================
// Helper OLED 4 dong (boot)
// =====================================================
void showOLED(const char* l1, const char* l2, const char* l3, const char* l4) {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);
  if (l1 && strlen(l1)) u8g2.drawStr(0, 13, l1);
  if (l2 && strlen(l2)) u8g2.drawStr(0, 28, l2);
  if (l3 && strlen(l3)) u8g2.drawStr(0, 43, l3);
  if (l4 && strlen(l4)) u8g2.drawStr(0, 58, l4);
  u8g2.sendBuffer();
}