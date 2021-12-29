package com.kuang.kuangshenesapi;

import com.alibaba.fastjson.JSON;
import com.kuang.pojo.User;
import com.kuang.utils.ESconst;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class KuangshenEsApiApplicationTests {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    // 测试索引的创建 Rest
    @Test
    void testCreateIndex() throws IOException {
        // 1,创建索引请求
        CreateIndexRequest request = new CreateIndexRequest("kuang_index");
        // 2、客户端执行创建请求 IndicesClient
        CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
        System.out.println(createIndexResponse);
    }

    @Test
    void createJdGoods() throws IOException {

        // 1.创建索引请求
        CreateIndexRequest jdGoods = new CreateIndexRequest("jd_goods");
        // 2.客户端执行创建请求 IndicesClient
        CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(jdGoods, RequestOptions.DEFAULT);
        System.out.println(createIndexResponse);

    }

    // 测试获取索引
    @Test
    void testExitIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("kuang_index");
        boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    // 测试删除索引
    @Test
    void testDeleteIndex() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("kuang_index");
        AcknowledgedResponse delete = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println(delete);
    }

    // 测试添加文档
    @Test
    void testAddDocument() throws IOException {
        // 创建对象
        User user = new User("狂神说",3);
        // 创建请求
        IndexRequest request = new IndexRequest("kuang_index");
        // 规则 put /kuang_index/_doc/1
        request.id("1");
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");

        //将我们的数据放入请求 json
        request.source(JSON.toJSONString(user), XContentType.JSON);
        // 客户端发送请求,获取响应结果
        IndexResponse indexResponse = restHighLevelClient.index(request, RequestOptions.DEFAULT);
        System.out.println(indexResponse.toString());

    }
    // 获取文档 ，判断是否存在 get /index/doc/1
    @Test
    void testExists() throws IOException {

        GetRequest getRequest = new GetRequest("kuang_index", "1");
        // 不获取返回的 _source 的上下文了
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        boolean exists = restHighLevelClient.exists(getRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    // 获取文档信息
    @Test
    void testGetDocument() throws IOException {
        GetRequest getRequest = new GetRequest("kuang_index", "1");
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        System.out.println(getResponse.getSourceAsString());    //打印文档的内容
        System.out.println(getResponse);    //返回额全部内容和命令是一样的
    }

    // 更新文档的信息
    @Test
    void testUpdateDocument() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest("kuang_index", "1");
        updateRequest.timeout("1s");    //超时时间
        User user = new User("狂神说Java", 18);
        updateRequest.doc(JSON.toJSONString(user),XContentType.JSON);   //json类型
        UpdateResponse update = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(update);
    }

    // 删除文档记录
    @Test
    void testDeleteRequest() throws IOException {
        DeleteRequest request = new DeleteRequest("kuang_index", "1");
        request.timeout("1s");
        DeleteResponse delete = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        System.out.println(delete.status());
    }

    // 大批量的插入
    @Test
    void testBulkRequest() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");
        ArrayList<User> list = new ArrayList<>();
        list.add(new User("zjz1",3));
        list.add(new User("zjz2",4));
        list.add(new User("zjz3",5));
        list.add(new User("zjz4",6));
        list.add(new User("zjz5",7));
        list.add(new User("zjz6",8));
        for (int i =0; i < list.size(); i++ ) {
            bulkRequest.add(
                    new IndexRequest("kuang_index")
                            .id(""+(i+1))
                            .source(JSON.toJSONString(list.get(i)),XContentType.JSON));
        }
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(bulkResponse.hasFailures()); //是否失败，返回false是成果
    }

    // 查询
    // SearchRequest 搜索请求
    // SearchSourceBuilder 条件构造
    // HighlightBuilder 构建高亮
    // TermQueryBuilder 精确查询
    // xxxQueryBuilder 对应的功能
    @Test
    void testSearch() throws IOException {
        SearchRequest searchRequest = new SearchRequest(ESconst.ES_INDEX);
        //构建搜索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //查询条件 ，我们可以使用QueryBuilders工具来实现
        //QueryBuilders.termQuery 精确
        // QueryBuilders.matchAllQuery() 匹配所有
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "zjz1");

        sourceBuilder.query(termQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//        sourceBuilder.from(1);
//        sourceBuilder.size(2);
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(JSON.toJSONString(searchResponse.getHits()));    //所有结果都封装到HITS
        System.out.println("==================");
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            System.out.println(documentFields.getSourceAsMap());
        }

    }

}
