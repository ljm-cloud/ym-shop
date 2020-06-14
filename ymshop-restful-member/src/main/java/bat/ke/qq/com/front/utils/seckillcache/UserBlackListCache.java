package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 黑名单缓存
 */
@Component
public class UserBlackListCache {
    public static  final String USER_BLACK_LIST = "USER_BLACK_LIST";
    @Autowired
    private JedisClient jedisClient;

    /**
     * 增加进入黑名单
     *
     * @param userId
     * @category 增加进入黑名单
     */
    public void addInto(String userId) {
        jedisClient.hset(USER_BLACK_LIST, userId, "");
    }

    /**
     * 是否在黑名单中
     *
     * @param userId
     */
    public boolean isIn(String userId) {
        return jedisClient.hget(USER_BLACK_LIST, userId) != null;
    }
}
