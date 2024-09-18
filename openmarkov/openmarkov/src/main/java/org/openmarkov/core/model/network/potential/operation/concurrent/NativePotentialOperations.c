#include <stdio.h>

#include "carmen_operations_concurrent_NativePotentialOperations.h"

JNIEXPORT void JNICALL Java_carmen_operations_concurrent_multiply(
		JNIEnv *env, jobject obj, jint tamTable, jdouble constantFactor) {
		
	int resultPosition;
	double mulResult;
	
	for (int resultPosition=0; resultPosition<tamTable; resultPosition++) {
		mulResult = constantFactor;

        /* increment the result coordinate and
	       find out which variable is to be incremented */
		for (int iVariable=0; iVariable < resultCoordinate.length;
				iVariable++) {
			// try by incrementing the current variable (given by iVariable)
			resultCoordinate[iVariable]++;
			if (resultCoordinate[iVariable] != resultDimension[iVariable]) {
				// we have incremented the right variable
				incrementedVariable = iVariable;
				// do not increment other variables;
				break;
			}
			/* this variable could not be incremented;
			   we set it to 0 in resultCoordinate
			   (the next iteration of the for-loop will increment
			   the next variable) */
			resultCoordinate[iVariable] = 0;
		}

		// multiply
		for (int iPotential=0; iPotential < numPotentials;
		    	iPotential++) {
			// multiply the numbers
			mulResult = mulResult *
			    tables[iPotential][potentialsPositions[iPotential]];
			// update the current position in each potential table
			potentialsPositions[iPotential] +=
				offsetAccumulate[iPotential][incrementedVariable];
		}
		result.table[resultPosition] = mulResult;
	}
}

