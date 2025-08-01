package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SphericalConstantThrustPropulsionModelTest {

    @Test
    void testGetFlowRate() {
        // GIVEN
        final double isp = 10.;
        final Vector3D thrustVector = new Vector3D(1, 2, 3);
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, "");
        // WHEN
        final double rate = propulsionModel.getFlowRate();
        // THEN
        assertEquals(-thrustVector.getNorm() / (Constants.G0_STANDARD_GRAVITY * isp), rate, 1e-12);
        assertEquals(propulsionModel.getFlowRate(AbsoluteDate.ARBITRARY_EPOCH), rate);
    }

    @Test
    void testGetThrustVector() {
        // GIVEN
        final double isp = 10.;
        final Vector3D thrustVector = new Vector3D(1, 2, 3);
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, "");
        // WHEN
        final Vector3D actualThrustVector = propulsionModel.getThrustVector();
        // THEN
        assertArrayEquals(thrustVector.toArray(), actualThrustVector.toArray(), 1e-12);
        assertEquals(propulsionModel.getThrustVector(AbsoluteDate.ARBITRARY_EPOCH), actualThrustVector);
        final BasicConstantThrustPropulsionModel basicConstantThrustPropulsionModel = new BasicConstantThrustPropulsionModel(thrustVector.getNorm(),
                isp, thrustVector.normalize(), "");
        assertEquals(basicConstantThrustPropulsionModel.getThrustVector(), thrustVector);
    }

    @Test
    void testGetThrustVectorField() {
        // GIVEN
        final double isp = 10.;
        final double thrustMagnitude = 3;
        final double thrustAlpha = 2;
        final double thrustDelta = 1.;
        final Vector3D thrustVector = new Vector3D(thrustAlpha, thrustDelta).scalarMultiply(thrustMagnitude);
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, "");
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] parameters = MathArrays.buildArray(field, 3);
        parameters[0] = new Binary64(thrustMagnitude);
        parameters[1] = new Binary64(thrustAlpha);
        parameters[2] = new Binary64(thrustDelta);
        // WHEN
        final FieldVector3D<Binary64> actualThrustVector = propulsionModel.getThrustVector(parameters);
        // THEN
        assertArrayEquals(propulsionModel.getThrustVector(new double[] {thrustMagnitude, thrustAlpha, thrustDelta}).toArray(),
                actualThrustVector.toVector3D().toArray(), 1e-12);
    }

    @Test
    void testGetFlowRateField() {
        // GIVEN
        final double isp = 10.;
        final double thrustMagnitude = 3;
        final double thrustAlpha = 2;
        final double thrustDelta = 1.;
        final Vector3D thrustVector = new Vector3D(thrustAlpha, thrustDelta).scalarMultiply(thrustMagnitude);
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, "");
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] parameters = MathArrays.buildArray(field, 3);
        parameters[0] = new Binary64(thrustMagnitude);
        parameters[1] = new Binary64(thrustAlpha);
        parameters[2] = new Binary64(thrustDelta);
        // WHEN
        final Binary64 actualFlowRate = propulsionModel.getFlowRate(parameters);
        // THEN
        assertEquals(propulsionModel.getFlowRate(new double[] {thrustMagnitude, thrustAlpha, thrustDelta}),
                actualFlowRate.getReal(), 1e-12);
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final double isp = 10.;
        final Vector3D thrustVector = new Vector3D(1, 2, 3);
        final SphericalConstantThrustPropulsionModel propulsionModel = new SphericalConstantThrustPropulsionModel(isp,
                thrustVector, "");
        // WHEN
        final List<ParameterDriver> parameterDriverList = propulsionModel.getParametersDrivers();
        // THEN
        assertEquals(3, parameterDriverList.size());
        assertEquals(thrustVector.getNorm(), parameterDriverList.get(0).getValue());
        assertEquals(thrustVector.getAlpha(), parameterDriverList.get(1).getValue());
        assertEquals(thrustVector.getDelta(), parameterDriverList.get(2).getValue());
    }
}
