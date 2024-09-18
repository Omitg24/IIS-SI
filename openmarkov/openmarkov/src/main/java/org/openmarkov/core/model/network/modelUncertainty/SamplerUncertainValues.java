/*
 * Copyright (c) CISIAD, UNED, Spain,  2019. Licensed under the GPLv3 licence
 * Unless required by applicable law or agreed to in writing,
 * this code is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OF ANY KIND.
 */

package org.openmarkov.core.model.network.modelUncertainty;

import java.util.ArrayList;
import java.util.List;

class SamplerUncertainValues {

	int[] indexesComplement;
	int[] indexesDirichlet;
	int[] indexesOther;
	ComplementFamily complementFamily;
	DirichletFamily dirFamily;
	FamilyDistribution otherFamily;

	public SamplerUncertainValues(List<UncertainValue> uncertainValues,
			List<Class<? extends ProbDensFunction>> functionTypes) {

		FamilyDistribution family = new FamilyDistribution(uncertainValues);
		List<UncertainValue> familyList = family.family;
		// calculates the indexes of the uncertain values for each
		// group: Other, Dirichlet and Complement
		indexesComplement = Sampler.getIndexesUncertainValuesOfClass(familyList, ComplementFunction.class);
		indexesDirichlet = Sampler.getIndexesUncertainValuesOfClass(familyList, DirichletFunction.class);
		indexesOther = Sampler.getIndexesUncertainValuesNotOfClasses(familyList, functionTypes);
		// Create the families of distributions
		List<UncertainValue> complements = constructListFromIndexes(familyList, indexesComplement);
		List<UncertainValue> dirichlets = constructListFromIndexes(familyList, indexesDirichlet);
		List<UncertainValue> others = constructListFromIndexes(familyList, indexesOther);
		complementFamily = new ComplementFamily(complements);
		dirFamily = new DirichletFamily(dirichlets);
		otherFamily = new FamilyDistribution(others);

	}

	List<UncertainValue> constructListFromIndexes(List<UncertainValue> arrayFamily, int[] indComp) {
		List<UncertainValue> array = new ArrayList<>();
		for (int i : indComp) {
			array.add(arrayFamily.get(i));
		}
		return array;
	}

}