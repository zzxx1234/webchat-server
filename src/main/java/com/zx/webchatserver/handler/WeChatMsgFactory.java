package com.zx.webchatserver.handler;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WeChatMsgFactory implements InitializingBean {

    @Resource
    private List<WeChatMsgHandler> weChatMsgHandlerList;

    private Map<WeChatMsgTypeEnum, WeChatMsgHandler> msgHandlerMap = new HashMap<>();

    public WeChatMsgHandler getHandlerByMsgType(String msgType) {
        WeChatMsgTypeEnum weChatMsgTypeEnum = WeChatMsgTypeEnum.getEnumByMsgType(msgType);
        return msgHandlerMap.get(weChatMsgTypeEnum);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (WeChatMsgHandler weChatMsgHandler : weChatMsgHandlerList) {
            msgHandlerMap.put(weChatMsgHandler.getMsgType(), weChatMsgHandler);
        }
    }
}
