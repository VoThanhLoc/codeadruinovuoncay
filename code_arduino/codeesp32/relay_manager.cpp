#include "esp32-hal-gpio.h"
#include "relay_manager.h"

bool zoneStates[NUM_ZONES] = { false };

bool irrigationStart = false;

void initRelays()
{
    for (int i = 0; i < NUM_ZONES; i++)
    {
        pinMode(RELAY_PINS[i], OUTPUT);
        digitalWrite(RELAY_PINS[i], RELAY_OFF);
    }

    pinMode(PUMP_PIN, OUTPUT);
    digitalWrite(PUMP_PIN, RELAY_OFF);
}

bool anyZoneRunning()
{
    for (int i = 0; i < NUM_ZONES; i++)
    {
        if (zoneStates[i])
        {
            return true;
        }
    }

    return false;
}

void pumpOff()
{
    digitalWrite(
        PUMP_PIN,
        RELAY_OFF
    );
}

void updatePumpState()
{
    bool anyZone =
        anyZoneRunning();

    bool pumpOn =
        irrigationStart &&
        anyZone;

    digitalWrite(
        PUMP_PIN,
        pumpOn
            ? RELAY_ON
            : RELAY_OFF
    );

    Serial.printf(
        "[PUMP] %s | start=%s | zone=%s\n",
        pumpOn ? "ON" : "OFF",
        irrigationStart ? "ON" : "OFF",
        anyZone ? "ON" : "OFF"
    );
}

void setIrrigationStart(bool state)
{
    irrigationStart = state;

    Serial.printf(
        "[START] %s\n",
        state ? "ON" : "OFF"
    );

    updatePumpState();
}

void setZoneState(
    int zone,
    bool state
)
{
    if (
        zone < 0 ||
        zone >= NUM_ZONES
    )
    {
        return;
    }

    zoneStates[zone] = state;

    digitalWrite(
        RELAY_PINS[zone],
        state
            ? RELAY_ON
            : RELAY_OFF
    );

    Serial.printf(
        "[ZONE %d] %s\n",
        zone + 1,
        state ? "ON" : "OFF"
    );

    updatePumpState();
}

void allZonesOff()
{
    for (
        int i = 0;
        i < NUM_ZONES;
        i++
    )
    {
        zoneStates[i] = false;

        digitalWrite(
            RELAY_PINS[i],
            RELAY_OFF
        );
    }

    digitalWrite(
        PUMP_PIN,
        RELAY_OFF
    );

    irrigationStart = false;

    Serial.println(
        "[SYSTEM] All zones OFF"
    );
}