package com.zx.webchatserver.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TextMsgHandler implements WeChatMsgHandler {

    @Override
    public WeChatMsgTypeEnum getMsgType() {
        return WeChatMsgTypeEnum.TEXT;
    }

    @Override
    public String dealMsg(Map<String, String> msgMap) {
        log.info("触发用户发送文本事件");
        String fromUserName = msgMap.get("FromUserName");
        String toUserName = msgMap.get("ToUserName");
        String content = msgMap.get("Content");
        int verifyCode = new Random().nextInt(899999) + 100000;
        if ("验证码".equals(content)) {
            String replyMsg = "<xml>\n" +
                    "  <ToUserName><![CDATA[" + fromUserName + "]]></ToUserName>\n" +
                    "  <FromUserName><![CDATA[" + toUserName + "]]></FromUserName>\n" +
                    "  <CreateTime>" + new Date().getTime() / 1000 + "</CreateTime>\n" +
                    "  <MsgType><![CDATA[text]]></MsgType>\n" +
                    "  <Content><![CDATA["+ verifyCode +"]]></Content>\n" +
                    "</xml>";
            return replyMsg;
        }
        return "unknown";
    }
}
