package bat.ke.qq.com.common.constants;
/**
 * 支付途径
 * 创建者 Monkey老师
 * 创建时间	2018年7月27日
 *
 */
public enum PayWay {
	PC("PC,平板",(short)1),MOBILE("手机",(short)2);
	
	private Short code;
	private String name;
	
	private PayWay(String name, Short code) {
		this.name = name;
		this.code = code;
	}

	public static String getName(Short code, String name) {
		for (PayWay c : PayWay.values()) {
			if (c.getCode() == code) {
				return c.name;
			}
		}
		return null;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public short getCode() {
		return code;
	}

	public void setCode(short code) {
		this.code = code;
	}

}
