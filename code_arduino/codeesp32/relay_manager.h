#pragma once

#include <Arduino.h>
#include "config.h"

extern bool zoneStates[NUM_ZONES];

void initRelays();

void setZoneState(int zone, bool state);

void updatePumpState();

void allZonesOff();

void pumpOff();

bool anyZoneRunning();  
void setIrrigationStart(bool state);