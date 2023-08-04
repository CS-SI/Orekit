package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class FieldAbsolutePVCoordinatesHermiteInterpolatorTest {

    private PolynomialFunction randomPolynomial(int degree, Random random) {
        double[] coeff = new double[1 + degree];
        for (int j = 0; j < degree; ++j) {
            coeff[j] = random.nextDouble();
        }
        return new PolynomialFunction(coeff);
    }

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // Given
        final Frame frameMock = Mockito.mock(Frame.class);

        // When
        final FieldAbsolutePVCoordinatesHermiteInterpolator<Binary64> interpolator =
                new FieldAbsolutePVCoordinatesHermiteInterpolator<>(frameMock);

        // Then
        final CartesianDerivativesFilter expectedFilter = CartesianDerivativesFilter.USE_PVA;

        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
        Assertions.assertEquals(frameMock, interpolator.getOutputFrame());
        Assertions.assertEquals(expectedFilter, interpolator.getFilter());
    }

    @Test
    void testInterpolateNonPolynomial() {
        final Field<Binary64>       field = Binary64Field.getInstance();
        final Binary64              one   = field.getOne();
        FieldAbsoluteDate<Binary64> t0    = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame                       frame = FramesFactory.getEME2000();

        // Create sample
        List<FieldAbsolutePVCoordinates<Binary64>> sample = new ArrayList<>();
        for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
            FieldVector3D<Binary64> position =
                    new FieldVector3D<>(one.multiply(FastMath.cos(dt)), one.multiply(FastMath.sin(dt)), one.multiply(0.0));
            FieldVector3D<Binary64> velocity =
                    new FieldVector3D<>(one.multiply(-FastMath.sin(dt)), one.multiply(FastMath.cos(dt)), one.multiply(0.0));
            FieldVector3D<Binary64> acceleration =
                    new FieldVector3D<>(one.multiply(-FastMath.cos(dt)), one.multiply(-FastMath.sin(dt)), one.multiply(0.0));
            sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, velocity,
                                                        acceleration));
        }

        // Create interpolator
        final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> interpolator =
                new FieldAbsolutePVCoordinatesHermiteInterpolator<>(sample.size(), frame,
                                                                    CartesianDerivativesFilter.USE_PVA);

        // Interpolate
        for (double dt = 0; dt < 1.0; dt += 0.01) {
            FieldAbsolutePVCoordinates<Binary64> interpolated =
                    interpolator.interpolate(t0.shiftedBy(one.multiply(dt)), sample);
            FieldVector3D<Binary64> p = interpolated.getPosition();
            FieldVector3D<Binary64> v = interpolated.getVelocity();
            FieldVector3D<Binary64> a = interpolated.getAcceleration();
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
    void testInterpolatePolynomialPositionOnly() {
        final Field<Binary64>       field  = Binary64Field.getInstance();
        final Binary64              one    = field.getOne();
        Random                      random = new Random(0x88740a12e4299003l);
        FieldAbsoluteDate<Binary64> t0     = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame                       frame  = FramesFactory.getEME2000();
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

            // Create sample
            List<FieldAbsolutePVCoordinates<Binary64>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
                FieldVector3D<Binary64> position =
                        new FieldVector3D<>(one.multiply(px.value(dt)), one.multiply(py.value(dt)),
                                            one.multiply(pz.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position,
                                                            FieldVector3D.getZero(field), FieldVector3D.getZero(field)));
            }

            // Create interpolator
            final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> interpolator =
                    new FieldAbsolutePVCoordinatesHermiteInterpolator<>(sample.size(), frame,
                                                                        CartesianDerivativesFilter.USE_P);

            // Interpolate
            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<Binary64> interpolated =
                        interpolator.interpolate(t0.shiftedBy(one.multiply(dt)), sample);
                FieldVector3D<Binary64> p = interpolated.getPosition();
                FieldVector3D<Binary64> v = interpolated.getVelocity();
                FieldVector3D<Binary64> a = interpolated.getAcceleration();
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
    void testInterpolatePolynomialPV() {
        final Field<Binary64>       field  = Binary64Field.getInstance();
        final Binary64              one    = field.getOne();
        Random                      random = new Random(0xae7771c9933407bdl);
        FieldAbsoluteDate<Binary64> t0     = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame                       frame  = FramesFactory.getEME2000();
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

            // Create sample
            List<FieldAbsolutePVCoordinates<Binary64>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<Binary64> position =
                        new FieldVector3D<>(one.multiply(px.value(dt)), one.multiply(py.value(dt)),
                                            one.multiply(pz.value(dt)));
                FieldVector3D<Binary64> velocity =
                        new FieldVector3D<>(one.multiply(pxDot.value(dt)), one.multiply(pyDot.value(dt)),
                                            one.multiply(pzDot.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)), position, velocity,
                                                            FieldVector3D.getZero(field)));
            }

            // Create interpolator
            final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> interpolator =
                    new FieldAbsolutePVCoordinatesHermiteInterpolator<>(sample.size(), frame,
                                                                        CartesianDerivativesFilter.USE_PV);

            // Interpolate
            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<Binary64> interpolated =
                        interpolator.interpolate(t0.shiftedBy(one.multiply(dt)), sample);
                FieldVector3D<Binary64> p = interpolated.getPosition();
                FieldVector3D<Binary64> v = interpolated.getVelocity();
                FieldVector3D<Binary64> a = interpolated.getAcceleration();
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
    void testInterpolatePolynomialPVA() {
        final Field<Binary64>       field  = Binary64Field.getInstance();
        final Binary64              one    = field.getOne();
        Random                      random = new Random(0xfe3945fcb8bf47cel);
        FieldAbsoluteDate<Binary64> t0     = FieldAbsoluteDate.getJ2000Epoch(field);
        Frame                       frame  = FramesFactory.getEME2000();
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

            // Create sample
            List<FieldAbsolutePVCoordinates<Binary64>> sample = new ArrayList<>();
            for (double dt : new double[] { 0.0, 0.5, 1.0 }) {
                FieldVector3D<Binary64> position =
                        new FieldVector3D<>(one.multiply(px.value(dt)),
                                            one.multiply(py.value(dt)),
                                            one.multiply(pz.value(dt)));
                FieldVector3D<Binary64> velocity =
                        new FieldVector3D<>(one.multiply(pxDot.value(dt)),
                                            one.multiply(pyDot.value(dt)),
                                            one.multiply(pzDot.value(dt)));
                FieldVector3D<Binary64> acceleration =
                        new FieldVector3D<>(one.multiply(pxDotDot.value(dt)),
                                            one.multiply(pyDotDot.value(dt)),
                                            one.multiply(pzDotDot.value(dt)));
                sample.add(new FieldAbsolutePVCoordinates<>(frame, t0.shiftedBy(one.multiply(dt)),
                                                            position, velocity, acceleration));
            }

            // Create interpolator
            final FieldTimeInterpolator<FieldAbsolutePVCoordinates<Binary64>, Binary64> interpolator =
                    new FieldAbsolutePVCoordinatesHermiteInterpolator<>(sample.size(), frame,
                                                                        CartesianDerivativesFilter.USE_PVA);

            // Interpolate
            for (double dt = 0; dt < 1.0; dt += 0.01) {
                FieldAbsolutePVCoordinates<Binary64> interpolated =
                        interpolator.interpolate(t0.shiftedBy(one.multiply(dt)), sample);
                FieldVector3D<Binary64> p = interpolated.getPosition();
                FieldVector3D<Binary64> v = interpolated.getVelocity();
                FieldVector3D<Binary64> a = interpolated.getAcceleration();
                Assertions.assertEquals(px.value(dt), p.getX().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(py.value(dt), p.getY().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pz.value(dt), p.getZ().getReal(), 4.0e-16 * p.getNorm().getReal());
                Assertions.assertEquals(pxDot.value(dt), v.getX().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pyDot.value(dt), v.getY().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pzDot.value(dt), v.getZ().getReal(), 9.0e-16 * v.getNorm().getReal());
                Assertions.assertEquals(pxDotDot.value(dt), a.getX().getReal(), 9.0e-15 * a.getNorm().getReal());
                Assertions.assertEquals(pyDotDot.value(dt), a.getY().getReal(), 9.0e-15 * a.getNorm().getReal());
                Assertions.assertEquals(pzDotDot.value(dt), a.getZ().getReal(), 9.0e-15 * a.getNorm().getReal());
            }

        }

    }

}