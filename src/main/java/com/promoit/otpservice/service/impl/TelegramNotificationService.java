package com.promoit.otpservice.service.impl;

import com.promoit.otpservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class TelegramNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String botToken;
    private final String defaultChatId;
    private final String apiUrl;
    private final HttpClient httpClient;

    public TelegramNotificationService() {
        Properties props = loadConfig();
        this.botToken = props.getProperty("telegram.bot.token");
        this.defaultChatId = props.getProperty("telegram.chat.id");
        this.apiUrl = props.getProperty("telegram.api.url", "https://api.telegram.org/bot");
        this.httpClient = HttpClient.newHttpClient();
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(TelegramNotificationService.class.getClassLoader()
                    .getResourceAsStream("telegram.properties"));
            return props;
        } catch (IOException e) {
            logger.error("Failed to load telegram.properties", e);
            throw new RuntimeException("Failed to load Telegram configuration", e);
        }
    }

    @Override
    public void sendCode(String chatId, String code) {
        String targetChatId = (chatId != null && !chatId.isEmpty()) ? chatId : defaultChatId;
        if (targetChatId == null || targetChatId.isEmpty()) {
            throw new RuntimeException("Telegram chat ID not provided and no default set");
        }
        String message = "Your verification code is: " + code;
        String url = String.format("%s%s/sendMessage?chat_id=%s&text=%s",
                apiUrl, botToken, targetChatId, urlEncode(message));
        sendTelegramRequest(url);
    }

    private void sendTelegramRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Telegram API error. Status: {}", response.statusCode());
                throw new RuntimeException("Telegram API returned non-200 status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Telegram request interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Telegram request IO error", e);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}