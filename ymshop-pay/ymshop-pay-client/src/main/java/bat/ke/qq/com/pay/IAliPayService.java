package bat.ke.qq.com.pay;


import bat.ke.qq.com.pay.model.PayOrder;

import java.util.Map;

/**
 * 支付
 * 创建者 Monkey老师 245553999
 */
public interface IAliPayService {
	/**
	 * 支付宝二维码支付
	 * @Author  Monkey老师
	 */
	String aliPayQrCode(PayOrder payOrder);
	/**
	 * 支付宝网站支付
	 * @Author  Monkey老师
	 */
	String aliPayPc(PayOrder payOrder);



	/***
	 * 验证签名 回调
	 * @param params
	 * @return
	 */
	Boolean  checkSign(Map<String, String> params);
}
