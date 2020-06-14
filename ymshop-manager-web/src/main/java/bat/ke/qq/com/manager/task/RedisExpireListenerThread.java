package bat.ke.qq.com.manager.task;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 订单自动取消监听线程初始化
 * 源码学院-真正的颜值担当 ANT 老师
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */
@Component
@Scope("singleton")
public class RedisExpireListenerThread {
    private static String redisTopic = "__keyevent@0__:expired";
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private RedisExpiredListener redisExpiredListener;

    @PostConstruct
    public void init() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                jedisClient.psubscribe(redisExpiredListener, redisTopic);
            }
        };
        thread.setName("orderExpiredListener-thread");
        thread.setDaemon(true);
        thread.start();
    }
}
