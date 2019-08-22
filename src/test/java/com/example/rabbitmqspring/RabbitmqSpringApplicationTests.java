package com.example.rabbitmqspring;

import com.example.rabbitmqspring.spring.entity.Order;
import com.example.rabbitmqspring.spring.entity.Packaged;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

import static java.lang.Thread.sleep;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {RabbitMqConfig.class})
public class RabbitmqSpringApplicationTests {

    @Test
    public void contextLoads() {
        System.out.println("bbb");
        int i = 1;
    }

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Test
    public void testRabbitAdmin() {
        //直连的交换机
        rabbitAdmin.declareExchange(new DirectExchange("test.direct", true, false));
        rabbitAdmin.declareExchange(new TopicExchange("test.topic", true, false));
        rabbitAdmin.declareExchange(new FanoutExchange("test.fanout", true, false));

        rabbitAdmin.declareQueue(new Queue("test.direct.queue", true));
        rabbitAdmin.declareQueue(new Queue("test.topic.queue", true));
        rabbitAdmin.declareQueue(new Queue("test.fanout.queue", true));

        //先定义号交换机和队列的绑定方法
        rabbitAdmin.declareBinding(new Binding(
                "test.direct.queue",//队列名称
                Binding.DestinationType.QUEUE,
                "test.direct",//交换机名称
                "direct",//routingkey
                new HashMap<>()));
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue("test.topic.queue", false)) //新建队列
                .to(new TopicExchange("test.topic", false, false))//新建交换机
                .with("user.#")//routingkey
        );
        rabbitAdmin.declareBinding(BindingBuilder
                .bind(new Queue("test.fanout.queue", false))
                .to(new FanoutExchange("test.fanout", false, false))//Fanout 是完全匹配 不需要routingkey
        );
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testRabbitTemplate() {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("desc", "信息描述..");
        messageProperties.getHeaders().put("type", "自定义类型..");
        Message message = new Message("Hello。。".getBytes(), messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.amqp", message, new MessagePostProcessor() {
            //额外添加的信息
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().getHeaders().put("desc", "额外添加的信息描述..");
                message.getMessageProperties().getHeaders().put("attr", "额外添加的属性");
                return message;
            }
        });
    }

    @Test
    public void testRabbitTemplate2() {
        rabbitTemplate.convertAndSend("topic001", "spring.amqp", "hello");
    }

    @Test
    public void testRabbitTemplate3() {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("desc", "信息描述..");
        messageProperties.getHeaders().put("type", "自定义类型..");
        messageProperties.setContentType("text/plain");
        Message message = new Message("Hello。。".getBytes(), messageProperties);
        rabbitTemplate.send("topic001", "spring.amqp", message);
    }

    @Test
    public void testSendJsonMessage() throws Exception {
        Order order = new Order();
        order.setId("001");
        order.setName("消息订单");
        order.setContent("描述信息");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.out.println("order 4:" + json);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");//这个一点要记得设置
        Message message = new Message(json.getBytes(), messageProperties);
        rabbitTemplate.send("topic001", "spring.amqp", message);
    }

    @Test
    public void testSendJavaMessage2() throws Exception {
//        Order order = new Order();
//        order.setId("001");
//        order.setName("消息订单");
//        order.setContent("描述信息");
//
//        ObjectMapper mapper = new ObjectMapper();
//        String json = mapper.writeValueAsString(order);
//        System.out.println("order 4:"+json);
//        MessageProperties messageProperties = new MessageProperties();
//        messageProperties.setContentType("application/json");//这个一点要记得设置
//        messageProperties.getHeaders().put("__TypeId__","com.example.rabbitmqspring.spring.entity.Order");
//        Message message = new Message(json.getBytes(),messageProperties);
//        rabbitTemplate.send("topic001", "spring.amqp", message);


        Order order = new Order();
        order.setId("001");
        order.setName("订单消息");
        order.setContent("订单描述信息");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.err.println("order 4 json: " + json);

        MessageProperties messageProperties = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties.setContentType("application/json");
        messageProperties.getHeaders().put("__TypeId__", "com.example.rabbitmqspring.spring.entity.Order");
        Message message = new Message(json.getBytes(), messageProperties);

        rabbitTemplate.send("topic001", "spring.order", message);
    }

    @Test
    public void testSendMappingMessage() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Order order = new Order();
        order.setId("001");
        order.setName("订单消息");
        order.setContent("订单描述信息");

        String json1 = mapper.writeValueAsString(order);
        System.err.println("order 4 json: " + json1);

        MessageProperties messageProperties1 = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties1.setContentType("application/json");
        messageProperties1.getHeaders().put("__TypeId__", "order");
        Message message1 = new Message(json1.getBytes(), messageProperties1);
        rabbitTemplate.send("topic001", "spring.order", message1);

        Packaged pack = new Packaged();
        pack.setId("002");
        pack.setName("包裹消息");
        pack.setDescription("包裹描述信息");

        String json2 = mapper.writeValueAsString(pack);
        System.err.println("pack 4 json: " + json2);

        MessageProperties messageProperties2 = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties2.setContentType("application/json");
        messageProperties2.getHeaders().put("__TypeId__", "packaged");
        Message message2 = new Message(json2.getBytes(), messageProperties2);
        rabbitTemplate.send("topic001", "spring.pack", message2);
    }
}
