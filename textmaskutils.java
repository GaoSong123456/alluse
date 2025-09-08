/**
 * 
 */
package com.routdata.rd3f.core.api.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * �������ش�����
 * 
 * @author zhoucl
 *
 */
public class TextMaskUtils {
	/**
	 * �������֤
	 * 
	 * @param mobile
	 * @return
	 */
	public static String processIdNo(String idNo) {
		if (StringUtils.isBlank(idNo)) {
			return "";
		}

		int len = idNo.length();
		if (len > 16) {
			return idNo.substring(0, 6) + "******" + idNo.substring(len-4);
		} else {
			return "err";
		}
	}
	
	
	/**
	 * �������п�����
	 * ��������ǰ6λ + *** +����λ
	 * @param cardNo
	 * @return
	 */
	public static String processCardNo(String cardNo) {
		if (StringUtils.isBlank(cardNo)) {
			return "";
		}

		int len = cardNo.length();
		if (len > 12) {
			return cardNo.substring(0, 6) + StringUtils.leftPad(cardNo.substring(len-4), len-6, '*') ;
		} else {
			return "err";
		}
	}
	
	

	/**
	 * �����ֻ����� ȫ��
	 * 
	 * @param mobile
	 * @return
	 */
	public static String processMobile(String mobile) {
		if (StringUtils.isBlank(mobile)) {
			return "";
		}

		int len = mobile.length();
		if (len > 11) {
			return mobile.substring(0, 3) + "****" + mobile.substring(7);
		} else if (len == 11) {
			// �й����ֻ�����
			return mobile.substring(0, 3) + "****" + mobile.substring(7);
		} else if (len == 10) {
			return mobile.substring(0, 3) + "***" + mobile.substring(6);
		} else if (len == 9) {
			return mobile.substring(0, 3) + "***" + mobile.substring(6);
		} else if (len == 8) {
			return mobile.substring(0, 2) + "***" + mobile.substring(5);
		} else if (len == 7) {
			return mobile.substring(0, 2) + "**" + mobile.substring(4);
		} else {
			return "err";
		}
	}

	/**
	 * �����ַ��Ϣ
	 * 
	 * @param address
	 * @return
	 */
	public static String processAddress(String address) {
		if (StringUtils.isBlank(address)) {
			return "";
		}

		int len = address.length();
		if (len >= 6) {
			return address.substring(0, len - 5) + "*****";
		} else {
			return address;
		}
	}

	/**
	 * ����������Ϣ
	 * 
	 * @param name
	 * @return
	 */
	public static String processUserName(String userName) {
		if (StringUtils.isBlank(userName)) {
			return "";
		}
		
		//������������֣������ص�2λ�Ϳ�����
		if (isContainChinese(userName)) {
			if (userName.length() > 1) {
				return userName.substring(0,1)+"*"+userName.substring(2);
			}else{
				return userName;
			}
		} else {
			if(userName.length()>=3){
				return "**"+userName.substring(2);
			}else{
				return userName;
			}
		}
	}

	/**
	 * �ж��ַ������Ƿ��������
	 * 
	 * @param str
	 *            ��У���ַ���
	 * @return �Ƿ�Ϊ����
	 * @warn ����У���Ƿ�Ϊ���ı�����
	 */
	public static boolean isContainChinese(String str) {
		Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		Matcher m = p.matcher(str);
		if (m.find()) {
			return true;
		}
		return false;
	}
}
