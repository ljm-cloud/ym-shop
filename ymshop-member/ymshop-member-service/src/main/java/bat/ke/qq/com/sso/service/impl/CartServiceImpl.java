package bat.ke.qq.com.sso.service.impl;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.constant.OrderSiteEnum;
import bat.ke.qq.com.common.exception.YmshopException;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.manager.dto.DtoUtil;
import bat.ke.qq.com.manager.dto.front.CartProduct;
import bat.ke.qq.com.manager.mapper.TbItemMapper;
import bat.ke.qq.com.manager.mapper.TbSeckillMapper;
import bat.ke.qq.com.manager.pojo.TbItem;
import bat.ke.qq.com.manager.pojo.TbSeckill;
import bat.ke.qq.com.sso.service.CartService;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 源码学院
 */
@Service
public class CartServiceImpl implements CartService {

    private final static Logger log= LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private JedisClient jedisClient;
//    @Value("${CART_PRE}")
//    private String CART_PRE;
    @Autowired
    private TbItemMapper itemMapper;
    @Autowired
    private TbSeckillMapper seckillMappper;

    @Override
    public int addCart(long userId, long itemId, int num,String oderSite,String promId) {
        //hash: "key:用户id" field："商品id" value："商品信息"
        String cartPre=OrderSiteEnum.getPrekey(oderSite);

        //只有主站订单走该逻辑，抢购订单只允许单品1件购买
        if(OrderSiteEnum.main.equals(oderSite)){
            Boolean hexists = jedisClient.hexists(cartPre + ":" + userId, itemId + "");
            //如果存在数量相加
            if (hexists ) {
                String json = jedisClient.hget(cartPre + ":" + userId, itemId + "");
                if(json!=null){
                    CartProduct cartProduct = new Gson().fromJson(json,CartProduct.class);
                    cartProduct.setProductNum(cartProduct.getProductNum() + num);
                    jedisClient.hset(cartPre + ":" + userId, itemId + "", new Gson().toJson(cartProduct));
                }else {
                    return 0;
                }
                return 1;
            }
        }
        //如果不存在，根据商品id取商品信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
        if(item==null){
            return 0;
        }
        CartProduct cartProduct= DtoUtil.TbItem2CartProduct(item);
        //如果是抢购商品，价格取抢购活动价格，数量只能为1
        if (OrderSiteEnum.rushbuy.name().equals(oderSite)){
            TbSeckill tbSeckill = JSONUtil.toBean(jedisClient.get(CommonConstant.SECKILL_REDISKEY + promId),TbSeckill.class);
            if(tbSeckill!=null ){
                Date now=new Date();
               if (now.before(tbSeckill.getStartdate())){
                   throw new YmshopException("抢购活动未开始");
               }else if(now.after(tbSeckill.getEnddate())){
                   throw new YmshopException("抢购活动已结束");
                }
            }else {
                throw new YmshopException("抢购活动已失效");
            }
            cartProduct.setSalePrice(tbSeckill.getSeckillPrice());
            cartProduct.setProductNum((long) 1);
            cartProduct.setPromId(promId);
        }else {
            cartProduct.setProductNum((long) num);
        }
        cartProduct.setChecked("1");
        jedisClient.hset(cartPre  + ":" + userId, itemId + "", new Gson().toJson(cartProduct));
        return 1;
    }

    @Override
    public List<CartProduct> getCartList(long userId,String oderSite) {
        List<String> jsonList = jedisClient.hvals(OrderSiteEnum.getPrekey(oderSite) + ":" + userId);
        List<CartProduct> list = new ArrayList<>();
        for (String json : jsonList) {
            CartProduct cartProduct = new Gson().fromJson(json,CartProduct.class);
            list.add(cartProduct);
        }
        return list;
    }

    @Override
    public int updateCartNum(long userId, long itemId, int num, String checked) {

        String json = jedisClient.hget( OrderSiteEnum.main.getPreKey()+":" + userId, itemId + "");
        if(json==null){
            return 0;
        }
        CartProduct cartProduct = new Gson().fromJson(json,CartProduct.class);
        cartProduct.setProductNum((long) num);
        cartProduct.setChecked(checked);
        jedisClient.hset(OrderSiteEnum.main.getPreKey() + ":" + userId, itemId + "", new Gson().toJson(cartProduct));
        return 1;
    }

    @Override
    public int checkAll(long userId,String checked) {
        List<String> jsonList = jedisClient.hvals(OrderSiteEnum.main.getPreKey() + ":" + userId);

        for (String json : jsonList) {
            CartProduct cartProduct = new Gson().fromJson(json,CartProduct.class);
            if("true".equals(checked)) {
                cartProduct.setChecked("1");
            }else if("false".equals(checked)) {
                cartProduct.setChecked("0");
            }else {
                return 0;
            }
            jedisClient.hset(OrderSiteEnum.main.getPreKey() + ":" + userId, cartProduct.getProductId() + "", new Gson().toJson(cartProduct));
        }
        return 1;
    }

    @Override
    public int deleteCartItem(long userId, long itemId) {
        jedisClient.hdel(OrderSiteEnum.main.getPreKey() + ":" + userId, itemId + "");
        return 1;
    }

    @Override
    public int delChecked(long userId) {
        List<String> jsonList = jedisClient.hvals(OrderSiteEnum.main.getPreKey()+":"+userId);
        for (String json : jsonList) {
            CartProduct cartProduct = new Gson().fromJson(json,CartProduct.class);
            if("1".equals(cartProduct.getChecked())) {
                jedisClient.hdel(OrderSiteEnum.main.getPreKey()+":"+userId, cartProduct.getProductId()+"");
            }
        }
        return 1;
    }
}
