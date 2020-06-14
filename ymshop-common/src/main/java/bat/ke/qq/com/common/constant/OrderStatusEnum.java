package bat.ke.qq.com.common.constant;

public enum OrderStatusEnum {
    // 0、未付款，1、已付款，2、未发货，3、已发货，4、交易成功，5、交易关闭
    waitPay(0),
    paid(1),
    notYetShipped(2),
    sent(3),
    traded(4),
    close(5);

    OrderStatusEnum(Integer code) {
        this.code = code;
    }

    private Integer code;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
