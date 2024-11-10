package com.zx.webchatserver.controller;

import com.zx.webchatserver.common.Result;
import com.zx.webchatserver.utils.WeChatApiUtils;
import com.zx.webchatserver.utils.model.WeChatQRCode;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/weChatUser")
public class WeChatUserController {

    @Resource
    private WeChatApiUtils weChatApiUtils;

    @Resource
    private RedisTemplate redisTemplate;

    public static final String WECHAT_TICKET_PREFIX = "wechat:ticket:";

    @GetMapping("/getWeChatQRCode")
    public Result getWeChatQRCode() {
        WeChatQRCode QRCode = weChatApiUtils.getQRCode();
        Result ok = Result.ok(QRCode);
        return ok;
    }

    @PostMapping("/doLogin")
    public Result doLogin(String ticket) {
        String key = WECHAT_TICKET_PREFIX + ticket;
        Boolean existed = redisTemplate.hasKey(key);
        if (existed) {
            String weChatOpenId = redisTemplate.opsForValue().get(key).toString(); // 获取微信openId
            redisTemplate.delete(key);
            // 根据openId查询用户信息，并完成登录逻辑返回token
            Map<String, String> map = new HashMap<>();
            map.put("token", "token");
            return Result.ok("登录成功", map);
        }
        return Result.fail("登录失败");
    }
}
