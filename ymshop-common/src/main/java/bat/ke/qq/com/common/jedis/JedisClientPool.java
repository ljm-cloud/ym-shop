package bat.ke.qq.com.common.jedis;

import bat.ke.qq.com.common.utils.ConvertUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 源码学院
 */
public class JedisClientPool implements JedisClient {
    private static final Log logger = LogFactory.getLog(JedisClientPool.class);
    private JedisPool jedisPool;

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public String set(String key, String value) {
        Jedis jedis = jedisPool.getResource();
        String result = jedis.set(key, value);
        jedis.close();
        return result;
    }

    @Override
    public String setex(String key, String value, int seconds) {
        Jedis jedis = jedisPool.getResource();
        String result = jedis.setex(key, seconds, value);
        jedis.close();
        return result;
    }

    @Override
    public String get(String key) {
        Jedis jedis = jedisPool.getResource();
        String result = jedis.get(key);
        jedis.close();
        return result;
    }

    @Override
    public Boolean exists(String key) {
        Jedis jedis = jedisPool.getResource();
        Boolean result = jedis.exists(key);
        jedis.close();
        return result;
    }

    @Override
    public Long expire(String key, int seconds) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.expire(key, seconds);
        jedis.close();
        return result;
    }

    @Override
    public Long ttl(String key) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.ttl(key);
        jedis.close();
        return result;
    }

    @Override
    public Long incr(String key) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.incr(key);
        jedis.close();
        return result;
    }

    @Override
    public Long decr(String key) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.decr(key);
        jedis.close();
        return result;
    }

    @Override
    public Long hset(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.hset(key, field, value);
        jedis.close();
        return result;
    }

    @Override
    public String hget(String key, String field) {
        Jedis jedis = jedisPool.getResource();
        String result = jedis.hget(key, field);
        jedis.close();
        return result;
    }

    @Override
    public Long hdel(String key, String... field) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.hdel(key, field);
        jedis.close();
        return result;
    }

    @Override
    public Boolean hexists(String key, String field) {
        Jedis jedis = jedisPool.getResource();
        Boolean result = jedis.hexists(key, field);
        jedis.close();
        return result;
    }

    @Override
    public List<String> hvals(String key) {
        Jedis jedis = jedisPool.getResource();
        List<String> result = jedis.hvals(key);
        jedis.close();
        return result;
    }

    @Override
    public Long del(String key) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.del(key);
        jedis.close();
        return result;
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        Jedis jedis = jedisPool.getResource();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                jedis.close();
            }
        }));

        jedis.psubscribe(jedisPubSub, patterns);
    }

    public <T> T blpop(String key, int waitSeconds, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            List<byte[]> values = jedis.brpop(waitSeconds, key.getBytes());

            if (values != null && values.size() > 0) {
                byte[] value = values.get(1);
                return ConvertUtil.unserialize(value, clazz);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("redis get data failed!", e);
            return null;
        } finally {
            jedis.close();
        }
    }

    public <T> Long rpush(String key, T value, int second) {
        Jedis jedis = null;
        Long ret = null;

        try {
            jedis = jedisPool.getResource();
            byte[] bytes = ConvertUtil.serialize(value);
            ret = jedis.rpush(key.getBytes(), new byte[][]{bytes});
            if (second > 0) {
                jedis.expire(key, second);
            }
        } catch (Exception var10) {
            logger.error("redis lpush data failed , key = " + key, var10);
        } finally {
            jedis.close();
        }
        return ret;
    }

    public Long llen(String key) {
        Jedis jedis = null;
        Object ret = null;

        try {
            jedis = jedisPool.getResource();
            Long var4 = jedis.llen(key);
            return var4;
        } catch (Exception var8) {
            logger.error("redis llen data failed , key = " + key, var8);
        } finally {
            jedis.close();
        }
        return (Long) ret;
    }

    /**
     * 从列表中后去元素
     *
     * @param key
     * @param clazz
     * @return
     * @category @author xiangyong.ding@weimob.com
     * @since 2017年3月30日 下午4:12:52
     */
    public <T> List<T> lrange(String key, int start, int end, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return ConvertUtil.unserialize(jedis.lrange(key.getBytes(), start, end), clazz);
        } catch (Exception e) {
            logger.error("redis lrange data failed , key = " + key, e);
        } finally {
            jedis.close();
        }
        return new ArrayList<T>();
    }

    /**
     * 向redis中存入列表
     *
     * @param key    键值
     * @param object 数据
     * @return
     */
    public <T> boolean lpush(String key, T object) {
        Long ret = lpush(key, object, 0);
        if (ret > 0) {
            return true;
        }
        return false;
    }

    /**
     * 存储REDIS队列 顺序存储,可设置过期时间，过期时间以秒为单位
     *
     * @param key    reids键名
     * @param value  键值
     * @param second 过期时间(秒)
     */
    public <T> Long lpush(String key, T value, int second) {
        Jedis jedis = null;
        Long ret = null;
        try {
            jedis = jedisPool.getResource();
            byte[] bytes = ConvertUtil.serialize(value);
            ret = jedis.lpush(key.getBytes(), bytes);

            if (second > 0) {
                jedis.expire(key, second);
            }
        } catch (Exception e) {
            logger.error("redis lpush data failed , key = " + key, e);
        } finally {
            jedis.close();
        }
        return ret;
    }
}
