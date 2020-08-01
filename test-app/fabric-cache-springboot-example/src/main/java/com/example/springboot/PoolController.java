package com.example.springboot;

import com.example.springboot.entity.ReqInvoke;
import com.example.springboot.entity.ReqQuery;
import com.example.springboot.entity.ReqWallet;
import com.example.springboot.utils.FabricCacheUtil;
import com.jason.fabric.pool.FabricGatewayPool;
import com.jason.fabric.pool.api.FabricConnection;
import com.jason.fabric.pool.utils.WalletUtil;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;

@RestController
public class PoolController {
	private static final Logger logger = LoggerFactory.getLogger(PoolController.class);

	/**
	 * 1. 添加一个钱包
	 * @return
	 */
	@RequestMapping("/putWallet")
	public String  putWallet(@RequestBody ReqWallet reqWallet){
		String certificate = "-----BEGIN CERTIFICATE-----\n" +
				"MIICKjCCAdCgAwIBAgIQYEUiWZa2R7Qn7VjhsxTxRDAKBggqhkjOPQQDAjBzMQsw\n" +
				"CQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\n" +
				"YW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eu\n" +
				"b3JnMS5leGFtcGxlLmNvbTAeFw0yMDA4MDExMzI5MDBaFw0zMDA3MzAxMzI5MDBa\n" +
				"MGwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1T\n" +
				"YW4gRnJhbmNpc2NvMQ8wDQYDVQQLEwZjbGllbnQxHzAdBgNVBAMMFlVzZXIxQG9y\n" +
				"ZzEuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATLUNsFGVZk\n" +
				"07cvM4EkzsUgKAnKAKhiV390hHQzPRV3cWkbqm4MeLecBD84u8rzHaGK3FbXrAjt\n" +
				"sB8SsRUmLI4Xo00wSzAOBgNVHQ8BAf8EBAMCB4AwDAYDVR0TAQH/BAIwADArBgNV\n" +
				"HSMEJDAigCC1JvTNIF9HhMBVm+aQ7cD9ZQUnq5xiiUmDnPzCAIykKTAKBggqhkjO\n" +
				"PQQDAgNIADBFAiEA+KC0MGSRh8hOUkXMVxQ3oeVMQXHgwsPAqqiXEenk+6MCIBLd\n" +
				"yaRSJluBnryyOkfTQGqAprm/UKOKcJwG6K6JZZ//\n" +
				"-----END CERTIFICATE-----";
		String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
				"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgBu5VYPEcNhqGx8rv\n" +
				"PrX+xxGZ96nZobAkWMFCsGhnmJmhRANCAATLUNsFGVZk07cvM4EkzsUgKAnKAKhi\n" +
				"V390hHQzPRV3cWkbqm4MeLecBD84u8rzHaGK3FbXrAjtsB8SsRUmLI4X\n" +
				"-----END PRIVATE KEY-----";
		try {
			return WalletUtil.getInstance().putWallet(reqWallet.getWalletName(),reqWallet.getMspId(),reqWallet.getCertificate(),reqWallet.getPrivateKey()).getMspId();
		} catch (IOException | InvalidKeyException | CertificateException e) {
			return "钱包添加异常！"+e.getMessage();
		}
	}

	/**
	 * 2. 查询
	 * 请求参数例：
	 * {
	 *     "userName":"test",
	 *     "channelName":"mychannel",
	 *     "chainCode":"hospital_recordInfo",
	 *     "fnc":"QueryHistoryRecord",
	 *     "args":["110121"]
	 * }
	 * @return
	 */
	@RequestMapping("/query")
	public String query(@RequestBody ReqQuery reqQuery) {
		try {
			FabricConnection connection = FabricCacheUtil.getConnect(reqQuery.getUserName(), reqQuery.getChannelName());
			return connection.query(reqQuery.getChainCode(), reqQuery.getFnc(),reqQuery.getArgs());
		} catch (Exception e) {
			return "获取失败！"+e.getMessage();
		}
	}

	/**
	 * 3. 修改
	 * @return
	 */
	@RequestMapping("/invoke")
	public String invoke(@RequestBody ReqInvoke reqInvoke) {
		try {
			FabricConnection connection = FabricCacheUtil.getConnect(reqInvoke.getUserName(), reqInvoke.getChannelName());
			return connection.invoke(reqInvoke.getChainCode(), reqInvoke.getFnc(),reqInvoke.getArgs());
		} catch (Exception e) {
			return "获取异常！"+e.getMessage();
		}
	}
}