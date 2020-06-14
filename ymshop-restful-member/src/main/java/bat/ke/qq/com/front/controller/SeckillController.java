package bat.ke.qq.com.front.controller;

import bat.ke.qq.com.common.constant.OrderSiteEnum;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.pojo.Result;
import bat.ke.qq.com.common.utils.AssertUtil;
import bat.ke.qq.com.common.utils.CookieUtil;
import bat.ke.qq.com.common.utils.ResultUtil;
import bat.ke.qq.com.front.common.AddSecKillCartRequest;
import bat.ke.qq.com.front.common.SecKillRequest;
import bat.ke.qq.com.front.service.SeckillService;
import bat.ke.qq.com.front.utils.seckillcache.SecKillSuccessTokenCache;
import bat.ke.qq.com.intercepter.MemberUtils;
import bat.ke.qq.com.manager.dto.front.Cart;
import bat.ke.qq.com.manager.dto.front.Member;
import bat.ke.qq.com.manager.dto.front.RushBuyItem;
import bat.ke.qq.com.search.service.RushBuySearchService;
import bat.ke.qq.com.sso.dto.CaptchaCodeRequest;
import bat.ke.qq.com.sso.service.CaptchaService;
import bat.ke.qq.com.sso.service.CartService;
import bat.ke.qq.com.sso.service.OrderService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@Api(description = "秒杀")
public class SeckillController {

    //设置一个验证码开关  ，方便压测
    private boolean isValidateCaptcha = false;

    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private SecKillSuccessTokenCache secKillSuccessTokenCache;

//    @HystrixCommand(
//            commandKey = "secKill",
//            commandProperties = {
//                    @HystrixProperty(name = "execution.isolation.strategy", value = "SEMAPHORE"),
//                    @HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "30")
//            },
//            fallbackMethod = "secKillFallbackMethod"
//    )
    @RequestMapping(value = "/member/secKill", method = RequestMethod.POST)
    @ApiOperation(value = "发起秒杀")
    public Result<Object> seckill(@Valid @RequestBody SecKillRequest secKillRequest, HttpServletRequest request) {
        if (isValidateCaptcha) {
            String uuid = MemberUtils.getUserId() + "_" + secKillRequest.getPromId() + CookieUtil.getCookieValue(request, MemberController.UUID_KEY);
            CaptchaCodeRequest captchaCodeRequest = new CaptchaCodeRequest();
            captchaCodeRequest.setUuid(uuid);
            captchaCodeRequest.setCode(secKillRequest.getCaptcha());
            if (!captchaService.validateCaptchaCode(captchaCodeRequest)) {
                return new ResultUtil<>().setErrorMsg(0, "验证码验证失败");
            }
        }
        seckillService.seckill(secKillRequest.getPromId());
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/member/querySecKillResult", method = RequestMethod.GET)
    @ApiOperation(value = "查询排队结果")
    public Result<Object> querySecKillResult(String promId) {
        AssertUtil.notNull(promId, "活动Id不能为空");
        // 直接取缓存查询是否有成功的记录生成

        String token = secKillSuccessTokenCache.queryToken(
                String.valueOf(MemberUtils.getUserId()), promId);
        if(StringUtils.isEmpty(token)){
            return new ResultUtil<Object>().setErrorMsg("排队中，请稍后重试。");
        }
        return new ResultUtil<Object>().setData(token);
    }

    @RequestMapping(value = "/member/addSecKillCart", method = RequestMethod.POST)
    @ApiOperation(value = "添加秒杀商品到秒杀购物车")
    public Result<Object> addSecKillCart(@Valid @RequestBody AddSecKillCartRequest request) {
        int result = seckillService.addSecKillCart(request.getPromId(), request.getToken());
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/member/commonSecKill", method = RequestMethod.POST)
    @ApiOperation(value = "无异步秒杀流程")
    public Result<Object> commonSecKill(@Valid @RequestBody AddSecKillCartRequest request) {
        int result = seckillService.commonSecKill(request.getPromId());
        return new ResultUtil<Object>().setData(result);
    }

    /**
     * 秒杀接口限流回调方法
     * @return
     */
    public Result<Object> secKillFallbackMethod(){
        return new ResultUtil<>().setErrorMsg("排队失败");
    }
}
