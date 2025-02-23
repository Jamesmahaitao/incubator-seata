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
package io.seata.saga.engine.mock;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateRouter;
import io.seata.saga.engine.pcext.StateRouterInterceptor;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class MockStateRouterInterceptor implements StateRouterInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockStateRouterInterceptor.class);

    @Override
    public void preRoute(ProcessContext context, State state) throws EngineExecutionException {
        LOGGER.info("test StateRouterInterceptor preRoute");
    }

    @Override
    public void postRoute(ProcessContext context, State state, Instruction instruction, Exception e) throws EngineExecutionException {
        LOGGER.info("test StateRouterInterceptor postRoute");
    }

    @Override
    public boolean match(Class<? extends InterceptableStateRouter> clazz) {
        return true;
    }
}
