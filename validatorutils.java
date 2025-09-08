package com.routdata.rd3f.core.api.common.utils;

import java.util.ArrayList;
import java.util.List;

import org.owasp.esapi.ESAPI;

import com.routdata.rd3f.core.api.common.exception.ServiceException;

/**
 * 验证类函数类，如手机类
 * 
 * @author zhoucl
 *
 */
public class ValidatorUtils {
	/**
	 * 手机号码规则，以1开头，第二位3|4|5|6|7|8|9，后跟9位数字 对应的正则表达式为/^1[3456789]\d{9}$/
	 * 正则表达式在大部分扫描引擎会引发安全性问题，因此此函数是实现上不采用正则进行检验
	 * 
	 * @param mobile
	 * @return true为正确的手机号码 false为错误的手机号码
	 */
	public static boolean checkPhone(String phoneNo) {
		// 判断是否为null，如果为空则直接返回false
		if (StringUtils.isBlank(phoneNo)) {
			return false;
		}

		// 不全是数字，直接返回false
		if (!StringUtils.isNumeric(phoneNo)) {
			return false;
		}

		// 不是11位也返回为false
		if (StringUtils.length(phoneNo) != 11) {
			return false;
		}

		// 开头不是1，返回false
		if (!StringUtils.startsWith(phoneNo, "1")) {
			return false;
		}

		// 第二位应该是3|4|5|6|7|8|9
		String second = StringUtils.substring(phoneNo, 1, 2);
		if (!StringUtils.inString(second, new String[] { "3", "4", "5", "6", "7", "8", "9" })) {
			return false;
		}

		return true;
	}

	/**
	 * 检测是否是数字,包含小数
	 * 
	 * @param str
	 *            需要检测的字符串
	 * @return
	 */
	public static boolean checkNumber(String str) {
		if (StringUtils.isBlank(str)) {
			return false;
		}
		// .号出现的次数
		int cnt = 0;
		// -号出现的次数
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '.') {
				cnt++;
			}

			if (str.charAt(i) == '-' && i > 0) {
				return false;
			}

			if (i == 0 || i == str.length() - 1) {
				// 第一位必须是数字
				if (!(Character.isDigit(str.charAt(i)) || str.charAt(i) == '-')) {
					return false;
				}
			} else {
				// 其他位是数字或者.都可以
				if (!(Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
					return false;
				}
			}
		}

		// 出现次数大于1则需要
		if (cnt > 1) {
			return false;
		}

		return true;
	}

	/**
	 * 判断是否时整数,包含负数.
	 * @param str
	 * @return
	 */
	public static boolean isInteger(final String str) {
		if (StringUtils.isEmpty(str)) {
			return false;
		}

		for (int i = 0; i < str.length(); i++) {
			if (i == 0 && str.charAt(0) == '-') {//第一位是-就直接跳过
				continue;
			} else if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 判断是否是正整数
	 * @param str
	 * @return
	 */
	public static boolean isPositiveInteger(final String str) {
		if (StringUtils.isEmpty(str)) {
			return false;
		}

		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 验证是否是合法的文件名
	 * @param input 需要检验的文件名
	 * @param input 需要检验的文件名
	 * @param allowedExtensions 允许的文件扩展名，多个扩展名以逗号,分隔
	 * @return 合法的数据返回true，不合法返回false
	 */
	public static boolean isValidFileName(String dir,final String fileName,final String allowedExtensions) {
		if(StringUtils.containsAny(dir, new char[] {'.', '*', '?', '<', '>', '|'})){
			return false;
		}		
		List<String> allows = new ArrayList<String>();
		
		if(StringUtils.isNotBlank(allowedExtensions)){
			String[] allowTmp = allowedExtensions.split(",");
			for (String ext : allowTmp) {
				allows.add(ext);
			}
		}
		
		return ESAPI.validator().isValidFileName("com.routdata.rd3f.core.api.common.utils.ValidatorUtils", fileName, allows, false);
	}
	
	/**
	 * 获取合法的文件名
	 * @param input 需要检验的文件名
	 * @param input 需要检验的文件名
	 * @param allowedExtensions 允许的文件扩展名，多个扩展名以,分隔
	 * @return 返回合法的文件全路径
	 */
	public static String getValidFileName(String dir,final String fileName,final String allowedExtensions) {
		if(isValidFileName(dir, fileName, allowedExtensions)){
			return dir + fileName;
		}else{
			throw new ServiceException("非法文件访问");
		}
	}	
	
}
