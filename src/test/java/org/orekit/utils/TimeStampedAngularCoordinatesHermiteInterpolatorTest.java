package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.sampling.ODEFixedStepHandler;
import org.hipparchus.ode.sampling.StepNormalizer;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;
import java.util.List;

class TimeStampedAngularCoordinatesHermiteInterpolatorTest {

    @Test
    public void testInterpolationAroundPI() {

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();

        // add angular coordinates at t0: 179.999 degrees rotation along X axis
        AbsoluteDate t0 = new AbsoluteDate("2012-01-01T00:00:00.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates ac0 = new TimeStampedAngularCoordinates(t0,
                                                                              new Rotation(Vector3D.PLUS_I,
                                                                                           FastMath.toRadians(179.999),
                                                                                           RotationConvention.VECTOR_OPERATOR),
                                                                              new Vector3D(FastMath.toRadians(0), 0, 0),
                                                                              Vector3D.ZERO);
        sample.add(ac0);

        // add angular coordinates at t1: -179.999 degrees rotation (= 180.001 degrees) along X axis
        AbsoluteDate t1 = new AbsoluteDate("2012-01-01T00:00:02.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates ac1 = new TimeStampedAngularCoordinates(t1,
                                                                              new Rotation(Vector3D.PLUS_I,
                                                                                           FastMath.toRadians(-179.999),
                                                                                           RotationConvention.VECTOR_OPERATOR),
                                                                              new Vector3D(FastMath.toRadians(0), 0, 0),
                                                                              Vector3D.ZERO);
        sample.add(ac1);

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), AngularDerivativesFilter.USE_R);

        // get interpolated angular coordinates at mid time between t0 and t1
        AbsoluteDate                  t            = new AbsoluteDate("2012-01-01T00:00:01.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates interpolated = interpolator.interpolate(t, sample);

        Assertions.assertEquals(FastMath.toRadians(180), interpolated.getRotation().getAngle(), 1.0e-12);

    }

    @Test
    public void testInterpolationWithoutAcceleration() {
        AbsoluteDate date   = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0 = 0.5 * FastMath.PI;
        double       omega  = 0.05 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0, RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);
        double[] errors = interpolationErrors(reference, 1.0);
        Assertions.assertEquals(0.0, errors[0], 1.4e-15);
        Assertions.assertEquals(0.0, errors[1], 3.0e-15);
        Assertions.assertEquals(0.0, errors[2], 3.0e-14);
    }

    @Test
    public void testInterpolationWithAcceleration() {
        AbsoluteDate date   = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0 = 0.5 * FastMath.PI;
        double       omega  = 0.05 * FastMath.PI;
        double       eta    = 0.005 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  new Vector3D(eta, Vector3D.PLUS_J));
        double[] errors = interpolationErrors(reference, 1.0);
        Assertions.assertEquals(0.0, errors[0], 3.0e-5);
        Assertions.assertEquals(0.0, errors[1], 2.0e-4);
        Assertions.assertEquals(0.0, errors[2], 4.6e-3);
    }

    @Test
    public void testInterpolationNeedOffsetWrongRate() {
        AbsoluteDate date  = AbsoluteDate.GALILEO_EPOCH;
        double       omega = 2.0 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  Rotation.IDENTITY,
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double dt : new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 }) {
            TimeStampedAngularCoordinates shifted = reference.shiftedBy(dt);
            sample.add(new TimeStampedAngularCoordinates(shifted.getDate(),
                                                         shifted.getRotation(),
                                                         Vector3D.ZERO, Vector3D.ZERO));
        }

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), AngularDerivativesFilter.USE_RR);

        for (TimeStampedAngularCoordinates s : sample) {
            TimeStampedAngularCoordinates interpolated = interpolator.interpolate(s.getDate(), sample);
            Rotation                      r            = interpolated.getRotation();
            Vector3D                      rate         = interpolated.getRotationRate();
            Assertions.assertEquals(0.0, Rotation.distance(s.getRotation(), r), 2.0e-14);
            Assertions.assertEquals(0.0, Vector3D.distance(s.getRotationRate(), rate), 2.0e-13);
        }

    }

    @Test
    public void testInterpolationRotationOnly() {
        AbsoluteDate date   = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0 = 0.5 * FastMath.PI;
        double       omega  = 0.5 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            Rotation r = reference.shiftedBy(dt).getRotation();
            sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(dt), r, Vector3D.ZERO, Vector3D.ZERO));
        }

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), AngularDerivativesFilter.USE_R);

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            TimeStampedAngularCoordinates interpolated = interpolator.interpolate(date.shiftedBy(dt), sample);
            Rotation                      r            = interpolated.getRotation();
            Vector3D                      rate         = interpolated.getRotationRate();
            Assertions.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 3.0e-4);
            Assertions.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 1.0e-2);
        }

    }

    @Test
    public void testInterpolationTooSmallSample() {
        AbsoluteDate date   = AbsoluteDate.GALILEO_EPOCH;
        double       alpha0 = 0.5 * FastMath.PI;
        double       omega  = 0.5 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        Rotation                            r      = reference.shiftedBy(0.2).getRotation();
        sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(0.2), r, Vector3D.ZERO, Vector3D.ZERO));

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), 0.3, AngularDerivativesFilter.USE_R);

        try {
            interpolator.interpolate(date.shiftedBy(0.3), sample);
            Assertions.fail("an exception should have been thrown");
        }
        catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testInterpolationGTODIssue() {
        AbsoluteDate t0 = new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI());
        double[][] params = new double[][] {
                { 0.0, -0.3802356750911964, -0.9248896320037013, 7.292115030462892e-5 },
                { 4.0, 0.1345716955788532, -0.990903859488413, 7.292115033301528e-5 },
                { 8.0, -0.613127541102373, 0.7899839354960061, 7.292115037371062e-5 }
        };
        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            Rotation     r = new Rotation(row[1], 0.0, 0.0, row[2], false);
            Vector3D     o = new Vector3D(row[3], Vector3D.PLUS_K);
            sample.add(new TimeStampedAngularCoordinates(t, r, o, Vector3D.ZERO));
        }

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), 18200, AngularDerivativesFilter.USE_RR);

        for (double dt = 0; dt < 29000; dt += 120) {
            TimeStampedAngularCoordinates shifted      = sample.get(0).shiftedBy(dt);
            TimeStampedAngularCoordinates interpolated = interpolator.interpolate(t0.shiftedBy(dt), sample);
            Assertions.assertEquals(0.0,
                                    Rotation.distance(shifted.getRotation(), interpolated.getRotation()),
                                    1.3e-7);
            Assertions.assertEquals(0.0,
                                    Vector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate()),
                                    1.0e-11);
        }

    }

    private double[] interpolationErrors(final TimeStampedAngularCoordinates reference, double dt) {

        final OrdinaryDifferentialEquation ode = new OrdinaryDifferentialEquation() {
            public int getDimension() {
                return 4;
            }

            public double[] computeDerivatives(final double t, final double[] q) {
                final double omegaX = reference.getRotationRate().getX() + t * reference.getRotationAcceleration().getX();
                final double omegaY = reference.getRotationRate().getY() + t * reference.getRotationAcceleration().getY();
                final double omegaZ = reference.getRotationRate().getZ() + t * reference.getRotationAcceleration().getZ();
                return new double[] {
                        0.5 * MathArrays.linearCombination(-q[1], omegaX, -q[2], omegaY, -q[3], omegaZ),
                        0.5 * MathArrays.linearCombination(q[0], omegaX, -q[3], omegaY, q[2], omegaZ),
                        0.5 * MathArrays.linearCombination(q[3], omegaX, q[0], omegaY, -q[1], omegaZ),
                        0.5 * MathArrays.linearCombination(-q[2], omegaX, q[1], omegaY, q[0], omegaZ)
                };
            }
        };
        final List<TimeStampedAngularCoordinates> complete   = new ArrayList<TimeStampedAngularCoordinates>();
        ODEIntegrator                             integrator = new DormandPrince853Integrator(1.0e-6, 1.0, 1.0e-12, 1.0e-12);
        integrator.addStepHandler(new StepNormalizer(dt / 2000, new ODEFixedStepHandler() {
            public void handleStep(ODEStateAndDerivative state, boolean isLast) {
                final double   t = state.getTime();
                final double[] q = state.getPrimaryState();
                complete.add(new TimeStampedAngularCoordinates(reference.getDate().shiftedBy(t),
                                                               new Rotation(q[0], q[1], q[2], q[3], true),
                                                               new Vector3D(1, reference.getRotationRate(),
                                                                            t, reference.getRotationAcceleration()),
                                                               reference.getRotationAcceleration()));
            }
        }));

        double[] y = new double[] {
                reference.getRotation().getQ0(),
                reference.getRotation().getQ1(),
                reference.getRotation().getQ2(),
                reference.getRotation().getQ3()
        };
        integrator.integrate(ode, new ODEState(0, y), dt);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        sample.add(complete.get(0));
        sample.add(complete.get(complete.size() / 2));
        sample.add(complete.get(complete.size() - 1));

        // Create interpolator
        final TimeInterpolator<TimeStampedAngularCoordinates> interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator(sample.size(), AngularDerivativesFilter.USE_RRA);

        double maxRotationError     = 0;
        double maxRateError         = 0;
        double maxAccelerationError = 0;
        for (TimeStampedAngularCoordinates acRef : complete) {
            TimeStampedAngularCoordinates interpolated =
                    interpolator.interpolate(acRef.getDate(), sample);
            double rotationError = Rotation.distance(acRef.getRotation(), interpolated.getRotation());
            double rateError     = Vector3D.distance(acRef.getRotationRate(), interpolated.getRotationRate());
            double accelerationError =
                    Vector3D.distance(acRef.getRotationAcceleration(), interpolated.getRotationAcceleration());
            maxRotationError     = FastMath.max(maxRotationError, rotationError);
            maxRateError         = FastMath.max(maxRateError, rateError);
            maxAccelerationError = FastMath.max(maxAccelerationError, accelerationError);
        }

        return new double[] {
                maxRotationError, maxRateError, maxAccelerationError
        };

    }

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        // Given
        // When
        final TimeStampedAngularCoordinatesHermiteInterpolator interpolator =
                new TimeStampedAngularCoordinatesHermiteInterpolator();

        // Then
        final AngularDerivativesFilter expectedFilter = AngularDerivativesFilter.USE_RR;

        Assertions.assertEquals(AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
                                interpolator.getExtrapolationThreshold());
        Assertions.assertEquals(expectedFilter, interpolator.getFilter());
    }

}