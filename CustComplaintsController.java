package com.routdata.zhfxjc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pfpj.common.msg.UserInfo;
import com.routdata.financial.common.annotation.Log;
import com.routdata.financial.common.controller.AesUtils;
import com.routdata.financial.common.controller.ContollerSupport;
import com.routdata.financial.common.core.domain.AjaxResult;
import com.routdata.financial.common.core.page.TableDataInfo;
import com.routdata.financial.common.enums.BusinessType;
import com.routdata.financial.common.exception.CustomException;
import com.routdata.financial.common.utils.CsvUtils;
import com.routdata.financial.common.utils.ExcFileUtils;
import com.routdata.financial.common.utils.RoutDateUtils;
import com.routdata.financial.common.utils.SecurityUtils;
import com.routdata.financial.common.utils.poi.PoiUtils;
import com.routdata.zhfxjc.entity.dto.TbCustComplaints;
import com.routdata.zhfxjc.service.ICustComplaintsService;
import com.routdata.zhfxjc.util.dataMasking.DesensitizationTypeEnum;
import com.routdata.zhfxjc.util.dataMasking.StringMaskUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gaos
 * @Description 风险账户客诉controller
 * @createTime 2024年03月01日
 */
@RestController
@RequestMapping("/zhfxjc/custcomplaints")
public class CustComplaintsController extends ContollerSupport {
    @Override
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        super.initBinder(binder);
        binder.setDisallowedFields(new String[]{""});
    }

    @Autowired
    private ICustComplaintsService custComplaintsService;

    /**
     * 列表查询
     */
    @Log(title = "风险账户客诉列表查询", businessType = BusinessType.QUERY)
    @PreAuthorize("@ss.hasPermi('custcomplaints:main:list')")
    @GetMapping("list")
    public TableDataInfo list(TbCustComplaints tbCustComplaints) {
        Page<TbCustComplaints> page = super.getPage();
        IPage<TbCustComplaints> pageResult = custComplaintsService.getPageForCustComplaints(page, tbCustComplaints); //查询数据
        List<TbCustComplaints> list = (pageResult.getSize() > 0) ? pageResult.getRecords() : new ArrayList<>();
        return getDataTable(list, page.getTotal());
    }

    /**
     * 批量导入
     *
     * @param file
     * @return
     */
    @Log(title = "风险账户客诉批量导入", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('custcomplaints:main:import')")
    @PostMapping("importfile")
    public AjaxResult importFile(@RequestParam("file") MultipartFile file) throws ParseException, IOException {
        List<TbCustComplaints> tbCustComplaintsList = new ArrayList<>();
        if (file.isEmpty()) { // 校验是否已上传文件
            return AjaxResult.error("没有上传导入文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (!originalFilename.toLowerCase().endsWith(".csv")) { //文件格式不对
            return AjaxResult.error("文件格式不对,请重新上传");
        }
        List<String[]> list = CsvUtils.getCsvDataList(file, 3);
        if (list.isEmpty()) { //无数据
            return AjaxResult.error("文件内容为空");
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).length < 69) {
                return AjaxResult.error("文件解析异常，第" + (i + 4) + "行，数据长度不正确！");
            }
        }
        saveCustComplaints(list, tbCustComplaintsList);
        return AjaxResult.success("成功保存" + tbCustComplaintsList.size() + "条数据");
    }

    /**
     * 封装并保存数据
     */
    private void saveCustComplaints(List<String[]> list, List<TbCustComplaints> tbCustComplaintsList) throws ParseException {
        UserInfo userInfo = SecurityUtils.getUserInfo(); //获取用户信息
        for (String[] data : list) {
            TbCustComplaints tbCustComplaints = new TbCustComplaints();
            tbCustComplaints.setBranchName(data[1]); // 网点名称
            tbCustComplaints.setBranchCode(data[2]); //网点机构号
            tbCustComplaints.setWorkNumber(data[3]); //工单号
            tbCustComplaints.setOrderWorkNumber(data[4]); //原系统工单号
            tbCustComplaints.setRenhangOrderNumber(data[5]); //人行工单号
            tbCustComplaints.setOrderStatus(data[6]); //工单状态

            tbCustComplaints.setvAcceptChannels(data[7]); //投诉受理渠道
            tbCustComplaints.setvAcceptDate(RoutDateUtils.stringToDate(data[8])); //投诉受理日期
            tbCustComplaints.setCompleDate(RoutDateUtils.stringToDate(data[9]));//办结日期
            tbCustComplaints.setvOrderSource(data[10]); //工单来源
            tbCustComplaints.setvIndptyName(data[11]); //投诉人姓名
            tbCustComplaints.setvSex(data[12]); //性别
            tbCustComplaints.setComplainantCountry(data[13]);// 投诉人所在国家
            tbCustComplaints.setComplainantProvince(data[14]); //投诉人所在省/自治区/直辖市
            tbCustComplaints.setComplainantCity(data[15]);// 投诉人所在市/自治州
            tbCustComplaints.setComplainantDistrict(data[16]);// 投诉人所在区/市/县

            tbCustComplaints.setvCertType("1"); //证件类型:1身份证（默认）
            tbCustComplaints.setvCertNo(data[17]); //身份证号码
            tbCustComplaints.setBirthYear(data[18]); // 出生年
            tbCustComplaints.setvCustContact(data[19]); //投诉电话/邮箱
            tbCustComplaints.setReservedPhone(data[20]); // 预留电话
            tbCustComplaints.setReplyPhone(data[21]); // 回复电话
            tbCustComplaints.setvComplaintContent(data[22]); //投诉内容
            tbCustComplaints.setvComplaintOrg(data[23]); //被投诉机构
            tbCustComplaints.setComplainedOrgLevel(data[24]);// 被投诉机构所在层级
            tbCustComplaints.setComplainedOrgParent(data[25]);// 被投诉机构上级机构
            tbCustComplaints.setComplainedOrgProvince(data[26]); //被投诉机构所在省/自治区/直辖市
            tbCustComplaints.setComplainedOrgCity(data[27]); // 被投诉机构所在市/自治州
            tbCustComplaints.setComplainedOrgDistrict(data[28]); // 被投诉机构所在区/县
            tbCustComplaints.setCompLevel(data[29]); // 投诉等级
            tbCustComplaints.setCompServiceChannel(data[30]); // 投诉业务办理渠道
            tbCustComplaints.setCompServiceCategory(data[31]); // 投诉业务类别
            tbCustComplaints.setvComplaintReason(data[32]); //投诉原因
            tbCustComplaints.setCompSource(data[33]); //投诉渠道
            tbCustComplaints.setLineType(data[34]); //条线类别
            tbCustComplaints.setProductType(data[35]); // 产品（服务）大类
            tbCustComplaints.setProductSubtype(data[36]); // 产品（服务）小类
            tbCustComplaints.setComplaintValue(PoiUtils.formatMoney(data[37])); // 投诉金额（万元）
            if (StringUtils.isNotBlank(data[38])) {
                tbCustComplaints.setCompNum(Long.valueOf(data[38])); //投诉人数
            }
            tbCustComplaints.setvComplaintRequest(data[39]); //投诉请求
            tbCustComplaints.setvCheckProcess(data[40]); //调查经过
            tbCustComplaints.setvHandlingSituation(data[41]); //投诉处理情况
            tbCustComplaints.setvHandlingResults(data[42]); //投诉处理结果
            tbCustComplaints.setvHandlingOrg(data[43]); //投诉处理机构
            tbCustComplaints.setvHandlingOrgLevel(data[44]); //投诉处理机构所在层级
            tbCustComplaints.setvHandlingOrgBelong(data[45]); //投诉处理机构归属机构
            tbCustComplaints.setvHandlingOrgType(data[46]); //投诉处理机构属性
            tbCustComplaints.setvHandlingOrgProv(data[47]); //投诉处理机构所在省/自治区/直辖市
            tbCustComplaints.setvHandlingOrgCity(data[48]); //投诉处理机构所在市/自治州
            tbCustComplaints.setvHandlingOrgDist(data[49]); //投诉处理机构所在区/市/县
            if (StringUtils.isNotBlank(data[50])) {
                tbCustComplaints.setnProcessTime(Long.valueOf(data[50]));//投诉处理时长
            }
            tbCustComplaints.setvIsReply(data[51]); //客户是否需要回复
            tbCustComplaints.setvIsReplyPeople(data[52]); //是否回复投诉人
            tbCustComplaints.setdReplyDate(RoutDateUtils.stringToDate(data[53])); //回复投诉人/作出处理决定日期
            tbCustComplaints.setvIsVisit(data[54]); //是否回访
            tbCustComplaints.setdVisitDate(RoutDateUtils.stringToDate(data[55])); //回访日期
            tbCustComplaints.setvVisitOrg(data[56]); //回访机构
            tbCustComplaints.setvVisitResult(data[57]); //回访结果
            tbCustComplaints.setvVisitSituation(data[58]); //回访情况
            tbCustComplaints.setvIsDuty(data[59]); //是否有责
            tbCustComplaints.setvDutySituation(data[60]); //问责情况
            tbCustComplaints.setSmallCompFlag(data[61]); // 是否进行小额补偿
            tbCustComplaints.setSmallCompAmount(PoiUtils.formatMoney(data[62])); // 小额补偿金额
            tbCustComplaints.setPbcReportingStatus(data[63]); // 报送人行状态
            tbCustComplaints.setIsUpgrade(data[64]); // 是否升级投诉
            tbCustComplaints.setDeadline(data[65]); // 办结期限
            tbCustComplaints.setOverdueReason(data[66]); // 超过15日办结原因
            tbCustComplaints.setImprovementStatus(data[67]); //产品与服务改进情况
            tbCustComplaints.setRemark(data[68]); //备注

            tbCustComplaints.setvImpName(userInfo.getUserId()); //导入人
            tbCustComplaints.setvImpDept(userInfo.getGroupId()); //导入机构
            tbCustComplaints.setvIssueState("0"); //下发状态 0-未下发 1-已下发
            tbCustComplaints.setvEntryState("0"); //网点录入状态 0-未录入 1-已录入
            tbCustComplaintsList.add(tbCustComplaints);
        }
        custComplaintsService.saveCustComplaintsImportInfo(tbCustComplaintsList); //保存数据
    }

    /**
     * 下发网点
     */
    @Log(title = "风险账户客诉下发网点", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('custcomplaints:main:issue')")
    @PostMapping("issueCustComplaints")
    public AjaxResult issueCustComplaints(@RequestParam(value = "irowids", required = true) List<String> irowids) throws ParseException {
        if (irowids == null || irowids.isEmpty()) {
            return AjaxResult.error("未选择要下发网点的数据");
        }
        List<Long> list = new ArrayList<>();
        for (String irowid : irowids) {
            String nCustComplaints = AesUtils.getInstance().decrypt(irowid);
            TbCustComplaints dbCustComplaints = custComplaintsService.getCustComplaintsByKey(Long.valueOf(nCustComplaints));
            if (dbCustComplaints == null || !dbCustComplaints.getvIssueState().equals("0")) {
                return AjaxResult.error("数据不存在或该数据已下发网点");
            }
            list.add(Long.valueOf(nCustComplaints));
        }
        custComplaintsService.updateIssueCustComplaintsInfo(list); //保存下发网点信息
        return AjaxResult.success("下发排查成功");
    }

    /**
     * 导出excel
     */
    @Log(title = "导出风险账户客诉列表", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('custcomplaints:main:excel')")
    @GetMapping(value = "outPortCustComplaints")
    public void outPortCustComplaints(TbCustComplaints tbCustComplaints, HttpServletResponse response) throws IOException {
        List<TbCustComplaints> list = custComplaintsService.getListForCustComplaints(tbCustComplaints); //查询数据

        if (list.size() > 1040000) {
            logger.info("数据量过大：", list.size());
            throw new CustomException("数据量过大,请缩小查询范围");
        }
        for (TbCustComplaints custComplaints : list) {
            //投诉人姓名、账号、户名
            custComplaints.setvIndptyName(StringMaskUtils.serializeWithLength(custComplaints.getvIndptyName(), DesensitizationTypeEnum.CHINESE_NAME));
            // 身份证号
            custComplaints.setvCertNo(StringMaskUtils.serializeWithLength(custComplaints.getvCertNo(), DesensitizationTypeEnum.ID_CARD));
            // 投诉电话
            custComplaints.setvCustContact(StringMaskUtils.serializeWithLength(custComplaints.getvCustContact(), DesensitizationTypeEnum.MOBILE_PHONE));
            // 预留电话
            custComplaints.setReservedPhone(StringMaskUtils.serializeWithLength(custComplaints.getReservedPhone(), DesensitizationTypeEnum.MOBILE_PHONE));
            // 账号
            custComplaints.setvImpCustacc(StringMaskUtils.serializeWithLength(custComplaints.getvImpCustacc(), DesensitizationTypeEnum.ACCOUNT));
            // 户名 accName
            custComplaints.setAccName(StringMaskUtils.serializeWithLength(custComplaints.getAccName(), DesensitizationTypeEnum.CHINESE_NAME));
        }


        String fileName = "风险账户客诉"; //文件名称
        this.excelCustComplaints(list, fileName, response);
    }

    /**
     * 描述：创建Excel模板并写入数据
     */
    private void excelCustComplaints(List<TbCustComplaints> list, String fileName, HttpServletResponse response) throws IOException {
        // list是表格数据，fileName是表格名称，titleName是表格字段名称
        OutputStream outPut = null;
        SXSSFWorkbook sfWorkbook = null;
        SXSSFRow sfRow = null;
        CellStyle cStyle = null;
        int number = 0;// 行序号
        sfWorkbook = new SXSSFWorkbook(100);// 创建 一个excel文档对象
        SXSSFSheet sfSheet = sfWorkbook.createSheet(fileName);// 创建一个工作薄对象
        cStyle = ExcFileUtils.createTitleStyle(sfWorkbook);
        sfRow = sfSheet.createRow(number);// 创建一个行对象
        sfRow.setHeightInPoints(60);// 设置行高60像素
        sfSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 74)); // 合并单元格
        ExcFileUtils.createCell(sfRow, 0, fileName, cStyle);// 创建大标题单元格
        number++;
        sfRow = sfSheet.createRow(number);// 创建一个行对象
        sfRow.setHeightInPoints(40);// 设置行高40像素+21
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 0, "网点名称", 5000, number, number + 1, 0, 0);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 1, "网点机构号", 5000, number, number + 1, 1, 1);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 2, "工单号", 5000, number, number + 1, 2, 2);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 3, "原系统工单号", 5000, number, number + 1, 3, 3);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 4, "人行工单号", 5000, number, number + 1, 4, 4);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 5, "工单状态", 5000, number, number + 1, 5, 5);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 6, "投诉受理渠道", 5000, number, number + 1, 6, 6);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 7, "投诉受理日期", 5000, number, number + 1, 7, 7);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 8, "办结日期", 5000, number, number + 1, 8, 8);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 9, "工单来源", 5000, number, number + 1, 9, 9);

        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 10, "投诉基本情况", 5000, number, number, 10, 38);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 39, "投诉处理情况", 5000, number, number, 39, 49);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 50, "投诉回复情况", 5000, number, number, 50, 52);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 53, "投诉回访情况", 5000, number, number, 53, 57);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 58, "是否有责", 5000, number, number + 1, 58, 58);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 59, "问责情况", 5000, number, number + 1, 59, 59);

        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 60, "是否进行小额补偿", 5000, number, number + 1, 60, 60);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 61, "小额补偿金额", 5000, number, number + 1, 61, 61);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 62, "报送人行状态", 5000, number, number + 1, 62, 62);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 63, "是否升级投诉", 5000, number, number + 1, 63, 63);

        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 64, "办结期限情况", 5000, number, number, 64, 65);

        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 66, "产品与服务改进情况", 5000, number, number + 1, 66, 66);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 67, "备注", 5000, number, number + 1, 67, 67);

        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 68, "网点录入情况", 5000, number, number, 68, 73);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 74, "导入人", 5000, number, number + 1, 74, 74);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 75, "导入时间", 5000, number, number + 1, 75, 75);
        ExcFileUtils.createRegionCell(sfSheet, sfRow, cStyle, 76, "导入机构", 5000, number, number + 1, 76, 76);
        sfRow = sfSheet.createRow(number + 1);
        sfRow.setHeightInPoints(40);// 设置行高40像素
        /*投诉基本情况*/
        ExcFileUtils.createCell(sfRow, 10, "投诉人姓名", cStyle);
        ExcFileUtils.createCell(sfRow, 11, "性别", cStyle);
        ExcFileUtils.createCell(sfRow, 12, "投诉人所在国家", cStyle);
        ExcFileUtils.createCell(sfRow, 13, "投诉人所在省/自治区/直辖市", cStyle);
        ExcFileUtils.createCell(sfRow, 14, "投诉人所在市/自治州", cStyle);
        ExcFileUtils.createCell(sfRow, 15, "投诉人所在区/市/县", cStyle);
        ExcFileUtils.createCell(sfRow, 16, "身份证号码", cStyle);
        ExcFileUtils.createCell(sfRow, 17, "出生年份", cStyle);
        ExcFileUtils.createCell(sfRow, 18, "投诉电话/邮箱", cStyle);
        ExcFileUtils.createCell(sfRow, 19, "预留电话", cStyle);
        ExcFileUtils.createCell(sfRow, 20, "回复电话", cStyle);
        ExcFileUtils.createCell(sfRow, 21, "投诉内容", cStyle);
        ExcFileUtils.createCell(sfRow, 22, "被投诉机构", cStyle);
        ExcFileUtils.createCell(sfRow, 23, "被投诉机构所在层级", cStyle);
        ExcFileUtils.createCell(sfRow, 24, "被投诉机构归属机构", cStyle);
        ExcFileUtils.createCell(sfRow, 25, "被投诉机构所在省/自治区/直辖市", cStyle);
        ExcFileUtils.createCell(sfRow, 26, "被投诉机构所在市/自治州", cStyle);
        ExcFileUtils.createCell(sfRow, 27, "被投诉机构所在区/市/县", cStyle);
        ExcFileUtils.createCell(sfRow, 28, "投诉等级", cStyle);
        ExcFileUtils.createCell(sfRow, 29, "投诉业务办理渠道", cStyle);
        ExcFileUtils.createCell(sfRow, 30, "投诉业务类别", cStyle);
        ExcFileUtils.createCell(sfRow, 31, "投诉原因", cStyle);
        ExcFileUtils.createCell(sfRow, 32, "投诉渠道", cStyle);
        ExcFileUtils.createCell(sfRow, 33, "条线类别", cStyle);
        ExcFileUtils.createCell(sfRow, 34, "产品（服务）大类", cStyle);
        ExcFileUtils.createCell(sfRow, 35, "产品（服务）小类", cStyle);
        ExcFileUtils.createCell(sfRow, 36, "投诉金额(万元)", cStyle);
        ExcFileUtils.createCell(sfRow, 37, "投诉人数", cStyle);
        ExcFileUtils.createCell(sfRow, 38, "投诉请求", cStyle);
        /*投诉处理情况*/
        ExcFileUtils.createCell(sfRow, 39, "调查经过", cStyle);
        ExcFileUtils.createCell(sfRow, 40, "投诉处理情况", cStyle);
        ExcFileUtils.createCell(sfRow, 41, "投诉处理结果", cStyle);
        ExcFileUtils.createCell(sfRow, 42, "投诉处理机构", cStyle);
        ExcFileUtils.createCell(sfRow, 43, "投诉处理机构所在层级", cStyle);
        ExcFileUtils.createCell(sfRow, 44, "投诉处理机构归属机构", cStyle);
        ExcFileUtils.createCell(sfRow, 45, "投诉处理机构属性", cStyle);
        ExcFileUtils.createCell(sfRow, 46, "投诉处理机构所在省/自治区/直辖市", cStyle);
        ExcFileUtils.createCell(sfRow, 47, "投诉处理机构所在市/自治州", cStyle);
        ExcFileUtils.createCell(sfRow, 48, "投诉处理机构所在区/市/县", cStyle);
        ExcFileUtils.createCell(sfRow, 49, "投诉处理时长", cStyle);
        /*投诉回复情况*/
        ExcFileUtils.createCell(sfRow, 50, "客户是否需要回复", cStyle);
        ExcFileUtils.createCell(sfRow, 51, "是否回复投诉人", cStyle);
        ExcFileUtils.createCell(sfRow, 52, "回复投诉人/作出处理决定日期", cStyle);
        /*投诉回访情况*/
        ExcFileUtils.createCell(sfRow, 53, "是否回访", cStyle);
        ExcFileUtils.createCell(sfRow, 54, "回访日期", cStyle);
        ExcFileUtils.createCell(sfRow, 55, "回访机构", cStyle);
        ExcFileUtils.createCell(sfRow, 56, "回访结果", cStyle);
        ExcFileUtils.createCell(sfRow, 57, "回访情况", cStyle);
        /*办结期限情况*/
        ExcFileUtils.createCell(sfRow, 64, "办结期限", cStyle);
        ExcFileUtils.createCell(sfRow, 65, "超过15日办结原因", cStyle);

        /*网点录入情况*/
        ExcFileUtils.createCell(sfRow, 68, "核减类型", cStyle);
        ExcFileUtils.createCell(sfRow, 69, "账号", cStyle);
        ExcFileUtils.createCell(sfRow, 70, "户名", cStyle);
        ExcFileUtils.createCell(sfRow, 71, "开户日期", cStyle);
        ExcFileUtils.createCell(sfRow, 72, "情况说明", cStyle);
        ExcFileUtils.createCell(sfRow, 73, "说明材料名称", cStyle);
        number++;
        number++; //加两次是因为合并了两行单元格
        // 生成表格内容
        cStyle = ExcFileUtils.createStyle(sfWorkbook);
        createDataCustComplaints(list, sfSheet, number, cStyle); //写入数据
        ExcFileUtils.setResponse(fileName, response);
        outPut = response.getOutputStream();
        sfWorkbook.write(outPut);
        outPut.flush();
        outPut.close();
    }

    /**
     * 写入数据
     */
    private void createDataCustComplaints(List<TbCustComplaints> list, SXSSFSheet sfSheet, int number, CellStyle cStyle) {
        for (int i = 0; i < list.size(); i++) {
            TbCustComplaints tbCustComplaints = list.get(i);
            if (tbCustComplaints != null) {
                SXSSFRow sfRow = sfSheet.createRow(number++);
                int j = 0;
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getBranchName(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getBranchCode(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getWorkNumber(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getOrderWorkNumber(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getRenhangOrderNumber(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getOrderStatus(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvAcceptChannels(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getvAcceptDate()), cStyle);
                // 办结日期
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getCompleDate()), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvOrderSource(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvIndptyName(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvSex(), cStyle);
                // 投诉人所在国家
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainantCountry(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainantProvince(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainantCity(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainantDistrict(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvCertNo(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getBirthYear(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvCustContact(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getReservedPhone(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getReplyPhone(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvComplaintContent(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvComplaintOrg(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainedOrgLevel(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainedOrgParent(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainedOrgProvince(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainedOrgCity(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getComplainedOrgDistrict(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getCompLevel(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getCompServiceChannel(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getCompServiceCategory(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvComplaintReason(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getCompSource(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getLineType(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getProductType(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getProductSubtype(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, null == tbCustComplaints.getComplaintValue() ? tbCustComplaints.getComplaintValue() : tbCustComplaints.getComplaintValue().setScale(2, RoundingMode.HALF_UP), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getCompNum(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvComplaintRequest(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvCheckProcess(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingSituation(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingResults(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrg(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgLevel(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgBelong(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgType(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgProv(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgCity(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvHandlingOrgDist(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getnProcessTime(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvIsReply(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvIsReplyPeople(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getdReplyDate()), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvIsVisit(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getdVisitDate()), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvVisitOrg(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvVisitResult(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvVisitSituation(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvIsDuty(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvDutySituation(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getSmallCompFlag(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, null == tbCustComplaints.getSmallCompAmount() ? tbCustComplaints.getSmallCompAmount() : tbCustComplaints.getSmallCompAmount().setScale(2, RoundingMode.HALF_UP), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getPbcReportingStatus(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getIsUpgrade(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getDeadline(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getOverdueReason(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getImprovementStatus(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getRemark(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getReductionTypeWords(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvImpCustacc(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getAccName(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getOpenDate()), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getDescription(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getDocumentName(), cStyle);

                ExcFileUtils.createCell(sfRow, j++, tbCustComplaints.getvImpNameWords(), cStyle);
                ExcFileUtils.createCell(sfRow, j++, RoutDateUtils.formatDate(tbCustComplaints.getdImpDate(), RoutDateUtils.YYYY_MM_DD_HH_MM_SS), cStyle);
                ExcFileUtils.createCell(sfRow, j, tbCustComplaints.getvImpDeptWords(), cStyle);
            }
        }
    }

    /**
     * 列表查询网点
     */
    @Log(title = "风险账户客诉查询网点列表", businessType = BusinessType.QUERY)
    @PreAuthorize("@ss.hasPermi('custcomplaints:branch:list')")
    @GetMapping("branchList")
    public TableDataInfo branchList(TbCustComplaints tbCustComplaints) {
        Page<TbCustComplaints> page = super.getPage();
        tbCustComplaints.setBranchCode(SecurityUtils.getUserInfo().getGroupId()); //机构
        tbCustComplaints.setvIssueState("1"); //已下发
        IPage<TbCustComplaints> pageResult = custComplaintsService.getPageForCustComplaints(page, tbCustComplaints); //查询数据
        List<TbCustComplaints> list = (pageResult.getSize() > 0) ? pageResult.getRecords() : new ArrayList<>();
        return getDataTable(list, page.getTotal());
    }

    /**
     * 保存网点录入信息
     */
    @Log(title = "风险账户客诉保存网点录入信息", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('custcomplaints:branch:entry')")
    @PostMapping("saveBranchEntry")
    public AjaxResult saveBranchEntry(@RequestBody TbCustComplaints tbCustComplaints) {
        if (StringUtils.isBlank(tbCustComplaints.getReductionType())
                || StringUtils.isBlank(tbCustComplaints.getvImpCustacc())
                || StringUtils.isBlank(tbCustComplaints.getAccName())) {
            return AjaxResult.error("核减类型、账号和户名为必填");
        }
        TbCustComplaints dbCustComplaints = custComplaintsService.getCustComplaintsByKey(tbCustComplaints.getnCustComplaints());
        if (dbCustComplaints == null) {
            return AjaxResult.error("数据不存在");
        }
        tbCustComplaints.setvEntryState("1"); //已录入
        custComplaintsService.saveBranchEntry(tbCustComplaints); //保存网点录入信息
        return AjaxResult.success();
    }

    /**
     * 导出excel
     */
    @Log(title = "导出风险账户客诉", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('custcomplaints:branch:excel')")
    @GetMapping(value = "outPortComplaintsForbranch")
    public void outPortComplaintsForbranch(TbCustComplaints tbCustComplaints, HttpServletResponse response) throws IOException {
        tbCustComplaints.setBranchCode(SecurityUtils.getUserInfo().getGroupId()); //机构
        tbCustComplaints.setvIssueState("1"); //已下发
        List<TbCustComplaints> list = custComplaintsService.getListForCustComplaints(tbCustComplaints);
        if (list.size() > 1040000) {
            logger.info("数据量过大：", list.size());
            throw new CustomException("数据量过大,请缩小查询范围");
        }
        for (TbCustComplaints custComplaints : list) {
            //投诉人姓名、账号、户名
            custComplaints.setvIndptyName(StringMaskUtils.serializeWithLength(custComplaints.getvIndptyName(), DesensitizationTypeEnum.CHINESE_NAME));
            // 身份证号
            custComplaints.setvCertNo(StringMaskUtils.serializeWithLength(custComplaints.getvCertNo(), DesensitizationTypeEnum.ID_CARD));
            // 投诉电话
            custComplaints.setvCustContact(StringMaskUtils.serializeWithLength(custComplaints.getvCustContact(), DesensitizationTypeEnum.MOBILE_PHONE));
            // 预留电话
            custComplaints.setReservedPhone(StringMaskUtils.serializeWithLength(custComplaints.getReservedPhone(), DesensitizationTypeEnum.MOBILE_PHONE));
            // 账号
            custComplaints.setvImpCustacc(StringMaskUtils.serializeWithLength(custComplaints.getvImpCustacc(), DesensitizationTypeEnum.ACCOUNT));
            // 户名 accName
            custComplaints.setAccName(StringMaskUtils.serializeWithLength(custComplaints.getAccName(), DesensitizationTypeEnum.CHINESE_NAME));
        }
        String fileName = "风险模型管理"; //文件名称
        this.excelCustComplaints(list, fileName, response);
    }
}
