package org.orekit.frames;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link StaticTransform}.
 *
 * @author Evan Ward
 */
public class StaticTransformTest {

    /** Test creating, composing, and using a StaticTransform. */
    @Test
    public void testSimpleComposition() {
        // setup
        Rotation rotation = new Rotation(
                Vector3D.PLUS_K, 0.5 * FastMath.PI,
                RotationConvention.VECTOR_OPERATOR);
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // action
        StaticTransform transform = StaticTransform.compose(
                date,
                StaticTransform.of(date, rotation),
                StaticTransform.of(date, Vector3D.PLUS_I));
        StaticTransform identity = StaticTransform
                .compose(date, transform, transform.getInverse());

        // verify
        double tol = 1e-15;
        Vector3D u = transform.transformPosition(new Vector3D(1.0, 1.0, 1.0));
        Vector3D v = new Vector3D(0.0, 1.0, 1.0);
        MatcherAssert.assertThat(u, OrekitMatchers.vectorCloseTo(v, tol));
        Vector3D w = transform.transformVector(new Vector3D(1, 2, 3));
        Vector3D x = new Vector3D(-2, 1, 3);
        MatcherAssert.assertThat(w, OrekitMatchers.vectorCloseTo(x, tol));
        MatcherAssert.assertThat(transform.getTranslation(),
                OrekitMatchers.vectorCloseTo(Vector3D.MINUS_J, tol));
        MatcherAssert.assertThat(transform.getRotation().getAngle(),
                CoreMatchers.is(rotation.getAngle()));
        MatcherAssert.assertThat(transform.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR),
                CoreMatchers.is(rotation.getAxis(RotationConvention.VECTOR_OPERATOR)));
        MatcherAssert.assertThat(
                identity.transformPosition(u),
                OrekitMatchers.vectorCloseTo(u, tol));
        MatcherAssert.assertThat(
                identity.transformVector(u),
                OrekitMatchers.vectorCloseTo(u, tol));
        // check line transform
        Vector3D p1 = new Vector3D(42.1e6, 42.1e6, 42.1e6);
        Vector3D d = new Vector3D(-42e6, 42e6, -42e6);
        Line line = Line.fromDirection(p1, d, 0);
        Line actualLine = transform.transformLine(line);
        MatcherAssert.assertThat(
                actualLine.getDirection(),
                OrekitMatchers.vectorCloseTo(transform.transformVector(d).normalize(), 1));
        // account for translation
        Vector3D expectedOrigin = new Vector3D(
               -56133332.666666666, 28066666.333333333, 28066666.333333333);
        MatcherAssert.assertThat(
                actualLine.getOrigin(),
                OrekitMatchers.vectorCloseTo(expectedOrigin, 3));
        MatcherAssert.assertThat(
                actualLine.getTolerance(),
                CoreMatchers.is(line.getTolerance()));
    }

}
