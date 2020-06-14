package bat.ke.qq.com.pay;/* ━━━━━━如来保佑━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　┻　　　┃
 * 　　┗━┓　　　┏━┛
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┗┻┛　┗┻┛
 * ━━━━━━永无BUG━━━━━━
 * 源码学院-只为培养BAT程序员而生
 * 以往视频加可可老师QQ：793900009
 * http://bat.ke.qq.com
 */

import bat.ke.qq.com.pay.model.PayOrder;

public interface IWeiXinPayService {

   /**
    * 微信二维码支付
    * @Author  Monkey老师
    */
   String weixinPay(PayOrder payOrder);
}
