package bat.ke.qq.com.front.controller;

import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.intercepter.MemberUtils;
import bat.ke.qq.com.pay.IAliPayService;
import bat.ke.qq.com.pay.IWeiXinPayService;
import bat.ke.qq.com.common.pojo.Result;
import bat.ke.qq.com.common.utils.ResultUtil;
import bat.ke.qq.com.common.utils.ZxingUtils;
import bat.ke.qq.com.manager.dto.front.Order;
import bat.ke.qq.com.pay.model.PayOrder;
import bat.ke.qq.com.sso.service.OrderService;
import com.github.pagehelper.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Api(description = "支付API")
public class PayController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private OrderService orderService;
    @Autowired
    private IWeiXinPayService weixinPayService;
    @Autowired
    private IAliPayService aliPayService;
    @RequestMapping("/member/alipayQrcode")
    public Result<Object> alipayQrcode(Long orderId) {
        logger.info("支付宝二维码支付");
        PayOrder payOrder = getPayOrder(orderId);
        //todo ant 订单校验，可以把个支付渠道的通用处理逻辑抽出模板方法
        String qrCode  =  aliPayService.aliPayQrCode(payOrder);
        if (StringUtil.isEmpty(qrCode)){
            throw new YmshopException("调用支付宝网关出现异常");
        }
        return new ResultUtil<Object>().setData(qrCode);
    }

    private PayOrder getPayOrder(Long orderId) {

        Order order = orderService.getOrder(orderId, MemberUtils.getUserId());
        if (order == null) {
            throw new NullPointerException("根据订单号查询不到订单信息！");
        }
        PayOrder payOrder = new PayOrder();//支付order
        payOrder.setProductId(order.getGoodsList().get(0).getProductId().toString());
        payOrder.setSubject(order.getGoodsList().get(0).getProductName());
        payOrder.setOutTradeNo(orderId.toString());
        payOrder.setBody(order.getGoodsList().get(0).getProductName());
        payOrder.setTotalFee(String.valueOf(order.getOrderTotal().multiply(new BigDecimal(100))));
        return payOrder;
    }

    @RequestMapping("/member/alipayPc")
    public String alipayPc(Long orderId) {
        logger.info("支付宝支付");
        PayOrder payOrder = getPayOrder(orderId);
        String form  =  aliPayService.aliPayPc(payOrder);
        return form;
    }

    @RequestMapping(value = "/member/wxpay",method = RequestMethod.POST)
    @ApiOperation(value = "支付订单,对接支付宝、微信")
    public Result<Object> payOrderNew(Long orderId){
        logger.info("微信支付");
        String imgPath= "";
        PayOrder payOrder = getPayOrder(orderId);
//       Constants.QRCODE_PATH+ Constants.SF_FILE_SEPARATOR+order.getId()+".png";
        String urlCode  =  weixinPayService.weixinPay(payOrder);
        logger.info("urlCode:{},imgPath:{}",urlCode,imgPath);
        ZxingUtils.getQRCodeImge(urlCode, 256, imgPath);// 生成二维码

        return new ResultUtil<Object>().setData("../qrcode/"+orderId+".png");
    }
}
