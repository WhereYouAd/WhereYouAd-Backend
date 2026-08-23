package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerErrorHandlerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> pushKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1_000L, 3L)));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> notificationKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            @Value("${notification.alert.retry.interval-seconds:60}") long retryIntervalSeconds,
            @Value("${notification.alert.retry.max-attempts:15}") long maxRetryAttempts
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(retryIntervalSeconds * 1_000L, maxRetryAttempts)));
        return factory;
    }
}
