// =====================================================
// SMART HOME - ESP32-S3
// DHT11 + MQ2 + Fan Auto + Living/Kitchen/Bedroom LED
// Firebase: users/{userId}/devices/{deviceId}/status
// =====================================================

#include <Wire.h>
#include <U8g2lib.h>
#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <DHT.h>
#include <time.h>

// ===== OLED (I2C: SDA=41, SCL=42) =====
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ===== WIFI =====
#define WIFI_SSID     "ESP32_Hotspot"
#define WIFI_PASSWORD "12345678"

// ===== FIREBASE =====
#define API_KEY      "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"
#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define USER_EMAIL    "Hungnguyen221104@gmail.com"
#define USER_PASSWORD "123456"

// ===== DHT11 (giu nguyen) =====
#define DHTPIN  4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// ===== MQ2 (giu nguyen) =====
#define MQ2_A0 5
#define MQ2_D0 2

// ===== RELAY QUAT & NUT NHAN QUAT (giu nguyen) =====
#define FAN_RELAY_PIN  18
#define BUTTON_FAN     13

// ===== LED PHONG KHACH =====
#define LIVING_LED     15   // GPIO15
#define BUTTON_LIVING  16   // GPIO16

// ===== LED PHONG BEP =====
#define KITCHEN_LED    17   // GPIO17
#define BUTTON_KITCHEN 21   // GPIO21

// ===== NGUONG GAS =====
#define GAS_DANGER_THRESHOLD  600
#define GAS_SAFE_THRESHOLD    600
#define GAS_WARNING_THRESHOLD 300

// ===== FIREBASE OBJECTS =====
FirebaseData fbdo;      // cam bien
FirebaseData fbdo2;     // notification gas
FirebaseData fbdo3;     // fan status
FirebaseData fbdo4;     // notification nut nhan
FirebaseData fbdo5;     // living room light status
FirebaseData fbdo6;     // kitchen light status
FirebaseAuth auth;
FirebaseConfig config;

String userUID       = "fWn2xoXCpDWoJyCV1yt3FXxLgMi2";
String lastGasStatus = "";
bool   firebaseReady = false;

// ===== DEVICE IDs =====
const String fanDeviceId     = "-OvLD7bW7bkRn_5Sr5uG";
const String livingDeviceId  = "-OvKwCdcMamePNGjS0Kt";
const String kitchenDeviceId = "-OvLD1-JevTrFOp2dNuy";

// ===== TRANG THAI =====
bool fanIsOn     = false;
bool fanAutoMode = false;
bool livingIsOn  = false;
bool kitchenIsOn = false;

// ===== DEBOUNCE NUT NHAN =====
bool          lastBtnFan      = HIGH;
bool          stableBtnFan    = HIGH;
unsigned long lastDebounceFan = 0;

bool          lastBtnLiving      = HIGH;
bool          stableBtnLiving    = HIGH;
unsigned long lastDebounceLiving = 0;

bool          lastBtnKitchen      = HIGH;
bool          stableBtnKitchen    = HIGH;
unsigned long lastDebounceKitchen = 0;

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
void setLight(bool on, bool sendNotif, bool &stateVar, int pin,
              FirebaseData &fd, const String &deviceId, String label);
void updateDeviceFirebase(FirebaseData &fd, const String &deviceId, bool on);
void pushNotification(FirebaseData &fd, String title, String message, int gasVal = -1);

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);

  Wire.begin(8, 9);
  u8g2.begin();
  showOLED("Booting...", "", "", "");

  dht.begin();
  pinMode(MQ2_D0, INPUT);

  // Relay quat
  pinMode(FAN_RELAY_PIN, OUTPUT);
  digitalWrite(FAN_RELAY_PIN, LOW);

  // LED phong khach
  pinMode(LIVING_LED, OUTPUT);
  digitalWrite(LIVING_LED, LOW);

  // LED phong bep
  pinMode(KITCHEN_LED, OUTPUT);
  digitalWrite(KITCHEN_LED, LOW);

  // Nut nhan
  pinMode(BUTTON_FAN,     INPUT_PULLUP);
  pinMode(BUTTON_LIVING,  INPUT_PULLUP);
  pinMode(BUTTON_KITCHEN, INPUT_PULLUP);

  // WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  showOLED("Connecting", "WiFi...", "", "");
  while (WiFi.status() != WL_CONNECTED) { delay(500); }
  showOLED("WiFi OK", WiFi.localIP().toString().c_str(), "", "");

  // NTP
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

  showOLED("Firebase", "Logging in...", "", "");
  while (auth.token.uid == "") { delay(500); }

  userUID       = auth.token.uid.c_str();
  firebaseReady = true;
  showOLED("Firebase OK", userUID.substring(0, 16).c_str(), "", "");
  delay(1000);

  // Reset trang thai Firebase
  updateDeviceFirebase(fbdo3, fanDeviceId,     false);
  updateDeviceFirebase(fbdo5, livingDeviceId,  false);
  updateDeviceFirebase(fbdo6, kitchenDeviceId, false);
}

// =====================================================
// LOOP
// =====================================================
void loop() {
  unsigned long now = millis();

  // ===== NUT NHAN QUAT =====
  bool btnFanRead = digitalRead(BUTTON_FAN);
  if (btnFanRead != lastBtnFan) lastDebounceFan = now;
  if ((now - lastDebounceFan) > DEBOUNCE_MS) {
    if (btnFanRead == LOW && stableBtnFan == HIGH) {
      setFan(!fanIsOn, true);
    }
    stableBtnFan = btnFanRead;
  }
  lastBtnFan = btnFanRead;

  // ===== NUT NHAN PHONG KHACH =====
  bool btnLivingRead = digitalRead(BUTTON_LIVING);
  if (btnLivingRead != lastBtnLiving) lastDebounceLiving = now;
  if ((now - lastDebounceLiving) > DEBOUNCE_MS) {
    if (btnLivingRead == LOW && stableBtnLiving == HIGH) {
      setLight(!livingIsOn, true, livingIsOn, LIVING_LED,
               fbdo5, livingDeviceId, "Đèn phòng khách");
    }
    stableBtnLiving = btnLivingRead;
  }
  lastBtnLiving = btnLivingRead;

  // ===== NUT NHAN PHONG BEP =====
  bool btnKitchenRead = digitalRead(BUTTON_KITCHEN);
  if (btnKitchenRead != lastBtnKitchen) lastDebounceKitchen = now;
  if ((now - lastDebounceKitchen) > DEBOUNCE_MS) {
    if (btnKitchenRead == LOW && stableBtnKitchen == HIGH) {
      setLight(!kitchenIsOn, true, kitchenIsOn, KITCHEN_LED,
               fbdo6, kitchenDeviceId, "Đèn phòng bếp");
    }
    stableBtnKitchen = btnKitchenRead;
  }
  lastBtnKitchen = btnKitchenRead;

  // ===== DOC CAM BIEN =====
  float t        = dht.readTemperature();
  float h        = dht.readHumidity();
  int   gas      = analogRead(MQ2_A0);
  int   gasAlert = digitalRead(MQ2_D0);

  String gasStatus;
  if      (gas < GAS_WARNING_THRESHOLD) gasStatus = "SAFE";
  else if (gas < GAS_DANGER_THRESHOLD)  gasStatus = "WARNING";
  else                                  gasStatus = "DANGER";

  // ===== LOGIC TU DONG QUAT (theo gas) =====
  // ===== TU DONG THEO CAM BIEN GAS =====

// Có gas nguy hiểm -> tự bật quạt
if (gas >= GAS_DANGER_THRESHOLD && !fanAutoMode) {

    fanAutoMode = true;

    setFan(true, false);

    pushNotification(
        fbdo4,
        "Thiết bị tự động bật",
        "Phát hiện khí gas, quạt đã tự động bật."
    );

    Serial.println("AUTO -> Fan ON");
}

// Chỉ khi quạt đang ở chế độ tự động mới được tự tắt
if (fanAutoMode &&
    gas < GAS_SAFE_THRESHOLD) {

    fanAutoMode = false;

    setFan(false, false);

    pushNotification(
        fbdo4,
        "Thiết bị tự động tắt",
        "Khí gas đã an toàn, quạt tự động tắt."
    );

    Serial.println("AUTO -> Fan OFF");
}

  // ===== DOC TRANG THAI TU FIREBASE (App dieu khien) =====
  if (firebaseReady && Firebase.ready()) {
// ===== Quat =====
String fanPath = "/users/" + userUID + "/devices/" + fanDeviceId + "/status";
if (Firebase.RTDB.getString(&fbdo3, fanPath)) {
  bool appWantsOn = (fbdo3.stringData() == "ON");

  if (appWantsOn != fanIsOn) {
    fanIsOn = appWantsOn;
    digitalWrite(FAN_RELAY_PIN, fanIsOn ? HIGH : LOW);

    Serial.println(">> APP: Quat -> " + String(fanIsOn ? "ON" : "OFF"));
  }
}
    // Den phong khach
    String livingPath = "/users/" + userUID + "/devices/" + livingDeviceId + "/status";
    if (Firebase.RTDB.getString(&fbdo5, livingPath)) {
      bool appWantsOn = (fbdo5.stringData() == "ON");
      if (appWantsOn != livingIsOn) {
        livingIsOn = appWantsOn;
        digitalWrite(LIVING_LED, livingIsOn ? HIGH : LOW);
        Serial.println(">> APP: Den phong khach -> " + String(livingIsOn ? "ON" : "OFF"));
      }
    }

    // Den phong bep
    String kitchenPath = "/users/" + userUID + "/devices/" + kitchenDeviceId + "/status";
    if (Firebase.RTDB.getString(&fbdo6, kitchenPath)) {
      bool appWantsOn = (fbdo6.stringData() == "ON");
      if (appWantsOn != kitchenIsOn) {
        kitchenIsOn = appWantsOn;
        digitalWrite(KITCHEN_LED, kitchenIsOn ? HIGH : LOW);
        Serial.println(">> APP: Den phong bep -> " + String(kitchenIsOn ? "ON" : "OFF"));
      }
    }
  }

  // ===== OLED =====
  if (gasStatus == "DANGER") {
    if (now - lastBlink >= BLINK_MS) { blinkState = !blinkState; lastBlink = now; }
    if (blinkState) showScreenDanger(gas);
    else            showScreenNormal(t, h, gas, gasStatus);
  } else {
    showScreenNormal(t, h, gas, gasStatus);
  }

  // ===== FIREBASE CAM BIEN =====
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

    if (gasStatus == "DANGER" && lastGasStatus != "DANGER") {
      pushNotification(fbdo2, "CẢNH Báo GAS", "Phát hiện khí gas nguy hiểm!", gas);
      Serial.println(">> Gas DANGER -> Notification");
    }
  }

  lastGasStatus = gasStatus;
  delay(500);
}

// =====================================================
// Bat/Tat quat
// =====================================================
void setFan(bool on, bool sendNotif) {
  if (fanIsOn == on) return;
  fanIsOn = on;
  digitalWrite(FAN_RELAY_PIN, on ? HIGH : LOW);
  updateDeviceFirebase(fbdo3, fanDeviceId, on);

  if (sendNotif) {
    if (on) pushNotification(fbdo4, "THIẾT BỊ BẬT", "Quạt đã được bật thủ công bằng nút bấm.");
    else    pushNotification(fbdo4, "THIẾT BỊ Tắt", "Quat đã được bật thủ công bằng nút bấm.");
  }
}

// =====================================================
// Bat/Tat den
// =====================================================
void setLight(bool on, bool sendNotif, bool &stateVar, int pin,
              FirebaseData &fd, const String &deviceId, String label) {
  if (stateVar == on) return;
  stateVar = on;
  digitalWrite(pin, on ? HIGH : LOW);
  updateDeviceFirebase(fd, deviceId, on);

  if (sendNotif) {
    if (on) pushNotification(fd, "THIẾT BỊ BẬT", label + " đã được bật thủ công bằng nút bấm.");
    else    pushNotification(fd, "THIẾT BỊ Tắt", label + " đã được bật thủ công bằng nút bấm.");
  }
}

// =====================================================
// Cap nhat trang thai thiet bi len Firebase
// =====================================================
void updateDeviceFirebase(FirebaseData &fd, const String &deviceId, bool on) {
  if (!(firebaseReady && Firebase.ready())) return;
  String path = "/users/" + userUID + "/devices/" + deviceId + "/status";
  if (!Firebase.RTDB.setString(&fd, path, on ? "ON" : "OFF"))
    Serial.println("!! Device ERR: " + fd.errorReason());
}

// =====================================================
// Push notification
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
    Serial.println(">> Noti OK: " + title + " | " + fd.pushName());
  else
    Serial.println("!! Noti ERR: " + fd.errorReason());
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