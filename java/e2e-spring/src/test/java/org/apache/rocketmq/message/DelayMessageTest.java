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
import org.apache.rocketmq.client.DelayListenerImpl;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class DelayMessageTest extends SpringBootBaseTest {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private DelayListenerImpl listener;
    @Value("${rocketmq.test.topic.delay}")
    private String delayTopic;

    @Test
    public void testDelayMessage() {
        int delaySeconds = 10;
        String message = "Hello, RocketMQ!";
        SendResult sendResult = rocketMQTemplate.syncSendDelayTimeMills(delayTopic, message, delaySeconds * 1000);
        log.info("Send delay message: " + sendResult.getMsgId());
        Assertions.assertEquals(sendResult.getSendStatus(), SendStatus.SEND_OK);

        Long sendTime = System.currentTimeMillis();
        await().atMost(20, SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean result = false;
                if (listener.getReceivedMessageIds().contains(sendResult.getMsgId())) {
                    Long receivedTime = listener.getReceivedMessages().get(sendResult.getMsgId());
                    if (Math.abs((receivedTime - sendTime) / 1000) - delaySeconds < 5) {
                        result = true;
                    } else {
                        log.warn("Inaccurate delay time");
                    }
                }
                return result;
            }
        });
    }

    @Test
    public void testDeliverTimeMessage() {
        int delaySeconds = 10;
        String message = "Hello, RocketMQ!";
        SendResult sendResult = rocketMQTemplate.syncSendDeliverTimeMills(delayTopic, message, System.currentTimeMillis() + delaySeconds * 1000);
        log.info("Send deliverTime message: " + sendResult.getMsgId());
        Assertions.assertEquals(sendResult.getSendStatus(), SendStatus.SEND_OK);

        Long sendTime = System.currentTimeMillis();
        await().atMost(20, SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean result = false;
                if (listener.getReceivedMessageIds().contains(sendResult.getMsgId())) {
                    Long receivedTime = listener.getReceivedMessages().get(sendResult.getMsgId());
                    if (Math.abs((receivedTime - sendTime) / 1000) - delaySeconds < 5) {
                        result = true;
                    } else {
                        log.warn("Inaccurate delay time");
                    }
                }
                return result;
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
