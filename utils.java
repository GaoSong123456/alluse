package com.routdata.consumemanage.web.controller.util;

import java.util.List;
import java.util.stream.Collectors;

/**      
 * @author: gaos
 * @date: 2022年6月13日     
 */
public class Utils {
	
	private Utils(){}
	
	//获取list集合中的重复元素
	public static  <E> List<E> getDuplicateElements(List<E> list) {
	    return list.stream()
	            .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
	            .entrySet().stream()
	            .filter(entry -> entry.getValue() > 1)
	            .map(entry -> entry.getKey())
	            .collect(Collectors.toList());
	}
}
