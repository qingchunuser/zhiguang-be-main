package com.tongji.auth.token;

import lombok.RequiredArgsConstructor;
import com.tongji.auth.config.AuthProperties;
import com.tongji.user.domain.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JWT 令牌服务。
 * <p>
 * 功能：签发 Access/Refresh Token（RS256），解码 JWT，提取用户 ID、令牌类型与令牌 ID。
 * 声明：
 * - `token_type`：标识 access 或 refresh；
 * - `uid`：用户 ID；
 * - `jti`：令牌 ID（用作 Refresh Token 的白名单键）。
 * 过期时间：来自 `AuthProperties.jwt.accessTokenTtl` 与 `refreshTokenTtl`。
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USER_ID = "uid";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    /**
     * 为指定用户签发一对 Access/Refresh Token。
     * <p>
     * 令牌类型通过 `token_type` 声明区分；Refresh Token 的 `jti` 用于白名单存储与撤销。
     * 过期时间取自配置 `AuthProperties.jwt`。
     *
     * @param user 用户实体。
     * @return 令牌对与对应过期时间及刷新令牌 ID。
     */
    public TokenPair issueTokenPair(User user) {
        String refreshTokenId = UUID.randomUUID().toString();
        //这段代码的作用是获取当前时间戳作为JWT令牌的签发时间（issued at time）
        Instant issuedAt = Instant.now(clock);
        Instant accessExpiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());/* 访问令牌有效期（TTL）。 */
        Instant refreshExpiresAt = issuedAt.plus(properties.getJwt().getRefreshTokenTtl());/* 刷新令牌有效期（TTL）。 */


        String accessToken = encodeToken(user, issuedAt, accessExpiresAt, "access", UUID.randomUUID().toString());
        String refreshToken = encodeRefreshToken(user, issuedAt, refreshExpiresAt, refreshTokenId);
        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, refreshTokenId);
    }

    /**
     * 解码 JWT 字符串为 {@link Jwt}。
     *
     * @param token JWT 字符串。
     * @return 解析后的 JWT 对象。
     */
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * 编码访问令牌。
     *
     * @param user      用户实体，作为 subject 与自定义声明来源。
     * @param issuedAt  签发时间。
     * @param expiresAt 过期时间。
     * @param tokenType 令牌类型（"access"）。
     * @param tokenId   令牌 ID（jti）。
     * @return 编码后的 JWT 字符串。
     */
    private String encodeToken(User user, Instant issuedAt, Instant expiresAt, String tokenType, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())          // 设置发行者
                .issuedAt(issuedAt)                               // 设置签发时间
                .expiresAt(expiresAt)                             // 设置过期时间
                .subject(String.valueOf(user.getId()))            // 设置主题（用户ID）
                .id(tokenId)                                      // 设置令牌ID
                .claim(CLAIM_TOKEN_TYPE, tokenType)               // 自定义声明：令牌类型
                .claim(CLAIM_USER_ID, user.getId())               // 自定义声明：用户ID
                .claim("nickname", user.getNickname())            // 自定义声明：用户昵称
                .build();

        /*Header（头部）：eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9
           Payload（载荷）：包含所有 claims 信息
           Signature（签名）：使用私钥签名的部分*/
         return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();/**/

    }

    /**
     * 编码刷新令牌。
     *
     * @param user      用户实体。
     * @param issuedAt  签发时间。
     * @param expiresAt 过期时间。
     * @param tokenId   刷新令牌 ID（jti）。
     * @return 编码后的刷新令牌字符串。
     */
    private String encodeRefreshToken(User user, Instant issuedAt, Instant expiresAt, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .claim(CLAIM_USER_ID, user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 从 JWT 中提取用户 ID。
     *
     * @param jwt 已解析的 JWT。
     * @return 用户 ID（long）。
     * @throws IllegalArgumentException 当声明类型不合法时抛出。
     */
    public long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_USER_ID);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("Invalid user id in token");
    }

    /**
     * 提取令牌类型声明。
     *
     * @param jwt 已解析的 JWT。
     * @return 令牌类型字符串（例如："access" 或 "refresh"）。
     */
    public String extractTokenType(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_TOKEN_TYPE);
        return claim != null ? claim.toString() : "";
    }

    /**
     * 提取令牌 ID（jti）。
     *
     * @param jwt 已解析的 JWT。
     * @return 令牌 ID。
     */
    public String extractTokenId(Jwt jwt) {
        return jwt.getId();
    }
}
