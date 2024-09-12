package com.formsdirectinc.services;

import com.formsdirectinc.environment.Environment;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;


@Service
public class AuthService {

    @Autowired
    private Environment environment;

    public String getTokens(HttpServletRequest request, String authorizationCode){

        HttpResponse<String> authResponse;
        String auth0AuthenticationURL = String.format("https://%s/oauth/token", environment.getProperty("com.auth0.domain"));

        try {
            authResponse = Unirest.post(auth0AuthenticationURL)
                    .header("content-type", "application/x-www-form-urlencoded")
                    .body("grant_type=authorization_code&client_id=" + environment.getProperty("com.auth0.clientId") + "&client_secret=" + environment.getProperty("com.auth0.clientSecret") + "&code=" + authorizationCode + "&redirect_uri=https://" + request.getServerName() + "/registration/callback.do")
                    .asString();

        } catch (UnirestException e) {
            throw new RuntimeException(String.format("Error while endpoint request - %s", e));
        }

        return authResponse.getBody();
    }
}
