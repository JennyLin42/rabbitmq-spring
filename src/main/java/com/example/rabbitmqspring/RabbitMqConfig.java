package com.example.rabbitmqspring;

import com.example.rabbitmqspring.spring.adaper.MessageDelegate;
import com.example.rabbitmqspring.spring.converter.TextMessageConverter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


//Configuration 表示这个是一个xml
//ComponentScan主要就是定义扫描的路径从中找出标识了需要装配的类自动装配到spring的bean容器中
@Configuration
@ComponentScan({"com.example.rabbitmqspring.*"})
public class RabbitMqConfig {

    //Bean表示这是一个<Bean></Bean>注入
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses("192.168.127.129:5672");
        connectionFactory.setPassword("guest");
        connectionFactory.setUsername("guest");
        connectionFactory.setVirtualHost("/");
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    public TopicExchange exchange001() {
        return new TopicExchange("topic001", true, false);
    }

    @Bean
    public Queue queue001() {
        return new Queue("queue001", true);
    }

    @Bean
    public Binding binding001() {
        return BindingBuilder.bind(queue001()).to(exchange001()).with("spring.#");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer messageContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue001());
        container.setConcurrentConsumers(1);//当前的消费者数量的值为 1
        container.setMaxConcurrentConsumers(5);//最大消费者数量
        container.setDefaultRequeueRejected(false);//是否重回队列
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);//签收模式 手动签收 自动签收
        container.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {
                return queue + "_" + UUID.randomUUID().toString();
            }
        });//消费端的标签策略

//        container.setMessageListener(new ChannelAwareMessageListener() {
//            @Override
//            public void onMessage(Message message, Channel channel) throws Exception {
//                String msg = new String(message.getBody());
//                System.out.println("msg"+ msg);
//            }
//        });//消息监听

        //消息监听的适配器方式：
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        container.setMessageListener(messageListenerAdapter);//调用默认的方法

//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        messageListenerAdapter.setDefaultListenerMethod("consumeMessage"); //配置调用的方法
//        container.setMessageListener(messageListenerAdapter);

//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        messageListenerAdapter.setDefaultListenerMethod("consumeMessage");
//        messageListenerAdapter.setMessageConverter(new TextMessageConverter());//配置调用的方法 并转换参数为string
//        container.setMessageListener(messageListenerAdapter);

        //队列名和方法名也可以一一对应 就是queue001会找到method1这个方法   文本转换器
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        Map<String,String> queueOrTagToMethodName = new HashMap<>();
//        queueOrTagToMethodName.put("queue001","method1");
//        messageListenerAdapter.setQueueOrTagToMethodName(queueOrTagToMethodName);
//        messageListenerAdapter.setMessageConverter(new TextMessageConverter());//配置每个队列需要调用的方法 并转换参数为string
//        container.setMessageListener(messageListenerAdapter);

        //json转换器
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        messageListenerAdapter.setDefaultListenerMethod("consumeMessage");
//        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();//配置调用的方法 并转换参数为JSON 就是map
//        messageListenerAdapter.setMessageConverter(jackson2JsonMessageConverter);
//        container.setMessageListener(messageListenerAdapter);

        //json转换器 支持java对象转换 2.1.7版本此代码会报错 可看源码
//        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());
//        messageListenerAdapter.setDefaultListenerMethod("consumeMessage");
//        Jackson2JsonMessageConverter jackson2JsonMessageConverter =new Jackson2JsonMessageConverter();//配置调用的方法 并转换参数为JSON 就是map
//        DefaultJackson2JavaTypeMapper jackson2JavaTypeMapper = new DefaultJackson2JavaTypeMapper();
//        jackson2JsonMessageConverter.setJavaTypeMapper(jackson2JavaTypeMapper);//再转换成Order  java对象
//        messageListenerAdapter.setMessageConverter(jackson2JsonMessageConverter);
//        container.setMessageListener(messageListenerAdapter);


        //DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象多映射转换
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<String, Class<?>>();
        idClassMapping.put("order", com.example.rabbitmqspring.spring.entity.Order.class);
        idClassMapping.put("packaged", com.example.rabbitmqspring.spring.entity.Packaged.class);

        javaTypeMapper.setIdClassMapping(idClassMapping);

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);


        return container;
    }


/*    @Bean
    public Jackson2JsonMessageConverter customConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(new ClassMapper() {
            @Override
            public Class<?> toClass(MessageProperties properties) {
                throw new UnsupportedOperationException("this mapper is only for outbound, do not use for receive message");
            }
            @Override
            public void fromClass(Class<?> clazz, MessageProperties properties) {
                properties.setHeader("__TypeId__", "com.example.rabbitmqspring.spring.entity.Order");
            }
        });
        return converter;
    }*/


}
