/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.HashSet;

import org.openmarkov.core.exception.NodeNotFoundException;

public class Util {

	/**
	 * find the symbols for left-closed, left-open, right-closed or right-open
	 * characters
	 *
	 * @param states - array of intervals of the states
	 * @return true if the states has bracket symbols; false otherwise
	 */
	public static boolean hasLimitBracketSymbols(State[] states) {

		boolean result = false;
		for (State stt : states) {
			result = (stt.getName().indexOf('[') != -1) || (
					stt.getName().indexOf('(') != -1
			) || (stt.getName().indexOf(')') != -1) || (stt.getName().indexOf(']') != -1);
			if (result) {
				break;
			}
		}
		return result;
	}

	/**
	 * Gets the corresponding position on potential edited.
	 *
	 * @param row          row of JTable
	 * @param col          column of JTable
	 * @param numStates    Number of states of the node.
	 * @param numOfParents Number of parents of the node.
	 * @return the value position on potential. -1 if the the position is
	 * undetermined.
	 */
	public static int toPositionOnPotentialReordered(int row, int col, int numStates, int numOfParents) {
		//ValuePosOnTable is the Relative position of the value on the table. 
		//First value row position is 0. The first value row position is that 
		//position next to the last parent state.
		//The firsts values of each configuration of the potential are in the last row 
		int valuePosOnTable = row - numOfParents;
		if (valuePosOnTable > -1) {
			return numStates - (valuePosOnTable + 1) + (
					numStates * (col - 1)
			);
		} else {
			return -1;
		}

	}

	/**
	 * Gets the corresponding position on JTable.
	 *
	 * @param index        index of the value in the potential
	 * @param col          column of JTable
	 * @param numOfStates  Number of states of the node.
	 * @param numOfParents Number of parents of the node.
	 * @return the value position on potential.
	 */
	public static int toPositionOnJtable(int index, int col, int numOfStates, int numOfParents) {

		return numOfParents - 1 + numOfStates + (numOfStates * (col - 1)) - index;

	}

	/**
	 * This method checks if exists the specified node.
	 * @param probNet Network
	 * @param name name of the node to search.
	 * @return true if the node exists; otherwise, false.
	 */

	//called by nodeDefinitionPanel and nodePropertiesDialogListenerAssitent
	//for static test (unused)
	public static boolean existNode(ProbNet probNet, String name) {

		try {
			probNet.getNode(name);
			return true;
		} catch (NodeNotFoundException e) {
			return false;
		}
	}

	/**
	 * Traslates a {@code String} from windows style to UNIX (change \\
	 * for /)
	 *
	 * @param windowsString {@code String}
	 * @return String
	 */
	public static String windows2unixPath(String windowsString) {
		int longStr = windowsString.length();
		String unixString = new String();
		String barra = "\\";
		char barraChar = barra.charAt(0);
		for (int i = 0; i < longStr; i++) {
			char c = windowsString.charAt(i);
			if (c == barraChar) {
				unixString = unixString + "/";
			} else {
				unixString = unixString + c;
			}
		}
		return unixString;
	}

	/**
	 * Returns a {@code String} of the value rounded to the precision and
	 * with the exact number of decimals; for example, 3.4 with precision 0.001
	 * is "3.400".
	 *
	 * @param value           the value to be rounded
	 * @param precisionString a {@code String}, such as "10", "1", "0.25", or "0.001".
	 * @return rounded value string
	 */

	public static String roundedString(double value, String precisionString) {
		int numDecimals;
		String pointPrecisionString = "";
		int indexE = precisionString.indexOf('E');
		if (indexE != -1) {
			numDecimals = Integer.parseInt(precisionString.substring(indexE + 2, indexE + 3));

			pointPrecisionString = "0.";
			for (int i = 0; i < numDecimals - 1; i++) {
				pointPrecisionString += "0";
			}
			pointPrecisionString += "1";

		} else {

			if (precisionString.indexOf(',') == -1 && precisionString.indexOf('.') != -1) {
				precisionString = precisionString.replace('.', ',');
			}
			// place of decimal point in precisionString
			int decimalPoint = precisionString.indexOf(',');
			int one = precisionString.indexOf('1');
			if (decimalPoint != -1 && one != -1) {
				numDecimals = one - decimalPoint;
			} else {
				numDecimals = -1;
			}
			pointPrecisionString = precisionString.replace(',', '.');
		}
		double precision = Double.valueOf(pointPrecisionString);
		double roundedValue = Math.round(value / precision) * precision;
		// number of decimals in precisionString

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
		if (roundedString.indexOf('.') == -1 && roundedString.indexOf(',') != -1) {
			roundedString = roundedString.replace(',', '.');
		}
		return roundedString;
	}

	/**
	 * @param value Value
	 * @param precision Precision
	 * @return rounded value with the given precision
	 * @author maryebra
	 */
	public static double roundWithPrecision(double value, String precision) {

		double valueRounded;
		String rounded;
		//double scale;
		int numDecimals;
		if (precision.equals("0.25") || precision.equals("0.5")) {
			double doublePrecision = Double.valueOf(precision);
			double roundedValue = Math.round(value / doublePrecision) * doublePrecision;
			rounded = Double.toString(roundedValue);
			int precisionStringDecimalPlace = precision.indexOf('.');
			if (precisionStringDecimalPlace != -1) {
				numDecimals = precision.length() - precisionStringDecimalPlace - 1;
			} else {
				numDecimals = -1;
			}
		} else {

			int indexE = precision.indexOf('E');
			if (indexE != -1) {
				numDecimals = Integer.parseInt(precision.substring(indexE + 2, indexE + 3));
			} else {
				int decimalPoint = precision.indexOf('.');
				int one = precision.indexOf('1');
				if (decimalPoint != -1 && one != -1) {
					numDecimals = one - decimalPoint;
				} else {
					numDecimals = 0;
				}
			}

			String pattern = "#.";
			for (int i = 0; i < numDecimals; i++) {
				pattern += "#";
			}
			DecimalFormat df = new DecimalFormat(pattern);
			rounded = df.format(value);
		}

		int roundedStringDecimalPlace = rounded.indexOf(',');
		if (roundedStringDecimalPlace == -1 && rounded.indexOf('.') == -1) {
			rounded += ",0";
		} else if (rounded.indexOf('.') != -1) {
			rounded = rounded.replace(".", ",");
		}
		roundedStringDecimalPlace = rounded.indexOf(',');
		int finalLength = roundedStringDecimalPlace + numDecimals + 1;
		if (finalLength <= rounded.length()) {
			rounded = rounded.substring(0, finalLength);
		} else {
			while (finalLength > rounded.length()) {
				rounded += "0";
			}
		}
		rounded = rounded.replace(',', '.');
		valueRounded = Double.valueOf(rounded).doubleValue();

		return valueRounded;
	}

	public static String roundWithPrecisionToString(double value, String precision) {

		String rounded;
		//double scale;
		int numDecimals;

		int indexE = precision.indexOf('E');
		if (indexE != -1) {
			numDecimals = Integer.parseInt(precision.substring(indexE + 2, indexE + 3));
		} else {
			int decimalPoint = precision.indexOf('.');
			int one = precision.indexOf('1');
			if (decimalPoint != -1 && one != -1) {
				numDecimals = one - decimalPoint;
			} else {
				numDecimals = 0;
			}
		}

		String pattern = "#.";
		for (int i = 0; i < numDecimals; i++) {
			pattern += "#";
		}
		DecimalFormat df = new DecimalFormat(pattern);
		rounded = df.format(value);

		int roundedStringDecimalPlace = rounded.indexOf(',');
		if (roundedStringDecimalPlace == -1) {
			rounded += ",0";
		}
		roundedStringDecimalPlace = rounded.indexOf(',');
		int finalLength = roundedStringDecimalPlace + numDecimals + 1;
		if (finalLength <= rounded.length()) {
			rounded = rounded.substring(0, finalLength);
		} else {
			while (finalLength > rounded.length()) {
				rounded += "0";
			}
		}
		rounded = rounded.replace(',', '.');

		return rounded;
	}

	/**
	 * It rounds 'x' with 'numDecimals' exact decimals.
	 * If the rounded number has got some zeros at the end,
	 * this function removes them (E.g.: 0.234000 &#62; 0.234)
	 * @param x X
	 * @param epsilon Epsilon
	 * @param numDecimals Number of decimals
	 * @return rounded double
	 */

	public static double roundAndReduce(double x, double epsilon, int numDecimals) {

		double lastRounded;
		double actualRounded;
		int i;
		boolean equals;

		lastRounded = roundWithPrecision(x, numDecimals);
		equals = true;
		for (i = numDecimals - 1; (i >= 0) && equals; i--) {
			actualRounded = roundWithPrecision(x, i);
			equals = ((Math.abs(actualRounded - lastRounded)) < epsilon);
			if (equals) {
				lastRounded = actualRounded;
			}
		}
		return lastRounded;
	}

	public static double roundWithSignificantFigures(double x, int numDecimals) {
		BigDecimal bigX = new BigDecimal(x);
		bigX = bigX.round(new MathContext(numDecimals));
		return bigX.doubleValue();
	}

	/**
	 * It rounds 'x' with 'numDecimals' exact decimals
	 * @param x X
	 * @param numDecimals Number of decimals
	 * @return rounded double
	 */

	public static double roundWithPrecision(double x, int numDecimals) {

		double xRounded;
		double scale;

		scale = Math.pow(10, numDecimals);
		xRounded = Math.round(x * scale);
		xRounded = xRounded / scale;

		return xRounded;
	}

	/**
	 * This method returns the name of a new node with a specified type. The
	 * name of the nodes starts with a letter that depends on its type: - Chance
	 * nodes name starts with any capital letter except 'D' and 'U'. - Decision
	 * nodes name starts with 'D'. - Utility nodes name starts with 'U'. Then
	 * the name continues with an index. If there are already a node that starts
	 * with a desired letter, an index is added to the letter to form the new
	 * name of the node.
	 *
	 * @param type          type of the new node.
	 * @param existingNames array that contains all the existing names of nodes.
	 * @return the name of the next node that is going to be created.
	 */
	public static String getNextNodeName(NodeType type, HashSet<String> existingNames) {

		String name = null;

		switch (type) {
		case CHANCE: {
			name = getNextChanceNodeName(existingNames);
			break;
		}
		case DECISION: {
			name = getNextDecisionNodeName(existingNames);
			break;
		}
		case UTILITY: {
			name = getNextUtilityNodeName(existingNames);
			break;
		}
		default:
			break;
		}
		return name;
	}

	/**
	 * This method returns the name of the next chance node. If exists the node
	 * 'A', then checks if exists the node 'B'. If this node already exists 'B',
	 * then checks the node 'C', and so on until 'Z'. If exists the node 'Z',
	 * then checks 'A1', 'B1', etc. If exists 'Z1' then checks 'A2'. The only
	 * letters that this method never returns are 'D' and 'U'.
	 *
	 * @param existingNames names of the nodes that exist.
	 * @return the name of the next chance node.
	 */
	private static String getNextChanceNodeName(HashSet<String> existingNames) {

		char letter;
		int index;
		boolean found = false;
		String name = "";

		index = 0;
		while (!found) {
			letter = 'A';
			while (!found && (letter <= 'Z')) {
				name = letter + ((index > 0) ? Integer.toString(index) : "");
				if (!existingNames.contains(name)) {
					found = true;
				} else {
					letter++;
					if ((letter == 'D') || (letter == 'U')) {
						letter++;
					}
				}
			}
			index++;
		}
		return name;
	}

	/**
	 * This method returns the name of the next decision node. If exists the
	 * node 'D', then checks if exists the node 'D1'. If this node exists, the
	 * checks the node 'D2', and so on.
	 *
	 * @param existingNames names of the nodes that exist.
	 * @return the name of the next decision node.
	 */
	private static String getNextDecisionNodeName(HashSet<String> existingNames) {

		return getNextNodeWithLetter(existingNames, 'D');
	}

	/**
	 * This method returns the name of the next utility node. If exists the node
	 * 'U', then checks if exists the node 'U1'. If this node exists, the checks
	 * the node 'U2', and so on.
	 *
	 * @param existingNames names of the nodes that exist.
	 * @return the name of the next utility node.
	 */
	private static String getNextUtilityNodeName(HashSet<String> existingNames) {

		return getNextNodeWithLetter(existingNames, 'U');
	}

	/**
	 * This method returns the name of the next node whose name starts with a
	 * specified letter. For example the letter is 'X'. If exists the node 'X',
	 * then checks if exists the node 'X1'. If this node exists, the checks the
	 * node 'X2', and so on.
	 *
	 * @param existingNames names of the nodes that exist.
	 * @param letter        letter that the next node starts with.
	 * @return the name of the next node that starts with a specified letter.
	 */
	private static String getNextNodeWithLetter(HashSet<String> existingNames, char letter) {

		String name = "";
		int index;
		boolean found = false;

		index = 0;
		while (!found) {
			name = letter + ((index > 0) ? Integer.toString(index) : "");
			if (!existingNames.contains(name)) {
				found = true;
			} else {
				index++;
			}
		}
		return name;
	}

	public static String getPath() {
		return System.getProperty("user.dir");
	}

	public static String getResourcesPath() {
		return getPath() + "\\src\\main\\resources";
	}

}
