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
import org.hyperledger.fabric.gateway.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 合约检测是否正常
 */
public class ContractTest {
    private Network network;

    @BeforeEach
    public void beforeEach() throws Exception {
        Gateway gateway = TestUtils.getInstance().newGatewayBuilder().connect();
        network = gateway.getNetwork("mychannel");
    }

    /**
     * channelcode中的一个tx
     */
    @Test
    public void testCreateTransaction() {
        Transaction txn = network.getContract("hospital_recordInfo").createTransaction("Save");
        assertThat(txn.getName()).isEqualTo("Save");
    }

    /**
     * channelcode中一个合约的的一个tx
     */
    @Test
    public void testCreateTransactionWithNamespace() {
        Transaction txn = network.getContract("hospital_recordInfo", "hospital_recordInfo").createTransaction("Save");
        assertThat(txn.getName()).isEqualTo("hospital_recordInfo:Save");
    }

    /**
     * 合约中不存在的一个方法，传入的是""
     */
    @Test
    public void testCreateTransactionWithEmptyNameThrows() {
        Contract contract = network.getContract("hospital_recordInfo");
        assertThatThrownBy(() -> contract.createTransaction(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 合约中不存在的一个方法，传入的是null
     */
    @Test
    public void testCreateTransactionWithNullNameThrows() {
        Contract contract = network.getContract("hospital_recordInfo");
        assertThatThrownBy(() -> contract.createTransaction(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
