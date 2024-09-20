package org.orekit.estimation.measurements;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedPVCoordinates;

class EstimatedMeasurementTest {

    @ParameterizedTest
    @EnumSource(EstimatedMeasurementBase.Status.class)
    void testConstructor(final EstimatedMeasurementBase.Status expectedtStatus) {
        // GIVEN
        final Position mockedPosition = Mockito.mock(Position.class);
        final int expectedIteration = 2;
        final int expectedCount = 3;
        final SpacecraftState[] states = new SpacecraftState[0];
        final double[] expectedEstimatedValue = new double[] {1., 2., 3.};
        final TimeStampedPVCoordinates[] timeStampedPVCoordinates = new TimeStampedPVCoordinates[0];
        final EstimatedMeasurementBase<Position> estimatedMeasurementBase = new EstimatedMeasurementBase<>(mockedPosition,
                expectedIteration, expectedCount, states, timeStampedPVCoordinates);
        estimatedMeasurementBase.setEstimatedValue(expectedEstimatedValue);
        estimatedMeasurementBase.setStatus(expectedtStatus);
        // WHEN
        final EstimatedMeasurement<Position> estimatedMeasurement = new EstimatedMeasurement<>(estimatedMeasurementBase);
        // THEN
        Assertions.assertEquals(mockedPosition, estimatedMeasurement.getObservedMeasurement());
        Assertions.assertEquals(expectedCount, estimatedMeasurement.getCount());
        Assertions.assertEquals(expectedIteration, estimatedMeasurement.getIteration());
        Assertions.assertEquals(expectedtStatus, estimatedMeasurement.getStatus());
        Assertions.assertArrayEquals(expectedEstimatedValue, estimatedMeasurement.getEstimatedValue());
    }

}
