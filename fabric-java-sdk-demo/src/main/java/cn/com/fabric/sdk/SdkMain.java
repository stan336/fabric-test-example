package cn.com.fabric.sdk;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

//需要手动先导入organizations、mychannel.tx文件
public class SdkMain {

    private static final Logger log = LoggerFactory.getLogger(SdkMain.class);

    /**
     * 需根据需要动态改变，主要是根据需要切换org1或org2
     */
    //系统平台配置
    private static final int SPLIT_NUM = 5; // mac：5，windows：6

    //grps
    private static final String grps = "grpcs://localhost";  //根地址

    //org切换
    private static final String orgSign = "org1";  //org通用配置中的文件目录切换，参数可选：org1,org2
    private static final String orgPort = "7051";  //org 端口切换：7051 9051
    private static final String org = "Org1";   //可切换Org1、Org2
    private static final String orgMsp = "Org1MSP";  //可切换Org1MSP、Org2MSP
    private static final String account = "李伟"; //名称，任意
    private static final String name = "Admin"; //英文名称

    /**
     * 不需要任何改动
     */
    //org通用配置
    private static final String rootPath = SdkMain.class.getResource("/").toString().substring(SPLIT_NUM);
    private static final String peer = "peer0." + orgSign + ".example.com";
    //用户密钥和证书相关路径
    private static final String keyFolderPath = rootPath + "organizations/peerOrganizations/" + orgSign + ".example.com/users/Admin@" + orgSign + ".example.com/msp/keystore";
    private static final String keyFileName = "priv_sk";
    private static final String certFoldePath = rootPath + "organizations/peerOrganizations/" + orgSign + ".example.com/users/Admin@" + orgSign + ".example.com/msp/signcerts";
    private static final String certFileName = "Admin@" + orgSign + ".example.com-cert.pem";
    //orderer证书相关路径
    private static final String ordererPort = "7050";
    private static final String channelName = "mychannel";
    private static final String orderer = "orderer.example.com";
    private static final String tlsOrderFilePath = rootPath + "organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
    private static final String txfilePath = rootPath + "mychannel.tx";
    private static final String tlsPeerFilePath = rootPath + "organizations/peerOrganizations/" + orgSign + ".example.com/peers/peer0." + orgSign + ".example.com/msp/tlscacerts/tlsca." + orgSign + ".example.com-cert.pem";
    private static final String tlsPeerFilePathAddtion = rootPath + "organizations/peerOrganizations/" + orgSign + ".example.com/tlsca/tlsca." + orgSign + ".example.com-cert.pem";

    /**
     * 测试路径
     * @param args
     */
    /*public static void main(String[] args) {
        log.error(certFoldePath);
    }*/

    /**
     * 1. 创建并加入channel
     * 需要配置切换为org1
     */
    public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, TransactionException, ProposalException, org.bouncycastle.crypto.CryptoException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation(org);  //组织机构
        userContext.setMspId(orgMsp);  // msp
        userContext.setAccount(account);  //名称，任意
        userContext.setName(name);  //英文名称，
        Enrollment enrollment = UserUtils.getEnrollment(keyFolderPath, keyFileName, certFoldePath, certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        //成功创建channel
        //mychannel.tx 需要用命令执行
        Channel channelTmp = fabricClient.createChannel(channelName, fabricClient.getOrderer(orderer, grps + ordererPort, tlsOrderFilePath), txfilePath);
        //方式一：创建后，立即加入一个peer
        channelTmp.joinPeer(fabricClient.getPeer(peer, grps + orgPort, tlsPeerFilePath));

        //方式二
        //获取到channel后，将一个peer加入到该channel中
        //加入peer前，需先关联orderer
        Channel channel = fabricClient.getChannel(channelName);  //获取到已有的channel
        channel.addOrderer(fabricClient.getOrderer(orderer,grps + ordererPort,tlsOrderFilePath));
        channel.joinPeer(fabricClient.getPeer(peer,grps + orgPort,tlsPeerFilePath));
        channel.initialize();
    }

    //安装合约
    /*public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, TransactionException, ProposalException {

        List list = new ArrayList();
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org2");
        userContext.setMspId("Org2MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment = UserUtils.getEnrollment(keyFolderPath, keyFileName, certFoldePath, certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer0 = fabricClient.getPeer("peer0.org2.example.com", "grpcs://peer0.org2.example.com:9051", tlsPeerFilePathAddtion);
        Peer peer1 = fabricClient.getPeer("peer1.org2.example.com", "grpcs://peer1.org2.example.com:10051", tlsPeerFilePathAddtion);
        List<Peer> peers = new ArrayList<Peer>();
        peers.add(peer0);
        peers.add(peer1);
        fabricClient.installChaincode(TransactionRequest.Type.GO_LANG, "basicinfo", "2.0", "E:\\chaincode", "basicinfo", peers);
    }*/

    //合约实例化
   /* public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org1");
        userContext.setMspId("Org1MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment =  UserUtils.getEnrollment(keyFolderPath,keyFileName,certFoldePath,certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Orderer order = fabricClient.getOrderer("orderer.example.com","grpcs://orderer.example.com:7050",tlsOrderFilePath);
        String initArgs[] = {""};
        fabricClient.initChaincode("mychannel", TransactionRequest.Type.GO_LANG,"basicinfo","1.0",order,peer,"init",initArgs);
    }*/

    //合约升级
    /*public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException, ChaincodeEndorsementPolicyParseException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org1");
        userContext.setMspId("Org1MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment =  UserUtils.getEnrollment(keyFolderPath,keyFileName,certFoldePath,certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Orderer order = fabricClient.getOrderer("orderer.example.com","grpcs://orderer.example.com:7050",tlsOrderFilePath);
        String initArgs[] = {""};
        fabricClient.upgradeChaincode("mychannel", TransactionRequest.Type.GO_LANG,"basicinfo","2.0",order,peer,"init",initArgs);
    }*/

    //invoke 合约
    /* public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException, ChaincodeEndorsementPolicyParseException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org2");
        userContext.setMspId("Org2MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment =  UserUtils.getEnrollment(keyFolderPath,keyFileName,certFoldePath,certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer0 = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Peer peer1 = fabricClient.getPeer("peer0.org2.example.com","grpcs://peer0.org2.example.com:9051",tlsPeerFilePathAddtion);
        List<Peer> peers = new ArrayList<>();
        peers.add(peer0);
        peers.add(peer1);
        Orderer order = fabricClient.getOrderer("orderer.example.com","grpcs://orderer.example.com:7050",tlsOrderFilePath);
        String initArgs[] = {"110114","{\"name\":\"zhangsan\",\"identity\":\"110114\",\"mobile\":\"18910012222\"}"};
        fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,"basicinfo",order,peers,"save",initArgs);
    }*/

    //查询合约
   /*public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException, org.bouncycastle.crypto.CryptoException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org2");
        userContext.setMspId("Org2MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment =  UserUtils.getEnrollment(keyFolderPath,keyFileName,certFoldePath,certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer0 = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Peer peer1 = fabricClient.getPeer("peer0.org2.example.com","grpcs://peer0.org2.example.com:9051",tlsPeerFilePathAddtion);
        List<Peer> peers = new ArrayList<>();
        peers.add(peer0);
        peers.add(peer1);
        String initArgs[] = {"110114"};
        Map map =  fabricClient.queryChaincode(peers,"mychannel", TransactionRequest.Type.GO_LANG,"basicinfo","query",initArgs);
        System.out.println(map);
    }*/

    //注册用户 hqCZUStrRTAR
   /*public static void main(String[] args) throws Exception {
        FabricCAClient caClient = new FabricCAClient("http://192.168.70.43",null);
        UserContext register = new UserContext();
        register.setName("lihua");
        register.setAffiliation("org2");
        Enrollment enrollment = caClient.enroll("admin","adminpw");
        UserContext registar = new UserContext();
        registar.setName("admin");
        registar.setAffiliation("org2");
        registar.setEnrollment(enrollment);
       String secret =  caClient.register(registar,register);
       System.out.println(secret);
    }*/

    //注册用户查询合约
    /*public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException, org.bouncycastle.crypto.CryptoException, EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        FabricCAClient caClient = new FabricCAClient("http://192.168.70.43",null);
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org2");
        userContext.setMspId("Org2MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment = caClient.enroll("lihua","hqCZUStrRTAR");
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer0 = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Peer peer1 = fabricClient.getPeer("peer0.org2.example.com","grpcs://peer0.org2.example.com:9051",tlsPeerFilePathAddtion);
        List<Peer> peers = new ArrayList<>();
        peers.add(peer0);
        peers.add(peer1);
        String initArgs[] = {"110120"};
        Map map =  fabricClient.queryChaincode(peers,"mychannel", TransactionRequest.Type.GO_LANG,"basicinfo","query",initArgs);
        System.out.println(map);
    }*/


    //注册用户invoke合约
    /*public static void main(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, IOException, IllegalAccessException, InvalidArgumentException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, org.hyperledger.fabric.sdk.exception.CryptoException, ProposalException, TransactionException, ChaincodeEndorsementPolicyParseException, EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        FabricCAClient caClient = new FabricCAClient("http://192.168.70.43",null);
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org2");
        userContext.setMspId("Org2MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment = caClient.enroll("lihua","hqCZUStrRTAR");
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);
        Peer peer0 = fabricClient.getPeer("peer0.org1.example.com","grpcs://peer0.org1.example.com:7051",tlsPeerFilePath);
        Peer peer1 = fabricClient.getPeer("peer0.org2.example.com","grpcs://peer0.org2.example.com:9051",tlsPeerFilePathAddtion);
        List<Peer> peers = new ArrayList<>();
        peers.add(peer0);
        peers.add(peer1);
        Orderer order = fabricClient.getOrderer("orderer.example.com","grpcs://orderer.example.com:7050",tlsOrderFilePath);
        String initArgs[] = {"110120","{\"name\":\"zhangsan\",\"identity\":\"110120\",\"mobile\":\"18910012222\"}"};
        fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,"basicinfo",order,peers,"save",initArgs);
    }*/
}
