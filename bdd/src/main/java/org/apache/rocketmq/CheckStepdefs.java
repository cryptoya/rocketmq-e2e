package org.apache.rocketmq;

import io.cucumber.java.en.Then;

public class CheckStepdefs {

    @Then("Check the Producer will start throw exception")
    public void checkTheProducerWillStartThrowException() {

    }

    @Then("Check all messages send {string}")
    public void checkAllMessagesSend(String arg0) {

    }

    @Then("Check send message failed")
    public void checkSendMessageFailed() {

    }

    /**
     *   check message num;
     *   check received messageId with producer send result
     */
    @Then("Check all messages that can be consumed within {int}s")
    public void checkAllMessagesThatCanBeConsumedWithinS(int arg0) {

    }

}
