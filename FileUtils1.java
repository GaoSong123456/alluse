package com.routdata.east.api.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @description:文件处理工具类
 */
public class FileUtils {
	
	private FileUtils() {
		
	}

	public static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
	
	private static final String DOWN_FILE_EX = "下载文件异常：";
	
	/**
	 * @description：下载模板文件
	 */
	public static void downTemplet(String fileName, HttpServletResponse response) {
		File file = new File(fileName);
        response.reset();
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        try {
            response.setHeader("Content-Disposition",
                    "filename=\"" + new String(fileName.getBytes("gb2312"), StandardCharsets.ISO_8859_1) + "\";");
        } catch (UnsupportedEncodingException e1) {
            LOGGER.info(DOWN_FILE_EX, e1);
        }
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
        } catch (IOException e1) {
            LOGGER.info(DOWN_FILE_EX, e1);
        }
        try(BufferedInputStream bis=new BufferedInputStream(new FileInputStream(file));
        	BufferedOutputStream bos=new BufferedOutputStream(out)) {
            byte[] buff = new byte[2048];
            int bytesRead;
            int off = 0;
            while (-1 != (bytesRead = bis.read(buff, off, buff.length))) {
                bos.write(buff, off, bytesRead);
            }
        } catch (final IOException e) {
            LOGGER.info(DOWN_FILE_EX, e);
        }
	}
	
	/**
	 * 下载Excel文件
	 */
	public static void downExcelFile(HttpServletResponse response,String tempPath,String downName) {
		File file = new File(tempPath);
		response.reset();
		response.setContentType("application/vnd.ms-excel;charset=utf-8");
		try {
			response.setHeader("Content-Disposition",
					"attachment;filename=\"" + new String(downName.getBytes("gb2312"), StandardCharsets.ISO_8859_1) + "\";");
		} catch (UnsupportedEncodingException e1) {
			LOGGER.info(DOWN_FILE_EX, e1);
		}
		ServletOutputStream out = null;
		try {
			out = response.getOutputStream();
		} catch (IOException e1) {
			LOGGER.info(DOWN_FILE_EX, e1);
		}
		try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			BufferedOutputStream bos = new BufferedOutputStream(out)) {
			byte[] buff = new byte[2048];
			int bytesRead;
			while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesRead);
			}
		} catch (final IOException e) {
			LOGGER.info(DOWN_FILE_EX, e);
		}
	}


}
