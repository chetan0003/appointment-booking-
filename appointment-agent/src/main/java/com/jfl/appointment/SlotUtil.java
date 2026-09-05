package com.jfl.appointment;

import java.time.LocalTime;

public class SlotUtil {

    public static boolean overlaps(
            LocalTime start1,
            LocalTime end1,
            LocalTime start2,
            LocalTime end2) {

        if (start2 == null || end2 == null) {
            return false;
        }

        return start1.isBefore(end2)
                && end1.isAfter(start2);
    }

    public static LocalTime min(
            LocalTime first,
            LocalTime second) {

        return first.isBefore(second)
                ? first
                : second;
    }

    public static LocalTime max(
            LocalTime first,
            LocalTime second) {

        return first.isAfter(second)
                ? first
                : second;
    }
}
