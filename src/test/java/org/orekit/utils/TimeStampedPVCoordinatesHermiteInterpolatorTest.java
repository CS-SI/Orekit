package org.orekit.utils;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class TimeStampedPVCoordinatesHermiteInterpolatorTest {

    public static PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    @Test
    public void testInterpolatePolynomialPVA() {
        Random       random = new Random(0xfe3945fcb8bf47cel);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3D position     = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                Vector3D velocity     = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                Vector3D acceleration = new Vector3D(pxDotDot.value(dt), pyDotDot.value(dt), pzDotDot.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, acceleration));
            }

            // create interpolator
            final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                    new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_PVA);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = interpolator.interpolate(t0.shiftedBy(dt), sample);
                Vector3D                 p            = interpolated.getPosition();
                Vector3D                 v            = interpolated.getVelocity();
                Vector3D                 a            = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(py.value(dt), p.getY(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(pz.value(dt), p.getZ(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(pxDot.value(dt), v.getX(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pyDot.value(dt), v.getY(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pzDot.value(dt), v.getZ(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX(), 9.0e-15 * a.getNorm());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY(), 9.0e-15 * a.getNorm());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ(), 9.0e-15 * a.getNorm());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPV() {
        Random       random = new Random(0xae7771c9933407bdl);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                Vector3D velocity = new Vector3D(pxDot.value(dt), pyDot.value(dt), pzDot.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, Vector3D.ZERO));
            }

            // create interpolator
            final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                    new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_PV);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = interpolator.interpolate(t0.shiftedBy(dt), sample);
                Vector3D                 p            = interpolated.getPosition();
                Vector3D                 v            = interpolated.getVelocity();
                Vector3D                 a            = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(py.value(dt), p.getY(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(pz.value(dt), p.getZ(), 4.0e-16 * p.getNorm());
                Assertions.assertEquals(pxDot.value(dt), v.getX(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pyDot.value(dt), v.getY(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pzDot.value(dt), v.getZ(), 9.0e-16 * v.getNorm());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX(), 1.0e-14 * a.getNorm());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY(), 1.0e-14 * a.getNorm());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ(), 1.0e-14 * a.getNorm());
            }

        }

    }

    @Test
    public void testInterpolatePolynomialPositionOnly() {
        Random       random = new Random(0x88740a12e4299003l);
        AbsoluteDate t0     = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {

            PolynomialFunction px       = randomPolynomial(5, random);
            PolynomialFunction py       = randomPolynomial(5, random);
            PolynomialFunction pz       = randomPolynomial(5, random);
            PolynomialFunction pxDot    = px.polynomialDerivative();
            PolynomialFunction pyDot    = py.polynomialDerivative();
            PolynomialFunction pzDot    = pz.polynomialDerivative();
            PolynomialFunction pxDotDot = pxDot.polynomialDerivative();
            PolynomialFunction pyDotDot = pyDot.polynomialDerivative();
            PolynomialFunction pzDotDot = pzDot.polynomialDerivative();

            List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                Vector3D position = new Vector3D(px.value(dt), py.value(dt), pz.value(dt));
                sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, Vector3D.ZERO, Vector3D.ZERO));
            }

            // create interpolator
            final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                    new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_P);

            for (double dt = 0; dt < 1.0; dt += 0.01) {
                TimeStampedPVCoordinates interpolated = interpolator.interpolate(t0.shiftedBy(dt), sample);
                Vector3D                 p            = interpolated.getPosition();
                Vector3D                 v            = interpolated.getVelocity();
                Vector3D                 a            = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX(), 5.0e-16 * p.getNorm());
                Assertions.assertEquals(py.value(dt), p.getY(), 5.0e-16 * p.getNorm());
                Assertions.assertEquals(pz.value(dt), p.getZ(), 5.0e-16 * p.getNorm());
                Assertions.assertEquals(pxDot.value(dt), v.getX(), 7.0e-15 * v.getNorm());
                Assertions.assertEquals(pyDot.value(dt), v.getY(), 7.0e-15 * v.getNorm());
                Assertions.assertEquals(pzDot.value(dt), v.getZ(), 7.0e-15 * v.getNorm());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX(), 2.0e-13 * a.getNorm());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY(), 2.0e-13 * a.getNorm());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ(), 2.0e-13 * a.getNorm());
            }

        }
    }

    @Test
    public void testInterpolateNonPolynomial() {
        AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            Vector3D position     = new Vector3D(FastMath.cos(dt), FastMath.sin(dt), 0.0);
            Vector3D velocity     = new Vector3D(-FastMath.sin(dt), FastMath.cos(dt), 0.0);
            Vector3D acceleration = new Vector3D(-FastMath.cos(dt), -FastMath.sin(dt), 0.0);
            sample.add(new TimeStampedPVCoordinates(t0.shiftedBy(dt), position, velocity, acceleration));
        }

        // create interpolator
        final TimeInterpolator<TimeStampedPVCoordinates> interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(sample.size(), CartesianDerivativesFilter.USE_PVA);

        for (double dt = 0; dt < 1.0; dt += 0.01) {
            TimeStampedPVCoordinates interpolated = interpolator.interpolate(t0.shiftedBy(dt), sample);
            Vector3D                 p            = interpolated.getPosition();
            Vector3D                 v            = interpolated.getVelocity();
            Vector3D                 a            = interpolated.getAcceleration();
            Assertions.assertEquals(FastMath.cos(dt), p.getX(), 3.0e-10 * p.getNorm());
            Assertions.assertEquals(FastMath.sin(dt), p.getY(), 3.0e-10 * p.getNorm());
            Assertions.assertEquals(0, p.getZ(), 3.0e-10 * p.getNorm());
            Assertions.assertEquals(-FastMath.sin(dt), v.getX(), 3.0e-9 * v.getNorm());
            Assertions.assertEquals(FastMath.cos(dt), v.getY(), 3.0e-9 * v.getNorm());
            Assertions.assertEquals(0, v.getZ(), 3.0e-9 * v.getNorm());
            Assertions.assertEquals(-FastMath.cos(dt), a.getX(), 4.0e-8 * a.getNorm());
            Assertions.assertEquals(-FastMath.sin(dt), a.getY(), 4.0e-8 * a.getNorm());
            Assertions.assertEquals(0, a.getZ(), 4.0e-8 * a.getNorm());
        }

    }

    @Test
    void testConstructor() {
        // WHEN
        final TimeStampedPVCoordinatesHermiteInterpolator interpolator = new TimeStampedPVCoordinatesHermiteInterpolator();

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

        final TimeStampedPVCoordinatesHermiteInterpolator interpolator =
                new TimeStampedPVCoordinatesHermiteInterpolator(2, givenFilter);

        // When
        final CartesianDerivativesFilter gottenFilter = interpolator.getFilter();

        // Then
        Assertions.assertEquals(givenFilter, gottenFilter);
    }
}