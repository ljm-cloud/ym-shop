package bat.ke.qq.com.front.controller;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.common.pojo.Result;
import bat.ke.qq.com.common.utils.ResultUtil;
import bat.ke.qq.com.content.service.ContentService;
import bat.ke.qq.com.manager.dto.front.AllGoodsResult;
import bat.ke.qq.com.manager.dto.front.ProductDet;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.manager.pojo.TbSeckill;
import bat.ke.qq.com.search.service.RushBuySearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@Api(description = "商品页面展示")
public class RushBuyGoodsController {
    @Autowired
    private RushBuySearchService rushBuySearchService;
    @Autowired
    private ContentService contentService;
    @Autowired
    private JedisClient jedisClient;

    @RequestMapping(value = "/goods/rushBuyGoods",method = RequestMethod.GET)
    @ApiOperation(value = "获取抢购活动列表")
    public Result<AllGoodsResult> getNavList(){
        AllGoodsResult rushBuyResult = rushBuySearchService.searchRushBuyItemList();
        return new ResultUtil<AllGoodsResult>().setData(rushBuyResult);
    }

    @RequestMapping(value = "/goods/rushBuyDetails",method = RequestMethod.GET)
    @ApiOperation(value = "获取抢购活动详情")
    public Result<RushBuyItem> rushBuyDetails(Long promId){
        RushBuyItem item = rushBuySearchService.getRushBuyItem(promId);
        if (item==null){
            return new ResultUtil<RushBuyItem>().setErrorMsg("查询抢购活动信息异常");
        }
        return new ResultUtil<RushBuyItem>().setData(item);
    }
}
