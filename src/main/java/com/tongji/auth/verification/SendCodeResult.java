package com.tongji.auth.verification;

/**
 * 发送验证码结果。
 * <p>
 * 返回规范化账号、发送场景与验证码有效期（秒）。
 */

/*record的作用

构造函数 - 接收三个参数
访问器方法 - identifier(), scene(), expireSeconds()
equals() 和 hashCode() - 基于所有字段
toString() - 格式化输出*/
public record SendCodeResult(String identifier,
                             VerificationScene scene,
                             int expireSeconds
) {
}
