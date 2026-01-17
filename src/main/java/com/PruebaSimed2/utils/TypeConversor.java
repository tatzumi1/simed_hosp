package com.PruebaSimed2.utils;

import lombok.extern.log4j.Log4j2;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Log4j2
public class TypeConversor {
    public static LocalDateTime convertSqlDateAndSqlTimeToDate(Date date, Time time) {
        if (date != null) {
            LocalDate dateLocal = date.toLocalDate();
            LocalTime timeLocal = (time != null) ? time.toLocalTime() : LocalTime.MIDNIGHT;
            LocalDateTime dateTime = LocalDateTime.of(dateLocal, timeLocal);
            log.debug("Converted date {} and time {} to LocalDateTime {}", date, time, dateTime);
            return dateTime;
        } else {
            log.warn("Date cannot be null, returning null LocalDateTime");
            return null;
        }
    }
}
