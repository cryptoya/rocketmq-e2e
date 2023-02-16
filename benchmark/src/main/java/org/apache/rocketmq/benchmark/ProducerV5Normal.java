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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.SerializeType;
import org.apache.rocketmq.srvutil.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ProducerV5Normal {
    protected static ClientServiceProvider provider = ClientServiceProvider.loadService();
    private static final Logger log = LoggerFactory.getLogger(ProducerV4.class);

    private static byte[] msgBody;
    private static final int MAX_LENGTH_ASYNC_QUEUE = 10000;
    private static final int SLEEP_FOR_A_WHILE = 100;

    public static void main(String[] args) throws MQClientException {
        System.setProperty(RemotingCommand.SERIALIZE_TYPE_PROPERTY, SerializeType.ROCKETMQ.name());

        Options options = ServerUtil.buildCommandlineOptions(new Options());
        CommandLine commandLine = ServerUtil.parseCmdLine("benchmarkProducer", args, buildCommandlineOptions(options), new DefaultParser());
        if (null == commandLine) {
            System.exit(-1);
        }

        final String topic = commandLine.hasOption('t') ? commandLine.getOptionValue('t').trim() : "BenchmarkTest";
        final int messageSize = commandLine.hasOption('s') ? Integer.parseInt(commandLine.getOptionValue('s')) : 128;
        final String messageKey = commandLine.hasOption('k') ? commandLine.getOptionValue('k') : "default";
        final String messageTag = commandLine.hasOption('l') ? commandLine.getOptionValue('l') : "abc";
        final int propertySize = commandLine.hasOption('p') ? Integer.parseInt(commandLine.getOptionValue('p')) : 0;
        final boolean msgTraceEnable = commandLine.hasOption('m') && Boolean.parseBoolean(commandLine.getOptionValue('m'));
        final boolean aclEnable = commandLine.hasOption('a') && Boolean.parseBoolean(commandLine.getOptionValue('a'));
        final long messageNum = commandLine.hasOption('q') ? Long.parseLong(commandLine.getOptionValue('q')) : 0;
        final long blockTime = commandLine.hasOption('b') ? Long.parseLong(commandLine.getOptionValue('b')) : 0;
        final boolean delayEnable = commandLine.hasOption('d') && Boolean.parseBoolean(commandLine.getOptionValue('d'));
        final int delayLevel = commandLine.hasOption('e') ? Integer.parseInt(commandLine.getOptionValue('e')) : 1;
        final boolean asyncEnable = commandLine.hasOption('y') && Boolean.parseBoolean(commandLine.getOptionValue('y'));
        final int threadCount = asyncEnable ? 1 : commandLine.hasOption('w') ? Integer.parseInt(commandLine.getOptionValue('w')) : 64;

        System.out.printf("topic: %s, threadCount: %d, messageSize: %d, messageKey: %s, propertySize: %d, messageTag: %s, " + "traceEnable: %s, aclEnable: %s, messageQuantity: %d, delayEnable: %s, delayLevel: %s, " + "asyncEnable: %s%n", topic, threadCount, messageSize, messageKey, propertySize, messageTag, msgTraceEnable, aclEnable, messageNum, delayEnable, delayLevel, asyncEnable);

        StringBuilder sb = new StringBuilder(messageSize);
        for (int i = 0; i < messageSize; i++) {
            sb.append(RandomStringUtils.randomAlphanumeric(1));
        }
        msgBody = sb.toString().getBytes(StandardCharsets.UTF_8);

        final ExecutorService sendThreadPool = Executors.newFixedThreadPool(threadCount);

        final StatsBenchmarkProducer statsBenchmark = new StatsBenchmarkProducer();

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("BenchmarkTimerThread-%d").daemon(true).build());

        final LinkedList<Long[]> snapshotList = new LinkedList<>();

        final long[] msgNums = new long[threadCount];

        if (messageNum > 0) {
            Arrays.fill(msgNums, messageNum / threadCount);
            long mod = messageNum % threadCount;
            if (mod > 0) {
                msgNums[0] += mod;
            }
        }

        executorService.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                snapshotList.addLast(statsBenchmark.createSnapshot());
                if (snapshotList.size() > 10) {
                    snapshotList.removeFirst();
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        executorService.scheduleAtFixedRate(new TimerTask() {
            private void printStats() {
                if (snapshotList.size() >= 10) {
                    doPrintStats(snapshotList, statsBenchmark, false);
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

        Producer producer = null;
        try {
            producer = provider.newProducerBuilder().setClientConfiguration(configuration).setTopics(topic).build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        for (int i = 0; i < threadCount; i++) {
            final long msgNumLimit = msgNums[i];
            if (messageNum > 0 && msgNumLimit == 0) {
                break;
            }
            Producer finalProducer = producer;
            sendThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    long startThreadTime = System.currentTimeMillis();
                    int num = 0;
                    while (true) {
                        final long beginTimestamp = System.currentTimeMillis();
                        try {
                            String key = messageKey + "-" + beginTimestamp / 1000;
                            final Message message = buildMessage(topic, messageTag, key);

                            finalProducer.send(message);
                            updateStatsSuccess(statsBenchmark, beginTimestamp);
                        } catch (ClientException e) {
                            statsBenchmark.getSendRequestFailedCount().increment();
                            log.error("[BENCHMARK_PRODUCER] Send Exception", e);
                        }
                        if (messageNum > 0 && ++num >= msgNumLimit) {
                            break;
                        }
                        if (blockTime > 0 && (startThreadTime + blockTime * 1000) < beginTimestamp) {
                            break;
                        }
                    }
                }
            });
        }
        try {
            sendThreadPool.shutdown();
            sendThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            executorService.shutdown();
            try {
                executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }

            producer.close();
            if (snapshotList.size() > 1) {
                doPrintStats(snapshotList, statsBenchmark, true);
            } else {
                System.out.printf("[Complete] Send Total: %d Send Failed: %d Response Failed: %d%n", statsBenchmark.getSendRequestSuccessCount().longValue() + statsBenchmark.getSendRequestFailedCount().longValue(), statsBenchmark.getSendRequestFailedCount().longValue(), statsBenchmark.getReceiveResponseFailedCount().longValue());
            }
        } catch (InterruptedException e) {
            log.error("[Exit] Thread Interrupted Exception", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updateStatsSuccess(StatsBenchmarkProducer statsBenchmark, long beginTimestamp) {
        statsBenchmark.getSendRequestSuccessCount().increment();
        statsBenchmark.getReceiveResponseSuccessCount().increment();
        final long currentRT = System.currentTimeMillis() - beginTimestamp;
        statsBenchmark.getSendMessageSuccessTimeTotal().add(currentRT);
        long prevMaxRT = statsBenchmark.getSendMessageMaxRT().longValue();
        while (currentRT > prevMaxRT) {
            boolean updated = statsBenchmark.getSendMessageMaxRT().compareAndSet(prevMaxRT, currentRT);
            if (updated) break;

            prevMaxRT = statsBenchmark.getSendMessageMaxRT().longValue();
        }
    }

    public static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("w", "threadCount", true, "Thread count, Default: 64");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("s", "messageSize", true, "Message Size, Default: 128");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("k", "keyEnable", true, "Message Key Enable, Default: false");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("t", "topic", true, "Topic name, Default: BenchmarkTest");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("l", "tagCount", true, "Tag count, Default: 0");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("m", "msgTraceEnable", true, "Message Trace Enable, Default: false");
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

        opt = new Option("q", "messageQuantity", true, "Send message quantity, Default: 0, running forever");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("b", "blockTime", true, "Send message time, Default: 0(Seconds), running forever");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("d", "delayEnable", true, "Delay message Enable, Default: false");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("e", "delayLevel", true, "Delay message level, Default: 1");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("y", "asyncEnable", true, "Enable async produce, Default: false");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }

    private static Message buildMessage(final String topic, String tags, String keys) {
        Message message = provider.newMessageBuilder().setTopic(topic).setBody(msgBody).setTag(tags).setKeys(keys).build();
        return message;
    }

    private static void doPrintStats(final LinkedList<Long[]> snapshotList, final StatsBenchmarkProducer statsBenchmark, boolean done) {
        Long[] begin = snapshotList.getFirst();
        Long[] end = snapshotList.getLast();

        final long sendTps = (long) (((end[3] - begin[3]) / (double) (end[0] - begin[0])) * 1000L);
        final double averageRT = (end[5] - begin[5]) / (double) (end[3] - begin[3]);

        if (done) {
            System.out.printf("[Complete] Send Total: %d | Send TPS: %d | Max RT(ms): %d | Average RT(ms): %7.3f | Send Failed: %d | Response Failed: %d%n", statsBenchmark.getSendRequestSuccessCount().longValue() + statsBenchmark.getSendRequestFailedCount().longValue(), sendTps, statsBenchmark.getSendMessageMaxRT().longValue(), averageRT, end[2], end[4]);

            OutputStreamWriter osw = null;
            try {
                osw = new OutputStreamWriter(new FileOutputStream("result.json"), "UTF-8");

                JSONObject obj = new JSONObject();
                JSONArray arr = new JSONArray();
                JSONObject objSub = new JSONObject();
                objSub.put("name", "send tps");
                objSub.put("data", sendTps);
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

        } else {
            System.out.printf("Current Time: %s | Send TPS: %d | Max RT(ms): %d | Average RT(ms): %7.3f | Send Failed: %d | Response Failed: %d%n", UtilAll.timeMillisToHumanString2(System.currentTimeMillis()), sendTps, statsBenchmark.getSendMessageMaxRT().longValue(), averageRT, end[2], end[4]);
        }
    }
}

