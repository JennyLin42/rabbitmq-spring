package com.example.rabbitmqspring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static java.lang.Thread.sleep;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes={RabbitMqConfig.class})
public class RabbitmqSpringApplicationTests {

    @Test
    public void contextLoads() {
        System.out.println("bbb");
    }
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Test
    public void testRabbitAdmin(){
        //直连的交换机
        rabbitAdmin.declareExchange(new DirectExchange("test.direct",true,false));
        rabbitAdmin.declareExchange(new TopicExchange("test.topic",true,false));
        rabbitAdmin.declareExchange(new FanoutExchange("test.fanout",true,false));

        rabbitAdmin.declareQueue(new Queue("test.direct.queue",true));
        rabbitAdmin.declareQueue(new Queue("test.topic.queue",true));
        rabbitAdmin.declareQueue(new Queue("test.fanout.queue",true));

        //先定义号交换机和队列的绑定方法
        rabbitAdmin.declareBinding(new Binding(
                "test.direct.queue",//队列名称
                Binding.DestinationType.QUEUE,
                "test.direct",//交换机名称
                "direct",//routingkey
                new HashMap<>()));
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue("test.topic.queue",false)) //新建队列
                .to(new TopicExchange("test.topic",false,false))//新建交换机
                .with("user.#")//routingkey
        );
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue("test.fanout.queue",false))
                .to(new FanoutExchange("test.fanout",false,false))//Fanout 是完全匹配 不需要routingkey
        );
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testRabbitTemplate(){
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("desc","信息描述..");
        messageProperties.getHeaders().put("type","自定义类型..");
        Message message = new Message("Hello。。".getBytes(),messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.amqp", message, new MessagePostProcessor() {
            //额外添加的信息
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().getHeaders().put("desc","额外添加的信息描述..");
                message.getMessageProperties().getHeaders().put("attr","额外添加的属性");
                return message;
            }
        });
    }

    @Test
    public void testRabbitTemplate2(){
        rabbitTemplate.convertAndSend("topic001", "spring.amqp", "hello");
    }

    @Test
    public void testRabbitTemplate3(){
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("desc","信息描述..");
        messageProperties.getHeaders().put("type","自定义类型..");
        Message message = new Message("Hello。。".getBytes(),messageProperties);
        rabbitTemplate.send("topic001", "spring.amqp", message);
    }
}
