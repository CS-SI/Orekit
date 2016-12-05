/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.analysis.UnivariateMatrixFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableMatrixFunction;
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
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.GroundStation.OffsetDerivatives;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
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
        final Transform topoToParent = base.getTransformTo(parent.getBodyFrame(), null);
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
                                     0.0, 4.6e-7,
                                     0.0, 1.9e-10);
        Assert.assertEquals(deltaTopo.getX(), moved.getEastOffsetDriver().getValue(),   3.1e-7);
        Assert.assertEquals(deltaTopo.getY(), moved.getNorthOffsetDriver().getValue(),  6.2e-7);
        Assert.assertEquals(deltaTopo.getZ(), moved.getZenithOffsetDriver().getValue(), 2.6e-7);

    }

    @Test
    public void testOffsetDerivativesOctantPxPyPz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.5e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.7e-14;
        double toleranceZDirectionValue       = 1.0e-20;
        double toleranceZDirectionDerivative  = 5.6e-14;
        doTestOffsetDerivatives(FastMath.toRadians(35), FastMath.toRadians(20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxPyMz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.5e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.7e-14;
        double toleranceZDirectionValue       = 1.0e-20;
        double toleranceZDirectionDerivative  = 5.6e-14;
        doTestOffsetDerivatives(FastMath.toRadians(-35), FastMath.toRadians(20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxMyPz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.3e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.7e-14;
        double toleranceZDirectionValue       = 1.0e-20;
        double toleranceZDirectionDerivative  = 5.6e-14;
        doTestOffsetDerivatives(FastMath.toRadians(35), FastMath.toRadians(-20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantPxMyMz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.5e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.7e-14;
        double toleranceZDirectionValue       = 1.0e-20;
        double toleranceZDirectionDerivative  = 5.6e-14;
        doTestOffsetDerivatives(FastMath.toRadians(-35), FastMath.toRadians(-20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxPyPz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.2e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.4e-14;
        double toleranceZDirectionValue       = 1.2e-16;
        double toleranceZDirectionDerivative  = 5.5e-14;
        doTestOffsetDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxPyMz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.2e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.4e-14;
        double toleranceZDirectionValue       = 1.2e-16;
        double toleranceZDirectionDerivative  = 5.5e-14;
        doTestOffsetDerivatives(FastMath.toRadians(150), FastMath.toRadians(20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxMyPz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.2e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.4e-14;
        double toleranceZDirectionValue       = 5.6e-17;
        double toleranceZDirectionDerivative  = 5.5e-14;
        doTestOffsetDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesOctantMxMyMz() throws OrekitException {
        double tolerancePositionValue         = 1.0e-20;
        double tolerancePositionDerivative    = 2.2e-7;
        double toleranceENDirectionValue      = 1.0e-20;
        double toleranceENDirectionDerivative = 5.4e-14;
        double toleranceZDirectionValue       = 5.6e-17;
        double toleranceZDirectionDerivative  = 5.5e-14;
        doTestOffsetDerivatives(FastMath.toRadians(-150), FastMath.toRadians(-20), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    @Test
    public void testOffsetDerivativesNearPole() throws OrekitException {
        double tolerancePositionValue         = 4.5e-16;
        double tolerancePositionDerivative    = 1.1e-8;
        double toleranceENDirectionValue      = 1.2e-16;
        double toleranceENDirectionDerivative = 0.002; // near pole, East and North vectors are singular
        double toleranceZDirectionValue       = 5.3e-23;
        double toleranceZDirectionDerivative  = 4.9e-14;
        doTestOffsetDerivatives(FastMath.toRadians(89.99995), FastMath.toRadians(90), 1200.0,
                                tolerancePositionValue,    tolerancePositionDerivative,
                                toleranceENDirectionValue, toleranceENDirectionDerivative,
                                toleranceZDirectionValue,  toleranceZDirectionDerivative);
    }

    private void doTestOffsetDerivatives(double latitude, double longitude, double altitude,
                                         double tolerancePositionValue, double tolerancePositionDerivative,
                                         double toleranceENDirectionValue, double toleranceENDirectionDerivative,
                                         double toleranceZDirectionValue, double toleranceZDirectionDerivative)
        throws OrekitException {
        Utils.setDataRoot("regular-data");
        final OneAxisEllipsoid earth =
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                             Constants.WGS84_EARTH_FLATTENING,
                                             FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final GroundStation station = new GroundStation(new TopocentricFrame(earth,
                                                                             new GeodeticPoint(latitude, longitude, altitude),
                                                                             "dummy"));
        final int parameters  = 3;
        final int eastIndex   = 0;
        final int northIndex  = 1;
        final int zenithIndex = 2;
        UnivariateDifferentiableMatrixFunction[] dF = new UnivariateDifferentiableMatrixFunction[parameters];
        for (final int k : new int[] { eastIndex, northIndex, zenithIndex }) {
            final FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, 1.0);
            dF[k] = differentiator.differentiate(new UnivariateMatrixFunction() {
                @Override
                public double[][] value(double x) {
                    final double[][] result = new double[4][3];
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
                        Vector3D origin = station.getOffsetFrame().getPVCoordinates(AbsoluteDate.J2000_EPOCH, earth.getFrame()).getPosition();
                        result[0][0] = origin.getX();
                        result[0][1] = origin.getY();
                        result[0][2] = origin.getZ();
                        Vector3D east = station.getOffsetFrame().getEast();
                        result[1][0] = east.getX();
                        result[1][1] = east.getY();
                        result[1][2] = east.getZ();
                        Vector3D north = station.getOffsetFrame().getNorth();
                        result[2][0] = north.getX();
                        result[2][1] = north.getY();
                        result[2][2] = north.getZ();
                        Vector3D zenith = station.getOffsetFrame().getZenith();
                        result[3][0] = zenith.getX();
                        result[3][1] = zenith.getY();
                        result[3][2] = zenith.getZ();
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

        double maxPosValueError        = 0;
        double maxPosDerivativeError   = 0;
        double maxENDirValueError      = 0;
        double maxENDirDerivativeError = 0;
        double maxZDirValueError       = 0;
        double maxZDirDerivativeError  = 0;
        for (double dEast = -2; dEast <= 2; dEast += 0.5) {
            for (double dNorth = -2; dNorth <= 2; dNorth += 0.5) {
                for (double dZenith = -2; dZenith <= 2; dZenith += 0.5) {
                    station.getEastOffsetDriver().setValue(dEast);
                    station.getNorthOffsetDriver().setValue(dNorth);
                    station.getZenithOffsetDriver().setValue(dZenith);
                    OffsetDerivatives od = station.getOffsetDerivatives(parameters, eastIndex, northIndex, zenithIndex);
                    for (final int k : new int[] { eastIndex, northIndex, zenithIndex }) {
                        DerivativeStructure[][] result = dF[k].value(new DerivativeStructure(1, 1, 0, 0.0));
                        int[] orders = new int[3];
                        orders[k] = 1;

                        // position of the offset frame
                        maxPosValueError = FastMath.max(maxPosValueError, FastMath.abs(result[0][0].getValue() - od.getOrigin().getX().getValue()));
                        maxPosValueError = FastMath.max(maxPosValueError, FastMath.abs(result[0][1].getValue() - od.getOrigin().getY().getValue()));
                        maxPosValueError = FastMath.max(maxPosValueError, FastMath.abs(result[0][2].getValue() - od.getOrigin().getZ().getValue()));
                        maxPosDerivativeError = FastMath.max(maxPosDerivativeError, FastMath.abs(result[0][0].getPartialDerivative(1) - od.getOrigin().getX().getPartialDerivative(orders)));
                        maxPosDerivativeError = FastMath.max(maxPosDerivativeError, FastMath.abs(result[0][1].getPartialDerivative(1) - od.getOrigin().getY().getPartialDerivative(orders)));
                        maxPosDerivativeError = FastMath.max(maxPosDerivativeError, FastMath.abs(result[0][2].getPartialDerivative(1) - od.getOrigin().getZ().getPartialDerivative(orders)));

                        // East vector of the offset frame
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[1][0].getValue() - od.getEast().getX().getValue()));
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[1][1].getValue() - od.getEast().getY().getValue()));
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[1][2].getValue() - od.getEast().getZ().getValue()));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[1][0].getPartialDerivative(1) - od.getEast().getX().getPartialDerivative(orders)));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[1][1].getPartialDerivative(1) - od.getEast().getY().getPartialDerivative(orders)));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[1][2].getPartialDerivative(1) - od.getEast().getZ().getPartialDerivative(orders)));

                        // North vector of the offset frame
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[2][0].getValue() - od.getNorth().getX().getValue()));
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[2][1].getValue() - od.getNorth().getY().getValue()));
                        maxENDirValueError = FastMath.max(maxENDirValueError, FastMath.abs(result[2][2].getValue() - od.getNorth().getZ().getValue()));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[2][0].getPartialDerivative(1) - od.getNorth().getX().getPartialDerivative(orders)));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[2][1].getPartialDerivative(1) - od.getNorth().getY().getPartialDerivative(orders)));
                        maxENDirDerivativeError = FastMath.max(maxENDirDerivativeError, FastMath.abs(result[2][2].getPartialDerivative(1) - od.getNorth().getZ().getPartialDerivative(orders)));

                        // Zenith vector of the offset frame
                        maxZDirValueError = FastMath.max(maxZDirValueError, FastMath.abs(result[3][0].getValue() - od.getZenith().getX().getValue()));
                        maxZDirValueError = FastMath.max(maxZDirValueError, FastMath.abs(result[3][1].getValue() - od.getZenith().getY().getValue()));
                        maxZDirValueError = FastMath.max(maxZDirValueError, FastMath.abs(result[3][2].getValue() - od.getZenith().getZ().getValue()));
                        maxZDirDerivativeError = FastMath.max(maxZDirDerivativeError, FastMath.abs(result[3][0].getPartialDerivative(1) - od.getZenith().getX().getPartialDerivative(orders)));
                        maxZDirDerivativeError = FastMath.max(maxZDirDerivativeError, FastMath.abs(result[3][1].getPartialDerivative(1) - od.getZenith().getY().getPartialDerivative(orders)));
                        maxZDirDerivativeError = FastMath.max(maxZDirDerivativeError, FastMath.abs(result[3][2].getPartialDerivative(1) - od.getZenith().getZ().getPartialDerivative(orders)));

                    }
                }
            }
        }

        Assert.assertEquals(0.0, maxPosValueError,        tolerancePositionValue);
        Assert.assertEquals(0.0, maxPosDerivativeError,   tolerancePositionDerivative);
        Assert.assertEquals(0.0, maxENDirValueError,      toleranceENDirectionValue);
        Assert.assertEquals(0.0, maxENDirDerivativeError, toleranceENDirectionDerivative);
        Assert.assertEquals(0.0, maxZDirValueError,       toleranceZDirectionValue);
        Assert.assertEquals(0.0, maxZDirDerivativeError,  toleranceZDirectionDerivative);

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
        GroundStation g = new GroundStation(new TopocentricFrame(nonEllipsoid,
                                                                 new GeodeticPoint(0, 0, 0),
                                                                 "dummy"));
        try {
            g.getOffsetDerivatives(3, 0, 1, 2);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.BODY_SHAPE_IS_NOT_AN_ELLIPSOID, oe.getSpecifier());
        }
    }

}


