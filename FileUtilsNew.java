package com.routdata.financialdata.web.controller.util;

import com.routdata.rd3f.core.api.common.utils.QaxUtils;
import com.routdata.rd3f.core.web.common.utils.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @description:文件处理工具类
 */
public class FileUtilsNew {
    private FileUtilsNew() {}
    public static final Logger LOGGER = Logger.getLogger(FileUtilsNew.class);
    private static final String DOWN_FILE_EX = "下载文件异常：";

    /**
     * @description：下载模板文件
     */
    public static void downTemplet(HttpServletRequest request,String fileName, HttpServletResponse response) {
        try {
            File file = new File(QaxUtils.getSafeFileName(fileName));  //创建文件对象
            FileUtils.downFile(file,request,response); //下载文件
        }catch (Exception e) {
            LOGGER.info(DOWN_FILE_EX, e); //下载异常
        }
    }

    /**
     * 下载Excel文件
     */
    public static void downExcelFile(HttpServletRequest request,HttpServletResponse response, String tempPath, String downName) {
        try {
            File file = new File(QaxUtils.getSafeFilePath(tempPath)); //创建文件对象
            FileUtils.downFile(file,request,response,downName); //下载文件
        } catch (Exception e) {
            LOGGER.info(DOWN_FILE_EX, e); //下载异常
        }
    }
}
