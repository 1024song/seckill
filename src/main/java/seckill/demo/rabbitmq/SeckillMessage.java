package seckill.demo.rabbitmq;


import seckill.demo.domain.SeckillUser;

/**
 * 在MQ中传递的秒杀信息
 * 包含参与秒杀的用户和商品的id
 */
public class SeckillMessage {
    private SeckillUser user;
    private long goodsId;

    public SeckillUser getUser() {
        return user;
    }

    public void setUser(SeckillUser user) {
        this.user = user;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }

    @Override
    public String toString() {
        return "SeckillMessage{" +
                "user=" + user +
                ", goodsId=" + goodsId +
                '}';
    }
}
