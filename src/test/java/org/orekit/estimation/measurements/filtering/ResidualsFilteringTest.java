/* Copyright 2002-2024 CS GROUP
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
package org.orekit.estimation.measurements.filtering;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.generation.EventBasedScheduler;
import org.orekit.estimation.measurements.generation.GatheringSubscriber;
import org.orekit.estimation.measurements.generation.Generator;
import org.orekit.estimation.measurements.generation.MeasurementBuilder;
import org.orekit.estimation.measurements.generation.RangeBuilder;
import org.orekit.estimation.measurements.generation.SignSemantic;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

public class ResidualsFilteringTest {


    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));
    }

    private Propagator buildPropagator(final Orbit orbit) {
        NumericalPropagator propa = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, 0.001, 0.001));
        propa.setOrbitType(OrbitType.CARTESIAN);
        propa.resetInitialState(new SpacecraftState(orbit));
        return propa;
    }

    private MeasurementBuilder<Range> getBuilder(final RandomGenerator random,
                                                   final GroundStation groundStation,
                                                   final ObservableSatellite satellite, final double noise) {
        final RealMatrix covariance = MatrixUtils.createRealDiagonalMatrix(new double[] { noise });
        MeasurementBuilder<Range> rb =
                        new RangeBuilder(new CorrelatedRandomVectorGenerator(covariance, 1.0e-10, new GaussianRandomGenerator(random)),
                                         groundStation, false, 1.0, 1.0, satellite);
        return rb;
    }

    private ElevationDetector getElvetaionDetector(final TopocentricFrame topo, final double minElevation) {
        ElevationDetector detector =
                        new ElevationDetector(topo).
                        withConstantElevation(FastMath.toRadians(5.0));
        return detector;
    }

    private Generator getGenerator(final Orbit orbit, final GroundStation station, final ObservableSatellite satellite, final TopocentricFrame topo, final double noise) {
        Generator generator = new Generator();
        Propagator propagator = buildPropagator(orbit);
        generator.addPropagator(propagator);
        RandomGenerator random = new Well19937a(0x01e226dd859c2c9dl);
        MeasurementBuilder<Range> builder = getBuilder(random, station, satellite, noise);
        EventDetector event = getElvetaionDetector(topo, 10.0);
        FixedStepSelector dateSelecor = new FixedStepSelector(30, TimeScalesFactory.getUTC());
        EventBasedScheduler<Range> scheduler = new EventBasedScheduler<Range>(builder, dateSelecor, propagator, event, SignSemantic.FEASIBLE_MEASUREMENT_WHEN_POSITIVE);
        generator.addScheduler(scheduler);
        return generator;
    }


    @Test
    public void testFilterWithoutRejection() {

        //Create the initial orbit
        final AbsoluteDate date = new AbsoluteDate(2016, 2, 13, 0, 1, 30.0, TimeScalesFactory.getUTC());
        final Vector3D pos = new Vector3D(17427070, -1841865, 20201040);
        final Vector3D vel = new Vector3D(-1102.915, 3471.771, 1237.860);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel),
                                  FramesFactory.getEME2000(), date,
                                  Constants.EGM96_EARTH_MU);

        //Create the measurements generator.
        final Frame bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final double equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double flattening = Constants.WGS84_EARTH_FLATTENING;
        final OneAxisEllipsoid body = new OneAxisEllipsoid(equatorialRadius, flattening, bodyFrame);
        final GeodeticPoint position = new GeodeticPoint(29.868112727, -89.673232869, -14.853146);
        final TopocentricFrame topo = new TopocentricFrame(body, position, "SBCH");
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final GroundStation station = new GroundStation(topo);

        final double noise      = 1;
        final double threshold  = 2.7;
        Generator generator = getGenerator(orbit, station, satellite, topo, noise);
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        generator.addSubscriber(gatherer);
        generator.generate(date, date.shiftedBy(3600*5));
        SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
        final ResidualFilter<Range> filter = new ResidualFilter<>(threshold);

        final List<ObservedMeasurement<?>> processMeasurements = new ArrayList<ObservedMeasurement<?>>();
        for (ObservedMeasurement<?> meas : measurements) {
            final Range range = (Range) meas;
            final SpacecraftState currentSC =
                            new SpacecraftState(orbit.shiftedBy(-1.0 * orbit.getDate().durationFrom(meas.getDate())));
            filter.filter(range, currentSC);
            if (meas.isEnabled()) {
                processMeasurements.add(meas);
            }
        }
        Assertions.assertEquals(processMeasurements.size(), measurements.size());
    }

    @Test
    public void testFilterWithRejection() {

        //Create the initial orbit
        final AbsoluteDate date = new AbsoluteDate(2016, 2, 13, 0, 1, 30.0, TimeScalesFactory.getUTC());
        final Vector3D pos = new Vector3D(17427070, -1841865, 20201040);
        final Vector3D vel = new Vector3D(-1102.915, 3471.771, 1237.860);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(pos, vel),
                                  FramesFactory.getEME2000(), date,
                                  Constants.EGM96_EARTH_MU);

        //Create the measurements generator.
        final Frame bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final double equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double flattening = Constants.WGS84_EARTH_FLATTENING;
        final OneAxisEllipsoid body = new OneAxisEllipsoid(equatorialRadius, flattening, bodyFrame);
        final GeodeticPoint position = new GeodeticPoint(29.868112727, -89.673232869, -14.853146);
        final TopocentricFrame topo = new TopocentricFrame(body, position, "SBCH");
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final GroundStation station = new GroundStation(topo);

        final double noise = 20;
        Generator generator = getGenerator(orbit, station, satellite, topo, noise);
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        generator.addSubscriber(gatherer);
        generator.generate(date, date.shiftedBy(3600*5));
        SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
        final ResidualFilter<Range> filter = new ResidualFilter<>(10);

        final List<ObservedMeasurement<?>> processMeasurements = new ArrayList<ObservedMeasurement<?>>();
        for (ObservedMeasurement<?> meas : measurements) {
            final Range range = (Range) meas;
            final SpacecraftState currentSC =
                            new SpacecraftState(orbit.shiftedBy(-1.0 * orbit.getDate().durationFrom(meas.getDate())));
            filter.filter(range, currentSC);
            if (meas.isEnabled()) {
                processMeasurements.add(meas);
            }
        }
        Assertions.assertEquals(2, measurements.size()-processMeasurements.size());
    }
}
