package seckill.demo.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seckill.demo.dao.OrderDao;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.redis.OrderKeyPrefix;
import seckill.demo.redis.RedisService;
import seckill.demo.vo.GoodsVo;

import java.util.Date;

@Service
public class OrderService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private RedisService redisService;

    /**
     * 通过用户id与商品id从订单列表中获取订单信息，这个地方用到了唯一索引（unique index!!!!!）
     * <p>
     * c5: 优化，不同每次都去数据库中读取秒杀订单信息，而是在第一次生成秒杀订单成功后，
     * 将订单存储在redis中，再次读取订单信息的时候就直接从redis中读取
     *
     * @param userId
     * @param goodsId
     * @return 秒杀订单信息
     */

    public SeckillOrder getSeckillOrderByUserIdAndGoodsId(Long userId, long goodsId) {

        // 从redis中取缓存，减少数据库的访问
        SeckillOrder seckillOrder = redisService.get(OrderKeyPrefix.getSeckillOrderByUidGid,
                ":" + userId+"_"+goodsId,SeckillOrder.class);
        if(seckillOrder != null){
            return seckillOrder;
        }
        //如果缓存中没有，则从数据库中查找。
        return orderDao.getSeckillOrderByUserIdAndGoodsId(userId,goodsId);
    }

    /**
     * 创建订单
     * <p>
     * c5: 增加redis缓存
     *
     * 增加两个表，是为了防止一个用户重复秒杀，在秒杀订单的表中增加一个unique索引。
     * @param user
     * @param goods
     * @return
     */
    public OrderInfo createOrder(SeckillUser user, GoodsVo goods){
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getSeckillPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());

        // 将订单信息插入order_info表中
        long orderId = orderDao.insert(orderInfo);
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setGoodsId(goods.getId());
        seckillOrder.setOrderId(orderId);
        seckillOrder.setUserId(user.getId());

        // 将秒杀订单插入seckill_order表中
        orderDao.insertSeckillOrder(seckillOrder);

        // 将秒杀订单信息存储于redis中
        redisService.set(OrderKeyPrefix.getSeckillOrderByUidGid,
                ":" + user.getId()+"_"+goods.getId(),seckillOrder);

        return orderInfo;
    }

    /**
     * 获取订单信息
     *
     * @param orderId
     * @return
     */
    public OrderInfo getOrderById(long orderId){
        return orderDao.getOrderById(orderId);
    }
}
