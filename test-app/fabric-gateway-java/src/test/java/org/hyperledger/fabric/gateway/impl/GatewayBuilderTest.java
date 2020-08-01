/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.gateway.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.stream.Collectors;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Credentials;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * gateway的builder测试，确保builder的正确和稳定
 */
public class GatewayBuilderTest {
    private static final Path CONFIG_PATH = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "gateway");
    private static final Path JSON_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.json");
    private static final Path YAML_NETWORK_CONFIG_PATH = CONFIG_PATH.resolve("connection.yaml");

    //org的admin配置
    private static final Path credentialPath = Paths.get("src", "test","organizations",
            "peerOrganizations", "org1.example.com", "users", "Admin@org1.example.com", "msp");
    private static final String PEM = "Admin@org1.example.com-cert.pem";
    private static final String MSP_ID = "Org1MSP";
    private static final String WALLET_ID = "admin";
    //认证信息加载
    private X509Credentials credentials = null;
    private Identity identity = null;
    private Gateway.Builder builder;
    private Wallet testWallet;



    @BeforeEach
    public void setup() throws IOException, CertificateException, InvalidKeyException {
        //证书和私钥
        //加载用户认证信息
        credentials = new X509Credentials(credentialPath.resolve(Paths.get("signcerts", PEM)),credentialPath.resolve(Paths.get("keystore", "priv_sk")));
        identity = Identities.newX509Identity(MSP_ID, credentials.getCertificate(),credentials.getPrivateKey());

        //网络启动
        builder = Gateway.createBuilder();
        builder.queryHandler(network -> (query -> null)); // Prevent failure if networks are created

        testWallet = Wallets.newInMemoryWallet();
        testWallet.put(WALLET_ID, identity);
    }

    /**
     * 不带有gateway信息，就是用户信息
     */
    @Test
    public void testBuilderNoOptions() {
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The gateway identity must be set");
    }

    /**
     * 测试没有网络配置时候是否有异常
     * @throws IOException
     */
    @Test
    public void testBuilderNoCcp() throws IOException {
        builder.identity(testWallet, WALLET_ID);  //加载对应id的wallet，该wallet必须已经存在
        assertThatThrownBy(() -> builder.connect())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The network configuration must be specified");
    }

    /**
     * 传入一个wallets中不存在的walletId，是否异常
     */
    @Test
    public void testBuilderInvalidIdentity() throws IOException {
        builder.identity(testWallet, "INVALID_IDENTITY");
        assertThatThrownBy(() -> builder.identity(testWallet, "INVALID_IDENTITY"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_IDENTITY");
    }

    /**
     * 校验使用yaml配置文件是否能正常启动网络
     * 建议在线将json转为yaml，手动写yaml容易出错且看不出来
     * @throws IOException
     */
    @Test
    public void testBuilderYamlCcp() throws IOException {
        builder.identity(testWallet, WALLET_ID)
                .networkConfig(YAML_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                    .map(Peer::getName)
                    .collect(Collectors.toList());
            assertThat(peerNames).containsExactly("peer0.org1.example.com");
        }
    }

    /**
     * 输入异常配置文件路径，检测结果
     * @throws IOException
     */
    @Test
    public void testBuilderInvalidCcp() throws IOException {
        builder.identity(testWallet, WALLET_ID);
        assertThatThrownBy(() -> builder.networkConfig(Paths.get("invalidPath")))
                .isInstanceOf(IOException.class);
    }

    /**
     * 检测json配置文件
     * @throws IOException
     */
    @Test
    public void testBuilderWithWalletIdentity() throws IOException {
        builder.identity(testWallet, WALLET_ID)
                .networkConfig(JSON_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            assertThat(gateway.getIdentity()).isEqualTo(identity);
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(credentials.getCertificatePem());
        }
    }

    /**
     * 字节流读取yaml配置文件
     * @throws IOException
     */
    @Test
    public void testYamlStreamNetworkConfig() throws IOException {
        try (InputStream configStream = new FileInputStream(YAML_NETWORK_CONFIG_PATH.toFile())) {
            builder.identity(testWallet, WALLET_ID)
                    .networkConfig(configStream);
            try (Gateway gateway = builder.connect()) {
                Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                        .map(Peer::getName)
                        .collect(Collectors.toList());
                assertThat(peerNames).containsExactly("peer0.org1.example.com");
            }
        }
    }

    /**
     * 字节流读取json配置文件
     * @throws IOException
     */
    @Test
    public void testJsonStreamNetworkConfig() throws IOException {
        try (InputStream configStream = new FileInputStream(JSON_NETWORK_CONFIG_PATH.toFile())) {
            builder.identity(testWallet, WALLET_ID)
                    .networkConfig(configStream);
            try (Gateway gateway = builder.connect()) {
                Collection<String> peerNames = gateway.getNetwork("mychannel").getChannel().getPeers().stream()
                        .map(Peer::getName)
                        .collect(Collectors.toList());
                assertThat(peerNames).containsExactly("peer0.org1.example.com");
            }
        }
    }

    /**
     * 使用一个无效的identity来执行网络
     * 正常时候identity是包含在wallet中的，当然，也可以直接使用来操作网络
     * @throws IOException
     */
    @Test
    public void testBuilderForUnsupportedIdentityType() throws IOException {
        Identity unsupportedIdentity = new Identity() {
            @Override
            public String getMspId() {
                return "mspId";
            }
        };
        assertThatThrownBy(() -> builder.identity(unsupportedIdentity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(unsupportedIdentity.getClass().getName());
    }

    /**
     * 不使用identity操作网络
     * @throws IOException
     */
    @Test
    public void testBuilderWithoutIdentity() throws IOException {
        assertThatThrownBy(() -> builder.identity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity must not be null");
    }

    /**
     * 使用identity操作网络,判断签名结果是否正确
     * @throws IOException
     */
    @Test
    public void testBuilderWithIdentity() throws IOException {
        builder.identity(identity)
                .networkConfig(JSON_NETWORK_CONFIG_PATH);
        try (Gateway gateway = builder.connect()) {
            assertThat(gateway.getIdentity()).isEqualTo(identity);
            HFClient client = ((GatewayImpl) gateway).getClient();
            assertThat(client.getUserContext().getEnrollment().getCert()).isEqualTo(credentials.getCertificatePem());
        }
    }

    /**
     * 使用配置文件生成builder是否为同一个builder
     * @throws IOException
     */
    @Test
    public void testFileNetworkConfigReturnsBuilder() throws IOException {
        Gateway.Builder result = builder.networkConfig(JSON_NETWORK_CONFIG_PATH);
        assertThat(result).isSameAs(builder);
    }

    /**
     * 使用字节流配置文件生成builder是否为同一个builder
     * @throws IOException
     */
    @Test
    public void testStreamNetworkConfigReturnsBuilder() throws IOException {
        try (InputStream configStream = new FileInputStream(JSON_NETWORK_CONFIG_PATH.toFile())) {
            Gateway.Builder result = builder.networkConfig(configStream);
            assertThat(result).isSameAs(builder);
        }
    }
}
