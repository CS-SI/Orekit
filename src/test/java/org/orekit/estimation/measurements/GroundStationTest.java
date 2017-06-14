/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class GroundStationTest {

    @Test
    public void testEstimateStationPosition() throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // move one station
        final RandomGenerator random = new Well19937a(0x4adbecfc743bda60l);
        final TopocentricFrame base = context.stations.get(0).getBaseFrame();
        final BodyShape parent = base.getParentShape();
        final Vector3D baseOrigin = parent.transform(base.getPoint());
        final Vector3D deltaTopo = new Vector3D(2 * random.nextDouble() - 1,
                                                2 * random.nextDouble() - 1,
                                                2 * random.nextDouble() - 1);
        final Transform topoToParent = base.getTransformTo(parent.getBodyFrame(), (AbsoluteDate) null);
        final Vector3D deltaParent   = topoToParent.transformVector(deltaTopo);
        final String movedSuffix     = "-moved";
        final GroundStation moved = new GroundStation(new TopocentricFrame(parent,
                                                                           parent.transform(baseOrigin.subtract(deltaParent),
                                                                                            parent.getBodyFrame(),
                                                                                            null),
                                                                           base.getName() + movedSuffix));

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final ObservedMeasurement<?> measurement : measurements) {
            final Range range = (Range) measurement;
            final String name = range.getStation().getBaseFrame().getName() + movedSuffix;
                if (moved.getBaseFrame().getName().equals(name)) {
                    estimator.addMeasurement(new Range(moved, range.getDate(),
                                                       range.getObservedValue()[0],
                                                       range.getTheoreticalStandardDeviation()[0],
                                                       range.getBaseWeight()[0]));
                } else {
                    estimator.addMeasurement(range);
                }
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(100);
        estimator.setMaxEvaluations(200);

        // we want to estimate station offsets
        moved.getEastOffsetDriver().setSelected(true);
        moved.getNorthOffsetDriver().setSelected(true);
        moved.getZenithOffsetDriver().setSelected(true);

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 7.6e-7,
                                     0.0, 1.9e-6,
                                     0.0, 1.4e-6,
                                     0.0, 6.2e-10);
        Assert.assertEquals(deltaTopo.getX(), moved.getEastOffsetDriver().getValue(),   4.5e-7);
        Assert.assertEquals(deltaTopo.getY(), moved.getNorthOffsetDriver().getValue(),  6.2e-7);
        Assert.assertEquals(deltaTopo.getZ(), moved.getZenithOffsetDriver().getValue(), 2.6e-7);

    }

    @Test
    public void testOffsetDerivativesOctantPxPyPz() throws OrekitException {
        double toleranceTranslationValue         = 2.0e-8;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 6.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 3.0e-14;
        double toleranceRotationRateValue        = 2.0e-19;
        double toleranceRotationRateDerivative   = 2.0e-19;
        doTestOffsetDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxPyMz() throws OrekitException {
        double toleranceTranslationValue         = 8.0e-9;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 6.0e-13;
        double toleranceRotationValue            = 7.0e-16;
        double toleranceRotationDerivative       = 2.0e-7;
        double toleranceRotationRateValue        = 9.0e-20;
        double toleranceRotationRateDerivative   = 4.0e-20;
        doTestOffsetDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxMyPz() throws OrekitException {
        double toleranceTranslationValue         = 2.0e-8;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 6.0e-13;
        double toleranceRotationValue            = 5.0e-16;
        double toleranceRotationDerivative       = 3.0e-14;
        double toleranceRotationRateValue        = 2.0e-19;
        double toleranceRotationRateDerivative   = 9.0e-20;
        doTestOffsetDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxMyMz() throws OrekitException {
        double toleranceTranslationValue         = 6.0e-9;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 7.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 2.0e-7;
        double toleranceRotationRateValue        = 7.0e-20;
        double toleranceRotationRateDerivative   = 4.0e-20;
        doTestOffsetDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxPyPz() throws OrekitException {
        double toleranceTranslationValue         = 2.0e-8;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 5.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 3.0e-14;
        double toleranceRotationRateValue        = 2.0e-19;
        double toleranceRotationRateDerivative   = 1.0e-19;
        doTestOffsetDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxPyMz() throws OrekitException {
        double toleranceTranslationValue         = 2.0e-8;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 6.0e-13;
        double toleranceVelocityDerivative       = 5.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 3.0e-14;
        double toleranceRotationRateValue        = 2.0e-19;
        double toleranceRotationRateDerivative   = 1.0e-19;
        doTestOffsetDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxMyPz() throws OrekitException {
        double toleranceTranslationValue         = 8.0e-9;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 7.0e-13;
        double toleranceVelocityDerivative       = 7.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 2.0e-7;
        double toleranceRotationRateValue        = 9.0e-20;
        double toleranceRotationRateDerivative   = 8.0e-20;
        doTestOffsetDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxMyMz() throws OrekitException {
        double toleranceTranslationValue         = 8.0e-9;
        double toleranceTranslationDerivative    = 4.0e-7;
        double toleranceVelocityValue            = 7.0e-13;
        double toleranceVelocityDerivative       = 7.0e-13;
        double toleranceRotationValue            = 6.0e-16;
        double toleranceRotationDerivative       = 2.0e-7;
        double toleranceRotationRateValue        = 9.0e-20;
        double toleranceRotationRateDerivative   = 8.0e-20;
        doTestOffsetDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    @Test
    public void testOffsetDerivativesNearPole() throws OrekitException {
        double toleranceTranslationValue         = 2.0e-8;
        double toleranceTranslationDerivative    = 9.0e-6;
        double toleranceVelocityValue            = 2.0e-17;
        double toleranceVelocityDerivative       = 2.0e-17;
        double toleranceRotationValue            = 7.0e-16;
        double toleranceRotationDerivative       = 0.23; // near pole, the East and North directions are singular
        double toleranceRotationRateValue        = 2.0e-19;
        double toleranceRotationRateDerivative   = 2.0e-19;
        doTestOffsetDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0,
                                toleranceTranslationValue,  toleranceTranslationDerivative,
                                toleranceVelocityValue,     toleranceVelocityDerivative,
                                toleranceRotationValue,     toleranceRotationDerivative,
                                toleranceRotationRateValue, toleranceRotationRateDerivative);
    }

    private void doTestOffsetDerivatives(double latitude, double longitude, double altitude,
                                         double toleranceTranslationValue,  double toleranceTranslationDerivative,
                                         double toleranceVelocityValue,     double toleranceVelocityDerivative,
                                         double toleranceRotationValue,     double toleranceRotationDerivative,
                                         double toleranceRotationRateValue, double toleranceRotationRateDerivative)
        throws OrekitException {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        final DSFactory factory = new DSFactory(3, 1);
        final FieldAbsoluteDate<DerivativeStructure> dateDS = new FieldAbsoluteDate<>(factory.getDerivativeField(), date);
        final int eastIndex   = 0;
        final int northIndex  = 1;
        final int zenithIndex = 2;
        UnivariateDifferentiableVectorFunction[] dF =
                        new UnivariateDifferentiableVectorFunction[factory.getCompiler().getFreeParameters()];
        for (final int k : new int[] { eastIndex, northIndex, zenithIndex }) {
            final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0);
            dF[k] = differentiator.differentiate(new UnivariateVectorFunction() {
                private double previous0 = Double.NaN;
                private double previous1 = Double.NaN;
                private double previous2 = Double.NaN;
                private double previous3 = Double.NaN;
                @Override
                public double[] value(double x) {
                    final double[] result = new double[13];
                    try {
                        final double previouspI;
                        switch (k) {
                            case 0 :
                                previouspI = station.getEastOffsetDriver().getValue();
                                station.getEastOffsetDriver().setValue(previouspI + x);
                                break;
                            case 1 :
                                previouspI = station.getNorthOffsetDriver().getValue();
                                station.getNorthOffsetDriver().setValue(previouspI + x);
                                break;
                            default :
                                previouspI = station.getZenithOffsetDriver().getValue();
                                station.getZenithOffsetDriver().setValue(previouspI + x);
                        }
                        Transform t = station.getOffsetFrame().getTransformTo(eme2000, date);
                        result[ 0] = t.getTranslation().getX();
                        result[ 1] = t.getTranslation().getY();
                        result[ 2] = t.getTranslation().getZ();
                        result[ 3] = t.getVelocity().getX();
                        result[ 4] = t.getVelocity().getY();
                        result[ 5] = t.getVelocity().getZ();
                        double sign = +1;
                        if (Double.isNaN(previous0)) {
                            sign = FastMath.copySign(1.0,
                                                     previous0 * t.getRotation().getQ0() +
                                                     previous1 * t.getRotation().getQ1() +
                                                     previous2 * t.getRotation().getQ2() +
                                                     previous3 * t.getRotation().getQ3());
                        }
                        previous0  = sign * t.getRotation().getQ0();
                        previous1  = sign * t.getRotation().getQ1();
                        previous2  = sign * t.getRotation().getQ2();
                        previous3  = sign * t.getRotation().getQ3();
                        result[ 6] = previous0;
                        result[ 7] = previous1;
                        result[ 8] = previous2;
                        result[ 9] = previous3;
                        result[10] = t.getRotationRate().getX();
                        result[11] = t.getRotationRate().getY();
                        result[12] = t.getRotationRate().getZ();
                        switch (k) {
                            case 0 :
                                station.getEastOffsetDriver().setValue(previouspI);
                                break;
                            case 1 :
                                station.getNorthOffsetDriver().setValue(previouspI);
                                break;
                            default :
                                station.getZenithOffsetDriver().setValue(previouspI);
                        }
                    } catch (OrekitException oe) {
                        Assert.fail(oe.getLocalizedMessage());
                    }
                    return result;
                }
            });
        }

        double maxTranslationValueError       = 0;
        double maxTranslationDerivativeError  = 0;
        double maxVelocityValueError          = 0;
        double maxVelocityDerivativeError     = 0;
        double maxRotationValueError          = 0;
        double maxRotationDerivativeError     = 0;
        double maxRotationRateValueError      = 0;
        double maxRotationRateDerivativeError = 0;
        for (double dEast = -2; dEast <= 2; dEast += 0.5) {
            for (double dNorth = -2; dNorth <= 2; dNorth += 0.5) {
                for (double dZenith = -2; dZenith <= 2; dZenith += 0.5) {
                    station.getEastOffsetDriver().setValue(dEast);
                    station.getNorthOffsetDriver().setValue(dNorth);
                    station.getZenithOffsetDriver().setValue(dZenith);
                    FieldTransform<DerivativeStructure> t = station.getOffsetToInertial(eme2000, dateDS, factory, eastIndex, northIndex, zenithIndex);
                    for (final int k : new int[] { eastIndex, northIndex, zenithIndex }) {
                        DerivativeStructure[] result = dF[k].value(new DSFactory(1, 1).variable(0, 0.0));
                        int[] orders = new int[3];
                        orders[k] = 1;

                        // translation
                        maxTranslationValueError      = FastMath.max(maxTranslationValueError, FastMath.abs(result[0].getValue() - t.getTranslation().getX().getValue()));
                        maxTranslationValueError      = FastMath.max(maxTranslationValueError, FastMath.abs(result[1].getValue() - t.getTranslation().getY().getValue()));
                        maxTranslationValueError      = FastMath.max(maxTranslationValueError, FastMath.abs(result[2].getValue() - t.getTranslation().getZ().getValue()));
                        maxTranslationDerivativeError = FastMath.max(maxTranslationDerivativeError, FastMath.abs(result[0].getPartialDerivative(1) - t.getTranslation().getX().getPartialDerivative(orders)));
                        maxTranslationDerivativeError = FastMath.max(maxTranslationDerivativeError, FastMath.abs(result[1].getPartialDerivative(1) - t.getTranslation().getY().getPartialDerivative(orders)));
                        maxTranslationDerivativeError = FastMath.max(maxTranslationDerivativeError, FastMath.abs(result[2].getPartialDerivative(1) - t.getTranslation().getZ().getPartialDerivative(orders)));

                        // velocity
                        maxVelocityValueError      = FastMath.max(maxVelocityValueError, FastMath.abs(result[3].getValue() - t.getVelocity().getX().getValue()));
                        maxVelocityValueError      = FastMath.max(maxVelocityValueError, FastMath.abs(result[4].getValue() - t.getVelocity().getY().getValue()));
                        maxVelocityValueError      = FastMath.max(maxVelocityValueError, FastMath.abs(result[5].getValue() - t.getVelocity().getZ().getValue()));
                        maxVelocityDerivativeError = FastMath.max(maxVelocityDerivativeError, FastMath.abs(result[3].getPartialDerivative(1) - t.getVelocity().getX().getPartialDerivative(orders)));
                        maxVelocityDerivativeError = FastMath.max(maxVelocityDerivativeError, FastMath.abs(result[4].getPartialDerivative(1) - t.getVelocity().getY().getPartialDerivative(orders)));
                        maxVelocityDerivativeError = FastMath.max(maxVelocityDerivativeError, FastMath.abs(result[5].getPartialDerivative(1) - t.getVelocity().getZ().getPartialDerivative(orders)));

                        // rotation
                        double sign = FastMath.copySign(1.0,
                                                        result[6].getValue() * t.getRotation().getQ0().getValue() +
                                                        result[7].getValue() * t.getRotation().getQ1().getValue() +
                                                        result[8].getValue() * t.getRotation().getQ2().getValue() +
                                                        result[9].getValue() * t.getRotation().getQ3().getValue());
                        maxRotationValueError = FastMath.max(maxRotationValueError, FastMath.abs(sign * result[6].getValue() - t.getRotation().getQ0().getValue()));
                        maxRotationValueError = FastMath.max(maxRotationValueError, FastMath.abs(sign * result[7].getValue() - t.getRotation().getQ1().getValue()));
                        maxRotationValueError = FastMath.max(maxRotationValueError, FastMath.abs(sign * result[8].getValue() - t.getRotation().getQ2().getValue()));
                        maxRotationValueError = FastMath.max(maxRotationValueError, FastMath.abs(sign * result[9].getValue() - t.getRotation().getQ3().getValue()));
                        maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError, FastMath.abs(result[6].getPartialDerivative(1) - t.getRotation().getQ0().getPartialDerivative(orders)));
                        maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError, FastMath.abs(result[7].getPartialDerivative(1) - t.getRotation().getQ1().getPartialDerivative(orders)));
                        maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError, FastMath.abs(result[8].getPartialDerivative(1) - t.getRotation().getQ2().getPartialDerivative(orders)));
                        maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError, FastMath.abs(result[9].getPartialDerivative(1) - t.getRotation().getQ3().getPartialDerivative(orders)));

                        // rotation rate
                        maxRotationRateValueError = FastMath.max(maxRotationRateValueError, FastMath.abs(result[10].getValue() - t.getRotationRate().getX().getValue()));
                        maxRotationRateValueError = FastMath.max(maxRotationRateValueError, FastMath.abs(result[11].getValue() - t.getRotationRate().getY().getValue()));
                        maxRotationRateValueError = FastMath.max(maxRotationRateValueError, FastMath.abs(result[12].getValue() - t.getRotationRate().getZ().getValue()));
                        maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, FastMath.abs(result[10].getPartialDerivative(1) - t.getRotationRate().getX().getPartialDerivative(orders)));
                        maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, FastMath.abs(result[11].getPartialDerivative(1) - t.getRotationRate().getY().getPartialDerivative(orders)));
                        maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, FastMath.abs(result[12].getPartialDerivative(1) - t.getRotationRate().getZ().getPartialDerivative(orders)));

                    }
                }
            }
        }

        Assert.assertEquals(0.0, maxTranslationValueError,        toleranceTranslationValue);
        Assert.assertEquals(0.0, maxTranslationDerivativeError,   toleranceTranslationDerivative);
        Assert.assertEquals(0.0, maxVelocityValueError,           toleranceVelocityValue);
        Assert.assertEquals(0.0, maxVelocityDerivativeError,      toleranceVelocityDerivative);
        Assert.assertEquals(0.0, maxRotationValueError,           toleranceRotationValue);
        Assert.assertEquals(0.0, maxRotationDerivativeError,      toleranceRotationDerivative);
        Assert.assertEquals(0.0, maxRotationRateValueError,       toleranceRotationRateValue);
        Assert.assertEquals(0.0, maxRotationRateDerivativeError,  toleranceRotationRateDerivative);

    }

    @Test
    public void testNonEllipsoid() throws OrekitException {
        Utils.setDataRoot("regular-data");
        final OneAxisEllipsoid ellipsoid =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        BodyShape nonEllipsoid = new BodyShape() {
            private static final long serialVersionUID = 1L;
            public Vector3D transform(GeodeticPoint point) {
                return ellipsoid.transform(point);
            }
            public GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date)
                throws OrekitException {
                return ellipsoid.transform(point, frame, date);
            }
            public <T extends RealFieldElement<T>> FieldGeodeticPoint<T> transform(FieldVector3D<T> point,
                                                                                   Frame frame,
                                                                                   FieldAbsoluteDate<T> date)
                throws OrekitException {
                return ellipsoid.transform(point, frame, date);
            }
            public TimeStampedPVCoordinates projectToGround(TimeStampedPVCoordinates pv, Frame frame)
                    throws OrekitException {
                return ellipsoid.projectToGround(pv, frame);
            }
            public Vector3D projectToGround(Vector3D point, AbsoluteDate date, Frame frame)
                    throws OrekitException {
                return ellipsoid.projectToGround(point, date, frame);
            }
            public GeodeticPoint getIntersectionPoint(Line line, Vector3D close,
                                                      Frame frame, AbsoluteDate date)
                                                              throws OrekitException {
                return ellipsoid.getIntersectionPoint(line, close, frame, date);
            }
            public Frame getBodyFrame() {
                return ellipsoid.getBodyFrame();
            }
        };
        final GroundStation g = new GroundStation(new TopocentricFrame(nonEllipsoid,
                                                                       new GeodeticPoint(0, 0, 0),
                                                                       "dummy"));
        final DSFactory factory = new DSFactory(3, 0); 
        try {
            g.getOffsetToInertial(FramesFactory.getEME2000(),
                                  FieldAbsoluteDate.getJ2000Epoch(factory.getDerivativeField()),
                                  factory, 0, 1, 2);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.BODY_SHAPE_IS_NOT_AN_ELLIPSOID, oe.getSpecifier());
        }
    }

}


