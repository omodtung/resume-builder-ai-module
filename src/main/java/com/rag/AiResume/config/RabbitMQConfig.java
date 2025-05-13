package com.rag.AiResume.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit //
public class RabbitMQConfig {

  public static final String RAG_QUEUE_NAME = "ragQueue";

  @Bean
  public Queue ragQueue() {
    return new Queue(RAG_QUEUE_NAME, true);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  // Configure the listener container factory to use the JSON message converter
  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
    ConnectionFactory connectionFactory,
    // SimpleRabbitListenerContainerFactoryConfigurer configurer
    MessageConverter messageConverter 
  ) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);

    // You can set other properties here like concurrency, prefetch count, etc.
    return factory;
  }
}
