#pragma once

#include <WiFi.h>

bool connectSavedWifi();
void startConfigPortal();
bool loadWifiConfig(String &ssid, String &password);
bool saveWifiConfig(const String &ssid, const String &password);