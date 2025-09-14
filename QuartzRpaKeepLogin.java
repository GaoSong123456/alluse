package com.routdata.east.job.detail;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.routdata.east.service.rpa.IRpaService;
import com.routdata.east.util.HttpUtil;
import com.routdata.east.util.TaskConstant;
/**
 * 功能：rpa保持登录定时任务类<br/>
 * @author Gaos
 * @version 创建时间：2021-02-10 17:15:41
 */
@Component
public class QuartzRpaKeepLogin {

	private static final Logger logger = LoggerFactory.getLogger(QuartzRpaKeepLogin.class);
	
	@Autowired 
	private IRpaService rpaService;

	/**
	 * 真正执行的方法
	 */
	public void running() {
		try {
			String sessionId=rpaService.getSessionId(1L); //获取sessionid
			try {
				getData(sessionId);
			} catch (Exception e) {
				logger.error("QuartzRpaKeepLogin爬数异常：", e);
			}
	    	
		} catch (Exception e) {
			logger.error("QuartzRpaKeepLogin任务异常：", e);
		}
	}
	
	/**
	 * 获取并且保存数据
	 */
	private void getData(String sessionId) throws IOException {
		CloseableHttpResponse response=null;
		try(CloseableHttpClient httpClient=HttpUtil.getHttpClient()) {
			StringBuilder url=new StringBuilder();
        	url.append(TaskConstant.RPA_KEEP_LOGIN_URL);
        	HttpPost httpGet=new HttpPost(url.toString());
        	httpGet.setHeader("Accept", "application/json,text/plain,*/*");
        	httpGet.setHeader("Accept-Encoding", "gzip,deflate");
        	httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
        	httpGet.setHeader("Connection", "keep-alive");
        	httpGet.setHeader("Content-Type", "application/json;charset=utf-8");
        	httpGet.setHeader("Host", TaskConstant.RPA_HOST_URL);
        	httpGet.setHeader("Origin", TaskConstant.RPA_ORIGIN_URL);
        	httpGet.setHeader("Referer", TaskConstant.RPA_REFERER_URL);
        	httpGet.setHeader("SESSIONID", sessionId);
        	httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        	response=httpClient.execute(httpGet);
        	String responseString = EntityUtils.toString(response.getEntity());
        	//JSONObject jsonData = JSONObject.parseObject(responseString);
        	logger.info(responseString);
		} finally {
			if (response!=null) {
				response.close();
			}
		}
	}
	
}
