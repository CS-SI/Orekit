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
package org.orekit.estimation.measurements;

import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroundStationTest {

    @Test
    public void testEstimateClockOffset() throws IOException, ClassNotFoundException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // change one station clock
        final TopocentricFrame base  = context.stations.get(0).getBaseFrame();
        final BodyShape parent       = base.getParentShape();
        final double deltaClock      = 0.00084532;
        final String changedSuffix   = "-changed";
        final GroundStation changed  = new GroundStation(new TopocentricFrame(parent, base.getPoint(),
                                                                              base.getName() + changedSuffix),
                                                         context.ut1.getEOPHistory(),
                                                         context.stations.get(0).getDisplacements());

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                propagatorBuilder);
        for (final ObservedMeasurement<?> measurement : measurements) {
            final Range range = (Range) measurement;
            final String name = range.getStation().getBaseFrame().getName() + changedSuffix;
                if (changed.getBaseFrame().getName().equals(name)) {
                    estimator.addMeasurement(new Range(changed, range.isTwoWay(),
                                                       range.getDate().shiftedBy(deltaClock),
                                                       range.getObservedValue()[0],
                                                       range.getTheoreticalStandardDeviation()[0],
                                                       range.getBaseWeight()[0],
                                                       range.getSatellites().get(0)));
                } else {
                    estimator.addMeasurement(range);
                }
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(100);
        estimator.setMaxEvaluations(200);

        // we want to estimate station clock offset
        changed.getClockOffsetDriver().setSelected(true);
        changed.getEastOffsetDriver().setSelected(false);
        changed.getNorthOffsetDriver().setSelected(false);
        changed.getZenithOffsetDriver().setSelected(false);

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 6.8e-7,
                                     0.0, 2.0e-6,
                                     0.0, 1.7e-7,
                                     0.0, 5.9e-11);
        Assertions.assertEquals(deltaClock, changed.getClockOffsetDriver().getValue(), 9.6e-11);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(7,        normalizedCovariances.getRowDimension());
        Assertions.assertEquals(7,        normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(7,        physicalCovariances.getRowDimension());
        Assertions.assertEquals(7,        physicalCovariances.getColumnDimension());
        Assertions.assertEquals(4.185e-9, physicalCovariances.getEntry(6, 6), 3.0e-13);

    }

    @Test
    public void testEstimateStationPosition() throws IOException, ClassNotFoundException {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // move one station
        final RandomGenerator random = new Well19937a(0x4adbecfc743bda60l);
        final TopocentricFrame base = context.stations.get(0).getBaseFrame();
        final BodyShape parent = base.getParentShape();
        final Vector3D baseOrigin = parent.transform(base.getPoint());
        final Vector3D deltaTopo = new Vector3D(2 * random.nextDouble() - 1,
                                                2 * random.nextDouble() - 1,
                                                2 * random.nextDouble() - 1);
        final StaticTransform topoToParent = base.getStaticTransformTo(parent.getBodyFrame(), (AbsoluteDate) null);
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
                    estimator.addMeasurement(new Range(moved, range.isTwoWay(), range.getDate(),
                                                       range.getObservedValue()[0],
                                                       range.getTheoreticalStandardDeviation()[0],
                                                       range.getBaseWeight()[0],
                                                       range.getSatellites().get(0)));
                } else {
                    estimator.addMeasurement(range);
                }
        }
        estimator.setParametersConvergenceThreshold(1.0e-3);
        estimator.setMaxIterations(100);
        estimator.setMaxEvaluations(200);

        // we want to estimate station offsets
        moved.getClockOffsetDriver().setSelected(false);
        moved.getEastOffsetDriver().setSelected(true);
        moved.getNorthOffsetDriver().setSelected(true);
        moved.getZenithOffsetDriver().setSelected(true);

        EstimationTestUtils.checkFit(context, estimator, 2, 3,
                                     0.0, 5.8e-7,
                                     0.0, 1.8e-6,
                                     0.0, 4.8e-7,
                                     0.0, 2.6e-10);
        Assertions.assertEquals(deltaTopo.getX(), moved.getEastOffsetDriver().getValue(),   4.5e-7);
        Assertions.assertEquals(deltaTopo.getY(), moved.getNorthOffsetDriver().getValue(),  6.2e-7);
        Assertions.assertEquals(deltaTopo.getZ(), moved.getZenithOffsetDriver().getValue(), 2.6e-7);

        GeodeticPoint result = moved.getOffsetGeodeticPoint(null);

        GeodeticPoint reference = context.stations.get(0).getBaseFrame().getPoint();
        Assertions.assertEquals(reference.getLatitude(),  result.getLatitude(),  3.3e-14);
        Assertions.assertEquals(reference.getLongitude(), result.getLongitude(), 2.9e-14);
        Assertions.assertEquals(reference.getAltitude(),  result.getAltitude(),  2.6e-7);

        RealMatrix normalizedCovariances = estimator.getOptimum().getCovariances(1.0e-10);
        RealMatrix physicalCovariances   = estimator.getPhysicalCovariances(1.0e-10);
        Assertions.assertEquals(9,       normalizedCovariances.getRowDimension());
        Assertions.assertEquals(9,       normalizedCovariances.getColumnDimension());
        Assertions.assertEquals(9,       physicalCovariances.getRowDimension());
        Assertions.assertEquals(9,       physicalCovariances.getColumnDimension());
        Assertions.assertEquals(0.55431, physicalCovariances.getEntry(6, 6), 1.0e-5);
        Assertions.assertEquals(0.22694, physicalCovariances.getEntry(7, 7), 1.0e-5);
        Assertions.assertEquals(0.13106, physicalCovariances.getEntry(8, 8), 1.0e-5);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(moved.getEstimatedEarthFrame().getTransformProvider());

        Assertions.assertTrue(bos.size() > 138000);
        Assertions.assertTrue(bos.size() < 139000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EstimatedEarthFrameProvider deserialized  = (EstimatedEarthFrameProvider) ois.readObject();
        Assertions.assertEquals(moved.getPrimeMeridianOffsetDriver().getValue(),
                            deserialized.getPrimeMeridianOffsetDriver().getValue(),
                            1.0e-15);
        Assertions.assertEquals(moved.getPrimeMeridianDriftDriver().getValue(),
                            deserialized.getPrimeMeridianDriftDriver().getValue(),
                            1.0e-15);
        Assertions.assertEquals(moved.getPolarOffsetXDriver().getValue(),
                            deserialized.getPolarOffsetXDriver().getValue(),
                            1.0e-15);
        Assertions.assertEquals(moved.getPolarDriftXDriver().getValue(),
                            deserialized.getPolarDriftXDriver().getValue(),
                            1.0e-15);
        Assertions.assertEquals(moved.getPolarOffsetYDriver().getValue(),
                            deserialized.getPolarOffsetYDriver().getValue(),
                            1.0e-15);
        Assertions.assertEquals(moved.getPolarDriftYDriver().getValue(),
                            deserialized.getPolarDriftYDriver().getValue(),
                            1.0e-15);

    }

    @Test
    public void testEstimateEOP() {

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
            Assertions.assertEquals(dut10 - dt * lod / Constants.JULIAN_DAY,
                                linearEOPContext.ut1.getEOPHistory().getUT1MinusUTC(date),
                                1.0e-15);
            Assertions.assertEquals(lod,
                                linearEOPContext.ut1.getEOPHistory().getLOD(date),
                                1.0e-15);
            Assertions.assertEquals((xp0 + xpDot * dt / Constants.JULIAN_DAY) * Constants.ARC_SECONDS_TO_RADIANS,
                                linearEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getXp(),
                                1.0e-15);
            Assertions.assertEquals((yp0 + ypDot * dt / Constants.JULIAN_DAY) * Constants.ARC_SECONDS_TO_RADIANS,
                                linearEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getYp(),
                                1.0e-15);
        }
        final NumericalPropagatorBuilder linearPropagatorBuilder =
                        linearEOPContext.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(linearEOPContext.initialOrbit,
                                                                           linearPropagatorBuilder);
        final List<ObservedMeasurement<?>> linearMeasurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(linearEOPContext),
                                                               1.0, 5.0, 60.0);

        Utils.clearFactories();
        Context zeroEOPContext = EstimationTestUtils.eccentricContext("zero-EOP:regular-data/de431-ephemerides:potential:potential:tides");
        for (double dt = -2 * Constants.JULIAN_DAY; dt < 2 * Constants.JULIAN_DAY; dt += 300.0) {
            AbsoluteDate date = refDate.shiftedBy(dt);
            Assertions.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getUT1MinusUTC(date),
                                1.0e-15);
            Assertions.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getLOD(date),
                                1.0e-15);
            Assertions.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getXp(),
                                1.0e-15);
            Assertions.assertEquals(0.0,
                                zeroEOPContext.ut1.getEOPHistory().getPoleCorrection(date).getYp(),
                                1.0e-15);
        }

        // create orbit estimator
        final NumericalPropagatorBuilder zeroPropagatorBuilder =
                        linearEOPContext.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final BatchLSEstimator estimator = new BatchLSEstimator(new LevenbergMarquardtOptimizer(),
                                                                zeroPropagatorBuilder);
        for (final ObservedMeasurement<?> linearMeasurement : linearMeasurements) {
            Range linearRange = (Range) linearMeasurement;
            for (final GroundStation station : zeroEOPContext.stations) {
                if (station.getBaseFrame().getName().equals(linearRange.getStation().getBaseFrame().getName())) {
                    Range zeroRange = new Range(station, linearRange.isTwoWay(),
                                                linearRange.getDate(),
                                                linearRange.getObservedValue()[0],
                                                linearRange.getTheoreticalStandardDeviation()[0],
                                                linearRange.getBaseWeight()[0],
                                                linearRange.getSatellites().get(0));
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
        Assertions.assertEquals(0.0, FastMath.abs(dut10 - computedDut1),  4.3e-10);
        Assertions.assertEquals(0.0, FastMath.abs(lod - computedLOD),     4.9e-10);
        Assertions.assertEquals(0.0, FastMath.abs(xp0 - computedXp),      5.7e-9);
        Assertions.assertEquals(0.0, FastMath.abs(xpDot - computedXpDot), 7.3e-9);
        Assertions.assertEquals(0.0, FastMath.abs(yp0 - computedYp),      1.1e-9);
        Assertions.assertEquals(0.0, FastMath.abs(ypDot - computedYpDot), 1.1e-10);

        // thresholds to use if orbit is estimated
        // (i.e. when commenting out the loop above that sets orbital parameters drivers to "not selected")
//         Assertions.assertEquals(dut10, computedDut1,  6.6e-3);
//         Assertions.assertEquals(lod,   computedLOD,   1.1e-9);
//         Assertions.assertEquals(xp0,   computedXp,    3.3e-8);
//         Assertions.assertEquals(xpDot, computedXpDot, 2.2e-8);
//         Assertions.assertEquals(yp0,   computedYp,    3.3e-8);
//         Assertions.assertEquals(ypDot, computedYpDot, 3.8e-8);

    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.3e-15;
        double relativeTolerancePositionDerivative =  2.5e-10;
        double relativeToleranceVelocityValue      =  3.0e-15;
        double relativeToleranceVelocityDerivative =  1.7e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.4e-15;
        double relativeTolerancePositionDerivative =  1.7e-10;
        double relativeToleranceVelocityValue      =  2.5e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue              =  1.7e-15;
        double toleranceRotationDerivative         =  5.0e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  2.6e-10;
        double relativeToleranceVelocityValue      =  2.8e-15;
        double relativeToleranceVelocityDerivative =  1.8e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  1.6e-10;
        double relativeToleranceVelocityValue      =  2.3e-15;
        double relativeToleranceVelocityDerivative =  1.7e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue              =  1.5e-15;
        double toleranceRotationDerivative         =  5.0e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  1.9e-15;
        double relativeTolerancePositionDerivative =  2.6e-10;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  2.0e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue              =  1.4e-15;
        double toleranceRotationDerivative         =  5.2e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantMxPyMz() {
        double relativeTolerancePositionValue      =  1.9e-15;
        double relativeTolerancePositionDerivative =  2.6e-10;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  2.0e-10;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantMxPyMz() {
        double toleranceRotationValue              =  1.4e-15;
        double toleranceRotationDerivative         =  5.2e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  1.7e-10;
        double relativeToleranceVelocityValue      =  2.9e-15;
        double relativeToleranceVelocityDerivative =  1.9e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesOctantMxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  1.7e-10;
        double relativeToleranceVelocityValue      =  2.9e-15;
        double relativeToleranceVelocityDerivative =  1.9e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesOctantMxMyMz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  4.9e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testClockOffsetCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  1.2e-15;
        double relativeTolerancePositionDerivative =  1.6e-04;
        double relativeToleranceVelocityValue      =  1.0e-13;
        double relativeToleranceVelocityDerivative =  4.3e-09;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-clock");
    }

    @Test
    public void testClockOffsetAngularDerivativesNearPole() {
        double toleranceRotationValue              =  1.5e-15;
        double toleranceRotationDerivative         =  5.7e-15;
        double toleranceRotationRateValue          =  1.1e-19;
        double toleranceRotationRateDerivative     =  6.3e-19;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-clock");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.3e-15;
        double relativeTolerancePositionDerivative =  1.1e-10;
        double relativeToleranceVelocityValue      =  3.3e-15;
        double relativeToleranceVelocityDerivative =  1.2e-10;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  3.1e-18;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.4e-15;
        double relativeTolerancePositionDerivative =  7.3e-11;
        double relativeToleranceVelocityValue      =  2.8e-15;
        double relativeToleranceVelocityDerivative =  1.3e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue            =  1.7e-15;
        double toleranceRotationDerivative       =  3.6e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  9.0e-11;
        double relativeToleranceVelocityValue      =  2.9e-15;
        double relativeToleranceVelocityDerivative =  8.8e-11;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  3.6e-18;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  4.2e-11;
        double relativeToleranceVelocityValue      =  2.7e-15;
        double relativeToleranceVelocityDerivative =  6.8e-11;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  2.6e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  9.9e-11;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  9.5e-11;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue            =  1.5e-15;
        double toleranceRotationDerivative       =  3.1e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxPyMz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  9.9e-11;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  9.5e-11;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxPyMz() {
        double toleranceRotationValue            =  1.5e-15;
        double toleranceRotationDerivative       =  3.1e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  6.2e-11;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  1.1e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  3.4e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesOctantMxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  6.2e-11;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  1.1e-10;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesOctantMxMyMz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  3.4e-18;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  2.1e-15;
        double relativeTolerancePositionDerivative =  9.4e-10;
        double relativeToleranceVelocityValue      =  7.5e-14;
        double relativeToleranceVelocityDerivative =  3.9e-10;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testStationOffsetAngularDerivativesNearPole() {
        double toleranceRotationValue              =  3.9e-15;
        double toleranceRotationDerivative         =  8.0e-02; // near pole, the East and North directions are singular
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  Precision.SAFE_MIN;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 100.0,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 ".*-offset-East", ".*-offset-North", ".*-offset-Zenith");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.3e-15;
        double relativeTolerancePositionDerivative =  7.3e-09;
        double relativeToleranceVelocityValue      =  3.3e-15;
        double relativeToleranceVelocityDerivative =  4.8e-09;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  6.6e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.4e-15;
        double relativeTolerancePositionDerivative =  4.1e-09;
        double relativeToleranceVelocityValue      =  2.8e-15;
        double relativeToleranceVelocityDerivative =  4.7e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue              =  1.7e-15;
        double toleranceRotationDerivative         =  6.9e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  7.8e-09;
        double relativeToleranceVelocityValue      =  2.9e-15;
        double relativeToleranceVelocityDerivative =  4.4e-09;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  7.5e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  4.1e-09;
        double relativeToleranceVelocityValue      =  2.7e-15;
        double relativeToleranceVelocityDerivative =  4.4e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue            =  1.6e-15;
        double toleranceRotationDerivative       =  7.3e-09;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  8.4e-09;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  5.0e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue              =  1.4e-15;
        double toleranceRotationDerivative         =  6.3e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxPyMz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  8.4e-09;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  5.0e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxPyMz() {
        double toleranceRotationValue              =  1.4e-15;
        double toleranceRotationDerivative         =  6.3e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  5.0e-09;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  5.3e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  7.7e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesOctantMxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  5.0e-09;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  5.3e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesOctantMxMyMz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  7.7e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  1.2e-15;
        double relativeTolerancePositionDerivative =  5.7e-09;
        double relativeToleranceVelocityValue      =  9.4e-13;
        double relativeToleranceVelocityDerivative =  1.2e-09;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "polar-offset-X", "polar-drift-X",
                                   "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPolarMotionAngularDerivativesNearPole() {
        double toleranceRotationValue              =  1.5e-15;
        double toleranceRotationDerivative         =  6.5e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.2e-13;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "polar-offset-X", "polar-drift-X",
                                 "polar-offset-Y", "polar-drift-Y");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxPyPz() {
        double relativeTolerancePositionValue      =  2.3e-15;
        double relativeTolerancePositionDerivative =  5.7e-09;
        double relativeToleranceVelocityValue      =  3.3e-15;
        double relativeToleranceVelocityDerivative =  2.7e-09;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxPyPz() {
        double toleranceRotationValue              =  1.9e-15;
        double toleranceRotationDerivative         =  4.9e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxPyMz() {
        double relativeTolerancePositionValue      =  1.4e-15;
        double relativeTolerancePositionDerivative =  3.1e-09;
        double relativeToleranceVelocityValue      =  2.8e-15;
        double relativeToleranceVelocityDerivative =  3.3e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxPyMz() {
        double toleranceRotationValue            =  1.7e-15;
        double toleranceRotationDerivative       =  5.7e-09;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxMyPz() {
        double relativeTolerancePositionValue      =  1.7e-15;
        double relativeTolerancePositionDerivative =  5.3e-09;
        double relativeToleranceVelocityValue      =  2.9e-15;
        double relativeToleranceVelocityDerivative =  3.4e-09;
        doTestCartesianDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  6.4e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantPxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  3.1e-09;
        double relativeToleranceVelocityValue      =  2.7e-15;
        double relativeToleranceVelocityDerivative =  3.0e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantPxMyMz() {
        double toleranceRotationValue              =  1.5e-15;
        double toleranceRotationDerivative         =  6.3e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxPyPz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  5.5e-09;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  2.8e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxPyPz() {
        double toleranceRotationValue            =  1.5e-15;
        double toleranceRotationDerivative       =  6.8e-09;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxPyMz() {
        double relativeTolerancePositionValue      =  1.6e-15;
        double relativeTolerancePositionDerivative =  5.5e-09;
        double relativeToleranceVelocityValue      =  2.6e-15;
        double relativeToleranceVelocityDerivative =  2.8e-09;
        doTestCartesianDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxPyMz() {
        double toleranceRotationValue            =  1.5e-15;
        double toleranceRotationDerivative       =  6.8e-09;
        double toleranceRotationRateValue        =  1.5e-19;
        double toleranceRotationRateDerivative   =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxMyPz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  3.1e-09;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  3.2e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxMyPz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  6.2e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesOctantMxMyMz() {
        double relativeTolerancePositionValue      =  1.5e-15;
        double relativeTolerancePositionDerivative =  3.1e-09;
        double relativeToleranceVelocityValue      =  3.2e-15;
        double relativeToleranceVelocityDerivative =  3.2e-09;
        doTestCartesianDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesOctantMxMyMz() {
        double toleranceRotationValue              =  1.6e-15;
        double toleranceRotationDerivative         =  6.2e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianCartesianDerivativesNearPole() {
        double relativeTolerancePositionValue      =  1.2e-15;
        double relativeTolerancePositionDerivative =  1.6e-03;
        double relativeToleranceVelocityValue      =  5.8e-14;
        double relativeToleranceVelocityDerivative =  2.6e-08;
        doTestCartesianDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 1.0,
                                   relativeTolerancePositionValue, relativeTolerancePositionDerivative,
                                   relativeToleranceVelocityValue, relativeToleranceVelocityDerivative,
                                   "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testPrimeMeridianAngularDerivativesNearPole() {
        double toleranceRotationValue              =  1.5e-15;
        double toleranceRotationDerivative         =  7.3e-09;
        double toleranceRotationRateValue          =  1.5e-19;
        double toleranceRotationRateDerivative     =  2.0e-13;
        doTestAngularDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0, 0.2,
                                 toleranceRotationValue,     toleranceRotationDerivative,
                                 toleranceRotationRateValue, toleranceRotationRateDerivative,
                                 "prime-meridian-offset", "prime-meridian-drift");
    }

    @Test
    public void testNoReferenceDateGradient() {
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
            station.getOffsetToInertial(eme2000, date, false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals("prime-meridian-offset", (String) oe.getParts()[0]);
        }

        try {
            int freeParameters = 9;
            Map<String, Integer> indices = new HashMap<>();
            for (final ParameterDriver driver : Arrays.asList(station.getPrimeMeridianOffsetDriver(),
                                                              station.getPrimeMeridianDriftDriver(),
                                                              station.getPolarOffsetXDriver(),
                                                              station.getPolarDriftXDriver(),
                                                              station.getPolarOffsetYDriver(),
                                                              station.getPolarDriftYDriver(),
                                                              station.getClockOffsetDriver(),
                                                              station.getEastOffsetDriver(),
                                                              station.getNorthOffsetDriver(),
                                                              station.getZenithOffsetDriver())) {
                indices.put(driver.getNameSpan(date), indices.size());
            }
            station.getOffsetToInertial(eme2000, date, freeParameters, indices);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_REFERENCE_DATE_FOR_PARAMETER, oe.getSpecifier());
            Assertions.assertEquals("prime-meridian-offset", (String) oe.getParts()[0]);
        }

    }

    private void doTestCartesianDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                            double relativeTolerancePositionValue, double relativeTolerancePositionDerivative,
                                            double relativeToleranceVelocityValue, double relativeToleranceVelocityDerivative,
                                            String... parameterPattern) {
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
        final GradientField gradientField = GradientField.getField(parameterPattern.length);
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
                    indices.put(selectedDrivers[k].getNameSpan(date0), k);
                }
            }
        };

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
            FieldTransform<Gradient> t = station.getOffsetToInertial(eme2000, date, parameterPattern.length, indices);
            FieldPVCoordinates<Gradient> pv = t.transformPVCoordinates(FieldPVCoordinates.getZero(gradientField));
            for (int k = 0; k < dFCartesian.length; ++k) {

                // reference values and derivatives computed using finite differences
                Gradient[] refCartesian = dFCartesian[k].value(Gradient.variable(1, 0, selectedDrivers[k].getValue()));

                // position
                final Vector3D refP = new Vector3D(refCartesian[0].getValue(),
                                                   refCartesian[1].getValue(),
                                                   refCartesian[2].getValue());
                final Vector3D resP = new Vector3D(pv.getPosition().getX().getValue(),
                                                   pv.getPosition().getY().getValue(),
                                                   pv.getPosition().getZ().getValue());
                maxPositionValueRelativeError =
                                FastMath.max(maxPositionValueRelativeError, Vector3D.distance(refP, resP) / refP.getNorm());
                final Vector3D refPD = new Vector3D(refCartesian[0].getPartialDerivative(0),
                                                    refCartesian[1].getPartialDerivative(0),
                                                    refCartesian[2].getPartialDerivative(0));
                final Vector3D resPD = new Vector3D(pv.getPosition().getX().getPartialDerivative(k),
                                                    pv.getPosition().getY().getPartialDerivative(k),
                                                    pv.getPosition().getZ().getPartialDerivative(k));
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
                final Vector3D refVD = new Vector3D(refCartesian[3].getPartialDerivative(0),
                                                    refCartesian[4].getPartialDerivative(0),
                                                    refCartesian[5].getPartialDerivative(0));
                final Vector3D resVD = new Vector3D(pv.getVelocity().getX().getPartialDerivative(k),
                                                    pv.getVelocity().getY().getPartialDerivative(k),
                                                    pv.getVelocity().getZ().getPartialDerivative(k));
                maxVelocityDerivativeRelativeError =
                                FastMath.max(maxVelocityDerivativeRelativeError, Vector3D.distance(refVD, resVD) / refVD.getNorm());

            }

        }

        if (maxPositionValueRelativeError      > relativeTolerancePositionValue      ||
            maxPositionDerivativeRelativeError > relativeTolerancePositionDerivative ||
            maxVelocityValueRelativeError      > relativeToleranceVelocityValue      ||
            maxVelocityDerivativeRelativeError > relativeToleranceVelocityDerivative) {
            print("relativeTolerancePositionValue",      maxPositionValueRelativeError);
            print("relativeTolerancePositionDerivative", maxPositionDerivativeRelativeError);
            print("relativeToleranceVelocityValue",      maxVelocityValueRelativeError);
            print("relativeToleranceVelocityDerivative", maxVelocityDerivativeRelativeError);
        }
        Assertions.assertEquals(0.0, maxPositionValueRelativeError,      relativeTolerancePositionValue);
        Assertions.assertEquals(0.0, maxPositionDerivativeRelativeError, relativeTolerancePositionDerivative);
        Assertions.assertEquals(0.0, maxVelocityValueRelativeError,      relativeToleranceVelocityValue);
        Assertions.assertEquals(0.0, maxVelocityDerivativeRelativeError, relativeToleranceVelocityDerivative);

    }

    private void doTestAngularDerivatives(double latitude, double longitude, double altitude, double stepFactor,
                                          double toleranceRotationValue,     double toleranceRotationDerivative,
                                          double toleranceRotationRateValue, double toleranceRotationRateDerivative,
                                          String... parameterPattern) {
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
                    indices.put(selectedDrivers[k].getNameSpan(date0), k);
                }
            }
        };
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
            FieldTransform<Gradient> t = station.getOffsetToInertial(eme2000, date, parameterPattern.length, indices);
            for (int k = 0; k < dFAngular.length; ++k) {

                // reference values and derivatives computed using finite differences
                Gradient[] refAngular = dFAngular[k].value(Gradient.variable(1, 0, selectedDrivers[k].getValue()));

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
                                                          FastMath.abs(sign * refAngular[0].getPartialDerivative(0) -
                                                                       t.getRotation().getQ0().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[1].getPartialDerivative(0) -
                                                                       t.getRotation().getQ1().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[2].getPartialDerivative(0) -
                                                                       t.getRotation().getQ2().getPartialDerivative(k)));
                maxRotationDerivativeError = FastMath.max(maxRotationDerivativeError,
                                                          FastMath.abs(sign * refAngular[3].getPartialDerivative(0) -
                                                                       t.getRotation().getQ3().getPartialDerivative(k)));

                // rotation rate
                final Vector3D refRate  = new Vector3D(refAngular[4].getValue(), refAngular[5].getValue(), refAngular[6].getValue());
                final Vector3D resRate  = t.getRotationRate().toVector3D();
                final Vector3D refRateD = new Vector3D(refAngular[4].getPartialDerivative(0),
                                                       refAngular[5].getPartialDerivative(0),
                                                       refAngular[6].getPartialDerivative(0));
                final Vector3D resRateD = new Vector3D(t.getRotationRate().getX().getPartialDerivative(k),
                                                       t.getRotationRate().getY().getPartialDerivative(k),
                                                       t.getRotationRate().getZ().getPartialDerivative(k));
                maxRotationRateValueError      = FastMath.max(maxRotationRateValueError, Vector3D.distance(refRate, resRate));
                maxRotationRateDerivativeError = FastMath.max(maxRotationRateDerivativeError, Vector3D.distance(refRateD, resRateD));

            }

        }

        if (maxRotationValueError          > toleranceRotationValue          ||
            maxRotationDerivativeError     > toleranceRotationDerivative     ||
            maxRotationRateValueError      > toleranceRotationRateValue      ||
            maxRotationRateDerivativeError > toleranceRotationRateDerivative) {
            print("toleranceRotationValue",          maxRotationValueError);
            print("toleranceRotationDerivative",     maxRotationDerivativeError);
            print("toleranceRotationRateValue",      maxRotationRateValueError);
            print("toleranceRotationRateDerivative", maxRotationRateDerivativeError);
        }
        Assertions.assertEquals(0.0, maxRotationValueError,           toleranceRotationValue);
        Assertions.assertEquals(0.0, maxRotationDerivativeError,      toleranceRotationDerivative);
        Assertions.assertEquals(0.0, maxRotationRateValueError,       toleranceRotationRateValue);
        Assertions.assertEquals(0.0, maxRotationRateDerivativeError,  toleranceRotationRateDerivative);

    }
    private void print(String name, double v) {
        if (v < Precision.SAFE_MIN) {
            System.out.format(Locale.US, "            double %-35s =  Precision.SAFE_MIN;%n", name);
        } else {
            double s = FastMath.pow(10.0, 1 - FastMath.floor(FastMath.log(v) / FastMath.log(10.0)));
            System.out.format(Locale.US, "            double %-35s = %8.1e;%n",
                              name, FastMath.ceil(s * v) / s);
        }
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
                    final double previouspI = driver.getValue(date);
                    driver.setValue(x, new AbsoluteDate());
                    Transform t = station.getOffsetToInertial(eme2000, date, false);
                    driver.setValue(previouspI, date);
                    PVCoordinates stationPV = t.transformPVCoordinates(PVCoordinates.ZERO);
                    result[ 0] = stationPV.getPosition().getX();
                    result[ 1] = stationPV.getPosition().getY();
                    result[ 2] = stationPV.getPosition().getZ();
                    result[ 3] = stationPV.getVelocity().getX();
                    result[ 4] = stationPV.getVelocity().getY();
                    result[ 5] = stationPV.getVelocity().getZ();
                } catch (OrekitException oe) {
                    Assertions.fail(oe.getLocalizedMessage());
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
                    final double previouspI = driver.getValue(date);
                    driver.setValue(x, date);
                    Transform t = station.getOffsetToInertial(eme2000, date, false);
                    driver.setValue(previouspI, date);
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
                    Assertions.fail(oe.getLocalizedMessage());
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
            station.getClockOffsetDriver(),
            station.getEastOffsetDriver(),
            station.getNorthOffsetDriver(),
            station.getZenithOffsetDriver()
        };
    }

}

