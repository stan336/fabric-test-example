/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import org.hyperledger.fabric.gateway.TestUtils;
import org.hyperledger.fabric.gateway.spi.Checkpointer;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * 测试文件相关的读写
 * 文件检测点？
 * 一些块的缓存信息会写入其中，比如块号、tx、版本号等
 * 方便直接读取块号等信息
 */
public class FileCheckpointerTest {
    private static final TestUtils testUtils = TestUtils.getInstance();

    private void writeToFile(Path file, String text) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(file);
        writer.write(text);
        writer.flush();
        writer.close();
    }

    /**
     * 缓存文件生成checkpointer
     * @throws IOException
     */
    @Test
    public void checkpointer_for_file_without_checkpoint_data_throws() throws IOException {
        Path file = testUtils.createTempFile();
        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class);
    }

    /**
     * 创建的缓存文件不存在（只有输入数据的时候才会生成），看能否读取块号
     * @throws IOException
     */
    @Test
    public void checkpointer_for_missing_file_is_has_unset_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            long blockNumber = checkpointer.getBlockNumber();
            assertThat(blockNumber).isEqualTo(Checkpointer.UNSET_BLOCK_NUMBER);
        }
    }

    /**
     * 创建的缓存文件不存在（只有输入数据的时候才会生成），看能否读取到交易信息
     * @throws IOException
     */
    @Test
    public void checkpointer_for_missing_file_has_no_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            Set<String> transactionIds = checkpointer.getTransactionIds();
            assertThat(transactionIds).isEmpty();
        }
    }

    /**
     * 创建的缓存文件不存在（只有输入数据的时候才会生成），写入块号信息，校验读取
     * @throws IOException
     */
    @Test
    public void set_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        long expectedBlockNumber = 1L;
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.setBlockNumber(expectedBlockNumber);

        long actualBlockNumber = checkpointer.getBlockNumber();
        assertThat(actualBlockNumber).isEqualTo(expectedBlockNumber);
    }

    /**
     * 创建的缓存文件不存在（只有输入数据的时候才会生成），写入交易信息，校验读取
     * @throws IOException
     */
    @Test
    public void add_transaction() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        String transactionId = "tx1";
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.addTransactionId(transactionId);

        Set<String> transactionIds = checkpointer.getTransactionIds();
        assertThat(transactionIds).containsExactly(transactionId);
    }

    /**
     * 创建的缓存文件不存在（只有输入数据的时候才会生成），用add追加修改txid，看是否异常
     * @throws IOException
     */
    @Test
    public void get_transactions_does_not_allow_modification_of_internal_state() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);

        assertThatThrownBy(() -> checkpointer.getTransactionIds().add("tx1"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 输入新的内容，覆盖旧的内容
     * 先addTransactionId，然后setBlockNumber，则先前的addTransactionId内容就会消失
     * @throws IOException
     */
    @Test
    public void set_block_number_clears_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);

        checkpointer.addTransactionId("tx1");
        checkpointer.setBlockNumber(1L);

        Set<String> transactionIds = checkpointer.getTransactionIds();
        assertThat(transactionIds).isEmpty();
    }

    /**
     * 同一个文件生成后，若未关闭，下次再new的时候，会提示已锁定
     * 实际，new后，文件已被打开，只有close后，才能继续new
     * @throws IOException
     */
    @Test
    public void checkpointer_locks_file() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        new FileCheckpointer(file);

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File is already locked")
                .hasMessageContaining(file.toString());
    }

    /**
     * 同一个文件生成，然后关闭，下次再new的时候，会正确生成
     * 实际，new后，文件已被打开，只有close后，才能继续new
     * @throws IOException
     */
    @Test
    public void close_unlocks_file() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        Checkpointer checkpointer = new FileCheckpointer(file);
        checkpointer.close();

        assertThatCode(() -> new FileCheckpointer(file))
                .doesNotThrowAnyException();
    }

    /**
     * 写入块，校验blocknumer是否读取正确
     * @throws IOException
     */
    @Test
    public void persists_block_number() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        long expectedBlockNumber = 1L;

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            checkpointer.setBlockNumber(expectedBlockNumber);
        }

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            long actualBlockNumber = checkpointer.getBlockNumber();
            assertThat(actualBlockNumber).isEqualTo(expectedBlockNumber);
        }
    }

    /**
     * 写入块，校验tx是否读取正确
     * @throws IOException
     */
    @Test
    public void persists_transactions() throws IOException {
        Path file = testUtils.getUnusedFilePath();
        String transactionId = "tx1";

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            checkpointer.addTransactionId(transactionId);
        }

        try (Checkpointer checkpointer = new FileCheckpointer(file)) {
            Set<String> transactionIds = checkpointer.getTransactionIds();
            assertThat(transactionIds).containsExactly(transactionId);
        }
    }

    /**
     * 输入错误的json看是否解析正确
     * @throws IOException
     */
    @Test
    public void throws_on_malformed_json() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 1"); // Missing closing brace

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to parse")
                .hasMessageContaining(file.toString());
    }

    /**
     * json 格式正确，version 1正确，其余参数不再，因此会报Bad format of checkpoint data
     * version从1开始，必须先1
     * @throws IOException
     */
    @Test
    public void throws_on_invalid_checkpoint_data() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 1 }"); // Missing data
        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Bad format of checkpoint data")
                .hasMessageContaining(file.toString());
    }

    /**
     * json格式正确，version要从1开始
     * @throws IOException
     */
    @Test
    public void throws_on_unsupported_checkpoint_data_version() throws IOException {
        Path file = testUtils.createTempFile();
        writeToFile(file, "{ \"version\": 0 }");

        assertThatThrownBy(() -> new FileCheckpointer(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported checkpoint data version")
                .hasMessageContaining(file.toString());
    }
}
