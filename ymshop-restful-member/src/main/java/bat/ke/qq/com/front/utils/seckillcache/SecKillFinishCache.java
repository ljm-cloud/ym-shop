package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static bat.ke.qq.com.common.constant.CommonConstant.SECKILL_STOCK_REDISKEY;

/**
 * 秒杀结束缓存
 * 源码学院-ANT
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */
@Component
public class SecKillFinishCache {
    @Autowired
    private JedisClient jedisClient;

    /**
     * 设定秒杀结束
     *
     * @param promId
     */
    public void setFinish(String promId) {
        jedisClient.set(getKey(promId), "");
    }

    /**
     * 指定商品秒杀是否结束
     * @return
     * @category 指定商品秒杀是否结束 true已结束   false未结束
     */
    public boolean isFinish(String promId) {
        return jedisClient.get(getKey(promId)) == null;
    }

    private String getKey(String promId) {
        String key = SECKILL_STOCK_REDISKEY + promId;
        return key;
    }
}
