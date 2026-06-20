#pragma once

#include <Firebase_ESP_Client.h>

extern FirebaseData scheduleFbdo;
extern FirebaseData historyFbdo;

void initFirebase();
void firebaseLoop();