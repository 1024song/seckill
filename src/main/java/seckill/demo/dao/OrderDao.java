package seckill.demo.dao;

import org.apache.ibatis.annotations.*;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;

/**
 * seckill_order表数据访问层
 */
@Mapper
public interface OrderDao {

    /**
     * 通过用户id与商品id从订单列表中获取订单信息
     *
     * @param userId  用户id
     * @param goodsId 商品id
     * @return 秒杀订单信息
     */
    @Select("select * from seckill_order where user_id=#{userId} and goods_id=#{goodsId}")
    SeckillOrder getSeckillOrderByUserIdAndGoodsId(@Param("userId") Long userId,
                                                   @Param("goodsId")Long goodsId);

    /**
     * 将订单信息插入seckill_order表中
     *
     * @param orderInfo 订单信息
     * @return 插入成功的订单信息id
     */
    @Insert("INSERT INTO order_info (user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)"
            + "VALUES (#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    // 查询出插入订单信息的表id，并返回
    @SelectKey(keyColumn = "id", keyProperty = "id", resultType = long.class, before = false, statement = "SELECT last_insert_id()")
    long insert(OrderInfo orderInfo);

    /**
     * 将秒杀订单信息插入到seckill_order中
     *
     * @param seckillOrder 秒杀订单
     */
    @Insert("INSERT INTO seckill_order(user_id, order_id, goods_id) VALUES (#{userId}, #{orderId}, #{goodsId})")
    void insertSeckillOrder(SeckillOrder seckillOrder);

    /**
     * 获取订单信息
     *
     * @param orderId
     * @return
     */
    @Select("select * from order_info where id = #{orderId}")
    OrderInfo getOrderById(@Param("orderId") long orderId);

    @Delete("delete from order_info")
    void deleteOrders();

    @Delete("delete from seckill_order")
    void deleteSeckillOrders();
}
