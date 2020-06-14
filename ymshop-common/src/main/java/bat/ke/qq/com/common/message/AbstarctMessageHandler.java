package bat.ke.qq.com.common.message;

import bat.ke.qq.com.common.jedis.JedisClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;

import javax.annotation.PostConstruct;

public abstract class AbstarctMessageHandler<T> implements Runnable {
    private static Log logger = LogFactory.getLog(AbstarctMessageHandler.class);

    @Autowired
    protected MessageTrunk messageTrunk;

    /**
     * 初始化启动本监听器
     */
    @PostConstruct
    public void startListen() {
        // 启动监听
        new Thread(this).start();
    }

    @Autowired
    private JedisClient jedisClient;
    /**
     * 监听的消息类型
     */
    private String messageType;
    /**
     * 消息的class对象
     */
    private Class<T> clazz;

    private boolean monitor;

    // 默认为3
    private int retryTimes = 3;

    public AbstarctMessageHandler(String messageType, Class<T> clazz, int retryTimes) {
        this.messageType = messageType;
        this.clazz = clazz;
        this.retryTimes = 3;
    }

    public AbstarctMessageHandler(String messageType, Class<T> clazz) {
        this.messageType = messageType;
        this.clazz = clazz;
    }

    public void run() {
        while (true) {
            listen();
        }
    }
    /**
     * 监听消息
     */
    @SuppressWarnings("unchecked")
    public void listen() {
        // 阻塞获取redis列表值
        final Object obj = jedisClient.blpop(messageType, Integer.MAX_VALUE, Message.class);
        // 如果获取失败,意味着redis连接有问题
        if (obj == null) {
            monitor = false;
            logger.warn("消息分发器获取redis连接失败");
            // 暂停5秒钟,防止线程空跑,打印无数日志
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.warn("消息分发器线程暂停失败");
            }
            return;
        }

        if (!monitor) {
            logger.warn("消息分发开始");
            monitor = true;
        }

        try {
            messageTrunk.getThreadPool().submit(new Runnable() {
                public void run() {
                    Message message = (Message) obj;
                    try {
                        // 如果获取成功，则交给子类做业务处理
                        handle((T) message.getContent());
                    } catch (Exception ex) {
                        logger.error(ex);
                        // 处理失败，判断是否需要重试
                        if (message.getFailTimes().intValue() >= retryTimes) {
                            handleFailed((T) message.getContent());
                        } else {
                            message.setFailTimes(message.getFailTimes().intValue() + 1);
                            // 再次put回消息总线，等待下次重试
                            messageTrunk.put(message);
                            if (logger.isDebugEnabled()) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("msg:[").append(message).append("], 执行失败，准备重试。");
                                logger.debug(sb.toString());
                            }
                        }
                    }
                }
            });

        } catch (TaskRejectedException ex) {
            logger.warn("线程池已满，准备回写任务，暂停本线程");
            // 如果发生任务拒绝加入线程池，则回写任务到redis，等待重试
            messageTrunk.put((Message) obj);
            // 暂停指定时间
            try {
                Thread.sleep(messageTrunk.getThreadPoolFullSleepSeconds() * 1000);
            } catch (InterruptedException e) {
                logger.warn("生产者暂停异常", ex);
            }
        } catch (Exception ex) {
            logger.error("消息总线发生异常", ex);
        }
    }

    /**
     * 获取到消息后做业务处理
     */
    public abstract void handle(T obj);
    /**
     * 消息多次重试处理失败
     */
    public abstract void handleFailed(T obj);
}
