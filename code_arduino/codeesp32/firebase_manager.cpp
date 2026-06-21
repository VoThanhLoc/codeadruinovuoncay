#include "firebase_manager.h"

#include <Firebase_ESP_Client.h>

#include "config.h"
#include "relay_manager.h"

FirebaseData streamFbdo;
FirebaseData scheduleFbdo;
FirebaseData historyFbdo;

FirebaseAuth auth;
FirebaseConfig config;

void streamCallback(FirebaseStream data)
{
    String path = data.dataPath();

    Serial.println("Firebase changed:");
    Serial.println(path);

    // irrigation/start
    if (path == "/start")
    {
        if (data.dataType() == "boolean")
        {
            bool state = data.boolData();

            Serial.printf(
                "Irrigation Start -> %s\n",
                state ? "ON" : "OFF"
            );

            setIrrigationStart(state);
        }

        return;
    }

    // irrigation/zones/zoneX/status
    if (
        path.startsWith("/zones/") &&
        path.endsWith("/status")
    )
    {
        int zone = -1;

        String zoneName =
            path.substring(
                7,
                path.lastIndexOf("/status")
            );

        Serial.printf(
            "Zone Name: %s\n",
            zoneName.c_str()
        );

        if (zoneName == "zone1") zone = 0;
        else if (zoneName == "zone2") zone = 1;
        else if (zoneName == "zone3") zone = 2;
        else if (zoneName == "zone4") zone = 3;
        else if (zoneName == "zone5") zone = 4;
        else if (zoneName == "zone6") zone = 5;
        else if (zoneName == "zone7") zone = 6;

        if (zone < 0)
        {
            Serial.println(
                "Unknown zone"
            );
            return;
        }

        if (data.dataType() != "boolean")
        {
            Serial.println(
                "Invalid data type"
            );
            return;
        }

        bool state =
            data.boolData();

        Serial.printf(
            "Zone %d -> %s\n",
            zone + 1,
            state ? "ON" : "OFF"
        );

        setZoneState(
            zone,
            state
        );
    }
}

void streamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println(
      "Firebase timeout");
  }
}

void initFirebase() {
  config.database_url =
    FIREBASE_URL;

  config.signer.tokens.legacy_token =
    FIREBASE_SECRET;

  Firebase.begin(
    &config,
    &auth);

  Firebase.reconnectWiFi(true);

  if (
    Firebase.RTDB.setString(
      &historyFbdo,
      "/test",
      "ESP32 Connected")) {
    Serial.println(
      "Firebase OK");
  } else {
    Serial.println(
      historyFbdo.errorReason());
  }

  // STREAM TOÀN irrigation
  if (
    !Firebase.RTDB.beginStream(
      &streamFbdo,
      "/irrigation")) {
    Serial.println(
      streamFbdo.errorReason());

    return;
  }

  Firebase.RTDB.setStreamCallback(
    &streamFbdo,
    streamCallback,
    streamTimeoutCallback);
}

void firebaseLoop() {
  if (!Firebase.ready()) {
    return;
  }

  if (!streamFbdo.httpConnected()) {
    Serial.println(
      "Reconnecting Stream...");

    Firebase.RTDB.beginStream(
      &streamFbdo,
      "/irrigation");
  }
}