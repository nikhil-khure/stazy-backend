package com.stazy.backend.common.config;

import com.stazy.backend.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.stazy.backend.common.config.AppProperties;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AppProperties appProperties;

    public WebSocketConfig(JwtService jwtService, UserDetailsService userDetailsService, AppProperties appProperties) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.appProperties = appProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = appProperties.getCors().getAllowedOrigins().split(",");
        for (int i = 0; i < allowedOrigins.length; i++) {
            allowedOrigins[i] = allowedOrigins[i].trim();
        }
        
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String authHeader = authorization.get(0);
                        if (authHeader.startsWith("Bearer ")) {
                            String jwt = authHeader.substring(7);
                            try {
                                String userEmail = jwtService.extractSubject(jwt);
                                if (userEmail != null && jwtService.isValid(jwt, userEmail)) {
                                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    );
                                    accessor.setUser(authentication);
                                }
                            } catch (Exception e) {
                                // Invalid token
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
