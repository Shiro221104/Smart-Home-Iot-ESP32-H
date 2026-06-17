#include <WiFi.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <driver/i2s.h>

// ================= WIFI =================
const char* ssid = "Quang Thu";
const char* password = "1000000000";

// ================= SERVER =================
const char* serverUrl =
"https://nacho-turbojet-decency.ngrok-free.dev/stt";

// ================= I2S =================
#define I2S_WS   16
#define I2S_SCK  17
#define I2S_SD   18

#define I2S_PORT I2S_NUM_0

#define SAMPLE_RATE 16000
#define RECORD_SECONDS 3

const int TOTAL_SAMPLES =
SAMPLE_RATE * RECORD_SECONDS;

// 3 giây PCM 16-bit
int16_t audioBuffer[TOTAL_SAMPLES];

// ==========================================
void setup() {

Serial.begin(115200);

WiFi.begin(ssid, password);

Serial.print("Connecting WiFi");

while (WiFi.status() != WL_CONNECTED) {
delay(500);
Serial.print(".");
}

Serial.println();
Serial.println("WiFi Connected");

i2s_config_t i2s_config = {
.mode = (i2s_mode_t)
(I2S_MODE_MASTER | I2S_MODE_RX),

.sample_rate = SAMPLE_RATE,

.bits_per_sample =
  I2S_BITS_PER_SAMPLE_32BIT,

.channel_format =
  I2S_CHANNEL_FMT_ONLY_LEFT,

.communication_format =
  I2S_COMM_FORMAT_I2S_MSB,

.intr_alloc_flags =
  ESP_INTR_FLAG_LEVEL1,

.dma_buf_count = 8,
.dma_buf_len = 64,

.use_apll = false,
.tx_desc_auto_clear = false,
.fixed_mclk = 0


};

i2s_pin_config_t pin_config = {
.bck_io_num = I2S_SCK,
.ws_io_num = I2S_WS,
.data_out_num = I2S_PIN_NO_CHANGE,
.data_in_num = I2S_SD
};

i2s_driver_install(
I2S_PORT,
&i2s_config,
0,
NULL
);

i2s_set_pin(
I2S_PORT,
&pin_config
);

Serial.println("I2S Ready");
}

// ==========================================
void recordAudio() {

Serial.println("===== RECORDING =====");

int index = 0;

while (index < TOTAL_SAMPLES) {


int32_t samples[256];
size_t bytesRead;

i2s_read(
  I2S_PORT,
  samples,
  sizeof(samples),
  &bytesRead,
  portMAX_DELAY
);

int count =
  bytesRead / sizeof(int32_t);

for (int i = 0; i < count; i++) {

  int32_t sample =
    samples[i] >> 14;

  audioBuffer[index] =
    constrain(
      sample,
      -32768,
      32767
    );

  index++;

  if (index >= TOTAL_SAMPLES)
    break;
}


}

Serial.println("===== RECORD DONE =====");
}

// ==========================================
void sendAudio() {

WiFiClientSecure client;
client.setInsecure();

HTTPClient http;

http.begin(client, serverUrl);

http.addHeader(
"Content-Type",
"application/octet-stream"
);

int code = http.POST(
(uint8_t*)audioBuffer,
sizeof(audioBuffer)
);

Serial.print("HTTP Code: ");
Serial.println(code);

if (code > 0) {


String response =
  http.getString();

Serial.println("==========");
Serial.println(response);
Serial.println("==========");

}

http.end();
}

// ==========================================
void loop() {

recordAudio();

sendAudio();

i2s_zero_dma_buffer(
I2S_PORT
);

delay(1000);
}
