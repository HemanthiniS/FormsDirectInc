package com.formsdirectinc.spring.config;

import com.formsdirectinc.interceptor.LanguageInterceptor;
import com.formsdirectinc.tenant.config.TenantInterceptor;
import com.formsdirectinc.ui.filters.i18n.LanguageFilter;
import net.rossillo.spring.web.mvc.CacheControlHandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@Configuration
@EnableWebMvc
@Profile("!dev")
@ComponentScan(basePackages = { "com.formsdirectinc"})
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Value("/WEB-INF/classes/com/formsdirectinc/registration/resources/ResourceBundle")
    private String basename;

    @Value("${authorize.controller.allowed.origins:}")
    private String authorizeControllerAllowedOrigins;

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(new Locale("en"));
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor());
        registry.addInterceptor(languageInterceptor());
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(cacheControlHandlerInterceptor());
    }

    @Bean
    public LanguageInterceptor languageInterceptor() {
        LanguageInterceptor languageInterceptor = new LanguageInterceptor();
        return languageInterceptor;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Bean
    public CacheControlHandlerInterceptor cacheControlHandlerInterceptor() {
        return new CacheControlHandlerInterceptor();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!authorizeControllerAllowedOrigins.isEmpty()) {
            registry.addMapping("/authorizeUser.do")
                    .allowedOrigins(authorizeControllerAllowedOrigins.split(","))
                    .allowedMethods("*")
                    .allowedHeaders("*");
        }
    }

}
