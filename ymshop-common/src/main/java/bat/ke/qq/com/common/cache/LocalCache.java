package bat.ke.qq.com.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LocalCache {

    private static final Logger logger = LoggerFactory.getLogger(LocalCache.class);
    //默认缓存容量
    private static final int DEFAULT_CAPACITY = 500;
    //最大缓存容量
    private static final int MAX_CAPACITY = 1000000;
    //监控清除过期缓存频率
    private static final int MONITOR_FREQUENCY = 3;

    //构建本地缓存的map
    private static ConcurrentHashMap<String, CacheEntity> cache = new ConcurrentHashMap<String, CacheEntity>(DEFAULT_CAPACITY);

    //监控线程启动
    static{
        new Thread(new MonitorThread()).start();
    }

    //监控线程
    static class MonitorThread implements Runnable{
        @Override
        public void run() {
            while(true){
                try {
//                    logger.info("START CACHE MONITOR");
                    TimeUnit.SECONDS.sleep(MONITOR_FREQUENCY);
                    checkTime();
                }catch (Exception e){
                    logger.error("MONITOR CACHE HAS THROWS AN EXCEPTION:", e);
                }
            }
        }

        //过期key剔除
        private void checkTime(){
            cache.entrySet().stream().forEach(item -> {
                CacheEntity value = item.getValue();
                if(value.getExpireTime() != -1 && TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - value.getGmtCreate()) > value.getExpireTime()){
                    String key = item.getKey();
                    cache.remove(key);
                    logger.info("REMOVE EXPIRE KEY:{}", key);
                }
            });
        }
    }

    /**
     * 将key-value 保存到本地缓存并设置缓存过期时间
     * @param key
     * @param value
     * @param expireTime 过期时间，如果是-1 则表示永不过期
     * @return
     */
    public boolean put(String key, Object value, int expireTime){
        if(cache.size() >= MAX_CAPACITY){
            throw new RuntimeException("CAPACITY OVERFLOW!");
        }
        return putCloneValue(key, value, expireTime);
    }

    /**
     *将值通过序列化 clone 处理后保存到缓存中，可以解决值引用的问题
     *@param key
     *@param value
     *@param expireTime 过期时间，如果是-1 则表示永不过期
     *@return boolean
     */
    private boolean putCloneValue(String key, Object value, int expireTime){
        try {
            CacheEntity cacheEntity = clone(new CacheEntity(value, System.nanoTime(), expireTime));
         /*CacheEntity cacheEntity = (CacheEntity)javassist();
         cacheEntity.setObj(value);
         cacheEntity.setExpireTime(expireTime);
         cacheEntity.setGmtCreate(System.nanoTime());*/
            cache.put(key, cacheEntity);
            return true;
        }catch (Exception e){
            logger.error("PUT VALUE HAS THROWS AN EXCEPTION:", e);
            return false;
        }
    }

//    /**
//     * javassist测试
//     */
//    public <T extends Serializable> T javassist() throws Exception {
//        ClassPool classPool = ClassPool.getDefault();
//        CtClass ctClass = classPool.makeClass("com.zaxxer.hikari.cacheLocal.CacheEntity1");
//        CtField ctField1 = new CtField(classPool.get("java.lang.Object"), "value", ctClass);
//        CtField ctField2 = new CtField(CtClass.longType, "gmtCreate", ctClass);
//        CtField ctField3 = new CtField(CtClass.intType, "expireTime", ctClass);
//        CtField ctField4 = new CtField(CtClass.intType, "serialVersionUID", ctClass);
//        ctField1.setModifiers(Modifier.PRIVATE);
//        ctField2.setModifiers(Modifier.PRIVATE);
//        ctField3.setModifiers(Modifier.PRIVATE);
//        ctField4.setModifiers(Modifier.PRIVATE);
//        ctField4.setModifiers(Modifier.STATIC);
//        ctField4.setModifiers(Modifier.FINAL);
//        ctClass.addField(ctField4, "7172649826282703560L");
//        ctClass.setInterfaces(new CtClass[]{classPool.get("java.io.Serializable")});
//        ctClass.addMethod(CtNewMethod.setter("setValue", ctField1));
//        ctClass.addMethod(CtNewMethod.setter("setGmtCreate", ctField2));
//        ctClass.addMethod(CtNewMethod.setter("setExpireTime", ctField3));
//        ctClass.addMethod(CtNewMethod.getter("getValue", ctField1));
//        ctClass.addMethod(CtNewMethod.getter("getGmtCreate", ctField2));
//        ctClass.addMethod(CtNewMethod.getter("getExpireTime", ctField3));
//        CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get("java.lang.Object"), classPool.get("long"), classPool.get("int")}, ctClass);
//        constructor.setBody("{$0.value = $1;$0.gmtCreate = $2;$0.expireTime = $3;}");
//        ctClass.addConstructor(constructor);
//        ctClass.writeFile("/dd/aa");
//        Object o = ctClass.toClass().newInstance();
//        ctClass.detach();
//        return (T) o;
//    }

    /**
     * 序列化 克隆处理
     * @param obj
     * @return CacheEntity
     */
    private <T extends Serializable> T clone(T obj){
        T target = null;
//      try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos);
//           ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))){
//         oos.writeObject(obj);
//         target = (CacheEntity) ois.readObject();
//      }catch (Exception e){
//         logger.error("CLONE VALUE HAS THROWS AN EXCEPTION:", e);
//      }
//      return target;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            target = (T) ois.readObject();
            ois.close();
        }catch (Exception e){
            logger.error("CLONE VALUE HAS THROWS AN EXCEPTION:", e);
        }
        return target;
    }

    /**
     *  清除缓存
     */
    public void clear(){
        cache.clear();
    }

    /**
     * 根据key得到value
     * @param key
     * @return Object
     */
    public Object getValue(String key){
        CacheEntity cacheEntity=cache.get(key);
        if (cacheEntity!=null){
            return cacheEntity.getObj();
        }
        return null;
    }
}