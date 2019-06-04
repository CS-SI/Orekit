package org.orekit.forces.inertia;

import static org.junit.Assert.assertFalse;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;


public class InertialForcesTest extends AbstractLegacyForceModelTest {

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel, final AbsoluteDate date,
                                                                         final Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass) {
        try {
            java.lang.reflect.Field refInertialFrameField = InertialForces.class.getDeclaredField("referenceInertialFrame");
            refInertialFrameField.setAccessible(true);
            Frame refInertialFrame = (Frame) refInertialFrameField.get(forceModel);

            final Field<DerivativeStructure> field = position.getX().getField();
            final FieldTransform<DerivativeStructure> inertToStateFrame = refInertialFrame.getTransformTo(frame,
                                                                                                          new FieldAbsoluteDate<>(field, date));
            final FieldVector3D<DerivativeStructure>  a1                = inertToStateFrame.getCartesian().getAcceleration();
            final FieldRotation<DerivativeStructure>  r1                = inertToStateFrame.getAngular().getRotation();
            final FieldVector3D<DerivativeStructure>  o1                = inertToStateFrame.getAngular().getRotationRate();
            final FieldVector3D<DerivativeStructure>  oDot1             = inertToStateFrame.getAngular().getRotationAcceleration();

            final FieldVector3D<DerivativeStructure>  p2                = position;
            final FieldVector3D<DerivativeStructure>  v2                = velocity;

            final FieldVector3D<DerivativeStructure> crossCrossP        = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
            final FieldVector3D<DerivativeStructure> crossV             = FieldVector3D.crossProduct(o1,    v2);
            final FieldVector3D<DerivativeStructure> crossDotP          = FieldVector3D.crossProduct(oDot1, p2);

            // we intentionally DON'T include s.getPVCoordinates().getAcceleration()
            // because we want only the coupling effect of the frames transforms
            return r1.applyTo(a1).subtract(new FieldVector3D<>(2, crossV, 1, crossCrossP, 1, crossDotP));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Test
    public void testJacobianVs80Implementation() {
        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getPVCoordinates());
        final InertialForces forceModel = new InertialForces(pva.getFrame());
        assertFalse(forceModel.dependsOnPositionOnly());
        checkStateJacobianVs80Implementation(new SpacecraftState(pva), forceModel,
                                             Propagator.DEFAULT_LAW,
                                             1.0e-50, false);
    }

    @Test
    public void testNoParametersDrivers() {
        try {
            // initialization
            AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                                 new TimeComponents(13, 59, 27.816),
                                                 TimeScalesFactory.getUTC());
            double i     = FastMath.toRadians(98.7);
            double omega = FastMath.toRadians(93.0);
            double OMEGA = FastMath.toRadians(15.0 * 22.5);
            Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                             0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                             Constants.EIGEN5C_EARTH_MU);
            final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getPVCoordinates());
            final InertialForces forceModel = new InertialForces(pva.getFrame());
            forceModel.getParameterDriver(" ");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
