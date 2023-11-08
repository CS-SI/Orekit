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
package org.orekit.estimation.measurements.gnss;

import java.util.SortedSet;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.generation.EventBasedScheduler;
import org.orekit.estimation.measurements.generation.GatheringSubscriber;
import org.orekit.estimation.measurements.generation.Generator;
import org.orekit.estimation.measurements.generation.SignSemantic;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.attitude.GPSBlockIIA;
import org.orekit.gnss.attitude.GPSBlockIIR;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FixedStepSelector;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class WindUpTest {

    @Test
    public void testYawSteering() {
        // this test corresponds to a classical yaw steering attitude far from turns
        // where Sun remains largely below orbital plane during the turn (β is about -18.8°)
        // in this case, yaw does not evolve a lot, so wind-up changes only about 0.024 cycle
        doTest(new CartesianOrbit(new TimeStampedPVCoordinates(new GNSSDate(1206, 307052.670, SatelliteSystem.GPS).getDate(),
                                                               new Vector3D( 8759594.455119, 12170903.262908, 21973798.932235),
                                                               new Vector3D(-2957.570165356,  2478.252315039,  -263.042027935)),
                                  FramesFactory.getGCRF(),
                                  Constants.EIGEN5C_EARTH_MU),
               new GPSBlockIIA(GPSBlockIIA.getDefaultYawRate(17), GPSBlockIIA.DEFAULT_YAW_BIAS,
                               AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                               CelestialBodyFactory.getSun(), FramesFactory.getGCRF()),
               SatelliteSystem.GPS, 17,
               new GroundStation(new TopocentricFrame(earth,
                                                      new GeodeticPoint(FastMath.toRadians(55.0 + ( 1.0 + 10.0 / 60.0) / 60.0),
                                                                        FastMath.toRadians(82.0 + (55.0 + 22.0 / 60.0) / 60.0),
                                                                        160.0),
                                                      "Новосибирск")),
               0.476338, 0.500405);
    }

    @Test
    public void testMidnightTurn() {
        // this test corresponds to a Block II-A midnight turn (prn = 07, satellite G37)
        // where Sun crosses the orbital plane during the turn (β increases from negative to positive values)
        // this very special case seems to trigger a possible bug in Block IIA attitude model.
        // The spacecraft starts its turn at about 0.1272°/s and the Sun changes
        // side, so the satellites keeps turning for about 70 minutes, completing one turn and an half
        // instead of only one half of a turn. One turn and an half seems unrealistic.
        // The wind-up effect changes therefore almost linearly by about 1.5 cycle
        doTest(new CartesianOrbit(new TimeStampedPVCoordinates(new GNSSDate(1218, 287890.543, SatelliteSystem.GPS).getDate(),
                                                               new Vector3D(-17920092.444521, -11889104.443797, -15318905.173501),
                                                               new Vector3D(   231.983556337,  -3232.849996931,   2163.378049467)),
                                  FramesFactory.getGCRF(),
                                  Constants.EIGEN5C_EARTH_MU),
               new GPSBlockIIA(GPSBlockIIA.getDefaultYawRate(7), GPSBlockIIA.DEFAULT_YAW_BIAS,
                               AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                               CelestialBodyFactory.getSun(), FramesFactory.getGCRF()),
               SatelliteSystem.GPS, 7,
               new GroundStation(new TopocentricFrame(earth,
                                                      new GeodeticPoint(FastMath.toRadians( -(25.0 + 4.0 / 60.0)),
                                                                        FastMath.toRadians(-(130.0 + 6.0 / 60.0)),
                                                                        0.0),
                                                      "Adamstown")),
               -1.853178, -0.361900);
    }

    @Test
    public void testNoonTurn() {
        // this test corresponds to a Block II-R noon turn (prn = 11, satellite G46)
        // where Sun remains slightly above orbital plane during the turn (β is about +1.5°)
        // this is a regular turn, corresponding to a half turn, so wind-up effect changes by about 0.5 cycle
        doTest(new CartesianOrbit(new TimeStampedPVCoordinates(new GNSSDate(1225, 509000.063, SatelliteSystem.GPS).getDate(),
                                                               new Vector3D( 2297608.196826, 20928500.842189, 16246321.092008),
                                                               new Vector3D(-2810.598090399,  1819.511241767, -1939.009527296)),
                                  FramesFactory.getGCRF(),
                                  Constants.EIGEN5C_EARTH_MU),
               new GPSBlockIIR(GPSBlockIIR.DEFAULT_YAW_RATE,
                               AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                               CelestialBodyFactory.getSun(), FramesFactory.getGCRF()),
               SatelliteSystem.GPS, 11,
               new GroundStation(new TopocentricFrame(earth,
                                                      new GeodeticPoint(FastMath.toRadians(   19.0 + (49.0 + 20.0 / 60.0) / 60.0),
                                                                        FastMath.toRadians(-(155.0 + (28.0 + 30.0 / 60.0) / 60.0)),
                                                                        4205.0),
                                                      "Mauna Kea")),
               -0.105573, 0.351411);
    }

    private void doTest(final Orbit orbit, final AttitudeProvider attitudeProvider,
                        final SatelliteSystem system, final int prn, final GroundStation station,
                        final double expectedMin, final double expectedMax) {

        // generate phase measurements from the ground station
        Generator           generator = new Generator();
        ObservableSatellite obsSat    = generator.addPropagator(new KeplerianPropagator(orbit, attitudeProvider));
        PhaseBuilder        builder   = new PhaseBuilder(null, station,
                                                         Frequency.G01.getWavelength(),
                                                         0.01 * Frequency.G01.getWavelength(),
                                                         1.0, obsSat);
        generator.addScheduler(new EventBasedScheduler<>(builder,
                                                         new FixedStepSelector(60.0, TimeScalesFactory.getUTC()),
                                                         generator.getPropagator(obsSat),
                                                         new ElevationDetector(station.getBaseFrame()).
                                                         withConstantElevation(FastMath.toRadians(5.0)).
                                                         withHandler(new ContinueOnEvent()),
                                                         SignSemantic.FEASIBLE_MEASUREMENT_WHEN_POSITIVE));
        final GatheringSubscriber gatherer = new GatheringSubscriber();
        generator.addSubscriber(gatherer);
        generator.generate(orbit.getDate(), orbit.getDate().shiftedBy(7200));
        SortedSet<ObservedMeasurement<?>> measurements = gatherer.getGeneratedMeasurements();
        Assertions.assertEquals(120, measurements.size());

        WindUp windUp = new WindUpFactory().getWindUp(system, prn, Dipole.CANONICAL_I_J, station.getBaseFrame().getName());
        Propagator propagator = new KeplerianPropagator(orbit, attitudeProvider);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (ObservedMeasurement<?> m : measurements) {
            Phase phase = (Phase) m;
            @SuppressWarnings("unchecked")
            EstimatedMeasurementBase<Phase> estimated = (EstimatedMeasurementBase<Phase>) m.estimateWithoutDerivatives(0, 0,
                                                                                                                       new SpacecraftState[] {
                                                                                                                           propagator.propagate(phase.getDate()) 
                                                                                                                       });
            final double original = estimated.getEstimatedValue()[0];
            windUp.modifyWithoutDerivatives(estimated);
            final double modified = estimated.getEstimatedValue()[0];
            final double correction = modified - original;
            min = FastMath.min(min, correction);
            max = FastMath.max(max, correction);
        }
        Assertions.assertEquals(expectedMin, min, 1.0e-5);
        Assertions.assertEquals(expectedMax, max, 1.0e-5);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    @AfterEach
    public void tearDown() {
        earth = null;
    }

    private OneAxisEllipsoid earth;

}
