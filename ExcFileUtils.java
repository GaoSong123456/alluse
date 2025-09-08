package com.routdata.financial.common.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * @ClassNameDescription:导出excle文件表格样式工具类
 */
public class ExcFileUtils {
    private ExcFileUtils() {
    }

    /**
     * @param workBook 工作薄对象
     * @description:创建标题样式
     */
    public static CellStyle createTitleStyle(Workbook workBook) {
        CellStyle tlStyle = createDefault(workBook); // 创建样式对象
        Font font = workBook.createFont(); // 创建字体对象
        font.setFontHeightInPoints((short) 13); // 设置字体大小
        font.setBold(true); // 设置粗体
        font.setFontName("黑体"); // 设置为黑体字
        tlStyle.setFont(font); // 将字体加入到样式对象
        return tlStyle;
    }

    /**
     * @param workBook 工作薄对象
     * @description:创建内容样式
     */
    public static CellStyle createStyle(Workbook workBook) {
        CellStyle style = createDefault(workBook); // 创建样式对象
        Font font = workBook.createFont(); // 创建字体对象
        font.setFontName("黑体");  // 设置为黑体字
        style.setFont(font);  // 将字体加入到样式对象
        return style;
    }

    /**
     * @param workBook 文本
     * @description:文件边框和对齐方式样式默认设置
     */
    private static CellStyle createDefault(Workbook workBook) {
        CellStyle tlStyle = workBook.createCellStyle(); // 创建样式对象
        tlStyle.setAlignment(HorizontalAlignment.CENTER_SELECTION); // 设置对齐方式水平居中
        tlStyle.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直居中
        tlStyle.setBorderTop(BorderStyle.THIN); // 设置边框顶部边框粗线
        tlStyle.setBorderBottom(BorderStyle.THIN); // 底部边框双线
        tlStyle.setBorderLeft(BorderStyle.THIN); // 左边边框
        tlStyle.setBorderRight(BorderStyle.THIN); // 右边边框
        return tlStyle;
    }

    /**
     * @param row          每一行
     * @param content      表格内容
     * @param columnsIndex 字段索引
     * @param cellStyle    列样式
     * @description:创建表格列
     */
    public static void createCell(Row row, int columnsIndex, Object content, CellStyle cellStyle) {
        Cell cell = row.createCell((short) columnsIndex);
        cell.setCellStyle(cellStyle);  //设置样式
        cell.setCellType(CellType.STRING); //设置类型
        cell.setCellValue(null == content ? "" : content.toString());  //设置值
    }

    /**
     * 创建合并单元格
     */
    public static void createRegionCell(Sheet sfSheet, Row sfRow, CellStyle cStyle, int columnsIndex, String content, int width, int firstRow,
                                        int lastRow, int firstCol, int lastCol) {
        createCell(sfRow, columnsIndex, content, cStyle);  //创建单元格
        sfSheet.setColumnWidth(columnsIndex, width);// 设置指定的列宽,单位为像素
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);  //合并单元格坐标
        sfSheet.addMergedRegion(region); //合并单元格
    }

    /**
     * 设置属性
     */
    public static void setResponse(String fileName, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "filename=\""
                + new String((fileName + ".xlsx").getBytes("gb2312"), StandardCharsets.ISO_8859_1) + "\";");
    }
}
