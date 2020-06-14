package bat.ke.qq.com.common.jedis;

import java.util.ArrayList;
import java.util.List;

import bat.ke.qq.com.common.utils.ConvertUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

/**
 * @author 源码学院
 */
public class JedisClientCluster implements JedisClient {
	private static final Log logger = LogFactory.getLog(JedisClientPool.class);

	private JedisCluster jedisCluster;


	public JedisCluster getJedisCluster() {
		return jedisCluster;
	}

	public void setJedisCluster(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	@Override
	public String set(String key, String value) {
		return jedisCluster.set(key, value);
	}

	@Override
	public String setex(String key, String value, int seconds) {
		return jedisCluster.setex(key,seconds, value);
	}

	@Override
	public String get(String key) {
		return jedisCluster.get(key);
	}

	@Override
	public Boolean exists(String key) {
		return jedisCluster.exists(key);
	}

	@Override
	public Long expire(String key, int seconds) {
		return jedisCluster.expire(key, seconds);
	}

	@Override
	public Long ttl(String key) {
		return jedisCluster.ttl(key);
	}

	@Override
	public Long incr(String key) {
		return jedisCluster.incr(key);
	}

	@Override
	public Long decr(String key) {
		return jedisCluster.decr(key);
	}

	@Override
	public Long hset(String key, String field, String value) {
		return jedisCluster.hset(key, field, value);
	}

	@Override
	public String hget(String key, String field) {
		return jedisCluster.hget(key, field);
	}

	@Override
	public Long hdel(String key, String... field) {
		return jedisCluster.hdel(key, field);
	}

	@Override
	public Boolean hexists(String key, String field) {
		return jedisCluster.hexists(key, field);
	}

	@Override
	public List<String> hvals(String key) {
		return jedisCluster.hvals(key);
	}

	@Override
	public Long del(String key) {
		return jedisCluster.del(key);
	}

	@Override
	public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
		jedisCluster.psubscribe(jedisPubSub,patterns);
	}

	@Override
	public <T> T blpop(String key, int waitSeconds, Class<T> clazz) {
		try {
			List<byte[]> values = jedisCluster.brpop(waitSeconds, key.getBytes());
			if (values != null && values.size() > 0) {
				byte[] value = values.get(1);
				return ConvertUtil.unserialize(value, clazz);
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error("redis get data failed!", e);
			return null;
		}
	}

	@Override
	public <T> Long rpush(String key, T value, int second) {
		Long ret = null;
		try {
			byte[] bytes = ConvertUtil.serialize(value);
			ret = jedisCluster.rpush(key.getBytes(), new byte[][]{bytes});
			if (second > 0) {
				jedisCluster.expire(key, second);
			}
		} catch (Exception var10) {
			logger.error("redis lpush data failed , key = " + key, var10);
		}
		return ret;
	}

	@Override
	public Long llen(String key) {
		Object ret = null;
		try {
			Long var4 = jedisCluster.llen(key);
			return var4;
		} catch (Exception var8) {
			logger.error("redis llen data failed , key = " + key, var8);
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
		try {
			return ConvertUtil.unserialize(jedisCluster.lrange(key.getBytes(), start, end), clazz);
		} catch (Exception e) {
			logger.error("redis lrange data failed , key = " + key, e);
		}
		return new ArrayList<T>();
	}

	/**
	 * 向redis中存入列表
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

		Long ret = null;
		try {

			byte[] bytes = ConvertUtil.serialize(value);
			ret = jedisCluster.lpush(key.getBytes(), bytes);

			if (second > 0) {
				jedisCluster.expire(key, second);
			}
		} catch (Exception e) {
			logger.error("redis lpush data failed , key = " + key, e);
		}
		return ret;
	}

}
