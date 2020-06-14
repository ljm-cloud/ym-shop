package bat.ke.qq.com.front.common;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SecKillRequest implements Serializable {
    @NotNull(message = "活动Id不能为空")
    private String promId;
    @NotNull(message = "验证码不能为空")
    private String captcha;
}
