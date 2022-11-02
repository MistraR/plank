package com.mistra.plank.service.impl;

import com.mistra.plank.model.entity.Message;
import com.mistra.plank.model.entity.Robot;
import com.mistra.plank.service.MessageService;
import com.mistra.plank.service.RobotService;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.common.util.StockConsts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Autowired
    private RobotService robotService;

    @Autowired
    private CloseableHttpClient httpClient;

    @Override
    public void send(String body) {
        Robot robot = robotService.getSystem();
        if (robot == null) {
            MessageServiceImpl.logger.error("system robot not config");
            return;
        }
        String target = robot.getWebhook();
        sendDingding(null, body, target, DingDingMessageType.Text);
    }

    @Override
    public void sendMd(String title, String body) {
        Robot robot = robotService.getSystem();
        if (robot == null) {
            MessageServiceImpl.logger.error("system robot not config");
            return;
        }
        String target = robot.getWebhook();
        sendDingding(title, body, target, DingDingMessageType.Markdown);
    }

    private void sendDingding(String title, String body, String target, DingDingMessageType type) {
        try {
            Message message = new Message(StockConsts.MessageType.Message.value(), target, body, new Date());
            Map<String, Object> params = type == DingDingMessageType.Text
                    ? buildTextMessageParams(message.getBody())
                    : buildMarkdownMessageParams(title, message.getBody());
            MessageServiceImpl.logger.info("send message content: {}", params);
            String result = HttpUtil.sendPostJson(httpClient, message.getTarget(), params);
            MessageServiceImpl.logger.info("send message result: {}", result);
        } catch (Exception e) {
            MessageServiceImpl.logger.error("send message error", e);
        }
    }

    private Map<String, Object> buildTextMessageParams(String content) {
        HashMap<String, Object> text = new HashMap<>();
        text.put("content", content);

        HashMap<String, Object> params = new HashMap<>();
        params.put("msgtype", "text");
        params.put("text", text);

        return params;
    }

    private Map<String, Object> buildMarkdownMessageParams(String title, String text) {
        HashMap<String, Object> markdown = new HashMap<>();
        markdown.put("title", title);
        markdown.put("content", text);
        markdown.put("text", text);

        HashMap<String, Object> params = new HashMap<>();
        params.put("msgtype", "markdown");
        params.put("markdown", markdown);

        return params;
    }

    private enum DingDingMessageType {
        Text, Markdown
    }

}
