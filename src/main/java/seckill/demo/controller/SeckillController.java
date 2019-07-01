package seckill.demo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.result.CodeMsg;
import seckill.demo.service.GoodsService;
import seckill.demo.service.OrderService;
import seckill.demo.service.SeckillService;
import seckill.demo.vo.GoodsVo;

/**
 * 秒杀按钮的业务逻辑控制
 * c6: 在秒杀接口上做优化，使用MQ将请求入队
 */

@Controller
@RequestMapping("/miaosha")
public class SeckillController {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private SeckillService seckillService;

    /**
     * 秒杀逻辑
     * 用户点击秒杀按钮后的逻辑控制
     * <p>
     * c6: 使用MQ优化
     * QPS 93.6 优化之前
     *
     * @param model   页面model，用于存储带给页面的变量
     * @param user    秒杀用户
     * @param goodsId 秒杀的商品id
     * @return 执行秒杀后的跳转
     */
    @RequestMapping("/do_miaosha")
    public String doMiaosha(Model model, SeckillUser user, @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        // 1. 如果用户为空，则返回登录界面
        if (user == null)
            return "login";

        // 2. 用户不为空，说明用户已登录, 可以继续执行下面的操作

        // 2.1 判断库存，库存有才可以继续往下执行
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goodsVo.getStockCount();
        if(stock <= 0){
            model.addAttribute("errmsg", CodeMsg.SECKILL_OVER.getMsg());
            return "miaosha_fail";
        }
        // 2.2 判断用户是否已经完成秒杀，如果没有秒杀成功，继续执行

        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(),goodsId);
        if(order != null){
            model.addAttribute("errmsg", CodeMsg.REPEATE_SECKILL.getMsg());
            return "miaosha_fail";
        }

        // 2.3 完成秒杀操作：减库存，下订单，写入秒杀订单

        OrderInfo orderInfo = seckillService.seckill(user,goodsVo);
        model.addAttribute("orderInfo",orderInfo);
        model.addAttribute("goods",goodsVo);
        return "order_detail";
    }
}
