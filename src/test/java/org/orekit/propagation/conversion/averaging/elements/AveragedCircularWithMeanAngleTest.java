package org.orekit.propagation.conversion.averaging.elements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AveragedCircularWithMeanAngleTest {

    @Test
    void toArrayTest() {
        // GIVEN
        final AveragedCircularWithMeanAngle elements = new AveragedCircularWithMeanAngle(1., 2., 3., 4., 5., 6.);
        // WHEN
        final double[] elementsAsArray = elements.toArray();
        // THEN
        Assertions.assertEquals(elements.getAveragedSemiMajorAxis(), elementsAsArray[0]);
        Assertions.assertEquals(elements.getAveragedCircularEx(), elementsAsArray[1]);
        Assertions.assertEquals(elements.getAveragedCircularEy(), elementsAsArray[2]);
        Assertions.assertEquals(elements.getAveragedInclination(), elementsAsArray[3]);
        Assertions.assertEquals(elements.getAveragedRightAscensionOfTheAscendingNode(), elementsAsArray[4]);
        Assertions.assertEquals(elements.getAveragedMeanLatitudeArgument(), elementsAsArray[5]);
    }

}
