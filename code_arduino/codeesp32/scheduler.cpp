#include "scheduler.h"

#include <Firebase_ESP_Client.h>
#include <time.h>

#include "firebase_manager.h"
#include "relay_manager.h"
#include "config.h"

FirebaseJson scheduleJson;
FirebaseJsonData jsonData;
ScheduleItem schedules[MAX_SCHEDULES];

int scheduleCount = 0;

unsigned long lastLoadSchedule = 0;

String normalizeDay(String day)
{
    day.trim();
    day.toUpperCase();
    return day;
}

int zoneNameToIndex(String zoneName)
{
    zoneName.trim();

    zoneName.replace(
        "Van",
        ""
    );

    return zoneName.toInt() - 1;
}

String getCurrentTimeHHMM()
{
    struct tm timeinfo;

    if (!getLocalTime(&timeinfo))
    {
        return "";
    }

    char buffer[6];

    strftime(
        buffer,
        sizeof(buffer),
        "%H:%M",
        &timeinfo
    );

    return String(buffer);
}

String getCurrentDay()
{
    struct tm timeinfo;

    if (!getLocalTime(&timeinfo))
    {
        return "";
    }

    char buffer[10];

    strftime(
        buffer,
        sizeof(buffer),
        "%a",
        &timeinfo
    );

    return String(buffer);
}

bool dayMatched(
    ScheduleItem &schedule,
    String currentDay
)
{
    currentDay =
        normalizeDay(
            currentDay
        );

    for (
        int i = 0;
        i < schedule.repeatDayCount;
        i++
    )
    {
        String day =
            normalizeDay(
                schedule.repeatDays[i]
            );

        if (day == currentDay)
        {
            return true;
        }
    }

    return false;
}

void startSchedule(
    ScheduleItem &schedule
)
{
    if (schedule.running)
    {
        return;
    }

    Serial.printf(
        "Start schedule: %s\n",
        schedule.id.c_str()
    );

    for (
        int i = 0;
        i < schedule.zoneCount;
        i++
    )
    {
        int zoneIndex =
            zoneNameToIndex(
                schedule.zones[i]
            );

        if (
            zoneIndex >= 0 &&
            zoneIndex < NUM_ZONES
        )
        {
            setZoneState(
                zoneIndex,
                true
            );
        }
    }

Serial.println("CALL START PUMP");
    setIrrigationStart(true);
    schedule.running = true;

    schedule.startMillis =
        millis();

    schedule.status = "on";

    Firebase.RTDB.setString(
        &scheduleFbdo,
        "/schedule/" +
        schedule.id +
        "/status",
        "on"
    );
}

void stopSchedule(
    ScheduleItem &schedule
)
{
    if (!schedule.running)
    {
        return;
    }

    Serial.printf(
        "Stop schedule: %s\n",
        schedule.id.c_str()
    );

    for (
        int i = 0;
        i < schedule.zoneCount;
        i++
    )
    {
        int zoneIndex =
            zoneNameToIndex(
                schedule.zones[i]
            );

        if (
            zoneIndex >= 0 &&
            zoneIndex < NUM_ZONES
        )
        {
            setZoneState(
                zoneIndex,
                false
            );
        }
    }

    setIrrigationStart(false);
    schedule.running = false;

    schedule.status = "off";

    Firebase.RTDB.setString(
        &scheduleFbdo,
        "/schedule/" +
        schedule.id +
        "/status",
        "off"
    );
}

void loadSchedules()
{
    if (
        !Firebase.RTDB.getJSON(
            &scheduleFbdo,
            "/schedule"
        )
    )
    {
        Serial.println(
            "Load schedule failed"
        );

        return;
    }

    scheduleCount = 0;

    FirebaseJson &json =
        scheduleFbdo.jsonObject();

    size_t count =
        json.iteratorBegin();

    FirebaseJson::IteratorValue value;

    for (
        size_t i = 0;
        i < count;
        i++
    )
    {
        value =
            json.valueAt(i);

        // Chỉ lấy node schedule
        if (
            !value.key.startsWith("-")
        )
        {
            continue;
        }

        if (
            scheduleCount >=
            MAX_SCHEDULES
        )
        {
            break;
        }

        ScheduleItem &schedule =
            schedules[scheduleCount];

        schedule.id =
            value.key;

        String basePath =
            value.key;

        if (
            json.get(
                jsonData,
                basePath +
                "/startTime"
            )
        )
        {
            schedule.startTime =
                jsonData.stringValue;
        }

        if (
            json.get(
                jsonData,
                basePath +
                "/duration"
            )
        )
        {
            schedule.duration =
                jsonData.stringValue.toInt();
        }

        if (
            json.get(
                jsonData,
                basePath +
                "/status"
            )
        )
        {
            schedule.status =
                jsonData.stringValue;
        }

       schedule.running = (schedule.status == "on");

        schedule.zoneCount = 0;

        for (
            int z = 0;
            z < MAX_ZONES_PER_SCHEDULE;
            z++
        )
        {
            if (
                json.get(
                    jsonData,
                    basePath +
                    "/zone/[" +
                    String(z) +
                    "]"
                )
            )
            {
                schedule.zones[
                    schedule.zoneCount++
                ] =
                    jsonData.stringValue;
            }
        }

        schedule.repeatDayCount = 0;

        for (
            int d = 0;
            d < MAX_REPEAT_DAYS;
            d++
        )
        {
            if (
                json.get(
                    jsonData,
                    basePath +
                    "/repeatDays/[" +
                    String(d) +
                    "]"
                )
            )
            {
                schedule.repeatDays[
                    schedule.repeatDayCount++
                ] =
                    jsonData.stringValue;
            }
        }

        Serial.printf(
            "Loaded ID=%s Time=%s Duration=%d Zones=%d Days=%d\n",
            schedule.id.c_str(),
            schedule.startTime.c_str(),
            schedule.duration,
            schedule.zoneCount,
            schedule.repeatDayCount
        );

        scheduleCount++;
    }

    json.iteratorEnd();

    Serial.printf(
        "Schedule Count: %d\n",
        scheduleCount
    );
}

void initScheduler()
{
    scheduleCount = 0;

    loadSchedules();

    Serial.println(
        "Scheduler Ready"
    );
}

void schedulerLoop()
{
    String currentTime =
        getCurrentTimeHHMM();

    String currentDay =
        getCurrentDay();

    // reload schedule mỗi 30s
    if (
        millis() -
        lastLoadSchedule >=
        30000
    )
    {
        lastLoadSchedule =
            millis();

        loadSchedules();
    }


    for (
        int i = 0;
        i < scheduleCount;
        i++
    )
    {
        ScheduleItem &schedule =
            schedules[i];
        if (
            !schedule.running &&
            schedule.status == "off"
        )
        {
            if (
                schedule.startTime ==
                currentTime &&
                dayMatched(
                    schedule,
                    currentDay
                )
            )
            {
                startSchedule(
                    schedule
                );
            }
        }

        if (schedule.running)
        {
            unsigned long durationMs =
                (unsigned long)
                schedule.duration *
                60000UL;

            if (
                millis() -
                schedule.startMillis >=
                durationMs
            )
            {
                stopSchedule(
                    schedule
                );
            }
        }
    }
}
