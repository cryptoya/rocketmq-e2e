Feature: Test the message transfer mode

  Scenario:  The Producer "Endpoint" is incorrectly, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("https://www.aliyun.com"), RequestTimeout("10s"), SessionCredentials("AccessKey","SecretKey")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the "AccessKey", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("no AccessKey","SecretKey")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the "SecretKey", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("AccessKey","no SecretKey")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the "SessionCredentials", expect start Producer success
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s")
    And The Producer set Topics("random-topic")
    Then Create a message, including the Topic("random-topic"), SubscriptionExpression("TagA"), Key("Key"), and Body("Body")
    And  Send "1" messages "synchronous"
    Then Check all messages send "success"

  Scenario:  The Producer does not set the "Endpoint", expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config RequestTimeout("10s")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the "RequestTimeout", expect start Producer success
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876")
    And The Producer set Topics("random-topic")
    Then Create a message, including the Topic("random-topic"), SubscriptionExpression("TagA"), Key("Key"), and Body("Body")
    And  Send "1" messages "synchronous"
    Then Check all messages send "success"

  Scenario:  The Producer set "ClientConfiguration" is null, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config is null
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the "Topic", expect an exception occurs when the client start
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("AccessKey","no SecretKey")
    Then Check the Producer will start throw exception

  Scenario Outline:  The Producer "MaxAttempts" is incorrectly set, expect an exception occurs when the client start
    Given Create a "Normal" topic:"random-topic" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("AccessKey","SecretKey")
    And The Producer set Topics("random-topic")
    And The Producer set MaxAttempts("<MaxAttempts>")
    Then Check the Producer will start throw exception
    Examples:
      | MaxAttempts |
      | 0           |
      | -1          |

  Scenario Outline:  The Producer "Topic" is incorrectly set, expect an exception occurs when the client start
    Given Create a "Normal" topic:"topic-test" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), RequestTimeout("10s"), SessionCredentials("AccessKey","no SecretKey")
    And The Producer set Topics("<topic>")
    Then Check the Producer will start throw exception
    Examples:
      | topic                       |
      | (topic-test, topic-test)    |
      | (notExistTopic)             |
      | (topic-test, notExistTopic) |
