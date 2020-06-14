package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecKillStockCache {
    @Autowired
    private JedisClient jedisClient;

    private String getKey(String promId) {
        return CommonConstant.SECKILL_STOCK_REDISKEY + promId;
    }

    /**
     * 减redis库存
     */
    public boolean decrStore(String goodsRandomName) {
        String key = getKey(goodsRandomName);
        // 减redis库存
        if (jedisClient.decr(key) >= 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 加redis库存
     * @return
     * @category 加redis库存
     */
    public void incrStore(String goodsRandomName) {
        jedisClient.incr(getKey(goodsRandomName));
    }
}
