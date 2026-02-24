package com.example.SpringCloudKafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class chavesExistentesConfig {


    @Bean
    public Map<String, LocalDateTime> chaveExistente(){
        return new HashMap<>();
    }


}
