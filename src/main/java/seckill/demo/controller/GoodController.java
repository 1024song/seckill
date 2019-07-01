package seckill.demo.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import seckill.demo.domain.SeckillUser;
import seckill.demo.redis.GoodsKeyPrefix;
import seckill.demo.redis.RedisService;
import seckill.demo.service.GoodsService;
import seckill.demo.service.SeckillUserService;
import seckill.demo.vo.GoodsVo;

import javax.servlet.http.HttpServletRequest;
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
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;
    @Autowired
    private ApplicationContext applicationContext;

    /*
    @RequestMapping("/to_list")
    public String list(Model model,SeckillUser user) {
        model.addAttribute("user", user);
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsList);
        return "goods_list";
    }*/

    /**
     * 从数据库中获取商品信息（包含秒杀信息）
     * 页面级缓存实现；从redis中取页面，如果没有则需要手动渲染页面，并且将渲染的页面存储在redis中供下一次访问时获取
     *
     * @param model 响应的资源文件
     * @param user   通过自定义参数解析器UserArgumentResolver解析的 SeckillUser 对象
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/to_list",produces = "text/html")
    @ResponseBody
    public String list(Model model, SeckillUser user,
                       HttpServletRequest request,
                       HttpServletResponse response) {

        model.addAttribute("user", user);
        // 1. 从redis缓存中取html
        String html = redisService.get(GoodsKeyPrefix.goodsListKeyPrefix,"",String.class);
        if(!StringUtils.isEmpty(html)){
            return html;
        }

        // 2. 如果redis中不存在该缓存，则需要手动渲染

        // 查询商品列表，用于手动渲染时将商品数据填充到页面
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList",goodsList);
        // 3. 渲染html
        //包含业务数据的context。
        WebContext webContext = new WebContext(request,response,request.getServletContext()
        ,request.getLocale(),model.asMap());

        html = thymeleafViewResolver.getTemplateEngine().process("goods_list",webContext);

        if(!StringUtils.isEmpty(html)){
            redisService.set(GoodsKeyPrefix.goodsListKeyPrefix,"",html);
        }
        return html;
    }

   /*
   //未做缓存
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
    }*/

    /**
     *处理商品详情页（未做页面静态化处理）
     *
     * <p>
     *c5: URL级缓存实现；从redis中取商品详情页面，如果没有则需要手动渲染页面，
     *    并且将渲染的页面存储在redis中供下一次访问时获取。
     *    实际上URL级缓存和页面级缓存是一样的，只不过URL级缓存会根据url的参数从redis中取不同的数据
     *
     * @param model  页面的域对象
     * @param user   用户信息
     * @param request
     * @param response
     * @param goodsId   商品id
     * @return   商品详情页
     */
   @RequestMapping(value = "/to_detail/{goodsId}",produces = "text/html")// 注意这种写法
   @ResponseBody
   public String detail(Model model, SeckillUser user,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        @PathVariable("goodsId")long goodsId) {

       //1.根据商品id从redis中取详情数据的缓存
       String html = redisService.get(GoodsKeyPrefix.goodsDetailKeyPrefix,"",String.class);
       if(!StringUtils.isEmpty(html)){
           return html;
       }
       //2.如果缓存不存在，则需要手动渲染详情页面数据并返回
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

       WebContext webContext = new WebContext(request,response,request.getServletContext(),
               response.getLocale(),model.asMap());

       html = thymeleafViewResolver.getTemplateEngine().process("goods_detail",webContext);

       if(!StringUtils.isEmpty(html)){
           redisService.set(GoodsKeyPrefix.goodsDetailKeyPrefix,""+ goodsId,html);
       }
       return html;
   }


}
