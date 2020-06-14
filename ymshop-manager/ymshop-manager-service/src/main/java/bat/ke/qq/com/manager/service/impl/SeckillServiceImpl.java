package bat.ke.qq.com.manager.service.impl;

import bat.ke.qq.com.common.constant.CommonConstant;
import bat.ke.qq.com.common.jedis.JedisClient;
import bat.ke.qq.com.common.pojo.DataTablesResult;
import bat.ke.qq.com.manager.dto.DtoUtil;
import bat.ke.qq.com.manager.dto.SeckillDto;
import bat.ke.qq.com.manager.mapper.TbSeckillExample;
import bat.ke.qq.com.manager.mapper.TbSeckillMapper;
import bat.ke.qq.com.manager.pojo.TbSeckill;
import bat.ke.qq.com.manager.service.SeckillService;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 源码学院-ANT
 * 只为培养BAT程序员而生
 * http://bat.ke.qq.com
 * 往期视频加群:516212256 暗号:6
 */
@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private TbSeckillMapper tbSeckillMapper;

    @Override
    public DataTablesResult querySeckillList() {
        DataTablesResult dataTablesResult = new DataTablesResult();
        TbSeckillExample tbSeckillExample = new TbSeckillExample();
        List<TbSeckill> tbSeckills = tbSeckillMapper.selectByExample(tbSeckillExample);
        dataTablesResult.setData(tbSeckills);
        return dataTablesResult;
    }

    @Override
    public int addSeckill(SeckillDto dto) {
        //todo  实际应用中应当校验当前秒杀信息是否可以保存,比如商品库存数是否足够
        //商品状态是否上架状态，秒杀价格是否低于普通价格
        TbSeckill tbSeckill = DtoUtil.seckillDto2TbSeckill(dto);
        int insert = tbSeckillMapper.insert(tbSeckill);
        if (insert > 0) {
            //step 1:秒杀信息插入成功，讲活动信息存入redis
            String seckill_key = CommonConstant.SECKILL_REDISKEY + tbSeckill.getId();
            Integer expireSeconds = Integer.valueOf(
                    String.valueOf(
                            (tbSeckill.getEnddate().getTime() - System.currentTimeMillis()) / 1000));
            jedisClient.setex(seckill_key, JSONUtil.toJsonStr(tbSeckill), expireSeconds);

            //step 2:秒杀库存存储redis
            String seckill_stock_key = CommonConstant.SECKILL_STOCK_REDISKEY + tbSeckill.getId();
            jedisClient.setex(seckill_stock_key, String.valueOf(tbSeckill.getSeckillStock()), expireSeconds);

            //step 3:如果设置了库存分割，将库存存平分到N个key中 (我这暂时默认切分成5分，具体分割数和算法精确这块后续完善)
            if (dto.isSplit()) {
                Integer splitStock = tbSeckill.getSeckillStock() / 5;
                String virtualRelastion = CommonConstant.SECKILL_VIRTUAL_STOCK_REDISKEY + tbSeckill.getId();
                for (int i = 0; i < 5; i++) {
                    String virtualKey = seckill_stock_key + "_" + i;
                    jedisClient.setex(virtualKey, String.valueOf(splitStock), expireSeconds);
                    jedisClient.lpush(virtualRelastion,  tbSeckill.getId()+"_"+i, expireSeconds);
                }
            }
        }
        return insert;
    }

    @Override
    public int delSeckill(Long id) {
        TbSeckill tbSeckill = tbSeckillMapper.selectByPrimaryKey(id);
        if (tbSeckill != null) {
            String seckill_key = CommonConstant.SECKILL_REDISKEY + tbSeckill.getId();
            if (jedisClient.exists(seckill_key)) {
                jedisClient.del(seckill_key);
            }
        }
        return tbSeckillMapper.deleteByPrimaryKey(id);
    }
}
