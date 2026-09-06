#include "wifi_manager.h"
#include "config.h"

#include <SPIFFS.h>
#include <WebServer.h>

WebServer server(80);

// ============================================================
// WiFi reconnect settings
// ============================================================

// Thời gian giữa mỗi lần thử reconnect
const unsigned long WIFI_RECONNECT_INTERVAL = 10000;

// Timeout kết nối ban đầu
const unsigned long WIFI_INITIAL_TIMEOUT = 30000;

unsigned long lastWifiReconnectAttempt = 0;


// ============================================================
// Load WiFi config
// ============================================================

bool loadWifiConfig(String &ssid, String &password)
{
    if (!SPIFFS.exists(WIFI_CONFIG_FILE))
        return false;

    File file = SPIFFS.open(WIFI_CONFIG_FILE, FILE_READ);

    if (!file)
        return false;

    ssid = file.readStringUntil('\n');
    password = file.readStringUntil('\n');

    ssid.trim();
    password.trim();

    file.close();

    if (ssid.length() == 0)
        return false;

    return true;
}


// ============================================================
// Save WiFi config
// ============================================================

bool saveWifiConfig(
    const String &ssid,
    const String &password
)
{
    File file =
        SPIFFS.open(
            WIFI_CONFIG_FILE,
            FILE_WRITE
        );

    if (!file)
        return false;

    file.println(ssid);
    file.println(password);

    file.close();

    return true;
}


// ============================================================
// Check WiFi
// ============================================================

bool isWifiConnected()
{
    return WiFi.status() == WL_CONNECTED;
}


// ============================================================
// Initial WiFi connection
// ============================================================

bool connectSavedWifi()
{
    String ssid;
    String password;

    if (!loadWifiConfig(ssid, password))
    {
        Serial.println("[WiFi] No WiFi config");
        return false;
    }

    Serial.println("[WiFi] Connecting...");
    Serial.println(ssid);

    WiFi.mode(WIFI_STA);

    WiFi.begin(
        ssid.c_str(),
        password.c_str()
    );

    unsigned long start =
        millis();

    while (
        WiFi.status() != WL_CONNECTED
    )
    {
        delay(500);

        Serial.print(".");

        if (
            millis() - start >=
            WIFI_INITIAL_TIMEOUT
        )
        {
            Serial.println();
            Serial.println(
                "[WiFi] Initial connection timeout"
            );

            WiFi.disconnect();

            return false;
        }
    }

    Serial.println();
    Serial.println(
        "[WiFi] Connected"
    );

    Serial.print(
        "[WiFi] IP: "
    );

    Serial.println(
        WiFi.localIP()
    );

    return true;
}


// ============================================================
// Maintain WiFi connection
// ============================================================

void maintainWifiConnection()
{
    if (WiFi.status() == WL_CONNECTED)
    {
        return;
    }

    unsigned long now =
        millis();

    if (
        now - lastWifiReconnectAttempt <
        WIFI_RECONNECT_INTERVAL
    )
    {
        return;
    }

    lastWifiReconnectAttempt =
        now;

    String ssid;
    String password;

    if (
        !loadWifiConfig(
            ssid,
            password
        )
    )
    {
        Serial.println(
            "[WiFi] No saved configuration"
        );

        return;
    }

    Serial.println(
        "[WiFi] Connection lost"
    );

    Serial.println(
        "[WiFi] Trying to reconnect..."
    );

    WiFi.mode(WIFI_STA);

    WiFi.begin(
        ssid.c_str(),
        password.c_str()
    );
}


// ============================================================
// Configuration portal
// ============================================================

void startConfigPortal()
{
    WiFi.mode(WIFI_AP);

    WiFi.softAP(
        AP_SSID,
        AP_PASSWORD
    );

    Serial.println(
        "[WiFi] AP Started"
    );

    Serial.print(
        "[WiFi] AP IP: "
    );

    Serial.println(
        WiFi.softAPIP()
    );


    // --------------------------------------------------------
    // GET /
    // --------------------------------------------------------

    server.on(
        "/",
        HTTP_GET,
        []()
        {
            String html =
                "<html>"
                "<body>"
                "<h2>WiFi Setup</h2>"

                "<form action='/save' method='POST'>"

                "SSID:<br>"
                "<input name='ssid'>"
                "<br><br>"

                "Password:<br>"
                "<input name='password' type='password'>"
                "<br><br>"

                "<input type='submit' value='Save'>"

                "</form>"

                "</body>"
                "</html>";

            server.send(
                200,
                "text/html",
                html
            );
        }
    );


    // --------------------------------------------------------
    // POST /save
    // --------------------------------------------------------

    server.on(
        "/save",
        HTTP_POST,
        []()
        {
            String ssid =
                server.arg("ssid");

            String password =
                server.arg("password");

            ssid.trim();
            password.trim();

            if (
                ssid.length() == 0
            )
            {
                server.send(
                    400,
                    "text/plain",
                    "SSID is required"
                );

                return;
            }

            if (
                saveWifiConfig(
                    ssid,
                    password
                )
            )
            {
                server.send(
                    200,
                    "text/html",
                    "Saved. Restarting..."
                );

                delay(2000);

                ESP.restart();
            }
            else
            {
                server.send(
                    500,
                    "text/plain",
                    "Save failed"
                );
            }
        }
    );


    server.begin();

    Serial.println(
        "[WiFi] Configuration portal ready"
    );


    // Configuration portal cần block
    // cho tới khi user nhập WiFi.
    while (true)
    {
        server.handleClient();

        delay(10);
    }
}