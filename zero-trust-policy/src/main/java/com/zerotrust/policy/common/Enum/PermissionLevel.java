package com.zerotrust.policy.common.Enum;

public enum PermissionLevel {

    FULL(1),     // CRUD
    MEDIUM(2),   // RW
    LOW(3),      // R
    DISABLED(4); // NONE

    private final int order;

    PermissionLevel(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /** 权限取较小的那个（数字越小权限越大） */
    public static PermissionLevel min(PermissionLevel a, PermissionLevel b) {
        return a.order >= b.order ? a : b;
    }
}