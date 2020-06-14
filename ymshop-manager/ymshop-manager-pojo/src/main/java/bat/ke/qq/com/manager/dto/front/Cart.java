package bat.ke.qq.com.manager.dto.front;

import bat.ke.qq.com.common.constant.OrderSiteEnum;

import java.io.Serializable;

/**
 * @author 源码学院
 */
public class Cart implements Serializable {

    private Long userId;

    private Long productId;

    private String checked;

    private int productNum;

    private String orderSite= OrderSiteEnum.main.name();

    private String promId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getChecked() {
        return checked;
    }

    public void setChecked(String checked) {
        this.checked = checked;
    }

    public int getProductNum() {
        return productNum;
    }

    public void setProductNum(int productNum) {
        this.productNum = productNum;
    }

    public String getOrderSite() {
        return orderSite;
    }

    public void setOrderSite(String orderSite) {
        this.orderSite = orderSite;
    }

    public String getPromId() {
        return promId;
    }

    public void setPromId(String promId) {
        this.promId = promId;
    }
}
