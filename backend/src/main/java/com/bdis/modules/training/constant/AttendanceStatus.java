package com.bdis.modules.training.constant;

import java.util.Set;

public final class AttendanceStatus {
    public static final String PENDING = "pending";
    public static final String PRESENT = "present";
    public static final String LATE = "late";
    public static final String ABSENT = "absent";
    public static final String LEAVE = "leave";
    public static final Set<String> VALUES = Set.of(PENDING, PRESENT, LATE, ABSENT, LEAVE);

    private AttendanceStatus() {}
}
