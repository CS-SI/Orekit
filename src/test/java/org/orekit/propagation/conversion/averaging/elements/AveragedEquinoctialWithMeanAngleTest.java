package org.orekit.propagation.conversion.averaging.elements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AveragedEquinoctialWithMeanAngleTest {

    @Test
    void toArrayTest() {
        // GIVEN
        final AveragedEquinoctialWithMeanAngle elements = new AveragedEquinoctialWithMeanAngle(1., 2., 3., 4., 5., 6.);
        // WHEN
        final double[] elementsAsArray = elements.toArray();
        // THEN
        Assertions.assertEquals(elements.getAveragedSemiMajorAxis(), elementsAsArray[0]);
        Assertions.assertEquals(elements.getAveragedEquinoctialEx(), elementsAsArray[1]);
        Assertions.assertEquals(elements.getAveragedEquinoctialEy(), elementsAsArray[2]);
        Assertions.assertEquals(elements.getAveragedHx(), elementsAsArray[3]);
        Assertions.assertEquals(elements.getAveragedHy(), elementsAsArray[4]);
        Assertions.assertEquals(elements.getAveragedMeanLongitudeArgument(), elementsAsArray[5]);
    }

}
