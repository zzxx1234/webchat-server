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

    // 前缀

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