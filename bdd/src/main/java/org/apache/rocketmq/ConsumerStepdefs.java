package org.apache.rocketmq;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ConsumerStepdefs {

    @And("Iterate the message list and invoke {string} one by one")
    public void iterateTheMessageListAndInvokeOneByOne(String arg0) {
    }

    @Then("Invoke the consumer {string} in a loop")
    public void invokeTheConsumerInALoop(String arg0) {

    }

    @When("Create a SimpleConsumer, set the Endpoint\\({string}), ConsumerGroup\\({string}), SubscriptionExpression\\({string}), Topic\\({string}), AwaitDuration\\({string})")
    public void createASimpleConsumerSetTheEndpointConsumerGroupSubscriptionExpressionTopicAwaitDuration(String arg0, String arg1, String arg2, String arg3, String arg4) {

    }
}
