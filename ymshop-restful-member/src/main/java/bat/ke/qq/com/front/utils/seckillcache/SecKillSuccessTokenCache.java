package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.UUID;

/**
 * 秒杀获取到了下单资格token缓存
 */
@Component
public class SecKillSuccessTokenCache {
    public static final String TOKEN_REDIS_KEY = "SECKILL_TOKEN:";
    // redis占位成功下单token
    public static final String SECKILL_SUCCESS_TOKEN = "SECKILL_SUCCESS_TOKEN_MOBILE:{0}_PROMID:{1}_";
    // redis占位成功下单token前缀
    public static final String SECKILL_SUCCESS_TOKEN_PREFIX = "SECKILL_SUCCESS_TOKEN_MOBILE:";

    @Autowired
    private JedisClient jedisClient;

    @Autowired
    private SecKillHandlingListCache miaoshaHandlingListCache;

    /**
     * 生成秒杀资格令牌  3分钟过期 (单测设置30分钟)
     *
     * @param userId
     * @param promId
     * @return
     */
    public String genToken(String userId, String promId) {
        String token = getToken();
        jedisClient.setex(getKey(userId, promId), token, 60 * 30);
        return token;
    }

    /**
     * 查询token
     *
     * @param userId
     * @param promId
     */
    public String queryToken(String userId, String promId) {
        String token = jedisClient.get(getKey(userId, promId));
        if (StringUtils.isNotEmpty(token)) {
            return token;
        }
        return StringUtils.EMPTY;
    }

    /**
     * 验证token
     *
     * @param token
     * @return false:token无效，true:token有效
     */
    public boolean validateToken(String userId, String promId, String token) {
        String key = getKey(userId, promId);
        if (token.equals(jedisClient.get(key))) {
            // 如果token验证成功   ant:优化token在提交订单时删除
//            jedisClient.del(key);
            return true;
        }
        return false;
    }

    /**
     * 删除token
     * @return false:token无效，true:token有效
     */
    public long delToken(String userId, String promId) {
        String key = getKey(userId, promId);
        return jedisClient.del(key);
    }

    protected String getKey(String userId, String promId) {
        String key = MessageFormat.format(SECKILL_SUCCESS_TOKEN,
                new Object[]{userId, promId});
        return key;
    }

    /**
     * 获取随机名称
     *
     * @return
     */
    public static String getToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
