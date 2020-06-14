package bat.ke.qq.com.front.service.impl;

import bat.ke.qq.com.common.constant.OrderSiteEnum;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.message.Message;
import bat.ke.qq.com.common.message.MessageTrunk;
import bat.ke.qq.com.common.message.MessageType;
import bat.ke.qq.com.common.pojo.Result;
import bat.ke.qq.com.common.utils.ResultUtil;
import bat.ke.qq.com.front.mq.message.SecKillRequestMessage;
import bat.ke.qq.com.front.utils.seckillcache.*;
import bat.ke.qq.com.front.service.SeckillService;
import bat.ke.qq.com.intercepter.MemberUtils;
import bat.ke.qq.com.manager.dto.ItemDto;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.manager.service.ItemService;
import bat.ke.qq.com.search.service.RushBuySearchService;
import bat.ke.qq.com.sso.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    private SecKillLimiter secKillLimiter;

    @Autowired
    private SecKillSuccessTokenCache secKillSuccessTokenCache;
    @Autowired
    private MessageTrunk messageTrunk;
    @Autowired
    private SecKillFinishCache secKillFinishCache;
    @Autowired
    private SecKillHandlingListCache secKillHandlingListCache;
    @Autowired
    private RushBuySearchService rushBuySearchService;
    @Autowired
    private CartService cartService;
    @Autowired
    private UserBlackListCache userBlackListCache;
    @Autowired
    private SecKillStockCache secKillStockCache;
    @Autowired
    private SecKillRoundLocalCache secKillRoundLocalCache;
    @Autowired
    private ItemService itemService;

    @Override
    public void seckill(String promId) {
        String userId = String.valueOf(MemberUtils.getUserId());
        //step1:是否有虚拟商品key
        String virtualId = secKillRoundLocalCache.roundCacheKey(promId);
        log.info("当前抢购Id:" + virtualId);
        //step2:先看抢购是否已经结束了

        if (secKillFinishCache.isFinish(String.valueOf(virtualId))) {
            throw new YmshopException("抢购已结束");
        }
        // step3:通过判断消息队列长度进行限流
        secKillLimiter.doLimit(promId, "啊呀，没挤进去");
        // 判断是否处理中(是否在处理列表中)
        if (secKillHandlingListCache.isInHanleList(userId, promId)) {

            // step4:判断是否处理中(是否在处理列表中)
            if (secKillHandlingListCache.isInHanleList(userId, promId)) {
                throw new YmshopException("您已经提交过抢购，如果抢购成功请下单，否则耐心等待哦...");
            }
            // 加入正在处理列表

            //step5:加入正在处理列表   待处理列表根据真实promId保存
            secKillHandlingListCache.add2HanleList(userId, promId);
            //step6:请求消息推入处理队列，结束
            Message message = new Message(MessageType.SECKILL_MESSAGE, new SecKillRequestMessage(userId, promId, virtualId));
            messageTrunk.put(message);
        }
    }

    @Override
    public int addSecKillCart(String promId,String token) {
        //先验证token
        String userId= String.valueOf(MemberUtils.getUserId());
        if (!secKillSuccessTokenCache.validateToken(userId, promId, token)) {
            throw new YmshopException("没有获得下单资格，不能下单哦");
        }
        //查询秒杀信息
        RushBuyItem rushBuyItem = rushBuySearchService.getRushBuyItem(Long.parseLong(promId));
        if(rushBuyItem==null){
            throw new YmshopException("秒杀活动信息为空");
        }
        return cartService.addCart(MemberUtils.getUserId(),
                rushBuyItem.getProductId(), 1, OrderSiteEnum.rushbuy.name(), promId);
    }

    @Override
    public int commonSecKill(String promId) {
        String userId = String.valueOf(MemberUtils.getUserId());
        // 先看抢购是否已经结束了
        if (secKillFinishCache.isFinish(String.valueOf(promId))) {
            throw new YmshopException("抢购已结束");
        }
        //判断是否购买过
        if (secKillHandlingListCache.isInHanleList(userId, promId))
        {
            throw new YmshopException("您已经提交过抢购，如果抢购成功请下单，否则耐心等待哦...");
        }

        if (userBlackListCache.isIn(userId)) {
            log.error("黑名单用户");
            return 0;
        }

        RushBuyItem rushBuyItem = rushBuySearchService.getRushBuyItem(Long.parseLong(promId));
        if(rushBuyItem==null){
            throw new YmshopException("秒杀活动信息为空");
        }

        //检查实际库存  假预留
        ItemDto itemById = itemService.getItemById(rushBuyItem.getProductId());
        if (itemById == null) {
            log.error("商品信息异常");
            return 0;
        } else if (itemById.getNum() <= 0) {
            log.error("商品库存不足");
            return 0;
        }

        if (!secKillStockCache.decrStore(promId)) {
            // 减秒杀库存失败
            throw new YmshopException("占redis名额失败，等待重试");
        }

        int result = cartService.addCart(MemberUtils.getUserId(),
                rushBuyItem.getProductId(), 1, OrderSiteEnum.rushbuy.name(), promId);
        // 记录购买记录
        secKillHandlingListCache.add2HanleList(userId, promId);
        return result;
    }
}
