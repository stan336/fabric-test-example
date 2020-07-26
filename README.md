# fabric-example
当前主要是一些整理和总结，以及为部分源码和脚本的中文注释
后续在逐步梳理该项目

除了官方自带的fabcar channelcode，还有额外加入的hospital病例记录channelcode

## 安装
GOPROXY=https://mirrors.aliyun.com/goproxy/
GO111MODULE=on

## 关键概念
1. fabric网络中，有两类channel，一类是orderer使用的系统channel，最高决策机构，用于块校验等；一类是业务组织自己创建的channel，用于管理成员
2. 一般一个联盟链中，只使用一个channel
3. 最新的块为当前的配置文件
4. 只有orderer的admin用户可以创建新的联盟
5. channel和联盟基本上一对一
6. AnchorPeers锚节点：跨组织通信需要用到；也可以用于更新普通channel
7. 每一个系统通道，有一个自己的创世块和链；每一个channel有自己的创世块和链
8. 初始创建联盟的时候，已经指定了哪些组织（对应证书）在哪个联盟内，某个组织创建channel后，另一个组织只要是这个联盟内的，并且提供了证书，就可以被加入到当前channel
9. 某组织创建了channel，但是并没有加入该channel，后续需要手动加入channel
10. 必须当前channel内大部分组织对新组织签名后，新组织才能加入channel
11. 获取channel信息：peer channel getinfo -c mychannel
12. orderer节点和普通节点不建议部署在一起，orderer节点权利非常大，如果出现意外，会影响普通节点
13. 每个组织下都有不同的peer
14. 账本分为两部分
    1. 区块链链条，类似存储账变记录，比如谁转给谁多少钱
    2. world state分布式数据库(counchdb、leveldb)，存储余额，比如，xx用户余额为yy元
    3. 无论哪一部分，都是分布式存储的，在多个节点里都存在副本，通过共识保证数据在各个节点上的一致性。
    4. 默认使用的leveldb，在网络部署前必须确认好使用哪种数据库，部署后不能再变
15. 智能合约，分为普通合约和系统合约
    1. 普通合约就是用户自己定义开发的合约，用于完成用户的业务需求；流程：安装、实例化、更新；普通智能合约是运行再docker里面的，而且此docker会自动生成和启动。
    2. 系统合约 运行在peer节点中(普通合约是在独立docker中运行)，而不是独立的容器中。所有系统的智能合约不符合普通合约的生命周期。
        1.  常见系统合约：
            1. Lifecycle System Chaincode(lscc)，普通合约的生命周期都会经过该lscc
            2.Query System Chaincode(qscc)，查询块上数据的合约
            3. Configuration System Chaincode(cscc)，比如更新channel配置，生成一个新的块
            4. Endorsement System Chaincode(escc) 背书合约
            5. Validation system chaincode(vscc)，用于验证的合约
    3. 策略中，要哪些节点背书，就需要预先给哪些节点都安装合约，不能只部分安装
    4. 链码上传后，才能在各个节点授权定义链码名称、版本、背书策略等。
16. 背书(endorsement)
    1. 背书就是在提交tx（交易信息）之前，进行签名的过程，上块的所有操作都需要进行背书
    2. 进行背书的节点叫做背书节点
    3. 背书的规则叫做背书策略
    4. 背书策略，也就是权限指定：
        1. 系统级别的策略，在创建系统channel时指定；自定义普通channel的策略也属于系统策略
        2. Chaincode级别的策略，在实例化阶段指定；指定后，调用合约时，必须先经过对应策略签名后，才能成功上链
        3. Key级别的策略（很少用）
    5. chaincode策略中角色：
        1. org1.admin：org1msp的admin
        2. org1.member：org1msp的任一成员（就是1 2 4的任意角色都可以）
        3. org1.client：org1msp的任一client，用于提交上块的用户
        4. org1.peer：org1msp的任一peer节点
    6. chaincode指定策略角色：OR('rg1.member','Org2.member')表示任意一个，  AND('rg1.member','Org2.member') 表示必须两个  OutOf('rg1.member','Org2.member')表示任意两个
17. Orderer service
    Orderer service对节点收到的tx进行排序，并通过共识算法分发给各个节点。
    Order admin可以维护联盟的信息，联盟的信息保存在orderer的system channel中。
    1. orderer service上块处理过程分为3个阶段：
        1. 客户端将请求发送给peer节点，peer节点进行背书，将背书后的提案返回给客户端
        2. 此阶段客户端将背书提案的tx提交给order节点，orderer会根据tx排序并打包成区块
        3. 此阶段将打包好的区块分发到连接到orderer的peer节点，并非所有的peer都必须链接到order，peer之间共享账本可以通过gossip(类似p2p内部共享)方式进行区块共享传递。每一个peer独立的对接收到区块做校验，来验证是否正确的背书，如果验证不通过，是不会更新world state。
    2. orderer service的实现
        1. solo模式：
            单节点此种方式适用于测试开发环境。因为此种方式无法提高系统的吞吐量，同时无法与orderer节点之间进行容错.只有一个orderer
        2. kafka模式
            kafka也是一种ctf容错的一种基于zk实现的leader-folower模式。kafka半去中心化
        3. 对于一个智能合约的开发者来说，orderer的实现是透明的。
18. fabric-ca
    1. 生产中，一般一个组织的ca使用一个独立服务器部署
    2. server数据存储支持db，ldap
    3. fabric-ca也可以配置中间ca，但如果有中间ca，则中间ca签发的证书是有效的，根ca签发的证书就是无效
    4. ca开启后，会生成一个sqlite的db文件，用工具打开后，可以看到一些表，主要留意`affiliations`和`users`这两张表
    5. 功能类似configtxgen
    6. 过程：
        1. 启动并生成证书
        2. 将证书加入(enroll)到对应节点
        3. 注册
19. chaincode:
    1. Init方法 初始化使用
    2. Invoke方法 上块操作使用，除了初始化，其余方法均需要通过invoke来访问
    3. `peer chaincode invoke`和`peer chaincode query`，两个命令最终都是调用合约的Invoke方法，之所以分成两个命令，是因为，前者修改块中数据，每次操作都需要背书策略相关，而后者只是查询，不需要背书
    4. 实例化合约时候，没有使用用户自定义节点背书，而是用的系统背书，也就是orderer节点。
    5. 合约升级，版本号变更
    6. GetHistoryForKey可以获取一条记录的历史修改内容，类似溯源
    7. 私有数据处理
        1. 针对的是，同一个channel中，同一个字段的值，指定某些组织可以查看，某些不可以查看，privateData
        2. 该功能使用的比较少，实例化前，文件设置好策略，定义好私有数据的使用权限，以便供后续合约中指定；实例化时，指定策略文件。编写合约时候，存储数据使用带有private标识的方法
        3. 知道有这个概念就行
    8. api总结
        1. GetFunctionAndParameters() (string,[]string) 获取方法名和参数
        2. GetState(key string)([]byte,error)  读取Key对应的值
        3. PutState(key string,value []byte) error  key中存入某值
        4. DelState(key string) error 删除某Key对应的值
        5. GetStateRange(startKey,endKey string) (StateQueryIteratorInterface,error) 获取某Key之间的数据
        6. GetQueryResult(query string)  传入一条sql来获取查询结果,主要是指couchdb的富查询
        7. GetStateByRangeWithPagination(startKey,endKey string,pageSize int32,bookmark string) (StateQueryIteratorInterface,*pb.QueryResponseMetadata,error)  分页查询
        8. GetQueryResultWithPagination(query string,pageSize int32,bookmark string) (StateQueryIteratorInterface,*pb.QueryResponseMetadata,error)  自行输入查询语句分页获取结果
        9. GetHistoryForKey(key string) (HistoryQueryIteratorInterface,error)  获取一条记录的历史修改记录
    9. 一个chaincode中可以包含多个智能合约，合约之间通过包名类名区分；第一个入口合约作为默认合约。也就是说，调用链码时，默认指定的是该合约，若要调用该链码中其余合约，需要显式指定。
    10. 一般一个链码中只有一个智能合约
    10. 链码命名空间
        1. 也就是链码的名称，注意和智能合约名称的区分，智能合约只是链码中的一部分。不同的链码，用于确保让自己在网络中隔离。
        2. 不同链码之间的通信是通过`invokeChaincode()`来交互的，交互时，这两个链码必须安装在相同节点上。
20. 事务上下文
    1. 在智能合约中，可以跨事务定义和维护用户变量；提供了Fabric API的访问，可以操作从查询或更新分类账到检索提交事务的应用程序的数字标识。
21. 交易处理器
    1. 类似过滤器，调用智能合约之前或者之后，统一处理一些行为。
    2. 一个合约内，每种类型的处理器智能添加一次，有3钟处理器：
        1. `beforeTransaction()`，前置处理器：在每个智能合约交易执行之前调用。该处理器通常用来改变交易使用的交易上下文。处理器可以访问所有 Fabric API；如，可以使用 getState() 和 putState()。如果处理器成功完成，使用更新后的上下文调用交易。如果处理器抛出异常，不会调用交易，智能合约失败并显示异常错误消息。
        2. `afterTransaction()`，后置处理器：在每个智能合约交易执行之后调用。处理器通常会对所有的交易执行通用的后置处理，同样可以访问所有的 Fabric API。如果处理器成功完成，则智能合约将按调用的交易确定完成。如果处理程序抛出异常，则交易将失败并显示异常错误消息。
        3. `UnknownFunction`，未知处理器：试图执行未在智能合约中定义的交易时被调用。通常，处理器将记录管理员后续处理的失败。处理器可以访问所有的 Fabric API。处理器应该通过抛出包含所需错误消息的异常来完成。如果未指定未知处理器，或者未引发异常，则存在合理的默认处理;智能合约将以未知交易错误消息失败。
22. 区块链链式结构：
    1. 数据world state使用的是couchdb或leveldb，而账本信息是链式存储，需要分析该结构
    2. payload中，由谁上块、背书

    
    

## 主要目录和文件说明：

1. test-network/organizations/cryptogen
各组织、Orderer生成证书的配置文件

2. test-network/organizations/ccp-generate.sh
用于根据证书生成对应的ccp，ccp的目的是给sdk等调用fabric使用

3. test-network/configtx/configtx.yaml
用于生成创世块的配置文件

4. test-network/system-genesis-block/genesis.block
使用configtxgen生成的创世块，该创世块是pb文件，可转成json来查看

5. test-network/configtx
是fabric网络启动的初始化配置文件，里面定义了有哪些联盟，每个联盟下有哪些组织，每个联盟有哪些channel
`configtx.yaml`的`Profiles`定义了初始化加入的联盟和channel，可仿照加入更多初始化的此类信息

5. test-network/docker/docker-compose-test-net.yaml
测试网络镜像

## 主要命令
1. 将protobuf转换为json，比如转换创世块:
`configtxlator proto_decode --input ./system-genesis-block/genesis.block --type common.Block`

## couchdb
1. 每一个peer都有一个couchdb，用于全量保存数据
