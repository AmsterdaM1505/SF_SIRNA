package com.promoit.otpservice.service.impl;

import com.promoit.otpservice.service.NotificationService;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationService() {
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host");
        this.port = Integer.parseInt(config.getProperty("smpp.port"));
        this.systemId = config.getProperty("smpp.system_id");
        this.password = config.getProperty("smpp.password");
        this.systemType = config.getProperty("smpp.system_type");
        this.sourceAddress = config.getProperty("smpp.source_addr");
    }

    private Properties loadConfig() {
        try {
            Properties props = new Properties();
            props.load(SmsNotificationService.class.getClassLoader()
                    .getResourceAsStream("sms.properties"));
            return props;
        } catch (IOException e) {
            logger.error("Failed to load sms.properties", e);
            throw new RuntimeException("Failed to load SMS configuration", e);
        }
    }

    @Override
    public void sendCode(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParam = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParam);

            String message = "Your code: " + code;
            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            logger.info("SMS sent successfully to {}", destination);
        } catch (Exception e) {
            logger.error("Failed to send SMS: {}", e.getMessage());
            throw new RuntimeException("Failed to send SMS", e);
        } finally {
            session.unbindAndClose();
        }
    }
}