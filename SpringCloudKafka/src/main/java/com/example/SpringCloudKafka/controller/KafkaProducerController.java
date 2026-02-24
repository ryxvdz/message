package com.example.SpringCloudKafka.controller;


import com.example.SpringCloudKafka.messaging.KafkaProducer;
import com.example.SpringCloudKafka.service.FraudeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/producer")
public class KafkaProducerController {
    private final KafkaProducer producerService;
    private final FraudeService fraudeService;

    public KafkaProducerController(KafkaProducer producerService, FraudeService fraudeService) {
        this.producerService = producerService;
        this.fraudeService = fraudeService;
    }



    @PostMapping("/publish")
    public void publish(@RequestParam String message){
        producerService.sendMessage(message);
    }

    @PostMapping("/IsFraude?")
    public void receiveKey(@RequestParam String key){
        fraudeService.isFraude(key);
    }
}
