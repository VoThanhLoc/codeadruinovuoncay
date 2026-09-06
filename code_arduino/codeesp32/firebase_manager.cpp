#include "firebase_manager.h"

#include <WiFi.h>
#include <Firebase_ESP_Client.h>

#include "config.h"
#include "relay_manager.h"

// ============================================================
// Firebase objects
// ============================================================

FirebaseData streamFbdo;
FirebaseData scheduleFbdo;
FirebaseData historyFbdo;

FirebaseAuth auth;
FirebaseConfig config;

// ============================================================
// Device information
// ============================================================

// Phiên bản firmware hiện tại
// Khi cập nhật firmware thì thay đổi giá trị này.
const char* FIRMWARE_VERSION = "1.1";

// Thời gian cập nhật trạng thái thiết bị
unsigned long lastDeviceUpdate = 0;

// Cập nhật heartbeat mỗi 10 giây
const unsigned long DEVICE_UPDATE_INTERVAL = 10000;


// ============================================================
// Firebase Stream Callback
// ============================================================

void streamCallback(FirebaseStream data)
{
    String path = data.dataPath();

    Serial.println("Firebase changed:");
    Serial.println(path);

    // ========================================================
    // irrigation/start
    // ========================================================

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


    // ========================================================
    // irrigation/zones/zoneX/status
    // ========================================================

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


        // ----------------------------------------------------
        // Xác định zone
        // ----------------------------------------------------

        if (zoneName == "zone1")
        {
            zone = 0;
        }
        else if (zoneName == "zone2")
        {
            zone = 1;
        }
        else if (zoneName == "zone3")
        {
            zone = 2;
        }
        else if (zoneName == "zone4")
        {
            zone = 3;
        }
        else if (zoneName == "zone5")
        {
            zone = 4;
        }
        else if (zoneName == "zone6")
        {
            zone = 5;
        }
        else if (zoneName == "zone7")
        {
            zone = 6;
        }


        // ----------------------------------------------------
        // Zone không hợp lệ
        // ----------------------------------------------------

        if (zone < 0)
        {
            Serial.println(
                "Unknown zone"
            );

            return;
        }


        // ----------------------------------------------------
        // Kiểm tra kiểu dữ liệu
        // ----------------------------------------------------

        if (data.dataType() != "boolean")
        {
            Serial.println(
                "Invalid data type"
            );

            return;
        }


        // ----------------------------------------------------
        // Lấy trạng thái
        // ----------------------------------------------------

        bool state =
            data.boolData();


        Serial.printf(
            "Zone %d -> %s\n",
            zone + 1,
            state ? "ON" : "OFF"
        );


        // ----------------------------------------------------
        // Điều khiển relay
        // ----------------------------------------------------

        setZoneState(
            zone,
            state
        );
    }
}


// ============================================================
// Firebase Stream Timeout Callback
// ============================================================

void streamTimeoutCallback(bool timeout)
{
    if (timeout)
    {
        Serial.println(
            "Firebase stream timeout"
        );
    }
}


// ============================================================
// Update Device Presence
// ============================================================
//
// Database:
//
// device
// ├── firmwareVersion
// ├── status
// └── lastSeen
//
// Ví dụ:
//
// "device": {
//     "firmwareVersion": "1.1",
//     "status": "Online",
//     "lastSeen": 1755529950000
// }
//
// ============================================================

void updateDevicePresence()
{
    // --------------------------------------------------------
    // Kiểm tra WiFi
    // --------------------------------------------------------

    if (WiFi.status() != WL_CONNECTED)
    {
        Serial.println(
            "[Device] WiFi disconnected"
        );

        return;
    }


    // --------------------------------------------------------
    // Kiểm tra Firebase
    // --------------------------------------------------------

    if (!Firebase.ready())
    {
        Serial.println(
            "[Device] Firebase not ready"
        );

        return;
    }


    // --------------------------------------------------------
    // Kiểm tra thời gian update
    // --------------------------------------------------------

    if (
        millis() - lastDeviceUpdate
        < DEVICE_UPDATE_INTERVAL
    )
    {
        return;
    }


    lastDeviceUpdate = millis();


    // ========================================================
    // 1. Firmware version
    // ========================================================

    if (
        !Firebase.RTDB.setString(
            &historyFbdo,
            "/device/firmwareVersion",
            FIRMWARE_VERSION
        )
    )
    {
        Serial.println(
            "[Device] Failed to update firmwareVersion"
        );

        Serial.println(
            historyFbdo.errorReason()
        );

        return;
    }


    // ========================================================
    // 2. Status
    // ========================================================

    if (
        !Firebase.RTDB.setString(
            &historyFbdo,
            "/device/status",
            "Online"
        )
    )
    {
        Serial.println(
            "[Device] Failed to update status"
        );

        Serial.println(
            historyFbdo.errorReason()
        );

        return;
    }


    // ========================================================
    // 3. Last Seen
    // ========================================================

    if (
        !Firebase.RTDB.setTimestamp(
            &historyFbdo,
            "/device/lastSeen"
        )
    )
    {
        Serial.println(
            "[Device] Failed to update lastSeen"
        );

        Serial.println(
            historyFbdo.errorReason()
        );

        return;
    }


    Serial.println(
        "[Device] Online"
    );

    Serial.println(
        "[Device] Heartbeat updated"
    );
}


// ============================================================
// Initialize Firebase
// ============================================================

void initFirebase()
{
    Serial.println(
        "[Firebase] Initializing..."
    );


    // ========================================================
    // Firebase configuration
    // ========================================================

    config.database_url =
        FIREBASE_URL;

    config.signer.tokens.legacy_token =
        FIREBASE_SECRET;


    // ========================================================
    // Start Firebase
    // ========================================================

    Firebase.begin(
        &config,
        &auth
    );

    Firebase.reconnectWiFi(true);


    Serial.println(
        "[Firebase] Started"
    );


    // ========================================================
    // Test Firebase connection
    // ========================================================

    if (
        Firebase.RTDB.setString(
            &historyFbdo,
            "/test",
            "ESP32 Connected"
        )
    )
    {
        Serial.println(
            "[Firebase] Connection OK"
        );
    }
    else
    {
        Serial.println(
            "[Firebase] Connection failed"
        );

        Serial.println(
            historyFbdo.errorReason()
        );
    }


    // ========================================================
    // Device information
    // ========================================================

    if (Firebase.ready())
    {
        // Firmware version
        Firebase.RTDB.setString(
            &historyFbdo,
            "/device/firmwareVersion",
            FIRMWARE_VERSION
        );


        // Device status
        Firebase.RTDB.setString(
            &historyFbdo,
            "/device/status",
            "Online"
        );


        // Last seen
        Firebase.RTDB.setTimestamp(
            &historyFbdo,
            "/device/lastSeen"
        );


        Serial.println(
            "[Device] Status -> Online"
        );
    }


    // ========================================================
    // Firebase Stream
    //
    // Theo dõi:
    //
    // /irrigation
    //
    // Bao gồm:
    //
    // /irrigation/start
    // /irrigation/zones/zone1/status
    // ...
    // /irrigation/zones/zone7/status
    // ========================================================

    if (
        !Firebase.RTDB.beginStream(
            &streamFbdo,
            "/irrigation"
        )
    )
    {
        Serial.println(
            "[Firebase] Stream failed"
        );

        Serial.println(
            streamFbdo.errorReason()
        );

        return;
    }


    Firebase.RTDB.setStreamCallback(
        &streamFbdo,
        streamCallback,
        streamTimeoutCallback
    );


    Serial.println(
        "[Firebase] Irrigation stream ready"
    );
}


// ============================================================
// Firebase Loop
// ============================================================

void firebaseLoop()
{
    // ========================================================
    // WiFi check
    // ========================================================

    if (WiFi.status() != WL_CONNECTED)
    {
        Serial.println(
            "[Firebase] WiFi disconnected"
        );

        return;
    }


    // ========================================================
    // Firebase check
    // ========================================================

    if (!Firebase.ready())
    {
        Serial.println(
            "[Firebase] Not ready"
        );

        return;
    }


    // ========================================================
    // Device heartbeat
    // ========================================================

    updateDevicePresence();


    // ========================================================
    // Firebase Stream reconnect
    // ========================================================

    if (!streamFbdo.httpConnected())
    {
        Serial.println(
            "[Firebase] Stream disconnected"
        );

        Serial.println(
            "[Firebase] Reconnecting Stream..."
        );


        if (
            Firebase.RTDB.beginStream(
                &streamFbdo,
                "/irrigation"
            )
        )
        {
            Serial.println(
                "[Firebase] Stream reconnected"
            );

            Firebase.RTDB.setStreamCallback(
                &streamFbdo,
                streamCallback,
                streamTimeoutCallback
            );
        }
        else
        {
            Serial.println(
                "[Firebase] Stream reconnect failed"
            );

            Serial.println(
                streamFbdo.errorReason()
            );
        }
    }
}