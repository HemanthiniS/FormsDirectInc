package com.formsdirectinc.spring.config;

import com.formsdirectinc.ui.auth.Authenticator;
import org.springframework.context.annotation.*;

@Configuration
@Profile("!dev")
@ComponentScan(basePackages = {"com.formsdirectinc"})
public class RegistrationConfig {
    @Bean
    public Authenticator authenticator() {
        return new Authenticator();
    }
}
