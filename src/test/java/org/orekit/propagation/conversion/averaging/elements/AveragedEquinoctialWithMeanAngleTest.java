package org.orekit.propagation.conversion.averaging.elements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AveragedEquinoctialWithMeanAngleTest {

    @Test
    void toArrayTest() {
        // GIVEN
        final AveragedEquinoctialWithMeanAngle elements = new AveragedEquinoctialWithMeanAngle(1., 2., 3., 4., 5., 6.);
        // WHEN
        final double[] elementsAsArray = elements.toArray();
        // THEN
        assertEquals(elements.getAveragedSemiMajorAxis(), elementsAsArray[0]);
        assertEquals(elements.getAveragedEquinoctialEx(), elementsAsArray[1]);
        assertEquals(elements.getAveragedEquinoctialEy(), elementsAsArray[2]);
        assertEquals(elements.getAveragedHx(), elementsAsArray[3]);
        assertEquals(elements.getAveragedHy(), elementsAsArray[4]);
        assertEquals(elements.getAveragedMeanLongitudeArgument(), elementsAsArray[5]);
    }

}
