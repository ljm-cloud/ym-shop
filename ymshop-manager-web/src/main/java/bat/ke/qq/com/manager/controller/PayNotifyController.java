package bat.ke.qq.com.manager.controller;

import bat.ke.qq.com.pay.IAliPayService;
import bat.ke.qq.com.pay.IWeiXinPayService;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.manager.dto.OrderDetail;
import bat.ke.qq.com.manager.service.OrderService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@Api(description = "订单通知")
public class PayNotifyController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IAliPayService aliPayService;
    @Autowired
    private IWeiXinPayService weixinPayService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private JedisClient jedisClient;
    /***
     * 后台回调
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/pay/alipayNotify")
    @ResponseBody
    public String notify(HttpServletRequest request, HttpServletResponse response) {
        logger.info("支付完成");
        Map<String, String> params = new HashMap<String, String>();
        String  message = "success";
        // 取出所有参数是为了验证签名
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            params.put(parameterName, request.getParameter(parameterName));
        }
        //验证签名 校验签名
        String orderId=null;//商户订单号
        String tradeNo=null;//第三方流水号
        boolean signVerified = false;
        //回调的通知也要验签，保证安全性
        signVerified = aliPayService.checkSign(params);
        if (signVerified) {
            logger.info("支付宝验证签名成功！");
            // 若参数中的appid和填入的appid不相同，则为异常通知
            orderId = params.get("out_trade_no");//todo ant 幂等应该根据支付平台交易流水号 而不是商户订单号 trade_no
            tradeNo = params.get("trade_no");
            if (!keyIsAtomic(tradeNo)) {
                //幂等校验
                logger.info("我已经收到了，不需要在请求了");
                message = "success";
                return message;
            }
            //在数据库中查找订单号对应的订单，并将其金额与数据库中的金额对比，若对不上，也为异常通知
            try {
                String status = params.get("trade_status");
                if (status.equals("WAIT_BUYER_PAY")) { // 如果状态是正在等待用户付款
                    logger.info(orderId + "订单的状态正在等待用户付款");
                } else if (status.equals("TRADE_CLOSED")) { // 如果状态是未付款交易超时关闭，或支付完成后全额退款
                    logger.info(orderId + "订单的状态已经关闭");
                } else if (status.equals("TRADE_SUCCESS") || status.equals("TRADE_FINISHED")) { // 如果状态是已经支付成功
                    logger.info("(支付宝订单号:" + orderId + "付款成功)");

                    OrderDetail order = orderService.getOrderDetail(orderId);
                    //todo ant 待优化   记录交易记录表
                    if (order == null) {
                        throw new NullPointerException("根据订单号查询不到订单信息！");
                    }else if (order.getTbOrder().getStatus()!=0){
                        //ant 如果不是未支付的状态 则返回异常
                        //todo ant 此时判断是否重复支付，如果重复支付，需要发起退款
                        throw new NullPointerException("订单已支付");
                    }else{
                        logger.info("订单支付成功:", order.getTbOrder().getOrderId());
                        orderService.payedOrder(order.getTbOrder().getOrderId());
                        request.setAttribute("orderid", order.getTbOrder().getOrderId());
                    }
                }
            }catch(Exception e){
                //回滚
                keyIsRollBack(tradeNo);
            }
        } else { // 如果验证签名没有通过
            message =  "failed";
            logger.info("验证签名失败！");
        }
        return  message;
    }

    /***
     * 判断这个key是否已经被占用了
     * @param key
     * @return
     */
    public  boolean keyIsAtomic(String key){
        //redis
        Long num = jedisClient.incr(key);
        jedisClient.expire(key,60*60*24);
        return num>1?false:true;
    }
    /***
     * 判断这个key是否已经被占用了
     * @param key
     * @param key
     * @return
     */
    public  boolean keyIsRollBack(String key){
        long num=jedisClient.incr(key);
        return num>1?false:true;
    }
}
