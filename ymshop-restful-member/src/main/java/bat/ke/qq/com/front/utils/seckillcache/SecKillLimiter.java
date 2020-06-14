package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.common.message.MessageMonitor;
import bat.ke.qq.com.common.message.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

/**
 * 商品购买限流器
 */
@Component
public class SecKillLimiter extends CurrentLimiter<String> {

    private static final String STORE_BY_ID = "SECKILL_STORE_BY_PROMID_{0}";
	@Autowired
	private JedisClient jedisClient;

    @Autowired
    private MessageMonitor messageMonitor;

    @Override
    protected String getLimiterName(String promId) {
        String key = MessageFormat.format(STORE_BY_ID, new Object[]{promId});
        return key;
    }

    @Override
    protected int getLimit(String promId) {
        return Integer.parseInt(jedisClient.get(CommonConstant.SECKILL_STOCK_REDISKEY+promId));
    }

    @Override
    protected int getCurrentLimit() {
        return messageMonitor.getMessageLeft(MessageType.SECKILL_MESSAGE);
    }

}
