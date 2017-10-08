package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpFilter;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.context.HttpFilterConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控用过滤器
 */
public class HttpMonitorFilter implements HttpFilter {
	private final static Map<String, Map<Long, Long>> REQUEST_START_TIME = new ConcurrentHashMap<String, Map<Long, Long>>();

	@Override
	public Object onRequest(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult ) {
		String sessionId = request.getSession().getId();
		synchronized (REQUEST_START_TIME) {
			if (!REQUEST_START_TIME.containsKey(sessionId)) {
				REQUEST_START_TIME.put(sessionId, new ConcurrentHashMap<Long, Long>());
			}
		}

		Map<Long, Long> threadMark = REQUEST_START_TIME.get(sessionId);

		threadMark.put(Thread.currentThread().getId(), System.currentTimeMillis());
		return prevFilterResult;
	}

	@Override
	public Object onResponse(HttpFilterConfig filterConfig, HttpRequest request, HttpResponse response, Object prevFilterResult ) {
		//处理请求分析数据
		requestAnalysis(request);

		//处理IP数据分析
		ipAddressAnalysis(request);

		return prevFilterResult;
	}

	/**
	 * 请求数据的分析
	 * @param request 请求对象
	 */
	public void requestAnalysis(HttpRequest request){
		Long startTime = null;

		String sessionId = request.getSession().getId();
		long threadId = Thread.currentThread().getId();
		if(REQUEST_START_TIME.containsKey(sessionId) &&
			REQUEST_START_TIME.get(sessionId).containsKey(threadId)){
			startTime = REQUEST_START_TIME.get(sessionId).get(threadId);
			REQUEST_START_TIME.get(sessionId).remove(threadId);
		}

		if(startTime!=null) {
			String requestPath = request.protocol().getPath();
			long dealTime = (System.currentTimeMillis() - startTime);

			RequestAnalysis requestAnalysis = null;
			if (MonitorGlobal.REQUEST_ANALYSIS.containsKey(requestPath)) {
				requestAnalysis = MonitorGlobal.REQUEST_ANALYSIS.get(requestPath);
			} else {
				requestAnalysis = new RequestAnalysis(requestPath);
				MonitorGlobal.REQUEST_ANALYSIS.put(requestPath, requestAnalysis);
			}

			requestAnalysis.addRequestTime(Long.valueOf(dealTime).intValue());
		}
	}


	/**
	 * ip 数据的分析
	 * @param request
	 */
	public void ipAddressAnalysis(HttpRequest request){
		String requestPath = request.protocol().getPath();
		String ipAddress = request.getRemoteAddres();

		IPAnalysis ipAnalysis = null;
		if(MonitorGlobal.IP_ANALYSIS.containsKey(ipAddress)){
			ipAnalysis = MonitorGlobal.IP_ANALYSIS.get(ipAddress);
		}else{
			ipAnalysis = new IPAnalysis(ipAddress);
			MonitorGlobal.IP_ANALYSIS.put(ipAddress, ipAnalysis);
		}

		ipAnalysis.addRequest(requestPath);
	}
}