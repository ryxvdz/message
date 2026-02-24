package com.example.SpringCloudKafka.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class KafkaConsumer {


    @Bean
    public Consumer<Message<String>> testConsumer(){
        return message ->{
            System.out.println("Mensagem recebida: "+ message.getPayload());
        };
    }

}
