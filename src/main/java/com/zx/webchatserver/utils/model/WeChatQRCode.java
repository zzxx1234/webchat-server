package com.zx.webchatserver.utils.model;

import lombok.Data;

@Data
public class WeChatQRCode {

    private String ticket; // 二维码的ticket，凭借此ticket可以在有效时间内换取二维码。

    private int expireSeconds; // 二维码的有效时间，以秒为单位。

    private String url; // 二维码图片解析后的地址

    private String QRCodeUrl; // 二维码图片的url

}
