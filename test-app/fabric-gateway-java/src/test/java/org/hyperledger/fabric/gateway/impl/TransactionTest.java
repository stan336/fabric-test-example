/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.DefaultCommitHandlers;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.Transaction;
import org.hyperledger.fabric.gateway.spi.CommitHandler;
import org.hyperledger.fabric.gateway.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提案提交正确与否，
 * 对应合约能否正常执行
 * 整个测试都是在模拟
 */
public class TransactionTest {
    private final TestUtils testUtils = TestUtils.getInstance();
    private final TimePeriod timeout = new TimePeriod(7, TimeUnit.DAYS);
    private Gateway.Builder gatewayBuilder;
    private Gateway gateway;
    private Channel channel;
    private Contract contract;
    private CommitHandlerFactory defaultCommithandlerFactory;
    private CommitHandler commitHandler;
    private Peer peer1;
    private Peer peer2;
    private ProposalResponse failureResponse;
    private Map<String, byte[]> transientMap;

    @Captor
    private ArgumentCaptor<Collection<ProposalResponse>> proposalResponseCaptor;
    @Captor
    private ArgumentCaptor<Collection<Peer>> peerCaptor;
    @Captor
    private ArgumentCaptor<Channel.DiscoveryOptions> discoveryOptionsCaptor;
    @Captor
    private ArgumentCaptor<TransactionProposalRequest> proposalRequestCaptor;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        peer1 = testUtils.newMockPeer("peer1");
        peer2 = testUtils.newMockPeer("peer2");

        channel = testUtils.newMockChannel("channel");
        when(channel.sendTransaction(anyCollection(), any(Channel.TransactionOptions.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(channel.getPeers(any())).thenReturn(Collections.singletonList(peer1));

        HFClient client = testUtils.newMockClient();
        when(client.getChannel(anyString())).thenReturn(channel);

        commitHandler = mock(CommitHandler.class);
        defaultCommithandlerFactory = spy(new CommitHandlerFactory() {
            @Override
            public CommitHandler create(final String transactionId, final Network network) {
                return commitHandler;
            }
        });

        gatewayBuilder = TestUtils.getInstance().newGatewayBuilder()
                .client(client)
                .commitHandler(defaultCommithandlerFactory)
                .commitTimeout(timeout.getTime(), timeout.getTimeUnit());
        gateway = gatewayBuilder.connect();
        contract = gateway.getNetwork("network").getContract("contract");

        failureResponse = testUtils.newFailureProposalResponse("Epic fail");

        transientMap = new HashMap<>();
    	transientMap.put("key1", "value1".getBytes());
    	transientMap.put("key2", "value2".getBytes());
    }

    @AfterEach
    public void afterEach() {
        gateway.close();
    }

    /**
     * 创建一笔交易后，能否正确拿到该交易名称
     */
    @Test
    public void testGetName() {
        String name = "txn";
        String result = contract.createTransaction(name).getName();
        assertThat(result).isEqualTo(name);
    }

    /**
     * 模拟提交一个无法到达的提案，channelcode查询提案结果，得到默认发出的提案
     * 提案失败，合约无法查询内容
     * @throws Exception
     */
    @Test
    public void testEvaluateNoResponse() throws Exception {
        ProposalResponse noResponse = testUtils.newUnavailableProposalResponse("No response");
        //queryByChaincode:向所有目标背书节点发送生成的交易提案，并提取出所有提案响应中的payload组成一个list返回
        //when表示的是执行一个条件，发起查询一个提案，返回一个noResponse结果
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(noResponse));

        //执行合约中的一个方法
        assertThatThrownBy(() -> contract.evaluateTransaction("txn", "arg1"))
                .isInstanceOf(ContractException.class);
    }

    /**
     * 同上，提交一个错误的提案，channelcode查询提案结果，得到错误的提案
     * 提案失败，合约无法查询内容
     * @throws Exception
     */
    @Test
    public void testEvaluateUnsuccessfulResponse() throws Exception {
        //failureResponse.getPeer() 为空
        when(failureResponse.getPeer()).thenReturn(peer1);
        //when表示的是执行一个条件，发起查询一个提案，返回一个failureResponse结果
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(failureResponse));

        assertThatThrownBy(() -> contract.evaluateTransaction("txn", "arg1"))
                .isInstanceOf(GatewayException.class);
    }

    /**
     * 提案成功提交，则合约可以正常查询内容
     * @throws Exception
     */
    @Test
    public void testEvaluateSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected);
        //when表示的是获取一个peer，则返回一个peer1
        when(response.getPeer()).thenReturn(peer1);
        //when表示的是执行一个条件，发起查询一个提案，返回一个response结果
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.evaluateTransaction("txn", "arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    /**
     * 提案成功，合约可以正常 提交参数修改
     * @throws Exception
     */
    @Test
    public void testEvaluateSuccessWithTransient() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected);
        //when表示的是获取一个peer，则返回一个peer1
        when(response.getPeer()).thenReturn(peer1);
        //when表示的是执行一个条件，发起查询一个提案，返回一个response结果
        when(channel.queryByChaincode(any(), anyCollection())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.createTransaction("txn").setTransient(transientMap).evaluate("arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    /**
     * 提交空的提案，则合约无法提交执行（修改操作）后的内容
     * @throws Exception
     */
    @Test
    public void submit_with_no_responses_throws_ContractException_with_no_responses() throws Exception {
        //when表示的是执行一个条件，发起查询提案，返回一个空结果
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.emptyList());

        ContractException e = catchThrowableOfType(
                () -> contract.submitTransaction("txn", "arg1"),
                ContractException.class);

        assertThat(e.getProposalResponses()).isEmpty();
    }

    /**
     * 提交错误的提案，则合约无法提交执行（修改操作）后的内容
     * @throws Exception
     */
    @Test
    public void submit_with_bad_responses_throws_ContractException_with_responses() throws Exception {
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(failureResponse));

        ContractException e = catchThrowableOfType(
                () -> contract.submitTransaction("txn", "arg1"),
                ContractException.class);
        assertThat(e).hasMessageContaining(failureResponse.getMessage());
        assertThat(e.getProposalResponses()).containsExactly(failureResponse);
    }

    /**
     * 提交一个正确的提案，则可以正常提交合约
     * @throws Exception
     */
    @Test
    public void testSubmitSuccess() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected);
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.submitTransaction("txn", "arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    /**
     * 提交一个正确的提案，则可以正常修改并提交合约
     * @throws Exception
     */
    @Test
    public void testSubmitSuccessWithTransient() throws Exception {
        String expected = "successful result";
        ProposalResponse response = testUtils.newSuccessfulProposalResponse(expected);
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        byte[] result = contract.createTransaction("txn")
                .setTransient(transientMap)
                .submit("arg1");
        assertThat(new String(result)).isEqualTo(expected);
    }

    /**
     * 提交一个正常的提案，合约提交后，看是否超时
     * @throws Exception
     */
    @Test
    public void testUsesGatewayCommitTimeout() throws Exception {
        ProposalResponse response = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        contract.submitTransaction("txn", "arg1");

        verify(commitHandler).waitForEvents(timeout.getTime(), timeout.getTimeUnit());
    }

    /**
     * 提交的提案有正确的和错误的，合约提交后，是否能校验到正确的提案
     * @throws Exception
     */
    @Test
    public void testSubmitSuccessWithSomeBadProposalResponses() throws Exception {
        ProposalResponse goodResponse = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Arrays.asList(failureResponse, goodResponse));

        contract.submitTransaction("txn", "arg1");

        verify(channel).sendTransaction(proposalResponseCaptor.capture(), any(Channel.TransactionOptions.class));
        assertThat(proposalResponseCaptor.getValue()).containsExactly(goodResponse);
    }

    /**
     * 合约修改后，让peer2来签名确认
     * @throws Exception
     */
    @Test
    public void testSubmitWithEndorsingPeers() throws Exception {
        ProposalResponse goodResponse = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any(TransactionProposalRequest.class), anyCollection()))
                .thenReturn(Collections.singletonList(goodResponse));

        contract.createTransaction("txn")
                .setEndorsingPeers(Collections.singletonList(peer2))
                .submit();

        verify(channel).sendTransactionProposal(any(TransactionProposalRequest.class), peerCaptor.capture());
        assertThat(peerCaptor.getValue()).containsExactly(peer2);
    }

    /**
     * 用发现服务获取gateway来提交合约
     * @throws Exception
     */
    @Test
    public void submit_using_discovery_sets_inspect_results_option() throws Exception {
        String expected = "successful result";
        ProposalResponse goodResponse = testUtils.newSuccessfulProposalResponse(expected);
        //签名成功
        when(channel.sendTransactionProposalToEndorsers(any(TransactionProposalRequest.class), any(Channel.DiscoveryOptions.class)))
                .thenReturn(Collections.singletonList(goodResponse));
        gateway = gatewayBuilder
                .discovery(true)
                .connect();
        contract = gateway.getNetwork("network").getContract("contract");

        byte[] result = contract.submitTransaction("txn");

        assertThat(new String(result)).isEqualTo(expected);
        verify(channel).sendTransactionProposalToEndorsers(any(TransactionProposalRequest.class), discoveryOptionsCaptor.capture());
        assertThat(discoveryOptionsCaptor.getValue().isInspectResults()).isTrue();
    }

    /**
     * 提案正常
     * 如果合约有异常，则合约提交失败
     * @throws Exception
     */
    @Test
    public void commit_failure_throws_ContractException_with_proposal_responses() throws Exception {
        ProposalResponse response = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));
        ContractException commitException = new ContractException("Commit failed");
        doThrow(commitException).when(commitHandler).waitForEvents(anyLong(), any(TimeUnit.class));

        ContractException e = catchThrowableOfType(
                () -> contract.submitTransaction("txn", "arg1"),
                ContractException.class);

        assertThat(e.getProposalResponses()).containsExactly(response);
    }

    /**
     * 获取合约txid
     */
    @Test
    public void get_transaction_ID() {
        String transactionId = contract.createTransaction("txn").getTransactionId();

        assertThat(transactionId).isNotEmpty();
    }

    /**
     * 检测合约提交成功后，txid是否存在
     * @throws Exception
     */
    @Test
    public void submit_proposal_includes_transaction_ID() throws Exception {
        ProposalResponse response = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        Transaction transaction = contract.createTransaction("txn");
        String expected = transaction.getTransactionId();
        transaction.submit();

        verify(channel).sendTransactionProposal(proposalRequestCaptor.capture());
        Optional<TransactionContext> actual = proposalRequestCaptor.getValue().getTransactionContext();
        assertThat(actual).hasValueSatisfying(context -> {
           assertThat(context.getTxID()).isEqualTo(expected);
        });
    }

    /**
     * 默认提交
     * @throws Exception
     */
    @Test
    public void submit_uses_default_commit_handler() throws Exception {
        ProposalResponse response = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        contract.submitTransaction("txn");

        verify(defaultCommithandlerFactory).create(anyString(), any(Network.class));
    }

    /**
     * 提案成功
     * 利用详细的commitHandler，合约提交时，其中可以回调处理业务
     * @throws Exception
     */
    @Test
    public void submit_uses_specified_commit_handler() throws Exception {
        CommitHandlerFactory commitHandlerFactory = spy(new CommitHandlerFactory() {
            @Override
            public CommitHandler create(final String transactionId, final Network network) {
                return commitHandler;
            }
        });
        ProposalResponse response = testUtils.newSuccessfulProposalResponse();
        when(channel.sendTransactionProposal(any())).thenReturn(Collections.singletonList(response));

        contract.createTransaction("txn")
                .setCommitHandler(commitHandlerFactory)
                .submit();

        verify(commitHandlerFactory).create(anyString(), any(Network.class));
    }
}
