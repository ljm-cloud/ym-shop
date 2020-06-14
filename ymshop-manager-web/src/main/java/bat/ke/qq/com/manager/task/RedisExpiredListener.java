package bat.ke.qq.com.manager.task;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.manager.dto.OrderDetail;
import bat.ke.qq.com.manager.pojo.TbOrder;
import bat.ke.qq.com.manager.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPubSub;

/**
 * 订单自动取消监听器
 * 源码学院-真正的颜值担当 ANT 老师
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */
@Component
public class RedisExpiredListener extends JedisPubSub {
    private final static Logger log = LoggerFactory.getLogger(RedisExpiredListener.class);
    @Autowired
    private OrderService orderService;
    @Autowired
    private JedisClient jedisClient;

    private static final String SECKILL_SUCCESS_TOKEN = "SECKILL_SUCCESS_TOKEN_MOBILE:";

    //    public static final String SECKILL_SUCCESS_TOKEN = "SECKILL_SUCCESS_TOKEN_MOBILE:{0}_PROMID:{1}_";
    //订阅的频道
    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        if (log.isInfoEnabled()) {
            log.info("启动订单自动取消监听器");
        }
    }

    //订阅的信息
    @Override
    public void onPMessage(String pattern, String channel, String message) {
        if (message.startsWith(CommonConstant.ORDER_EXPIRE_KEY)) {//订单如果超时，查询数据库，如果state为超时未支付，则交易关闭
            String orderId = message.split(":")[1];
            OrderDetail orderDetail = orderService.getOrderDetail(orderId);
            if (orderDetail != null) {
                TbOrder tbOrder = orderDetail.getTbOrder();
                if (tbOrder != null && tbOrder.getStatus() == 0) {
//                    0、未付款，1、已付款，2、未发货，3、已发货，4、交易成功，5、交易关闭
                    orderService.cancelOrderByAdmin(orderId);
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("订单-" + orderId + "-不是待付款状态");
                    }
                }
            } else if (message.startsWith(CommonConstant.ORDER_EXPIRE_KEY)) {
                //token过期，回滚活动库存
                String promId = message.substring(message.lastIndexOf(":"), message.lastIndexOf("_"));
                jedisClient.incr(CommonConstant.SECKILL_STOCK_REDISKEY + promId);
            }
        }
    }
}

