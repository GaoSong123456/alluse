/**
 * 
 */
package com.routdata.east.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * http工具类
 * @author gaos
 * @date: 2023年5月24日
 */
public class HttpUtil {
	private HttpUtil(){}
	
	//获取httpClient
	public static CloseableHttpClient getHttpClient(){
		RequestConfig config = RequestConfig.custom().setConnectTimeout(80000).setSocketTimeout(80000).build();
    	return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}
}
