#include <Keypad.h>
#include <ESP32Servo.h>
#include <Wire.h>
#include <U8g2lib.h>

// ===== OLED =====
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

// ===== Keypad =====
#define ROW_NUM   4
#define COLUMN_NUM 4

char keys[ROW_NUM][COLUMN_NUM] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};

byte pin_rows[ROW_NUM]   = {23, 14, 27, 26};
byte pin_column[COLUMN_NUM] = {25, 33, 32, 19};

Keypad keypad = Keypad(makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM);

// ===== Password =====
String correctPassword = "1234";
String inputPassword = "";
int maxLength = 4;

// ===== Servo =====
Servo myServo;
int servoPin = 18;

// ===== OLED function =====
void showMessage(String line1, String line2 = "") {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(0, 20, line1.c_str());
  u8g2.drawStr(0, 40, line2.c_str());
  u8g2.sendBuffer();
}

// ===== Setup =====
void setup() {
  Serial.begin(115200);
  delay(1000); // đảm bảo Serial ổn định

  Serial.println("=== SYSTEM START ===");

  // OLED
  u8g2.begin();
  showMessage("DOOR LOCK", "Enter Password");

  // Servo
  myServo.attach(servoPin);
  myServo.write(0);

  Serial.println("Setup done");
}

// ===== Loop =====
void loop() {

  // 🔥 DEBUG: luôn in để biết ESP còn chạy
  Serial.println("Loop running...");
  delay(200);

  char key = keypad.getKey();

  if (key != NO_KEY) {

    Serial.print("Key pressed: ");
    Serial.println(key);

    // 🔁 Clear
    if (key == '*') {
      inputPassword = "";
      showMessage("Cleared", "Enter Again");
      Serial.println("Password cleared");
      return;
    }

    // 🔢 Input
    if (inputPassword.length() < maxLength) {
      inputPassword += key;

      String stars = "";
      for (int i = 0; i < inputPassword.length(); i++) {
        stars += "*";
      }

      showMessage("Input:", stars);

      Serial.print("Current input: ");
      Serial.println(inputPassword);
    }

    // ✅ Check password
    if (inputPassword.length() == maxLength) {

      Serial.println("Checking password...");

      if (inputPassword == correctPassword) {
        Serial.println("Access Granted");
        showMessage("Access Granted", "Opening...");

        myServo.write(90);
        delay(3000);
        myServo.write(0);

      } else {
        Serial.println("Wrong Password");
        showMessage("Wrong Password", "Try Again");
        delay(2000);
      }

      // Reset
      inputPassword = "";
      showMessage("Enter Password");
    }
  }
}