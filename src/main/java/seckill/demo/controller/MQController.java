package seckill.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import seckill.demo.rabbitmq.MQSender;
import seckill.demo.result.Result;

/**
 * c6： MQ测试
 */
@Controller
@RequestMapping("/mq")
public class MQController {

    @Autowired
    private MQSender sender;

    @RequestMapping("/hello_mq")
    @ResponseBody
    public Result<String> helloMQ(){
        sender.send("Hello RabbitMQ");

        return Result.success("hello RabbitMQ!");
    }

    @RequestMapping("/topic_mq")
    @ResponseBody
    public Result<String> helloMQTopic(){
        sender.sendTopic("Hello RabbitMQ");

        return Result.success("hello RabbitMQ!");
    }

    /**
     * 将请求发送到fanout exchange
     *
     * @return
     */
    @RequestMapping("/fanout_mq")
    @ResponseBody
    public Result<String> fanout() {
        sender.sendFanout("Hello, RabbitMQ");

        return Result.success("hello RabbitMQ!");
    }


    /**
     * 将请求发送到Header exchange
     *
     * @return
     */
    @RequestMapping("/header_mq")
    @ResponseBody
    public Result<String> header() {
        sender.sendHeader("Hello, RabbitMQ");

        return Result.success("hello RabbitMQ!");
    }

}
