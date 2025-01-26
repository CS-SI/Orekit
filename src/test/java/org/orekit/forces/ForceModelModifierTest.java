package org.orekit.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ForceModelModifierTest {

    @Test
    void testGetParametersDriver() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        // WHEN
        final List<ParameterDriver> drivers = modelModifier.getParametersDrivers();
        // THEN
        final List<ParameterDriver> expectedDrivers = modelModifier.getParametersDrivers();
        Assertions.assertEquals(expectedDrivers.size(), drivers.size());
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        // WHEN
        final List<EventDetector> detectors = modelModifier.getEventDetectors().collect(Collectors.toList());
        // THEN
        final List<EventDetector> expectedDetectors = modelModifier.getEventDetectors().collect(Collectors.toList());
        Assertions.assertEquals(expectedDetectors.size(), detectors.size());
    }

    @Test
    void testGetFieldEventDetectors() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        final Binary64Field field = Binary64Field.getInstance();
        // WHEN
        final List<FieldEventDetector<Binary64>> detectors = modelModifier.getFieldEventDetectors(field).collect(Collectors.toList());
        // THEN
        final List<FieldEventDetector<Binary64>> expectedDetectors = modelModifier.getFieldEventDetectors(field).collect(Collectors.toList());
        Assertions.assertEquals(expectedDetectors.size(), detectors.size());
    }

    @Test
    void testAcceleration() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        // WHEN
        final Vector3D actualAcceleration = modelModifier.acceleration(null, new double[0]);
        // THEN
        final Vector3D expectedAcceleration = forceModel.acceleration(null, new double[0]);
        Assertions.assertEquals(expectedAcceleration, actualAcceleration);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAccelerationField() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        final Binary64[] array = new Binary64[0];
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        // WHEN
        final FieldVector3D<Binary64> actualAcceleration = modelModifier.acceleration(mockedState, array);
        // THEN
        final FieldVector3D<Binary64> expectedAcceleration = forceModel.acceleration(mockedState, array);
        Assertions.assertEquals(expectedAcceleration, actualAcceleration);
    }

    @Test
    void testDependsOn() {
        // GIVEN
        final TestForceModel forceModel = new TestForceModel();
        final ForceModelModifier modelModifier = () -> forceModel;
        // WHEN & THEN
        Assertions.assertEquals(forceModel.dependsOnPositionOnly(), modelModifier.dependsOnPositionOnly());
        Assertions.assertEquals(forceModel.dependsOnAttitudeRate(), modelModifier.dependsOnAttitudeRate());
    }


    private static class TestForceModel implements ForceModel {

        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return Vector3D.ZERO;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return new FieldVector3D<>(s.getDate().getField(), Vector3D.ZERO);
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }
}
