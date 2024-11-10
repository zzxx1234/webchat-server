# 微信公众号扫码登录对接

源码地址：

## 公众号扫码登录实现原理

**用户**：用户是扫码登录的发起方，点击登录，然后扫描登录二维码。

**浏览器**：浏览器为用户展示二维码，然后不断的轮询扫码状态。

**服务端**：网站服务端需要向微信服务端获取携带 Ticket 信息的公众号二维码，在微信服务端回调时绑定用户身份信息。

**微信服务端**：用户扫码后，会请求到微信服务端，微信服务端会携带扫描的二维码的 Ticket 和用户**身份标识**回调网站服务端。

微信服务端回调网站服务端时，携带的用户身份信息其实只是一串无意义字符串，但是微信可以保证的是同一个微信用户扫码时携带的身份信息字符是相同的，以此识别用户。也因此公众号扫码登录用作身份认证非常安全。

## 实现微信调用接口

官方文档：[微信回调接口对接文档](https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html)、[接收普通消息文档](https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_standard_messages.html)、[接收事件推送](https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_event_pushes.html)、[回复用户消息文档](https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Passive_user_reply_message.html)

需要实现两个接口第一个是GET请求，微信会调用该接口，该接口需要将微信传过来的产生进行SHA1加密，然后返回微信。第二个接口是POST请求，当用户扫码、关注、发送信息给公众号时微信会调用该接口（在该接口中我们需要实现用户扫码关注时将，二维码的ticket、和微信用户唯一标识openId存到redis中）

此外微信调用接口需要外网地址，我们可以使用natapp进行内网穿透后提问外网调用地址

软件地址：https://natapp.cn/
NATAPP1分钟快速新手图文教程：https://natapp.cn/article/natapp_newbie

```java
package com.zx.webchatserver.controller;

import com.zx.webchatserver.handler.WeChatMsgFactory;
import com.zx.webchatserver.handler.WeChatMsgHandler;
import com.zx.webchatserver.utils.MessageUtil;
import com.zx.webchatserver.utils.SHA1;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信回调接口
 */
@RestController
@RequestMapping("/weChatCallBack")
@Slf4j
public class WeChatCallBackController {

    private static final String token = "sihjkbbhfyybebnjbhjkhjdbsfvcbnjm%$^";

    @Resource
    private WeChatMsgFactory weChatMsgFactory;

    @GetMapping("/checkSignature")
    public String checkSignature(@RequestParam("signature") String signature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce,
                                 @RequestParam("echostr") String echostr) {
        log.info("get验签请求参数：signature:{}，timestamp:{}，nonce:{}，echostr:{}",
                signature, timestamp, nonce, echostr);
        String shaStr = SHA1.getSHA1(token, timestamp, nonce, "");
        log.info("shaStr:{}", shaStr);
        if (signature.equals(shaStr)) {
            return echostr;
        }
        return "unknown";
    }

    @PostMapping("/checkSignature")
    public String checkSignature(@RequestBody String requestBody,
                                 @RequestParam("signature") String signature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce,
                                 @RequestParam(value = "msg_signature", required = false) String msgSignature) {
        log.info("接收到微信消息：requestBody：{}", requestBody);
        Map<String, String> msgMap = MessageUtil.parseXml(requestBody);
        StringBuilder stringBuilder = new StringBuilder();
        String msgType = msgMap.get("MsgType");
        String event = msgMap.get("Event");
        stringBuilder.append(msgType);
        if (event != null) {
            stringBuilder.append(".");
            stringBuilder.append(event);
        }
        WeChatMsgHandler weChatMsgHandler = weChatMsgFactory.getHandlerByMsgType(stringBuilder.toString());
        String replyContent = weChatMsgHandler.dealMsg(msgMap);
        log.info("回复内容:{}", replyContent);
        return replyContent;
    }

}
```

实现上述两个接口后可以在微信公众平台注册一个测试号

链接：https://mp.weixin.qq.com/

## 获取 Access Token

官方文档：https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Get_access_token.html

> Access Token 是公众号的全局唯一接口调用凭据，公众号调用各接口时都需使用 Access Token 。每次获取有效期目前为2个小时，需定时刷新，重复获取将导致上次获取的 Access Token 失效。

```java
package com.zx.webchatserver.utils;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zx.webchatserver.utils.model.WeChatQRCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WeChatApiUtils {

    private final String appid = "wx3b33cf88f976905d";

    private final String secret = "fa2ed6f16e38fb830a33b725f981a50c";

    private final String WECHAT_ACCESS_TOKEN_KEY = "wechat:access:token";

    private final String WECHAT_API_PREFIX = "https://api.weixin.qq.com/cgi-bin/";

    private final String WECHAT_QR_CODE_URL_PREFIX = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";

    private final int WECHAT_QR_CODE_EXPIRE_SECONDS = 60;
    
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取 accessToken
     * @return
     */
    public String getAccessToken() {
        Boolean existed = redisTemplate.hasKey(WECHAT_ACCESS_TOKEN_KEY);
        if (existed) {
            long expire = redisTemplate.getExpire(WECHAT_ACCESS_TOKEN_KEY);
            if (expire > 60) {
                String accessToken = redisTemplate.opsForValue().get(WECHAT_ACCESS_TOKEN_KEY).toString();
                return accessToken;
            }
        }
        String accessToken = this.getAccessTokenFromWeChat();
        return accessToken;
    }

    /**
     * 从微信服务器获取 accessToken
     * @return
     */
    private String getAccessTokenFromWeChat() {
        String url = WECHAT_API_PREFIX + "token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                // 转换为 Map
                Map<String, String> map = gson.fromJson(response.body(), type);
                String accessToken = map.get("access_token");
                int expiresIn = Integer.parseInt(map.get("expires_in"));
                // 缓存到redis
                redisTemplate.opsForValue().set(WECHAT_ACCESS_TOKEN_KEY, accessToken, expiresIn, TimeUnit.SECONDS);
                return accessToken;
            }
        } catch (Exception e) {
            log.error("获取微信accessToken失败:", e);
        }
        return null;
    }
}
```

## 生成带 Ticket 二维码

官方文档：https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html

```java
package com.zx.webchatserver.utils;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zx.webchatserver.utils.model.WeChatQRCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WeChatApiUtils {

    private final String appid = "wx3b33cf88f976905d";

    private final String secret = "fa2ed6f16e38fb830a33b725f981a50c";

    private final String WECHAT_ACCESS_TOKEN_KEY = "wechat:access:token";

    private final String WECHAT_API_PREFIX = "https://api.weixin.qq.com/cgi-bin/";

    private final String WECHAT_QR_CODE_URL_PREFIX = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";

    private final int WECHAT_QR_CODE_EXPIRE_SECONDS = 60;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取 accessToken
     * @return
     */
    public String getAccessToken() {
        Boolean existed = redisTemplate.hasKey(WECHAT_ACCESS_TOKEN_KEY);
        if (existed) {
            long expire = redisTemplate.getExpire(WECHAT_ACCESS_TOKEN_KEY);
            if (expire > 60) {
                String accessToken = redisTemplate.opsForValue().get(WECHAT_ACCESS_TOKEN_KEY).toString();
                return accessToken;
            }
        }
        String accessToken = this.getAccessTokenFromWeChat();
        return accessToken;
    }

    /**
     * 从微信服务器获取 accessToken
     * @return
     */
    private String getAccessTokenFromWeChat() {
        String url = WECHAT_API_PREFIX + "token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                // 转换为 Map
                Map<String, String> map = gson.fromJson(response.body(), type);
                String accessToken = map.get("access_token");
                int expiresIn = Integer.parseInt(map.get("expires_in"));
                // 缓存到redis
                redisTemplate.opsForValue().set(WECHAT_ACCESS_TOKEN_KEY, accessToken, expiresIn, TimeUnit.SECONDS);
                return accessToken;
            }
        } catch (Exception e) {
            log.error("获取微信accessToken失败:", e);
        }
        return null;
    }

    /**
     * 获取公众号二维码链接
     * @return
     */
    public WeChatQRCode getQRCode() {
        String sceneId = IdUtil.randomUUID();
        String accessToken = this.getAccessToken();
        String url = WECHAT_API_PREFIX + "qrcode/create?access_token=" + accessToken;
        String params = "{\"expire_seconds\": %d, \"action_name\": \"QR_STR_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": \"%s\"}}}";
        params = String.format(params, WECHAT_QR_CODE_EXPIRE_SECONDS, sceneId);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(params))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> map = gson.fromJson(response.body(), type);
            String ticket = map.get("ticket");
            String weChatUrl = map.get("url");
            int expireSeconds = Integer.parseInt(map.get("expire_seconds"));
            String QRCodeUrl = WECHAT_QR_CODE_URL_PREFIX + ticket;
            WeChatQRCode weChatQRCode = new WeChatQRCode();
            weChatQRCode.setTicket(ticket);
            weChatQRCode.setExpireSeconds(expireSeconds);
            weChatQRCode.setUrl(weChatUrl);
            weChatQRCode.setQRCodeUrl(QRCodeUrl);
            log.info("微信接口返回的responseBody:{}", map);
            log.info("解析成weChatQRCode:{}", weChatQRCode);
            return weChatQRCode;
        } catch (Exception e) {
            log.error("获取二维码失败:", e);
        }
        return null;
    }
}
```

## 实现前端轮询调用用户登录接口

```java
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
	
    // 获取二维码接口
    @GetMapping("/getWeChatQRCode")
    public Result getWeChatQRCode() {
        WeChatQRCode QRCode = weChatApiUtils.getQRCode();
        Result ok = Result.ok(QRCode);
        return ok;
    }

    // 前端轮询是否登录成功接口
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
```

