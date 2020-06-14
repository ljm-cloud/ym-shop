package bat.ke.qq.com.common.cache;

import java.io.Serializable;

public class CacheEntity implements Serializable {

    private static final long serialVersionUID = 7172649826282703560L;

    private Object obj;

    private long gmtCreate;

    private int expireTime;

    public CacheEntity(Object obj, long gmtCreate, int expireTime) {
        this.obj = obj;
        this.gmtCreate = gmtCreate;
        this.expireTime = expireTime;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
    }
}