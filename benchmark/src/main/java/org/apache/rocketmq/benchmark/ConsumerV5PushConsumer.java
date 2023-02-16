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

package org.apache.rocketmq.benchmark;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.SerializeType;
import org.apache.rocketmq.srvutil.ServerUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ConsumerV5PushConsumer {

    protected static ClientServiceProvider provider = ClientServiceProvider.loadService();

    public static void main(String[] args) throws InterruptedException, IOException {
        System.setProperty(RemotingCommand.SERIALIZE_TYPE_PROPERTY, SerializeType.ROCKETMQ.name());
        Options options = ServerUtil.buildCommandlineOptions(new Options());
        CommandLine commandLine = ServerUtil.parseCmdLine("benchmarkConsumer", args, buildCommandlineOptions(options), new DefaultParser());
        if (null == commandLine) {
            System.exit(-1);
        }

        final String topic = commandLine.hasOption('t') ? commandLine.getOptionValue('t').trim() : "BenchmarkTest";
        final String groupPrefix = commandLine.hasOption('g') ? commandLine.getOptionValue('g').trim() : "benchmark_consumer";
        final String isSuffixEnable = commandLine.hasOption('p') ? commandLine.getOptionValue('p').trim() : "false";
        final String filterType = commandLine.hasOption('f') ? commandLine.getOptionValue('f').trim() : null;
        final String expression = commandLine.hasOption('e') ? commandLine.getOptionValue('e').trim() : "*";
        final double failRate = commandLine.hasOption('r') ? Double.parseDouble(commandLine.getOptionValue('r').trim()) : 0.0;
        final long blockTime = commandLine.hasOption('b') ? Long.parseLong(commandLine.getOptionValue('b')) : 0;
        final boolean aclEnable = commandLine.hasOption('a') && Boolean.parseBoolean(commandLine.getOptionValue('a'));

        String group = groupPrefix;
        if (Boolean.parseBoolean(isSuffixEnable)) {
            group = groupPrefix + "_" + (System.currentTimeMillis() % 100);
        }

        System.out.printf("topic: %s, group: %s, suffix: %s, filterType: %s, expression: %s, aclEnable: %s%n", topic, group, isSuffixEnable, filterType, expression, aclEnable);

        final StatsBenchmarkConsumer statsBenchmarkConsumer = new StatsBenchmarkConsumer();

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("BenchmarkTimerThread-%d").daemon(true).build());

        final LinkedList<Long[]> snapshotList = new LinkedList<>();

        ScheduledFuture collectDataFuture = executorService.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                snapshotList.addLast(statsBenchmarkConsumer.createSnapshot());
                if (snapshotList.size() > 10) {
                    snapshotList.removeFirst();
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        ScheduledFuture computeDataFuture = executorService.scheduleAtFixedRate(new TimerTask() {
            private void printStats() {
                if (snapshotList.size() >= 10) {
                    Long[] begin = snapshotList.getFirst();
                    Long[] end = snapshotList.getLast();

                    final long consumeTps = (long) (((end[1] - begin[1]) / (double) (end[0] - begin[0])) * 1000L);
                    final double averageB2CRT = (end[2] - begin[2]) / (double) (end[1] - begin[1]);
                    final double averageS2CRT = (end[3] - begin[3]) / (double) (end[1] - begin[1]);
                    final long failCount = end[4] - begin[4];
                    final long b2cMax = statsBenchmarkConsumer.getBorn2ConsumerMaxRT().get();
                    final long s2cMax = statsBenchmarkConsumer.getStore2ConsumerMaxRT().get();

                    statsBenchmarkConsumer.getBorn2ConsumerMaxRT().set(0);
                    statsBenchmarkConsumer.getStore2ConsumerMaxRT().set(0);

                    System.out.printf("Current Time: %s | Consume TPS: %d | AVG(B2C) RT(ms): %7.3f | AVG(S2C) RT(ms): %7.3f | MAX(B2C) RT(ms): %d | MAX(S2C) RT(ms): %d | Consume Fail: %d%n", UtilAll.timeMillisToHumanString2(System.currentTimeMillis()), consumeTps, averageB2CRT, averageS2CRT, b2cMax, s2cMax, failCount);
                }
            }

            @Override
            public void run() {
                try {
                    this.printStats();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);


        String endpoint = commandLine.getOptionValue('n');
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoint).setRequestTimeout(Duration.ofSeconds(10));
        if (aclEnable) {
            String ak = commandLine.hasOption("ak") ? String.valueOf(commandLine.getOptionValue("ak")) : AclClient.ACL_ACCESS_KEY;
            String sk = commandLine.hasOption("sk") ? String.valueOf(commandLine.getOptionValue("sk")) : AclClient.ACL_SECRET_KEY;
            StaticSessionCredentialsProvider staticSessionCredentialsProvider = new StaticSessionCredentialsProvider(ak, sk);
            builder.setCredentialProvider(staticSessionCredentialsProvider);
        }
        ClientConfiguration configuration = builder.build();
        PushConsumer consumer = null;
        try {
            consumer = provider.newPushConsumerBuilder().setClientConfiguration(configuration).setConsumerGroup(group).setSubscriptionExpressions(Collections.singletonMap(topic, new FilterExpression(expression, FilterExpressionType.TAG))).setMessageListener(new MessageListener() {
                @Override
                public ConsumeResult consume(MessageView messageView) {
//                            MessageExt msg = messageView.get(0);
                    long now = System.currentTimeMillis();

                    statsBenchmarkConsumer.getReceiveMessageTotalCount().increment();

                    long born2ConsumerRT = now - messageView.getBornTimestamp();
                    statsBenchmarkConsumer.getBorn2ConsumerTotalRT().add(born2ConsumerRT);

//                            long store2ConsumerRT = now - messageView.getDeliveryTimestamp();
//                            statsBenchmarkConsumer.getStore2ConsumerTotalRT().add(store2ConsumerRT);

                    compareAndSetMax(statsBenchmarkConsumer.getBorn2ConsumerMaxRT(), born2ConsumerRT);

//                            compareAndSetMax(statsBenchmarkConsumer.getStore2ConsumerMaxRT(), store2ConsumerRT);

                    if (ThreadLocalRandom.current().nextDouble() < failRate) {
                        statsBenchmarkConsumer.getFailCount().increment();
                        return ConsumeResult.SUCCESS;
                    } else {
                        return ConsumeResult.FAILURE;
                    }
                }
            }).build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.printf("Consumer Started.%n");
        if (blockTime > 0) {
            Thread.sleep(blockTime * 1000);
            collectDataFuture.cancel(true);
            computeDataFuture.cancel(true);
            consumer.close();
        }

        if (snapshotList.size() >= 10) {
            Long[] begin = snapshotList.getFirst();
            Long[] end = snapshotList.getLast();

            final long consumeTps = (long) (((end[1] - begin[1]) / (double) (end[0] - begin[0])) * 1000L);
            final double averageB2CRT = (end[2] - begin[2]) / (double) (end[1] - begin[1]);
            final double averageS2CRT = (end[3] - begin[3]) / (double) (end[1] - begin[1]);
            final long failCount = end[4] - begin[4];
            final long b2cMax = statsBenchmarkConsumer.getBorn2ConsumerMaxRT().get();
            final long s2cMax = statsBenchmarkConsumer.getStore2ConsumerMaxRT().get();

            statsBenchmarkConsumer.getBorn2ConsumerMaxRT().set(0);
            statsBenchmarkConsumer.getStore2ConsumerMaxRT().set(0);

            System.out.printf("Current Time: %s | Consume TPS: %d | AVG(B2C) RT(ms): %7.3f | AVG(S2C) RT(ms): %7.3f | MAX(B2C) RT(ms): %d | MAX(S2C) RT(ms): %d | Consume Fail: %d%n", UtilAll.timeMillisToHumanString2(System.currentTimeMillis()), consumeTps, averageB2CRT, averageS2CRT, b2cMax, s2cMax, failCount);
            OutputStreamWriter osw = null;
            try {
                osw = new OutputStreamWriter(new FileOutputStream("result.json"), "UTF-8");

                JSONObject obj = new JSONObject();
                JSONArray arr = new JSONArray();
                JSONObject objSub = new JSONObject();
                objSub.put("name", "recv tps");
                objSub.put("data", consumeTps);
                obj.put("name", "性能测试");
                arr.add(objSub);
                obj.put("datas", arr);

                System.out.println(obj.toString());

                osw.write(obj.toString());
                osw.flush();//清空缓冲区，强制输出数据
                osw.close();//关闭输出流
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("t", "topic", true, "Topic name, Default: BenchmarkTest");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("g", "group", true, "Consumer group name, Default: benchmark_consumer");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("p", "group prefix enable", true, "Is group prefix enable, Default: false");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("f", "filterType", true, "TAG, SQL92");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("e", "expression", true, "filter expression content file path.ie: ./test/expr");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("r", "fail rate", true, "consumer fail rate, default 0");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("b", "blockTime", true, "Consume message time, Default: 0(Seconds), running forever");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("a", "aclEnable", true, "Acl Enable, Default: false");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("ak", "accessKey", true, "Acl access key, Default: 12345678");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("sk", "secretKey", true, "Acl secret key, Default: rocketmq2");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }

    public static void compareAndSetMax(final AtomicLong target, final long value) {
        long prev = target.get();
        while (value > prev) {
            boolean updated = target.compareAndSet(prev, value);
            if (updated) break;

            prev = target.get();
        }
    }
}

