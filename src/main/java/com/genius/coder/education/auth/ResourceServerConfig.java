package com.genius.coder.education.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**资源服务配置
 * @author GaoWeicai.(lili14520 @ gmail.com)
 * @date 2020/3/20
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Autowired
    private TokenStore tokenStore;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.stateless(true).tokenStore(tokenStore);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 所以在我们的app登录的时候我们只要提交的action，不要跳转到登录页
        http.formLogin()
                //登录页面
                .loginPage("/login")
                //登录提交action，app会用到
                // 用户名登录地址
                .loginProcessingUrl("/auth/token")
                //成功处理器 返回Token
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler());

        http
                // 手机验证码登录
                //.apply(SmsCodeAuthenticationSecurityConfig)
                //.and()
                .authorizeRequests()
                //手机验证码登录地址
                .antMatchers("/mobile/token", "/email/token")
                .permitAll()
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/api/**",
                        "/**/*.js",
                        "/**/*.css",
                        "/**/*.jpg",
                        "/**/*.png",
                        "/**/*.woff2",
                        "/wx/**")
                .permitAll()//以上的请求都不需要认证
                .anyRequest()
                .authenticated()
                .and()
                .csrf().disable();
    }

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler(){
        return new SavedRequestAwareAuthenticationSuccessHandler(){
            @Autowired
            private ObjectMapper objectMapper;

            @Autowired
            private ClientDetailsService clientDetailsService;

            @Autowired
            private AuthorizationServerTokenServices authorizationServerTokenServices;

            /*
             * (non-Javadoc)
             *
             * @see org.springframework.security.web.authentication.
             * AuthenticationSuccessHandler#onAuthenticationSuccess(javax.servlet.http.
             * HttpServletRequest, javax.servlet.http.HttpServletResponse,
             * org.springframework.security.core.Authentication)
             */
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                String header = request.getHeader("Authorization");
                String name = authentication.getName();
//        String password = (String) authentication.getCredentials();
                if (header == null || !header.startsWith("Basic ")) {
                    throw new UnapprovedClientAuthenticationException("请求头中无client信息");
                }

                String[] tokens = extractAndDecodeHeader(header, request);
                assert tokens.length == 2;
                String clientId = tokens[0];
                String clientSecret = tokens[1];

                ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

                if (clientDetails == null) {
                    throw new UnapprovedClientAuthenticationException("clientId对应的配置信息不存在:" + clientId);
                } else if (!StringUtils.equals(clientDetails.getClientSecret(), clientSecret)) {
                    throw new UnapprovedClientAuthenticationException("clientSecret不匹配:" + clientId);
                }

                TokenRequest tokenRequest = new TokenRequest(MapUtils.EMPTY_SORTED_MAP, clientId, clientDetails.getScope(), "custom");

                OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);

                OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

                OAuth2AccessToken token = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);
                tokenStore.storeAccessToken(token, oAuth2Authentication);//保存token到redis
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(token));
            }

            private String[] extractAndDecodeHeader(String header, HttpServletRequest request) throws IOException {

                byte[] base64Token = header.substring(6).getBytes("UTF-8");
                byte[] decoded;
                try {
                    decoded = Base64.decode(base64Token);
                } catch (IllegalArgumentException e) {
                    throw new BadCredentialsException("Failed to decode basic authentication token");
                }
                String token = new String(decoded, "UTF-8");
                int delim = token.indexOf(":");
                if (delim == -1) {
                    throw new BadCredentialsException("Invalid basic authentication token");
                }
                return new String[]{token.substring(0, delim), token.substring(delim + 1)};
            }
        };
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler loginFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {

            @Autowired
            private ObjectMapper objectMapper;


            /* (non-Javadoc)
             * @see org.springframework.security.web.authentication.AuthenticationFailureHandler#onAuthenticationFailure(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.security.core.AuthenticationException)
             */
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {

                System.out.println("----------failure-------");
                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }
}