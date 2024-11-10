package com.zx.webchatserver.handler;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ScanMsgHandler implements WeChatMsgHandler{

    public static final String WECHAT_TICKET_PREFIX = "wechat:ticket:";

    private final int WECHAT_QR_CODE_EXPIRE_SECONDS = 60;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public WeChatMsgTypeEnum getMsgType() {
        return WeChatMsgTypeEnum.SCAN;
    }

    @Override
    public String dealMsg(Map<String, String> msgMap) {
        log.info("触发用户扫码事件");
        String fromUserName = msgMap.get("FromUserName");
        String ticket = msgMap.get("Ticket");
        if (ticket != null && ticket != "") { // 如果Ticket，说明是扫码登入，把用户信息存到redis中
            String key = WECHAT_TICKET_PREFIX + ticket;
            redisTemplate.opsForValue().set(key, fromUserName, WECHAT_QR_CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        return "unknown";
    }


}
