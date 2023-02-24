package com.chatgptbot;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.MessageChain;


/**
 * @author healthyin
 * @version ChatBot 2023/2/18
 */
public class ChatBot {
    String conversationId = null;
    String authorization = "";
    String parentId;
    private Map<String, String> headers = new HashMap<>();

    private String host = "https://chatgpt.duti.tech/";


    public ChatBot(Config config) {
        this(config, null);
    }

    public ChatBot(Config config, String conversationId) {
        this.authorization = config.getAccess_token();
        this.conversationId = conversationId;
        this.parentId = UUID.randomUUID().toString();
        refreshHeaders();
    }

    /**
     * 初始化
     */
    public ChatBot() throws Exception {
        this.parentId = UUID.randomUUID().toString();
        refreshHeaders();
    }

    // Resets the conversation ID and parent ID
    public void resetChat() {
        this.conversationId = null;
        this.parentId = UUID.randomUUID().toString();
    }


    // Refreshes the headers -- Internal use only
    public void refreshHeaders() {

        if (StrUtil.isEmpty(authorization)) {
            authorization = "";
        }
        this.headers = new HashMap<String, String>() {{
            put("Accept", "text/event-stream");
            put("Authorization", "Bearer " + authorization);
            put("Content-Type", "application/json");
            put("X-Openai-Assistant-App-Id", "");
            put("Connection", "close");
            put("Accept-Language", "en-US,en;q=0.9");
            put("Referer", "https://chat.openai.com/chat");
        }};

    }

    void getAndSendChatStream(Map<String, Object> data, Group group, Contact contact, MessageChain msgChain){
        String requestUrl = host + "api/conversation";
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = null;
         request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + authorization)
                .header("Content-Type", "application/json")
                .header("X-Openai-Assistant-App-Id", "")
                //.header("Connection", "close")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://chat.openai.com/chat")
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(data)))
                .build();
        httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(java.net.http.HttpResponse::body)
                .thenAccept(body -> {
                    String message = "";
                    Map<String, Object> chatData = new HashMap<>();
                    String[] bodySplit = body.split("\n");
                    for (int i = bodySplit.length - 1;i > 0;i--) {
                        String s = bodySplit[i];
                        if ((s == null) || "".equals(s)) {
                            continue;
                        }
                        if (s.contains("data: [DONE]") || !s.contains("data: {")) {
                            continue;
                        }

                        String part = s.substring(6);
                        JSONObject lineData = JSON.parseObject(part);

                        try {

                            JSONArray jsonArray = lineData.getJSONObject("message")
                                    .getJSONObject("content")
                                    .getJSONArray("parts");

                            if (jsonArray.size() == 0) {
                                continue;
                            }
                            message = jsonArray.getString(0);

                            conversationId = lineData.getString("conversation_id");
                            parentId = (lineData.getJSONObject("message")).getString("id");
                            QQBotService.sendMessage(group, contact, msgChain, message);
                            break;
                        } catch (Exception e) {
                            System.out.println("getChatStream Exception: " + part);
                        }
                    }
                });
    }


    Map<String, Object> getChatStream(Map<String, Object> data) {
        String url = host + "api/conversation";

        String body = HttpUtil.createPost(url)
                .headerMap(headers, true)
                .body(JSON.toJSONString(data), "application/json")
                .execute()
                .body();

        String message = "";
        Map<String, Object> chatData = new HashMap<>();
        for (String s : body.split("\n")) {
            if ((s == null) || "".equals(s)) {
                continue;
            }
            if (s.contains("data: [DONE]") || !s.contains("data: {")) {
                continue;
            }

            String part = s.substring(6);
            JSONObject lineData = JSON.parseObject(part);

            try {

                JSONArray jsonArray = lineData.getJSONObject("message")
                        .getJSONObject("content")
                        .getJSONArray("parts");

                if (jsonArray.size() == 0) {
                    continue;
                }
                message = jsonArray.getString(0);

                conversationId = lineData.getString("conversation_id");
                parentId = (lineData.getJSONObject("message")).getString("id");

                chatData.put("message", message);
                chatData.put("conversation_id", conversationId);
                chatData.put("parent_id", parentId);
            } catch (Exception e) {
                System.out.println("getChatStream Exception: " + part);
                //  e.printStackTrace();
                continue;
            }

        }
        return chatData;

    }

    // Gets the chat response as text -- Internal use only
    public Map<String, Object> getChatText(Map<String, Object> data) {

        // Create request session
        Session session = new Session();

        // set headers
        session.setHeaders(this.headers);

        HttpResponse response = session.post2(host + "api/conversation",
                data);
        String body = response.body();

        String errorDesc = "";


        String message = "";
        Map<String, Object> chatData = new HashMap<>();
        for (String s : body.split("\n")) {
            if ((s == null) || "".equals(s)) {
                continue;
            }
            if (s.contains("data: [DONE]")) {
                continue;
            }

            String part = s.substring(5);

            try {
                JSONObject lineData = JSON.parseObject(part);

                JSONArray jsonArray = lineData.getJSONObject("message")
                        .getJSONObject("content")
                        .getJSONArray("parts");

                if (jsonArray.size() == 0) {
                    continue;
                }
                message = jsonArray.getString(0);

                conversationId = lineData.getString("conversation_id");
                parentId = (lineData.getJSONObject("message")).getString("id");

                chatData.put("message", message);
                chatData.put("conversation_id", conversationId);
                chatData.put("parent_id", parentId);
            } catch (Exception e) {
                System.out.println("getChatStream Exception: " + part);
                //  e.printStackTrace();
                continue;
            }

        }
        return chatData;
    }

    public void getChatResponseAndSendAsync(String prompt, Group group, Contact contact, MessageChain msgChain) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", "next");
        data.put("conversation_id", this.conversationId);
        data.put("parent_message_id", this.parentId);
        data.put("model", "text-davinci-002-render-sha");

        Map<String, Object> message = new HashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "user");
        Map<String, Object> content = new HashMap<>();
        content.put("content_type", "text");
        content.put("parts", Collections.singletonList(prompt));
        message.put("content", content);
        data.put("messages", Collections.singletonList(message));

        this.getAndSendChatStream(data, group, contact, msgChain);
    }

    public Map<String, Object> getChatResponse(String prompt, String output){
        Map<String, Object> data = new HashMap<>();
        data.put("action", "next");
        data.put("conversation_id", this.conversationId);
        data.put("parent_message_id", this.parentId);
        data.put("model", "text-davinci-002-render-sha");

        Map<String, Object> message = new HashMap<>();
        message.put("id", UUID.randomUUID().toString());
        message.put("role", "user");
        Map<String, Object> content = new HashMap<>();
        content.put("content_type", "text");
        content.put("parts", Collections.singletonList(prompt));
        message.put("content", content);
        data.put("messages", Collections.singletonList(message));

        if (output.equals("text")) {
            return this.getChatText(data);
        } else if (output.equals("stream")) {
            return this.getChatStream(data);
        } else {
            throw new RuntimeException("Output must be either 'text' or 'stream'");
        }
    }

    public Map<String, Object> getChatResponse(String prompt) {
        return this.getChatResponse(prompt, "text");
    }
}
