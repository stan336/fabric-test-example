/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用于检测网络某channel和合约是否正常
 * 需要先开启fabric网络
 */
public class NetworkTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private Network network;

    @BeforeEach
    public void beforeEach() throws Exception {
        gateway = testUtils.newGatewayBuilder().connect();
        //获取一个已经存在的channel
        //不存在则抛出异常
        network = gateway.getNetwork("mychannel");  //获取当前channel
    }

    @AfterEach
    public void afterEach() {
        gateway.close();
    }

    /**
     * 检测channel是否一致
     */
    @Test
    public void testGetChannel() {
        Channel ch1 = network.getChannel();
        assertThat(ch1.getName()).isEqualTo("mychannel");
    }

    @Test
    public void testGetGateway() {
        Gateway gw = network.getGateway();
        assertThat(gw).isSameAs(gateway);
    }

    /**
     * 判断读取到的合约是不是一个合约类
     * 这里无论填入的链码ID在网络中是否存在，其实都是合约类，
     */
    @Test
    public void testGetContract() {
        Contract contract = network.getContract("hospital_recordInfo");
        assertThat(contract).isInstanceOf(ContractImpl.class);
    }

    /**
     * 两次读取同一个合约，看读取到的是否为同一个合约
     * 第二次读是从缓存中读取
     */
    @Test
    public void testGetCachedContract() {
        Contract contract = network.getContract("hospital_recordInfo");
        Contract contract2 = network.getContract("hospital_recordInfo");
        assertThat(contract).isSameAs(contract2);
    }

    /**
     * 传入字符串空链码名称，看是否有异常显示
     */
    @Test
    public void testGetContractEmptyId() {
        assertThatThrownBy(() -> network.getContract(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    /**
     * 传入null空链码名称，看是否有异常显示
     */
    @Test
    public void testGetContractNullId() {
        assertThatThrownBy(() -> network.getContract(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: chaincodeId must be a non-empty string");
    }

    /**
     * 如果一个链码下有多个合约，若合约传入空，看是否有异常
     * 合约默认名称为类名
     */
    @Test
    public void testGetContractNullName() {
        assertThatThrownBy(() -> network.getContract("hospital_recordInfo", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("getContract: name must not be null");
    }

    /**
     * 测试网络链接是否关闭
     */
    @Test
    public void testCloseNetworkShutsDownTheChannel() {
        ((NetworkImpl)network).close();
        assertThat(network.getChannel().isShutdown()).isTrue();
    }
}
