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

package org.apache.rocketmq;

import io.cucumber.java.en.Then;

public class CheckStepdefs {

    @Then("Check all messages send {string}")
    public void checkAllMessagesSend(String success) {

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

    @Then("Check the {string} will start throw exception")
    public void checkTheWillStartThrowException(String client) {

    }
}
