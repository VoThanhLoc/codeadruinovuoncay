#include "config.h"
#include "relay_manager.h"
#include "wifi_manager.h"
#include "firebase_manager.h"
#include "scheduler.h"
#include <SPIFFS.h>
#include <time.h>

void setup() {
  
  Serial.begin(115200);

  SPIFFS.begin(true);

  initRelays();

  if (!connectSavedWifi()) {
    startConfigPortal();
  }

  Serial.println("WiFi Connected");

  configTime(
    7 * 3600,
    0,
    "pool.ntp.org",
    "time.nist.gov");

  struct tm timeinfo;

  while (!getLocalTime(&timeinfo)) {
    Serial.println(
      "Waiting NTP...");

    delay(1000);
  }

  Serial.println(
    &timeinfo,
    "%A, %B %d %Y %H:%M:%S");

  initFirebase();
  if (
    Firebase.RTDB.getJSON(
      &scheduleFbdo,
      "/schedule")) {
    Serial.println("Schedule Read OK");
  } else {
    Serial.println(
      scheduleFbdo.errorReason());
  }
  initScheduler();
}

void loop() {
  firebaseLoop();
  schedulerLoop();
}

// void setup()
// {
//     Serial.begin(115200);

//     for(int i=0;i<7;i++)
//     {
//         pinMode(RELAY_PINS[i], OUTPUT);
//         digitalWrite(RELAY_PINS[i], RELAY_OFF);
//     }

//     delay(3000);

//     digitalWrite(RELAY_PINS[0], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[1], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[2], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[3], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[4], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[5], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[6], RELAY_ON);
//     delay(3000);

//     digitalWrite(RELAY_PINS[7], RELAY_ON);
//     delay(3000);
// }

// void loop()
// {
//}