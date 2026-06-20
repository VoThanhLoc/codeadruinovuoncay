#pragma once

#include <Arduino.h>

#define MAX_SCHEDULES 30
#define MAX_ZONES_PER_SCHEDULE 7
#define MAX_REPEAT_DAYS 7

struct ScheduleItem
{
    String id;

    String startTime;

    int duration;

    String status;

    String zones[MAX_ZONES_PER_SCHEDULE];
    int zoneCount;

    String repeatDays[MAX_REPEAT_DAYS];
    int repeatDayCount;

    bool running;

    unsigned long startMillis;
};

extern ScheduleItem schedules[MAX_SCHEDULES];
extern int scheduleCount;

void initScheduler();

void schedulerLoop();

void loadSchedules();

bool dayMatched(
    ScheduleItem &schedule,
    String currentDay
);

void startSchedule(
    ScheduleItem &schedule
);

void stopSchedule(
    ScheduleItem &schedule
);

int zoneNameToIndex(
    String zoneName
);