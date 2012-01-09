package com.ichi2.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

	public static double round(double n, int decimalPlace) {
		BigDecimal result = new BigDecimal(n);
		result = result.setScale(decimalPlace, RoundingMode.HALF_EVEN);
		
		return result.doubleValue();
	}
	
}
