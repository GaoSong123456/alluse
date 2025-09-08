package com.routdata.rd3f.core.api.common.utils;

import java.util.ArrayList;
import java.util.List;

import org.owasp.esapi.ESAPI;

import com.routdata.rd3f.core.api.common.exception.ServiceException;

/**
 * ��֤�ຯ���࣬���ֻ���
 * 
 * @author zhoucl
 *
 */
public class ValidatorUtils {
	/**
	 * �ֻ����������1��ͷ���ڶ�λ3|4|5|6|7|8|9�����9λ���� ��Ӧ��������ʽΪ/^1[3456789]\d{9}$/
	 * ������ʽ�ڴ󲿷�ɨ�������������ȫ�����⣬��˴˺�����ʵ���ϲ�����������м���
	 * 
	 * @param mobile
	 * @return trueΪ��ȷ���ֻ����� falseΪ������ֻ�����
	 */
	public static boolean checkPhone(String phoneNo) {
		// �ж��Ƿ�Ϊnull�����Ϊ����ֱ�ӷ���false
		if (StringUtils.isBlank(phoneNo)) {
			return false;
		}

		// ��ȫ�����֣�ֱ�ӷ���false
		if (!StringUtils.isNumeric(phoneNo)) {
			return false;
		}

		// ����11λҲ����Ϊfalse
		if (StringUtils.length(phoneNo) != 11) {
			return false;
		}

		// ��ͷ����1������false
		if (!StringUtils.startsWith(phoneNo, "1")) {
			return false;
		}

		// �ڶ�λӦ����3|4|5|6|7|8|9
		String second = StringUtils.substring(phoneNo, 1, 2);
		if (!StringUtils.inString(second, new String[] { "3", "4", "5", "6", "7", "8", "9" })) {
			return false;
		}

		return true;
	}

	/**
	 * ����Ƿ�������,����С��
	 * 
	 * @param str
	 *            ��Ҫ�����ַ���
	 * @return
	 */
	public static boolean checkNumber(String str) {
		if (StringUtils.isBlank(str)) {
			return false;
		}
		// .�ų��ֵĴ���
		int cnt = 0;
		// -�ų��ֵĴ���
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '.') {
				cnt++;
			}

			if (str.charAt(i) == '-' && i > 0) {
				return false;
			}

			if (i == 0 || i == str.length() - 1) {
				// ��һλ����������
				if (!(Character.isDigit(str.charAt(i)) || str.charAt(i) == '-')) {
					return false;
				}
			} else {
				// ����λ�����ֻ���.������
				if (!(Character.isDigit(str.charAt(i)) || str.charAt(i) == '.')) {
					return false;
				}
			}
		}

		// ���ִ�������1����Ҫ
		if (cnt > 1) {
			return false;
		}

		return true;
	}

	/**
	 * �ж��Ƿ�ʱ����,��������.
	 * @param str
	 * @return
	 */
	public static boolean isInteger(final String str) {
		if (StringUtils.isEmpty(str)) {
			return false;
		}

		for (int i = 0; i < str.length(); i++) {
			if (i == 0 && str.charAt(0) == '-') {//��һλ��-��ֱ������
				continue;
			} else if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * �ж��Ƿ���������
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
	 * ��֤�Ƿ��ǺϷ����ļ���
	 * @param input ��Ҫ������ļ���
	 * @param input ��Ҫ������ļ���
	 * @param allowedExtensions ������ļ���չ���������չ���Զ���,�ָ�
	 * @return �Ϸ������ݷ���true�����Ϸ�����false
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
	 * ��ȡ�Ϸ����ļ���
	 * @param input ��Ҫ������ļ���
	 * @param input ��Ҫ������ļ���
	 * @param allowedExtensions ������ļ���չ���������չ����,�ָ�
	 * @return ���غϷ����ļ�ȫ·��
	 */
	public static String getValidFileName(String dir,final String fileName,final String allowedExtensions) {
		if(isValidFileName(dir, fileName, allowedExtensions)){
			return dir + fileName;
		}else{
			throw new ServiceException("�Ƿ��ļ�����");
		}
	}	
	
}
