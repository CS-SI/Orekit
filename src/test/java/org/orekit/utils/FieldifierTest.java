package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;

class FieldifierTest {

    final Field<Binary64> field = Binary64Field.getInstance();

    @BeforeAll
    static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testCircularOrbitFieldification() {
        // GIVEN
        // Create fake orbit with derivatives
        final Frame        frame = FramesFactory.getGCRF();
        final AbsoluteDate date  = new AbsoluteDate();
        final double       mu    = Constants.IERS2010_EARTH_MU;

        final CircularOrbit initialOrbit =
                new CircularOrbit(6778000, 0.1, 0.2, FastMath.toRadians(10), FastMath.toRadians(20),
                                  FastMath.toRadians(30), 1, 2, 3, 4, 5, 6, PositionAngleType.MEAN,
                                  frame, date, mu);

        // WHEN
        final FieldCircularOrbit<Binary64> fieldOrbit =
                (FieldCircularOrbit<Binary64>) Fieldifier.fieldify(field, initialOrbit);

        // THEN

        // Assert orbit type
        Assertions.assertEquals(initialOrbit.getType(), fieldOrbit.getType());

        // Assert orbital elements
        Assertions.assertEquals(initialOrbit.getA(), fieldOrbit.getA().getReal());
        Assertions.assertEquals(initialOrbit.getCircularEx(), fieldOrbit.getCircularEx().getReal());
        Assertions.assertEquals(initialOrbit.getCircularEy(), fieldOrbit.getCircularEy().getReal());
        Assertions.assertEquals(initialOrbit.getI(), fieldOrbit.getI().getReal());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNode(), fieldOrbit.getRightAscensionOfAscendingNode().getReal());
        Assertions.assertEquals(initialOrbit.getAlphaM(), fieldOrbit.getAlphaM().getReal());

        // Assert orbital elements derivatives
        Assertions.assertEquals(initialOrbit.getADot(), fieldOrbit.getADot().getReal());
        Assertions.assertEquals(initialOrbit.getCircularExDot(), fieldOrbit.getCircularExDot().getReal());
        Assertions.assertEquals(initialOrbit.getCircularEyDot(), fieldOrbit.getCircularEyDot().getReal());
        Assertions.assertEquals(initialOrbit.getIDot(), fieldOrbit.getIDot().getReal());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNodeDot(), fieldOrbit.getRightAscensionOfAscendingNodeDot().getReal());
        Assertions.assertEquals(initialOrbit.getAlphaMDot(), fieldOrbit.getAlphaMDot().getReal());
    }

    @Test
    void testCartesianOrbitFieldification() {
        // GIVEN
        // Create fake orbit with derivatives
        final Frame        frame = FramesFactory.getGCRF();
        final AbsoluteDate date  = new AbsoluteDate();
        final double       mu    = Constants.IERS2010_EARTH_MU;

        final CartesianOrbit initialOrbit = new CartesianOrbit(new PVCoordinates(new Vector3D(6778000, 1000, 2000),
                                                                                 new Vector3D(7000, 10, 20),
                                                                                 new Vector3D(1, 2, 3)),
                                                               frame, date, mu);

        // WHEN
        final FieldCartesianOrbit<Binary64> fieldOrbit =
                (FieldCartesianOrbit<Binary64>) Fieldifier.fieldify(field, initialOrbit);

        // THEN
        final PVCoordinates  refPV           = initialOrbit.getPVCoordinates();
        final Vector3D       refPosition     = refPV.getPosition();
        final Vector3D       refVelocity     = refPV.getVelocity();
        final Vector3D       refAcceleration = refPV.getAcceleration();

        // Assert orbit type
        Assertions.assertEquals(initialOrbit.getType(), fieldOrbit.getType());

        // Assert orbital elements
        final TimeStampedFieldPVCoordinates<Binary64> actualPV           = fieldOrbit.getPVCoordinates();
        final FieldVector3D<Binary64>                 actualPosition     = actualPV.getPosition();
        final FieldVector3D<Binary64>                 actualVelocity     = actualPV.getVelocity();
        final FieldVector3D<Binary64>                 actualAcceleration = actualPV.getAcceleration();

        TestUtils.validateFieldVector3D(refPosition, actualPosition, 1e-20);
        TestUtils.validateFieldVector3D(refVelocity, actualVelocity, 1e-20);
        TestUtils.validateFieldVector3D(refAcceleration, actualAcceleration, 1e-20);
    }

    @Test
    void testKeplerianOrbitFieldification() {
        // GIVEN
        // Create fake orbit with derivatives
        final Frame        frame = FramesFactory.getGCRF();
        final AbsoluteDate date  = new AbsoluteDate();
        final double       mu    = Constants.IERS2010_EARTH_MU;

        final KeplerianOrbit initialOrbit =
                new KeplerianOrbit(6778000, 0.1, FastMath.toRadians(10), FastMath.toRadians(20),
                                   FastMath.toRadians(30), FastMath.toRadians(40), 1, 2, 3, 4, 5, 6, PositionAngleType.MEAN,
                                   frame, date, mu);

        // WHEN
        final FieldKeplerianOrbit<Binary64> fieldOrbit =
                (FieldKeplerianOrbit<Binary64>) Fieldifier.fieldify(field, initialOrbit);

        // THEN

        // Assert orbit type
        Assertions.assertEquals(initialOrbit.getType(), fieldOrbit.getType());

        // Assert orbital elements
        Assertions.assertEquals(initialOrbit.getA(), fieldOrbit.getA().getReal());
        Assertions.assertEquals(initialOrbit.getE(), fieldOrbit.getE().getReal());
        Assertions.assertEquals(initialOrbit.getI(), fieldOrbit.getI().getReal());
        Assertions.assertEquals(initialOrbit.getPerigeeArgument(), fieldOrbit.getPerigeeArgument().getReal());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNode(), fieldOrbit.getRightAscensionOfAscendingNode().getReal());
        Assertions.assertEquals(initialOrbit.getMeanAnomaly(), fieldOrbit.getMeanAnomaly().getReal());

        // Assert orbital elements derivatives
        Assertions.assertEquals(initialOrbit.getADot(), fieldOrbit.getADot().getReal());
        Assertions.assertEquals(initialOrbit.getEDot(), fieldOrbit.getEDot().getReal());
        Assertions.assertEquals(initialOrbit.getIDot(), fieldOrbit.getIDot().getReal());
        Assertions.assertEquals(initialOrbit.getPerigeeArgumentDot(), fieldOrbit.getPerigeeArgumentDot().getReal());
        Assertions.assertEquals(initialOrbit.getRightAscensionOfAscendingNodeDot(), fieldOrbit.getRightAscensionOfAscendingNodeDot().getReal());
        Assertions.assertEquals(initialOrbit.getMeanAnomalyDot(), fieldOrbit.getMeanAnomalyDot().getReal());
    }

    @Test
    void testEquinoctialOrbitFieldification() {
        // GIVEN
        // Create fake orbit with derivatives
        final Frame        frame = FramesFactory.getGCRF();
        final AbsoluteDate date  = new AbsoluteDate();
        final double       mu    = Constants.IERS2010_EARTH_MU;

        final EquinoctialOrbit initialOrbit =
                new EquinoctialOrbit(6778000, 0.1, 0.2, 0.3, 0.4, FastMath.toRadians(40),
                                     1, 2, 3, 4, 5, 6, PositionAngleType.MEAN,
                                     frame, date, mu);

        // WHEN
        final FieldEquinoctialOrbit<Binary64> fieldOrbit =
                (FieldEquinoctialOrbit<Binary64>) Fieldifier.fieldify(field, initialOrbit);

        // THEN

        // Assert orbit type
        Assertions.assertEquals(initialOrbit.getType(), fieldOrbit.getType());

        // Assert orbital elements
        Assertions.assertEquals(initialOrbit.getA(), fieldOrbit.getA().getReal());
        Assertions.assertEquals(initialOrbit.getEquinoctialEx(), fieldOrbit.getEquinoctialEx().getReal());
        Assertions.assertEquals(initialOrbit.getEquinoctialEy(), fieldOrbit.getEquinoctialEy().getReal());
        Assertions.assertEquals(initialOrbit.getHx(), fieldOrbit.getHx().getReal());
        Assertions.assertEquals(initialOrbit.getHy(), fieldOrbit.getHy().getReal());
        Assertions.assertEquals(initialOrbit.getLM(), fieldOrbit.getLM().getReal());

        // Assert orbital elements derivatives
        Assertions.assertEquals(initialOrbit.getADot(), fieldOrbit.getADot().getReal());
        Assertions.assertEquals(initialOrbit.getEquinoctialExDot(), fieldOrbit.getEquinoctialExDot().getReal());
        Assertions.assertEquals(initialOrbit.getEquinoctialEyDot(), fieldOrbit.getEquinoctialEyDot().getReal());
        Assertions.assertEquals(initialOrbit.getHxDot(), fieldOrbit.getHxDot().getReal());
        Assertions.assertEquals(initialOrbit.getHyDot(), fieldOrbit.getHyDot().getReal());
        Assertions.assertEquals(initialOrbit.getLMDot(), fieldOrbit.getLMDot().getReal());
    }

}