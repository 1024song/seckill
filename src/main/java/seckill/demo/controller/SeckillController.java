package seckill.demo.controller;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import seckill.demo.domain.OrderInfo;
import seckill.demo.domain.SeckillOrder;
import seckill.demo.domain.SeckillUser;
import seckill.demo.rabbitmq.MQSender;
import seckill.demo.rabbitmq.SeckillMessage;
import seckill.demo.redis.GoodsKeyPrefix;
import seckill.demo.redis.OrderKeyPrefix;
import seckill.demo.redis.RedisService;
import seckill.demo.redis.SeckillKeyPrefix;
import seckill.demo.result.CodeMsg;
import seckill.demo.result.Result;
import seckill.demo.service.GoodsService;
import seckill.demo.service.OrderService;
import seckill.demo.service.SeckillService;
import seckill.demo.vo.GoodsVo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀按钮的业务逻辑控制
 * c6: 在秒杀接口上做优化，使用MQ将请求入队
 */

@Controller
@RequestMapping("/miaosha")
public class SeckillController implements InitializingBean {

    @Autowired
    private GoodsService goodsService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private MQSender sender;

    private Map<Long,Boolean> localOverMap = new HashMap<>();


    /**
     * 系统初始化
     **/
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if(goodsList == null){
            return;
        }
        for(GoodsVo goods : goodsList){
            redisService.set(GoodsKeyPrefix.seckillGoodsStockPrefix,""+goods.getId(),goods.getStockCount());
            localOverMap.put(goods.getId(),false);
        }
    }

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
    /*
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
        //用redis实现
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
     */
    /**
     * 秒杀逻辑
     * 用户点击秒杀按钮后的逻辑控制
     * <p>
     * c6: 使用MQ优化之后
     * QPS 367.9
     *
     * @param model   页面model，用于存储带给页面的变量
     * @param user    秒杀用户
     * @param goodsId 秒杀的商品id
     * @return 执行秒杀后的跳转
     */
    @RequestMapping(value = "/do_miaosha",method=RequestMethod.POST)
    @ResponseBody
    public Result<Integer> doMiaosha(Model model, SeckillUser user,
                            @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if(user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //内存标记，减少redis的访问
        boolean over = localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.SECKILL_OVER);
        }
        //预减库存,返回减少之后的数值
        long stock = redisService.decr(GoodsKeyPrefix.seckillGoodsStockPrefix,""+goodsId);
        if(stock < 0){
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.SECKILL_OVER);
        }

        // 2.2 判断用户是否已经完成秒杀，如果没有秒杀成功，继续执行
        SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(),goodsId);
        if(order != null){
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }
        // 2.3 入队
        SeckillMessage mm = new SeckillMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);//排队中
    }

    /**
     * c5: 秒杀逻辑（页面静态化分离，不需要直接将页面返回给客户端，而是返回客户端需要的页面动态数据，返回数据时json格式）
     * <p>
     * <p>
     * GET/POST的@RequestMapping是有区别的
     * <p>
     * c6： 通过随机的path，客户端隐藏秒杀接口
     *
     * @param model
     * @param user
     * @param goodsId
     * @param path    隐藏的秒杀地址，为客户端回传的path，最初也是有服务端产生的
     * @return 订单详情或错误码
     */
    // {path}为客户端回传的path，最初也是有服务端产生的
    @RequestMapping(value = "/{path}/do_miaosha_static", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> doMiaoshaStatic(Model model, SeckillUser user,
                                           @RequestParam("goodsId") long goodsId,
                                           @PathVariable("path") String path){
        model.addAttribute("user",user);
        //1.如果用户为空，则返回登陆界面
        if(user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        return Result.success(0);

    }


    /**
     * 用于返回用户秒杀的结果
     *
     * @param model
     * @param user
     * @param goodsId
     * @return orderId：成功, -1：秒杀失败, 0： 排队中
     */
    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model,SeckillUser user,
                                      @RequestParam("goodsId") long goodsId){
        model.addAttribute("user",user);
        if(user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result = seckillService.getSeckillResult(user.getId(),goodsId);
        return Result.success(result);
    }

    /**
     * 重置数据库中的商品数量和redis中的状态
     * @param model
     * @return
     */
    @RequestMapping(value = "/reset",method = RequestMethod.GET)
    @ResponseBody
    public Result<Boolean> reset(Model model){
        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
        for(GoodsVo goods : goodsVoList){
            goods.setStockCount(10);
            redisService.set(GoodsKeyPrefix.seckillGoodsStockPrefix,""+goods.getId(),10);
            localOverMap.put(goods.getId(),false);
        }
        redisService.delete(OrderKeyPrefix.getSeckillOrderByUidGid);
        redisService.delete(SeckillKeyPrefix.isGoodsOver);
        seckillService.reset(goodsVoList);
        return Result.success(true);
    }
}
