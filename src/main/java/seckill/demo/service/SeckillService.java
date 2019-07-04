package seckill.demo.service;

import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.redis.RedisService;
import seckill.demo.redis.SeckillKeyPrefix;
import seckill.demo.vo.GoodsVo;

import java.util.List;

/**
 * 秒杀业务处理
 */
@Service
public class SeckillService {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisService redisService;

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
        if(success) {
            return orderService.createOrder(user, goods);
        }else {
            setGoodsOver(goods.getId());
            return null;
        }
    }

    /**
     * 获取秒杀结果
     *
     * @param userId
     * @param goodsId
     * @return
     */
    public long getSeckillResult(Long userId,long goodsId){
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(userId, goodsId);
        if(order != null){//秒杀成功
            return order.getOrderId();
        }else {
            boolean isOver = getGoodsOver(goodsId);
            if(isOver){
                return -1;
            }else {
                return 0;
            }
        }
    }

    /**得到商品是否被秒杀完。
     *
     * @param goodsId
     * @return
     */
    private boolean getGoodsOver(long goodsId){
        return redisService.exists(SeckillKeyPrefix.isGoodsOver,""+goodsId);
    }

    /**
     *  秒杀完的设置
     * @param goodsId
     */
    public void setGoodsOver(long goodsId){
        redisService.set(SeckillKeyPrefix.isGoodsOver,""+goodsId,true);
    }

    public void reset(List<GoodsVo> goodsVoList){
        goodsService.resetStock(goodsVoList);
        orderService.deleteOrders();
    }
}
