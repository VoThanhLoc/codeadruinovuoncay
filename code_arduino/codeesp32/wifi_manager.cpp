#include "wifi_manager.h"
#include "config.h"

#include <SPIFFS.h>
#include <WebServer.h>

WebServer server(80);

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

    return true;
}

bool saveWifiConfig(const String &ssid, const String &password)
{
    File file = SPIFFS.open(WIFI_CONFIG_FILE, FILE_WRITE);

    if (!file)
        return false;

    file.println(ssid);
    file.println(password);

    file.close();

    return true;
}

bool connectSavedWifi()
{
    String ssid;
    String password;

    if (!loadWifiConfig(ssid, password))
    {
        Serial.println("No WiFi config");
        return false;
    }

    Serial.println("Connecting WiFi...");
    Serial.println(ssid);

    WiFi.begin(ssid.c_str(), password.c_str());

    unsigned long start = millis();

    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");

        if (millis() - start > 30000)
        {
            Serial.println("\nConnect timeout");
            return false;
        }
    }

    Serial.println();
    Serial.print("IP: ");
    Serial.println(WiFi.localIP());

    return true;
}

void startConfigPortal()
{
    WiFi.mode(WIFI_AP);

    WiFi.softAP(
        AP_SSID,
        AP_PASSWORD
    );

    Serial.println("AP Started");
    Serial.println(WiFi.softAPIP());

    server.on("/", HTTP_GET, []()
    {
        String html =
            "<html>"
            "<body>"
            "<h2>WiFi Setup</h2>"
            "<form action='/save' method='POST'>"
            "SSID:<br>"
            "<input name='ssid'><br><br>"
            "Password:<br>"
            "<input name='password'><br><br>"
            "<input type='submit' value='Save'>"
            "</form>"
            "</body>"
            "</html>";

        server.send(
            200,
            "text/html",
            html
        );
    });

    server.on("/save", HTTP_POST, []()
    {
        String ssid =
            server.arg("ssid");

        String password =
            server.arg("password");

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
    });

    server.begin();

    while (true)
    {
        server.handleClient();
        delay(10);
    }
}