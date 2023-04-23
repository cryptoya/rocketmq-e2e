Feature: Test the message transfer mode

  Scenario:  The Endpoint setting of the Producer failed, expect an exception occurs when the client start
    Given Create a Producer, set client config Endpoint("https://www.aliyun.com"), SessionCredentials("AccessKey","SecretKey")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario:  The Producer does not set the AccessKey, expect an exception occurs when the client start
    Given Create a Producer, set client config Endpoint("127.0.0.1:9876"), SessionCredentials("no AccessKey","SecretKey")
    And The Producer set Topics("random-topic")
    Then Check the Producer will start throw exception

  Scenario Outline:  The Producer does not set the SecretKey, expect an exception occurs when the client start
    Given Create a Producer, set client config Endpoint("127.0.0.1:9876"), SessionCredentials("AccessKey","no SecretKey")
    And The Producer set Topics("random-topic")
    And The Producer set MaxAttempts("<MaxAttempts>")
    Then Check the Producer will start throw exception
    Examples:
      | MaxAttempts |
      | 0           |
      | -1          |

  Scenario Outline:  The Producer topic is incorrectly set, expect an exception occurs when the client start
    Given Create a "Normal" topic:"topic-test" if not exist
    When Create a Producer, set client config Endpoint("127.0.0.1:9876"), SessionCredentials("AccessKey","no SecretKey")
    And The Producer set Topics("<topic>")
    Then Check the Producer will start throw exception
    Examples:
      | topic                       |
      | (topic-test, topic-test)    |
      | (notExistTopic)             |
      | (topic-test, notExistTopic) |
