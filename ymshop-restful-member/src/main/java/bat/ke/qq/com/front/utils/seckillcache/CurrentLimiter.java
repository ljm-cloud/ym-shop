package bat.ke.qq.com.front.utils.seckillcache;

import bat.ke.qq.com.common.exception.YmshopException;

/**
 * redis限流器
 */
public abstract class CurrentLimiter<P>
{
	/**
	 * 做限流，如果超过了流量则抛出异常
	 */
	public void doLimit(P param, String errorMsg)
	{
		// 获取流量最大值
		int limit = getLimit(param);
		// 现有流量值
		Integer currentLimit = getCurrentLimit();

		// 如果现有流量值大于了限流值，或者自增了流量之后大于了限流值则表示操作收到了限流
		if (currentLimit != null && currentLimit >= limit)
		{
			throw new YmshopException(errorMsg);
		}
	}

	/**
	 * 获取即时流量值
	 */
	protected abstract int getCurrentLimit();

	/**
	 * 获取限流器名字
	 */
	protected abstract String getLimiterName(P param);

	/**
	 * 获取限流的最大流量
	 */
	protected abstract int getLimit(P param);

}
