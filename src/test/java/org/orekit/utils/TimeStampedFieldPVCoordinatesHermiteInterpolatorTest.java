package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class TimeStampedFieldPVCoordinatesHermiteInterpolatorTest {

    @Test
    public void testInterpolatePolynomialPVA() {
        Random       random = new Random(0xfe3945fcb8bf47cel);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction py       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pz       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position =
                        TimeStampedFieldPVCoordinatesTest.createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                FieldVector3D<DerivativeStructure> velocity =
                        TimeStampedFieldPVCoordinatesTest.createVector(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt), 4);
                FieldVector3D<DerivativeStructure> acceleration =
                        TimeStampedFieldPVCoordinatesTest.createVector(pxDotDot.value(dt), pyDotDot.value(dt),
                                                                       pzDotDot.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<>(t0.shiftedBy(dt), position, velocity, acceleration));
            }

            Field<DerivativeStructure> field = sample.get(0).getDate().getField();

            // create interpolator
            final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<DerivativeStructure>, DerivativeStructure>
                    interpolator =
                    new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(),
                                                                           CartesianDerivativesFilter.USE_PVA);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsoluteDate<DerivativeStructure> t =
                        new FieldAbsoluteDate<>(field, t0.shiftedBy(dt));
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated = interpolator.interpolate(t, sample);
                FieldVector3D<DerivativeStructure>                 p            = interpolated.getPosition();
                FieldVector3D<DerivativeStructure>                 v            = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure>                 a            = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(py.value(dt), p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pz.value(dt), p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pxDot.value(dt), v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pyDot.value(dt), v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pzDot.value(dt), v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 6.0e-15 * a.getNorm().getReal());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 6.0e-15 * a.getNorm().getReal());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 6.0e-15 * a.getNorm().getReal());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPV() {
        Random       random = new Random(0xae7771c9933407bdl);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction py       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pz       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<DerivativeStructure> position =
                        TimeStampedFieldPVCoordinatesTest.createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                FieldVector3D<DerivativeStructure> velocity =
                        TimeStampedFieldPVCoordinatesTest.createVector(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<>(t0.shiftedBy(dt), position, velocity,
                                                               TimeStampedFieldPVCoordinatesTest.createVector(0, 0, 0, 4)));
            }

            Field<DerivativeStructure> field = sample.get(0).getDate().getField();

            // create interpolator
            final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<DerivativeStructure>, DerivativeStructure>
                    interpolator =
                    new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_PV);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsoluteDate<DerivativeStructure> t = new FieldAbsoluteDate<>(field, t0.shiftedBy(dt));
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated =
                        interpolator.interpolate(t, sample);
                FieldVector3D<DerivativeStructure> p = interpolated.getPosition();
                FieldVector3D<DerivativeStructure> v = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure> a = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(py.value(dt), p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pz.value(dt), p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pxDot.value(dt), v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pyDot.value(dt), v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pzDot.value(dt), v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 1.0e-14 * a.getNorm().getReal());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 1.0e-14 * a.getNorm().getReal());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPositionOnly() {
        Random       random = new Random(0x88740a12e4299003l);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction py       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pz       = TimeStampedPVCoordinatesHermiteInterpolatorTest.randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                FieldVector3D<DerivativeStructure> position =
                        TimeStampedFieldPVCoordinatesTest.createVector(px.value(dt), py.value(dt), pz.value(dt), 4);
                sample.add(new TimeStampedFieldPVCoordinates<>(t0.shiftedBy(dt),
                                                               position,
                                                               TimeStampedFieldPVCoordinatesTest.createVector(0, 0, 0, 4),
                                                               TimeStampedFieldPVCoordinatesTest.createVector(0, 0, 0, 4)));
            }

            Field<DerivativeStructure> field = sample.get(0).getDate().getField();

            // create interpolator
            final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<DerivativeStructure>, DerivativeStructure>
                    interpolator =
                    new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_P);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsoluteDate<DerivativeStructure> t =
                        new FieldAbsoluteDate<>(field, t0.shiftedBy(dt));
                TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated = interpolator.interpolate(t, sample);
                FieldVector3D<DerivativeStructure>                 p            = interpolated.getPosition();
                FieldVector3D<DerivativeStructure>                 v            = interpolated.getVelocity();
                FieldVector3D<DerivativeStructure>                 a            = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(py.value(dt), p.getY().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pz.value(dt), p.getZ().getReal(), 5.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pxDot.value(dt), v.getX().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assertions.assertEquals(pyDot.value(dt), v.getY().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assertions.assertEquals(pzDot.value(dt), v.getZ().getReal(), 7.0e-15 * v.getNorm().getReal());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 2.0e-13 * a.getNorm().getReal());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 2.0e-13 * a.getNorm().getReal());
            }

        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        List<TimeStampedFieldPVCoordinates<DerivativeStructure>> sample = new ArrayList<>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            FieldVector3D<DerivativeStructure> position =
                    TimeStampedFieldPVCoordinatesTest.createVector(FastMath.cos(dt), FastMath.sin(dt), 0.0, 4);
            FieldVector3D<DerivativeStructure> velocity =
                    TimeStampedFieldPVCoordinatesTest.createVector(-FastMath.sin(dt), FastMath.cos(dt), 0.0, 4);
            FieldVector3D<DerivativeStructure> acceleration =
                    TimeStampedFieldPVCoordinatesTest.createVector(-FastMath.cos(dt), -FastMath.sin(dt), 0.0, 4);
            sample.add(new TimeStampedFieldPVCoordinates<>(t0.shiftedBy(dt), position, velocity, acceleration));
        }

        Field<DerivativeStructure> field = sample.get(0).getDate().getField();

        // create interpolator
        final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<DerivativeStructure>, DerivativeStructure> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_PVA);

        for (double dt = 0; dt < 1.0; dt += 0.01) {
            FieldAbsoluteDate<DerivativeStructure> t =
                    new FieldAbsoluteDate<>(field, t0.shiftedBy(dt));
            TimeStampedFieldPVCoordinates<DerivativeStructure> interpolated = interpolator.interpolate(t, sample);
            FieldVector3D<DerivativeStructure>                 p            = interpolated.getPosition();
            FieldVector3D<DerivativeStructure>                 v            = interpolated.getVelocity();
            FieldVector3D<DerivativeStructure>                 a            = interpolated.getAcceleration();
            Assertions.assertEquals(FastMath.cos(dt), p.getX().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assertions.assertEquals(FastMath.sin(dt), p.getY().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assertions.assertEquals(0, p.getZ().getReal(), 3.0e-10 * p.getNorm().getReal());
            Assertions.assertEquals(-FastMath.sin(dt), v.getX().getReal(), 3.0e-9 * v.getNorm().getReal());
            Assertions.assertEquals(FastMath.cos(dt), v.getY().getReal(), 3.0e-9 * v.getNorm().getReal());
            Assertions.assertEquals(0, v.getZ().getReal(), 3.0e-9 * v.getNorm().getReal());
            Assertions.assertEquals(-FastMath.cos(dt), a.getX().getReal(), 4.0e-8 * a.getNorm().getReal());
            Assertions.assertEquals(-FastMath.sin(dt), a.getY().getReal(), 4.0e-8 * a.getNorm().getReal());
            Assertions.assertEquals(0, a.getZ().getReal(), 4.0e-8 * a.getNorm().getReal());
        }

    }

    @Test
    void testConstructor() {
        // WHEN
        final TimeStampedFieldPVCoordinatesHermiteInterpolator<Binary64> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>();

        // THEN
        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_INTERPOLATION_POINTS,
                                interpolator.getNbInterpolationPoints());
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PVA, interpolator.getFilter());

    }

    @Test
    @DisplayName("Test default constructor and getter")
    void testDefaultConstructorAndGetter() {
        // Given
        final CartesianDerivativesFilter givenFilter = CartesianDerivativesFilter.USE_PVA;

        final TimeStampedFieldPVCoordinatesHermiteInterpolator<Binary64> interpolator =
                new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(2, givenFilter);

        // When
        final CartesianDerivativesFilter gottenFilter = interpolator.getFilter();

        // Then
        Assertions.assertEquals(givenFilter, gottenFilter);
    }

}