package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.baidu.GreenImgCensor;
import com.heima.common.baidu.GreenTextCensor;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSentiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     * @param id 自媒体文章id
     */
    @Override
    @Async//表明当前方法是异步调用
    public void autoScanWmNews(Integer id) {
        //1.查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if(wmNews == null){
            throw new RuntimeException("WmNewsAutoScanServiceImpl-自媒体文章不存在");
        }

        if(!wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())){
            throw new RuntimeException("WmNewsAutoScanServiceImpl-自媒体文章未提交");
        }

        //从内容中提取文本和图片
        Map<String,Object> textAndImages =handleTextAndImages(wmNews);

        //自管理的敏感词过滤
        boolean isSentive = handleSentiveScan(textAndImages.get("content").toString(),wmNews);
        if(!isSentive)return;

        //2.审核文本内容 百度接口
        boolean isTextScan = handleTextScan(textAndImages.get("content").toString(),wmNews);
        if(!isTextScan)return;

        //3。审核图片内容 百度接口
        boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"),wmNews);
        if(!isImageScan)return;

        //4.审核通过 保存app端的相关文章数据
        ResponseResult responseResult = saveAppArticle(wmNews);
        if(!responseResult.getCode().equals(200)){
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败！");
        }

        //回填article_id
        wmNews.setArticleId((Long) responseResult.getData());
        updateWmNews(wmNews,(short)9,"审核成功" );
    }

    @Autowired
    private WmSentiveMapper wmSentiveMapper;

    /**
     *自管理的敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSentiveScan(String content, WmNews wmNews) {

        boolean flag = true;

        //获取所有的敏感词
        List<WmSensitive> wmSensitives = wmSentiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

        //初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        //查看是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size()>0){
            updateWmNews(wmNews,(short)2,"文章中包含敏感词"+map);
            flag = false;
        }

        return flag;
    }

    @Resource
    private IArticleClient iArticleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;
    /**
     * 保存app端相关的文章数据
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(wmNews,dto);
        //文章布局
        dto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null){
            dto.setChannelName(wmChannel.getName());
        }

        //作者id,name
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser != null){
            dto.setAuthorName(wmUser.getName());
        }

        //文章id
        if(wmNews.getArticleId()!=null){
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = iArticleClient.saveArticle(dto);
        return responseResult;
    }

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImgCensor greenImgCensor;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        boolean flag = true;
        if(images.isEmpty() ){
            return flag;
        }
        //下载图片 minio中
        //图片去重
        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();

        try {
            for (String image : images) {
                byte[] bytes= fileStorageService.downLoadFile(image);

                //byte[] 转换为 bufferedImage
                ByteArrayInputStream in =new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);

                //图片文本识别
                String result = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                boolean isSentive = handleSentiveScan(result, wmNews);
                if(!isSentive){
                    return isSentive;
                }

                imageList.add(bytes);
            }
        }catch (Exception e){
            e.printStackTrace();
        }



        //审核图片
        Map map =JSONObject.parseObject(greenImgCensor.ImgCensor(imageList), Map.class);
        if(map!=null){
            //审核不通过
            if(map.get("conclusion").equals("不合规") || map.get("conclusion").equals("审核失败")) {
                flag = false;
                updateWmNews(wmNews,(short)2,"当前图片中存在违规内容！".toString());
            }
            //不确定信息，需要人工审核
            if(map.get("conclusion").equals("疑似")) {
                flag = false;
                updateWmNews(wmNews,(short)3,"疑似，需要人工审核".toString());
            }
        }
        return flag;
    }

    @Autowired
    private GreenTextCensor greenTextCensor;
    /**
     * 审核纯文本内容
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;
        if((wmNews.getTitle()+"-"+content).length() == 0){
            return flag;
        }

        Map map = JSONObject.parseObject(greenTextCensor.TextCensor(wmNews.getTitle()+"-"+content), Map.class);
        if(map!=null){
            //审核不通过
            if(map.get("conclusion").equals("不合规")) {
                flag = false;
                updateWmNews(wmNews,(short)2,"当前文章中存在违规内容！".toString());
            }
            //不确定信息，需要人工审核
            if(map.get("conclusion").equals("疑似")) {
                flag = false;
                updateWmNews(wmNews,(short)3,"疑似，需要人工审核".toString());
            }
        }

        return flag;
    }

    /**
     * 修改文章内容
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews,short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 1.从自媒体内容中提取文本和图片
     * 2。提取封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {
        //存文本内容
        StringBuilder stringBuilder = new StringBuilder();

        //存图片内容
        List<String> images = new ArrayList<>();

        //1.从自媒体内容中提取文本和图片
        if(StringUtils.isNoneBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if(map.get("type").equals("text")){
                    stringBuilder.append(map.get("value"));
                }else if(map.get("type").equals("image")){
                    images.add(map.get("value").toString());
                }
            }
        }
        if(StringUtils.isNoneBlank(wmNews.getImages())){
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("content",stringBuilder.toString());
        resultMap.put("images",images);
        return resultMap;
    }
}
