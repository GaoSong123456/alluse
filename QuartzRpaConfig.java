package com.routdata.east.job.conf;


import org.quartz.CronTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.routdata.rd3f.core.api.common.utils.Global;

/*  
 * @author: pihao  
 * @date:  创建时间    2020年6月23日 下午10:38:10   
 */
@Configuration
public class QuartzRpaConfig {
	
	public static final String TARGET_METHOD = "running";

	/**
	 * 第一步定义MethodInvokingJobDetailFactoryBean
	 * 
	 * @return
	 */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaA3701FactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaA3701");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}

	/**
	 * 第二步定义CronTriggerFactoryBean
	 * 
	 * @return
	 */
	@Bean
	CronTriggerFactoryBean quartzRpaA3701Config() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaA3701FactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.a3701.info"));
		return bean;
	}

	/* rpa3703汇总表 */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaA3703FactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaA3703");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaA3703Config() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaA3703FactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.a3703.info"));
		return bean;
	}
	
	/* rpa3703网贷 */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaA3703WDFactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaA3703WD");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaA3703WDConfig() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaA3703WDFactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.a3703wd.info"));
		return bean;
	}
	
	/* rpaGf0103 */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaGf0103FactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaGf0103");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaGf0103Config() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaGf0103FactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.gf0103.info"));
		return bean;
	}
	
	
	/* rpaGf0107 */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaGf0107FactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaGf0107");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaGf0107Config() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaGf0107FactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.gf0107.info"));
		return bean;
	}
	
	/* QuartzRpaFailTask */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaFailTaskFactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaFailTask");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaFailTaskConfig() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaFailTaskFactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.fail.task"));
		return bean;
	}
	
	/* QuartzRpaKeepLogin */
	@Bean
	MethodInvokingJobDetailFactoryBean quartzRpaKeepLoginFactoryBean() {
		MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();

		bean.setTargetBeanName("quartzRpaKeepLogin");
		bean.setTargetMethod(TARGET_METHOD);
		bean.setConcurrent(false); // 上一个任务没有执行完，不重新启动新的任务
		return bean;
	}
	
	@Bean
	CronTriggerFactoryBean quartzRpaKeepLoginConfig() {
		CronTriggerFactoryBean bean = new CronTriggerFactoryBean();
		bean.setJobDetail(quartzRpaKeepLoginFactoryBean().getObject());
		bean.setCronExpression(Global.getConfig("rpa.keep.login"));
		return bean;
	}

	/**
	 * 第3步定义SchedulerFactoryBean，所有的需要启动的任务都可以写到这里
	 * 
	 * @return
	 */
	
	@Bean
	SchedulerFactoryBean quartzBean() {
		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		CronTrigger quartzRpaA3701Trigger = quartzRpaA3701Config().getObject();
		CronTrigger quartzRpaA3703Trigger = quartzRpaA3703Config().getObject();
		CronTrigger quartzRpaA3703WDTrigger = quartzRpaA3703WDConfig().getObject();
		CronTrigger quartzRpaGf0103Trigger = quartzRpaGf0103Config().getObject();
		CronTrigger quartzRpaGf0107Trigger = quartzRpaGf0107Config().getObject();
		CronTrigger quartzRpaFailTaskTrigger = quartzRpaFailTaskConfig().getObject();
		CronTrigger quartzKeepLoginTrigger = quartzRpaKeepLoginConfig().getObject();
		bean.setTriggers(quartzRpaA3701Trigger,quartzRpaA3703Trigger,quartzRpaA3703WDTrigger,quartzRpaGf0103Trigger,
						 quartzRpaGf0107Trigger,quartzRpaFailTaskTrigger,quartzKeepLoginTrigger);
		return bean;
	}
	
	

}
