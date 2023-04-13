/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.SpringBootBaseTest;
import org.apache.rocketmq.client.NormalListenerImpl;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class NormalMessageTest extends SpringBootBaseTest {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private NormalListenerImpl consumer;
    @Value("${rocketmq.test.topic.normal}")
    private String normalTopic;

    @Test
    public void testSendMessage() {
        String message = "Hello, RocketMQ!";
        SendResult sendResult = rocketMQTemplate.syncSend(normalTopic, MessageBuilder.withPayload(message).build());
        log.info(sendResult.getMsgId());
        Assertions.assertNotNull(sendResult);
        Assertions.assertEquals(sendResult.getSendStatus(), SendStatus.SEND_OK);
        await().atMost(10, SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return consumer.getReceivedMessageIds().contains(sendResult.getMsgId());
            }
        });
    }

    @Test
    public void testAsyncSendMessage() {
        final String[] messageId = {""};
        rocketMQTemplate.asyncSend(normalTopic, new OrderPaidEvent("T_001", new BigDecimal("88.00")), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info(sendResult.getMsgId());
                messageId[0] = sendResult.getMsgId();
            }

            @Override
            public void onException(Throwable throwable) {
                log.warn(throwable.getMessage());
            }
        });
        await().atMost(10, SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return consumer.getReceivedMessageIds().contains(messageId[0]);
            }
        });
    }

    @Test
    public void testConvertAndSend() {
        String message = "Hello, RocketMQ!";
        rocketMQTemplate.convertAndSend(normalTopic, message);
    }

    @Test
    public void testSendOrderly() {
        SendResult sendResult = rocketMQTemplate.syncSendOrderly(normalTopic, new OrderPaidEvent("T_001", new BigDecimal("88.00")), "test");
        log.info(sendResult.getMsgId());
        Assertions.assertEquals(sendResult.getSendStatus(), SendStatus.SEND_OK);
        await().atMost(10, SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return consumer.getReceivedMessageIds().contains(sendResult.getMsgId());
            }
        });
    }


    @Data
    @AllArgsConstructor
    public class OrderPaidEvent implements Serializable {
        private String orderId;

        private BigDecimal paidMoney;
    }

}
