package bat.ke.qq.com.front.mq.handler;

import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.message.AbstarctMessageHandler;
import bat.ke.qq.com.common.message.MessageType;
import bat.ke.qq.com.front.mq.message.SecKillRequestMessage;
import bat.ke.qq.com.front.utils.seckillcache.SecKillFinishCache;
import bat.ke.qq.com.front.utils.seckillcache.SecKillStockCache;
import bat.ke.qq.com.front.utils.seckillcache.SecKillSuccessTokenCache;
import bat.ke.qq.com.front.utils.seckillcache.UserBlackListCache;
import bat.ke.qq.com.manager.dto.ItemDto;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.manager.service.ItemService;
import bat.ke.qq.com.search.service.RushBuySearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

/**
 * Message消息的处理器
 */
@Service
public class SecKillRequestHandler extends AbstarctMessageHandler<SecKillRequestMessage> {
    private static Log logger = LogFactory.getLog(SecKillRequestHandler.class);

    @Autowired
    private SecKillStockCache secKillStockCache;
    @Autowired
    private SecKillSuccessTokenCache secKillSuccessTokenCache;
    @Autowired
    private SecKillFinishCache secKillFinishCache;
    @Autowired
    private UserBlackListCache userBlackListCache;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RushBuySearchService rushBuySearchService;

    public SecKillRequestHandler() {
        // 说明该handler监控的消息类型；失败重试次数设定为MAX_VALUE
        super(MessageType.SECKILL_MESSAGE, SecKillRequestMessage.class, Integer.MAX_VALUE);
    }

    /**
     * 监听到消息后处理方法
     */
    public void handle(SecKillRequestMessage message) {
        // 查看请求用户是否在黑名单中（黑名单用户为通过拦截器计算超过访问频次的用户）  实际业务中可对接分控系统检测当前用户是否存在风险
        if (userBlackListCache.isIn(message.getUserId())) {
            logger.error("黑名单用户");
            return;
        }
        // 先看抢购是否已经结束了
        if (secKillFinishCache.isFinish(message.getVirtualId())) {
            logger.error("抱歉，您来晚了，抢购已经结束了");
            return;
        }
        //活动时间判断
        RushBuyItem rushBuyItem = rushBuySearchService.getRushBuyItem(Long.parseLong(message.getPromId()));
        if (rushBuyItem == null) {
            logger.error("查询秒杀信息异常");
            return;
        }
        //检查实际库存  假预留
        ItemDto itemById = itemService.getItemById(rushBuyItem.getProductId());
        if (itemById == null) {
            logger.error("商品信息异常");
            return;
        } else if (itemById.getNum() <= 0) {
            logger.error("商品库存不足");
            return;
        }
        // 先减redis分割的库存 PromId、VirtualId不相等说明建立了分割库存
        if (!message.getPromId().equals(message.getVirtualId())
            &&!secKillStockCache.decrStore(message.getVirtualId())){
            throw new YmshopException("占redis名额失败，等待重试");
        }
        // 先减redis的真实库存  ps:分割后还要不要保留总库存，可以根据自己的实现调整，我为了方便openResty查询redis，所以保存了一份。
        if (!secKillStockCache.decrStore(message.getPromId())) {
            throw new YmshopException("占redis名额失败，等待重试");
        }
        // 减库存成功：生成下单token，并存入redis供前端获取
        String token = secKillSuccessTokenCache.genToken(message.getUserId(), message.getPromId());

        logger.info(MessageFormat.format("SecKillRequestHandler handle " +
                "userId:{0} genToken ()", new String[]{message.getUserId(), token}));
    }

    public void handleFailed(SecKillRequestMessage obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("msg:[").append(obj).append("], 超过失败次数，停止重试。");
        logger.warn(sb.toString());
    }
}
