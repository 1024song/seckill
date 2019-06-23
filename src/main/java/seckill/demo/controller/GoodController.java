package seckill.demo.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import seckill.demo.domain.SeckillUser;
import seckill.demo.redis.RedisService;
import seckill.demo.service.GoodsService;
import seckill.demo.service.SeckillUserService;
import seckill.demo.vo.GoodsVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodController {
    @Autowired
    private SeckillUserService seckillUserService;
    @Autowired
    private RedisService redisService;

    @Autowired
    private GoodsService goodsService;

    @RequestMapping("/to_list")
    public String list(Model model,SeckillUser user) {
        model.addAttribute("user", user);
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsList);
        return "goods_list";
    }

    @RequestMapping("/to_detail/{goodsId}")
    public String detail(Model model, SeckillUser user,
                         @PathVariable("goodsId")long goodsId) {
        model.addAttribute("user", user);

        // 通过商品id查询
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods",goodsVo);

        // 获取商品的秒杀开始与结束的时间
        long startAt = goodsVo.getStartDate().getTime();
        long endAt = goodsVo.getEndDate().getTime();
        long now = System.currentTimeMillis();

        // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
        int seckillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;

        if(now < startAt){// 秒杀未开始
            seckillStatus = 0;
            remainSeconds = (int)((startAt - now)/1000);
        }else if(now > endAt){// 秒杀已结束
            seckillStatus = 2;
            remainSeconds = -1;
        }else {
            seckillStatus = 1;
            remainSeconds = 0;
        }

        // 将秒杀状态和秒杀剩余时间传递给页面（goods_detail）
        model.addAttribute("seckillStatus", seckillStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        return "goods_detail";
    }

}
