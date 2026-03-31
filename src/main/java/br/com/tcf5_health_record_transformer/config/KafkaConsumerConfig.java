package br.com.tcf5_health_record_transformer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // O Spring Boot 3 configura o ConcurrentKafkaListenerContainerFactory automaticamente
}