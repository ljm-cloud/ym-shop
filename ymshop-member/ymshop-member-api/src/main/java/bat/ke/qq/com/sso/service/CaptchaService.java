package bat.ke.qq.com.sso.service;


import bat.ke.qq.com.sso.dto.CaptchaCodeRequest;
import bat.ke.qq.com.sso.dto.CaptchaCodeResponse;

public interface CaptchaService {
    public static final String CAPTCHA_UUID = "CAPTCHA_UUID:";

    /**
     * 获取图形验证码
     * @param request
     * @return
     */
    CaptchaCodeResponse getCaptchaCode(CaptchaCodeRequest request);

    /**
     * 验证图形验证码
     * @param request
     * @return
     */
    boolean validateCaptchaCode(CaptchaCodeRequest request);

}
