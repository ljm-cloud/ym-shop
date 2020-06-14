package bat.ke.qq.com.common.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

/**
 * @author 源码学院
 */
public interface JedisClient {

	String set(String key, String value);

	String setex(String key, String value,int seconds);

	String get(String key);

	Boolean exists(String key);

	Long expire(String key, int seconds);

	Long ttl(String key);

	Long incr(String key);

	Long decr(String key);

	Long hset(String key, String field, String value);

	String hget(String key, String field);

	Long hdel(String key, String... field);

	Boolean hexists(String key, String field);

	List<String> hvals(String key);

	Long del(String key);

	void psubscribe(final JedisPubSub jedisPubSub, final String... patterns);

	<T> T blpop(String key, int waitSeconds, Class<T> clazz);

	<T> Long rpush(String key, T value, int second);

	Long llen(String key);

	<T> Long lpush(String key, T value, int second);

	<T> boolean lpush(String key, T object);

	<T> List<T> lrange(String key, int start, int end, Class<T> clazz);

}
