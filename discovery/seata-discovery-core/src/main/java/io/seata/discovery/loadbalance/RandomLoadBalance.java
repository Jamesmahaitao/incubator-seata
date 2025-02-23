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
package io.seata.discovery.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import io.seata.common.loader.LoadLevel;

import static io.seata.discovery.loadbalance.LoadBalanceFactory.RANDOM_LOAD_BALANCE;

/**
 * The type Random load balance.
 *
 */
@LoadLevel(name = RANDOM_LOAD_BALANCE)
public class RandomLoadBalance implements LoadBalance {

    @Override
    public <T> T select(List<T> invokers, String xid) {
        int length = invokers.size();
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }
}
