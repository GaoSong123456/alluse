package com.routdata.consumemanage.svc.report.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.Lists;
import com.routdata.consumemanage.api.baseinfo.dto.TbClassInfo;
import com.routdata.consumemanage.api.baseinfo.dto.TbNameInfo;
import com.routdata.consumemanage.api.report.dto.DeviceReportApplyQuery;
import com.routdata.consumemanage.api.report.dto.ReportExpand;
import com.routdata.consumemanage.api.report.dto.ReportQuery;
import com.routdata.consumemanage.api.report.dto.TbDeviceReportApply;
import com.routdata.consumemanage.api.report.service.IReportService;
import com.routdata.consumemanage.api.stock.dto.DeviceFlowQuery;
import com.routdata.consumemanage.api.stock.dto.TbDeviceFlow;
import com.routdata.consumemanage.api.stock.dto.TbDeviceInfo;
import com.routdata.consumemanage.api.transfer.dto.OrgTree;
import com.routdata.consumemanage.svc.report.dao.ReportDao;
import com.routdata.rd3f.core.api.common.dto.Page;
import com.routdata.rd3f.core.api.org.dto.VDepartment;

/**     
 * 统计报表serviceimpl     
 * @author: gaos
 * @date: 2021年8月20日     
 */
@Service
public class ReportService implements IReportService{
	
	@Autowired
	private ReportDao reportDao;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	//队列
	private static BlockingQueue<Runnable> lbp=new LinkedBlockingQueue<Runnable>();
	
	//线程池
	private static ThreadPoolExecutor tpe=new ThreadPoolExecutor(5, 10, 30000, TimeUnit.MILLISECONDS, lbp);
	
	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
	
	private static final Integer MAX_BATCH_QUERY_SIZE = 500;
	
	/**
	 * 根据条件获取设备成本报表分页数据
	 * 
	 * @param query
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceCostReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceCostReport(query));
		return page;
	}

	/**
	 * 获取设备目录信息
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TbNameInfo> getNameInfo() {
		return reportDao.getNameInfo();
	}

	/**
	 * 获取设备分类信息
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TbClassInfo> getClassInfo() {
		return reportDao.getClassInfo();
	}

	/**
	 * 根据条件获取设备成本报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceCostReport(ReportQuery query) {
		return reportDao.findListForDeviceCostReport(query);
	}
	
	/**
	 * 根据条件获取设备成本详情报表数据     
	 * 
	 * @param query
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceCostDetailReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceCostDetailReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取设备成本详情报表数据  
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceCostDetailReport(ReportQuery query) {
		return reportDao.findListForDeviceCostDetailReport(query);
	}

	/**
	 * 根据条件获取借用归还统计报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForBorrowRevertReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForBorrowRevertReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取借用归还统计报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForBorrowRevertReport(ReportQuery query) {
		return reportDao.findListForBorrowRevertReport(query);
	}
	
	/**
	 * 根据条件获取设备维修统计报表数据
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceRepairReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceRepairReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取设备维修统计报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceRepairReport(ReportQuery query) {
		return reportDao.findListForDeviceRepairReport(query);
	}
	
	/**
	 * 根据条件获取设备维修统计详情报表数据
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceRepairDetailReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceRepairDetailReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取设备维修统计详情报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceRepairDetailReport(ReportQuery query) {
		return reportDao.findListForDeviceRepairDetailReport(query);
	}
	
	/**
	 * 根据条件获取设备使用情况统计数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceUseReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceUseReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取设备使用情况统计数据  
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceUseReport(ReportQuery query) {
		return reportDao.findListForDeviceUseReport(query);
	}
	
	/**
	 * 根据条件获取设备使用详情数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceDetailReport(ReportQuery query) {
		Page<ReportExpand> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceDetailReport(query));
		return page;
	}
	
	/**
	 * 根据条件获取设备使用详情数据   
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceDetailReport(ReportQuery query) {
		return reportDao.findListForDeviceDetailReport(query);
	}
	
	/**
	 * 根据条件获取设备详情报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<ReportExpand> findPageForDeviceSituationReport(ReportQuery query,VDepartment dsjDepartment) {
		Page<ReportExpand> page = query.initPageQuery();
		//如果查询条件中设备条码信息不为空,则用条码做全表查询,不限机构(限为当前地市局)
		if (StringUtils.isNotBlank(query.getRepBarCode()) || 
			StringUtils.isNotBlank(query.getRepBeginBarCodeSeq()) || StringUtils.isNotBlank(query.getRepEndBarCodeSeq())) {
			query.setRepDsjTreeNo(dsjDepartment.getTreeNo());
			page.setList(reportDao.findAllForDeviceBarCode(query));
		}
		else {
			page.setList(reportDao.findListForDeviceSituationReport(query));
		}
		return page;
	}
	
	/**
	 * 根据条件获取设备详情报表数据 
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceSituationReport(ReportQuery query,VDepartment dsjDepartment) {
		//如果查询条件中设备条码信息不为空,则用条码做全表查询,不限机构(限为当前地市局)
		if (StringUtils.isNotBlank(query.getRepBarCode()) || 
			StringUtils.isNotBlank(query.getRepBeginBarCodeSeq()) || StringUtils.isNotBlank(query.getRepEndBarCodeSeq())) {
			query.setRepDsjTreeNo(dsjDepartment.getTreeNo());
			return reportDao.findAllForDeviceBarCode(query);
		}
		return reportDao.findListForDeviceSituationReport(query);
	}

	/**
	 * 获取机构树
	 */
	@Override
	@Transactional(readOnly = true)
	public List<OrgTree> getOrgTree(String treeNo) {
		return reportDao.getOrgTree(treeNo);
	}
	
	/**
	 * 根据条件获取设备流转信息   
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<TbDeviceFlow> findPageForDeviceFlow(DeviceFlowQuery<TbDeviceFlow> query) {
		Page<TbDeviceFlow> page = query.initPageQuery();
		page.setList(reportDao.findListForDeviceFlow(query));
		return page;
	}

	/**
	 * 校验设备是否存在   
	 */
	@Override
	@Transactional(readOnly = true)
	public boolean ckDeviceExist(Long ndeviceInfo) {
		return reportDao.ckDeviceExist(ndeviceInfo)>0L;
	}

	/**      
	 * 校验固定资产编号是否存在       
	 */
	@Override
	@Transactional(readOnly = true)
	public boolean ckDeciceCodeExist(String vDeciceCode) {
		return reportDao.ckDeciceCodeExist(vDeciceCode)>0L;
	}

	/**      
	 * 修改固定资产编号       
	 */
	@Override
	@Transactional(readOnly = false)
	public void updateDeciceCode(TbDeviceInfo tbDeviceInfo) {
		reportDao.updateDeciceCode(tbDeviceInfo);  //修改设备信息
		// 保存设备流转信息
		TbDeviceFlow dFlow = new TbDeviceFlow();
		dFlow.setnDeviceInfo(tbDeviceInfo.getnDeviceInfo());
		dFlow.setvFlowType((long) 9); // (1-入库 2-出库 3-使用 4-归还 5-故障 6-维修 7-报废 8-故障修复,9-设备信息修改)
		dFlow.setdCreateOpt(tbDeviceInfo.getdModifyOpt());
		dFlow.setvOptDept(tbDeviceInfo.getvModifyDept());
		reportDao.saveDeviceFlowInfo(dFlow);
	}

	/**
	 * 查询设备信息报表申请列表
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<TbDeviceReportApply> findPageForReportApply(DeviceReportApplyQuery query) {
		Page<TbDeviceReportApply> page = query.initPageQuery();
		page.setList(reportDao.findListForReportApply(query));
		return page;
	}

	/**
	 * 新增设备信息申请下载
	 */
	@Override
	@Transactional(readOnly = false)
	public void insertDeviceReportApply(TbDeviceReportApply tbDeviceReportApply) {
		reportDao.insertDeviceReportApply(tbDeviceReportApply);
	}

	/**
	 * 查询待生成文件的设备信息申请
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TbDeviceReportApply> findWaiDeviceReportApply() {
		return reportDao.findWaiDeviceReportApply();
	}
	
	/**      
	 * 查询全市设备信息详情    
	 */ 
	@Override
	@Transactional(readOnly = true)
	public List<ReportExpand> getListForDeviceReportApplyJob(String dsjTreeNo) {
		return reportDao.getListForDeviceReportApplyJob(dsjTreeNo);
	}
	
	@Override
	@Transactional(readOnly = true)
	public void getListForDeviceReportApplyJob1(String dsjTreeNo) {
		//ExecutorService executorService= Executors.newSingleThreadExecutor(); //线程池
		if (redisTemplate.hasKey("DeviceReportApplyJobList")) { //key存在先删掉
			redisTemplate.delete("DeviceReportApplyJobList");
		}
        List<ReportExpand> dataList= new CopyOnWriteArrayList<>();
        int totalPage = getTotalPage(reportDao.getCountForDeviceReportApplyJob(dsjTreeNo));
        List<Integer> pageNumbers = getPageNumbers(totalPage);
        CompletableFuture[] completableFutures = pageNumbers.stream()
                .map(pageNumber -> CompletableFuture
                        .supplyAsync(() -> {
                        	List<ReportExpand> list = reportDao.getListForDeviceReportApplyJobByPage(pageNumber,MAX_BATCH_QUERY_SIZE,dsjTreeNo);
                            return Objects.nonNull(list) ? list : new ArrayList<ReportExpand>();
                        }, tpe)
                        .whenComplete((list, throwable) -> {
                            if (!ObjectUtils.isEmpty(list)) {
                                dataList.addAll((Collection<? extends ReportExpand>) list);
                            }
                        })).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(completableFutures).join();
		//redisTemplate.opsForValue().set("DeviceReportApplyJobList", dataList, 5, TimeUnit.MINUTES);
		
		/*
		 * for (ReportExpand reportExpand : dataList) {
		 * redisTemplate.opsForList().rightPush("DeviceReportApplyJobList",reportExpand)
		 * ; }
		 */
		 redisTemplate.opsForList().rightPushAll("DeviceReportApplyJobList", dataList);
         redisTemplate.expire("DeviceReportApplyJobList", 5, TimeUnit.MINUTES);
	}

	/**
	 * 更新生成文件状态和文件名称
	 */
	@Override
	@Transactional(readOnly = false)
	public void updateDeviceReportApply(TbDeviceReportApply tbDeviceReportApply) {
		reportDao.updateDeviceReportApply(tbDeviceReportApply);
	}

	/**
	 * 根据申请id获取设备信息申请对象
	 */
	@Override
	@Transactional(readOnly = false)
	public TbDeviceReportApply getDeviceReportApply(Long nReportApply) {
		return reportDao.getDeviceReportApply(nReportApply);
	}
	
	 /**
     * 处理总页数
     *
     * @param totalSize
     * @return
     */
    private Integer getTotalPage(Integer totalSize) {
        return (totalSize % MAX_BATCH_QUERY_SIZE == 0)
                ? (totalSize / MAX_BATCH_QUERY_SIZE)
                : (totalSize / MAX_BATCH_QUERY_SIZE + 1);
    }
 
    /**
     * 获取页数
     *
     * @param totalPage
     * @return
     */
    private List<Integer> getPageNumbers(Integer totalPage) {
    	Integer pageNumber = 1;
        List<Integer> pageNumbers = Lists.newArrayList();
        while (pageNumber <= totalPage) {
            pageNumbers.add(pageNumber++);
        }
        return pageNumbers;
    }
}
