package ec.edu.espe.banquito.accountcore.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue clearingOutboundQueue(
            @Value("${app.rabbitmq.clearing-queue:clearing.outbound.queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public DirectExchange clearingExchange(
            @Value("${app.rabbitmq.clearing-exchange:clearing.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public Binding clearingOutboundBinding(Queue clearingOutboundQueue, DirectExchange clearingExchange,
            @Value("${app.rabbitmq.clearing-routing-key:clearing.outbound}") String routingKey) {
        return BindingBuilder.bind(clearingOutboundQueue).to(clearingExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
