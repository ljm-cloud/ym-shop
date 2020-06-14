package bat.ke.qq.com.front.interceptor;

import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.front.utils.seckillcache.UserBlackListCache;
import bat.ke.qq.com.intercepter.MemberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 恶意用户检测拦截器
 *
 * @category @author xiangyong.ding@weimob.com
 * @since 2017年3月26日 下午3:41:21
 */
@Component
public class UserInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JedisClient jedisClient;

    @Autowired
    private UserBlackListCache userBlackListCache;

    /**
     * 检测恶意用户，多少秒被出现多少次请求
     */
    @Value("${user_black_count}")
    private int userBlackCount;

    /**
     * 检测恶意用户，多少秒被出现多少次请求
     */
    @Value("${user_black_seconds}")
    private int userBlackSeconds;

//    @Autowired
//    private SystemConfig systemConfig;

    private static final String USER_REQUEST_TIMES_PREFIX = "user_request_times_";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 1.用户id
        String userId = MemberUtils.getUserId().toString();

        // 2. 验证用户是否在黑名单中
        if (userBlackListCache.isIn(userId)) {
            throw new YmshopException("抢购已经结束啦");
        }
        // 查询该用户访问记录
        List<UserRequestRecord> requestRecords = jedisClient.lrange(USER_REQUEST_TIMES_PREFIX + userId, 0,
                userBlackCount, UserRequestRecord.class);

        // 超过限定时间内的访问频率
        if (requestRecords.size() + 1 >= userBlackCount && (System.currentTimeMillis()
                - requestRecords.get(requestRecords.size() - 1).timestamp < userBlackSeconds )) {
            // 模拟加入IP黑名单，实际应用时这里要优化入库，下次重启服务时重新加载
            userBlackListCache.addInto(userId);
            // 清空访问记录缓存
            jedisClient.del(USER_REQUEST_TIMES_PREFIX + userId);
            throw new YmshopException("抢购已经结束啦");
        } else {
            UserRequestRecord requestRecord = new UserRequestRecord();
            requestRecord.setMobile(userId);
            requestRecord.setTimestamp(System.currentTimeMillis());
            // 如果第一次设定访问次数，则增加过期时间
            jedisClient.lpush(USER_REQUEST_TIMES_PREFIX + userId, requestRecord);
        }
        return true;
    }

    /**
     * 用户访问记录
     */
    public static class UserRequestRecord {
        /**
         * 手机号，唯一标志用户身份
         */
        private String mobile;

        /**
         * 时间戳
         */
        private long timestamp;

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("UserRequestRecord [mobile=");
            builder.append(mobile);
            builder.append(", timestamp=");
            builder.append(timestamp);
            builder.append("]");
            return builder.toString();
        }

    }
}
