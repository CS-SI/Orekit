package org.orekit.propagation.conversion.averaging.elements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AveragedCircularWithMeanAngleTest {

    @Test
    void toArrayTest() {
        // GIVEN
        final AveragedCircularWithMeanAngle elements = new AveragedCircularWithMeanAngle(1., 2., 3., 4., 5., 6.);
        // WHEN
        final double[] elementsAsArray = elements.toArray();
        // THEN
        assertEquals(elements.getAveragedSemiMajorAxis(), elementsAsArray[0]);
        assertEquals(elements.getAveragedCircularEx(), elementsAsArray[1]);
        assertEquals(elements.getAveragedCircularEy(), elementsAsArray[2]);
        assertEquals(elements.getAveragedInclination(), elementsAsArray[3]);
        assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(), elementsAsArray[4]);
        assertEquals(elements.getAveragedMeanLatitudeArgument(), elementsAsArray[5]);
    }

}
