package seckill.demo.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.redis.RedisService;
import seckill.demo.service.GoodsService;
import seckill.demo.service.OrderService;
import seckill.demo.service.SeckillService;
import seckill.demo.vo.GoodsVo;


/**
 * c6:
 * MQ消息接收者
 * 消费者绑定在队列监听，既可以接收到队列中的消息
 */
@Service
public class MQReceiver {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private SeckillService seckillService;

    private static Logger logger = LoggerFactory.getLogger(MQReceiver.class);

    /**
     * Direct模式 交换机Exchange
     * @param message
     */
    @RabbitListener(queues = {MQConfig.QUEUE})
    public void receive(String message){
        logger.info("MQ: message: " + message);
    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void receiveTopic1(String message){
        logger.info("topic queue1 message: " + message);
    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void receiveTopic2(String message){
        logger.info("topic queue2 message: " + message);
    }
    @RabbitListener(queues = MQConfig.HEADER_QUEUE)
    public void receiveHeader(byte[] message){
        logger.info("header queue message: " + new String(message));
    }

    /**
     * 处理收到的秒杀成功信息
     *
     * @param message
     */
    @RabbitListener(queues = MQConfig.SECKILL_QUEUE)
    public void receiveSeckillInfo(String message){
        logger.info("MQ: message: " + message);

        SeckillMessage seckillMessage = RedisService.stringToBean(message,SeckillMessage.class);
        // 获取秒杀用户信息与商品id
        SeckillUser user = seckillMessage.getUser();
        long goodsId = seckillMessage.getGoodsId();

        //获取商品的库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if(stockCount < 0){
            return;
        }
        // 判断是否已经秒杀到了
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(),goodsId);
        if(order != null){
            return;
        }
        // 减库存 下订单 写入秒杀订单
        seckillService.seckill(user,goods);
    }
}
