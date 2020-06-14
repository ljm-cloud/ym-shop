package bat.ke.qq.com.manager.service.impl;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.constant.OrderStatusEnum;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.common.pojo.DataTablesResult;
import bat.ke.qq.com.manager.dto.OrderDetail;
import bat.ke.qq.com.manager.mapper.TbOrderItemMapper;
import bat.ke.qq.com.manager.mapper.TbOrderMapper;
import bat.ke.qq.com.manager.mapper.TbOrderShippingMapper;
import bat.ke.qq.com.manager.mapper.TbThanksMapper;
import bat.ke.qq.com.manager.pojo.*;
import bat.ke.qq.com.manager.service.OrderService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 源码学院
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper tbOrderMapper;
    @Autowired
    private TbOrderItemMapper tbOrderItemMapper;
    @Autowired
    private TbOrderShippingMapper tbOrderShippingMapper;
    @Autowired
    private TbThanksMapper tbThanksMapper;

    @Autowired
    private JedisClient jedisClient;

    @Autowired
    private EmailUtil emailUtil;

    @Override
    public DataTablesResult getOrderList(int draw, int start, int length, String search, String orderCol, String orderDir) {

        DataTablesResult result=new DataTablesResult();
        //分页
        PageHelper.startPage(start/length+1,length);
        List<TbOrder> list = tbOrderMapper.selectByMulti("%"+search+"%",orderCol,orderDir);
        PageInfo<TbOrder> pageInfo=new PageInfo<>(list);

        result.setRecordsFiltered((int)pageInfo.getTotal());
        result.setRecordsTotal(Math.toIntExact(cancelOrder()));

        result.setDraw(draw);
        result.setData(list);
        return result;
    }

    @Override
    public Long countOrder() {

        TbOrderExample example=new TbOrderExample();
        Long result=tbOrderMapper.countByExample(example);
        if(result==null){
            throw new YmshopException("统计订单数目失败");
        }
        return result;
    }

    @Override
    public OrderDetail getOrderDetail(String orderId) {

        OrderDetail orderDetail = new OrderDetail();
        TbOrder tbOrder = tbOrderMapper.selectByPrimaryKey(orderId);

        TbOrderItemExample example=new TbOrderItemExample();
        TbOrderItemExample.Criteria criteria= example.createCriteria();
        criteria.andOrderIdEqualTo(orderId);
        List<TbOrderItem> orderItemList = tbOrderItemMapper.selectByExample(example);

        TbOrderShipping tbOrderShipping=tbOrderShippingMapper.selectByPrimaryKey(orderId);

        orderDetail.setTbOrder(tbOrder);
        orderDetail.setTbOrderItem(orderItemList);
        orderDetail.setTbOrderShipping(tbOrderShipping);
        return orderDetail;
    }

    @Override
    public int deliver(String orderId, String shippingName, String shippingCode, BigDecimal postFee) {

        TbOrder o = tbOrderMapper.selectByPrimaryKey(orderId);
        o.setShippingName(shippingName);
        o.setShippingCode(shippingCode);
        o.setPostFee(postFee);
        o.setConsignTime(new Date());
        o.setUpdateTime(new Date());
        //之前忘记设置常量了 将就这样看吧 0、未付款，1、已付款，2、未发货，3、已发货，4、交易成功，5、交易关闭
        o.setStatus(3);
        tbOrderMapper.updateByPrimaryKey(o);
        return 1;
    }

    @Override
    public int remark(String orderId, String message) {

        TbOrder o = tbOrderMapper.selectByPrimaryKey(orderId);
        o.setBuyerMessage(message);
        o.setUpdateTime(new Date());
        tbOrderMapper.updateByPrimaryKey(o);
        return 1;
    }

    @Override
    public int cancelOrderByAdmin(String orderId) {
        TbOrder o = tbOrderMapper.selectByPrimaryKey(orderId);
        o.setCloseTime(new Date());
        o.setUpdateTime(new Date());
        //之前忘记设置常量了 将就这样看吧 0、未付款，1、已付款，2、未发货，3、已发货，4、交易成功，5、交易关闭
        o.setStatus(5);
        tbOrderMapper.updateByPrimaryKey(o);
        return 1;
    }

    @Override
    public int payedOrder(String orderId) {
        TbOrder tbOrder = tbOrderMapper.selectByPrimaryKey(orderId);
        tbOrder.setStatus(1);
        tbOrder.setUpdateTime(new Date());
        tbOrder.setPaymentTime(new Date());
        if (tbOrderMapper.updateByPrimaryKey(tbOrder) != 1) {
            throw new YmshopException("更新订单失败");
        }
        //清除redis超时取消订单的数据
        jedisClient.del(CommonConstant.ORDER_EXPIRE_KEY+tbOrder.getOrderId());
        //todo  通知物流系统，生成物流单 ...
        return 1;
    }

    @Override
    public int deleteOrder(String id) {

        if(tbOrderMapper.deleteByPrimaryKey(id)!=1){
            throw new YmshopException("删除订单数失败");
        }

        TbOrderItemExample example=new TbOrderItemExample();
        TbOrderItemExample.Criteria criteria= example.createCriteria();
        criteria.andOrderIdEqualTo(id);
        List<TbOrderItem> list =tbOrderItemMapper.selectByExample(example);
        for(TbOrderItem tbOrderItem:list){
            if(tbOrderItemMapper.deleteByPrimaryKey(tbOrderItem.getId())!=1){
                throw new YmshopException("删除订单商品失败");
            }
        }

        if(tbOrderShippingMapper.deleteByPrimaryKey(id)!=1){
            throw new YmshopException("删除物流失败");
        }
        return 1;
    }

    @Override
    public int cancelOrder() {

        TbOrderExample example=new TbOrderExample();
        List<TbOrder> list=tbOrderMapper.selectByExample(example);
        for(TbOrder tbOrder:list){
            judgeOrder(tbOrder);
        }
        return 1;
    }

    /**
     * 判断订单是否超时未支付
     */
    public String judgeOrder(TbOrder tbOrder){

        String result=null;
        if(tbOrder.getStatus()==0){
            //判断是否已超1天
            long diff=System.currentTimeMillis()-tbOrder.getCreateTime().getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if(days>=1){
                //设置失效
                tbOrder.setStatus(5);
                tbOrder.setCloseTime(new Date());
                if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                    throw new YmshopException("设置订单关闭失败");
                }
            }else {
                //返回到期时间
                long time=tbOrder.getCreateTime().getTime()+1000 * 60 * 60 * 24;
                result= String.valueOf(time);
            }
        }
        return result;
    }

    @Override
    public int passPay(String tokenName, String token, String id) {

        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(id)){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //展示支付
        TbThanks tbThanks=tbThanksMapper.selectByPrimaryKey(Integer.valueOf(id));
        if(tbThanks==null){
            return 0;
        }
        tbThanks.setState(1);
        if(tbThanksMapper.updateByPrimaryKey(tbThanks)!=1){
            return 0;
        }
        //修改订单状态
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        if(tbOrder!=null){
            tbOrder.setStatus(4);
            tbOrder.setEndTime(new Date());
            tbOrder.setUpdateTime(new Date());
            if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                return 0;
            }
        }
        //发送通知邮箱
        if(StringUtils.isNotBlank(tbThanks.getEmail())&&EmailUtil.checkEmail(tbThanks.getEmail())){
            String content="您的订单已支付成功";
//            emailUtil.sendEmailPayResult(tbThanks.getEmail(),"【YMSHOP商城】支付成功通知",content);
        }
        return 1;
    }

    @Override
    public int backPay(String tokenName, String token, String id) {

        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(id)){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //展示支付
        TbThanks tbThanks=tbThanksMapper.selectByPrimaryKey(Integer.valueOf(id));
        if(tbThanks==null){
            return 0;
        }
        tbThanks.setState(2);
        if(tbThanksMapper.updateByPrimaryKey(tbThanks)!=1){
            return 0;
        }
        //修改订单状态
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        if(tbOrder!=null){
            tbOrder.setStatus(6);
            tbOrder.setCloseTime(new Date());
            tbOrder.setUpdateTime(new Date());
            if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                return 0;
            }
        }
        //发送通知邮箱
        if(StringUtils.isNotBlank(tbThanks.getEmail())&& EmailUtil.checkEmail(tbThanks.getEmail())){
            String content="抱歉，由于您支付不起或其他原因，您的订单支付失败，请尝试重新支付！<br>Powered By XPay. bat.ke.qq.com Present.";
            emailUtil.sendEmailPayResult(tbThanks.getEmail(),"【YMSHOP商城】支付失败通知",content);
        }
        return 1;
    }

    @Override
    public int notShowPay(String tokenName, String token, String id) {

        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(id)){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //展示支付
        TbThanks tbThanks=tbThanksMapper.selectByPrimaryKey(Integer.valueOf(id));
        if(tbThanks==null){
            return 0;
        }
        tbThanks.setState(3);
        if(tbThanksMapper.updateByPrimaryKey(tbThanks)!=1){
            return 0;
        }
        //修改订单状态
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        if(tbOrder!=null){
            tbOrder.setStatus(4);
            tbOrder.setEndTime(new Date());
            tbOrder.setUpdateTime(new Date());
            if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                return 0;
            }
        }
        //发送通知邮箱
        if(StringUtils.isNotBlank(tbThanks.getEmail())&&EmailUtil.checkEmail(tbThanks.getEmail())){
            String content="您的订单已支付成功，十分感谢您的支付！<br>但由于您的支付金额过低或其他原因，将不会在支付名单中显示，敬请谅解！" +
                    "<br>Powered By XPay. bat.ke.qq.com Present.";
            emailUtil.sendEmailPayResult(tbThanks.getEmail(),"【YMSHOP商城】支付支付成功通知",content);
        }
        return 1;
    }

    @Override
    public int editPay(String tokenName, String token, TbThanks tbThanks) {
        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(tbThanks.getId().toString())){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //保存
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date=sdf.parse(tbThanks.getTime());
            tbThanks.setDate(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(tbThanksMapper.updateByPrimaryKey(tbThanks)!=1){
            return 0;
        }
        return 1;
    }

    @Override
    public int payDelNotNotify(String tokenName, String token, String id) {

        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(id)){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //获得支付
        TbThanks tbThanks=tbThanksMapper.selectByPrimaryKey(Integer.valueOf(id));
        if(tbThanks==null){
            return 0;
        }
        //删除支付
        if(tbThanksMapper.deleteByPrimaryKey(Integer.valueOf(id))!=1){
            return 0;
        }
        //修改订单状态
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        if(tbOrder!=null){
            tbOrder.setStatus(6);
            tbOrder.setCloseTime(new Date());
            tbOrder.setUpdateTime(new Date());
            if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                return 0;
            }
        }
        return 1;
    }

    @Override
    public int payDel(String tokenName, String token, String id) {

        //验证token
        if(StringUtils.isBlank(tokenName)||StringUtils.isBlank(tokenName)||StringUtils.isBlank(id)){
            return -1;
        }
        String value=jedisClient.get(tokenName);
        if(!value.equals(token)){
            return -1;
        }
        //获得支付
        TbThanks tbThanks=tbThanksMapper.selectByPrimaryKey(Integer.valueOf(id));
        if(tbThanks==null){
            return 0;
        }
        //删除支付
        if(tbThanksMapper.deleteByPrimaryKey(Integer.valueOf(id))!=1){
            return 0;
        }
        //修改订单状态
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        if(tbOrder!=null){
            tbOrder.setStatus(6);
            tbOrder.setCloseTime(new Date());
            tbOrder.setUpdateTime(new Date());
            if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                return 0;
            }
        }
        //发送通知邮箱
        if(StringUtils.isNotBlank(tbThanks.getEmail())&& EmailUtil.checkEmail(tbThanks.getEmail())){
            String content="抱歉，由于您支付不起或其他原因，您的订单支付失败，请尝试重新支付！<br>Powered By XPay. bat.ke.qq.com Present.";
            emailUtil.sendEmailPayResult(tbThanks.getEmail(),"【YMSHOP商城】支付失败通知",content);
        }
        return 1;
    }
}
