package seckill.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.result.CodeMsg;
import seckill.demo.result.Result;
import seckill.demo.service.GoodsService;
import seckill.demo.service.OrderService;
import seckill.demo.vo.GoodsVo;
import seckill.demo.vo.OrderDetailVo;

/**
 * c5：订单详情页面静态化
 */
@Controller
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private GoodsService goodsService;

    /**
     * 获取订单详情
     *
     * @param model
     * @param user
     * @param orderId
     * @return
     */
    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> orderInfo(Model model,
                                           SeckillUser user,
                                           @RequestParam("orderId")long orderId){
        if(user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //获取订单消息
        OrderInfo order = orderService.getOrderById(orderId);
        if(order == null){
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }

        //如果订单存在
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setGoods(goods);
        vo.setOrder(order);
        return Result.success(vo);
    }
}
