package com.zerotrust.rgw.enums;

public enum ClientType {
    USER,
    API_CLIENT,
    SOFTWARE_TOOL;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (ClientType type : values()) {
            if (type.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
