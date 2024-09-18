/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network;

/**
 * A criterion has a name and the units of measure
 *
 * @author jperez
 */
public class Criterion implements Cloneable {

	/**
	 * Constant with the default criterion of a ProbNet
	 */
	private final static String defaultCriterion = "---";
	/**
	 * Constant with the default unit of a criterion
	 */
	private final static String defaultUnit = "---";
	/**
	 * Name of the criterion
	 */
	private String criterionName;
	/**
	 * Units of measure
	 */
	private String criterionUnit;
	/**
     * In the unicriteria analysis, the scale used for unicriterization
	 */
    private double unicriterizationScale;
	/**
	 * In the cost-effectiveness analysis, the scale of this criterion
	 */
	private double ceScale;
	/**
	 * In the cost-effectiveness, specifies if the criterion acts as a cost or as effectiveness
	 */
	private CECriterion ceCriterion;
	/**
	 * In temporal evolution analysis, the rate of discount of the criterion
	 */
	private double discount;
	/**
	 * In temporal evolution analysis, the measure units for the discount of the criterion
	 */
	private CycleLength.DiscountUnit discountUnit;

    /**
     * String that conforms the global criterion obtained during the unicriterization process
     */
    public static final String C_GLOBALCRITERION = "***Global utility***";
	/**
	 * Constructor with parameters
	 *
	 * @param criterionName Name of the criterion
	 * @param criterionUnit Units of measure
	 */
	public Criterion(String criterionName, String criterionUnit) {
		this.criterionName = criterionName;
		this.criterionUnit = criterionUnit;
		this.discount = 0;
        this.unicriterizationScale = 1;
		this.ceScale = 1;
		this.discountUnit = CycleLength.DiscountUnit.YEAR;
		this.ceCriterion = CECriterion.Cost; // Default.
		for (CECriterion ceCriterion : CECriterion.values()) {
			if (ceCriterion.toString().toLowerCase().contentEquals(criterionName.toLowerCase())) {
				this.ceCriterion = ceCriterion;
			}
		}
	}

	/**
	 * Constructor with only one parameter
	 *
	 * @param criterionName Name of the criterion
	 */
	public Criterion(String criterionName) {
		this(criterionName, defaultUnit);
	}

	/**
	 * Empty constructor, this creates the default criterion
	 */
	public Criterion() {
		this(defaultCriterion, defaultUnit);
	}

	public Criterion(Criterion criterion) {
		this.criterionName = criterion.criterionName;
		this.criterionUnit = criterion.criterionUnit;
		this.discount = criterion.discount;
        this.unicriterizationScale = criterion.unicriterizationScale;
		this.ceScale = criterion.ceScale;
		this.discountUnit = criterion.discountUnit;
		this.ceCriterion = criterion.ceCriterion;
	}

	public String getCriterionName() {
		return criterionName;
	}

	public void setCriterionName(String criterionName) {
		this.criterionName = criterionName;
	}

	public String getCriterionUnit() {
		return criterionUnit;
	}

	public void setCriterionUnit(String criterionUnit) {
		this.criterionUnit = criterionUnit;
	}

	public String getDefaultCriterion() {
		return defaultCriterion;
    }

    public double getUnicriterizationScale() {
        return unicriterizationScale;
    }

    public void setUnicriterizationScale(double scale) {
        this.unicriterizationScale = scale;
	}

	public double getCeScale() {
		return ceScale;
	}

	public void setCeScale(double ceScale) {
		this.ceScale = ceScale;
	}

	public CECriterion getCECriterion() {
		return ceCriterion;
	}

	public void setCECriterion(CECriterion ce_criterion) {
		this.ceCriterion = ce_criterion;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public CycleLength.DiscountUnit getDiscountUnit() {
		return this.discountUnit;
	}

	public void setDiscountUnit(CycleLength.DiscountUnit discountUnit) {
		this.discountUnit = discountUnit;
	}

	@Override public String toString() {
		return criterionName + " (" + criterionUnit + ")";
	}

	/**
	 * Gets a copy of the criterion in a new object
	 *
	 * @return copied criterion
	 */
	public Criterion clone() {
		Criterion criterion = new Criterion(this.criterionName, this.criterionUnit);
		criterion.setCECriterion(this.getCECriterion());
		criterion.setDiscount(this.getDiscount());
        criterion.setUnicriterizationScale(this.getUnicriterizationScale());
		criterion.setCeScale(this.getCeScale());
		criterion.setDiscountUnit(this.getDiscountUnit());
		return criterion;
	}

	/**
	 * Copy the attributes of the new criterion in the current object
	 *
	 * @param newCriterion Criterion to be copied
	 */
	public void copy(Criterion newCriterion) {
		this.ceCriterion = newCriterion.getCECriterion();
		this.criterionName = newCriterion.getCriterionName();
		this.criterionUnit = newCriterion.getCriterionUnit();
		this.discount = newCriterion.getDiscount();
		this.discountUnit = newCriterion.getDiscountUnit();
        this.unicriterizationScale = newCriterion.getUnicriterizationScale();
		this.ceScale = newCriterion.getCeScale();
	}

	/**
	 * Emum with the values of Cost and Effectiveness for the CE Analysis
	 */
	public enum CECriterion {
		Cost, Effectiveness
	}

}
