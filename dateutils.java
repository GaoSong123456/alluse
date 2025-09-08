package com.routdata.rd3f.core.api.common.utils;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * ���ڹ�����, �̳�org.apache.commons.lang.time.DateUtils��
 * @author ThinkGem
 * @version 2014-4-15
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    
    private static String[] parsePatterns = {
        "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM", 
        "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
        "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};
    
    /**
     * yyyy-MM-dd HH:mm:ss ��ʽ
     */
    public static final String DF_YYYY_MM_DD_HH_MM_SS="yyyy-MM-dd HH:mm:ss";
    /**
     * yyyy-MM-dd��ʽ
     */
    public static final String DF_YYYY_MM="yyyy-MM-dd";
    /**
     * yyyyMMdd
     */
    public static final String DFYYYYMM="yyyyMMdd";
    
    
    /**
     * yyyyMMddHHmmss
     */
    public static final String DFYYYYMM_HHMMSS="yyyyMMddHHmmss";

    /**
     * �õ���ǰ�����ַ��� ��ʽ��yyyy-MM-dd��
     */
    public static String getDate() {
        return getDate("yyyy-MM-dd");
    }
    
    /**
     * �õ���ǰ�����ַ��� ��ʽ��yyyy-MM-dd�� pattern����Ϊ��"yyyy-MM-dd" "HH:mm:ss" "E"
     */
    public static String getDate(String pattern) {
        return DateFormatUtils.format(new Date(), pattern);
    }
    
    /**
     * �õ������ַ��� Ĭ�ϸ�ʽ��yyyy-MM-dd�� pattern����Ϊ��"yyyy-MM-dd" "HH:mm:ss" "E"
     */
    public static String formatDate(Date date, Object... pattern) {
    	//���ΪNULL������""
    	if(date==null){
    		return "";
    	}
    	
        String formatDate = null;
        if (pattern != null && pattern.length > 0) {
            formatDate = DateFormatUtils.format(date, pattern[0].toString());
        } else {
            formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
        }
        return formatDate;
    }
    
    /**
     * �õ�����ʱ���ַ�����ת����ʽ��yyyy-MM-dd HH:mm:ss��
     */
    public static String formatDateTime(Date date) {
        return formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * �õ���ǰʱ���ַ��� ��ʽ��HH:mm:ss��
     */
    public static String getTime() {
        return formatDate(new Date(), "HH:mm:ss");
    }

    /**
     * �õ���ǰ���ں�ʱ���ַ��� ��ʽ��yyyy-MM-dd HH:mm:ss��
     */
    public static String getDateTime() {
        return formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * �õ���ǰ����ַ��� ��ʽ��yyyy��
     */
    public static String getYear() {
        return formatDate(new Date(), "yyyy");
    }

    /**
     * �õ���ǰ�·��ַ��� ��ʽ��MM��
     */
    public static String getMonth() {
        return formatDate(new Date(), "MM");
    }

    /**
     * �õ������ַ��� ��ʽ��dd��
     */
    public static String getDay() {
        return formatDate(new Date(), "dd");
    }

    /**
     * �õ���ǰ�����ַ��� ��ʽ��E�����ڼ�
     */
    public static String getWeek() {
        return formatDate(new Date(), "E");
    }
    
    /**
     * �������ַ���ת��Ϊ���� ��ʽ
     * { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", 
     *   "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm",
     *   "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm" }
     */
    public static Date parseDate(String str) {
        if (StringUtils.isBlank(str)){
            return null;
        }
        try {
            return parseDate(str, parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * ��ȡ��ȥ������
     * @param date
     * @return
     */
    public static long pastDays(Date date) {
        long t = new Date().getTime()-date.getTime();
        return t/(24*60*60*1000);
    }

    /**
     * ��ȡ��ȥ��Сʱ
     * @param date
     * @return
     */
    public static long pastHour(Date date) {
        long t = new Date().getTime()-date.getTime();
        return t/(60*60*1000);
    }
    
    /**
     * ��ȡ��ȥ�ķ���
     * @param date
     * @return
     */
    public static long pastMinutes(Date date) {
        long t = new Date().getTime()-date.getTime();
        return t/(60*1000);
    }
    
    /**
     * ��ȡ��ȥ������
     * @param date
     * @return
     */
    public static long pastSeconds(Date date) {
        long t = new Date().getTime()-date.getTime();
        return t/1000;
    }
    
    /**
     * ��ȡ��ȥ�ĺ�����
     * @param date
     * @return
     */
    public static long pastMillSeconds(Date date) {
        return new Date().getTime()-date.getTime();
    }
    
    /**
     * ת��Ϊʱ�䣨��,ʱ:��:��.���룩
     * @param timeMillis
     * @return
     */
    public static String formatDateTime(long timeMillis){
        long day = timeMillis/(24*60*60*1000);
        long hour = (timeMillis/(60*60*1000)-day*24);
        long min = ((timeMillis/(60*1000))-day*24*60-hour*60);
        long s = (timeMillis/1000-day*24*60*60-hour*60*60-min*60);
        long sss = (timeMillis-day*24*60*60*1000-hour*60*60*1000-min*60*1000-s*1000);
        return (day>0?day+",":"")+hour+":"+min+":"+s+"."+sss;
    }
    
    /**
     * ��ȡ��������֮�������
     * 
     * @param before ��ʼ����
     * @param after ��������
     * @return
     */
	public static int getDistanceOfTwoDate(Date before, Date after) {
		long beforeTime = before.getTime();
		long afterTime = after.getTime();
		long days = (afterTime - beforeTime) / (1000 * 60 * 60 * 24);
		return (new Long(days)).intValue();
	}

}
