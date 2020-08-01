/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.impl.event.StubPeerDisconnectEventSource;
import org.hyperledger.fabric.gateway.spi.CommitListener;
import org.hyperledger.fabric.gateway.spi.PeerDisconnectEvent;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 事件加入监听相关，并发起提交
 * stubBlockEventSource发送块事件，CommitListener接收其中的txid事件
 */
public class CommitListenerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private Network network;
    private StubBlockEventSource stubBlockEventSource;
    private StubPeerDisconnectEventSource stubPeer1DisconnectEventSource;
    private StubPeerDisconnectEventSource stubPeer2DisconnectEventSource;
    private final Peer peer1 = testUtils.newMockPeer("peer1");
    private final Peer peer2 = testUtils.newMockPeer("peer2");
    private final Set<Peer> peers = Stream.of(peer1, peer2).collect(Collectors.toSet());
    private final String transactionId = "txId";
    private final CommitListener stubCommitListener = new CommitListener() {
        @Override
        public void acceptCommit(BlockEvent.TransactionEvent transactionEvent) { }

        @Override
        public void acceptDisconnect(PeerDisconnectEvent disconnectEvent) { }
    };

    @BeforeEach
    public void beforeEach() throws Exception {
        //用于发送块事件
        stubBlockEventSource = new StubBlockEventSource(); // Must be before network is created
        stubPeer1DisconnectEventSource = new StubPeerDisconnectEventSource(peer1);
        stubPeer2DisconnectEventSource = new StubPeerDisconnectEventSource(peer2);
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("mychannel");
    }

    @AfterEach
    public void afterEach() {
        stubBlockEventSource.close();
        stubPeer1DisconnectEventSource.close();
        stubPeer2DisconnectEventSource.close();
        gateway.close();
    }

    private void fireCommitEvents(Peer peer, String... transactionIds) {
        //一批要提交的tx事件
        //检查txid有效后，从新加入List
        List<BlockEvent.TransactionEvent> transactionEvents = Arrays.stream(transactionIds)
                .map(transactionId -> testUtils.newValidMockTransactionEvent(peer, transactionId))
                .collect(Collectors.toList());
        BlockEvent blockEvent = testUtils.newMockBlockEvent(peer, 1, transactionEvents);
        stubBlockEventSource.sendEvent(blockEvent);
    }

    /**
     * 将交易提交到监听，然后分发给指定的peers
     */
    @Test
    public void add_listener_returns_the_listener() {
        //stubCommitListener：用于监听要提交的交易
        //peers：哪些peer接收要提交的交易
        //transactionId：交易id
        CommitListener result = network.addCommitListener(stubCommitListener, peers, transactionId);

        assertThat(result).isSameAs(stubCommitListener);
    }

    /**
     * 传入一个正常可以监听的txid，然后传入一个不正常，没有加入过提交监听的badTxid
     * 给一个peer发送txid，比如同一笔交易发送给2个peer，CommitListener算是监听到2次提交
     * 看CommitListener最终接收了几笔交易，错误交易应该被忽略
     */
    @Test
    public void listener_only_receives_commits_for_correct_transaction_id() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, peers, transactionId);
        fireCommitEvents(peer1, transactionId, "BAD_" + transactionId);

        verify(listener, times(2)).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    /**
     * 只给一个peer设置CommitListener，当给多个peer发送监听事件，只有一个peer能收到
     */
    @Test
    public void listener_only_receives_commits_for_correct_peers() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, Collections.singleton(peer1), transactionId);
        fireCommitEvents(peer1, transactionId);
        fireCommitEvents(peer2, transactionId);

        //只有peer1接收到
        verify(listener, times(1)).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    /**
     * 移除掉CommitListener，则无法监听到内容
     */
    @Test
    public void removed_listener_does_not_receive_commit_events() {
        CommitListener listener = spy(stubCommitListener);

        network.addCommitListener(listener, peers, transactionId);
        network.removeCommitListener(listener);
        fireCommitEvents(peer1, transactionId);

        verify(listener, never()).acceptCommit(any(BlockEvent.TransactionEvent.class));
    }

    /**
     * 若只一个peer设置了CommitListener,当发送两个peer的断开连接的请求后，只有一个能收到
     */
    @Test
    public void listener_only_receives_disconnects_for_specified_peers() {
        CommitListener listener = spy(stubCommitListener);
        //newPeerDisconnectedEvent表示的是否该节点的监听断开
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);
        PeerDisconnectEvent peer2Event = testUtils.newPeerDisconnectedEvent(peer2);

        network.addCommitListener(listener, Collections.singleton(peer1), transactionId);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);
        stubPeer2DisconnectEventSource.sendEvent(peer2Event);

        verify(listener, times(1)).acceptDisconnect(any(PeerDisconnectEvent.class));
    }

    /**
     * 移除掉CommitListener，则无法监听到Peer的断开事件连接的事件
     */
    @Test
    public void removed_listener_does_not_receive_disconnect_events() {
        CommitListener listener = spy(stubCommitListener);
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);

        network.addCommitListener(listener, peers, transactionId);
        network.removeCommitListener(listener);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);

        verify(listener, never()).acceptDisconnect(any(PeerDisconnectEvent.class));
    }

    /**
     * 网络连接断开后，能否接收到tx事件
     */
    @Test
    public void close_network_removes_listeners() {
        CommitListener listener = spy(stubCommitListener);
        PeerDisconnectEvent peer1Event = testUtils.newPeerDisconnectedEvent(peer1);

        network.addCommitListener(listener, peers, transactionId);
        ((NetworkImpl)network).close();
        fireCommitEvents(peer1, transactionId);
        stubPeer1DisconnectEventSource.sendEvent(peer1Event);

        verify(listener, never()).acceptCommit(any(BlockEvent.TransactionEvent.class));
        verify(listener, never()).acceptDisconnect(any(PeerDisconnectEvent.class));
    }
}
