package bat.ke.qq.com.search.service;

import bat.ke.qq.com.manager.dto.front.AllGoodsResult;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.manager.pojo.TbSeckill;

public interface RushBuySearchService {
    public AllGoodsResult searchRushBuyItemList();

    public RushBuyItem getRushBuyItem(long promId);
}
