package com.kuang.utils;

import com.kuang.pojo.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class HtmlParseUtil {
//    public static void main(String[] args) throws Exception {
//        new HtmlParseUtil().parseJD("java").forEach(System.out::println);
//    }
    public List<Content> parseJD(String keywords) throws Exception{
        // 获取请求 https://search.jd.com/Search?keyword=java
        // 前提，需要联网，ajax 不能获取到！
        String url = "https://search.jd.com/Search?keyword="+keywords;
        //解析网页 (就是js页面对象)
        Document document = Jsoup.parse(new URL(url), 30000);
        Element element = document.getElementById("J_goodsList");
        //https://search.jd.com/Search?keyword=java
        // 获取所有的li元素
        Elements li = element.getElementsByTag("li");

        ArrayList<Content> goodList = new ArrayList<>();

        for (Element el : li) {
            String img = el.getElementsByTag("img").eq(0).attr("source-data-lazy-img");
            String price = el.getElementsByClass("p-price").eq(0).text();
            String title = el.getElementsByClass("p-name").eq(0).text();

            Content content = new Content();
            content.setTitle(title);
            content.setPrice(price);
            content.setImg(img);
            goodList.add(content);
        }
        return goodList;
    }

}
