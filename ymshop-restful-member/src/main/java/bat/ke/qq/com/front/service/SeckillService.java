package bat.ke.qq.com.front.service;

public interface SeckillService {
   void seckill(String promId);

   int addSecKillCart(String promId, String token);

   int commonSecKill(String promId);
}
