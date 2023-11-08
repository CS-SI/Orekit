package org.orekit.frames;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Unit tests for {@link StaticTransform}.
 *
 * @author Evan Ward
 */
public class FieldStaticTransformTest {

    /** Test creating, composing, and using a StaticTransform. */
    @Test
    public void testSimpleComposition() {
        doTestSimpleComposition(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimpleComposition(Field<T> field) {
        
        // setup
        
        // Rotation of π/2 around Z axis
        FieldRotation<T> fieldRotation = new FieldRotation<>(
                FieldVector3D.getPlusK(field), field.getZero().newInstance(0.5 * FastMath.PI),
                RotationConvention.VECTOR_OPERATOR);
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);

        // action
        
        // Compose rotation and a translation of one along X axis with using two different constructors
        FieldStaticTransform<T> fieldTransform = FieldStaticTransform.compose(
                date,
                FieldStaticTransform.of(date, fieldRotation),
                FieldStaticTransform.of(date, FieldVector3D.getPlusI(field)));
        
        // From unfielded static transform
        StaticTransform transform = StaticTransform.compose(date.toAbsoluteDate(),
                                                            StaticTransform.of(date.toAbsoluteDate(), fieldRotation.toRotation()),
                                                            StaticTransform.of(date.toAbsoluteDate(), Vector3D.PLUS_I));
        FieldStaticTransform<T> fieldTransform2 = FieldStaticTransform.of(date, transform);

        // verify
        verifyTransform(field, fieldRotation, fieldTransform);
        verifyTransform(field, fieldRotation, fieldTransform2);
    }
    
    /** Verify the transform built. */
    private <T extends CalculusFieldElement<T>> void verifyTransform(final Field<T> field,
                                                                     final FieldRotation<T> rotation,
                                                                     final FieldStaticTransform<T> transform) {
        
        final T zero = field.getZero();
        final T one  = field.getOne();
        
        // identity transform
        FieldStaticTransform<T> identity = FieldStaticTransform
                        .compose(new FieldAbsoluteDate<>(field, transform.getDate()), transform, transform.getInverse());
        
        // verify
        double tol = 1e-15;
        FieldVector3D<T> u = transform.transformPosition(new FieldVector3D<>(one, one, one));
        FieldVector3D<T> v = new FieldVector3D<>(zero, one, one);
        MatcherAssert.assertThat(u.toVector3D(), OrekitMatchers.vectorCloseTo(v.toVector3D(), tol));
        FieldVector3D<T> w = transform.transformVector(new FieldVector3D<>(zero.newInstance(1), zero.newInstance(2), zero.newInstance(3)));
        FieldVector3D<T> x = new FieldVector3D<>(zero.newInstance(-2), zero.newInstance(1), zero.newInstance(3));
        MatcherAssert.assertThat(w.toVector3D(), OrekitMatchers.vectorCloseTo(x.toVector3D(), tol));
        MatcherAssert.assertThat(transform.getTranslation().toVector3D(),
                OrekitMatchers.vectorCloseTo(Vector3D.MINUS_J, tol));
        MatcherAssert.assertThat(transform.getRotation().getAngle().getReal(),
                CoreMatchers.is(rotation.getAngle().getReal()));
        MatcherAssert.assertThat(transform.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR).toVector3D(),
                CoreMatchers.is(rotation.getAxis(RotationConvention.VECTOR_OPERATOR).toVector3D()));
        MatcherAssert.assertThat(
                identity.transformPosition(u).toVector3D(),
                OrekitMatchers.vectorCloseTo(u.toVector3D(), tol));
        MatcherAssert.assertThat(
                identity.transformVector(u).toVector3D(),
                OrekitMatchers.vectorCloseTo(u.toVector3D(), tol));
        // check line transform
        FieldVector3D<T> p1 = new FieldVector3D<>(zero.newInstance(42.1e6), zero.newInstance(42.1e6), zero.newInstance(42.1e6));
        FieldVector3D<T> d  = new FieldVector3D<>(zero.newInstance(-42e6), zero.newInstance(42e6), zero.newInstance(-42e6));
        FieldLine<T> line = new FieldLine<>(p1, p1.add(d), 0);
        FieldLine<T> actualLine = transform.transformLine(line);
        MatcherAssert.assertThat(
                actualLine.getDirection().toVector3D(),
                OrekitMatchers.vectorCloseTo(transform.transformVector(d).normalize().toVector3D(), 44));
        // account for translation
        FieldVector3D<T> expectedOrigin = new FieldVector3D<>(
                        zero.newInstance(-56133332.666666666), zero.newInstance(28066666.333333333), zero.newInstance(28066666.333333333));
        MatcherAssert.assertThat(
                actualLine.getOrigin().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedOrigin.toVector3D(), 33));
        MatcherAssert.assertThat(
                actualLine.getTolerance(),
                CoreMatchers.is(line.getTolerance()));
    }

}
