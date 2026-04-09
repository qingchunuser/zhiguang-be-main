package com.tongji.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 *
 * Spring Security 安全配置。
 * <p>
 * - 关闭 CSRF（后端纯 API，使用 JWT 无会话）；
 * - 启用 CORS，当前允许所有来源（后续需替换白名单）；
 * - 无状态会话；
 * - 公开认证相关接口与健康检查，其余接口需鉴权；
 * - 资源服务器启用 JWT 校验。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    /**
     * 配置 Spring Security 过滤链。
     *
     * <p>主要包含：</p>
     * - 关闭 CSRF；
     * - 启用 CORS；
     * - 使用无状态会话策略；
     * - 公开认证接口与健康检查，其余接口需鉴权；
     * - 启用资源服务器的 JWT 校验。
     *
     * @param http Spring 的 {@link HttpSecurity} 构建器。
     * @return 构建完成的 {@link SecurityFilterChain}。
     * @throws Exception 构建过滤链过程中可能抛出的异常。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        /*.csrf() - CSRF防护配置
           功能：CSRF（Cross-Site Request Forgery，跨站请求伪造）防护
           作用：防止恶意网站利用用户的认证状态发起非预期的请求
           在本项目中：使用.csrf(AbstractHttpConfigurer::disable)禁用了CSRF保护，这是因为在无状态的JWT认证系统中（使用STATELESS会话策略），通常不需要CSRF保护

           .cors() - CORS跨域配置
           功能：CORS（Cross-Origin Resource Sharing，跨源资源共享）配置
           作用：控制哪些外部域名可以访问你的API
           在本项目中：使用.cors(Customizer.withDefaults())启用了默认的跨域配置，允许前端应用从不同域名访问后端API

           .sessionManagement() - 会话管理配置
           功能：配置Spring Security如何处理用户会话
           作用：决定是否使用HTTP Session以及如何管理认证状态
           在本项目中：设置为.sessionCreationPolicy(SessionCreationPolicy.STATELESS)，表示不创建HTTP会话，适用于无状态的REST API，每次请求都携带认证信息（如JWT）

           .authorizeHttpRequests() - 请求授权配置
           功能：定义哪些URL路径需要认证，哪些可以公开访问
           作用：实现细粒度的访问控制
           在本项目中：

           .permitAll() - 允许所有用户访问（如登录、注册、健康检查等）

           .anyRequest().authenticated() - 其他所有请求都需要身份验证

           .oauth2ResourceServer() - OAuth2资源服务器配置
           功能：将应用配置为OAuth2资源服务器
           作用：验证传入请求中的访问令牌（如JWT）
           在本项目中：使用.jwt(Customizer.withDefaults())配置了JWT令牌验证，与前面配置的jwtEncoder/jwtDecoder配合工作
           这些配置共同构成了一个完整的安全体系，保护API免受各种攻击，同时提供灵活的访问控制策略。*/
        http

                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()//允许所有用户访问（如登录、注册、健康检查等）
                        // 公开内容：首页 Feed 不需要登录
                        .requestMatchers("/api/v1/knowposts/feed").permitAll()//允许所有用户访问（如登录、注册、健康检查等）
                        // 知文详情（公开已发布内容，非公开由服务层校验）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/knowposts/detail/*").permitAll()
                        // 知文详情页 RAG 问答（SSE 流式输出）允许匿名访问
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/knowposts/*/qa/stream").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/send-code",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/token/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password/reset"
                        ).permitAll()
                        .anyRequest().authenticated()//其他所有请求都需要身份验证
                )
                /*Bean自动发现：当Spring Security看到配置了oauth.jwt()时，它会在应用上下文中查找类型为JwtDecoder的Bean
                  自动装配：由于你在AuthConfiguration中定义了名为jwtDecoder的@Bean方法，Spring Security会自动使用这个Bean来处理JWT解码
                  默认行为：Customizer.withDefaults()表示使用Spring Security的默认配置，其中包括自动使用上下文中存在的JwtDecoder Bean*/
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * 定义并提供 CORS 配置源。
     *
     * <p>当前允许所有来源（后续建议替换为产品白名单），允许常见方法与请求头，且不携带凭证。</p>
     *
     * @return {@link CorsConfigurationSource}，用于为所有路径注册 CORS 规则。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // TODO replace with product whitelist
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
