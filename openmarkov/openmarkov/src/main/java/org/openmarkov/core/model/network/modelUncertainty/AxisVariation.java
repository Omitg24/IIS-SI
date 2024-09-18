/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

/**
 * Created by Jorge on 02/07/2015.
 */
public class AxisVariation {

	private DeterministicAxisVariationType variationType;

	private double variationValue;

	private double[] variationBounds = new double[2];

	public AxisVariation() {
	}

	public DeterministicAxisVariationType getVariationType() {
		return variationType;
	}

	public void setVariationType(DeterministicAxisVariationType variationType) {
		this.variationType = variationType;
	}

	public double getVariationValue() {
		return variationValue;
	}

	public void setVariationValue(double variationValue) {
		this.variationValue = variationValue;
	}

	public double[] getVariationBounds() {
		return variationBounds;
	}

	public void setVariationBounds(double[] variationBounds) {
		this.variationBounds = variationBounds;
	}

	/**
	 * Returns the upper value in the variation interval for a UncertainParameter according to the analysis type
	 *
	 * @param uncertainParameter Parameter to calculate the interval for
	 * @return Upper value in the variation interval
	 */
	public double getMaxValue(UncertainParameter uncertainParameter) {
		double maxValue = 0.0;
		double variationRatio;
		switch (variationType) {
		case PORV:
			variationRatio = Math.abs(this.variationValue) / 100;
			maxValue = uncertainParameter.getBaseLineValue() * (1 + variationRatio);
			break;
		case RORV:
			maxValue = uncertainParameter.getBaseLineValue() * this.variationValue;
			break;
		case POPP:
			maxValue = uncertainParameter.max(this.variationValue / 100);
			break;
		case UDIN:
			maxValue = this.variationBounds[1];
			break;
		default:
			break;
		}

		//        // Check if the variation value is above the upper bound (1)
		//        String variation = select(stringDatabase.getString("ComponentsAndGroups." + vertical + "VARIATION_TYPE")).getDisplay();
		//        //if (maxValue > 1 && (variation.equals(Option.VT_PORV) || variation.equals(Option.VT_RORV))) {
		//        if (maxValue > 1 && (variationType.equals(Option.VT_PORV) || variationType.equals(Option.VT_RORV))) {
		//            this.above1 = true;
		//        }
		return maxValue;
	}

	/**
	 * Returns the lower value in the variation interval for a UncertainParameter according to the analysis type
	 *
	 * @param uncertainParameter Parameter to calculate the interval for
	 * @return Lower value in the variation interval
	 */
	public double getMinValue(UncertainParameter uncertainParameter) {
		double minValue = 0.0;
		double variationRatio = 0.0;

		switch (variationType) {
		case PORV:
			variationRatio = Math.abs(this.variationValue) / 100;
			minValue = uncertainParameter.getBaseLineValue() * (1 - variationRatio);
			break;
		case RORV:
			minValue = uncertainParameter.getBaseLineValue() / this.variationValue;
			break;
		case POPP:
			minValue = uncertainParameter.min(this.variationValue / 100);
			break;
		case UDIN:
			minValue = this.variationBounds[0];
			break;
		default:
			break;
		}
		return minValue;
	}
}
