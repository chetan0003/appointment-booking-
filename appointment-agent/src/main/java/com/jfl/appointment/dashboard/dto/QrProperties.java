package com.jfl.appointment.dashboard.dto;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.qr")
public class QrProperties {

    private String baseUrl = "http://localhost:5173";

    private int width = 400;

    private int height = 400;
}
