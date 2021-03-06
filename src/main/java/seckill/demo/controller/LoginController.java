package seckill.demo.controller;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import seckill.demo.result.CodeMsg;
import seckill.demo.result.Result;
import seckill.demo.service.SeckillUserService;
import seckill.demo.util.ValidatorUtil;
import seckill.demo.vo.LoginVo;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private SeckillUserService seckillUserService;

    @RequestMapping("/to_login")
    public String toLogin(){
        return "login";
    }

    @RequestMapping("/do_login")
    @ResponseBody
    public Result<Boolean> doLogin(HttpServletResponse response, @Valid LoginVo loginVo){
        log.info(loginVo.toString());

        //参数校验,
        /* 参数校验（没有使用@Valid对参数校验时的参数校验方式, 使用时注释掉这段，没有使用时需要取消注释）
        String mobile = loginVo.getMobile();
        String passInput = loginVo.getPassword();
        if(StringUtils.isEmpty(mobile)){
            return Result.error(CodeMsg.MOBILE_EMPTY);
        }
        if(StringUtils.isEmpty(passInput)){
            return Result.error(CodeMsg.PASSWORD_EMPTY);
        }
        if(!ValidatorUtil.isMobile(mobile)){
            return Result.error(CodeMsg.MOBILE_ERROR);
        }
         */
        //登陆
        seckillUserService.login(response,loginVo);
        return Result.success(true);
    }


    @RequestMapping("/create_token")
    @ResponseBody
    public Result<String> createToken(HttpServletResponse response, @Valid LoginVo loginVo) {
        log.info(loginVo.toString());
        String token = seckillUserService.login(response, loginVo);
        return Result.success(token);
    }

}
