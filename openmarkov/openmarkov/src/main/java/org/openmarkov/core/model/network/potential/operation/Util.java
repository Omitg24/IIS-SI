/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.potential.operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Stack;

import javax.security.auth.login.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author manuel
 * @author fjdiez
 * @version 1.0
 * @since OpenMarkov 1.0
 * In this class we have put all the methods that are not very well classified
 * in the rest of this application
 */
public class Util {

	// Attributes
	public static Configuration openMarkovConfiguration = null;
	private static Logger logger = LogManager.getLogger(Util.class);

	/**
	 * @param className {@code String}
	 * @param object    {@code Object}.
	 * @return {@code true} if the {@code object} is a instance
	 * of a class with the name {@code className}
	 */
	public static boolean instanceOf(String className, Object object) {
		try {
			return Class.forName(className).isInstance(object);
		} catch (Exception e) {
			logger.fatal(e);
		}
		return false;
	}

	/**
	 * @param condition {@code boolean} it it's true there is no mistake
	 * @param message   The string that is show to the user if condition
	 *                  is false
	 */ // Quitar ???
	public static void argumentCondition(boolean condition, String message) {
		if (!condition) {
			logger.info(message);
		}
	}

	/**
	 * Returns a {@code String} with a integer with commas.<p>
	 * Example: {@code printInteger(1510000)} returns 1,510,000
	 *
	 * @param number {@code long}
	 * @return The string with commas
	 */
	public static String printInteger(long number) {
		String numberString = new String();
		Stack<String> stack = new Stack<>();
		long remainder;
		do {
			remainder = number % 1000;
			number = number / 1000;
			String stackElement = new String();
			if (number > 0) {
				if (remainder < 100) {
					stackElement = "0";
					if (remainder < 10) {
						stackElement = stackElement + "0";
					}
				}
				stackElement = "," + stackElement + remainder;
			} else {
				stackElement = remainder + "";
			}
			stack.push(stackElement);
		} while (number > 0);

		while (!stack.isEmpty()) {
			numberString = numberString + stack.pop();
		}
		return numberString;
	}

	/**
	 * @param arrayInts {@code int[]}
	 * @return String with ''[num 1, num 2, ... num n]''
	 */
	public static String printArrayOfIntegers(int[] arrayInts) {
		String arrayStr = new String("[" + arrayInts[0]);
		for (int i = 1; i < arrayInts.length; i++) {
			arrayStr = arrayStr + ", " + arrayInts[i];
		}
		arrayStr = arrayStr + "]";
		return arrayStr;
	}

	/**
	 * @param msg {@code String} to the user
	 * @return readed {@code int}
	 */
	public static String readStringFromKeyboard(String msg) {
		String cadena = null;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		System.out.print(msg);
		try {
			cadena = br.readLine();
		} catch (Exception e) {
			logger.fatal(e);
		}
		return cadena;
	}

	/**
	 * Translates a {@code String} from windows style to UNIX (change \\
	 * for /)
	 *
	 * @param windowsString {@code String}
	 * @return String
	 */
	public static String platformDependentPath(String windowsString) {
		return windowsString.replace("\\", File.separator);
	}

	/**
	 * Replaces {@code path} for a new path with bars type / in unix
	 * case
	 * @param path Path
	 * @return New path
	 */
	public static String getOSPath(String path) {
		return platformDependentPath(path);
	}

	/**
	 * Returns the the value rounded to the precision
	 *
	 * @param value           the value to be rounded
	 * @param precisionString a {@code String} like "10", "1", "0.1",
	 *                        "0.001"
	 * @return the rounded value
	 */
	public static double round(double value, String precisionString) {
		double precision = Double.valueOf(precisionString);
		value = Math.round(value / precision) * precision;
		return value;
	}

	/**
	 * Returns a {@code String} of the value rounded to the precision and
	 * with the exact number of decimals (3.4 with precision 0.001 is "3.400")
	 *
	 * @param value           the value to be rounded
	 * @param precisionString a {@code String} like "10", "1", "0.1",
	 *                        "0.001"
	 * @return rounded value string
	 */
	public static String roundedString(double value, String precisionString) {
		// place of decimal point in precisionString
		int precisionStringDecimalPlace = precisionString.indexOf('.');
		double precision = Double.valueOf(precisionString);
		double roundedValue = Math.round(value / precision) * precision;
		// number of decimals in precisionString
		int numDecimals;
		if (precisionStringDecimalPlace != -1) {
			numDecimals = precisionString.length() - precisionStringDecimalPlace - 1;
		} else {
			numDecimals = -1;
		}

		String roundedString = Double.toString(roundedValue);
		if (roundedString.indexOf('.') == -1) {
			roundedString += ",0";
		} else {
			roundedString = roundedString.replace('.', ',');
		}
		// place of decimal point in roundedString
		int roundedStringDecimalPlace = roundedString.indexOf(',');

		int finalLength = roundedStringDecimalPlace + numDecimals + 1;
		if (finalLength <= roundedString.length()) {
			roundedString = roundedString.substring(0, finalLength);
		} else {
			while (finalLength > roundedString.length()) {
				roundedString += "0";
			}
		}
		return roundedString;
	}
}
