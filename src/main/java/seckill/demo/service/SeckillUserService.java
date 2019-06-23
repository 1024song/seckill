package seckill.demo.service;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seckill.demo.dao.SeckillUserDao;
import seckill.demo.domain.SeckillUser;
import seckill.demo.exception.GlobalException;
import seckill.demo.redis.RedisService;
import seckill.demo.redis.SeckillUserKeyPrefix;
import seckill.demo.result.CodeMsg;
import seckill.demo.result.Result;
import seckill.demo.util.MD5Util;
import seckill.demo.util.UUIDUtil;
import seckill.demo.vo.LoginVo;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class SeckillUserService {
    private static Logger log = LoggerFactory.getLogger(SeckillUserService.class);
    public static final String COOKIE_NAME_TOKEN = "token";

    @Autowired
    private SeckillUserDao seckillUserDao;

    @Autowired
    private RedisService redisService;

    public SeckillUser getById(Long id){
        return seckillUserDao.getById(id);
    }

    public SeckillUser getByToken(HttpServletResponse response,String token){
        if(StringUtils.isEmpty(token)){
            return null;
        }
        SeckillUser user = redisService.get(SeckillUserKeyPrefix.token,token,SeckillUser.class);
        //延长有限期
        if(user != null){
            addCookie(response, token, user);
        }
        return user;
    }

    /**
     * 用户登录, 要么处理成功返回true，否则会抛出全局异常
     * 抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器
     *
     * @param loginVo 封装了客户端请求传递过来的数据（即账号密码）
     *                （使用post方式，请求参数放在了请求体中，这个参数就是获取请求体中的数据）
     * @return 登录成功与否
     */

    public Result<Boolean> login(HttpServletResponse response,LoginVo loginVo){
        if(loginVo == null){
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String fromPass = loginVo.getPassword();
        //判断手机号是否存在
        SeckillUser seckillUser = this.getById(Long.parseLong(mobile));
        if(seckillUser == null){
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        // 判断手机号对应的密码是否一致
        String dbPass = seckillUser.getPassword();
        String dbSalt = seckillUser.getSalt();
        log.info(dbPass + " " + dbSalt);
        String calcPass = MD5Util.fromPassToDBPass(fromPass,dbSalt);
        log.info(calcPass);
        if(!calcPass.equals(dbPass)){
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }

        // 执行到这里表明登录成功了
        // 生成cookie
        String token = UUIDUtil.uuid();
        addCookie(response,token,seckillUser);

        return Result.success(true);
    }

    private void addCookie(HttpServletResponse response, String token, SeckillUser user) {
        // 每次访问都会生成一个新的session存储于redis和反馈给客户端，
        // 一个session对应存储一个seckillUser对象
        redisService.set(SeckillUserKeyPrefix.token, token, user);
        // 将token写入cookie中, 然后传给客户端（一个cookie对应一个用户，
        // 这里将这个cookie的用户信息写入redis中）
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        // 保持与redis中的session一致
        cookie.setMaxAge(SeckillUserKeyPrefix.token.expireSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
