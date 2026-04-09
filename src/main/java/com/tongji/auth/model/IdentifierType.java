package com.tongji.auth.model;

public enum IdentifierType {
    PHONE,
    EMAIL;

    public static IdentifierType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("identifier type required");
        }
        /*value.toLowerCase()功能：将字符串中的所有字符转换为小写形式*/
        return switch (value.toLowerCase()) {
            case "phone", "mobile" -> PHONE;
            case "email" -> EMAIL;
            default -> throw new IllegalArgumentException("Unsupported identifier type: " + value);
        };
    }
}
