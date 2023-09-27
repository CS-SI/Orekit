/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.forces.gravity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.SphericalCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class HolmesFeatherstoneAttractionModelTest extends AbstractLegacyForceModelTest {

    private static final int SCALING = 930;

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field bodyFrameField = HolmesFeatherstoneAttractionModel.class.getDeclaredField("bodyFrame");
            bodyFrameField.setAccessible(true);
            Frame bodyFrame = (Frame) bodyFrameField.get(forceModel);

            // get the position in body frame
            final StaticTransform fromBodyFrame = bodyFrame.getStaticTransformTo(state.getFrame(), date);
            final StaticTransform toBodyFrame   = fromBodyFrame.getInverse();
            final Vector3D positionBody         = toBodyFrame.transformPosition(position.toVector3D());

            // compute gradient and Hessian
            final GradientHessian gh   = gradientHessian((HolmesFeatherstoneAttractionModel) forceModel,
                                                         date, positionBody);

            // gradient of the non-central part of the gravity field
            final double[] gInertial = fromBodyFrame.transformVector(new Vector3D(gh.getGradient())).toArray();

            // Hessian of the non-central part of the gravity field
            final RealMatrix hBody     = new Array2DRowRealMatrix(gh.getHessian(), false);
            final RealMatrix rot       = new Array2DRowRealMatrix(toBodyFrame.getRotation().getMatrix());
            final RealMatrix hInertial = rot.transpose().multiply(hBody).multiply(rot);

            // distribute all partial derivatives in a compact acceleration vector
            final double[] derivatives = new double[1 + state.getMass().getFreeParameters()];
            final DerivativeStructure[] accDer = new DerivativeStructure[3];
            for (int i = 0; i < 3; ++i) {

                // first element is value of acceleration (i.e. gradient of field)
                derivatives[0] = gInertial[i];

                // next three elements are one row of the Jacobian of acceleration (i.e. Hessian of field)
                derivatives[1] = hInertial.getEntry(i, 0);
                derivatives[2] = hInertial.getEntry(i, 1);
                derivatives[3] = hInertial.getEntry(i, 2);

                // next elements (three or four depending on mass being used or not) are left as 0

                accDer[i] = state.getMass().getFactory().build(derivatives);

            }

            return new FieldVector3D<>(accDer);

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state) {
        try {
            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            java.lang.reflect.Field bodyFrameField = HolmesFeatherstoneAttractionModel.class.getDeclaredField("bodyFrame");
            bodyFrameField.setAccessible(true);
            Frame bodyFrame = (Frame) bodyFrameField.get(forceModel);

            // get the position in body frame
            final Transform fromBodyFrame = bodyFrame.getTransformTo(state.getFrame(), date);
            final Transform toBodyFrame   = fromBodyFrame.getInverse();
            final Vector3D positionBody   = toBodyFrame.transformPosition(position.toVector3D());

            // compute gradient and Hessian
            final GradientHessian gh   = gradientHessian((HolmesFeatherstoneAttractionModel) forceModel,
                                                         date, positionBody);

            // gradient of the non-central part of the gravity field
            final double[] gInertial = fromBodyFrame.transformVector(new Vector3D(gh.getGradient())).toArray();

            // Hessian of the non-central part of the gravity field
            final RealMatrix hBody     = new Array2DRowRealMatrix(gh.getHessian(), false);
            final RealMatrix rot       = new Array2DRowRealMatrix(toBodyFrame.getRotation().getMatrix());
            final RealMatrix hInertial = rot.transpose().multiply(hBody).multiply(rot);

            // distribute all partial derivatives in a compact acceleration vector
            final double[] derivatives = new double[state.getMass().getFreeParameters()];
            final Gradient[] accDer = new Gradient[3];
            for (int i = 0; i < 3; ++i) {

                // next three elements are one row of the Jacobian of acceleration (i.e. Hessian of field)
                derivatives[0] = hInertial.getEntry(i, 0);
                derivatives[1] = hInertial.getEntry(i, 1);
                derivatives[2] = hInertial.getEntry(i, 2);

                // next elements (three or four depending on mass being used or not) are left as 0

                accDer[i] = new Gradient(gInertial[i], derivatives);

            }

            return new FieldVector3D<>(accDer);

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    private GradientHessian gradientHessian(final HolmesFeatherstoneAttractionModel hfModel,
                                            final AbsoluteDate date, final Vector3D position)
        {
        try {

        java.lang.reflect.Field providerField = HolmesFeatherstoneAttractionModel.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        NormalizedSphericalHarmonicsProvider provider = (NormalizedSphericalHarmonicsProvider) providerField.get(hfModel);
        java.lang.reflect.Method createDistancePowersArrayMethod =
                        HolmesFeatherstoneAttractionModel.class.getDeclaredMethod("createDistancePowersArray", Double.TYPE);
        createDistancePowersArrayMethod.setAccessible(true);
        java.lang.reflect.Method createCosSinArraysMethod =
                        HolmesFeatherstoneAttractionModel.class.getDeclaredMethod("createCosSinArrays", Double.TYPE, Double.TYPE);
        createCosSinArraysMethod.setAccessible(true);
        java.lang.reflect.Method computeTesseralMethod =
                        HolmesFeatherstoneAttractionModel.class.getDeclaredMethod("computeTesseral",
                                                                                  Integer.TYPE, Integer.TYPE, Integer.TYPE,
                                                                                  Double.TYPE, Double.TYPE, Double.TYPE,
                                                                                  double[].class, double[].class, double[].class,
                                                                                  double[].class, double[].class, double[].class);
        computeTesseralMethod.setAccessible(true);

        final int degree = provider.getMaxDegree();
        final int order  = provider.getMaxOrder();
        final NormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // allocate the columns for recursion
        double[] pnm0Plus2  = new double[degree + 1];
        double[] pnm0Plus1  = new double[degree + 1];
        double[] pnm0       = new double[degree + 1];
        double[] pnm1Plus1  = new double[degree + 1];
        double[] pnm1       = new double[degree + 1];
        final double[] pnm2 = new double[degree + 1];

        // compute polar coordinates
        final double x    = position.getX();
        final double y    = position.getY();
        final double z    = position.getZ();
        final double x2   = x * x;
        final double y2   = y * y;
        final double z2   = z * z;
        final double r2   = x2 + y2 + z2;
        final double r    = FastMath.sqrt (r2);
        final double rho2 = x2 + y2;
        final double rho  = FastMath.sqrt(rho2);
        final double t    = z / r;   // cos(theta), where theta is the polar angle
        final double u    = rho / r; // sin(theta), where theta is the polar angle
        final double tOu  = z / rho;

        // compute distance powers
        final double[] aOrN = (double[]) createDistancePowersArrayMethod.invoke(hfModel, provider.getAe() / r);

        // compute longitude cosines/sines
        final double[][] cosSinLambda = (double[][]) createCosSinArraysMethod.invoke(hfModel, position.getX() / rho, position.getY() / rho);

        // outer summation over order
        int    index = 0;
        double value = 0;
        final double[]   gradient = new double[3];
        final double[][] hessian  = new double[3][3];
        for (int m = degree; m >= 0; --m) {

            // compute tesseral terms
            index = ((Integer) computeTesseralMethod.invoke(hfModel, m, degree, index, t, u, tOu,
                                                            pnm0Plus2, pnm0Plus1, pnm1Plus1, pnm0, pnm1, pnm2)).intValue();

            if (m <= order) {
                // compute contribution of current order to field (equation 5 of the paper)

                // inner summation over degree, for fixed order
                double sumDegreeS               = 0;
                double sumDegreeC               = 0;
                double dSumDegreeSdR            = 0;
                double dSumDegreeCdR            = 0;
                double dSumDegreeSdTheta        = 0;
                double dSumDegreeCdTheta        = 0;
                double d2SumDegreeSdRdR         = 0;
                double d2SumDegreeSdRdTheta     = 0;
                double d2SumDegreeSdThetadTheta = 0;
                double d2SumDegreeCdRdR         = 0;
                double d2SumDegreeCdRdTheta     = 0;
                double d2SumDegreeCdThetadTheta = 0;
                for (int n = FastMath.max(2, m); n <= degree; ++n) {
                    final double qSnm         = aOrN[n] * harmonics.getNormalizedSnm(n, m);
                    final double qCnm         = aOrN[n] * harmonics.getNormalizedCnm(n, m);
                    final double nOr          = n / r;
                    final double nnP1Or2      = nOr * (n + 1) / r;
                    final double s0           = pnm0[n] * qSnm;
                    final double c0           = pnm0[n] * qCnm;
                    final double s1           = pnm1[n] * qSnm;
                    final double c1           = pnm1[n] * qCnm;
                    final double s2           = pnm2[n] * qSnm;
                    final double c2           = pnm2[n] * qCnm;
                    sumDegreeS               += s0;
                    sumDegreeC               += c0;
                    dSumDegreeSdR            -= nOr * s0;
                    dSumDegreeCdR            -= nOr * c0;
                    dSumDegreeSdTheta        += s1;
                    dSumDegreeCdTheta        += c1;
                    d2SumDegreeSdRdR         += nnP1Or2 * s0;
                    d2SumDegreeSdRdTheta     -= nOr * s1;
                    d2SumDegreeSdThetadTheta += s2;
                    d2SumDegreeCdRdR         += nnP1Or2 * c0;
                    d2SumDegreeCdRdTheta     -= nOr * c1;
                    d2SumDegreeCdThetadTheta += c2;
                }

                // contribution to outer summation over order
                final double sML = cosSinLambda[1][m];
                final double cML = cosSinLambda[0][m];
                value            = value         * u + sML * sumDegreeS + cML * sumDegreeC;
                gradient[0]      = gradient[0]   * u + sML * dSumDegreeSdR + cML * dSumDegreeCdR;
                gradient[1]      = gradient[1]   * u + m * (cML * sumDegreeS - sML * sumDegreeC);
                gradient[2]      = gradient[2]   * u + sML * dSumDegreeSdTheta + cML * dSumDegreeCdTheta;
                hessian[0][0]    = hessian[0][0] * u + sML * d2SumDegreeSdRdR + cML * d2SumDegreeCdRdR;
                hessian[1][0]    = hessian[1][0] * u + m * (cML * dSumDegreeSdR - sML * dSumDegreeCdR);
                hessian[2][0]    = hessian[2][0] * u + sML * d2SumDegreeSdRdTheta + cML * d2SumDegreeCdRdTheta;
                hessian[1][1]    = hessian[1][1] * u - m * m * (sML * sumDegreeS + cML * sumDegreeC);
                hessian[2][1]    = hessian[2][1] * u + m * (cML * dSumDegreeSdTheta - sML * dSumDegreeCdTheta);
                hessian[2][2]    = hessian[2][2] * u + sML * d2SumDegreeSdThetadTheta + cML * d2SumDegreeCdThetadTheta;

            }

            // rotate the recursion arrays
            final double[] tmp0 = pnm0Plus2;
            pnm0Plus2 = pnm0Plus1;
            pnm0Plus1 = pnm0;
            pnm0      = tmp0;
            final double[] tmp1 = pnm1Plus1;
            pnm1Plus1 = pnm1;
            pnm1      = tmp1;

        }

        // scale back
        value = FastMath.scalb(value, SCALING);
        for (int i = 0; i < 3; ++i) {
            gradient[i] = FastMath.scalb(gradient[i], SCALING);
            for (int j = 0; j <= i; ++j) {
                hessian[i][j] = FastMath.scalb(hessian[i][j], SCALING);
            }
        }


        // apply the global mu/r factor
        final double muOr = provider.getMu() / r;
        value         *= muOr;
        gradient[0]    = muOr * gradient[0] - value / r;
        gradient[1]   *= muOr;
        gradient[2]   *= muOr;
        hessian[0][0]  = muOr * hessian[0][0] - 2 * gradient[0] / r;
        hessian[1][0]  = muOr * hessian[1][0] -     gradient[1] / r;
        hessian[2][0]  = muOr * hessian[2][0] -     gradient[2] / r;
        hessian[1][1] *= muOr;
        hessian[2][1] *= muOr;
        hessian[2][2] *= muOr;

        // convert gradient and Hessian from spherical to Cartesian
        final SphericalCoordinates sc = new SphericalCoordinates(position);
        return new GradientHessian(sc.toCartesianGradient(gradient),
                                   sc.toCartesianHessian(hessian, gradient));


        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException |
                 SecurityException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    /** Container for gradient and Hessian. */
    private static class GradientHessian implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130219L;

        /** Gradient. */
        private final double[] gradient;

        /** Hessian. */
        private final double[][] hessian;

        /** Simple constructor.
         * <p>
         * A reference to the arrays is stored, they are <strong>not</strong> cloned.
         * </p>
         * @param gradient gradient
         * @param hessian hessian
         */
        public GradientHessian(final double[] gradient, final double[][] hessian) {
            this.gradient = gradient;
            this.hessian  = hessian;
        }

        /** Get a reference to the gradient.
         * @return gradient (a reference to the internal array is returned)
         */
        public double[] getGradient() {
            return gradient;
        }

        /** Get a reference to the Hessian.
         * @return Hessian (a reference to the internal array is returned)
         */
        public double[][] getHessian() {
            return hessian;
        }

    }

    @Test
    public void testRelativeNumericPrecision() {

        // this test is similar in spirit to section 4.2 of Holmes and Featherstone paper,
        // but reduced to lower degree since our reference implementation is MUCH slower
        // than the one used in the paper (Clenshaw method)
        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);
        Assertions.assertTrue(model.dependsOnPositionOnly());


        // Note that despite it uses adjustable high accuracy, the reference model
        // uses unstable formulas and hence loses lots of digits near poles.
        // This implies that if we still want about 16 digits near the poles, we
        // need to ask for at least 30 digits in computation. Setting for example
        // the following to 28 digits makes the test fail as the relative errors
        // raise to about 10^-12 near North pole and near 10^-11 near South pole.
        // The reason for this is that the reference becomes less accurate than
        // the model we are testing!
        int digits = 30;
        ReferenceFieldModel refModel = new ReferenceFieldModel(provider, digits);

        double r = 1.25;
        for (double theta = 0.01; theta < 3.14; theta += 0.1) {
            Vector3D position = new Vector3D(r * FastMath.sin(theta), 0.0, r * FastMath.cos(theta));
            Dfp refValue = refModel.nonCentralPart(null, position);
            double value = model.nonCentralPart(null, position, model.getMu());
            double relativeError = error(refValue, value).divide(refValue).toDouble();
            Assertions.assertEquals(0, relativeError, 7.0e-15);
        }

    }

    @Test
    public void testValue() {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double refValue = provider.getMu() / position.getNorm() +
                                  model.nonCentralPart(AbsoluteDate.GPS_EPOCH, position, model.getMu());
                double  value   = model.value(AbsoluteDate.GPS_EPOCH, position, model.getMu());
                Assertions.assertEquals(refValue, value, 1.0e-15 * FastMath.abs(refValue));
            }
        }

    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7201009.7124401);
        DerivativeStructure e_0 = factory.variable(1, 5e-3);
        DerivativeStructure i_0 = factory.variable(2, 98.7 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 15.0 * 22.5 * FastMath.PI / 180);
        DerivativeStructure O_0 = factory.variable(4, 93.0 * FastMath.PI / 180);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                  TideSystem.UNKNOWN,
                                                                                                  c, s);
        HolmesFeatherstoneAttractionModel forceModel =
                        new HolmesFeatherstoneAttractionModel(itrf, provider);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-14, 6.0e-8, 1.8e-11, 1.8e-10,
                                  1, false);
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 0);
        DerivativeStructure a_0 = factory.variable(0, 7201009.7124401);
        DerivativeStructure e_0 = factory.variable(1, 1e-3);
        DerivativeStructure i_0 = factory.variable(2, 98.7 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 15.0 * 22.5 * FastMath.PI / 180);
        DerivativeStructure O_0 = factory.variable(4, 93.0 * FastMath.PI / 180);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                  TideSystem.UNKNOWN,
                                                                                                  c, s);
        HolmesFeatherstoneAttractionModel forceModel =
                        new HolmesFeatherstoneAttractionModel(itrf, provider);

        //FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(100.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }
    @Test
    public void testGradient() {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double[] refGradient = gradient(model, null, position, 1.0e-3);
                double norm  = FastMath.sqrt(refGradient[0] * refGradient[0] +
                                             refGradient[1] * refGradient[1] +
                                             refGradient[2] * refGradient[2]);
                double[] gradient = model.gradient(null, position, model.getMu());
                double errorX = refGradient[0] - gradient[0];
                double errorY = refGradient[1] - gradient[1];
                double errorZ = refGradient[2] - gradient[2];
                double relativeError = FastMath.sqrt(errorX * errorX + errorY * errorY + errorZ * errorZ) /
                                       norm;
                Assertions.assertEquals(0, relativeError, 3.0e-12);
            }
        }

    }

    @Test
    public void testHessian() {

        int max = 50;
        NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        HolmesFeatherstoneAttractionModel model =
                new HolmesFeatherstoneAttractionModel(itrf, provider);

        double r = 1.25;
        for (double lambda = 0; lambda < 2 * FastMath.PI; lambda += 0.5) {
            for (double theta = 0.05; theta < 3.11; theta += 0.03) {
                Vector3D position = new Vector3D(r * FastMath.sin(theta) * FastMath.cos(lambda),
                                                 r * FastMath.sin(theta) * FastMath.sin(lambda),
                                                 r * FastMath.cos(theta));
                double[][] refHessian = hessian(model, null, position, 1.0e-3);
                double[][] hessian = gradientHessian(model, null, position).getHessian();
                double normH2 = 0;
                double normE2 = 0;
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        double error = refHessian[i][j] - hessian[i][j];
                        normH2 += refHessian[i][j] * refHessian[i][j];
                        normE2 += error * error;
                    }
                }
                Assertions.assertEquals(0, FastMath.sqrt(normE2 / normH2), 5.0e-12);
            }
        }

    }

    private Dfp error(Dfp refValue, double value) {
        return refValue.getField().newDfp(value).subtract(refValue);
    }

    private double[] gradient(final HolmesFeatherstoneAttractionModel model,
                              final AbsoluteDate date, final Vector3D position, final double h)
        {
        return new double[] {
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_I)), model.getMu()),
                          h),
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_J)), model.getMu()),
                          h),
            differential8(model.nonCentralPart(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.nonCentralPart(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_K)), model.getMu()),
                          h)
        };
    }

    private double[][] hessian(final HolmesFeatherstoneAttractionModel model,
                               final AbsoluteDate date, final Vector3D position, final double h)
        {
        return new double[][] {
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_I)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_I)), model.getMu()),
                          h),
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_J)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_J)), model.getMu()),
                          h),
            differential8(model.gradient(date, position.add(new Vector3D(-4 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-3 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-2 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(-1 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+1 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+2 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+3 * h, Vector3D.PLUS_K)), model.getMu()),
                          model.gradient(date, position.add(new Vector3D(+4 * h, Vector3D.PLUS_K)), model.getMu()),
                          h)
        };
    }

    /** Dummy provider for testing purposes.
     * <p>
     * This providers correspond to the testing regime used in the
     * Holmes and Featherstone paper, who credit it to D. M. Gleason.
     * </p>
     */
    private static class GleasonProvider implements NormalizedSphericalHarmonicsProvider {

        private final int degree;
        private final int order;

        public GleasonProvider(int degree, int order) {
            this.degree = degree;
            this.order = order;
        }

        public int getMaxDegree() {
            return degree;
        }

        public int getMaxOrder() {
            return order;
        }

        public double getMu() {
            return 1;
        }

        public double getAe() {
            return 1;
        }

        public AbsoluteDate getReferenceDate() {
            return null;
        }

        public TideSystem getTideSystem() {
            return TideSystem.UNKNOWN;
        }

        @Override
        public NormalizedSphericalHarmonics onDate(final AbsoluteDate date) {
            return new NormalizedSphericalHarmonics() {
                @Override
                public double getNormalizedCnm(int n, int m) {
                    return 1;
                }

                @Override
                public double getNormalizedSnm(int n, int m) {
                    return 1;
                }

                @Override
                public AbsoluteDate getDate() {
                    return date;
                }
            };
        }

    }

    // rough test to determine if J2 alone creates heliosynchronism
    @Test
    public void testHelioSynchronous()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        Transform itrfToEME2000 = itrf.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, poleAligned, date, mu);
        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = normalizedC20;
        double[][] s = new double[3][1];
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(6378136.460, mu,
                                                                                                                 TideSystem.UNKNOWN,
                                                                                                                 c, s)));

        // let the step handler perform the test
        propagator.setStepHandler(Constants.JULIAN_DAY, new SpotStepHandler(date, mu));
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.propagate(date.shiftedBy(7 * Constants.JULIAN_DAY));
        Assertions.assertTrue(propagator.getCalls() < 9200);

    }

    private static class SpotStepHandler implements OrekitFixedStepHandler {

        public SpotStepHandler(AbsoluteDate date, double mu) {
            sun       = CelestialBodyFactory.getSun();
            previous  = Double.NaN;
        }

        private PVCoordinatesProvider sun;
        private double previous;
        public void handleStep(SpacecraftState currentState) {


            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos = sun.getPosition(current , FramesFactory.getEME2000());
            Vector3D normal = currentState.getPVCoordinates().getMomentum();
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                Assertions.assertEquals(previous, angle, 0.0013);
            }
            previous = angle;
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    @Test
    public void testEcksteinHechlerReference() {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToEME2000 = itrf.getTransformTo(FramesFactory.getEME2000(), date);
        Vector3D pole           = itrfToEME2000.transformVector(Vector3D.PLUS_K);
        Frame poleAligned       = new Frame(FramesFactory.getEME2000(),
                                            new Transform(date, new Rotation(pole, Vector3D.PLUS_K)),
                                            "pole aligned", true);

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                poleAligned, date, mu);

        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf,
                                                                       GravityFieldFactory.getNormalizedProvider(ae, mu,
                                                                                                                 TideSystem.UNKNOWN,
                                                                       new double[][] {
                { 0.0 }, { 0.0 }, { normalizedC20 }, { normalizedC30 },
                { normalizedC40 }, { normalizedC50 }, { normalizedC60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        })));

        // let the step handler perform the test
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setStepHandler(20, new EckStepHandler(initialOrbit, ae,
                                                         unnormalizedC20, unnormalizedC30, unnormalizedC40,
                                                         unnormalizedC50, unnormalizedC60));
        propagator.propagate(date.shiftedBy(50000));
        Assertions.assertTrue(propagator.getCalls() < 1100);

    }

    private static class EckStepHandler implements OrekitFixedStepHandler {

        /** Body mu */
        private static final double mu =  3.986004415e+14;

        private EckStepHandler(Orbit initialOrbit, double ae,
                               double c20, double c30, double c40, double c50, double c60)
        {
            referencePropagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              ae, mu, c20, c30, c40, c50, c60);
        }

        private EcksteinHechlerPropagator referencePropagator;
        public void handleStep(SpacecraftState currentState) {

            SpacecraftState EHPOrbit   = referencePropagator.propagate(currentState.getDate());
            Vector3D posEHP  = EHPOrbit.getPosition();
            Vector3D posDROZ = currentState.getPosition();
            Vector3D velEHP  = EHPOrbit.getPVCoordinates().getVelocity();
            Vector3D dif     = posEHP.subtract(posDROZ);

            Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
            Vector3D W = EHPOrbit.getPVCoordinates().getMomentum().normalize();
            Vector3D N = Vector3D.crossProduct(W, T);

            Assertions.assertTrue(dif.getNorm() < 111);
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, T)) < 111);
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, N)) <  54);
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(dif, W)) <  12);

        }

    }

    @Test
    public void testParameterDerivative() {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel), frame,
                date, Constants.EIGEN5C_EARTH_MU);
        final LofOffset lofOffset = new LofOffset(frame, LOFType.LVLH_CCSDS);
        final Attitude attitude = lofOffset.getAttitude(orbit, date, frame);  // necessary for non-regression
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        final HolmesFeatherstoneAttractionModel holmesFeatherstoneModel =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                      GravityFieldFactory.getNormalizedProvider(20, 20));

        final String name = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        checkParameterDerivative(state, holmesFeatherstoneModel, name, 1.0e-5, 5.0e-12);

    }

    @Test
    public void testParameterDerivativeGradient() {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // pos-vel (from a ZOOM ephemeris reference)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        final HolmesFeatherstoneAttractionModel holmesFeatherstoneModel =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                      GravityFieldFactory.getNormalizedProvider(20, 20));

        final String name = NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT;
        checkParameterDerivativeGradient(state, holmesFeatherstoneModel, name, 1.0e-5, 5.0e-12);

    }

    @Test
    public void testTimeDependentField() {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState spacecraftState =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2005, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       GravityFieldFactory.getUnnormalizedProvider(1, 1).getMu()));

        double dP = 0.1;
        double duration = 3 * Constants.JULIAN_DAY;
        BoundedPropagator fixedFieldEphemeris   = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getConstantNormalizedProvider(8, 8, new AbsoluteDate("2005-01-01T00:00:00.000", TimeScalesFactory.getTAI())));
        BoundedPropagator varyingFieldEphemeris = createEphemeris(dP, spacecraftState, duration,
                                                                  GravityFieldFactory.getNormalizedProvider(8, 8));

        double step = 60.0;
        double maxDeltaT = 0;
        double maxDeltaN = 0;
        double maxDeltaW = 0;
        for (AbsoluteDate date = fixedFieldEphemeris.getMinDate();
             date.compareTo(fixedFieldEphemeris.getMaxDate()) < 0;
             date = date.shiftedBy(step)) {
            PVCoordinates pvFixedField   = fixedFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            PVCoordinates pvVaryingField = varyingFieldEphemeris.getPVCoordinates(date, FramesFactory.getGCRF());
            Vector3D t = pvFixedField.getVelocity().normalize();
            Vector3D w = pvFixedField.getMomentum().normalize();
            Vector3D n = Vector3D.crossProduct(w, t);
            Vector3D delta = pvVaryingField.getPosition().subtract(pvFixedField.getPosition());
            maxDeltaT = FastMath.max(maxDeltaT, FastMath.abs(Vector3D.dotProduct(delta, t)));
            maxDeltaN = FastMath.max(maxDeltaN, FastMath.abs(Vector3D.dotProduct(delta, n)));
            maxDeltaW = FastMath.max(maxDeltaW, FastMath.abs(Vector3D.dotProduct(delta, w)));
        }
        Assertions.assertTrue(maxDeltaT > 0.65);
        Assertions.assertTrue(maxDeltaT < 0.85);
        Assertions.assertTrue(maxDeltaN > 0.02);
        Assertions.assertTrue(maxDeltaN < 0.03);
        Assertions.assertTrue(maxDeltaW > 0.05);
        Assertions.assertTrue(maxDeltaW < 0.10);

    }

    private BoundedPropagator createEphemeris(double dP, SpacecraftState initialState, double duration,
                                              NormalizedSphericalHarmonicsProvider provider)
        {
        double[][] tol = NumericalPropagator.tolerances(dP, initialState.getOrbit(), OrbitType.CARTESIAN);
        AbstractIntegrator integrator =
                new DormandPrince853Integrator(0.001, 120.0, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider));
        propagator.setInitialState(initialState);
        propagator.propagate(initialState.getDate().shiftedBy(duration));
        return generator.getGeneratedEphemeris();
    }

    @Test
    public void testStateJacobian()
        {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assertions.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        propagator.addForceModel(hfModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        propagator.setInitialState(state0);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           50000, tolerances[0], 7.8e-6);
    }

    @Test
    public void testStateJacobianVs80Implementation()
        {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);

        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assertions.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        SpacecraftState state = new SpacecraftState(orbit);

        checkStateJacobianVs80Implementation(state, hfModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVs80ImplementationGradient()
        {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);

        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assertions.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        SpacecraftState state = new SpacecraftState(orbit);

        checkStateJacobianVs80ImplementationGradient(state, hfModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             2.0e-15, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferences()
        {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);

        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assertions.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        SpacecraftState state = new SpacecraftState(orbit);

        checkStateJacobianVsFiniteDifferences(state, hfModel, Utils.defaultLaw(),
                                              10.0, 2.0e-10, false);

    }

    @Test
    public void testStateJacobianVsFiniteDifferencesGradient()
        {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);

        HolmesFeatherstoneAttractionModel hfModel =
                new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(50, 50));
        Assertions.assertEquals(TideSystem.UNKNOWN, hfModel.getTideSystem());
        SpacecraftState state = new SpacecraftState(orbit);

        checkStateJacobianVsFiniteDifferencesGradient(state, hfModel, Utils.defaultLaw(),
                                              10.0, 2.0e-10, false);

    }

    @Test
    public void testIssue996() {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2000, 07, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit  = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                          0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date, mu);

        Vector3D pos = orbit.getPosition(itrf);

        double[] zeroGradient = new double[] {-0., 0., 0.};

        NormalizedSphericalHarmonicsProvider provider00 = GravityFieldFactory.getNormalizedProvider(0,  0);
        HolmesFeatherstoneAttractionModel hfModel00 = new HolmesFeatherstoneAttractionModel(itrf, provider00);

        Assertions.assertEquals(0., hfModel00.nonCentralPart(date, pos, mu));
        Assertions.assertEquals(mu / pos.getNorm(), hfModel00.value(date, pos, mu));
        Assertions.assertArrayEquals(zeroGradient, hfModel00.gradient(date, pos, mu));

        NormalizedSphericalHarmonicsProvider provider10 = GravityFieldFactory.getNormalizedProvider(1,  0);
        HolmesFeatherstoneAttractionModel hfModel10 = new HolmesFeatherstoneAttractionModel(itrf, provider10);

        Assertions.assertEquals(0., hfModel10.nonCentralPart(date, pos, mu));
        Assertions.assertEquals(mu / pos.getNorm(), hfModel10.value(date, pos, mu));
        Assertions.assertArrayEquals(zeroGradient, hfModel10.gradient(date, pos, mu));

        NormalizedSphericalHarmonicsProvider provider11 = GravityFieldFactory.getNormalizedProvider(1,  1);
        HolmesFeatherstoneAttractionModel hfModel11 = new HolmesFeatherstoneAttractionModel(itrf, provider11);

        Assertions.assertEquals(0., hfModel11.nonCentralPart(date, pos, mu));
        Assertions.assertEquals(mu / pos.getNorm(), hfModel11.value(date, pos, mu));
        Assertions.assertArrayEquals(zeroGradient, hfModel11.gradient(date, pos, mu));

    }


    @Test
    @DisplayName("Test that acceleration derivatives with respect to absolute date are not equal to zero.")
    public void testIssue1070() {
        // GIVEN
        // Define possibly shifted absolute date
        final int freeParameters = 1;
        final GradientField field = GradientField.getField(freeParameters);
        final Gradient zero = field.getZero();
        final Gradient variable = Gradient.variable(freeParameters, 0, 0.);
        final FieldAbsoluteDate<Gradient> fieldAbsoluteDate = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH).
                shiftedBy(variable);

        // Define mock state
        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Gradient> stateMock = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(stateMock.getDate()).thenReturn(fieldAbsoluteDate);
        Mockito.when(stateMock.getPosition()).thenReturn(new FieldVector3D<>(zero, zero));
        Mockito.when(stateMock.getFrame()).thenReturn(FramesFactory.getGCRF());
        Mockito.when(stateMock.getMass()).thenReturn(zero);

        // Create potential
        final int max = 2;
        final NormalizedSphericalHarmonicsProvider provider = new GleasonProvider(max, max);
        final HolmesFeatherstoneAttractionModel model = new HolmesFeatherstoneAttractionModel(itrf, provider);

        // WHEN
        final Gradient dummyGm = zero.add(1.);
        final FieldVector3D<Gradient> accelerationVector = model.acceleration(stateMock, new Gradient[] { dummyGm });

        // THEN
        final double[] derivatives = accelerationVector.getNormSq().getGradient();
        Assertions.assertNotEquals(0., MatrixUtils.createRealVector(derivatives).getNorm());
    }

    @BeforeEach
    public void setUp() {
        itrf   = null;
        propagator = null;
        Utils.setDataRoot("regular-data");
        try {
            // Eigen 6s model truncated to degree 6
            mu              =  3.986004415e+14;
            ae              =  6378136.460;
            normalizedC20   = -4.84165299820e-04;
            normalizedC30   =  9.57211326674e-07;
            normalizedC40   =  5.39990167207e-07;
            normalizedC50   =  6.86846073356e-08 ;
            normalizedC60   = -1.49953256913e-07;
            unnormalizedC20 = FastMath.sqrt( 5) * normalizedC20;
            unnormalizedC30 = FastMath.sqrt( 7) * normalizedC30;
            unnormalizedC40 = FastMath.sqrt( 9) * normalizedC40;
            unnormalizedC50 = FastMath.sqrt(11) * normalizedC50;
            unnormalizedC60 = FastMath.sqrt(13) * normalizedC60;

            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            propagator = new NumericalPropagator(integrator);
        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        itrf       = null;
        propagator = null;
    }

    private double unnormalizedC20;
    private double unnormalizedC30;
    private double unnormalizedC40;
    private double unnormalizedC50;
    private double unnormalizedC60;
    private double normalizedC20;
    private double normalizedC30;
    private double normalizedC40;
    private double normalizedC50;
    private double normalizedC60;
    private double mu;
    private double ae;

    private Frame   itrf;
    private NumericalPropagator propagator;

}


