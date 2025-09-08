package com.routdata.financialdata.web.controller.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gaos
 * @Description: TODO(poi工具)
 */
public class PoiUtils {
	private PoiUtils(){}
    public static final Logger LOGGER = Logger.getLogger(PoiUtils.class);
    
    /**
     * 读取2003格式
     * startRowNum:读取数据的开始行,从0开始; startCellNum:读取数据的开始列,从0开始;endCellNum:读取数据的结束列,从0开始
     * @throws IOException
     */
    public static List<List<String>> readExcel2003(MultipartFile file,int startRowNum,int startCellNum,int endCellNum) throws IOException{
        InputStream fis =file.getInputStream();  //获取流
        HSSFWorkbook xwb = new HSSFWorkbook(fis);   // 构造 HSSFWorkbook 对象
        return getExcelData(xwb,fis,startRowNum,startCellNum,endCellNum); //获取数据
    }
    
    /**
     * 读取2007-2013格式
     * startRowNum:读取数据的开始行,从0开始; startCellNum:读取数据的开始列,从0开始;endCellNum:读取数据的结束列,从0开始
     * @throws IOException
     */
    public static List<List<String>> readExcel2007(MultipartFile file,int startRowNum,int startCellNum,int endCellNum) throws IOException{
        InputStream fis =file.getInputStream(); //获取流
        XSSFWorkbook xwb = new XSSFWorkbook(fis);   // 构造 XSSFWorkbook 对象
        return getExcelData(xwb,fis,startRowNum,startCellNum,endCellNum); //获取数据
    }

    //获取excel文件数据
    private static List<List<String>> getExcelData(Workbook xwb,InputStream inputStream,int startRowNum,int startCellNum,int endCellNum) throws IOException{
        List<List<String>> valueList=new ArrayList<>();
        Sheet sheet = xwb.getSheetAt(0);            // 读取第一张表格内容
        // 定义 row、cell
        Row row;
        // 循环输出表格中的从第二行开始内容
        for (int i = startRowNum; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i); //获取行
            if (row != null) {
                boolean isValidRow = false; //校验标志
                List<String> val = new ArrayList<>();
                for (int j = startCellNum; j <= endCellNum; j++) {
                	Cell cell = row.getCell(j); //获取单元格
                    String cellValue = getCellValue(cell);  //获取单元格的值
                    val.add(cellValue);
                    if(!isValidRow && cellValue!= null && cellValue.length()>0){
                        isValidRow = true;
                    }
                }
                // 第I行所有的列数据读取完毕且不是所有列都为空，放入valuelist
                if (isValidRow) {
                    valueList.add(val);
                }
            }
        }
        inputStream.close();
        xwb.close();
        return valueList;
    }
    
    //根据单元格类型获取值
    private static String getCellValue(Cell cell) {
        String cellValue = "";
        if (cell == null) {  //空
            return cellValue;
        }
        // 判断数据的类型
        switch (cell.getCellTypeEnum()) {
            case NUMERIC: // 数字
                if (HSSFDateUtil.isCellDateFormatted(cell)) {// 处理日期格式、时间格式
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    cellValue=sdf.format(HSSFDateUtil.getJavaDate(cell.getNumericCellValue())); //format
                } else if (cell.getCellStyle().getDataFormat() == 0) {//处理数值格式
                    cellValue = NumberToTextConverter.toText(cell.getNumericCellValue()); //装换
                }
                else {
                    DataFormatter dataFormatter=new DataFormatter(); //装换
                    cellValue =dataFormatter.formatCellValue(cell); //装换
                }
                break;
            case STRING: // 字符串
                cellValue = String.valueOf(cell.getStringCellValue()); //字符串
                break;
            case BOOLEAN: // Boolean
                cellValue = String.valueOf(cell.getBooleanCellValue()); //Boolean
                break;
            case FORMULA: // 公式
                cellValue = String.valueOf(cell.getCellFormula()); //公式
                break;
            case BLANK: // 空值
                cellValue = ""; //""
                break;
            case ERROR: // 故障
                cellValue = "非法字符"; //非法字符
                break;
            default:
                cellValue = "未知类型"; //未知类型
                break;
        }
        return cellValue.trim();
    }
    
    //将金额类型字符串转化为BigDecimal,有,会去掉
  	public static BigDecimal formatMoney(String money) {
  		if (StringUtils.isBlank(money)) {
  			return BigDecimal.ZERO;  //0
  		}
  		else if (money.contains(",")) { //包含,
  			return new BigDecimal(money.replace(",", "")); //替换
  		}
  		return new BigDecimal(money); //返回
  	}
}
