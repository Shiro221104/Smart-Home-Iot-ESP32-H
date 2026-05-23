#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <driver/i2s.h>
#include "esp_heap_caps.h"

// =====================================================
// WIFI
// =====================================================
const char* ssid = "Tầng 3";
const char* password = "88888888";

// =====================================================
// SERVER
// =====================================================
const char* serverUrl =
  "https://nacho-turbojet-decency.ngrok-free.dev/stt";

const char* serverCheck =
  "https://nacho-turbojet-decency.ngrok-free.dev/";

// =====================================================
// INMP441 PINS
// =====================================================
#define I2S_WS     5
#define I2S_SD     6
#define I2S_SCK    4

#define I2S_PORT   I2S_NUM_0

// =====================================================
// AUDIO CONFIG
// =====================================================
#define SAMPLE_RATE    16000

#define RECORD_TIME    3

#define TOTAL_SAMPLES \
  (SAMPLE_RATE * RECORD_TIME)

// =====================================================
// AUDIO BUFFER
// =====================================================
int32_t* audioBuffer  = NULL;
int16_t* outputBuffer = NULL;


// =====================================================
// WIFI CONNECT
// =====================================================
void connectWiFi() {

  Serial.println();
  Serial.println("===== WIFI CONNECT =====");

  WiFi.mode(WIFI_STA);

  WiFi.begin(ssid, password);

  int retry = 0;

  while (
    WiFi.status() != WL_CONNECTED &&
    retry < 30
  ) {

    delay(500);
    Serial.print(".");
    retry++;
  }

  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {

    Serial.println("WiFi Connected");

    Serial.print("ESP IP: ");
    Serial.println(WiFi.localIP());

    Serial.print("RSSI: ");
    Serial.println(WiFi.RSSI());

  } else {

    Serial.println("WiFi FAIL");
  }
}


// =====================================================
// SERVER CHECK
// =====================================================
bool checkServer() {

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;

  Serial.println();
  Serial.println("===== SERVER TEST =====");

  http.begin(client, serverCheck);
  http.setTimeout(5000);

  int code = http.GET();

  http.end();

  if (code > 0) {

    Serial.print("Server OK: ");
    Serial.println(code);
    return true;

  } else {

    Serial.println("SERVER NOT REACHABLE");
    return false;
  }
}


// =====================================================
// I2S SETUP
// =====================================================
void setupI2S() {

  i2s_config_t i2s_config = {

    .mode = (i2s_mode_t)(
      I2S_MODE_MASTER |
      I2S_MODE_RX
    ),

    .sample_rate = SAMPLE_RATE,

    // INMP441 output 24bit in 32bit frame
    .bits_per_sample =
      I2S_BITS_PER_SAMPLE_32BIT,

    .channel_format =
      I2S_CHANNEL_FMT_ONLY_RIGHT,

    .communication_format =
      I2S_COMM_FORMAT_STAND_I2S,

    .intr_alloc_flags =
      ESP_INTR_FLAG_LEVEL1,

    .dma_buf_count = 8,

    // tăng lên tránh dropout
    .dma_buf_len = 512,

    .use_apll = false,

    .tx_desc_auto_clear = false,

    .fixed_mclk = 0
  };

  i2s_pin_config_t pin_config = {

    .bck_io_num = I2S_SCK,

    .ws_io_num = I2S_WS,

    .data_out_num =
      I2S_PIN_NO_CHANGE,

    .data_in_num = I2S_SD
  };

  esp_err_t err;

  err = i2s_driver_install(
          I2S_PORT,
          &i2s_config,
          0,
          NULL
        );

  if (err != ESP_OK) {

    Serial.println("I2S INSTALL FAIL");
    while (1);
  }

  err = i2s_set_pin(
          I2S_PORT,
          &pin_config
        );

  if (err != ESP_OK) {

    Serial.println("I2S PIN FAIL");
    while (1);
  }

  i2s_zero_dma_buffer(I2S_PORT);

  Serial.println("I2S READY");
}


// =====================================================
// RECORD AUDIO
// =====================================================
void recordAudio() {

  Serial.println();
  Serial.println("===== RECORDING =====");

  memset(
    audioBuffer,
    0,
    TOTAL_SAMPLES * sizeof(int32_t)
  );

  size_t bytesRead  = 0;
  size_t totalBytes = 0;

  while (
    totalBytes <
    TOTAL_SAMPLES * sizeof(int32_t)
  ) {

    esp_err_t err = i2s_read(

      I2S_PORT,

      (char*)audioBuffer + totalBytes,

      (TOTAL_SAMPLES * sizeof(int32_t))
      - totalBytes,

      &bytesRead,

      portMAX_DELAY
    );

    if (err == ESP_OK && bytesRead > 0) {

      totalBytes += bytesRead;

    } else {

      Serial.println("I2S READ FAIL");
      break;
    }
  }

  Serial.print("Recorded bytes: ");
  Serial.println(totalBytes);

  // =====================================================
  // CONVERT 32bit -> 16bit
  // INMP441: data ở bit 8-31, shift >> 8
  // Không nhân gain để tránh noise
  // =====================================================
  for (int i = 0; i < TOTAL_SAMPLES; i++) {

    int32_t sample = audioBuffer[i] >> 8;

    if (sample > 32767)  sample = 32767;
    if (sample < -32768) sample = -32768;

    outputBuffer[i] = (int16_t)sample;
  }

  // debug amplitude
  int32_t maxVal = 0;
  for (int i = 0; i < TOTAL_SAMPLES; i++) {
    int32_t v = abs((int32_t)outputBuffer[i]);
    if (v > maxVal) maxVal = v;
  }

  Serial.print("Max amplitude: ");
  Serial.println(maxVal);
}


// =====================================================
// SEND AUDIO
// =====================================================
void sendAudio() {

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;

  Serial.println();
  Serial.println("===== SEND AUDIO =====");

  http.begin(client, serverUrl);
  http.setTimeout(30000);

  http.addHeader(
    "Content-Type",
    "application/octet-stream"
  );

  http.addHeader(
    "ngrok-skip-browser-warning",
    "true"
  );

  int httpCode = http.POST(

                   (uint8_t*)outputBuffer,

                   TOTAL_SAMPLES *
                   sizeof(int16_t)
                 );

  Serial.print("HTTP Code: ");
  Serial.println(httpCode);

  if (httpCode > 0) {

    String response = http.getString();

    Serial.println();
    Serial.println("===== SERVER RESPONSE =====");
    Serial.println(response);

  } else {

    Serial.println();
    Serial.println("===== HTTP ERROR =====");
    Serial.println(
      http.errorToString(httpCode)
    );
  }

  http.end();
  client.stop();
}


// =====================================================
// SETUP
// =====================================================
void setup() {

  Serial.begin(115200);

  delay(2000);

  Serial.println();
  Serial.println("ESP32-S3 START");

  if (psramFound()) {

    Serial.println("PSRAM FOUND");
    Serial.print("PSRAM SIZE: ");
    Serial.println(ESP.getPsramSize());

  } else {

    Serial.println("NO PSRAM");
  }

  audioBuffer = (int32_t*) ps_malloc(
                  TOTAL_SAMPLES *
                  sizeof(int32_t)
                );

  if (!audioBuffer) {
    Serial.println("AUDIO BUFFER FAIL");
    while (1);
  }

  outputBuffer = (int16_t*) ps_malloc(
                   TOTAL_SAMPLES *
                   sizeof(int16_t)
                 );

  if (!outputBuffer) {
    Serial.println("OUTPUT BUFFER FAIL");
    while (1);
  }

  Serial.println("BUFFER OK");

  Serial.print("Free Heap: ");
  Serial.println(ESP.getFreeHeap());

  Serial.print("Free PSRAM: ");
  Serial.println(ESP.getFreePsram());

  connectWiFi();

  setupI2S();
}


// =====================================================
// LOOP
// =====================================================
void loop() {

  if (WiFi.status() != WL_CONNECTED) {

    Serial.println("WiFi Lost");
    connectWiFi();
    delay(1000);
    return;
  }

  if (!checkServer()) {

    delay(5000);
    return;
  }

  recordAudio();

  sendAudio();

  Serial.println();
  Serial.println("Wait 5 sec...");

  delay(5000);
}