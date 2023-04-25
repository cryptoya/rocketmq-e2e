Feature: Test PushConsumer client init

  Scenario:  The PushConsumer "Endpoint" is incorrectly, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("https://www.aliyun.com"), RequestTimeout("10s"), SessionCredentials("AccessKey","SecretKey")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer does not set the "AccessKey", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("no AccessKey","SecretKey")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer does not set the "SecretKey", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("AccessKey","no SecretKey")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer does not set the "SessionCredentials", expect start Producer success
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Create a message, including the Topic("random-topic"), SubscriptionExpression("TagA"), Key("Key"), and Body("Body")
    And  Send "1" messages "synchronous"
    Then Check all messages send "success"

  Scenario:  The PushConsumer does not set the "Endpoint", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config RequestTimeout("10s")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer does not set the "RequestTimeout", expect start PushConsumer success
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Create a message, including the Topic("random-topic"), SubscriptionExpression("TagA"), Key("Key"), and Body("Body")
    And  Send "1" messages "synchronous"
    Then Check all messages that can be consumed within 60s

  Scenario:  The PushConsumer set "ClientConfiguration" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config is null
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer set "ConsumerGroup" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876")
    And The "PushConsumer" set ConsumerGroup("null"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("default")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer set "MessageListener" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("random-topic"), MessageListener("null")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer set "SubscriptionExpression" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("null"), Topic("random-topic"), MessageListener("null")
    Then Check the "PushConsumer" will start throw exception

  Scenario:  The PushConsumer set "Topic" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist, a "Concurrently" group:"random-group"
    When Create a "PushConsumer", set client config Endpoint("127.0.0.1:9876")
    And The "PushConsumer" set ConsumerGroup("random-group"), SubscriptionExpression("TagA"), Topic("null"), MessageListener("null")
    Then Check the "PushConsumer" will start throw exception