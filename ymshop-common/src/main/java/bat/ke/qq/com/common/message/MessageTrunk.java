package bat.ke.qq.com.common.message;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class MessageTrunk {
    @Autowired
    private JedisClient jedisClient;

    @Autowired
    @Qualifier(value = "messageTrunktaskExecutor")
    private ThreadPoolTaskExecutor threadPool;

    /**
     * 失败重试次数，超过此值则不再重试，默认3次
     */
    private int failRetryTimes = 3;

    /**
     * 如果线程池满了，生产者暂停的时间，单位：S
     */
    private int threadPoolFullSleepSeconds = 1;

    public ThreadPoolTaskExecutor getThreadPool()
    {
        return threadPool;
    }

    public void setThreadPool(ThreadPoolTaskExecutor threadPool)
    {
        this.threadPool = threadPool;
    }

    public int getFailRetryTimes()
    {
        return failRetryTimes;
    }

    public void setFailRetryTimes(int failRetryTimes)
    {
        this.failRetryTimes = failRetryTimes;
    }

    public int getThreadPoolFullSleepSeconds()
    {
        return threadPoolFullSleepSeconds;
    }

    public void setThreadPoolFullSleepSeconds(int threadPoolFullSleepSeconds)
    {
        this.threadPoolFullSleepSeconds = threadPoolFullSleepSeconds;
    }

    /**
     * 推送消息
     *
     * @param message
     */
    public void put(Message message)
    {
        jedisClient.rpush(message.getKey().toString(), message, 0);
    }
}
