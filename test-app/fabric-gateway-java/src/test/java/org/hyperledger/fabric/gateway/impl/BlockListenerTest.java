/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import java.io.IOException;
import java.util.function.Consumer;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.GatewayException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.impl.event.StubBlockEventSource;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 块信息变动的监听
 * 发送和接收事件
 * 只是将事件加入了监听，并未提交
 * stubBlockEventSource发送块事件，CommitListener接收其中的txid事件
 */
public class BlockListenerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private Gateway gateway;
    private Network network;
    private StubBlockEventSource stubBlockEventSource;
    private final Peer peer1 = testUtils.newMockPeer("peer1");
    private final Peer peer2 = testUtils.newMockPeer("peer2");

    @BeforeEach
    public void beforeEach() throws Exception {
        //用户测试网络块事件
        stubBlockEventSource = new StubBlockEventSource(); // Must be before network is created
        gateway = testUtils.newGatewayBuilder().connect();
        network = gateway.getNetwork("mychannel");
    }

    @AfterEach
    public void afterEach() {
        stubBlockEventSource.close();
        gateway.close();
    }

    /**
     * 监听集合前后是否为同一个实例
     */
    @Test
    public void add_listener_returns_the_listener() {
        Consumer<BlockEvent> listener = blockEvent -> {};  //空事件

        Consumer<BlockEvent> result = network.addBlockListener(listener);

        assertThat(result).isSameAs(listener);
    }

    /**
     * 检测发送的事件能否被接收
     */
    @Test
    public void listener_receives_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());// 用于接收事件

        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener).accept(event);
    }

    /**
     * 同一事件加入后，立即删除，监听是否有变化
     * 其实就是，只有sendEvent之后，监听才会变化
     */
    @Test
    public void removed_listener_does_not_receive_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);

        network.addBlockListener(listener);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).accept(event);
    }

    /**
     * 同一个事件发送后，只有最先发送的被监听接收，后面的被忽略
     */
    @Test
    public void duplicate_events_ignored() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent duplicateEvent = testUtils.newMockBlockEvent(peer2, 1);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event);
        stubBlockEventSource.sendEvent(duplicateEvent);

        Mockito.verify(listener, Mockito.never()).accept(duplicateEvent);
    }

    /**
     * 每次传入不同的事件，发送后，依次都能被接收到
     */
    @Test
    public void listener_receives_events_in_order() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event1); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event3);
        stubBlockEventSource.sendEvent(event2);

        InOrder orderVerifier = Mockito.inOrder(listener);
        orderVerifier.verify(listener).accept(event1);
        orderVerifier.verify(listener).accept(event2);
        orderVerifier.verify(listener).accept(event3);
    }

    /**
     * 块号大的数据接收到后，块号小的就被忽略
     */
    @Test
    public void old_events_ignored() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2);
        stubBlockEventSource.sendEvent(event1);

        Mockito.verify(listener, Mockito.never()).accept(event1);
    }

    /**
     * 接收到旧的事件后，继续接收新的事件
     */
    @Test
    public void still_receive_new_events_after_ignoring_old_events() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);
        BlockEvent event3 = testUtils.newMockBlockEvent(peer1, 3);

        network.addBlockListener(listener);
        stubBlockEventSource.sendEvent(event2); // Prime the listener with an initial block number
        stubBlockEventSource.sendEvent(event1);
        stubBlockEventSource.sendEvent(event3);
        Mockito.verify(listener).accept(event3);
    }

    /**
     * listener接收到新的事件后，listener更新为接收到的
     * 始终保持一致
     * checkpointer一般保存的是块的信息
     * @throws IOException
     */
    @Test
    public void add_checkpoint_listener_returns_the_listener() throws IOException {
        Consumer<BlockEvent> listener = blockEvent -> {};
        Checkpointer checkpointer = new InMemoryCheckpointer();

        Consumer<BlockEvent> result = network.addBlockListener(checkpointer, listener);

        assertThat(result).isSameAs(listener);
    }

    /**
     * 通过checkpointer获取块信息，并监听
     * @throws IOException
     */
    @Test
    public void listener_with_new_checkpointer_receives_events() throws IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(checkpointer, listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener).accept(event);
    }

    /**
     * 利用缓存加入事件，然后删除该事件，然后sendEvent，事件不会被接收到
     * @throws IOException
     */
    @Test
    public void removed_checkpoint_listener_does_not_receive_events() throws IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(checkpointer, listener);
        network.removeBlockListener(listener);
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).accept(event);
    }

    /**
     * 校验linstner接收了几次某事件
     * @throws IOException
     */
    @Test
    public void listener_with_saved_checkpointer_resumes_from_previous_event() throws IOException {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        Checkpointer checkpointer = new InMemoryCheckpointer();
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(checkpointer, listener);
        stubBlockEventSource.sendEvent(event1);
        network.removeBlockListener(listener);  //该listener被删除
        stubBlockEventSource.sendEvent(event2); //此时接收不到信息
        network.addBlockListener(checkpointer, listener);  //加入listener
        //此时接收到event2
        stubBlockEventSource.sendEvent(event2); // Without checkpoint this will be ignored as a duplicate

        Mockito.verify(listener, Mockito.times(1)).accept(event2);  //总共只接收到1次event2
    }

    /**
     * 添加指定起始块号后的监听事件，实例是否保持一致
     */
    @Test
    public void add_replay_listener_returns_the_listener() {
        Consumer<BlockEvent> listener = blockEvent -> {};

        Consumer<BlockEvent> result = network.addBlockListener(1, listener);

        assertThat(result).isSameAs(listener);
    }

    /**
     * 指定块后，只接收最新的块
     */
    @Test
    public void replay_listener_receives_replay_events() {
        Consumer<BlockEvent> realtimeListener = event -> {};
        Consumer<BlockEvent> replayListener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event1 = testUtils.newMockBlockEvent(peer1, 1);
        BlockEvent event2 = testUtils.newMockBlockEvent(peer1, 2);

        network.addBlockListener(realtimeListener);
        stubBlockEventSource.sendEvent(event2); // Non-replay listeners will only receive later blocks
        network.addBlockListener(2, replayListener);
        stubBlockEventSource.sendEvent(event1); // Should be ignored
        stubBlockEventSource.sendEvent(event2); // Should be received

        Mockito.verify(replayListener, Mockito.only()).accept(event2);
    }

    /**
     * 关闭网络连接后，不能接收事件
     */
    @Test
    public void close_network_removes_listeners() {
        Consumer<BlockEvent> listener = Mockito.spy(testUtils.stubBlockListener());
        BlockEvent event = testUtils.newMockBlockEvent(peer1, 1);

        network.addBlockListener(listener);
        ((NetworkImpl)network).close();
        stubBlockEventSource.sendEvent(event);

        Mockito.verify(listener, Mockito.never()).accept(event);
    }
}
