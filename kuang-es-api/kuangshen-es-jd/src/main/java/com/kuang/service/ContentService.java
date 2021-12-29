package com.kuang.service;

import com.alibaba.fastjson.JSON;
import com.kuang.pojo.Content;
import com.kuang.utils.HtmlParseUtil;
import org.apache.lucene.search.highlight.Highlighter;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 业务编写
@Service
public class ContentService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    // 1.解析数据放入到es索引中
    public Boolean parseContent(String keywords) throws Exception {
        List<Content> contents = new HtmlParseUtil().parseJD(keywords);
        // 把插入的数据放入es中
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");
        for (int i = 0; i<contents.size(); i++ )
        {
            bulkRequest.add(
                    new IndexRequest("jd_goods")
                            .source(JSON.toJSONString(contents.get(i)), XContentType.JSON)
            );
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !bulk.hasFailures();
    }
    //2.获取这些数据实现搜索功能
    public List<Map<String,Object>> searchPage(String keyword,int pageNo,int pageSize) throws IOException {
        if (pageNo < 1) {
            pageNo = 1;
        }
        // 条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);

        // 精准匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        // 客户端执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 解析结果
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            list.add(documentFields.getSourceAsMap());
        }
        return list;
    }

    // 实现搜索高亮功能
    public List<Map<String,Object>> searchHighLightPage(String keyword,int pageNo,int pageSize) throws IOException {
        if (pageNo < 1) {
            pageNo = 1;
        }
        // 条件搜索
        SearchRequest searchRequest = new SearchRequest("jd_goods");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 分页
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.requireFieldMatch(false);  //多个高亮显示只显示一个vue
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);    //构造高亮搜索

        // 精准匹配
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("title", keyword);
        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        // 客户端执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 解析结果
        ArrayList<Map<String,Object>> list = new ArrayList<>();
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();  //原来的结果
            //解析高亮的字段
            if (title!=null){
                Text[] fragments = title.fragments();
                String n_title = "";
                for (Text text : fragments){
                    n_title += text;
                }
                sourceAsMap.put("title",n_title);   //高亮字段替换掉原来的内容即可
            }
            list.add(sourceAsMap);
        }
        return list;
    }
}
