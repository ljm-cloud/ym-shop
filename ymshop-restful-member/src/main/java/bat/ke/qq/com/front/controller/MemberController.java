package bat.ke.qq.com.front.controller;

import bat.ke.qq.com.annotation.IgnoreAuth;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.common.utils.CookieUtil;
import bat.ke.qq.com.intercepter.MemberUtils;
import bat.ke.qq.com.intercepter.TokenIntercepter;
import bat.ke.qq.com.manager.dto.front.CommonDto;
import bat.ke.qq.com.manager.dto.front.MemberLoginRegist;
import bat.ke.qq.com.common.pojo.Result;
import bat.ke.qq.com.common.utils.ResultUtil;
import bat.ke.qq.com.manager.dto.front.Member;
import bat.ke.qq.com.sso.dto.CaptchaCodeRequest;
import bat.ke.qq.com.sso.dto.CaptchaCodeResponse;
import bat.ke.qq.com.sso.service.CaptchaService;
import bat.ke.qq.com.sso.service.LoginService;
import bat.ke.qq.com.sso.service.MemberService;
import bat.ke.qq.com.sso.service.RegisterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author 源码学院
 */
@RestController
@Api(description = "会员注册登录")
public class MemberController {

    private final static Logger log= LoggerFactory.getLogger(MemberController.class);
    public static final String UUID_KEY = "captcha_uuid";
    //设置一个验证码开关  ，方便压测
    private boolean isValidateCaptcha = false;
    @Autowired
    private LoginService loginService;
    @Autowired
    private RegisterService registerService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private CaptchaService captchaService;

    @RequestMapping(value = "/member/geetestInit",method = RequestMethod.GET)
    @ApiOperation(value = "验证码初始化")
    @IgnoreAuth
    public Result<String> geetesrInit(HttpServletRequest request,HttpServletResponse response){
        CaptchaCodeRequest captchaCodeRequest=new CaptchaCodeRequest();
        //4567  -》图片
        CaptchaCodeResponse captchaCodeResponse=captchaService.getCaptchaCode(captchaCodeRequest);
        if(captchaCodeResponse!=null){
            Cookie cookie=CookieUtil.genCookie(UUID_KEY,captchaCodeResponse.getUuid(),"/",60);
            response.addCookie(cookie);
            return new ResultUtil<String>().setData(captchaCodeResponse.getImageCode());
        }
        return new ResultUtil<String>().setErrorMsg("获取验证码失败");
    }

    /**
     * 安全强化的验证码初始化   验证码与用户绑定、也支持指定扩展参数绑定
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/member/getStrongCaptcha",method = RequestMethod.GET)
    @ApiOperation(value = "安全强化的验证码初始化")
    public Result<String> getStrongCaptcha(@RequestParam(defaultValue = "")String extendKey, HttpServletRequest request,HttpServletResponse response){
        Long userId = MemberUtils.getUserId();
        CaptchaCodeRequest captchaCodeRequest=new CaptchaCodeRequest();
        captchaCodeRequest.setExtendKey(userId+"_"+extendKey);
        CaptchaCodeResponse captchaCodeResponse=captchaService.getCaptchaCode(captchaCodeRequest);
        if(captchaCodeResponse!=null){
            Cookie cookie=CookieUtil.genCookie(UUID_KEY,captchaCodeResponse.getUuid(),"/",60);
            response.addCookie(cookie);
            return new ResultUtil<String>().setData(captchaCodeResponse.getImageCode());
        }
        return new ResultUtil<String>().setErrorMsg("获取验证码失败");
    }

    @RequestMapping(value = "/member/login",method = RequestMethod.POST)
    @ApiOperation(value = "用户登录")
    @IgnoreAuth
        public Result<Member> login(@RequestBody MemberLoginRegist memberLoginRegist,
                                HttpServletRequest request, HttpServletResponse response){
        CaptchaCodeRequest captchaCodeRequest = new CaptchaCodeRequest();
        String uuid = CookieUtil.getCookieValue(request, "captcha_uuid");
        captchaCodeRequest.setCode(memberLoginRegist.getCaptcha());
        captchaCodeRequest.setUuid(uuid);
        boolean validate = true;
        if (isValidateCaptcha){
            validate =captchaService.validateCaptchaCode(captchaCodeRequest);
        }
        Member member=null;
        if(validate){
            member=loginService.userLogin(memberLoginRegist.getUserName(), memberLoginRegist.getUserPwd());
            // 登录成功写入cookies sn   uuid  redis
            Cookie cookie= CookieUtil.genCookie(TokenIntercepter.ACCESS_TOKEN,member.getToken(),"/",24*60*60);
            response.addCookie(cookie);
        }else {
           throw new YmshopException("请输入正确的验证码");
        }

        return new ResultUtil<Member>().setData(member);
    }

    @RequestMapping(value = "/member/checkLogin",method = RequestMethod.GET)
    @ApiOperation(value = "判断用户是否登录")
    @IgnoreAuth
    public Result<Member> checkLogin(@RequestParam(defaultValue = "") String token){
        Member member=loginService.getUserByToken(token);
        return new ResultUtil<Member>().setData(member);
    }

    @RequestMapping(value = "/member/loginOut",method = RequestMethod.GET)
    @ApiOperation(value = "退出登录")
    public Result<Object> logout(@RequestParam(defaultValue = "") String token){

        loginService.logout(token);
        return new ResultUtil<Object>().setData(null);
    }

    @RequestMapping(value = "/member/register",method = RequestMethod.POST)
    @ApiOperation(value = "用户注册")
    @IgnoreAuth
    public Result<Object> register(@RequestBody MemberLoginRegist memberLoginRegist,
                                   HttpServletRequest request){
        CaptchaCodeRequest captchaCodeRequest = new CaptchaCodeRequest();
        String uuid = CookieUtil.getCookieValue(request, UUID_KEY);
        captchaCodeRequest.setCode(memberLoginRegist.getCaptcha());
        captchaCodeRequest.setUuid(uuid);
        boolean validate = captchaService.validateCaptchaCode(captchaCodeRequest);

        int result=0;
        if(validate){
            result=registerService.register(memberLoginRegist.getUserName(), memberLoginRegist.getUserPwd());
            if(result==0){
                return new ResultUtil<Object>().setErrorMsg("该用户名已被注册");
            }else if(result==-1){
                return new ResultUtil<Object>().setErrorMsg("用户名密码不能为空");
            }
        }else{
            return new ResultUtil<Object>().setErrorMsg("验证码验证失败");
        }
        return new ResultUtil<Object>().setData(result);
    }

    @RequestMapping(value = "/member/imgaeUpload",method = RequestMethod.POST)
    @ApiOperation(value = "用户头像上传")
    public Result<Object> imgaeUpload(@RequestBody CommonDto common){

        String imgPath = memberService.imageUpload(common.getUserId(),common.getToken(),common.getImgData());
        return new ResultUtil<Object>().setData(imgPath);
    }
}
