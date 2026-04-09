package com.tongji.auth.util;

import java.util.regex.Pattern;

public final class IdentifierValidator {


    /*^1 - 字符串必须以数字 1 开头（符合中国大陆手机号以1开头的规则）
      \\d{10} - 匹配恰好10个数字字符
       $ - 字符串结束位置*/
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    /*[A-Z0-9._%+-]+ - 用户名部分：字母、数字及特殊字符(._%+-)，至少一个
      @ - 必须包含@符号
      [A-Z0-9.-]+ - 域名部分：字母、数字、点号、连字符，至少一个
      \. - 转义的点号（.）
      [A-Z]{2,} - 顶级域名：至少2个字母
      Pattern.CASE_INSENSITIVE - 不区分大小写标志*/
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private IdentifierValidator() {
    }

    /**
     * 校验手机号格式（中国大陆 11 位，以 1 开头）。
     *
     * @param phone 手机号字符串。
     * @return 是否匹配手机号正则。
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 校验邮箱格式（大小写不敏感）。
     *
     * @param email 邮箱字符串。
     * @return 是否匹配邮箱正则。
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
