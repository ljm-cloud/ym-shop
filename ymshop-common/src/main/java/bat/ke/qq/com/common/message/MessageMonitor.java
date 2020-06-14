package bat.ke.qq.com.common.message;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class MessageMonitor {
    @Autowired
    private JedisClient jedisClient;

    /**
     * 获取待处理消息个数
     * @param messageType
     * @return
     */
    public int getMessageLeft(String messageType){
        return jedisClient.llen(messageType).intValue();
    }
}
