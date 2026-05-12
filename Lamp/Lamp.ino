#include <WiFi.h>
#include <Firebase_ESP_Client.h>

// ================= WIFI =================
const char* ssid = "Tầng 3";
const char* password = "88888888";

// ================= FIREBASE =================
#define API_KEY "AIzaSyBGLN7JVyZU8T_DRnqIP1BUE4b1p3-dv3M"

#define DATABASE_URL "https://esp32-c9b75-default-rtdb.asia-southeast1.firebasedatabase.app/"

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// ================= LED =================
#define LIVING_LED   4
#define KITCHEN_LED  5
#define BEDROOM_LED  18

// ================= BUTTON =================
#define BTN_LIVING   13
#define BTN_KITCHEN  14
#define BTN_BEDROOM  27

// ================= STATE =================
bool livingState = false;
bool kitchenState = false;
bool bedroomState = false;

// ================= FIREBASE DEVICE ID =================
// COPY đúng ID trong Firebase của bạn

String livingID  = "-OrOZoB0pgLW-n9wekiL";
String bedroomID = "-OrO_6DF0E5liW15kwjs";


String kitchenID = "-OrO_6DF0E5liW15kwjs";

// ================= WIFI =================
void setup_wifi() {

  Serial.begin(115200);

  WiFi.begin(ssid, password);

  Serial.print("Connecting WiFi");

  while (WiFi.status() != WL_CONNECTED) {

    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.println("WiFi Connected!");
  Serial.println(WiFi.localIP());
}

// ================= FIREBASE =================
void setupFirebase() {

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  // anonymous auth
  if (Firebase.signUp(&config, &auth, "", "")) {

    Serial.println("Firebase SignUp OK");

  } else {

    Serial.println(config.signer.signupError.message.c_str());
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  Serial.println("Firebase Connected!");
}

// ================= HANDLE DEVICE =================
void handleDevice(
  String room,
  String status
) {

  int pin = -1;

  if (room == "livingroom") {

    pin = LIVING_LED;
    livingState = (status == "ON");
  }

  else if (room == "kitchen") {

    pin = KITCHEN_LED;
    kitchenState = (status == "ON");
  }

  else if (room == "bedroom") {

    pin = BEDROOM_LED;
    bedroomState = (status == "ON");
  }

  if (pin == -1) return;

  digitalWrite(
    pin,
    status == "ON" ? HIGH : LOW
  );

  Serial.println(
    room + " -> " + status
  );
}

// ================= UPDATE FIREBASE =================
void updateStatus(
  String deviceID,
  String status
) {

  String path =
    "/devices/" +
    deviceID +
    "/status";

  if (Firebase.RTDB.setString(
        &fbdo,
        path,
        status
      )) {

    Serial.println("Updated: " + status);

  } else {

    Serial.println("Update Failed");
    Serial.println(fbdo.errorReason());
  }
}

// ================= BUTTON =================
void handleButton() {

  // ===== LIVING =====
  if (digitalRead(BTN_LIVING) == LOW) {

    delay(200);

    if (digitalRead(BTN_LIVING) == LOW) {

      livingState = !livingState;

      updateStatus(
        livingID,
        livingState ? "ON" : "OFF"
      );

      while (digitalRead(BTN_LIVING) == LOW);
    }
  }

  // ===== KITCHEN =====
  if (digitalRead(BTN_KITCHEN) == LOW) {

    delay(200);

    if (digitalRead(BTN_KITCHEN) == LOW) {

      kitchenState = !kitchenState;

      updateStatus(
        kitchenID,
        kitchenState ? "ON" : "OFF"
      );

      while (digitalRead(BTN_KITCHEN) == LOW);
    }
  }

  // ===== BEDROOM =====
  if (digitalRead(BTN_BEDROOM) == LOW) {

    delay(200);

    if (digitalRead(BTN_BEDROOM) == LOW) {

      bedroomState = !bedroomState;

      updateStatus(
        bedroomID,
        bedroomState ? "ON" : "OFF"
      );

      while (digitalRead(BTN_BEDROOM) == LOW);
    }
  }
}

// ================= READ FIREBASE =================
void readDevices() {

  if (!Firebase.ready()) return;

  // ===== LIVING ROOM =====
  if (Firebase.RTDB.getString(
        &fbdo,
        "/devices/" + livingID + "/status"
      )) {

    String status = fbdo.stringData();

    handleDevice(
      "livingroom",
      status
    );
  }

  // ===== BEDROOM =====
  if (Firebase.RTDB.getString(
        &fbdo,
        "/devices/" + bedroomID + "/status"
      )) {

    String status = fbdo.stringData();

    handleDevice(
      "bedroom",
      status
    );
  }

  // ===== KITCHEN =====
  if (Firebase.RTDB.getString(
        &fbdo,
        "/devices/" + kitchenID + "/status"
      )) {

    String status = fbdo.stringData();

    handleDevice(
      "kitchen",
      status
    );
  }
}

// ================= SETUP =================
void setup() {

  // LED
  pinMode(LIVING_LED, OUTPUT);
  pinMode(KITCHEN_LED, OUTPUT);
  pinMode(BEDROOM_LED, OUTPUT);

  // BUTTON
  pinMode(BTN_LIVING, INPUT_PULLUP);
  pinMode(BTN_KITCHEN, INPUT_PULLUP);
  pinMode(BTN_BEDROOM, INPUT_PULLUP);

  // wifi
  setup_wifi();

  // firebase
  setupFirebase();
}

// ================= LOOP =================
void loop() {

  // button
  handleButton();

  // firebase
  readDevices();

  delay(500);
}