#include <Wire.h>
#include <U8g2lib.h>

// SH1106 128x64 I2C
U8G2_SH1106_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, U8X8_PIN_NONE);

void setup() {
  Serial.begin(115200);

  Wire.begin();

  u8g2.begin();

  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_ncenB08_tr);
  u8g2.drawStr(10, 30, "HELLO");
  u8g2.drawStr(10, 50, "ESP32-S3");
  u8g2.sendBuffer();
}

void loop() {}