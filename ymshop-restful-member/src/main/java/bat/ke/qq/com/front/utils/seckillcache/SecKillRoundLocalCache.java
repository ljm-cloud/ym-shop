package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.cache.LocalCache;
import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.jedis.JedisClient;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SecKillRoundLocalCache {
    private LocalCache cache = new LocalCache();

    @Autowired
    private JedisClient jedisClient;

    /**
     * 根据本地缓存的虚拟key进行轮询，获取当前请求执行的key
     *
     * @param promId
     * @return
     */
    public String roundCacheKey(String promId) {
        SecKillLocalCacheEntity cache = getRelationShipKeyCache(promId);
        return cache.getKeys().get(cache.reqCount.incrementAndGet() % cache.getKeys().size());
    }

    /**
     * 根据秒杀的promId查询是否有虚拟商品key 本地保存关联key  以及轮询次数
     *
     *
     * @param promId
     * @return
     */
    public SecKillLocalCacheEntity getRelationShipKeyCache(String promId) {
        SecKillLocalCacheEntity secKillLocalCacheEntity = null;
        Object cacheValue = cache.getValue(promId);
        if (cacheValue == null) {
            synchronized (promId) {
                cacheValue = cache.getValue(promId);
                if (cacheValue != null) {
                    secKillLocalCacheEntity = (SecKillLocalCacheEntity) cacheValue;
                } else {
                    secKillLocalCacheEntity = new SecKillLocalCacheEntity();
                    List<String> promIdRelastionKeys =
                            jedisClient.lrange(CommonConstant.SECKILL_VIRTUAL_STOCK_REDISKEY + promId,
                                    0,-1,String.class);
                    //查询redis没有虚拟key，直接存当前promId，保证一个活动id只查询一次redis
                    if (CollectionUtil.isEmpty(promIdRelastionKeys)) {
                        promIdRelastionKeys = new ArrayList<>();
                        promIdRelastionKeys.add(promId);
                    }
                    secKillLocalCacheEntity.setKeys(promIdRelastionKeys);
                    cache.put(promId, secKillLocalCacheEntity, 1000 * 60 * 10);
                }
            }
        } else {
            secKillLocalCacheEntity = (SecKillLocalCacheEntity) cacheValue;
        }
        return secKillLocalCacheEntity;
    }


    static class SecKillLocalCacheEntity implements Serializable {
        /**
         * 关联的虚拟key
         */
        private List<String> keys;
        /**
         * 请求次数
         */
        private AtomicInteger reqCount = new AtomicInteger(1);

        public List<String> getKeys() {
            return keys;
        }

        public void setKeys(List<String> keys) {
            this.keys = keys;
        }

        public AtomicInteger getReqCount() {
            return reqCount;
        }

        public void setReqCount(AtomicInteger reqCount) {
            this.reqCount = reqCount;
        }
    }
}
