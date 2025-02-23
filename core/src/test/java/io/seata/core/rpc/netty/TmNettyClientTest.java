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
package io.seata.core.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Tm rpc client test.
 *
 */
public class TmNettyClientTest {

    private static final ThreadPoolExecutor
        workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * Test get instance.
     *
     * @throws Exception the exceptionDataSourceManager.
     */
    @Test
    public void testGetInstance() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        Field nettyClientKeyPoolField = getDeclaredField(tmNettyRemotingClient.getClientChannelManager(), "nettyClientKeyPool");
        nettyClientKeyPoolField.setAccessible(true);
        GenericKeyedObjectPool nettyClientKeyPool = (GenericKeyedObjectPool) nettyClientKeyPoolField.get(tmNettyRemotingClient.getClientChannelManager());
        NettyClientConfig defaultNettyClientConfig = new NettyClientConfig();
        Assertions.assertEquals(defaultNettyClientConfig.getMaxPoolActive(), nettyClientKeyPool.getMaxActive());
        Assertions.assertEquals(defaultNettyClientConfig.getMinPoolIdle(), nettyClientKeyPool.getMinIdle());
        Assertions.assertEquals(defaultNettyClientConfig.getMaxAcquireConnMills(), nettyClientKeyPool.getMaxWait());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolTestBorrow(), nettyClientKeyPool.getTestOnBorrow());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolTestReturn(), nettyClientKeyPool.getTestOnReturn());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolLifo(), nettyClientKeyPool.getLifo());
    }

    /**
     * Do connect.
     *
     * @throws Exception the exception
     */
    @Test
    public void testInit() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);

        tmNettyRemotingClient.init();

        //check if attr of tmNettyClient object has been set success
        Field clientBootstrapField = getDeclaredField(tmNettyRemotingClient, "clientBootstrap");
        clientBootstrapField.setAccessible(true);
        NettyClientBootstrap clientBootstrap = (NettyClientBootstrap)clientBootstrapField.get(tmNettyRemotingClient);
        Field bootstrapField = getDeclaredField(clientBootstrap, "bootstrap");
        bootstrapField.setAccessible(true);
        Bootstrap bootstrap = (Bootstrap) bootstrapField.get(clientBootstrap);

        Assertions.assertNotNull(bootstrap);
        Field optionsField = getDeclaredField(bootstrap, "options");
        optionsField.setAccessible(true);
        Map<ChannelOption<?>, Object> options = (Map<ChannelOption<?>, Object>)optionsField.get(bootstrap);
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.TCP_NODELAY));
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.SO_KEEPALIVE));
        Assertions.assertEquals(10000, options.get(ChannelOption.CONNECT_TIMEOUT_MILLIS));
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.SO_KEEPALIVE));
        Assertions.assertEquals(153600, options.get(ChannelOption.SO_RCVBUF));

        Field channelFactoryField = getDeclaredField(bootstrap, "channelFactory");
        channelFactoryField.setAccessible(true);
        ChannelFactory<? extends Channel>
            channelFactory = (ChannelFactory<? extends Channel>)channelFactoryField.get(bootstrap);
        Assertions.assertNotNull(channelFactory);
        Assertions.assertTrue(channelFactory.newChannel() instanceof NioSocketChannel);

    }

    /**
     * Gets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void getApplicationId() throws Exception {

    }

    /**
     * Sets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void setApplicationId() throws Exception {

    }

    /**
     * get private field in parent class
     *
     * @param object    the object
     * @param fieldName the field name
     * @return declared field
     */
    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {

            }
        }

        return null;
    }
}
