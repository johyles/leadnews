package com.heima.search.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ArticleSearchSearchImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * es文章分页检索
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {
        //1.检查参数
        if(dto == null|| StringUtils.isBlank(dto.getSearchWords())){
            return  ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //异步调用 保存搜索记录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user !=null && dto.getFromIndex() == 0) {
            apUserSearchService.insert(dto.getSearchWords(), AppThreadLocalUtil.getUser().getId());
        }

        //2.设置查询条件
        SearchRequest searchRequest = new SearchRequest("app_info_article");//2
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();//4

        //因为查询条件有两个，不能直接放进searchSourceBuilder
        //布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//6

        //关键字的分词之后查询
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").field("content").defaultOperator(Operator.OR);//5
        boolQueryBuilder.must(queryStringQueryBuilder); //7

        //查询小于mindate的数据
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime()); //8
        boolQueryBuilder.filter(rangeQueryBuilder); //9

        //分页查询
        searchSourceBuilder.from(0); //10
        searchSourceBuilder.size(dto.getPageSize()); //11

        //根据发布时间倒序查询
        searchSourceBuilder.sort("publishTime", SortOrder.DESC); //12

        //设置高亮 title
        HighlightBuilder highlightBuilder = new HighlightBuilder(); //13
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>"); //设置前缀
        highlightBuilder.postTags("</font>");//后缀
        searchSourceBuilder.highlighter(highlightBuilder);

        searchSourceBuilder.query(boolQueryBuilder); //8
        searchRequest.source(searchSourceBuilder);  //3
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);//1

        //3.结构封装返回

        List<Map> list = new ArrayList<>();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class);

            //处理高亮
            if(hit.getHighlightFields() != null && hit.getHighlightFields().size() >0){
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String title = StringUtils.join(titles);
                //高亮标题
                map.put("h_title",title);
            }{
                //原始标题
                map.put("h-title",map.get("title"));
            }
            list.add(map);

        }
        return ResponseResult.okResult(list);
    }
}
