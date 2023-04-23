Feature: Test the message transfer mode

  Scenario:  Send 20 normal messages synchronously and expect consume with receive() and ack() messages successful
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a SimpleConsumer, set the Endpoint("127.0.0.1:9876"), ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), AwaitDuration("10s")
    And Create a Producer, set the Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), Topic("random-topic")
    And Create a message, including the Topic("random-topic"), SubscriptionExpression("TagA"), Key("Key"), and Body("Body")
    And  Send "20" messages "synchronous"
    Then Check all messages send "success"
    When Invoke the consumer "receive(maxMessageNum, invisibleDuration)" in a loop
    And Iterate the message list and invoke "ack()" one by one
    Then Check all messages that can be consumed within 60s
    And Shutdown the producer and consumer
