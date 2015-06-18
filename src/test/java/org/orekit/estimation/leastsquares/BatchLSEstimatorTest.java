/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation.leastsquares;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.PropagationException;
import org.orekit.estimation.measurements.Evaluation;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Range;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.AstronomicalAmplitudeReader;
import org.orekit.forces.gravity.potential.FESCHatEpsilonReader;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.OceanLoadDeformationCoefficients;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class BatchLSEstimatorTest {

    private IERSConventions                      conventions;
    private OneAxisEllipsoid                     earth;
    private CelestialBody                        sun;
    private CelestialBody                        moon;
    private SphericalSpacecraft                  spacecraft;
    private NormalizedSphericalHarmonicsProvider gravity;
    private TimeScale                            utc;
    private UT1Scale                             ut1;
    private Orbit                                initialOrbit;
    private List<GroundStation>                  stations;

    @Test
    public void testKeplerDistances() throws OrekitException {

        final NumericalPropagatorBuilder propagatorBuilder =
                        createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE);

        // create perfect range measurements
        final double[] parameters = new double[6];
        propagatorBuilder.getOrbitType().mapOrbitToArray(initialOrbit,
                                                         propagatorBuilder.getPositionAngle(),
                                                         parameters);
        final Propagator propagator = propagatorBuilder.buildPropagator(initialOrbit.getDate(), parameters);
        final RangeMeasurementCreator creator = new RangeMeasurementCreator();
        propagator.setMasterMode(300.0, creator);
        final double       period = initialOrbit.getKeplerianPeriod();
        final AbsoluteDate start  = initialOrbit.getDate().shiftedBy(1 * period);
        final AbsoluteDate end    = initialOrbit.getDate().shiftedBy(3 * period);
        propagator.propagate(start, end);

        // create orbit estimator
        final BatchLSEstimator estimator = new BatchLSEstimator(propagatorBuilder,
                                                                new LevenbergMarquardtOptimizer());
        for (final Range range : creator.ranges) {
            estimator.addMeasurement(range);
        }
        estimator.setConvergenceThreshold(1.0e-10, 1.0e-10);
        estimator.setMaxIterations(20);

        // estimate orbit, starting from a wrong point
        Vector3D position = initialOrbit.getPVCoordinates().getPosition().add(new Vector3D(1000.0, 0, 0));
        Vector3D velocity = initialOrbit.getPVCoordinates().getVelocity().add(new Vector3D(0, 0, 1.0));
        Orbit wrongOrbit  = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                               initialOrbit.getFrame(),
                                               initialOrbit.getDate(),
                                               initialOrbit.getMu());
        Orbit estimated   = estimator.estimate(wrongOrbit);
        for (final Evaluation evaluation : estimator.getLastEvaluations()) {
            Range range = (Range) evaluation.getMeasurement();
            System.out.println(range.getDate() +
                               " " + range.getStation().getBaseFrame().getName() +
                               " " + range.getObservedValue()[0] +
                               " " + evaluation.getValue()[0] +
                               " " + (range.getObservedValue()[0] - evaluation.getValue()[0]));
        }
        System.out.println(initialOrbit);
        System.out.println(wrongOrbit);
        System.out.println(estimated);

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential:tides");
        conventions = IERSConventions.IERS_2010;
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(conventions, true));
        sun = CelestialBodyFactory.getSun();
        moon = CelestialBodyFactory.getMoon();
        spacecraft = new SphericalSpacecraft(2.0, 1.2, 0.2, 0.8);
        utc = TimeScalesFactory.getUTC();
        ut1 = TimeScalesFactory.getUT1(conventions, true);
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        AstronomicalAmplitudeReader aaReader =
                        new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        GravityFieldFactory.addOceanTidesReader(new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                                         0.01, FastMath.toRadians(1.0),
                                                                         OceanLoadDeformationCoefficients.IERS_2010,
                                                                         map));
        gravity = GravityFieldFactory.getNormalizedProvider(20, 20);
        initialOrbit = new KeplerianOrbit(15000000.0, 0.125, 1.25,
                                          0.250, 1.375, 0.0625, PositionAngle.TRUE,
                                          FramesFactory.getEME2000(),
                                          new AbsoluteDate(2000, 2, 24, 11, 35,47.0, utc),
                                          gravity.getMu());

        stations = Arrays.asList(//createStation(-18.59146, -173.98363,   76.0, "Leimatu`a"),
                                 createStation(-53.05388,  -75.01551, 1750.0, "Isla Desolación"),
                                 createStation( 62.29639,   -7.01250,  880.0, "Slættaratindur")
                                 //createStation( -4.01583,  103.12833, 3173.0, "Gunung Dempo")
                                 );

    }

    private NumericalPropagatorBuilder createBuilder(final OrbitType orbitType,
                                                     final PositionAngle positionAngle,
                                                     final Force ... forces)
        throws OrekitException {
        final NumericalPropagatorBuilder propagatorBuilder =
                        new NumericalPropagatorBuilder(gravity.getMu(), initialOrbit.getFrame(),
                                                       new DormandPrince853IntegratorBuilder(0.001, 3600.0, 1.0),
                                                       orbitType, positionAngle);
        for (Force force : forces) {
            propagatorBuilder.addForceModel(force.getForceModel(this));
        }

        return propagatorBuilder;

    }

    private GroundStation createStation(double latitudeInDegrees, double longitudeInDegrees,
                                        double altitude, String name)
        throws OrekitException {
        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(latitudeInDegrees),
                                                   FastMath.toRadians(longitudeInDegrees),
                                                   0.0);
        return new GroundStation(new TopocentricFrame(earth, gp, name));
    }

    private enum Force {

        POTENTIAL() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new HolmesFeatherstoneAttractionModel(test.earth.getBodyFrame(), test.gravity);
            }
        },

        THIRD_BODY_SUN() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new ThirdBodyAttraction(test.sun);
            }
        },

        THIRD_BODY_MOON() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new ThirdBodyAttraction(test.moon);
            }
        },

        DRAG() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new DragForce(new HarrisPriester(test.sun, test.earth), test.spacecraft);
            }
        },

        SOLAR_RADIATION_PRESSURE() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new SolarRadiationPressure(test.sun, test.earth.getEquatorialRadius(),
                                                  test.spacecraft);
            }
        },

        OCEAN_TIDES() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) throws OrekitException {
                return new OceanTides(test.earth.getBodyFrame(), test.gravity.getAe(), test.gravity.getMu(),
                                      7, 7, test.conventions, test.ut1);
            }
        },

        SOLID_TIDES() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) throws OrekitException {
                return new SolidTides(test.earth.getBodyFrame(), test.gravity.getAe(), test.gravity.getMu(),
                                      test.gravity.getTideSystem(),
                                      test.conventions, test.ut1, test.sun, test.moon);
            }
        },

        RELATIVITY() {
            public ForceModel getForceModel(BatchLSEstimatorTest test) {
                return new Relativity(test.gravity.getMu());
            }
        };

        public abstract ForceModel getForceModel(BatchLSEstimatorTest test) throws OrekitException;

    }

    /** Local class for creating range measurements. */
    private class RangeMeasurementCreator implements OrekitFixedStepHandler {

        private final List<Range> ranges = new ArrayList<Range>();

        public void init(SpacecraftState s0, AbsoluteDate t) {
            ranges.clear();
        }

        public void handleStep(final SpacecraftState currentState, final boolean isLast)
            throws PropagationException {
            try {
                for (final GroundStation station : stations) {
                    final Vector3D position = currentState.getPVCoordinates().getPosition();
                    final double elevation =
                                    station.getBaseFrame().getElevation(position,
                                                                        currentState.getFrame(),
                                                                        currentState.getDate());

                    if (elevation > FastMath.toRadians(30.0)) {
                        final UnivariateFunction f = new UnivariateFunction() {
                           public double value(final double x) throws OrekitExceptionWrapper {
                               try {
                                   final AbsoluteDate date = currentState.getDate().shiftedBy(x);
                                   final Transform t = station.getBaseFrame().getTransformTo(currentState.getFrame(),
                                                                                             date);
                                   final double d = Vector3D.distance(position,
                                                                      t.transformPosition(Vector3D.ZERO));
                                   return d - x * Constants.SPEED_OF_LIGHT;
                               } catch (OrekitException oe) {
                                   throw new OrekitExceptionWrapper(oe);
                               }
                            }
                        };
                        final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-10, 5);
                        final double dt = solver.solve(1000, f, -1.0, 1.0);
                        final AbsoluteDate date = currentState.getDate().shiftedBy(dt);
                        final Transform t = station.getBaseFrame().getTransformTo(currentState.getFrame(),
                                                                                  date);
                        ranges.add(new Range(station, date,
                                             Vector3D.distance(position,
                                                               t.transformPosition(Vector3D.ZERO)),
                                             15.0, 10));
                    }

                }
            } catch (OrekitException oe) {
                throw new PropagationException(oe);
            }
        }

    }

}


