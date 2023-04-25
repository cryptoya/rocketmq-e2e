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

    @When("Create a {string}, set client config Endpoint\\({string}), RequestTimeout\\({string}), SessionCredentials\\({string},{string})")
    public void createASetClientConfigEndpointRequestTimeoutSessionCredentials(String arg0, String arg1, String arg2, String arg3, String arg4) {

    }
}
