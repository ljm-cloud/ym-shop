package bat.ke.qq.com.common.constant;

public enum OrderSiteEnum {
    main("CART"),
    rushbuy("RUSH");

    public static String getPrekey(String orderSite){
        String preKey=main.getPreKey();
        for (OrderSiteEnum e : OrderSiteEnum.values()) {
            if(e.name().equals(orderSite)){
                preKey=e.getPreKey();
            }
        }
        return preKey;
    }

    OrderSiteEnum(String preKey) {
        this.preKey = preKey;
    }

    private String preKey;

    public String getPreKey() {
        return preKey;
    }

    public void setPreKey(String preKey) {
        this.preKey = preKey;
    }
}
