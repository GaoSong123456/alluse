package com.routdata.east.job.detail;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.routdata.east.api.dto.rpa.TbA3701;
import com.routdata.east.api.dto.rpa.TbRpaBaseInfo;
import com.routdata.east.api.dto.rpa.TbRpaOrgInfo;
import com.routdata.east.api.dto.rpa.TbRpaTaskInfo;
import com.routdata.east.service.rpa.IRpaService;
import com.routdata.east.util.HttpUtil;
import com.routdata.east.util.PoiUtils;
import com.routdata.east.util.TaskConstant;
import com.routdata.east.util.Utils;
import com.routdata.rd3f.core.api.common.exception.ServiceException;
import com.routdata.rd3f.core.api.common.utils.DateUtils;
/**
 * 功能：rpaA3701定时任务类<br/>
 * @author Gaos
 * @version 创建时间：2021-02-10 17:15:41
 */
@Component
public class QuartzRpaA3701 {

	private static final Logger logger = LoggerFactory.getLogger(QuartzRpaA3701.class);
	
	private static final String RPA_TASK_TYPE = "A3701";
	
	@Autowired 
	private IRpaService rpaService;

	/**
	 * 真正执行的方法
	 */
	public void running() {
		List<TbRpaOrgInfo> tbRpaOrgInfoList=rpaService.getRpaOrg();  //获取要爬网的机构
		String sessionId=rpaService.getSessionId(1L); //获取sessionid
		if (tbRpaOrgInfoList!=null && !tbRpaOrgInfoList.isEmpty()) {
			/**
			 * 首先判断是否需要补爬数据,任务正常则不需要重新爬取,直到已爬取的数据日期正常,如：今天为8/8,补爬日期为8/3,则需补爬8/3,8/4,8/5,8/6的数据,再爬今天正常爬取的8/7的数据;
			 * 如果一个任务都没有,可能存在数据表存了数据任务表数据没写进去,删除数据表数据后重新爬数;
			 * 有任务数据时,判断是否所有机构的任务数都爬完了,总任务数量与爬网机构总数量不相等需删除任务数据和数据表数据,再重新爬数;
			 */
			Date trapsDateTime=rpaService.getMaxTrapsDate(RPA_TASK_TYPE); //获取要补漏爬取的最大数据日期
			Date updateTrapsDateTime=trapsDateTime; //要修改状态的补漏日期
			Date twoBeforeDate=Utils.getTwoBeforeDate(); //获取两天前的日期
			Date normalDateTime=Utils.getYesterDayDate();  //获取正常流程要爬网的数据日期
			if (trapsDateTime!=null) {
				while (twoBeforeDate.compareTo(trapsDateTime)>= 0) {
					if (!rpaService.ckRpaTaskExistByDate(trapsDateTime,RPA_TASK_TYPE)) { //该数据日期没有爬数任务,则进行爬数
						normalRpaData(tbRpaOrgInfoList, trapsDateTime, sessionId);
					}
					else if (!rpaService.ckRpaTaskCount(trapsDateTime,RPA_TASK_TYPE)) { //爬数任务数量与应爬机构数量不相等,重新爬数
						normalRpaData(tbRpaOrgInfoList, trapsDateTime, sessionId);
					}
					trapsDateTime=DateUtils.addDays(trapsDateTime, 1); //日期加一天
				}
				rpaService.updateTrapsDateTimeState(updateTrapsDateTime,RPA_TASK_TYPE); //修改该爬网日期的状态为已处理
			}
			normalRpaData(tbRpaOrgInfoList, normalDateTime, sessionId);
		}
	}
	
	/**
	 * 执行爬数逻辑
	 */
	private void normalRpaData(List<TbRpaOrgInfo> tbRpaOrgInfoList,Date dataTime,String sessionId) {
		isAllowRpaTime(); //判断当前任务执行时间是否在允许的时间内
		for (TbRpaOrgInfo tbRpaOrgInfo : tbRpaOrgInfoList) {
			Map<String, Integer> statusCodeMap=new HashMap<>();  //响应状态码
			Date startDate=new Date();  //开始时间
			TbRpaTaskInfo tbRpaTaskInfo=new TbRpaTaskInfo();
			try {
				Thread.sleep(10000); //休眠10秒，服务器限制了短时间内请求次数
				getAndSaveData(tbRpaOrgInfo.getvOrgCode(),tbRpaOrgInfo.getvOrgName(),dataTime,sessionId,statusCodeMap);
				tbRpaTaskInfo.setnState(1L); //1-成功
			}catch (InterruptedException e) {
				tbRpaTaskInfo.setnState(2L); //2-失败
				logger.error("Thread异常：", e);
			}catch (Exception e) {
				logger.error("QuartzRpaA3701爬数异常：", e);
				tbRpaTaskInfo.setnState(2L); //2-失败
			}
			finally {
				getFailMsg(tbRpaTaskInfo,statusCodeMap); //获取爬网失败信息
				tbRpaTaskInfo.setvOrgCode(tbRpaOrgInfo.getvOrgCode());
				tbRpaTaskInfo.setvOrgName(tbRpaOrgInfo.getvOrgName());
				tbRpaTaskInfo.setvRpaTaskType(RPA_TASK_TYPE);
				tbRpaTaskInfo.setdDataTime(dataTime);
				tbRpaTaskInfo.setdStartTime(startDate);
				tbRpaTaskInfo.setdEndTime(new Date());
				rpaService.saveRpaTaskInfo(tbRpaTaskInfo);
			}
		}
	}
	
	/**
	 * 获取并且保存数据
	 */
	private void getAndSaveData(String vOrgCode,String vOrgName,Date dataTime,String sessionId,Map<String, Integer> statusCodeMap) throws IOException{
		CloseableHttpResponse response=null;
		try(CloseableHttpClient httpClient=HttpUtil.getHttpClient()) {
			List<TbRpaBaseInfo> tbRpaBaseInfoList=new ArrayList<>();
	    	StringBuilder url=new StringBuilder();
	    	url.append(TaskConstant.RPA_A3701_URL);
	    	url.append("&txdate="+DateUtils.formatDate(dataTime, DateUtils.DF_YYYY_MM));
	    	url.append("&instNo="+vOrgCode);
	    	HttpGet httpGet=new HttpGet(url.toString());
	    	httpGet.setHeader("Accept", "*/*");
	    	httpGet.setHeader("Accept-Encoding", "gzip, deflate");
	    	httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
	    	httpGet.setHeader("Connection", "keep-alive");
	    	httpGet.setHeader("Host", TaskConstant.RPA_HOST_URL);
	    	httpGet.setHeader("Referer", TaskConstant.RPA_REFERER_URL);
	    	httpGet.setHeader("SESSIONID", sessionId);
	    	httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
	    	response=httpClient.execute(httpGet);
	    	statusCodeMap.put("statusCode", response.getStatusLine().getStatusCode()); //响应状态码
	        if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
	        	logger.error("状态码异常："+response.getStatusLine().getStatusCode());
	        	throw new ServiceException("爬数失败");
			}
	    	HttpEntity entity=response.getEntity();
	    	List<List<String>> dataList=PoiUtils.readExcel2007(entity.getContent(), 3, 0, 3);  //参数分别为:文件流,数据开始行,数据开始列,数据结束列
	    	if (dataList.isEmpty()) {
				throw new ServiceException("未获取到数据");
			}
	    	//如果TB_RPA_BASE_INFO表中没有数据，则需要先写入数据
	    	if (!rpaService.ckRpaBaseInfo(RPA_TASK_TYPE)) {
				for (List<String> list : dataList) {
					TbRpaBaseInfo tbRpaBaseInfo=new TbRpaBaseInfo();
					tbRpaBaseInfo.setvNumber(list.get(1));
					tbRpaBaseInfo.setvName(list.get(2));
					tbRpaBaseInfo.setvType(RPA_TASK_TYPE);
					tbRpaBaseInfo.setnState(1L); //1 正常
					tbRpaBaseInfo.setnFzProject(2L); //2 附注项目否
					tbRpaBaseInfoList.add(tbRpaBaseInfo);
				}
				rpaService.saveRpaBaseInfo(tbRpaBaseInfoList);
			}
			TbA3701 tbA3701=new TbA3701();
			tbA3701.setdDataTime(dataTime);
			tbA3701.setvOrgCode(vOrgCode);
			tbA3701.setvOrgName(vOrgName);
			rpaService.saveRpaA3701Info(tbA3701, dataList);
		}finally {
			if (response!=null) {
				response.close();
			}
		}
	}
	
	//获取爬网失败信息
	private void getFailMsg(TbRpaTaskInfo tbRpaTaskInfo,Map<String, Integer> statusCodeMap) {
		Integer statusCode= statusCodeMap.get("statusCode");
		if (statusCode==null) {
			tbRpaTaskInfo.setvFailMsg("爬网异常");
		}
		else {
			if (statusCode!= 200) {
				if (statusCode==401) {
					tbRpaTaskInfo.setvFailMsg("登录认证失败");
				}
				else {
					tbRpaTaskInfo.setvFailMsg("爬网异常");
				}
			}
			else {
				tbRpaTaskInfo.setvFailMsg(null);
			}
		}
	}
	
	//判断当前任务执行时间是否在允许的时间内,是则执行;不是则休眠一段时间在判断
	private void isAllowRpaTime() {
		while (true) {
            LocalTime now = LocalTime.now();
            if (Utils.isWorkHours(now)) { //可以执行
                break;
            } else {
                try {
					Thread.sleep(TimeUnit.HOURS.toMillis(Utils.getRpaTaskSleepHourVal())); //休眠
				} catch (InterruptedException e) {
					logger.error(e.getMessage(),e);
				}
            }
        }
	}
}
