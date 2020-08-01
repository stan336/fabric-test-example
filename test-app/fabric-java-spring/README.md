# fabric-java-springboot

## 接口和性能测试
环境：http://localhost:8080

### 1. 生成钱包
#### 接口：
* url：/putWallet
* method: Post
* req:
    ```json
    {
        "walletName":"test",
        "mspId":"Org1MSP",
        "certificate":"-----BEGIN CERTIFICATE-----\nMIICKjCCAdCgAwIBAgIQYEUiWZa2R7Qn7VjhsxTxRDAKBggqhkjOPQQDAjBzMQsw\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eu\nb3JnMS5leGFtcGxlLmNvbTAeFw0yMDA4MDExMzI5MDBaFw0zMDA3MzAxMzI5MDBa\nMGwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1T\nYW4gRnJhbmNpc2NvMQ8wDQYDVQQLEwZjbGllbnQxHzAdBgNVBAMMFlVzZXIxQG9y\nZzEuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATLUNsFGVZk\n07cvM4EkzsUgKAnKAKhiV390hHQzPRV3cWkbqm4MeLecBD84u8rzHaGK3FbXrAjt\nsB8SsRUmLI4Xo00wSzAOBgNVHQ8BAf8EBAMCB4AwDAYDVR0TAQH/BAIwADArBgNV\nHSMEJDAigCC1JvTNIF9HhMBVm+aQ7cD9ZQUnq5xiiUmDnPzCAIykKTAKBggqhkjO\nPQQDAgNIADBFAiEA+KC0MGSRh8hOUkXMVxQ3oeVMQXHgwsPAqqiXEenk+6MCIBLd\nyaRSJluBnryyOkfTQGqAprm/UKOKcJwG6K6JZZ//\n-----END CERTIFICATE-----",
        "privateKey":"-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgBu5VYPEcNhqGx8rv\nPrX+xxGZ96nZobAkWMFCsGhnmJmhRANCAATLUNsFGVZk07cvM4EkzsUgKAnKAKhi\nV390hHQzPRV3cWkbqm4MeLecBD84u8rzHaGK3FbXrAjtsB8SsRUmLI4X\n-----END PRIVATE KEY-----"
    }
    ```
* resp:
    ```
    Org1MSP
    ```
 
#### 测试信息
结果：500进程
![生成钱包接口测试](img/1.PNG)

### 2. 查询用户历史修改记录
* url：/query
* method: Post
* req:
    ```json
    {
        "userName":"test",
        "channelName":"mychannel",
        "chainCode":"hospital_recordInfo",
        "fnc":"QueryHistoryRecord",
        "args":["110117"]
    }
    ```
* resp:
    ```json
    {
        "recordInfos": [{
            "identity": "110114",
            "sickName": "zhangsan",
            "drugName": ["110114", "110115"]
        }]
    }
    ```
#### 测试信息
结果：500进程
![查询用户历史修改记录接口测试](img/2.png)
  
### 3. 修改或添加某用户记录
* url：/invoke
* method: Post
* req:
    ```json
    {
        "userName": "test",
        "channelName": "mychannel",
        "chainCode": "hospital_recordInfo",
        "fnc": "Save",
        "args": ["110121","zhangsan2","[\"110114\",\"110115\"]","110121"]
    }
    ```
* resp:
    ```text
    若添加或修改成功，则返回空
    ```
#### 测试信息
结果：200进程 22秒。
设备能力有限，进程数若继续增加，docker将异常退出，以下是200进程测试结果：
![查询用户历史修改记录接口测试](img/3.png)
由此个人认为，如果并发量大，而设备有限，可以使用消息队列来处理写入请求。
