package com.chatgptbot;

import com.alibaba.fastjson2.JSON;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

public class Session {
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> cookies = new HashMap<>();
    private Map<String, String> proxies = new HashMap<>();

    public Session() {
    }

    public HttpResponse post2(String url, Map<String, Object> data) {
        getCookiesString();


        return HttpUtil.createPost(url)
                .addHeaders(headers)
                .cookie(getCookiesString())
                .body(JSON.toJSONString(data), "application/json")
                .execute();

    }

    public HttpResponse get2(String url) {
        getCookiesString();

        return HttpUtil.createGet(url)
                .addHeaders(headers)
                .cookie(getCookiesString())
                .execute();
    }


    public String getCookiesString() {
        String result = "";
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result = result + key + "=" + value + "; ";
        }
        headers.put("cookie", result);
        return result;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Map<String, String> getProxies() {
        return proxies;
    }

    public void setProxies(Map<String, String> proxies) {
        this.proxies = proxies;
    }
}