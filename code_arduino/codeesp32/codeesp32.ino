#include "config.h"

#include "relay_manager.h"
#include "wifi_manager.h"
#include "firebase_manager.h"
#include "scheduler.h"

#include <SPIFFS.h>
#include <time.h>


// ============================================================
// Connection Safety
// ============================================================

// Nếu mất WiFi liên tục quá thời gian này
// trong lúc đang tưới thì tắt toàn bộ hệ thống.
const unsigned long CONNECTION_SAFETY_TIMEOUT =
    30000;

unsigned long wifiLostSince = 0;


// ============================================================
// NTP
// ============================================================

const unsigned long NTP_TIMEOUT =
    30000;


// ============================================================
// Setup
// ============================================================

void setup()
{
    Serial.begin(115200);

    delay(1000);

    Serial.println();
    Serial.println(
        "================================"
    );

    Serial.println(
        "   VUON CAY CONG TOAN"
    );

    Serial.println(
        "   ESP32 STARTING..."
    );

    Serial.println(
        "================================"
    );


    // ========================================================
    // SPIFFS
    // ========================================================

    if (!SPIFFS.begin(true))
    {
        Serial.println(
            "[SPIFFS] Mount failed"
        );
    }
    else
    {
        Serial.println(
            "[SPIFFS] Ready"
        );
    }


    // ========================================================
    // Relay
    // ========================================================

    initRelays();

    // initRelays() đã đưa toàn bộ
    // Van + Pump về OFF.
    allZonesOff();


    // ========================================================
    // WiFi
    // ========================================================

    if (!connectSavedWifi())
    {
        Serial.println(
            "[WiFi] Starting configuration portal..."
        );

        startConfigPortal();
    }

    Serial.println(
        "[WiFi] Connected"
    );


    // ========================================================
    // NTP
    // ========================================================

    Serial.println(
        "[NTP] Synchronizing time..."
    );

    configTime(
        7 * 3600,
        0,
        "pool.ntp.org",
        "time.nist.gov"
    );


    struct tm timeinfo;

    unsigned long ntpStart =
        millis();

    bool ntpReady = false;


    while (
        millis() - ntpStart <
        NTP_TIMEOUT
    )
    {
        if (
            getLocalTime(
                &timeinfo
            )
        )
        {
            ntpReady = true;
            break;
        }

        Serial.println(
            "[NTP] Waiting..."
        );

        delay(1000);
    }


    if (ntpReady)
    {
        Serial.println(
            "[NTP] Time synchronized"
        );

        Serial.println(
            &timeinfo,
            "%A, %B %d %Y %H:%M:%S"
        );
    }
    else
    {
        Serial.println(
            "[NTP] Synchronization timeout"
        );

        Serial.println(
            "[NTP] ESP32 will continue running"
        );
    }


    // ========================================================
    // Firebase
    // ========================================================

    initFirebase();


    // ========================================================
    // Initial schedule load
    // ========================================================

    if (
        Firebase.ready()
    )
    {
        if (
            Firebase.RTDB.getJSON(
                &scheduleFbdo,
                "/schedule"
            )
        )
        {
            Serial.println(
                "[Schedule] Read OK"
            );
        }
        else
        {
            Serial.println(
                "[Schedule] Initial read failed"
            );

            Serial.println(
                scheduleFbdo.errorReason()
            );
        }
    }
    else
    {
        Serial.println(
            "[Schedule] Firebase not ready"
        );
    }


    // ========================================================
    // Scheduler
    // ========================================================

    initScheduler();


    Serial.println();
    Serial.println(
        "================================"
    );

    Serial.println(
        "   ESP32 READY"
    );

    Serial.println(
        "================================"
    );
}


// ============================================================
// Connection Safety
// ============================================================

void checkConnectionSafety()
{
    // --------------------------------------------------------
    // WiFi OK
    // --------------------------------------------------------

    if (
        WiFi.status() == WL_CONNECTED
    )
    {
        // Nếu vừa reconnect lại
        // thì reset timer mất kết nối.

        if (wifiLostSince != 0)
        {
            Serial.println(
                "[Safety] WiFi connection restored"
            );

            wifiLostSince = 0;
        }

        return;
    }


    // --------------------------------------------------------
    // WiFi disconnected
    // --------------------------------------------------------

    if (wifiLostSince == 0)
    {
        wifiLostSince =
            millis();

        Serial.println(
            "[Safety] WiFi connection lost"
        );

        return;
    }


    unsigned long disconnectedTime =
        millis() -
        wifiLostSince;


    // --------------------------------------------------------
    // Safety timeout
    // --------------------------------------------------------

    if (
        disconnectedTime >=
        CONNECTION_SAFETY_TIMEOUT
    )
    {
        Serial.println(
            "[Safety] WiFi disconnected too long"
        );

        Serial.println(
            "[Safety] Turning OFF all zones and pump"
        );


        // Quan trọng:
        // Tắt toàn bộ Van + Pump
        allZonesOff();


        // Reset timer để không gọi
        // allZonesOff() liên tục.
        wifiLostSince =
            millis();
    }
}


// ============================================================
// Loop
// ============================================================

void loop()
{
    // ========================================================
    // 1. Maintain WiFi
    // ========================================================

    maintainWifiConnection();


    // ========================================================
    // 2. Safety check
    // ========================================================

    checkConnectionSafety();


    // ========================================================
    // 3. Firebase
    // ========================================================

    firebaseLoop();


    // ========================================================
    // 4. Scheduler
    // ========================================================

    schedulerLoop();


    // Không delay dài ở đây.
}