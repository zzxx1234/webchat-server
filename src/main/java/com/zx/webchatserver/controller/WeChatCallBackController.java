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
