/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

public class Tools {
	/**
	 * @param aa Array of real numbers
	 * @return The sum of an array of real numbers
	 */
	public static double sum(double[] aa) {
		double sum = 0.0D;
		for (int i = 0; i < aa.length; i++) {
			sum = sum + aa[i];
		}
		return sum;
	}

	/**
	 * @param numbers Numbers
	 * @return The product of an array of real numbers
	 */
	public static double multiply(double[] numbers) {
		double product = 1.0;
		for (int i = 0; i < numbers.length; i++) {
			product = product * numbers[i];
		}
		return product;
	}

	public static double max(double[] numbers) {
		double maximum;
		double aux;
		maximum = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < numbers.length; i++) {
			aux = numbers[i];
			if (aux > maximum) {
				maximum = aux;
			}
		}
		return maximum;
	}

	public static double min(double[] numbers) {
		double minimum;
		double aux;
		minimum = Double.POSITIVE_INFINITY;
		for (int i = 0; i < numbers.length; i++) {
			aux = numbers[i];
			if (aux < minimum) {
				minimum = aux;
			}
		}
		return minimum;
	}

	public static double[] normalize(double[] x, double mass) {
		double[] y;
		double divisor;
		int length = x.length;
		y = new double[length];
		double sum;
		sum = sum(x);
		divisor = sum / mass;
		for (int i = 0; i < length; i++) {
			y[i] = x[i] / divisor;
		}
		return y;
	}

	public static double[] normalize(double[] x) {
		return normalize(x, 1.0);
	}

	public static double square(double x) {
		return Math.pow(x, 2.0);
	}

	/**
	 * @param x Array of real numbers
	 * @return An estimation of the mean of an array of real numbers sampled
	 */
	public static double meanSample(double[] x) {
		double mu;
		int length = x.length;
		mu = (length > 0) ? (sum(x) / length) : 0.0;
		return mu;
	}

	/**
	 * @param x Array of real numbers
	 * @return An estimation of the variance of an array of real numbers
	 * sampled. It uses the equation of the quasi-variance
	 */
	public static double varianceSample(double[] x) {
		double mu;
		double sumSquares;
		double variance;
		mu = meanSample(x);
		sumSquares = 0.0;
		int length = x.length;
		if (length > 1) {
			for (int i = 0; i < length; i++) {
				sumSquares = sumSquares + Math.pow((x[i] - mu), 2);
			}
			variance = sumSquares / (length - 1);
		} else {
			variance = 0.0;
		}
		return variance;
	}
}
