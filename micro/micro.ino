#include <driver/i2s.h>

#define I2S_WS      5
#define I2S_SD      6
#define I2S_SCK     4

#define I2S_PORT I2S_NUM_0
#define BUFFER_LEN 64

int16_t sBuffer[BUFFER_LEN];

void setup() {
  Serial.begin(115200);

  i2s_config_t i2s_config = {
    .mode = (i2s_mode_t)(I2S_MODE_MASTER | I2S_MODE_RX),
    .sample_rate = 16000,
    .bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT,
    .channel_format = I2S_CHANNEL_FMT_ONLY_LEFT,
    .communication_format = I2S_COMM_FORMAT_STAND_I2S,
    .intr_alloc_flags = 0,
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

  i2s_driver_install(I2S_PORT, &i2s_config, 0, NULL);
  i2s_set_pin(I2S_PORT, &pin_config);

  Serial.println("Mic test...");
}

void loop() {
  size_t bytesIn = 0;

  esp_err_t result = i2s_read(
    I2S_PORT,
    &sBuffer,
    sizeof(sBuffer),
    &bytesIn,
    portMAX_DELAY
  );

  if (result == ESP_OK) {
    int samples = bytesIn / 2;

    float avg = 0;

    for (int i = 0; i < samples; i++) {
      avg += abs(sBuffer[i]);
    }

    avg /= samples;

    Serial.println(avg);
  }
}