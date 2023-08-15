package com.heima.common.baidu;

import com.alibaba.fastjson.JSONObject;
import com.heima.common.baidu.utils.Base64Util;
import com.heima.common.baidu.utils.HttpUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "baidu")
public class GreenImgCensor {


    public  String ImgCensor(List<byte[]> imageList) {

        // 请求url
        String url = "https://aip.baidubce.com/rest/2.0/solution/v1/img_censor/v2/user_defined";
        try {
            // 本地文件路径
//            String filePath = "C:\\Users\\16090\\Desktop\\picture\\1.jpg";
//            byte[] imgData = FileUtil.readFileByBytes(filePath);
            String result = null;
            for (byte[] imgData : imageList) {
                String imgStr = Base64Util.encode(imgData);
                String imgParam = URLEncoder.encode(imgStr, "UTF-8");

                String param = "image=" + imgParam;

                // 注意这里仅为了简化编码每一次请求都去获取access_token，线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
                String accessToken = AuthService.getAuth();

                result = HttpUtil.post(url, accessToken, param);
                Map map = JSONObject.parseObject(result, Map.class);
                if(map.get("conclusion").equals("疑似")|| map.get("conclusion").equals("不合规") ||map.get("conclusion").equals("审核失败")){
                    return result;
                }
            }
            System.out.println(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
