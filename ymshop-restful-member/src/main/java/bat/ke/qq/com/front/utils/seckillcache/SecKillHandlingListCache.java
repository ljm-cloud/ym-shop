package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

/**
 * 秒杀正在处理请求列表
 */
@Component
public class SecKillHandlingListCache {
    // redis占位成功下单token前缀
//    public static final String SECKILL_SUCCESS_TOKEN_PREFIX = "SECKILL_SUCCESS_TOKEN";
    // 秒杀处理列表
    public static final String SECKILL_HANDLE_LIST = "SECKILL_HANDLE_LIST_PROMID:{0}";

    @Autowired
    private JedisClient jedisClient;

    /**
     * 增加到处理列表
     *
     * @param mobile
     * @param goodsRandomName
     */
    public void add2HanleList(String mobile, String goodsRandomName) {
        jedisClient.hset(getKey(goodsRandomName), mobile, mobile);
    }

    /**
     * 增加到处理列表
     *
     * @param mobile
     * @param goodsRandomName
     */
    public void removeFromHanleList(String mobile, String goodsRandomName) {
        jedisClient.hdel(getKey(goodsRandomName), mobile);
    }

    /**
     * 是否在正在处理列表中
     *
     * @param userId
     * @param promId
     */
    public boolean isInHanleList(String userId, String promId) {
        return jedisClient.hget(getKey(promId), userId) != null;
    }

    private String getKey(String promId) {
        return MessageFormat.format(SECKILL_HANDLE_LIST,
                new Object[]{promId});
    }

}
