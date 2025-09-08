package com.routdata.consumemanage.web.controller.stock;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.routdata.consumemanage.api.baseinfo.dto.TbClassInfo;
import com.routdata.consumemanage.api.baseinfo.dto.TbNameInfo;
import com.routdata.consumemanage.api.baseinfo.dto.TbPlaceInfo;
import com.routdata.consumemanage.api.baseinfo.dto.TbSupplierInfo;
import com.routdata.consumemanage.api.stock.dto.DeviceInfoQuery;
import com.routdata.consumemanage.api.stock.dto.TbBarcodeType;
import com.routdata.consumemanage.api.stock.dto.TbDeviceDelApply;
import com.routdata.consumemanage.api.stock.dto.TbDeviceInfo;
import com.routdata.consumemanage.api.stock.service.IDeviceInfoBatchAddService;
import com.routdata.consumemanage.api.stock.service.IDeviceInfoService;
import com.routdata.consumemanage.api.transfer.dto.OrgTree;
import com.routdata.consumemanage.api.util.FileUtils;
import com.routdata.consumemanage.web.controller.util.Constants;
import com.routdata.consumemanage.web.controller.util.ExcFileUtils;
import com.routdata.consumemanage.web.controller.util.PoiUtils;
import com.routdata.consumemanage.web.controller.util.Utils;
import com.routdata.rd3f.core.api.common.dto.Page;
import com.routdata.rd3f.core.api.common.exception.ServiceException;
import com.routdata.rd3f.core.api.common.utils.AesUtils;
import com.routdata.rd3f.core.api.common.utils.DateUtils;
import com.routdata.rd3f.core.api.common.utils.Encodes;
import com.routdata.rd3f.core.api.common.utils.Global;
import com.routdata.rd3f.core.api.common.utils.StringUtils;
import com.routdata.rd3f.core.api.org.dto.OrgPerson;
import com.routdata.rd3f.core.api.org.dto.VDepartment;
import com.routdata.rd3f.core.api.org.service.IOrgDepartmentService;
import com.routdata.rd3f.core.web.common.utils.UserUtils;
import com.routdata.rd3f.core.web.common.web.BaseController;

/**
 * 设备信息操作WEB层控制类
 * 
 * @author: gaos
 * @date: 2021年7月7日
 */
@Controller
@RequestMapping(value = "${adminPath}/consumemanage/deviceinfo")
public class DeviceInfoController extends BaseController {
	public static final Logger LOGGER = LoggerFactory.getLogger(DeviceInfoController.class);

	private static final String TITLE_NAME = "序号,设备类别,设备目录,设备分类,卡片编号,条码编号,固定资产编号,品牌,型号,含税价格,不含税价格,资产原值,购置时间,使用日期,使用人,已使用年限,建档时间,供应厂商,使用(存放)地点,地点补充,分账类型,保修信息,备注,工程名称,工程文件号,用途,报账单编号,资产来源";
	private static final String SAVE_ONE = "1"; // 保存一条
	private static final String SAVE_SAME = "2"; // 保存相同一条
	private static final String SAVE_BATCH = "3"; // 保存多条
	private static final String PLACE_LIST = "placeList";
	private static final String EDIT_SUCCESS = "修改成功!";
	private static final String EDIT_FAIL = "修改失败!";
	private static final String SAVE_SUCCESS = "保存成功";
	private static final String SAVE_FAIL = "保存失败";
	private static final String ILLEGAL_OPER = "非法操作";
	private static final String DECICE_CODE_EXIST = "固定资产编号已存在,不能重复";
	private static final String REDIRECT = "redirectUrl";
	private static final String REDIRECT_URL = "/consumemanage/deviceinfo/list";
	private static final String CONTENT = "content";
	private static final String SUFFIX_TYPE = ".xlsx";
	private static final String REPEAT = "重复<br/>";
	private static final String NO_IMPORT_EXE = "没有上传导入文件";
	private static final String EXE_FORMAT_ERROR = "文件格式不对,请重新上传!";
	private static final String EXE_CONTENT_EMPTY = "表格内容为空";
	private static final String CAR_NUM = "卡片编号:";
	private static final String BAR_CODE = "条码编号:";
	private static final String PARAM_QHEX = "param_qhex";
	private static final List<String> assetSourceList = Arrays.asList("新增购入", "在建工程转入", "调拨转入", "接受捐赠", "资产盘盈", "其他");

	@Autowired
	IDeviceInfoService deviceInfoService;
	@Autowired
	IOrgDepartmentService orgDepartmentService;
	@Autowired
	IDeviceInfoBatchAddService deviceInfoBatchAddService;

	@Override
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		super.initBinder(binder);
		binder.setDisallowedFields(new String[] { "" });
	}

	/**
	 * 设备入库信息列表
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "list")
	@RequiresPermissions(value = "user")
	public String list(HttpServletRequest request, Model model, @ModelAttribute("query") DeviceInfoQuery query,
			String qhex) throws IOException {
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		if (StringUtils.isNotBlank(qhex)) {
			query = Encodes.deserializeHex(qhex); // 反序列化查询对象
		}
		// 所属机构不为空赋值为所属机构,为空为当前登录机构
		query.setDeptCode(
				StringUtils.isNotBlank(query.getBlongDeptCode()) ? query.getBlongDeptCode() : department.getDeptCode());
		Page<TbDeviceInfo> page = deviceInfoService.findPageForDeviceInfo(query);
		model.addAttribute("page", page);
		model.addAttribute("user", user);
		model.addAttribute("department", department);
		model.addAttribute(PLACE_LIST,
				deviceInfoService.getPlaceInfoBySearch(
						StringUtils.isNotBlank(query.getBlongDeptCode()) ? query.getBlongDeptCode()
								: department.getDeptCode()));
		model.addAttribute("query", query);
		return "consumemanage/deviceinfo/deviceinfo_list";
	}

	/**
	 * 去新增设备页面
	 *
	 */
	@RequiresPermissions(value = "deviceinfo:toAdd")
	@RequestMapping(value = "toAdd")
	public String toAddDeviceinfo(Model model, TbDeviceInfo tbDeviceInfo, String qhex) {
		VDepartment department = UserUtils.getDept();
		model.addAttribute("supplierList", this.getSupplierInfo());
		model.addAttribute(PLACE_LIST, deviceInfoService.getPlaceInfoLimitUnderDept(department.getDeptCode()));
		model.addAttribute("deviceInfo", tbDeviceInfo);
		model.addAttribute("tbBarcodeType", deviceInfoService.getBarcodeType(department.getDeptCode()));
		model.addAttribute("nameList", deviceInfoService.getNameInfo());
		model.addAttribute("qhex", qhex);
		return "consumemanage/deviceinfo/deviceinfo_add";
	}

	/**
	 * 新增设备入库信息 insertType 保存类型(1-1条 2-同类连续 3-批量) insertNumber 保存条数
	 */
	@RequiresPermissions(value = "deviceinfo:toAdd")
	@RequestMapping(value = "saveDeviceinfo")
	public String saveDeviceinfo(HttpServletRequest request, Model model, TbDeviceInfo tbDeviceInfo, String qhex,
			@RequestParam(value = "insertType", required = false) String insertType,
			@RequestParam(value = "insertNumber", required = false) String insertNumber) {
		String msg = SAVE_SUCCESS;
		Integer number = 1;
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		if (StringUtils.isNotBlank(insertNumber)) {
			number = Integer.valueOf(insertNumber);
		}
		tbDeviceInfo.setvCreateOpt(user.getPersonCode()); // 创建人
		tbDeviceInfo.setvCreateDept(department.getDeptCode()); // 创建机构
		// tbDeviceInfo.setnUseState((long) 0); // 使用状态(0-闲置 1-使用)
		tbDeviceInfo.setnState((long) 1); // 设备状态(1-正常 2-故障 3-报废)
		switch (insertType) {
		case SAVE_ONE:
			try {
				this.ckSaveDecCodeExist(tbDeviceInfo.getvDeciceCode()); // 校验资产编号
				this.setBarcodeValByType(tbDeviceInfo, department);
				tbDeviceInfo.setvCardNum(getVcardCode(department));
				deviceInfoService.saveDeviceInfoToApproval(tbDeviceInfo, department);
			} catch (Exception e) {
				msg = SAVE_FAIL;
			}
			addMessage(model, msg);
			break;
		case SAVE_SAME:
			try {
				this.ckSaveDecCodeExist(tbDeviceInfo.getvDeciceCode()); // 校验资产编号
				this.setBarcodeValByType(tbDeviceInfo, department);
				tbDeviceInfo.setvCardNum(getVcardCode(department));
				deviceInfoService.saveDeviceInfoToApproval(tbDeviceInfo, department);
			} catch (Exception e) {
				msg = SAVE_FAIL;
			}
			addMessage(model, msg);
			this.setBackHandBarCode(msg, tbDeviceInfo);
			return this.toAddDeviceinfo(model, tbDeviceInfo, qhex);
		case SAVE_BATCH:
			try {
				List<TbDeviceInfo> batchList = new ArrayList<>();
				// 获取批量保存数据
				this.getBatchListByBarType(tbDeviceInfo, batchList, department, number);
				deviceInfoService.batchSaveDeviceInfoToApproval(batchList, department, tbDeviceInfo.getnBarcodeType());
			} catch (Exception e) {
				msg = SAVE_FAIL;
			}
			addMessage(model, msg);
			break;
		default:
			break;
		}
		if (msg.equals(SAVE_FAIL)) {
			return GLOBAL_ERROR;
		}
		model.addAttribute(REDIRECT, Global.getAdminPath() + REDIRECT_URL);
		model.addAttribute(PARAM_QHEX, qhex);
		return GLOBAL_SUCCESS;

	}

	/**
	 * 为同类设备连续加入回显手工录入条码赋值
	 */
	private void setBackHandBarCode(String msg, TbDeviceInfo tbDeviceInfo) {
		if (msg.equals(SAVE_SUCCESS)) {
			tbDeviceInfo.setvDeciceCode(null); // 保存成功将下一次的资产编号清空回显
			tbDeviceInfo.setHandBarCode(null); // 保存成功将下一次的条码编号清空回显,现支持字母无法递增
		}
	}

	/**
	 * 根据手工录入和自动生成条码为设备条码赋值 手工录入条码可以为空
	 */
	private void setBarcodeValByType(TbDeviceInfo tbDeviceInfo, VDepartment department) {
		if (tbDeviceInfo.getnBarcodeType().equals(0L)) { // 手工录入
			if (StringUtils.isNotBlank(tbDeviceInfo.getHandBarCode())) {
				tbDeviceInfo.setvBarCode(tbDeviceInfo.getHandBarCode());
			}
		} else { // 自动生成
			tbDeviceInfo.setvBarCode(getVbarCode(department.getDsjbh()));
		}
	}

	/**
	 * 根据手工录入和自动生成条码获取要批量保存的数据 手工录入条码可以为空,先区分手工还是自动生成,
	 * 手工录入再看是否为空，如果为空，则不需要给条码编号字段赋值即可;如果不为空,生成待保存的所有条码,再赋值给条码编号字段
	 */
	private void getBatchListByBarType(TbDeviceInfo tbDeviceInfo, List<TbDeviceInfo> batchList, VDepartment department,
			Integer number) {
		String cardNoSeqName = deviceInfoService.getCardNoSeqName(department.getDsjbh());
		if (tbDeviceInfo.getnBarcodeType().equals(0L)) { // 手工录入
			if (StringUtils.isNotBlank(tbDeviceInfo.getHandBarCode())) {
				List<String> bList = getHandBarcodeList(tbDeviceInfo.getHandBarCode(), number);
				for (String bCode : bList) {
					TbDeviceInfo tbDeviceInfo1 = new TbDeviceInfo();
					BeanUtils.copyProperties(tbDeviceInfo, tbDeviceInfo1);
					tbDeviceInfo1.setvBarCode(bCode);
					tbDeviceInfo1.setvCardNum(getVcardCodeNew(department, cardNoSeqName));
					batchList.add(tbDeviceInfo1);
				}
			} else {
				for (int i = 0; i < number; i++) {
					TbDeviceInfo tbDeviceInfo2 = new TbDeviceInfo();
					BeanUtils.copyProperties(tbDeviceInfo, tbDeviceInfo2);
					tbDeviceInfo2.setvCardNum(getVcardCodeNew(department, cardNoSeqName));
					batchList.add(tbDeviceInfo2);
				}
			}
		} else { // 自动生成
			String barCodeSeqName = deviceInfoService.getBarCodeSeqName(department.getDsjbh());
			for (int i = 0; i < number; i++) {
				TbDeviceInfo tbDeviceInfo3 = new TbDeviceInfo();
				BeanUtils.copyProperties(tbDeviceInfo, tbDeviceInfo3);
				tbDeviceInfo3.setvBarCode(getVbarCodeNew(department.getDsjbh(), barCodeSeqName));
				tbDeviceInfo3.setvCardNum(getVcardCodeNew(department, cardNoSeqName));
				batchList.add(tbDeviceInfo3);
			}
		}
	}

	/*
	 * 根据序列和规则生成条码编号
	 */
	private String getVbarCode(String dsjCode) {
		String seqName = deviceInfoService.getBarCodeSeqName(dsjCode);
		Integer vBarCodeSeq = Integer.valueOf(deviceInfoService.getVbarCodeSeq(seqName));
		String vPapaValue = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, dsjCode);
		DecimalFormat df = new DecimalFormat(Constants.NUMBER_SUFF);
		return vPapaValue + df.format(vBarCodeSeq);
	}

	/*
	 * 根据序列和规则生成条码编号new
	 */
	private String getVbarCodeNew(String dsjCode, String seqName) {
		Integer vBarCodeSeq = Integer.valueOf(deviceInfoService.getVbarCodeSeq(seqName));
		String vPapaValue = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, dsjCode);
		DecimalFormat df = new DecimalFormat(Constants.NUMBER_SUFF);
		return vPapaValue + df.format(vBarCodeSeq);
	}

	/*
	 * 根据序列和规则生成卡片编号
	 */
	private String getVcardCode(VDepartment department) {
		String seqName = deviceInfoService.getCardNoSeqName(department.getDsjbh());
		Integer vCardCodeSeq = Integer.valueOf(deviceInfoService.getVcardCodeSeq(seqName));
		DecimalFormat df = new DecimalFormat(Constants.NUMBER_SUFF);
		return department.getDeptCode() + df.format(vCardCodeSeq);
	}

	/*
	 * 根据序列和规则生成卡片编号new
	 */
	private String getVcardCodeNew(VDepartment department, String seqName) {
		Integer vCardCodeSeq = Integer.valueOf(deviceInfoService.getVcardCodeSeq(seqName));
		DecimalFormat df = new DecimalFormat(Constants.NUMBER_SUFF);
		return department.getDeptCode() + df.format(vCardCodeSeq);
	}

	/**
	 * 获取条码编号
	 */
	@ResponseBody
	@RequestMapping(value = { "getVbarCodeEdit" }, method = RequestMethod.POST)
	public String getVbarCodeEdit(HttpServletResponse response) {
		VDepartment department = UserUtils.getDept();
		String barCode = this.getVbarCode(department.getDsjbh());
		return super.renderJson(response, barCode);
	}

	/**
	 * 获取卡片编号
	 */
	@ResponseBody
	@RequestMapping(value = { "getVcardCodeEdit" }, method = RequestMethod.POST)
	public String getVcardCodeEdit(HttpServletResponse response) {
		VDepartment department = UserUtils.getDept();
		String cardCode = this.getVcardCode(department);
		return super.renderJson(response, cardCode);
	}

	/**
	 * 查看设备详情
	 *
	 */
	@RequiresPermissions(value = "deviceinfo:toView")
	@RequestMapping(value = "toView")
	public String toView(Model model, @RequestParam(value = "nDeviceInfo", required = true) String nDeviceInfo) {
		OrgPerson user = UserUtils.getUser();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), nDeviceInfo);// 解密
		TbDeviceInfo tbDeviceInfo = deviceInfoService.get(Long.valueOf(ndeviceInfo));
		this.getTbDeviceInfo(model, tbDeviceInfo, user);
		return "consumemanage/deviceinfo/deviceinfo_view";
	}

	/**
	 * 去修改设备页面
	 *
	 */
	@RequiresPermissions(value = "deviceinfo:toEdit")
	@RequestMapping(value = "toEdit")
	public String toEdit(Model model, @RequestParam(value = "cryDeviceInfo", required = true) String cryDeviceInfo,
			@ModelAttribute("qhex") String qhex) {
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfo);// 解密
		TbDeviceInfo tbDeviceInfo = deviceInfoService.get(Long.valueOf(ndeviceInfo));
		// 取消创建机构才能修改限制(22/5/15)
		if (tbDeviceInfo == null || !tbDeviceInfo.getvRepDeptCode().equals(department.getDeptCode())) {
			addMessage(model, ILLEGAL_OPER);
			return GLOBAL_ERROR;
		}
		if (tbDeviceInfo.getnTmpState().equals(2L)) {
			addMessage(model, "该设备处于修改审批流程");
			return GLOBAL_ERROR;
		}
		if (tbDeviceInfo.getnTmpState().equals(1L)) {
			addMessage(model, "该设备处于设备调拨流程");
			return GLOBAL_ERROR;
		}
		if (deviceInfoService.isExistDeviceScrap(Long.valueOf(ndeviceInfo))) {
			addMessage(model, "该设备处于设备报废流程");
			return GLOBAL_ERROR;
		}
		if (deviceInfoService.isExistDeviceDelApply(Long.valueOf(ndeviceInfo))) {
			addMessage(model, "该设备处于删除审批流程");
			return GLOBAL_ERROR;
		}
		this.getTbDeviceInfo(model, tbDeviceInfo, user);
		List<TbNameInfo> nameInfoList = deviceInfoService.getNameInfo();
		model.addAttribute("nameInfoList", nameInfoList);
		model.addAttribute("supplierList", this.getSupplierInfo());
		model.addAttribute(PLACE_LIST, deviceInfoService.getPlaceInfoLimitUnderDept(department.getDeptCode()));
		String cityBarCode = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, department.getDsjbh());
		// 判断条码编号前缀是否是系统自动生成,自动生成的条码不允许修改,1为自动,0为手工
		if (StringUtils.isNotBlank(tbDeviceInfo.getvBarCode()) && tbDeviceInfo.getvBarCode().startsWith(cityBarCode)) {
			model.addAttribute("nBarcodeType", "1");
		} else {
			model.addAttribute("nBarcodeType", "0");
		}
		return "consumemanage/deviceinfo/deviceinfo_edit";
	}

	/**
	 * 根据主键获取设备信息对象
	 */
	private void getTbDeviceInfo(Model model, TbDeviceInfo tbDeviceInfo, OrgPerson user) {
		switch (tbDeviceInfo.getnDeviceType().toString()) { // 1-甲种低值易耗品
															// 2-乙种低值易耗品
															// 3-丙种低值易耗品 4.固定资产
		case "1":
			tbDeviceInfo.setnDeviceTypeName("甲种低值易耗品");
			break;
		case "2":
			tbDeviceInfo.setnDeviceTypeName("乙种低值易耗品");
			break;
		case "3":
			tbDeviceInfo.setnDeviceTypeName("丙种低值易耗品");
			break;
		case "4":
			tbDeviceInfo.setnDeviceTypeName("固定资产");
			break;
		default:
			break;
		}
		String nDclassName = deviceInfoService.getClassInfoByClassId(tbDeviceInfo.getnDclassCode());
		tbDeviceInfo.setnDclassName(nDclassName);
		model.addAttribute("deviceInfo", tbDeviceInfo);
		model.addAttribute("user", user);
	}

	/**
	 * 修改设备信息执行体
	 *
	 */
	@RequiresPermissions(value = "deviceinfo:toEdit")
	@RequestMapping(value = "edit")
	public String edit(Model model, TbDeviceInfo tbDeviceInfo,
			@RequestParam(value = "cryDeviceInfoId", required = true) String cryDeviceInfoId, String qhex) {
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfoId);// 解密
		TbDeviceInfo dbDeviceInfo = deviceInfoService.get(Long.valueOf(ndeviceInfo));
		// 取消创建机构才能修改限制(22/5/15)
		if (dbDeviceInfo == null || !dbDeviceInfo.getvRepDeptCode().equals(department.getDeptCode())) {
			addMessage(model, ILLEGAL_OPER);
			return GLOBAL_ERROR;
		}
		if (dbDeviceInfo.getnTmpState().equals(2L)) {
			addMessage(model, "该设备处于修改审批流程");
			return GLOBAL_ERROR;
		}
		if (dbDeviceInfo.getnTmpState().equals(1L)) {
			addMessage(model, "该设备处于设备调拨流程");
			return GLOBAL_ERROR;
		}
		if (deviceInfoService.isExistDeviceScrap(Long.valueOf(ndeviceInfo))) {
			addMessage(model, "该设备处于设备报废流程");
			return GLOBAL_ERROR;
		}
		if (deviceInfoService.isExistDeviceDelApply(Long.valueOf(ndeviceInfo))) {
			addMessage(model, "该设备处于删除审批流程");
			return GLOBAL_ERROR;
		}
		String cityBarCode = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, department.getDsjbh());
		// 判断条码编号前缀是否是系统自动生成,自动生成的条码不允许修改
		if (tbDeviceInfo.getnBarcodeType().equals(0L) && StringUtils.isNotBlank(dbDeviceInfo.getvBarCode())
				&& dbDeviceInfo.getvBarCode().startsWith(cityBarCode)) {
			addMessage(model, ILLEGAL_OPER);
			return GLOBAL_ERROR;
		}
		tbDeviceInfo.setnDeviceInfo(Long.valueOf(ndeviceInfo));
		tbDeviceInfo.setdModifyOpt(user.getPersonCode());
		tbDeviceInfo.setvModifyDept(department.getDeptCode());
		try {
			// 校验资产编号
			this.ckEditDecCodeExist(tbDeviceInfo.getvDeciceCode(), tbDeviceInfo.getnDeviceInfo());
			deviceInfoService.updateDeviceInfoToApproval(tbDeviceInfo);// 修改
		} catch (Exception e) {
			addMessage(model, "提交修改申请失败");
			return GLOBAL_ERROR;
		}
		addMessage(model, "提交修改申请成功");
		model.addAttribute(REDIRECT, Global.getAdminPath() + REDIRECT_URL);
		model.addAttribute(PARAM_QHEX, qhex);
		return GLOBAL_SUCCESS;
	}

	/**
	 * 去批量修改设备页面
	 *
	 */
	@RequiresPermissions(value = "devinfo:batchupdate")
	@RequestMapping(value = "toBatchEdit")
	public String toBatchEdit(Model model, @RequestParam(value = "cryDevIds", required = true) String[] cryDevIds,
			@ModelAttribute("qhex") String qhex) {
		Map<String, String> hBarCodeMap = new HashMap<>(); // 是否存在要修改手工录入条码的记录，有为0,没有为1
		Integer hInteger = 0; // 记录手工条码修改的记录数
		List<String> devIds = new ArrayList<>();
		TbDeviceInfo tbDeviceInfo = null;
		List<TbDeviceInfo> devInfoList = new ArrayList<>();
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		String cityBarCode = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, department.getDsjbh());
		for (String devId : cryDevIds) {
			devIds.add(AesUtils.getInstance().decrypt(user.getPersonCode(), devId));
		}
		for (String ndeviceInfo : devIds) {
			tbDeviceInfo = deviceInfoService.get(Long.valueOf(ndeviceInfo));
			// 取消创建机构才能修改限制(22/5/15)
			if (tbDeviceInfo == null) {
				addMessage(model, ILLEGAL_OPER);
				return GLOBAL_ERROR;
			}
			// 只能修改属于本机构的设备
			if (!tbDeviceInfo.getvRepDeptCode().equals(department.getDeptCode())) {
				addMessage(model, "只能修改属于本机构的设备");
				return GLOBAL_ERROR;
			}
			if (tbDeviceInfo.getnTmpState().equals(2L)) {
				addMessage(model, "设备处于修改审批流程");
				return GLOBAL_ERROR;
			}
			if (tbDeviceInfo.getnTmpState().equals(1L)) {
				addMessage(model, "设备处于设备调拨流程");
				return GLOBAL_ERROR;
			}
			if(deviceInfoService.isExistDeviceScrap(Long.valueOf(ndeviceInfo))) {
				addMessage(model, "设备处于设备报废流程");
				return GLOBAL_ERROR;
			}
			if(deviceInfoService.isExistDeviceDelApply(Long.valueOf(ndeviceInfo))) {
				addMessage(model, "设备处于删除审批流程");
				return GLOBAL_ERROR;
			}
			// 判断条码编号前缀是否是系统自动生成,自动生成的条码不允许修改
			if (StringUtils.isNotBlank(tbDeviceInfo.getvBarCode())
					&& tbDeviceInfo.getvBarCode().startsWith(cityBarCode)) {
				tbDeviceInfo.setnBarcodeType(1L);
			} else {
				tbDeviceInfo.setnBarcodeType(0L);
				hInteger++;
			}
			devInfoList.add(tbDeviceInfo);
		}
		model.addAttribute("devInfoList", devInfoList);
		if (hInteger > 0) {
			hBarCodeMap.put("hBarCodeFlag", "0");
		} else {
			hBarCodeMap.put("hBarCodeFlag", "1");
		}
		model.addAttribute("hBarCodeMap", hBarCodeMap);
		return "consumemanage/deviceinfo/deviceinfo_batchEdit";
	}

	/**
	 * 保存批量修改设备信息执行体
	 *
	 */
	@RequiresPermissions(value = "devinfo:batchupdate")
	@ResponseBody
	@RequestMapping(value = { "saveBatchEdit" }, method = RequestMethod.POST)
	public Map<String, String> saveBatchEdit(Model model, @RequestBody JSONObject[] obj) {
		Map<String, String> result = new HashMap<>();
		TbDeviceInfo dbDeviceInfo = null;
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		List<TbDeviceInfo> devInfoList = new ArrayList<>();
		for (JSONObject jObject : obj) {
			devInfoList.add(JSON.parseObject(jObject.toJSONString(), TbDeviceInfo.class));
		}
		String cityBarCode = deviceInfoService.getPapaValue(Constants.CITY_BAR_CODE, department.getDsjbh());
		// 验证是否可以修改,有一个不符合就返回
		for (TbDeviceInfo tbDeviceInfo : devInfoList) {
			dbDeviceInfo = deviceInfoService.get(tbDeviceInfo.getnDeviceInfo());
			// 取消创建机构才能修改限制(22/5/15)
			if (dbDeviceInfo == null) {
				result.put("code", "202");
				result.put("msg", ILLEGAL_OPER);
				return result;
			}
			// 只能修改属于本机构的设备
			if (!dbDeviceInfo.getvRepDeptCode().equals(department.getDeptCode())) {
				result.put("code", "203");
				result.put("msg", "只能修改属于本机构的设备");
				return result;
			}
			// 判断条码编号前缀是否是系统自动生成,自动生成的条码不允许修改
			if (tbDeviceInfo.getnBarcodeType().equals(0L) && StringUtils.isNotBlank(dbDeviceInfo.getvBarCode())
					&& dbDeviceInfo.getvBarCode().startsWith(cityBarCode)) {
				result.put("code", "204");
				result.put("msg", ILLEGAL_OPER);
				return result;
			}
			if (dbDeviceInfo.getnTmpState().equals(2L)) {
				result.put("code", "205");
				result.put("msg", "设备处于修改审批流程");
				return result;
			}
			if (dbDeviceInfo.getnTmpState().equals(1L)) {
				result.put("code", "206");
				result.put("msg", "设备处于设备调拨流程");
				return result;
			}
			if(deviceInfoService.isExistDeviceScrap(tbDeviceInfo.getnDeviceInfo())) {
				result.put("code", "207");
				result.put("msg", "设备处于设备报废流程");
				return result;
			}
			if(deviceInfoService.isExistDeviceDelApply(tbDeviceInfo.getnDeviceInfo())) {
				result.put("code", "208");
				result.put("msg", "设备处于删除审批流程");
				return result;
			}
			tbDeviceInfo.setnDplaceId(dbDeviceInfo.getnDplaceId());  //需要将存放地id保存到审批表中
			tbDeviceInfo.setdModifyOpt(user.getPersonCode());   //修改人
			tbDeviceInfo.setvModifyDept(department.getDeptCode());  //修改机构
		}
		try {
			deviceInfoService.saveBatchEditToApproval(devInfoList);// 保存批量修改信息到审批表
			result.put("code", "200");
			result.put("msg", "修改申请成功");
		} catch (Exception e) {
			result.put("code", "201");
			result.put("msg", "修改申请失败");
		}
		return result;
	}

	/**
	 * 去修改设备使用(存放)地
	 */
	@RequiresPermissions(value = "devinfo:EditPlace")
	@RequestMapping("toEditPlace")
	public String toEditPlace(Model model,
			@RequestParam(value = "cryDeviceInfoId", required = true) String cryDeviceInfoId) {
		OrgPerson user = UserUtils.getUser();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfoId);// 解密
		TbDeviceInfo tbDevInfo = deviceInfoService.get(Long.valueOf(ndeviceInfo));
		if (tbDevInfo == null) {
			addMessage(model, ILLEGAL_OPER);
			return GLOBAL_ERROR;
		}
		model.addAttribute("tbDevInfo", tbDevInfo);
		model.addAttribute(PLACE_LIST, this.getPlaceInfo());
		model.addAttribute("user", user);
		return "consumemanage/deviceinfo/deviceinfo_updatePlace";
	}

	/**
	 * 修改设备使用(存放)地
	 *
	 */
	@ResponseBody
	@RequestMapping(value = { "editDevPlace" }, method = RequestMethod.POST)
	public Map<String, String> editDevPlace(Model model, TbDeviceInfo tbDeviceInfo,
			@RequestParam(value = "cryDeviceInfoId", required = true) String cryDeviceInfoId) {
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfoId);// 解密
		tbDeviceInfo.setnDeviceInfo(Long.valueOf(ndeviceInfo));
		tbDeviceInfo.setdModifyOpt(user.getPersonCode());
		tbDeviceInfo.setvModifyDept(department.getDeptCode());
		try {
			deviceInfoService.updateDevicePlace(tbDeviceInfo);// 修改设备存放地
		} catch (Exception e) {
			return super.renderJsonError(EDIT_FAIL);
		}
		return super.renderJsonSuccess(EDIT_SUCCESS);
	}

	/**
	 * 获取设备分类
	 */
	@RequestMapping("toClassInfoTree")
	public String toClassInfoTree(Model model) {
		List<TbClassInfo> list = deviceInfoService.getClassInfo();
		model.addAttribute("list", list);
		return "consumemanage/deviceinfo/classTree";
	}

	/**
	 * 获取设备分类check
	 */
	@RequestMapping("toClassInfoTreeCheck")
	public String toClassInfoTreeCheck(Model model) {
		List<TbClassInfo> list = deviceInfoService.getClassInfoBySearch();
		model.addAttribute("list", list);
		return "consumemanage/deviceinfo/classTreeCheck";
	}

	/**
	 * 获取供应厂商
	 */
	private List<TbSupplierInfo> getSupplierInfo() {
		VDepartment department = UserUtils.getDept();
		return deviceInfoService.getSupplierInfo(department.getDeptCode(), department.getDsjbh());
	}

	/**
	 * 获取设备存放地
	 */
	private List<TbPlaceInfo> getPlaceInfo() {
		VDepartment department = UserUtils.getDept();
		return deviceInfoService.getPlaceInfo(department.getDeptCode());
	}

	/**
	 * 获取存放地
	 */
	@ResponseBody
	@RequestMapping(value = { "getPlaceInfoByCode" }, method = RequestMethod.POST)
	public String getPlaceInfoByCode(HttpServletResponse response,
			@RequestParam(value = "blongDeptCode", required = true) String blongDeptCode) {
		List<TbPlaceInfo> placeInfoList = deviceInfoService.getPlaceInfoBySearch(blongDeptCode);
		return super.renderJson(response, placeInfoList);
	}

	/**
	 * 获取设备目录
	 */
	@ResponseBody
	@RequestMapping(value = { "getNameInfoByClassKey" }, method = RequestMethod.POST)
	public String getNameInfoByClassKey(HttpServletResponse response,
			@RequestParam(value = "classKey", required = false) String classKey) {
		List<TbNameInfo> list = deviceInfoService.getNameInfoByClassKey(classKey);
		return super.renderJson(response, list);
	}

	/**
	 * 获取设备折旧年限
	 */
	@ResponseBody
	@RequestMapping(value = { "getDepYearByKey" }, method = RequestMethod.POST)
	public String getDepYearByKey(HttpServletResponse response,
			@RequestParam(value = "nameKey", required = false) String nameKey) {
		TbNameInfo nameInfo = deviceInfoService.getDepYearByKey(nameKey);
		return super.renderJson(response, nameInfo);
	}

	/**
	 * 补全设备目录查询
	 */
	@ResponseBody
	@RequestMapping(value = { "getDeviceNameByQuery" }, method = RequestMethod.POST)
	public String getDeviceNameByQuery(HttpServletResponse response,
			@RequestParam(value = "query", required = false) String query) {
		List<TbNameInfo> list = deviceInfoService.getDeviceNameByQuery(query);
		return super.renderJson(response, list);
	}

	/**
	 * 补全品牌查询
	 */
	@ResponseBody
	@RequestMapping(value = { "getvBrandByQuery" }, method = RequestMethod.POST)
	public String getvBrandByQuery(HttpServletResponse response,
			@RequestParam(value = "query", required = false) String query) {
		List<TbDeviceInfo> list = deviceInfoService.getvBrandByQuery(query);
		return super.renderJson(response, list);
	}

	/**
	 * 补全规格查询
	 */
	@ResponseBody
	@RequestMapping(value = { "getvSpecificationsByQuery" }, method = RequestMethod.POST)
	public String getvSpecificationsByQuery(HttpServletResponse response,
			@RequestParam(value = "query", required = false) String query) {
		List<TbDeviceInfo> list = deviceInfoService.getvSpecificationsByQuery(query);
		return super.renderJson(response, list);
	}

	/**
	 * 导出excel
	 */
	@RequiresPermissions(value = "deviceinfo:excel")
	@RequestMapping(value = "outPutExcel")
	public void outPutExcel(@ModelAttribute("query") DeviceInfoQuery query, HttpServletResponse response) {
		VDepartment department = UserUtils.getDept();
		// 所属机构不为空赋值为所属机构,为空为当前登录机构
		query.setDeptCode(
				StringUtils.isNotBlank(query.getBlongDeptCode()) ? query.getBlongDeptCode() : department.getDeptCode());
		List<TbDeviceInfo> deviceList = deviceInfoService.getListForDeviceInfo(query);
		String fileName = "设备信息表";
		this.excel(deviceList, fileName, TITLE_NAME, response);
	}

	/**
	 * 描述：创建Excel模板并写入数据
	 *
	 */
	private void excel(List<TbDeviceInfo> list, String fileName, String titleName, HttpServletResponse response) {
		// list是表格数据，fileName是表格名称，titleName是表格字段名称
		OutputStream outPut = null;
		SXSSFWorkbook sfWorkbook = null;
		SXSSFRow sfRow = null;
		CellStyle cStyle = null;
		int number = 0;// 行序号
		try {
			sfWorkbook = new SXSSFWorkbook(100);// 创建 一个excel文档对象
			SXSSFSheet sfSheet = sfWorkbook.createSheet(fileName);// 创建一个工作薄对象
			// 创建大标题
			sfRow = sfSheet.createRow(number);// 创建一个行对象
			sfRow.setHeightInPoints(60);// 设置行高60像素
			cStyle = ExcFileUtils.createTitleStyle(sfWorkbook);
			String[] sfTitle = titleName.split(",");// 表格标题字段名称
			// 合并单元格
			sfSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, sfTitle.length - 1));
			ExcFileUtils.createCell(sfRow, 0, fileName, cStyle);// 创建大标题单元格
			number++;
			// 创建表格标题
			sfRow = sfSheet.createRow(number);// 创建一个行对象
			sfRow.setHeightInPoints(40);// 设置行高40像素
			for (int i = 0; i < sfTitle.length; i++) {
				ExcFileUtils.createCell(sfRow, i, sfTitle[i], cStyle);
				sfSheet.setColumnWidth(i, 5000);// 设置指定的列宽,单位为像素
			}
			// 生成表格内容
			cStyle = ExcFileUtils.createStyle(sfWorkbook);
			for (int i = 0; i < list.size(); i++) {
				TbDeviceInfo deviceInfo = list.get(i);
				if (deviceInfo != null) {
					sfRow = sfSheet.createRow(i + 1 + number);
					int j = 0;
					ExcFileUtils.createCell(sfRow, j++, String.valueOf(i + 1), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnDclassName(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnDeviceName(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnDeviceTypeName(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvCardNum(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvBarCode(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvDeciceCode(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvBrand(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvModel(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnPrice(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnExcludingTaxPrice(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnOriginalPrice(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, DateUtils.formatDate(deviceInfo.getdBuyDate()), cStyle);
					ExcFileUtils.createCell(sfRow, j++, DateUtils.formatDate(deviceInfo.getdInTime()), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvUseOpt(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getUserYear(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, DateUtils.formatDate(deviceInfo.getvDInStockTime()), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnDsupplierName(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnDplaceName(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvAddrMemo(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getnLedgerAccount(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvSource(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvRemarks(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvPname(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvPfno(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvPurpose(), cStyle);
					ExcFileUtils.createCell(sfRow, j++, deviceInfo.getvReimburse(), cStyle);
					ExcFileUtils.createCell(sfRow, j, deviceInfo.getvAssetSourceWords(), cStyle);
				}
			}

			response.setContentType("application/vnd.ms-excel");
			response.setHeader("Content-Disposition", "filename=\""
					+ new String((fileName + SUFFIX_TYPE).getBytes("gb2312"), StandardCharsets.ISO_8859_1) + "\";");
			outPut = response.getOutputStream();
			sfWorkbook.write(outPut);
			outPut.flush();
		} catch (Exception e) {
			LOGGER.info(e.getMessage(), e);
		} finally {
			if (outPut != null) {
				try {
					outPut.close();
				} catch (IOException e) {
					LOGGER.info(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * 提交设备删除申请
	 */
	@RequiresPermissions(value = "deviceinfo:toDelete")
	@RequestMapping(value = { "todelete" }, method = RequestMethod.POST)
	@ResponseBody
	public Map<String, String> toDelete(Model model, @RequestParam(value = "devId", required = true) String devId) {
		Map<String, String> result = new HashMap<>();
		try {
			OrgPerson user = UserUtils.getUser();
			VDepartment department = UserUtils.getDept();
			String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), devId);// 解密

			Long deviceId = Long.valueOf(ndeviceInfo);
			TbDeviceInfo tbDevInfo = deviceInfoService.get(deviceId);

			// 判断该设备的创建和所属机构是否为当前登录机构,会存在有本机构创建调拨到下级机构设备
			if (tbDevInfo == null || !tbDevInfo.getvCreateDept().equals(department.getDeptCode())
					|| !tbDevInfo.getvRepDeptCode().equals(department.getDeptCode())) {
				result.put("code", "202");
				result.put("msg", "提交删除申请失败,设备不存在或只能提交删除申请本机构创建且属于本机构的设备！");
				return result;
			}
			if (deviceInfoService.isExistDeviceScrap(deviceId)) {
				result.put("code", "203");
				result.put("msg", "提交删除申请失败,此设备处于设备报废流程！");
				return result;
			}
			if (deviceInfoService.isExistDeviceDelApply(deviceId)) {
				result.put("code", "204");
				result.put("msg", "提交删除申请失败,此设备处于删除申请流程中,请勿重复提交！");
				return result;
			}
			if (tbDevInfo.getnTmpState().equals(1L)) {
				result.put("code", "205");
				result.put("msg", "提交删除申请失败,此设备处于调拨流程中！");
				return result;
			}
			if (tbDevInfo.getnTmpState().equals(2L)) {
				result.put("code", "206");
				result.put("msg", "提交删除申请失败,此设备处于修改审批流程中！");
				return result;
			}
			TbDeviceDelApply tbDeviceDelApply = new TbDeviceDelApply();
			tbDeviceDelApply.setnDeviceInfo(deviceId);
			tbDeviceDelApply.setvApplyPerson(user.getPersonCode());
			tbDeviceDelApply.setvApplyDept(department.getDeptCode());
			tbDeviceDelApply.setnState(0L); // 待审批
			deviceInfoService.saveDeviceDelApply(tbDeviceDelApply);
			result.put("code", "200");
			result.put("msg", "提交删除申请成功！");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			result.put("code", "201");
			result.put("msg", "提交删除申请失败！");
		}

		return result;
	}

	/**
	 * 提交批量删除申请设备
	 */
	@RequiresPermissions(value = "devinfo:batchDelete")
	@RequestMapping(value = { "tobatchdelete" }, method = RequestMethod.POST)
	@ResponseBody
	public Map<String, String> toBatchDelete(Model model,
			@RequestParam(value = "devIds", required = true) String devIds) {
		Map<String, String> result = new HashMap<>();
		try {
			OrgPerson user = UserUtils.getUser();
			VDepartment department = UserUtils.getDept();

			String[] ids = devIds.split(","); // 将所选id还原为数组
			if (null == ids || ids.length == 0) {
				result.put("code", "202");
				result.put("msg", "提交删除申请失败,未选择设备！");
				return result;
			}

			// 验证是否可以执行删除审批操作，只要一个不符合就返回
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), id);// 解密
					Long deviceId = Long.valueOf(ndeviceInfo);
					TbDeviceInfo tbDevInfo = deviceInfoService.get(deviceId);

					// 判断该设备的创建和所属机构是否为当前登录机构,会存在有本机构创建调拨到下级机构设备
					if (tbDevInfo == null || !tbDevInfo.getvCreateDept().equals(department.getDeptCode())
							|| !tbDevInfo.getvRepDeptCode().equals(department.getDeptCode())) {
						result.put("code", "203");
						result.put("msg", "提交删除申请失败,设备不存在或只能提交删除申请本机构创建且属于本机构的设备！");
						return result;
					}
					if (deviceInfoService.isExistDeviceScrap(deviceId)) {
						result.put("code", "204");
						result.put("msg", "提交删除申请失败,设备处于设备报废流程！");
						return result;
					}
					if (deviceInfoService.isExistDeviceDelApply(deviceId)) {
						result.put("code", "205");
						result.put("msg", "提交删除申请失败,设备处于删除申请流程中,请勿重复提交！");
						return result;
					}
					if (tbDevInfo.getnTmpState().equals(1L)) {
						result.put("code", "206");
						result.put("msg", "提交删除申请失败,此设备处于调拨流程中！");
						return result;
					}
					if (tbDevInfo.getnTmpState().equals(2L)) {
						result.put("code", "207");
						result.put("msg", "提交删除申请失败,此设备处于修改审批流程中！");
						return result;
					}
				}
			}
			List<TbDeviceDelApply> deviceDelApplyList = new ArrayList<>();
			this.getBatchDeviceDelApply(user, department, ids, deviceDelApplyList);
			deviceInfoService.saveBatchDeviceDelApply(deviceDelApplyList);
			result.put("code", "200");
			result.put("msg", "提交删除申请成功！");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			result.put("code", "201");
			result.put("msg", "提交删除申请失败！");
		}
		return result;
	}

	// 获取批量设备删除申请数据
	private void getBatchDeviceDelApply(OrgPerson user, VDepartment department, String[] ids,
			List<TbDeviceDelApply> deviceDelApplyList) {
		for (String id : ids) {
			if (StringUtils.isNotBlank(id)) {
				String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), id);// 解密
				TbDeviceDelApply tbDeviceDelApply = new TbDeviceDelApply();
				tbDeviceDelApply.setnDeviceInfo(Long.valueOf(ndeviceInfo));
				tbDeviceDelApply.setvApplyPerson(user.getPersonCode());
				tbDeviceDelApply.setvApplyDept(department.getDeptCode());
				tbDeviceDelApply.setnState(0L); // 待审批
				deviceDelApplyList.add(tbDeviceDelApply);
			}
		}
	}

	/**
	 * 校验手工录入设备条码(新增)
	 */
	@RequiresPermissions(value = "deviceinfo:toAdd")
	@ResponseBody
	@RequestMapping(value = { "checkHandBarcode" }, method = RequestMethod.POST)
	public Map<String, String> checkHandBarcode(HttpServletResponse response,
			@RequestParam(value = "insertType", required = true) String insertType,
			@RequestParam(value = "handBarCode", required = true) String handBarCode,
			@RequestParam(value = "insertNumber", required = false) String insertNumber) {
		Map<String, String> result = new HashMap<>();
		if (insertType.equals(SAVE_BATCH)) { // 批量加入
			List<String> bList = getHandBarcodeList(handBarCode, Integer.valueOf(insertNumber));
			ckHandBarListLength(bList, result);
			if (result.get("code").equals("1")) {
				result.put("msg", "批量加入手工录入的条码编号长度只能为" + Constants.NUMBER_SUFF.length());
				return result;
			}
			ckHandBarListExist(bList, result);
		} else { // 单个或连续加入
			result.put("code", "0");
			if (deviceInfoService.ckHandBarCodeExist(handBarCode)
					|| deviceInfoService.ckHandBarCodeApprovalExist(handBarCode)) {
				result.put("code", "1");
				result.put("msg", "条码编号已存在，不能重复！");
			}
		}

		return result;
	}

	/**
	 * 获取手工录入条码编号集合,生成的全部条码包含自身
	 */
	private List<String> getHandBarcodeList(String handBarCode, Integer insertNumber) {
		List<String> barList = new ArrayList<>();
		Integer numCode = Integer.valueOf(handBarCode);
		DecimalFormat df = new DecimalFormat(Constants.NUMBER_SUFF);
		for (int i = 0; i < insertNumber; i++) {
			barList.add(df.format(numCode++));
		}
		return barList;
	}

	/**
	 * 校验手工录入条码编号长度
	 */
	private void ckHandBarListLength(List<String> barList, Map<String, String> result) {
		result.put("code", "0");
		for (String barCode : barList) {
			if (barCode.length() != Constants.NUMBER_SUFF.length()) {
				result.put("code", "1");
				break;
			}
		}

	}

	/**
	 * 校验手工录入条码编号是否存在
	 */
	private void ckHandBarListExist(List<String> barList, Map<String, String> result) {
		result.put("code", "0");
		if (deviceInfoService.ckHandBarListExist(barList) || deviceInfoService.ckHandBarListApprovalExist(barList)) {
			result.put("code", "1");
			result.put("msg", "批量保存的条码编号已存在,不能重复！");
		}
	}

	/**
	 * 校验单个设备条码(单个修改)
	 */
	@RequiresPermissions(value = "deviceinfo:toEdit")
	@ResponseBody
	@RequestMapping(value = { "checkHandBarcodeEdit" }, method = RequestMethod.POST)
	public Map<String, String> checkHandBarcodeEdit(HttpServletResponse response,
			@RequestParam(value = "cryDeviceInfoId", required = true) String cryDeviceInfoId,
			@RequestParam(value = "handBarCode", required = true) String handBarCode) {
		Map<String, String> result = new HashMap<>();
		OrgPerson user = UserUtils.getUser();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfoId);// 解密
		if (deviceInfoService.ckHandBarCodeList(handBarCode, Long.valueOf(ndeviceInfo))
				|| deviceInfoService.ckHandBarCodeApprovalExist(handBarCode)) {
			result.put("code", "1");
			result.put("msg", "条码编号已存在，不能重复！");
		} else {
			result.put("code", "0");
		}
		return result;
	}

	/**
	 * 校验批量保存的手工录入条码(不包含本身)
	 */
	@RequiresPermissions(value = "devinfo:batchupdate")
	@ResponseBody
	@RequestMapping(value = { "ckBatchHandCodeEdit" }, method = RequestMethod.POST)
	public Map<String, String> ckBatchHandCodeEdit(Model model, @RequestBody JSONObject[] obj) {
		Map<String, String> result = new HashMap<>();
		List<TbDeviceInfo> devInfoList = new ArrayList<>();
		for (JSONObject jObject : obj) {
			devInfoList.add(JSON.parseObject(jObject.toJSONString(), TbDeviceInfo.class));
		}
		// 验证批量保存的手工录入条码,有一个不符合就返回
		for (TbDeviceInfo tbDeviceInfo : devInfoList) {
			if (tbDeviceInfo.getnBarcodeType().equals(0L) && StringUtils.isNotBlank(tbDeviceInfo.getHandBarCode())) {
				if (deviceInfoService.ckHandBarCodeList(tbDeviceInfo.getHandBarCode(),tbDeviceInfo.getnDeviceInfo())
					|| deviceInfoService.ckHandBarCodeApprovalExist(tbDeviceInfo.getHandBarCode())) {
					result.put("code", "201");
					result.put("msg", tbDeviceInfo.getHandBarCode()+"条码编号已存在,不能重复");
					return result;
				}
			}
		}
		result.put("code", "200");
		return result;
	}

	/**
	 * 校验资产编号(单个新增)
	 */
	@RequiresPermissions(value = "deviceinfo:toAdd")
	@ResponseBody
	@RequestMapping(value = { "checkDeciceCodeSave" }, method = RequestMethod.POST)
	public Map<String, String> checkDeciceCodeSave(HttpServletResponse response,
			@RequestParam(value = "vDeciceCode", required = true) String vDeciceCode) {
		Map<String, String> result = new HashMap<>();
		if (deviceInfoService.ckDeciceCodeExist(vDeciceCode)
				|| deviceInfoService.ckDeciceCodeApprovalExist(vDeciceCode)) {
			result.put("code", "1");
			result.put("msg", DECICE_CODE_EXIST);
		} else {
			result.put("code", "0");
		}
		return result;
	}

	/**
	 * 校验资产编号(单个修改)
	 */
	@RequiresPermissions(value = "deviceinfo:toEdit")
	@ResponseBody
	@RequestMapping(value = { "checkDeciceCodeEdit" }, method = RequestMethod.POST)
	public Map<String, String> checkDeciceCodeEdit(HttpServletResponse response,
			@RequestParam(value = "cryDeviceInfoId", required = true) String cryDeviceInfoId,
			@RequestParam(value = "vDeciceCode", required = true) String vDeciceCode) {
		Map<String, String> result = new HashMap<>();
		OrgPerson user = UserUtils.getUser();
		String ndeviceInfo = AesUtils.getInstance().decrypt(user.getPersonCode(), cryDeviceInfoId);// 解密
		if (deviceInfoService.ckDeciceCodeList(vDeciceCode, Long.valueOf(ndeviceInfo))
				|| deviceInfoService.ckDeciceCodeApprovalExist(vDeciceCode)) {
			result.put("code", "1");
			result.put("msg", DECICE_CODE_EXIST);
		} else {
			result.put("code", "0");
		}
		return result;
	}

	/**
	 * 校验资产编号是否存在(后台新增时)
	 */
	private void ckSaveDecCodeExist(String vDeciceCode) {
		if (StringUtils.isNotBlank(vDeciceCode) && (deviceInfoService.ckDeciceCodeExist(vDeciceCode)
				|| deviceInfoService.ckDeciceCodeApprovalExist(vDeciceCode))) {
			throw new ServiceException(DECICE_CODE_EXIST);
		}
	}

	/**
	 * 校验资产编号是否存在(后台修改时)
	 */
	private void ckEditDecCodeExist(String vDeciceCode, Long nDeviceInfo) {
		if (StringUtils.isNotBlank(vDeciceCode) && (deviceInfoService.ckDeciceCodeList(vDeciceCode, nDeviceInfo)
				|| deviceInfoService.ckDeciceCodeApprovalExist(vDeciceCode))) {
			throw new ServiceException(DECICE_CODE_EXIST);
		}
	}

	/**
	 * 校验批量保存的资产编号(不包含本身)
	 */
	@RequiresPermissions(value = "devinfo:batchupdate")
	@ResponseBody
	@RequestMapping(value = { "ckBatchDeciceCodeEdit" }, method = RequestMethod.POST)
	public Map<String, String> ckBatchDeciceCodeEdit(Model model, @RequestBody JSONObject[] obj) {
		Map<String, String> result = new HashMap<>();
		List<TbDeviceInfo> devInfoList = new ArrayList<>();
		for (JSONObject jObject : obj) {
			devInfoList.add(JSON.parseObject(jObject.toJSONString(), TbDeviceInfo.class));
		}
		// 验证批量保存的资产编号,有一个不符合就返回
		for (TbDeviceInfo tbDeviceInfo : devInfoList) {
			if (StringUtils.isNotBlank(tbDeviceInfo.getvDeciceCode())) {
				if (deviceInfoService.ckDeciceCodeList(tbDeviceInfo.getvDeciceCode(), tbDeviceInfo.getnDeviceInfo())
					|| deviceInfoService.ckDeciceCodeApprovalExist(tbDeviceInfo.getvDeciceCode())) {
					result.put("code", "201");
					result.put("msg", tbDeviceInfo.getvDeciceCode()+DECICE_CODE_EXIST);
					return result;
				}
			}
		}
		result.put("code", "200");
		return result;
	}

	/**
	 * 根据目录id获取分类信息
	 */
	@RequiresPermissions(value = "user")
	@ResponseBody
	@RequestMapping(value = { "getClassInfoByNameKey" }, method = RequestMethod.POST)
	public String getClassInfoByNameKey(HttpServletResponse response,
			@RequestParam(value = "nameKey", required = true) String nameKey) {
		TbClassInfo tbClassInfo = deviceInfoService.getClassInfoByNameKey(nameKey);
		return super.renderJson(response, tbClassInfo);
	}

	/**
	 * 描述：进入固定资产编号导入页面
	 */
	@RequiresPermissions(value = "devinfo:impdecicode")
	@RequestMapping(value = { "toImportDeciCode" })
	public String toImportDeciCode(Model model, String qhex) {
		model.addAttribute("qhex", qhex);
		return "consumemanage/deviceinfo/deviceinfo_impDeciCode";
	}

	/**
	 * 描述：进入条码编号导入页面
	 */
	@RequiresPermissions(value = "devinfo:impbarcode")
	@RequestMapping(value = { "toImportBarCode" })
	public String toImportBarCode(Model model, String qhex) {
		model.addAttribute("qhex", qhex);
		return "consumemanage/deviceinfo/deviceinfo_impBarCode";
	}

	/**
	 * 解析excel文件
	 */
	@RequiresPermissions(value = "devinfo:impdecicode")
	@RequestMapping(value = { "importimportDeciCodeFile" }, method = RequestMethod.POST)
	public String importDeciCodeFile(MultipartFile file, HttpServletRequest request, HttpServletResponse response,
			Model model, String qhex) {
		List<TbDeviceInfo> upExcList = new ArrayList<>();
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		StringBuilder errMsg = new StringBuilder();
		boolean isAllRight = true; // 验证所有数据是否正确
		// 校验是否已上传文件
		if (file.isEmpty()) {
			model.addAttribute(CONTENT, NO_IMPORT_EXE);
			return toImportDeciCode(model, qhex);
		}
		if (!file.getOriginalFilename().endsWith(SUFFIX_TYPE)) {
			model.addAttribute(CONTENT, EXE_FORMAT_ERROR);
			return toImportDeciCode(model, qhex);
		}
		// 读取excel文件
		try {
			List<List<String>> dataList = PoiUtils.readExcel(file, 1, 0, 1); // 参数分别为:文件,数据开始行,数据开始列,数据结束列
			if (!dataList.isEmpty()) {
				List<String> carNumList = new ArrayList<>(); // 存放卡片编号list
				List<String> deciCodeList = new ArrayList<>(); // 存放固定资产编号list
				List<String> tempDeciCodeList = new ArrayList<>(); // 存放不为空的固定资产编号list
				for (int i = 0; i < dataList.size(); i++) {
					if (StringUtils.isBlank(dataList.get(i).get(0))) {
						isAllRight = false;
						errMsg.append("第" + (i + 2) + "行数据 卡片编号为空<br/>");
					} else {
						carNumList.add(dataList.get(i).get(0));
					}
					if (StringUtils.isNotBlank(dataList.get(i).get(1))) {
						tempDeciCodeList.add(dataList.get(i).get(1));
					}
					deciCodeList.add(dataList.get(i).get(1));
				}
				if (isAllRight) {
					// 校验卡片编号 和 固定资产编号是否有重复
					List<String> dupCarNumList = Utils.getDuplicateElements(carNumList);
					if (!dupCarNumList.isEmpty()) {
						for (String dcString : dupCarNumList) {
							errMsg.append(CAR_NUM + dcString + REPEAT);
						}
						model.addAttribute(CONTENT, errMsg.toString());
						return toImportDeciCode(model, qhex);
					}
					if (!tempDeciCodeList.isEmpty()) {
						List<String> dupTempDeciCodeList = Utils.getDuplicateElements(tempDeciCodeList);
						if (!dupTempDeciCodeList.isEmpty()) {
							for (String dtcString : dupTempDeciCodeList) {
								errMsg.append("固定资产编号:" + dtcString + REPEAT);
							}
							model.addAttribute(CONTENT, errMsg.toString());
							return toImportDeciCode(model, qhex);
						}
					}
					// 根据当前登录机构获取当前地市局
					VDepartment dsjDepartment = orgDepartmentService.getVDepartmentByCode(department.getDsjbh());
					// 校验卡片编号 在库里是否存在,校验固定资产编号是否在库中已使用
					for (int i = 0; i < carNumList.size(); i++) {
						if (deviceInfoService.ckCarNumExistUnLimitDept(carNumList.get(i), dsjDepartment.getTreeNo())) {
							if (StringUtils.isNotBlank(deciCodeList.get(i)) && deviceInfoService
									.ckDecCodeExistByCarNum(carNumList.get(i), deciCodeList.get(i))) {
								isAllRight = false;
								errMsg.append("第" + (i + 2) + "行数据 固定资产编号已被使用<br/>");
							}
						} else {
							isAllRight = false;
							errMsg.append("第" + (i + 2) + "行数据 卡片编号不存在<br/>");
						}
					}
					if (isAllRight) {
						// 根据卡片编号修改固定资产编号
						for (List<String> list : dataList) {
							TbDeviceInfo dbDeviceInfo = deviceInfoService.getDeviceInfoByCarNumUnLimitDept(list.get(0),
									dsjDepartment.getTreeNo());
							dbDeviceInfo.setvDeciceCode(list.get(1));
							dbDeviceInfo.setdModifyOpt(user.getPersonCode());
							dbDeviceInfo.setvModifyDept(department.getDeptCode());
							upExcList.add(dbDeviceInfo);
						}
						try {
							deviceInfoService.updateDecCodeByCarNumExcel(upExcList);
						} catch (Exception e) {
							addMessage(model, EDIT_FAIL);
							return GLOBAL_ERROR;
						}
					} else {
						model.addAttribute(CONTENT, errMsg.toString());
						return toImportDeciCode(model, qhex);
					}
				} else {
					model.addAttribute(CONTENT, errMsg.toString());
					return toImportDeciCode(model, qhex);
				}
			} else {
				model.addAttribute(CONTENT, EXE_CONTENT_EMPTY);
				return toImportDeciCode(model, qhex);
			}

		} catch (UnsupportedEncodingException e) {
			LOGGER.info(e.getMessage(), e);
			model.addAttribute(CONTENT, "读取文件失败!");
			return toImportDeciCode(model, qhex);
		} catch (IOException e) {
			LOGGER.info(e.getMessage(), e);
			model.addAttribute(CONTENT, "读取解析文件失败:请检查文件格式!");
			return toImportDeciCode(model, qhex);
		}
		addMessage(model, "已成功修改" + upExcList.size() + "条数据");
		model.addAttribute(REDIRECT, Global.getAdminPath() + "/consumemanage/report/deviceSituationList");
		model.addAttribute(PARAM_QHEX, qhex);
		return GLOBAL_SUCCESS;
	}

	/**
	 * 解析excel文件
	 */
	@RequiresPermissions(value = "devinfo:impbarcode")
	@RequestMapping(value = { "importimportBarCodeFile" }, method = RequestMethod.POST)
	public String importimportBarCodeFile(MultipartFile file, HttpServletRequest request, HttpServletResponse response,
			Model model, String qhex) {
		List<TbDeviceInfo> upExcList = new ArrayList<>();
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		StringBuilder errMsg = new StringBuilder();
		boolean isAllRight = true; // 验证所有数据是否正确
		// 校验是否已上传文件
		if (file.isEmpty()) {
			model.addAttribute(CONTENT, NO_IMPORT_EXE);
			return toImportBarCode(model, qhex);
		}
		if (!file.getOriginalFilename().endsWith(SUFFIX_TYPE)) {
			model.addAttribute(CONTENT, EXE_FORMAT_ERROR);
			return toImportBarCode(model, qhex);
		}
		// 读取excel文件
		try {
			List<List<String>> dataList = PoiUtils.readExcel(file, 1, 0, 1); // 参数分别为:文件,数据开始行,数据开始列,数据结束列
			if (!dataList.isEmpty()) {
				List<String> carNumList = new ArrayList<>(); // 存放卡片编号list
				List<String> barCodeList = new ArrayList<>(); // 存放条码编号list
				List<String> tempBarCodeList = new ArrayList<>(); // 存放不为空的条码编号list
				for (int i = 0; i < dataList.size(); i++) {
					if (StringUtils.isBlank(dataList.get(i).get(0))) {
						isAllRight = false;
						errMsg.append("第" + (i + 2) + "行数据 卡片编号为空<br/>");
					} else {
						carNumList.add(dataList.get(i).get(0));
					}
					if (StringUtils.isNotBlank(dataList.get(i).get(1))) {
						tempBarCodeList.add(dataList.get(i).get(1));
					}
					barCodeList.add(dataList.get(i).get(1));
				}
				if (isAllRight) {
					// 校验卡片编号 和 条码编号是否有重复
					List<String> dupCarNumList = Utils.getDuplicateElements(carNumList);
					if (!dupCarNumList.isEmpty()) {
						for (String dcString : dupCarNumList) {
							errMsg.append(CAR_NUM + dcString + REPEAT);
						}
						model.addAttribute(CONTENT, errMsg.toString());
						return toImportBarCode(model, qhex);
					}
					if (!tempBarCodeList.isEmpty()) {
						List<String> dupTempBarCodeList = Utils.getDuplicateElements(tempBarCodeList);
						if (!dupTempBarCodeList.isEmpty()) {
							for (String dtcString : dupTempBarCodeList) {
								errMsg.append(BAR_CODE + dtcString + REPEAT);
							}
							model.addAttribute(CONTENT, errMsg.toString());
							return toImportBarCode(model, qhex);
						}
					}
					// 校验条码编号长度
					if (!barCodeList.isEmpty()) {
						for (int i = 0; i < barCodeList.size(); i++) {
							if (StringUtils.isNotBlank(barCodeList.get(i))
									&& barCodeList.get(i).length() != Constants.NUMBER_SUFF.length()) {
								isAllRight = false;
								errMsg.append(
										"第" + (i + 2) + "行数据 条码编号长度不为:" + Constants.NUMBER_SUFF.length() + "<br/>");
							}
						}
					}
					if (isAllRight) {
						// 校验卡片编号 在库里是否存在,校验条码编号是否已有值,是否在库中已使用
						for (int i = 0; i < carNumList.size(); i++) {
							if (deviceInfoService.ckCarNumExist(carNumList.get(i), department.getDeptCode())) {
								if (deviceInfoService.ckHasBarCodeByCarNum(carNumList.get(i))) {
									if (StringUtils.isNotBlank(barCodeList.get(i)) && deviceInfoService
											.ckBarCodeExistByCarNum(carNumList.get(i), barCodeList.get(i))) {
										isAllRight = false;
										errMsg.append("第" + (i + 2) + "行数据 条码编号已被使用<br/>");
									}
								} else {
									isAllRight = false;
									errMsg.append("第" + (i + 2) + "行数据 该卡片编号的设备,条码编号已有值,不允许修改<br/>");
								}

							} else {
								isAllRight = false;
								errMsg.append("第" + (i + 2) + "行数据 卡片编号不存在<br/>");
							}
						}

						if (isAllRight) {
							// 根据卡片编号修改条码编号
							for (List<String> list : dataList) {
								TbDeviceInfo dbDeviceInfo = deviceInfoService.getDeviceInfoByCarNum(list.get(0),
										department.getDeptCode());
								dbDeviceInfo.setvBarCode(list.get(1));
								dbDeviceInfo.setdModifyOpt(user.getPersonCode());
								dbDeviceInfo.setvModifyDept(department.getDeptCode());
								upExcList.add(dbDeviceInfo);
							}
							try {
								deviceInfoService.updateBarCodeByCarNumExcel(upExcList);
							} catch (Exception e) {
								addMessage(model, EDIT_FAIL);
								return GLOBAL_ERROR;
							}
						} else {
							model.addAttribute(CONTENT, errMsg.toString());
							return toImportBarCode(model, qhex);
						}
					} else {
						model.addAttribute(CONTENT, errMsg.toString());
						return toImportBarCode(model, qhex);
					}
				} else {
					model.addAttribute(CONTENT, errMsg.toString());
					return toImportBarCode(model, qhex);
				}
			} else {
				model.addAttribute(CONTENT, EXE_CONTENT_EMPTY);
				return toImportBarCode(model, qhex);
			}

		} catch (UnsupportedEncodingException e) {
			LOGGER.info(e.getMessage(), e);
			model.addAttribute(CONTENT, "读取文件失败!");
			return toImportBarCode(model, qhex);
		} catch (IOException e) {
			LOGGER.info(e.getMessage(), e);
			model.addAttribute(CONTENT, "读取解析文件失败:请检查文件格式!");
			return toImportBarCode(model, qhex);
		}
		addMessage(model, "已成功修改" + upExcList.size() + "条数据");
		model.addAttribute(REDIRECT, Global.getAdminPath() + REDIRECT_URL);
		model.addAttribute(PARAM_QHEX, qhex);
		return GLOBAL_SUCCESS;
	}

	/**
	 * 描述；下载模板
	 */
	@RequiresPermissions(value = "user")
	@RequestMapping(value = { "downTemplet" }, method = RequestMethod.POST)
	public void downTemplet(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "tempName", required = true) String tempName,
			@RequestParam(value = "downName", required = true) String downName) {
		String tempPath = request.getSession().getServletContext().getRealPath("") + File.separator + "WEB-INF"
				+ File.separator + "template" + File.separator + tempName + SUFFIX_TYPE;
		FileUtils.downExcelFile(response, tempPath, downName);
	}

	/**
	 * 获取机构树
	 */
	@RequestMapping(value = "toOrgTree")
	public String toOrgTree(Model model) {
		VDepartment department = UserUtils.getDept();
		List<OrgTree> list = deviceInfoService.getOrgTree(department.getTreeNo());
		model.addAttribute("list", list);
		return "consumemanage/deviceinfo/orgTree2";
	}

	/**
	 * 描述：进入设备信息批量导入页面
	 */
	@RequiresPermissions(value = "devinfo:impdevinfo")
	@RequestMapping(value = { "toImportDeviceInfo" })
	public String toImportDeviceInfo(Model model, String qhex) {
		model.addAttribute("qhex", qhex);
		return "consumemanage/deviceinfo/deviceinfo_impDeviceInfo";
	}

	/**
	 * 解析excel文件
	 */
	@RequiresPermissions(value = "devinfo:impdevinfo")
	@RequestMapping(value = { "importDeviceInfoFile" }, method = RequestMethod.POST)
	public String importDeviceInfoFile(MultipartFile file, HttpServletRequest request, HttpServletResponse response,
			Model model, String qhex) {
		List<TbDeviceInfo> upExcList = new ArrayList<>();
		OrgPerson user = UserUtils.getUser();
		VDepartment department = UserUtils.getDept();
		StringBuilder errMsg = new StringBuilder();
		boolean isAllRight = true; // 验证所有数据是否正确
		Pattern pattern1 = Pattern.compile("^(([1-9][0-9]*)|(([0]\\.\\d{1,2}|[1-9][0-9]*\\.\\d{1,2})))$"); // 判断小数点后2位的数字的正则表达式
		// 校验是否已上传文件
		if (file.isEmpty()) {
			model.addAttribute(CONTENT, NO_IMPORT_EXE);
			return toImportDeviceInfo(model, qhex);
		}
		if (!file.getOriginalFilename().endsWith(SUFFIX_TYPE)) {
			model.addAttribute(CONTENT, EXE_FORMAT_ERROR);
			return toImportDeviceInfo(model, qhex);
		}
		// 读取excel文件
		try {
			List<List<String>> dataList = PoiUtils.readExcel(file, 1, 0, 25); // 参数分别为:文件,数据开始行,数据开始列,数据结束列
			if (dataList.isEmpty()) { // 空数据
				model.addAttribute(CONTENT, EXE_CONTENT_EMPTY);
				return toImportDeviceInfo(model, qhex);
			}
			List<String> tempCarNumList = new ArrayList<>(); // 存放不为空的卡片编号list
			List<String> tempDeciceCodeList = new ArrayList<>(); // 存放不为空的固定资产编号list
			List<String> tempBarCodeList = new ArrayList<>(); // 存放不为空的条码编号list
			for (int i = 0; i < dataList.size(); i++) {
				if (StringUtils.isNotBlank(dataList.get(i).get(10))) {
					tempCarNumList.add(dataList.get(i).get(10));
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(11))) {
					tempDeciceCodeList.add(dataList.get(i).get(11));
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(12))) {
					tempBarCodeList.add(dataList.get(i).get(12));
				}
			}
			// 校验卡片编号在excel列中是否有重复
			List<String> dupCarNumList = Utils.getDuplicateElements(tempCarNumList);
			if (!dupCarNumList.isEmpty()) {
				for (String dnString : dupCarNumList) {
					errMsg.append(CAR_NUM + dnString + REPEAT);
				}
				model.addAttribute(CONTENT, errMsg.toString());
				return toImportDeviceInfo(model, qhex);
			}
			// 校验固定编号在excel列中是否有重复
			List<String> dupDecideCodeList = Utils.getDuplicateElements(tempDeciceCodeList);
			if (!dupDecideCodeList.isEmpty()) {
				for (String dcString : dupDecideCodeList) {
					errMsg.append("固定资产编号:" + dcString + REPEAT);
				}
				model.addAttribute(CONTENT, errMsg.toString());
				return toImportDeviceInfo(model, qhex);
			}
			TbBarcodeType tbBarcodeType = deviceInfoService.getBarcodeType(department.getDeptCode()); // 获取条码自动生成配置
			// 条码手工录入需校验(未读到配置按手工录入处理)
			if (tbBarcodeType == null || tbBarcodeType.getnBarcodeType().equals(0L)) {
				// 校验条码编号在excel列中是否有重复
				List<String> dupBarCodeList = Utils.getDuplicateElements(tempBarCodeList);
				if (!dupBarCodeList.isEmpty()) {
					for (String dbString : dupBarCodeList) {
						errMsg.append(BAR_CODE + dbString + REPEAT);
					}
					model.addAttribute(CONTENT, errMsg.toString());
					return toImportDeviceInfo(model, qhex);
				}
			}

			// 校验数据
			for (int i = 0; i < dataList.size(); i++) {
				if (StringUtils.isBlank(dataList.get(i).get(0))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 设备目录不能为空<br/>");
				} else if (!deviceInfoBatchAddService.ckDeviceNameExist(dataList.get(i).get(0))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 设备目录不存在<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(1))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 使用(存放)地点不能为空<br/>");
				} else if (!deviceInfoBatchAddService.ckPlaceExist(dataList.get(i).get(1), department.getDeptCode())) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 使用(存放)地点不存在<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(2))
						|| DateUtils.parseDate(dataList.get(i).get(2)) == null) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 使用日期不能为空或者格式不正确<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(3))
						|| !pattern1.matcher(dataList.get(i).get(3)).matches()) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 含税价格不能为空并且只能为正整数或最多2位小数的数字<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(4))
						|| !pattern1.matcher(dataList.get(i).get(4)).matches()) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 不含税价格不能为空并且只能为正整数或最多2位小数的数字<br/>");
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(5))
						&& !pattern1.matcher(dataList.get(i).get(5)).matches()) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 资产原值只能为正整数或最多2位小数的数字<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(6))
						|| DateUtils.parseDate(dataList.get(i).get(6)) == null) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 购置时间不能为空或者格式不正确<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(7))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 使用人不能为空<br/>");
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(8)) && !deviceInfoBatchAddService
						.ckDsupplierExist(dataList.get(i).get(8), department.getDeptCode(), department.getDsjbh())) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 供应厂商不存在<br/>");
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(9))
						&& DateUtils.parseDate(dataList.get(i).get(9)) == null) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 建档时间格式不正确<br/>");
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(10))
						&& (deviceInfoBatchAddService.ckCarNumExist(dataList.get(i).get(10))
								|| deviceInfoBatchAddService.ckCarNumApprovalExist(dataList.get(i).get(10)))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 卡片编号已被使用<br/>");
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(11))
						&& (deviceInfoBatchAddService.ckDeciceCodeExist(dataList.get(i).get(11))
								|| deviceInfoBatchAddService.ckDeciceCodeApprovalExist(dataList.get(i).get(11)))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 固定资产编号已被使用<br/>");
				}
				// 条码手工录入且不为空需校验(未读到配置按手工录入处理)
				if (tbBarcodeType == null || tbBarcodeType.getnBarcodeType().equals(0L)) {
					if (StringUtils.isNotBlank(dataList.get(i).get(12))) {
						if (dataList.get(i).get(12).trim().length() != Constants.NUMBER_SUFF.length()) {
							isAllRight = false;
							errMsg.append("第" + (i + 2) + "行数据 条码编号长度只能为:" + Constants.NUMBER_SUFF.length() + "<br/>");
						} else if (deviceInfoBatchAddService.ckBarCodeExist(dataList.get(i).get(12))
								|| deviceInfoBatchAddService.ckBarCodeApprovalExist(dataList.get(i).get(12))) {
							isAllRight = false;
							errMsg.append("第" + (i + 2) + "行数据 条码编号已被使用<br/>");
						}
					}
				}
				if (StringUtils.isNotBlank(dataList.get(i).get(16))
						&& DateUtils.parseDate(dataList.get(i).get(16)) == null) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 保修期限格式不正确<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(17)) || (!dataList.get(i).get(17).equals("A")
						&& !dataList.get(i).get(17).equals("B") && !dataList.get(i).get(17).equals("C"))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 分账类型不能为空并且只能填A或B或C<br/>");
				}
				if (StringUtils.isBlank(dataList.get(i).get(25))
						|| !assetSourceList.contains(dataList.get(i).get(25))) {
					isAllRight = false;
					errMsg.append("第" + (i + 2) + "行数据 资产来源不能为空并且只能填新增购入或在建工程转入或调拨转入或接受捐赠或资产盘盈或其他<br/>");
				}
			}
			if (isAllRight) {
				String cardNoSeqName = deviceInfoService.getCardNoSeqName(department.getDsjbh());
				String barCodeSeqName = deviceInfoService.getBarCodeSeqName(department.getDsjbh());
				// 赋值并保存数据
				for (List<String> list : dataList) {
					TbDeviceInfo tbDeviceInfo = new TbDeviceInfo();
					tbDeviceInfo.setnDeviceType(1L);
					tbDeviceInfo.setnState(1L); // 设备状态(1-正常)
					tbDeviceInfo.setnUseState(1L); // 是否使用(1-使用)
					tbDeviceInfo.setvCreateOpt(user.getPersonCode()); // 创建人
					tbDeviceInfo.setvCreateDept(department.getDeptCode()); // 创建机构
					tbDeviceInfo.setnNameId(deviceInfoBatchAddService.getNameInfoByName(list.get(0))); // 设备目录id
					tbDeviceInfo.setnDplaceId(
							deviceInfoBatchAddService.getPlaceInfoByName(list.get(1), department.getDeptCode())); // 存放地id
					tbDeviceInfo.setdInTime(DateUtils.parseDate(list.get(2))); // 使用日期
					tbDeviceInfo.setnPrice(Double.valueOf(list.get(3))); // 含税价格
					tbDeviceInfo.setnExcludingTaxPrice(Double.valueOf(list.get(4))); // 不含税价格
					if (StringUtils.isNotBlank(list.get(5))) {
						tbDeviceInfo.setnOriginalPrice(Double.valueOf(list.get(5))); // 资产原值
					}
					tbDeviceInfo.setdBuyDate(DateUtils.parseDate(list.get(6))); // 购置时间
					tbDeviceInfo.setvUseOpt(list.get(7)); // 使用人
					if (StringUtils.isNotBlank(list.get(8))) {
						tbDeviceInfo.setnSupplierInfo(deviceInfoBatchAddService.getDsupplierInfoByName(list.get(8),
								department.getDeptCode(), department.getDsjbh())); // 供应商id
					}
					// 建档时间
					if (StringUtils.isNotBlank(list.get(9))) {
						tbDeviceInfo.setdCreateTime(DateUtils.parseDate(list.get(9)));
					} else {
						tbDeviceInfo.setdCreateTime(new Date());
					}
					// 卡片编号
					if (StringUtils.isNotBlank(list.get(10))) {
						tbDeviceInfo.setvCardNum(list.get(10));
					} else {
						tbDeviceInfo.setvCardNum(getVcardCodeNew(department, cardNoSeqName)); // 卡片编号
					}
					tbDeviceInfo.setvDeciceCode(list.get(11)); // 固定资产编号
					// 未读取到配置或配置为手工录入条码
					if (tbBarcodeType == null || tbBarcodeType.getnBarcodeType().equals(0L)) {
						tbDeviceInfo.setvBarCode(list.get(12));
					} else {
						tbDeviceInfo.setvBarCode(getVbarCodeNew(department.getDsjbh(), barCodeSeqName)); // 条码编号
					}
					tbDeviceInfo.setvBrand(list.get(13)); // 品牌
					tbDeviceInfo.setvModel(list.get(14)); // 型号
					tbDeviceInfo.setvSpecifications(list.get(15)); // 规格
					tbDeviceInfo.setdGuarantee(DateUtils.parseDate(list.get(16))); // 保修期限
					tbDeviceInfo.setnLedgerAccount(list.get(17)); // 分账类型
					tbDeviceInfo.setvPname(list.get(18)); // 工程名称
					tbDeviceInfo.setvPfno(list.get(19)); // 工程文件号
					tbDeviceInfo.setvPurpose(list.get(20)); // 用途
					tbDeviceInfo.setvSource(list.get(21)); // 保修信息
					tbDeviceInfo.setvAddrMemo(list.get(22)); // 地点补充
					tbDeviceInfo.setvRemarks(list.get(23)); // 备注
					tbDeviceInfo.setvReimburse(list.get(24)); // 报账单编号
					tbDeviceInfo.setnAssetSource(parseAssetSource(list.get(25))); // 资产来源 1-新增购入 2-在建工程转入 3-调拨转入 4-接受捐赠
																					// 5-资产盘盈 6-其他
					upExcList.add(tbDeviceInfo);
				}
				try {
					deviceInfoBatchAddService.savaDeviceInfoBatchAddInfoToApproval(upExcList);
				} catch (Exception e) {
					addMessage(model, SAVE_FAIL);
					return GLOBAL_ERROR;
				}
			} else {
				model.addAttribute(CONTENT, errMsg.toString());
				return toImportDeviceInfo(model, qhex);
			}
		} catch (Exception e) {
			LOGGER.info(e.getMessage(), e);
			model.addAttribute(CONTENT, "读取解析文件失败!");
			return toImportDeviceInfo(model, qhex);
		}
		addMessage(model, "已成功保存" + upExcList.size() + "条数据");
		model.addAttribute(REDIRECT, Global.getAdminPath() + REDIRECT_URL);
		model.addAttribute(PARAM_QHEX, qhex);
		return GLOBAL_SUCCESS;
	}

	// 资产来源转换
	private Long parseAssetSource(String nAssetSource) {
		switch (nAssetSource) {
		case "新增购入":
			return 1L;
		case "在建工程转入":
			return 2L;
		case "调拨转入":
			return 3L;
		case "接受捐赠":
			return 4L;
		case "资产盘盈":
			return 5L;
		case "其他":
			return 6L;
		default:
			return null;
		}
	}

}
