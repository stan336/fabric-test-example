/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用于检测fabric网络是否正常
 */
public class GatewayTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway.Builder builder = null;

    @BeforeEach
    public void beforeEach() throws Exception {
        builder = testUtils.newGatewayBuilder();
    }

    /**
     * 类似NetworkTest.java中，测试channel是否存在
     * 输入正确的channel
     */
    @Test
    public void testGetNetworkFromConfig() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            assertThat(network.getChannel().getName()).isEqualTo("mychannel");
        }
    }

    /**
     * 类似NetworkTest.java中，测试channel是否存在，同上
     */
    @Test
    public void testGetMychannelNetwork() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            assertThat(network.getChannel().getName()).isEqualTo("mychannel");
        }
    }

    /**
     * 两次读取mychannel，第二次是内存中读取
     */
    @Test
    public void testGetCachedNetwork() {
        try (Gateway gateway = builder.connect()) {
            Network network = gateway.getNetwork("mychannel");
            Network network2 = gateway.getNetwork("mychannel");
            assertThat(network).isSameAs(network2);
        }
    }

    /**
     * 输入“”字符串，看是否读取channel正常
     */
    @Test
    public void testGetNetworkEmptyString() {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    /**
     * 输入null字符串，看是否读取channel正常
     */
    @Test
    public void testGetNetworkNullString() {
        try (Gateway gateway = builder.connect()) {
            assertThatThrownBy(() -> gateway.getNetwork(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Channel name must be a non-empty string");
        }
    }

    /**
     * 测试能否正常关闭网络连接
     */
    @Test
    public void testCloseGatewayClosesNetworks() {
        Gateway gateway = builder.connect();
        Channel channel = gateway.getNetwork("assumed").getChannel();

        gateway.close();

        assertThat(channel.isShutdown()).isTrue();
    }
}
