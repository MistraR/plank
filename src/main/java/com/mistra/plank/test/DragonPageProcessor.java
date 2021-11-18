package com.mistra.plank.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.IOException;
import java.net.URI;

/**
 * 描述
 *
 * @author mistra@future.com
 * @date 2021/11/18
 */
public class DragonPageProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(100);

    @SneakyThrows
    @Override
    public void process(Page page) {

        String data = page.getJson().toString();
        data = data.substring(data.indexOf("(") + 1, data.indexOf(")"));
        JSONObject jsonObject = JSON.parseObject(data);
        System.out.println(jsonObject);
        page.putField("name", page.getHtml().css("div.lhbstock").toString());
        page.putField("name", page.getHtml().xpath("//table[@class='lhbtable']/wname/a/title()").toString());
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) throws IOException {
//        Spider.create(new DragonPageProcessor()).addUrl("http://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112306080694619123275_1635760992000&reportName=RPT_DAILYBILLBOARD_PROFILE&columns=SECURITY_NAME_ABBR%2CCHANGE_RATE%2CTRADE_MARKET_CODE%2CTRADE_DATE%2CSECURITY_CODE&pageNumber=1&pageSize=500&sortTypes=1&sortColumns=SECURITY_CODE&source=WEB&client=WEB&_=1635760992000").thread(1).run();
//        Spider.create(new GithubRepoPageProcessor()).addUrl("http://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112306080694619123275_1637229792389&reportName=RPT_DAILYBILLBOARD_PROFILE&columns=SECURITY_NAME_ABBR%2CCHANGE_RATE%2CTRADE_MARKET_CODE%2CTRADE_DATE%2CSECURITY_CODE&pageNumber=1&pageSize=500&sortTypes=1&sortColumns=SECURITY_CODE&source=WEB&client=WEB&_=1637229792390").thread(1).run();
//        Spider.create(new GithubRepoPageProcessor()).addUrl("http://stock.xueqiu.com/v5/stock/chart/kline.json?symbol=SZ002466&begin=1637318960367&period=day&type=before&count=-284&indicator=kline,pe,pb,ps,pcf,market_capital,agt,ggt,balance").thread(1).run();

        CloseableHttpClient aDefault = HttpClients.createDefault();
        HttpGet request = new HttpGet();
        request.setURI(URI.create("http://stock.xueqiu.com/v5/stock/chart/kline.json?symbol=SZ002466&begin=1637318960367&period=day&type=before&count=-284&indicator=kline,pe,pb,ps,pcf,market_capital,agt,ggt,balance"));
        CloseableHttpResponse response = aDefault.execute(request);
        response.getEntity();
        System.out.println(response.getEntity().getContent().toString());
    }
}