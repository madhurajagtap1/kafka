/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.test;

import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.NotControllerException;
import org.apache.kafka.common.message.AlterIsrRequestData;
import org.apache.kafka.common.message.AlterIsrResponseData;
import org.apache.kafka.common.message.BrokerHeartbeatRequestData;
import org.apache.kafka.common.message.BrokerRegistrationRequestData;
import org.apache.kafka.common.message.CreateTopicsRequestData;
import org.apache.kafka.common.message.CreateTopicsResponseData;
import org.apache.kafka.common.message.ElectLeadersRequestData;
import org.apache.kafka.common.message.ElectLeadersResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaEntity;
import org.apache.kafka.common.requests.ApiError;
import org.apache.kafka.controller.Controller;
import org.apache.kafka.controller.ResultOrError;
import org.apache.kafka.metadata.BrokerHeartbeatReply;
import org.apache.kafka.metadata.BrokerRegistrationReply;
import org.apache.kafka.metadata.FeatureMapAndEpoch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class MockController implements Controller {
    private final static NotControllerException NOT_CONTROLLER_EXCEPTION =
        new NotControllerException("This is not the correct controller for this cluster.");

    public static class Builder {
        private final Map<String, MockTopic> initialTopics = new HashMap<>();

        public Builder newInitialTopic(String name, Uuid id) {
            initialTopics.put(name, new MockTopic(name, id));
            return this;
        }

        public MockController build() {
            return new MockController(initialTopics.values());
        }
    }

    private volatile boolean active = true;

    private MockController(Collection<MockTopic> initialTopics) {
        for (MockTopic topic : initialTopics) {
            topics.put(topic.id, topic);
            topicNameToId.put(topic.name, topic.id);
        }
    }

    @Override
    public CompletableFuture<AlterIsrResponseData> alterIsr(AlterIsrRequestData request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<CreateTopicsResponseData> createTopics(CreateTopicsRequestData request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> unregisterBroker(int brokerId) {
        throw new UnsupportedOperationException();
    }

    static class MockTopic {
        private final String name;
        private final Uuid id;

        MockTopic(String name, Uuid id) {
            this.name = name;
            this.id = id;
        }
    }

    private final Map<String, Uuid> topicNameToId = new HashMap<>();

    private final Map<Uuid, MockTopic> topics = new HashMap<>();

    @Override
    synchronized public CompletableFuture<Map<String, ResultOrError<Uuid>>>
            findTopicIds(Collection<String> topicNames) {
        Map<String, ResultOrError<Uuid>> results = new HashMap<>();
        for (String topicName : topicNames) {
            if (!topicNameToId.containsKey(topicName)) {
                results.put(topicName, new ResultOrError<>(new ApiError(Errors.UNKNOWN_TOPIC_OR_PARTITION)));
            } else {
                results.put(topicName, new ResultOrError<>(topicNameToId.get(topicName)));
            }
        }
        return CompletableFuture.completedFuture(results);
    }

    @Override
    synchronized public CompletableFuture<Map<Uuid, ResultOrError<String>>>
            findTopicNames(Collection<Uuid> topicIds) {
        Map<Uuid, ResultOrError<String>> results = new HashMap<>();
        for (Uuid topicId : topicIds) {
            MockTopic topic = topics.get(topicId);
            if (topic == null) {
                results.put(topicId, new ResultOrError<>(new ApiError(Errors.UNKNOWN_TOPIC_ID)));
            } else {
                results.put(topicId, new ResultOrError<>(topic.name));
            }
        }
        return CompletableFuture.completedFuture(results);
    }

    @Override
    synchronized public CompletableFuture<Map<Uuid, ApiError>>
            deleteTopics(Collection<Uuid> topicIds) {
        if (!active) {
            CompletableFuture<Map<Uuid, ApiError>> future = new CompletableFuture<>();
            future.completeExceptionally(NOT_CONTROLLER_EXCEPTION);
            return future;
        }
        Map<Uuid, ApiError> results = new HashMap<>();
        for (Uuid topicId : topicIds) {
            MockTopic topic = topics.remove(topicId);
            if (topic == null) {
                results.put(topicId, new ApiError(Errors.UNKNOWN_TOPIC_ID));
            } else {
                topicNameToId.remove(topic.name);
                results.put(topicId, ApiError.NONE);
            }
        }
        return CompletableFuture.completedFuture(results);
    }

    @Override
    public CompletableFuture<Map<ConfigResource, ResultOrError<Map<String, String>>>> describeConfigs(Map<ConfigResource, Collection<String>> resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<ElectLeadersResponseData> electLeaders(ElectLeadersRequestData request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<FeatureMapAndEpoch> finalizedFeatures() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map<ConfigResource, ApiError>> incrementalAlterConfigs(
            Map<ConfigResource, Map<String, Map.Entry<AlterConfigOp.OpType, String>>> configChanges,
            boolean validateOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map<ConfigResource, ApiError>> legacyAlterConfigs(
            Map<ConfigResource, Map<String, String>> newConfigs, boolean validateOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<BrokerHeartbeatReply>
            processBrokerHeartbeat(BrokerHeartbeatRequestData request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<BrokerRegistrationReply>
            registerBroker(BrokerRegistrationRequestData request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> waitForReadyBrokers(int minBrokers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map<ClientQuotaEntity, ApiError>>
            alterClientQuotas(Collection<ClientQuotaAlteration> quotaAlterations, boolean validateOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Long> beginWritingSnapshot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beginShutdown() {
        this.active = false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public long curClaimEpoch() {
        return active ? 1 : -1;
    }

    @Override
    public void close() {
        beginShutdown();
    }
}
