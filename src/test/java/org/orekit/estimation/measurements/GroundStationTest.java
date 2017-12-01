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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
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
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

public class GroundStationTest {

    @Deprecated
    @Test
    public void testDeprecatedMethod() throws OrekitException {
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        for (final GroundStation station : context.stations) {
            Assert.assertThat(station.getOffsetGeodeticPoint(),
                              OrekitMatchers.geodeticPointCloseTo(station.getOffsetGeodeticPoint(null), 1.0e-15));
        }
    }

    @Test
    public void testEstimateStationPosition() throws OrekitException, IOException, ClassNotFoundException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

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
                                                                           base.getName() + movedSuffix),
                                                      context.ut1.getEOPHistory(),
                                                      context.stations.get(0).getDisplacements());

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
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
                                     0.0, 5.6e-7,
                                     0.0, 1.4e-6,
                                     0.0, 4.8e-7,
                                     0.0, 2.6e-10);
        Assert.assertEquals(deltaTopo.getX(), moved.getEastOffsetDriver().getValue(),   4.5e-7);
        Assert.assertEquals(deltaTopo.getY(), moved.getNorthOffsetDriver().getValue(),  6.2e-7);
        Assert.assertEquals(deltaTopo.getZ(), moved.getZenithOffsetDriver().getValue(), 2.6e-7);

        GeodeticPoint result = moved.getOffsetGeodeticPoint(null);

        GeodeticPoint reference = context.stations.get(0).getBaseFrame().getPoint();
        Assert.assertEquals(reference.getLatitude(),  result.getLatitude(),  1.4e-14);
        Assert.assertEquals(reference.getLongitude(), result.getLongitude(), 2.9e-14);
        Assert.assertEquals(reference.getAltitude(),  result.getAltitude(),  2.6e-7);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assert.assertEquals(9,       normalizedCovariances.getRowDimension());
        Assert.assertEquals(9,       normalizedCovariances.getColumnDimension());
        Assert.assertEquals(9,       physicalCovariances.getRowDimension());
        Assert.assertEquals(9,       physicalCovariances.getColumnDimension());
        Assert.assertEquals(0.55431, physicalCovariances.getEntry(6, 6), 1.0e-5);
        Assert.assertEquals(0.22694, physicalCovariances.getEntry(7, 7), 1.0e-5);
        Assert.assertEquals(0.13106, physicalCovariances.getEntry(8, 8), 1.0e-5);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(moved.getEstimatedEarthFrame().getTransformProvider());

        Assert.assertTrue(bos.size() > 145000);
        Assert.assertTrue(bos.size() < 150000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EstimatedEarthFrameProvider deserialized  = (EstimatedEarthFrameProvider) ois.readObject();
        Assert.assertEquals(moved.getPrimeMeridianOffsetDriver().getValue(),
                            deserialized.getPrimeMeridianOffsetDriver().getValue(),
                            1.0e-15);
        Assert.assertEquals(moved.getPrimeMeridianDriftDriver().getValue(),
                            deserialized.getPrimeMeridianDriftDriver().getValue(),
                            1.0e-15);
        Assert.assertEquals(moved.getPolarOffsetXDriver().getValue(),
                            deserialized.getPolarOffsetXDriver().getValue(),
                            1.0e-15);
        Assert.assertEquals(moved.getPolarDriftXDriver().getValue(),
                            deserialized.getPolarDriftXDriver().getValue(),
                            1.0e-15);
        Assert.assertEquals(moved.getPolarOffsetYDriver().getValue(),
                            deserialized.getPolarOffsetYDriver().getValue(),
                            1.0e-15);
        Assert.assertEquals(moved.getPolarDriftYDriver().getValue(),
                            deserialized.getPolarDriftYDriver().getValue(),
                            1.0e-15);

    }

    @Test
    public void testEstimateEOP() throws OrekitException {

        Context linearEOPContext = EstimationTestUtils.eccentricContext("linear-EOP:regular-data/de431-ephemerides:potential:tides");

        final AbsoluteDate refDate = new AbsoluteDate(2000, 2, 24, linearEOPContext.utc);
        final double dut10 = 0.3079738;
        final double lod   = 0.0011000;
        final double xp0   = 68450.0e-6;
        final double xpDot =   -50.0e-6;
        final double yp0   =    60.0e-6;
        final double ypDot =     2.0e-6;
        for (double dt = -2 * Constants.JULIAN_DAY; dt < 2 * Constants.JULIAN_DAY; dt += 300.0) {
            AbsoluteDate date = refDate.shiftedBy(dt);
            Assert.assertEquals(dut10 - dt * lod / Constants.JULIAN_DAY,
                                linearEOPContext.ut1.getEOPHistory().getUT1MinusUTC(date),
                                1.0e-15);
            Assert.assertEquals(lod,
                                linearEOPContext.ut1.getEOPHistory().getLOD(date),
                                1.0e-15);
            Assert.assertEquals((xp0 + xpDot * dt / Constants.JULIAN_DAY) * Constants.ARC_SECONDS_TO_RADIANS,
                                linearEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getXp(),
                                1.0e-15);
            Assert.assertEquals((yp0 + ypDot * dt / Constants.JULIAN_DAY) * Constants.ARC_SECONDS_TO_RADIANS,
                                linearEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getYp(),
                                1.0e-15);
        }
        final NumericalPropagatorBuilder linearPropagatorBuilder =
                        linearEOPContext.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(linearEOPContext.initialOrbit,
                                                                           linearPropagatorBuilder);
        final List<ObservedMeasurement<?>> linearMeasurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(linearEOPContext),
                                                               1.0, 5.0, 60.0);

        Utils.clearFactories();
        Context zeroEOPContext = EstimationTestUtils.eccentricContext("zero-EOP:regular-data/de431-ephemerides:potential:potential:tides");
        for (double dt = -2 * Constants.JULIAN_DAY; dt < 2 * Constants.JULIAN_DAY; dt += 300.0) {
            AbsoluteDate date = refDate.shiftedBy(dt);
            Assert.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getUT1MinusUTC(date),
                                1.0e-15);
            Assert.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getLOD(date),
                                1.0e-15);
            Assert.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getXp(),
                                1.0e-15);
            Assert.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getYp(),
                                1.0e-15);
        }

        // create orbit estimator
        final NumericalPropagatorBuilder zeroPropagatorBuilder =
                        linearEOPContext.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                zeroPropagatorBuilder);
        for (final ObservedMeasurement<?> linearMeasurement : linearMeasurements) {
            Range linearRange = (Range) linearMeasurement;
            for (final GroundStation station : zeroEOPContext.stations) {
                if (station.getBaseFrame().getName().equals(linearRange.getStation().getBaseFrame().getName())) {
                    Range zeroRange = new Range(station,
                                                linearRange.getDate(),
                                                linearRange.getObservedValue()[0],
                                                linearRange.getTheoreticalStandardDeviation()[0],
                                                linearRange.getBaseWeight()[0]);
                    estimator.addMeasurement(zeroRange);
                }
            }
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(100);
        estimator.setMaxEvaluations(200);

        // we want to estimate pole and prime meridian
        GroundStation station = zeroEOPContext.stations.get(0);
        station.getPrimeMeridianOffsetDriver().setReferenceDate(refDate);
        station.getPrimeMeridianOffsetDriver().setSelected(true);
        station.getPrimeMeridianDriftDriver().setSelected(true);
        station.getPolarOffsetXDriver().setReferenceDate(refDate);
        station.getPolarOffsetXDriver().setSelected(true);
        station.getPolarDriftXDriver().setSelected(true);
        station.getPolarOffsetYDriver().setReferenceDate(refDate);
        station.getPolarOffsetYDriver().setSelected(true);
        station.getPolarDriftYDriver().setSelected(true);

        // just for the fun and to speed up test, we will use orbit determination, *without* estimating orbit
        for (final ParameterDriver driver : zeroPropagatorBuilder.getOrbitalParametersDrivers().getDrivers()) {
            driver.setSelected(false);
        }

        estimator.estimate();

        final double computedDut1  = station.getPrimeMeridianOffsetDriver().getValue() / EstimatedEarthFrameProvider.EARTH_ANGULAR_VELOCITY;
        final double computedLOD   = station.getPrimeMeridianDriftDriver().getValue() * (-Constants.JULIAN_DAY / EstimatedEarthFrameProvider.EARTH_ANGULAR_VELOCITY);
        final double computedXp    = station.getPolarOffsetXDriver().getValue() / Constants.ARC_SECONDS_TO_RADIANS;
        final double computedXpDot = station.getPolarDriftXDriver().getValue()  / Constants.ARC_SECONDS_TO_RADIANS * Constants.JULIAN_DAY;
        final double computedYp    = station.getPolarOffsetYDriver().getValue() / Constants.ARC_SECONDS_TO_RADIANS;
        final double computedYpDot = station.getPolarDriftYDriver().getValue()  / Constants.ARC_SECONDS_TO_RADIANS * Constants.JULIAN_DAY;
        Assert.assertEquals(dut10, computedDut1,  4.3e-10);
        Assert.assertEquals(lod,   computedLOD,   4.9e-10);
        Assert.assertEquals(xp0,   computedXp,    5.6e-9);
        Assert.assertEquals(xpDot, computedXpDot, 7.2e-9);
        Assert.assertEquals(yp0,   computedYp,    1.1e-9);
        Assert.assertEquals(ypDot, computedYpDot, 2.8e-11);

        // thresholds to use if orbit is estimated
        // (i.e. when commenting out the loop above that sets orbital parameters drivers to "not selected")
//         Assert.assertEquals(dut10, computedDut1,  6.6e-3);
//         Assert.assertEquals(lod,   computedLOD,   1.1e-9);
//         Assert.assertEquals(xp0,   computedXp,    3.3e-8);
//         Assert.assertEquals(xpDot, computedXpDot, 2.2e-8);
//         Assert.assertEquals(yp0,   computedYp,    3.3e-8);
//         Assert.assertEquals(ypDot, computedYpDot, 3.8e-8);

    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.9e-15;
        double relativeTolerancePositionDerivative =  1.9e-10;
        double relativeToleranceVelocityValue      =  5.5e-15;
        double relativeToleranceVelocityDerivative =  3.1e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.1e-15;
        double toleranceRotationDerivative       =  4.2e-18;
        double toleranceRotationRateValue        =  2.7e-19;
        double toleranceRotationRateDerivative   =  1.6e-21;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  8.1e-11;
        double relativeToleranceVelocityValue      =  4.7e-15;
        double relativeToleranceVelocityDerivative =  2.4e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  3.7e-18;
        double toleranceRotationRateValue        =  1.6e-19;
        double toleranceRotationRateDerivative   =  5.0e-22;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.7e-15;
        double relativeTolerancePositionDerivative =  1.7e-10;
        double relativeToleranceVelocityValue      =  4.9e-15;
        double relativeToleranceVelocityDerivative =  3.0e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxMyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  3.6e-18;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  1.2e-21;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  7.5e-11;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  4.1e-18;
        double toleranceRotationRateValue        =  1.4e-19;
        double toleranceRotationRateDerivative   =  4.9e-22;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  1.4e-10;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  2.3e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  4.0e-18;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  1.3e-21;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  1.4e-10;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  2.3e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  4.0e-18;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  1.3e-21;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  1.5e-10;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxMyPz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  4.7e-18;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  1.1e-21;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  1.5e-10;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  4.7e-18;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  1.1e-21;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesNearPole() throws OrekitException {
        double relativeTolerancePositionValue      =  5.2e-15;
        double relativeTolerancePositionDerivative =  8.0e-10;
        double relativeToleranceVelocityValue      =  2.7e-13;
        double relativeToleranceVelocityDerivative =  1.1e-08;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesNearPole() throws OrekitException {
        double toleranceRotationValue            =  4.5e-15;
        double toleranceRotationDerivative       =  8.1e-02; // near pole, the East and North directions are singular
        double toleranceRotationRateValue        =  3.2e-19;
        double toleranceRotationRateDerivative   =  2.2e-21;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.9e-15;
        double relativeTolerancePositionDerivative =  1.9e-08;
        double relativeToleranceVelocityValue      =  5.5e-15;
        double relativeToleranceVelocityDerivative =  2.4e-08;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.1e-15;
        double toleranceRotationDerivative       =  7.7e-09;
        double toleranceRotationRateValue        =  2.7e-19;
        double toleranceRotationRateDerivative   =  2.9e-12;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  1.2e-08;
        double relativeToleranceVelocityValue      =  4.7e-15;
        double relativeToleranceVelocityDerivative =  1.7e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  8.4e-09;
        double toleranceRotationRateValue        =  1.6e-19;
        double toleranceRotationRateDerivative   =  1.1e-12;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.7e-15;
        double relativeTolerancePositionDerivative =  1.5e-08;
        double relativeToleranceVelocityValue      =  4.9e-15;
        double relativeToleranceVelocityDerivative =  3.0e-08;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxMyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  9.2e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  2.8e-12;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  1.1e-08;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  1.8e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  8.3e-09;
        double toleranceRotationRateValue        =  1.4e-19;
        double toleranceRotationRateDerivative   =  1.1e-12;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  2.1e-08;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  1.7e-08;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.6e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  2.9e-12;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  2.1e-08;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  1.7e-08;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.6e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  2.9e-12;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  1.5e-08;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.6e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxMyPz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  9.4e-09;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  2.2e-12;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  1.5e-08;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.6e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  9.4e-09;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  2.2e-12;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesNearPole() throws OrekitException {
        double relativeTolerancePositionValue      =  4.2e-15;
        double relativeTolerancePositionDerivative =  1.7e-08;
        double relativeToleranceVelocityValue      =  2.7e-13;
        double relativeToleranceVelocityDerivative =  1.2e-09;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesNearPole() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  7.4e-09;
        double toleranceRotationRateValue        =  3.2e-19;
        double toleranceRotationRateDerivative   =  3.7e-12;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.9e-15;
        double relativeTolerancePositionDerivative =  1.3e-08;
        double relativeToleranceVelocityValue      =  5.5e-15;
        double relativeToleranceVelocityDerivative =  1.7e-08;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.1e-15;
        double toleranceRotationDerivative       =  8.4e-09;
        double toleranceRotationRateValue        =  2.7e-19;
        double toleranceRotationRateDerivative   =  2.7e-12;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  7.6e-09;
        double relativeToleranceVelocityValue      =  4.7e-15;
        double relativeToleranceVelocityDerivative =  1.1e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.8e-09;
        double toleranceRotationRateValue        =  1.6e-19;
        double toleranceRotationRateDerivative   =  1.3e-12;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.7e-15;
        double relativeTolerancePositionDerivative =  1.2e-08;
        double relativeToleranceVelocityValue      =  4.9e-15;
        double relativeToleranceVelocityDerivative =  1.5e-08;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxMyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.5e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  3.2e-12;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.2e-15;
        double relativeTolerancePositionDerivative =  6.5e-09;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  1.2e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  8.0e-09;
        double toleranceRotationRateValue        =  1.4e-19;
        double toleranceRotationRateDerivative   =  1.1e-12;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxPyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  1.4e-08;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  9.1e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxPyPz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.3e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  2.6e-12;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxPyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  3.2e-15;
        double relativeTolerancePositionDerivative =  1.4e-08;
        double relativeToleranceVelocityValue      =  3.9e-15;
        double relativeToleranceVelocityDerivative =  9.1e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxPyMz() throws OrekitException {
        double toleranceRotationValue            =  2.0e-15;
        double toleranceRotationDerivative       =  7.3e-09;
        double toleranceRotationRateValue        =  2.6e-19;
        double toleranceRotationRateDerivative   =  2.6e-12;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxMyPz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  9.1e-09;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.1e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxMyPz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  7.7e-09;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  2.0e-12;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxMyMz() throws OrekitException {
        double relativeTolerancePositionValue      =  2.7e-15;
        double relativeTolerancePositionDerivative =  9.1e-09;
        double relativeToleranceVelocityValue      =  3.8e-15;
        double relativeToleranceVelocityDerivative =  1.1e-08;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxMyMz() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  7.7e-09;
        double toleranceRotationRateValue        =  1.7e-19;
        double toleranceRotationRateDerivative   =  2.0e-12;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesNearPole() throws OrekitException {
        double relativeTolerancePositionValue      =  4.2e-15;
        double relativeTolerancePositionDerivative =  1.8e-03; // near pole, the East and North directions are singular
        double relativeToleranceVelocityValue      =  2.7e-13;
        double relativeToleranceVelocityDerivative =  7.0e-08;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesNearPole() throws OrekitException {
        double toleranceRotationValue            =  1.9e-15;
        double toleranceRotationDerivative       =  8.1e-09;
        double toleranceRotationRateValue        =  3.2e-19;
        double toleranceRotationRateDerivative   =  3.6e-12;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testNoReferenceDate() throws OrekitException {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(0.1, 0.2, 100),
                                                                             "dummy"));
        try {
            station.getOffsetToInertial(eme2000, date);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assert.assertEquals("prime-meridian-offset", (String) oe.getParts()[0]);
        }
        
        try {
            DSFactory factory = new DSFactory(9,  1);
            Map<String, Integer> indices = new HashMap<>();
            for (final ParameterDriver driver : Arrays.asList(station.getPrimeMeridianOffsetDriver(),
                                                              station.getPrimeMeridianDriftDriver(),
                                                              station.getPolarOffsetXDriver(),
                                                              station.getPolarDriftXDriver(),
                                                              station.getPolarOffsetYDriver(),
                                                              station.getPolarDriftYDriver(),
                                                              station.getEastOffsetDriver(),
                                                              station.getNorthOffsetDriver(),
                                                              station.getZenithOffsetDriver())) {
                indices.put(driver.getName(), indices.size());
            }
            station.getOffsetToInertial(eme2000, new FieldAbsoluteDate<>(factory.getDerivativeField(), date), factory,
                                        indices);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assert.assertEquals("prime-meridian-offset", (String) oe.getParts()[0]);
        }
        
    }

    private void doTestCartesianDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                            double relativeTolerancePositionValue, double relativeTolerancePositionDerivative,
                                            double relativeToleranceVelocityValue, double relativeToleranceVelocityDerivative,
                                            String... parameterPattern)
        throws OrekitException {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date0 = date.shiftedBy(50000);
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        final DSFactory factory = new DSFactory(parameterPattern.length, 1);
        final FieldAbsoluteDate<DerivativeStructure> dateDS =
                        new FieldAbsoluteDate<>(factory.getDerivativeField(), date);
        ParameterDriver[] selectedDrivers = new ParameterDriver[parameterPattern.length];
        UnivariateDifferentiableVectorFunction[] dFCartesian = new UnivariateDifferentiableVectorFunction[parameterPattern.length];
        final ParameterDriver[] allDrivers = selectAllDrivers(station);
        for (ParameterDriver driver : allDrivers) {
            driver.setReferenceDate(date0);
        }
        Map<String, Integer> indices = new HashMap<>();
        for (int k = 0; k < dFCartesian.length; ++k) {
            for (int i = 0; i < allDrivers.length; ++i) {
                if (allDrivers[i].getName().matches(parameterPattern[k])) {
                    selectedDrivers[k] = allDrivers[i];
                    dFCartesian[k] = differentiatedStationPV(station, eme2000, date, selectedDrivers[k], stepFactor);
                    indices.put(selectedDrivers[k].getName(), k);
                }
            }
        };
        DSFactory factory11 = new DSFactory(1, 1);

        RandomGenerator generator = new Well19937a(0x084d58a19c498a54l);

        double maxPositionValueRelativeError      = 0;
        double maxPositionDerivativeRelativeError = 0;
        double maxVelocityValueRelativeError      = 0;
        double maxVelocityDerivativeRelativeError = 0;
        for (int i = 0; i < 1000; ++i) {

            // randomly change one parameter
            ParameterDriver changed = allDrivers[generator.nextInt(allDrivers.length)];
            changed.setNormalizedValue(2 * generator.nextDouble() - 1);

            // transform to check
            FieldTransform<DerivativeStructure> t = station.getOffsetToInertial(eme2000, dateDS, factory, indices);
            FieldPVCoordinates<DerivativeStructure> pv = t.transformPVCoordinates(FieldPVCoordinates.getZero(factory.getDerivativeField()));
            for (int k = 0; k < dFCartesian.length; ++k) {

                // reference values and derivatives computed using finite differences
                DerivativeStructure[] refCartesian = dFCartesian[k].value(factory11.variable(0, selectedDrivers[k].getValue()));

                // position
                final Vector3D refP = new Vector3D(refCartesian[0].getValue(),
                                                   refCartesian[1].getValue(),
                                                   refCartesian[2].getValue());
                final Vector3D resP = new Vector3D(pv.getPosition().getX().getValue(),
                                                   pv.getPosition().getY().getValue(),
                                                   pv.getPosition().getZ().getValue());
                maxPositionValueRelativeError =
                                FastMath.max(maxPositionValueRelativeError, Vector3D.distance(refP, resP) / refP.getNorm());
                final Vector3D refPD = new Vector3D(refCartesian[0].getPartialDerivative(1),
                                                    refCartesian[1].getPartialDerivative(1),
                                                    refCartesian[2].getPartialDerivative(1));
                final Vector3D resPD = new Vector3D(pv.getPosition().getX().getAllDerivatives()[k + 1],
                                                    pv.getPosition().getY().getAllDerivatives()[k + 1],
                                                    pv.getPosition().getZ().getAllDerivatives()[k + 1]);
                maxPositionDerivativeRelativeError =
                                FastMath.max(maxPositionDerivativeRelativeError, Vector3D.distance(refPD, resPD) / refPD.getNorm());

                // velocity
                final Vector3D refV = new Vector3D(refCartesian[3].getValue(),
                                                   refCartesian[4].getValue(),
                                                   refCartesian[5].getValue());
                final Vector3D resV = new Vector3D(pv.getVelocity().getX().getValue(),
                                                   pv.getVelocity().getY().getValue(),
                                                   pv.getVelocity().getZ().getValue());
                maxVelocityValueRelativeError =
                                FastMath.max(maxVelocityValueRelativeError, Vector3D.distance(refV, resV) / refV.getNorm());
                final Vector3D refVD = new Vector3D(refCartesian[3].getPartialDerivative(1),
                                                    refCartesian[4].getPartialDerivative(1),
                                                    refCartesian[5].getPartialDerivative(1));
                final Vector3D resVD = new Vector3D(pv.getVelocity().getX().getAllDerivatives()[k + 1],
                                                    pv.getVelocity().getY().getAllDerivatives()[k + 1],
                                                    pv.getVelocity().getZ().getAllDerivatives()[k + 1]);
                maxVelocityDerivativeRelativeError =
                                FastMath.max(maxVelocityDerivativeRelativeError, Vector3D.distance(refVD, resVD) / refVD.getNorm());

            }

        }

        Assert.assertEquals(0.0, maxPositionValueRelativeError,      relativeTolerancePositionValue);
        Assert.assertEquals(0.0, maxPositionDerivativeRelativeError, relativeTolerancePositionDerivative);
        Assert.assertEquals(0.0, maxVelocityValueRelativeError,      relativeToleranceVelocityValue);
        Assert.assertEquals(0.0, maxVelocityDerivativeRelativeError, relativeToleranceVelocityDerivative);

    }

    private void doTestAngularDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                          double toleranceRotationValue,     double toleranceRotationDerivative,
                                          double toleranceRotationRateValue, double toleranceRotationRateDerivative,
                                          String... parameterPattern)
        throws OrekitException {
        Utils.setDataRoot("regular-data");
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate date0 = date.shiftedBy(50000);
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        final DSFactory factory = new DSFactory(parameterPattern.length, 1);
        final FieldAbsoluteDate<DerivativeStructure> dateDS =
                        new FieldAbsoluteDate<>(factory.getDerivativeField(), date);
        ParameterDriver[] selectedDrivers = new ParameterDriver[parameterPattern.length];
        UnivariateDifferentiableVectorFunction[] dFAngular   = new UnivariateDifferentiableVectorFunction[parameterPattern.length];
        final ParameterDriver[] allDrivers = selectAllDrivers(station);
        for (ParameterDriver driver : allDrivers) {
            driver.setReferenceDate(date0);
        }
        Map<String, Integer> indices = new HashMap<>();
        for (int k = 0; k < dFAngular.length; ++k) {
            for (int i = 0; i < allDrivers.length; ++i) {
                if (allDrivers[i].getName().matches(parameterPattern[k])) {
                    selectedDrivers[k] = allDrivers[i];
                    dFAngular[k]   = differentiatedTransformAngular(station, eme2000, date, selectedDrivers[k], stepFactor);
                    indices.put(selectedDrivers[k].getName(), k);
                }
            }
        };
        DSFactory factory11 = new DSFactory(1, 1);
        RandomGenerator generator = new Well19937a(0xa01a1d8fe5d80af7l);

        double maxRotationValueError          = 0;
        double maxRotationDerivativeError     = 0;
        double maxRotationRateValueError      = 0;
        double maxRotationRateDerivativeError = 0;
        for (int i = 0; i < 1000; ++i) {

            // randomly change one parameter
            ParameterDriver changed = allDrivers[generator.nextInt(allDrivers.length)];
            changed.setNormalizedValue(2 * generator.nextDouble() - 1);

            // transform to check
            FieldTransform<DerivativeStructure> t = station.getOffsetToInertial(eme2000, dateDS, factory, indices);
            for (int k = 0; k < dFAngular.length; ++k) {

                // reference values and derivatives computed using finite differences
                DerivativeStructure[] refAngular = dFAngular[k].value(factory11.variable(0, selectedDrivers[k].getValue()));

                // rotation
                final Rotation refQ = new Rotation(refAngular[0].getValue(),
                                                   refAngular[1].getValue(),
                                                   refAngular[2].getValue(),
                                                   refAngular[3].getValue(),
                                                   true);
                final Rotation resQ = t.getRotation().toRotation();
                maxRotationValueError      = FastMath.max(maxRotationValueError, Rotation.distance(refQ, resQ));
                double sign = FastMath.copySign(1.0,
                                                refAngular[0].getValue() * t.getRotation().getQ0().getValue() +
                                                refAngular[1].getValue() * t.getRotation().getQ1().getValue() +
                                                refAngular[2].getValue() * t.getRotation().getQ2().getValue() +
                                                refAngular[3].getValue() * t.getRotation().getQ3().getValue());
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[0].getPartialDerivative(1) -
                                                                       t.getRotation().getQ0().getAllDerivatives()[k + 1]));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[1].getPartialDerivative(1) -
                                                                       t.getRotation().getQ1().getAllDerivatives()[k + 1]));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[2].getPartialDerivative(1) -
                                                                       t.getRotation().getQ2().getAllDerivatives()[k + 1]));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[3].getPartialDerivative(1) -
                                                                       t.getRotation().getQ3().getAllDerivatives()[k + 1]));

                // rotation rate
                final Vector3D refRate  = new Vector3D(refAngular[4].getValue(), refAngular[5].getValue(), refAngular[6].getValue());
                final Vector3D resRate  = t.getRotationRate().toVector3D();
                final Vector3D refRateD = new Vector3D(refAngular[4].getPartialDerivative(1),
                                                       refAngular[5].getPartialDerivative(1),
                                                       refAngular[6].getPartialDerivative(1));
                final Vector3D resRateD = new Vector3D(t.getRotationRate().getX().getAllDerivatives()[k + 1],
                                                       t.getRotationRate().getY().getAllDerivatives()[k + 1],
                                                       t.getRotationRate().getZ().getAllDerivatives()[k + 1]);
                maxRotationRateValueError      = FastMath.max(maxRotationRateValueError, Vector3D.distance(refRate, resRate));
                maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, Vector3D.distance(refRateD, resRateD));

            }

        }

        Assert.assertEquals(0.0, maxRotationValueError,           toleranceRotationValue);
        Assert.assertEquals(0.0, maxRotationDerivativeError,      toleranceRotationDerivative);
        Assert.assertEquals(0.0, maxRotationRateValueError,       toleranceRotationRateValue);
        Assert.assertEquals(0.0, maxRotationRateDerivativeError,  toleranceRotationRateDerivative);

    }

    private UnivariateDifferentiableVectorFunction differentiatedStationPV(final GroundStation station,
                                                                           final Frame eme2000,
                                                                           final AbsoluteDate date,
                                                                           final ParameterDriver driver,
                                                                           final double stepFactor) {

        final FiniteDifferencesDifferentiator differentiator =
                        new FiniteDifferencesDifferentiator(5, stepFactor * driver.getScale());

        return differentiator.differentiate(new UnivariateVectorFunction() {
            @Override
            public double[] value(double x) {
                final double[] result = new double[6];
                try {
                    final double previouspI = driver.getValue();
                    driver.setValue(x);
                    Transform t = station.getOffsetToInertial(eme2000, date);
                    driver.setValue(previouspI);
                    PVCoordinates stationPV = t.transformPVCoordinates(PVCoordinates.ZERO);
                    result[ 0] = stationPV.getPosition().getX();
                    result[ 1] = stationPV.getPosition().getY();
                    result[ 2] = stationPV.getPosition().getZ();
                    result[ 3] = stationPV.getVelocity().getX();
                    result[ 4] = stationPV.getVelocity().getY();
                    result[ 5] = stationPV.getVelocity().getZ();
                } catch (OrekitException oe) {
                    Assert.fail(oe.getLocalizedMessage());
                }
                return result;
            }
        });
    }

    private UnivariateDifferentiableVectorFunction differentiatedTransformAngular(final GroundStation station,
                                                                                  final Frame eme2000,
                                                                                  final AbsoluteDate date,
                                                                                  final ParameterDriver driver,
                                                                                  final double stepFactor) {

        final FiniteDifferencesDifferentiator differentiator =
                        new FiniteDifferencesDifferentiator(5, stepFactor * driver.getScale());

        return differentiator.differentiate(new UnivariateVectorFunction() {
            private double previous0 = Double.NaN;
            private double previous1 = Double.NaN;
            private double previous2 = Double.NaN;
            private double previous3 = Double.NaN;
            @Override
            public double[] value(double x) {
                final double[] result = new double[7];
                try {
                    final double previouspI = driver.getValue();
                    driver.setValue(x);
                    Transform t = station.getOffsetToInertial(eme2000, date);
                    driver.setValue(previouspI);
                    final double sign;
                    if (Double.isNaN(previous0)) {
                        sign = +1;
                    } else {
                        sign = FastMath.copySign(1.0,
                                                 previous0 * t.getRotation().getQ0() +
                                                 previous1 * t.getRotation().getQ1() +
                                                 previous2 * t.getRotation().getQ2() +
                                                 previous3 * t.getRotation().getQ3());
                    }
                    previous0 = sign * t.getRotation().getQ0();
                    previous1 = sign * t.getRotation().getQ1();
                    previous2 = sign * t.getRotation().getQ2();
                    previous3 = sign * t.getRotation().getQ3();
                    result[0] = previous0;
                    result[1] = previous1;
                    result[2] = previous2;
                    result[3] = previous3;
                    result[4] = t.getRotationRate().getX();
                    result[5] = t.getRotationRate().getY();
                    result[6] = t.getRotationRate().getZ();
                } catch (OrekitException oe) {
                    Assert.fail(oe.getLocalizedMessage());
                }
                return result;
            }
        });
    }

    private ParameterDriver[] selectAllDrivers(final GroundStation station) {
        return new ParameterDriver[] {
            station.getPrimeMeridianOffsetDriver(),
            station.getPrimeMeridianDriftDriver(),
            station.getPolarOffsetXDriver(),
            station.getPolarDriftXDriver(),
            station.getPolarOffsetYDriver(),
            station.getPolarDriftYDriver(),
            station.getEastOffsetDriver(),
            station.getNorthOffsetDriver(),
            station.getZenithOffsetDriver()
        };
    }

}

