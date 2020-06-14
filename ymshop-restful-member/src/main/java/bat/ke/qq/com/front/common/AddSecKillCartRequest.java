package bat.ke.qq.com.front.common;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AddSecKillCartRequest implements Serializable {
    @NotNull(message = "活动Id不能为空")
    private String promId;
    @NotNull(message = "令牌不能为空")
    private String token;
}
