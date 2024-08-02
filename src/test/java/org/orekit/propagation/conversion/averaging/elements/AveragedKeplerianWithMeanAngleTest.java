package org.orekit.propagation.conversion.averaging.elements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AveragedKeplerianWithMeanAngleTest {

    @Test
    void toArrayTest() {
        // GIVEN
        final AveragedKeplerianWithMeanAngle elements = new AveragedKeplerianWithMeanAngle(1., 2., 3., 4., 5., 6.);
        // WHEN
        final double[] elementsAsArray = elements.toArray();
        // THEN
        assertEquals(elements.getAveragedSemiMajorAxis(), elementsAsArray[0]);
        assertEquals(elements.getAveragedEccentricity(), elementsAsArray[1]);
        assertEquals(elements.getAveragedInclination(), elementsAsArray[2]);
        assertEquals(elements.getAveragedPerigeeArgument(), elementsAsArray[3]);
        assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(), elementsAsArray[4]);
        assertEquals(elements.getAveragedMeanAnomaly(), elementsAsArray[5]);
    }
}
