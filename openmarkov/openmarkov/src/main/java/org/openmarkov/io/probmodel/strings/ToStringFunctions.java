/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.io.probmodel.strings;

public class ToStringFunctions {
	
	private final static char wordSeparator = '_';

	/** Given a <code>String</code> with words separated by underline character, 
	 * produces another <code>String</code> with initials in capital letters,
	 * the rest in lower case and removing the underline characters.
	 * <p>
	 * Example:<p>
	 * Input String: THIS_IS_A_TYPICAL_STRING<p>
	 * Output String: ThisIsATypicalString.
	 * @param o <code>Object</code>
	 * @return <code>String</code> */
	public static String toStringInitials(Object o) {
		String auxStr = o.toString();
		StringBuilder outStr = new StringBuilder().append(auxStr.charAt(0));
		// get underline characters
		int lastIndex = -1;
		int newIndex;
		do {
			newIndex = auxStr.indexOf(wordSeparator, lastIndex + 1);
			outStr.append(auxStr.charAt(lastIndex + 1));
			if (newIndex != -1) {
				outStr = outStr.append(
						auxStr.substring(lastIndex + 2, newIndex - 1).
						toLowerCase());
				lastIndex = newIndex;
			} else {
				outStr = outStr.append(
						auxStr.substring(lastIndex + 2).toLowerCase());
			}
		} while (newIndex != -1);
		return outStr.toString();
	}
	
	/** Given a <code>String</code> with words separated by underline character, 
	 * produces another <code>String</code> with initials in capital letters, 
	 * except the first word, the rest in lower case and removing the underline
	 * characters.<p>
	 * Example:<p>
	 * Input String: THIS_IS_A_TYPICAL_STRING<p>
	 * Output String: thisIsATypicalString.
	 * @param o <code>Object</code>
	 * @return <code>String</code> */
	public static String toStringInitialsSecond(Object o) {
		String auxStr = o.toString();
		StringBuilder outStr = new StringBuilder().append(auxStr.charAt(0));
		// get underline characters
		int lastIndex = -1;
		outStr.append(auxStr.charAt(lastIndex + 1));
		int newIndex = auxStr.indexOf(wordSeparator, lastIndex + 1);
		if (newIndex != -1) {
			outStr = outStr.append(
					auxStr.substring(lastIndex + 2, newIndex - 1).
					toLowerCase());
			lastIndex = newIndex;
		} else {
			outStr = outStr.append(
					auxStr.substring(lastIndex + 2).toLowerCase());
		}
		while (newIndex != -1) {
			newIndex = auxStr.indexOf(wordSeparator, lastIndex + 1);
			outStr.append(auxStr.charAt(lastIndex + 1));
			if (newIndex != -1) {
				outStr = outStr.append(
						auxStr.substring(lastIndex + 2, newIndex - 1).
						toLowerCase());
				lastIndex = newIndex;
			} else {
				outStr = outStr.append(
						auxStr.substring(lastIndex + 2).toLowerCase());
			}
		}
		return outStr.toString();
	}
	/** Given a <code>String</code> with words separated by underline character, 
	 * produces another <code>String</code> with initials in capital letters, 
	 * except the first word, the rest in lower case and removing the underline
	 * characters.<p>
	 * Example:<p>
	 * Input String: thisIsATypicalString <p>
	 * Output String: THIS_IS_A_TYPICAL_STRING.
	 * @param o <code>Object</code>
	 * @return <code>String</code> */
	public static String toStringEnumformat(Object o) {
		String auxStr = o.toString();
		String charString = String.valueOf(auxStr.charAt(0)).toUpperCase();
		StringBuilder outStr = new StringBuilder().append(charString);
		for (int i=1; i<auxStr.length(); i++){
			charString = String.valueOf(auxStr.charAt(i)).toUpperCase();
			outStr.append(charString);
			if (charString.equals(String.valueOf(auxStr.charAt(i)))){
				//outStr.append(wordSeparator);
				outStr.insert(outStr.length()-1, wordSeparator);
			}
		}
		
		// get underline characters
		return outStr.toString();
	}
}
