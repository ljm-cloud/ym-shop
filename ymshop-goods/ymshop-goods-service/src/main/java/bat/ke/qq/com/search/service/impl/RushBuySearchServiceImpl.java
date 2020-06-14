package bat.ke.qq.com.search.service.impl;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.manager.dto.front.AllGoodsResult;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.manager.pojo.TbSeckill;
import bat.ke.qq.com.search.mapper.ItemMapper;
import bat.ke.qq.com.search.service.RushBuySearchService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RushBuySearchServiceImpl implements RushBuySearchService {
    private static final String RUSHBUY_LIST_KEY = "rusBuyList";
    private static final String RUSHBUY_DETAILS_KEY = "rusBuyDetails:";
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private JedisClient jedisClient;

    @Override
    public AllGoodsResult searchRushBuyItemList() {
        AllGoodsResult rushBuyResult = new AllGoodsResult();
//        List<RushBuyItem> rushBuyItemList=null;
        String rusBuyList = jedisClient.get(RUSHBUY_LIST_KEY);
        if (StringUtils.isEmpty(rusBuyList)) {
            List<RushBuyItem> rushBuyItemList = itemMapper.getRushBuyItemList();
            rushBuyResult = new AllGoodsResult();
            rushBuyResult.setData(rushBuyItemList);
        } else {
            rushBuyResult = JSONUtil.toBean(rusBuyList, AllGoodsResult.class);
        }


        if (rushBuyResult.getData() == null || rushBuyResult.getData().size() == 0) {
            jedisClient.setex(RUSHBUY_LIST_KEY, JSONUtil.toJsonStr(rushBuyResult), 20);
        } else {
            List<RushBuyItem> newList = new ArrayList<>();
            RushBuyItem item = null;
            for (Object obj : rushBuyResult.getData()) {
                if (obj instanceof JSONObject) {
                    item = ((JSONObject) obj).toBean(RushBuyItem.class);
                } else {
                    item = (RushBuyItem) obj;
                }
                String image = item.getProductImageBig();
                if (image != null && !"".equals(image)) {
                    String[] strings = image.split(",");
                    image = strings[0];
                } else {
                    image = "";
                }
                item.setProductImageBig(image);
                String seckill_stock_key = CommonConstant.SECKILL_STOCK_REDISKEY + item.getPromId();
                String stockStr = jedisClient.get(seckill_stock_key);
                long stock = Long.parseLong(stockStr == null ? "0" : stockStr);
                Date now = new Date();
                if (StringUtils.isEmpty(stockStr)) {
                    //redis缓存过期，可判断为活动已失效
                    item.setState("end");
                } else if (stock <= 0) {
                    item.setState("sellout");
                } else if (item.getStartDate() != null && item.getStartDate().after(now)) {
                    item.setState("nobegin");
                } else if (item.getEndDate() != null && item.getEndDate().after(now)) {
                    item.setState("ing");
                } else {
                    item.setState("end");
                }
                newList.add(item);
            }
            rushBuyResult.setData(newList);
            rushBuyResult.setTotal(rushBuyResult.getData().size());
            jedisClient.setex(RUSHBUY_LIST_KEY, JSONUtil.toJsonStr(rushBuyResult), 60 * 10);
        }
        return rushBuyResult;
    }

    @Override
    public RushBuyItem getRushBuyItem(long promId) {
        RushBuyItem item =null;

        String redisKey = RUSHBUY_DETAILS_KEY + promId;
        String redisStr = jedisClient.get(redisKey);
        if (StringUtils.isNotEmpty(redisStr)){
            item =JSONUtil.toBean(redisStr,RushBuyItem.class);
        }else {
            item = itemMapper.getRushBuyItem(promId);
        }
        if (item!=null){
            //图片处理
            String[] images=item.getProductImageBig().split(",");
            if(images!=null){
                item.setProductImageBig(images[0]);
                List<String> imageSmall=new ArrayList<>();
                for (String imgPath:images){
                    imageSmall.add(imgPath);
                }
                item.setProductImageSmall(imageSmall);
            }
            String seckill_stock_key = CommonConstant.SECKILL_STOCK_REDISKEY + item.getPromId();
            String stockStr = jedisClient.get(seckill_stock_key);
            long stock = Long.parseLong(stockStr == null ? "0" : stockStr);
            item.setStock(stock);
            Date now = new Date();
            if (StringUtils.isEmpty(stockStr)) {
                //redis缓存过期，可判断为活动已失效
                item.setState("end");
            } else if (stock <= 0) {
                item.setState("sellout");
            } else if (item.getStartDate() != null && item.getStartDate().after(now)) {
                item.setState("nobegin");
            } else if (item.getEndDate() != null && item.getEndDate().after(now)) {
                item.setState("ing");
            } else {
                item.setState("end");
            }
            jedisClient.setex(redisKey,JSONUtil.toJsonStr(item),60*60);
        }else {
            jedisClient.setex(redisKey,JSONUtil.toJsonStr(item),30);
        }
        return item;
    }
}
