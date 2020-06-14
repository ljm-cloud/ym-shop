package bat.ke.qq.com.common.constant;


/**
 * 常量
 * @author 源码学院
 */
public interface CommonConstant {

    /**
     * 限流标识
     */
    String LIMIT_ALL="YMSHOP_LIMIT_ALL";

    /**
     * 秒杀活动key前缀
     */
    String SECKILL_REDISKEY = "SECKILL_INFO:";
    /**
     * 秒杀活动库存key前缀
     */
    String SECKILL_STOCK_REDISKEY = "SECKILL_STOCK:";

    /**
     * 秒杀活动库存key前缀
     */
    String SECKILL_VIRTUAL_STOCK_REDISKEY = "SECKILL_VIRTUAL_RELATION:";

    /**
     * 订单过期标识
     */
    String ORDER_EXPIRE_KEY="ORDER_EXPIRE:";

}
