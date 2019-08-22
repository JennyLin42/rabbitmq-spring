package com.example.rabbitmqspring.spring.adaper;

import com.example.rabbitmqspring.spring.entity.Order;
import com.example.rabbitmqspring.spring.entity.Packaged;

import java.io.File;
import java.util.Map;

public class MessageDelegate {
    //方法名称是固定的
    public void handleMessage(byte[] messageBody) {
        System.out.println("默认方法" + new String(messageBody));
    }

    public void consumeMessage(byte[] messageBody) {
        System.out.println("非默认方法 修改的调用方法： " + new String(messageBody));
    }

    public void consumeMessage(String messageBody) {
        System.out.println("非默认方法 String参数： " + messageBody);
    }

    public void method1(String messageBody) {
        System.out.println("指定的Method方法 String参数： " + messageBody);
    }

    public void consumeMessage(Map messageBody) {
        System.out.println("非默认方法 json： " + messageBody);
    }

    public void consumeMessage(Order order) {
        System.out.println("非默认方法 order对象： " + order.getId() + order.getName() + order.getContent());
    }

    public void consumeMessage(Packaged packaged) {
        System.out.println("非默认方法 packaged对象： " + packaged.getId() + packaged.getName() + packaged.getDescription());
    }

    public void consumeMessage(File file) {
        System.out.println("非默认方法 file： " + file.getName());
    }
}
