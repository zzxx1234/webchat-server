package com.zx.webchatserver.handler;

public enum WeChatMsgTypeEnum {

    TEXT("text", "用户发送文本消息"),

    SUBSCRIBE("event.subscribe", "用户关注事件"),

    SCAN("event.SCAN", "用户扫码");

    private String msgType;

    private String desc;

    WeChatMsgTypeEnum(String msgType, String desc){
        this.msgType = msgType;
        this.desc = desc;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getDesc() {
        return desc;
    }

    public static WeChatMsgTypeEnum getEnumByMsgType(String msgType){
        for (WeChatMsgTypeEnum msgTypeEnum : WeChatMsgTypeEnum.values()) {
            if(msgTypeEnum.getMsgType().equals(msgType)){
                return msgTypeEnum;
            }
        }
        return null;
    }
}
