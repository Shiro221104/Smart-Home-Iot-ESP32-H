#include <DHT.h>
#include <DHT_U.h>

DHT dht(26, DHT11);

void setup() {
  Serial.begin(115200);
  dht.begin();
  delay(2000);
}

void loop() {
  float temp = dht.readTemperature();
  float humidity = dht.readHumidity();

  Serial.print("Temp: ");
  Serial.print(temp);
  Serial.print(" C ");

  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.println(" % ");

  delay(2000);
}