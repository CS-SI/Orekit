package org.orekit.frames;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.orbits.KeplerianExtendedPositionProvider;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


/**
 * Unit tests for {@link OriginTransformProvider}.
 *
 * @author Evan Ward
 */
public class OriginTransformProviderTest {

    /** Check methods are consistent with each other and PV provider. */
    @Test
    public void testConsistency() {
        // setup
        final DataContext context = Utils.newDataContext("regular-data");
        final Frame eci = context.getFrames().getGCRF();
        AbsoluteDate epoch = context.getTimeScales().getCcsdsEpoch();
        final Binary64Field field = Binary64Field.getInstance();
        FieldAbsoluteDate<?> fieldEpoch  = new FieldAbsoluteDate<>(field, epoch);
        double gm = Constants.GRIM5C1_EARTH_MU;
        Orbit orbit = new KeplerianOrbit(6378137 + 500e3, 1e-3, 1, 2, 3, 4,
                PositionAngleType.TRUE, eci, epoch, gm);
        final KeplerianExtendedPositionProvider pvProvider =
                new KeplerianExtendedPositionProvider(orbit);

        // action
        final OriginTransformProvider actual =
                new OriginTransformProvider(pvProvider, eci);

        // verify
        final Matcher<Rotation> isIdentityRotation = OrekitMatchers
                .distanceIs(Rotation.IDENTITY, Matchers.closeTo(0.0, 0.0));
        final PVCoordinates expectedPv = orbit.getPVCoordinates().negate();
        final Vector3D expectedP = expectedPv.getPosition();
        final Vector3D expectedV = expectedPv.getVelocity();
        final Vector3D expectedA = expectedPv.getAcceleration();
        // a few ulps is acceptable because Orbit uses shiftedBy(0.0)
        final Matcher<Vector3D> isExpectedPosition =
                OrekitMatchers.vectorCloseTo(expectedP, 6);
        final Matcher<Vector3D> isExpectedVelocity =
                OrekitMatchers.vectorCloseTo(expectedV, 6);
        final Matcher<Vector3D> isExpectedAcceleration =
                OrekitMatchers.vectorCloseTo(expectedA, 8);

        StaticTransform staticTransform = actual.getStaticTransform(epoch);
        MatcherAssert.assertThat(staticTransform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(staticTransform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(staticTransform.getRotation(),
                isIdentityRotation);

        FieldStaticTransform<?> fieldStaticTransform = actual.getStaticTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldStaticTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldStaticTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldStaticTransform.getRotation().toRotation(),
                isIdentityRotation);

        KinematicTransform kinematicTransform = actual.getKinematicTransform(epoch);
        MatcherAssert.assertThat(kinematicTransform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(kinematicTransform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(kinematicTransform.getRotation(),
                isIdentityRotation);
        MatcherAssert.assertThat(kinematicTransform.getVelocity(),
                isExpectedVelocity);
        MatcherAssert.assertThat(kinematicTransform.getRotationRate(),
                Matchers.is(Vector3D.ZERO));

        FieldKinematicTransform<?> fieldKinematicTransform = actual.getKinematicTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldKinematicTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldKinematicTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldKinematicTransform.getRotation().toRotation(),
                isIdentityRotation);
        MatcherAssert.assertThat(fieldKinematicTransform.getVelocity().toVector3D(),
                isExpectedVelocity);
        MatcherAssert.assertThat(fieldKinematicTransform.getRotationRate().toVector3D(),
                Matchers.is(Vector3D.ZERO));

        Transform transform = actual.getTransform(epoch);
        MatcherAssert.assertThat(transform.getDate(), Matchers.is(epoch));
        MatcherAssert.assertThat(transform.getTranslation(),
                isExpectedPosition);
        MatcherAssert.assertThat(transform.getRotation(),
                isIdentityRotation);
        MatcherAssert.assertThat(transform.getVelocity(),
                isExpectedVelocity);
        MatcherAssert.assertThat(transform.getRotationRate(),
                Matchers.is(Vector3D.ZERO));
        MatcherAssert.assertThat(transform.getAcceleration(),
                isExpectedAcceleration);
        MatcherAssert.assertThat(transform.getRotationAcceleration(),
                Matchers.is(Vector3D.ZERO));

        FieldTransform<?> fieldTransform = actual.getTransform(fieldEpoch);
        MatcherAssert.assertThat(fieldTransform.getFieldDate(),
                Matchers.is(fieldEpoch));
        MatcherAssert.assertThat(fieldTransform.getTranslation().toVector3D(),
                isExpectedPosition);
        MatcherAssert.assertThat(fieldTransform.getRotation().toRotation(),
                isIdentityRotation);
        MatcherAssert.assertThat(fieldTransform.getVelocity().toVector3D(),
                isExpectedVelocity);
        MatcherAssert.assertThat(fieldTransform.getRotationRate().toVector3D(),
                Matchers.is(Vector3D.ZERO));
        MatcherAssert.assertThat(fieldTransform.getAcceleration().toVector3D(),
                isExpectedAcceleration);
        MatcherAssert.assertThat(fieldTransform.getRotationAcceleration().toVector3D(),
                Matchers.is(Vector3D.ZERO));

    }
}
