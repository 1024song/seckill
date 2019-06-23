package seckill.demo.service;

import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillUser;
import seckill.demo.vo.GoodsVo;

/**
 * 秒杀业务处理
 */
@Service
public class SeckillService {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private OrderService orderService;

    /**
     * 执行秒杀操作，包含以下两步：
     * 1. 从goods表中减库存
     * 2. 将生成的订单写入miaosha_order表中
     *
     * @param user  秒杀商品的用户
     * @param goods 所秒杀的商品
     * @return 生成的订单信息
     */
    @Transactional
    public OrderInfo seckill(SeckillUser user, GoodsVo goods){
        // 1. 减库存
        boolean success = goodsService.reduceStock(goods);
        // 2. 生成订单；向order_info表和maiosha_order表中写入订单信息

        return orderService.createOrder(user,goods);

    }
}
