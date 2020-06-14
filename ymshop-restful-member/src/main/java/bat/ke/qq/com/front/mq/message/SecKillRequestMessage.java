package bat.ke.qq.com.front.mq.message;

import java.io.Serializable;

/**
 * 秒杀请求消息
 */
public class SecKillRequestMessage implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5810025604361901986L;

	/**
	 * 手机号，标识用户唯一身份
	 */
	private String userId;

	/**
	 * 秒杀活动编号
	 */
	private String promId;

	/**
	 * 秒杀虚拟活动编码
	 */
	private String virtualId;

	public SecKillRequestMessage()
	{
		super();
	}

	public SecKillRequestMessage(String userId, String promId, String virtualId) {
		this.userId = userId;
		this.promId = promId;
		this.virtualId = virtualId;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public String getPromId()
	{
		return promId;
	}

	public void setPromId(String promId)
	{
		this.promId = promId;
	}

	public String getVirtualId() {
		return virtualId;
	}

	public void setVirtualId(String virtualId) {
		this.virtualId = virtualId;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("MiaoshaRequestMessage [mobile=");
		builder.append(userId);
		builder.append(", goodsRandomName=");
		builder.append(promId);
		builder.append("]");
		return builder.toString();
	}

}
