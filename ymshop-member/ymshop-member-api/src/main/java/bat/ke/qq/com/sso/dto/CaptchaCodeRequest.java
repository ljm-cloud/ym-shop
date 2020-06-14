package bat.ke.qq.com.sso.dto;


import java.io.Serializable;

public class CaptchaCodeRequest implements Serializable {
    private String uuid;
    private String code;
    private String extendKey;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getExtendKey() {
        return extendKey;
    }

    public void setExtendKey(String extendKey) {
        this.extendKey = extendKey;
    }
}
