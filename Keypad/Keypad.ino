#include <Keypad.h>
#include <ESP32Servo.h>

#define ROW_NUM   4
#define COLUMN_NUM 4

char keys[ROW_NUM][COLUMN_NUM] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};

// ✅ Chân đã tối ưu
byte pin_rows[ROW_NUM]   = {23, 14, 27, 26};
byte pin_column[COLUMN_NUM] = {25, 33, 32, 21};

Keypad keypad = Keypad(makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM);

// 🔐 Password
String correctPassword = "1234";
String inputPassword = "";
int maxLength = 4;

// 🔧 Servo
Servo myServo;
int servoPin = 22;

void setup() {
  Serial.begin(115200);

  myServo.attach(servoPin);
  myServo.write(0); // đóng cửa

  Serial.println("=== DOOR LOCK SYSTEM ===");
  Serial.println("Enter Password:");
}

void loop() {
  char key = keypad.getKey();

  if (key) {

    Serial.print("Key: ");
    Serial.println(key);

    // 🔁 Xóa
    if (key == '*') {
      inputPassword = "";
      Serial.println("Cleared!");
    }

    // ✅ Xác nhận
    else if (key == '#') {
      if (inputPassword == correctPassword) {
        Serial.println("✅ Access Granted");

        // 🔓 Mở cửa
        myServo.write(90);
        delay(3000);

        // 🔒 Đóng lại
        myServo.write(0);
      } else {
        Serial.println("❌ Wrong Password");
      }

      inputPassword = "";
    }

    // 🔢 Nhập số
    else {
      if (inputPassword.length() < maxLength) {
        inputPassword += key;

        Serial.print("Input: ");
        Serial.println(inputPassword);
      } else {
        Serial.println("⚠️ Max length reached!");
      }
    }
  }
}