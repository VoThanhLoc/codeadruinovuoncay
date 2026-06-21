#pragma once

#define FIREBASE_URL "https://vuoncaycongtoan-default-rtdb.firebaseio.com"
#define FIREBASE_SECRET "2qVdRhSRCsjySGWagTC4UMjvSXsWkLgzmaR2ZX35"

#define WIFI_CONFIG_FILE "/wifi.txt"

#define AP_SSID "VuonCay_Setup"
#define AP_PASSWORD "12345678"

#define NUM_ZONES 7

const int RELAY_PINS[NUM_ZONES] = {
  13, // Van1
  12, // Van2
  14, // Van3
  27, // Van4
  26, // Van5
  25, // Van6
  33  // Van7
};

#define PUMP_PIN 32

#define RELAY_ON HIGH
#define RELAY_OFF LOW