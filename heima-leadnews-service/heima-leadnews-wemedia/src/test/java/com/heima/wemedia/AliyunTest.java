package com.heima.wemedia;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.baidu.GreenImgCensor;
import com.heima.common.baidu.GreenTextCensor;
import com.heima.file.service.FileStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {

    @Autowired
    private GreenTextScan greenTextScan;

    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImgCensor greenImgCensor;

    @Autowired
    private GreenTextCensor greenTextCensor;

    @Test
    public void testScanText() throws Exception {
        String result  = greenTextCensor.TextCensor("我是一个好人,冰毒");
        Map map = JSONObject.parseObject(result, Map.class);
        System.out.println(map);
    }

    @Test
    public void testScanImage() throws Exception {
        byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2023/08/07/2d6aeaf797e241ed90d26170cee2c5eb.jpg");
        String result = greenImgCensor.ImgCensor(Arrays.asList(bytes));
        Map map = JSONObject.parseObject(result, Map.class);
        System.out.println(map);
    }
}
