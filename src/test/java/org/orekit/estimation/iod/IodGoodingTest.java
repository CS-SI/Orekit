/* Copyright 2002-2025 CS GROUP
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

package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.*;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.Month;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 *
 * Source: <a href="http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.html">...</a>
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodGoodingTest extends AbstractIodTest {

    /** Based on example provided in forum thread:
     * <a href="https://forum.orekit.org/t/iodgooging-orbit-got-from-three-angular-observations/2749">IodGooding</a> */
    @Test
    public void testIssue1166RaDec() {
        AbsoluteDate t1 = new AbsoluteDate(2023, Month.JUNE, 9, 17, 4,59.10, TimeScalesFactory.getUTC());
        AbsoluteDate t2 = new AbsoluteDate(2023, Month.JUNE, 9, 17, 10,50.66, TimeScalesFactory.getUTC());
        AbsoluteDate t3 = new AbsoluteDate(2023, Month.JUNE, 9, 17, 16,21.09, TimeScalesFactory.getUTC());

        Vector3D RA = new Vector3D((15.* (16. + 5./60. + 51.20/3600.)),
                (15.*(16.+ 11./60. + 43.73/3600.)), (15.*(16.+ 17./60. + 15.1/3600. )));

        Vector3D DEC = new Vector3D((-(6.+ 31./60. + 44.22/3600.)),
                (-(6. + 31./60. + 52.36/3600.)),
                (-(6. +32./60. + 0.03/3600.)));

        Frame ITRF = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS ,Constants.WGS84_EARTH_FLATTENING, ITRF);
        GeodeticPoint stationCoord = new GeodeticPoint(FastMath.toRadians(43.05722), FastMath.toRadians(76.971667), 2735.0);
        TopocentricFrame stationFrame = new TopocentricFrame(earth, stationCoord, "N42");
        GroundStation ground_station = new GroundStation(stationFrame);

        double[] angular1 = {FastMath.toRadians(RA.getX()), FastMath.toRadians(DEC.getX())};
        double[] angular2 = {FastMath.toRadians(RA.getY()), FastMath.toRadians(DEC.getY())};
        double[] angular3 = {FastMath.toRadians(RA.getZ()), FastMath.toRadians(DEC.getZ())};

        AngularRaDec raDec1 = new AngularRaDec(ground_station, FramesFactory.getEME2000(), t1,
                angular1, new double[] {1.0, 1.0}, new double[] {1.0, 1.0}, new ObservableSatellite(1));
        AngularRaDec raDec2 = new AngularRaDec(ground_station, FramesFactory.getEME2000(), t2,
                angular2, new double[] {1.0, 1.0}, new double[] {1.0, 1.0}, new ObservableSatellite(1));
        AngularRaDec raDec3 = new AngularRaDec(ground_station, FramesFactory.getEME2000(), t3,
                angular3, new double[] {1.0, 1.0}, new double[] {1.0, 1.0}, new ObservableSatellite(1));

        // Gauss: {a: 4.238973764054024E7; e: 0.004324857593564294; i: 0.09157752601786696; pa: 170.725916897286; raan: 91.00902931155805; v: -19.971524129451392;}
        // Laplace: {a: 4.2394495034863256E7; e: 0.004440883687182993; i: 0.09000218139994348; pa: 173.17005925268154; raan: 91.20208239937111; v: -22.60862919684909;}
        // BEFORE the fix -> Gooding: {a: 6.993021221010809E7; e: 0.3347390725866758; i: 0.5890565053278204; pa: -108.07120996868652; raan: -12.64337508041537; v: 2.587189785272028;}
        // AFTER the fix -> Gooding: {a:  4.2394187540973224E7; e: 0.004411368860770379; i: 0.09185983299662298; pa: 169.74389246605776; raan: 90.92874061328043; v: -18.909215663128727;}
        Orbit estimated_orbit_Gooding = new IodGooding(mu).estimate(eme2000, raDec1,raDec2,raDec3);
        KeplerianOrbit orbitGooding = new KeplerianOrbit(estimated_orbit_Gooding);
        Assertions.assertEquals(4.2394187540973224E7, orbitGooding.getA(), 1.0e-10);
        Assertions.assertEquals(0.004411368860770379, orbitGooding.getE(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(0.09185983299662298), orbitGooding.getI(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(169.74389246605776), orbitGooding.getPerigeeArgument(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(90.92874061328043), orbitGooding.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(-18.909215663128727), orbitGooding.getTrueAnomaly(), 1.0e-10);
    }

    @Test
    public void testIssue1166AzEl() {
        // Generate measurements
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true, 1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,  propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements = EstimationTestUtils.createMeasurements(propagator,
                                                                                                 new AngularAzElMeasurementCreator(context),
                                                                                                 0.0, 1.0, 60.0);
        final AngularAzEl azEl1 = (AngularAzEl) measurements.get(0);
        final AngularAzEl azEl2 = (AngularAzEl) measurements.get(20);
        final AngularAzEl azEl3 = (AngularAzEl) measurements.get(40);

        // Gauss: {a: 1.4240687661878748E7; e: 0.16505257340554763; i: 71.54945520547201; pa: 21.27193872599194; raan: 78.78440298193975; v: -163.45049044435925;}
        // AFTER the fix -> Gooding: {a: 1.4197961507698055E7; e: 0.16923654961240223; i: 71.52638181160407; pa: 21.450082668672675; raan: 78.76324220205018; v: -163.62886990452034;}
        Orbit estimated_orbit_Gooding = new IodGooding(mu).estimate(eme2000, azEl1,azEl2,azEl3);
        KeplerianOrbit orbitGooding = new KeplerianOrbit(estimated_orbit_Gooding);
        Assertions.assertEquals(1.4197961507698055E7, orbitGooding.getA(), 1.0e-10);
        Assertions.assertEquals(0.16923654961240223, orbitGooding.getE(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(71.52638181160407), orbitGooding.getI(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(21.450082668672675), orbitGooding.getPerigeeArgument(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(78.76324220205018), orbitGooding.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(FastMath.toRadians(-163.62886990452034), orbitGooding.getTrueAnomaly(), 1.0e-10);
    }

    @Test
    public void testGooding() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new PVMeasurementCreator(),
                                                       0.0, 1.0, 60.0);

        // measurement data 1
        final int idMeasure1 = 0;
        final AbsoluteDate date1 = measurements.get(idMeasure1).getDate();
        final Vector3D stapos1 = Vector3D.ZERO;/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                                    .getBaseFrame()
                                    .getPVCoordinates(date1, frame)
                                    .getPosition();*/
        final Vector3D position1 = new Vector3D(measurements.get(idMeasure1).getObservedValue()[0],
                                                measurements.get(idMeasure1).getObservedValue()[1],
                                                measurements.get(idMeasure1).getObservedValue()[2]);
        final double r1 = position1.getNorm();
        final Vector3D lineOfSight1 = position1.normalize();

        // measurement data 2
        final int idMeasure2 = 20;
        final AbsoluteDate date2 = measurements.get(idMeasure2).getDate();
        final Vector3D stapos2 = Vector3D.ZERO;/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date2, frame)
                        .getPosition();*/
        final Vector3D position2 = new Vector3D(
                                                measurements.get(idMeasure2).getObservedValue()[0],
                                                measurements.get(idMeasure2).getObservedValue()[1],
                                                measurements.get(idMeasure2).getObservedValue()[2]);
        final Vector3D lineOfSight2 = position2.normalize();

        // measurement data 3
        final int idMeasure3 = 40;
        final AbsoluteDate date3 = measurements.get(idMeasure3).getDate();
        final Vector3D stapos3 = Vector3D.ZERO;/*context.stations.get(0)  // FIXME we need to access the station of the measurement
                        .getBaseFrame()
                        .getPVCoordinates(date3, frame)
                        .getPosition();*/
        final Vector3D position3 = new Vector3D(
                                                measurements.get(idMeasure3).getObservedValue()[0],
                                                measurements.get(idMeasure3).getObservedValue()[1],
                                                measurements.get(idMeasure3).getObservedValue()[2]);
        final double r3 = position3.getNorm();
        final Vector3D lineOfSight3 = position3.normalize();

        // instantiate the IOD method
        final IodGooding iod = new IodGooding(mu);

        // the problem is very sensitive, and unless one can provide the exact
        // initial range estimate, the estimate may be far off the truth...
        final Orbit orbit = iod.estimate(frame,
                                         stapos1, stapos2, stapos3,
                                         lineOfSight1, date1,
                                         lineOfSight2, date2,
                                         lineOfSight3, date3,
                                         r1 * 1.0, r3 * 1.0);
        Assertions.assertEquals(orbit.getA(), context.initialOrbit.getA(), 1.0e-6 * context.initialOrbit.getA());
        Assertions.assertEquals(orbit.getE(), context.initialOrbit.getE(), 1.0e-6 * context.initialOrbit.getE());
        Assertions.assertEquals(orbit.getI(), context.initialOrbit.getI(), 1.0e-6 * context.initialOrbit.getI());

        Assertions.assertEquals(13127847.99808, iod.getRange1(), 1.0e-3);
        Assertions.assertEquals(13375711.51931, iod.getRange2(), 1.0e-3);
        Assertions.assertEquals(13950296.64852, iod.getRange3(), 1.0e-3);


    }

    @Test
    public void testIssue756() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
                EstimationTestUtils.createMeasurements(propagator,
                                                       new AngularRaDecMeasurementCreator(context),
                                                       0.0, 1.0, 60.0);

        // Angular measurements
        final AngularRaDec raDec1 = (AngularRaDec) measurements.get(0);
        final AngularRaDec raDec2 = (AngularRaDec) measurements.get(20);
        final AngularRaDec raDec3 = (AngularRaDec) measurements.get(40);

        // Range estimations
        final double rhoInit1 = 1.3127847998082995E7;
        final double rhoInit3 = 1.3950296648518201E7;

        // instantiate the IOD method
        final IodGooding iod = new IodGooding(mu);

        final KeplerianOrbit orbit1 = new KeplerianOrbit(iod.estimate(frame, raDec1, raDec2, raDec3, rhoInit1, rhoInit3));
        final KeplerianOrbit orbit2 = new KeplerianOrbit(iod.estimate(frame,
                                                         raDec1.getGroundStationPosition(frame),
                                                         raDec2.getGroundStationPosition(frame),
                                                         raDec3.getGroundStationPosition(frame),
                                                         raDec1.getObservedLineOfSight(frame), raDec1.getDate(),
                                                         raDec2.getObservedLineOfSight(frame), raDec2.getDate(),
                                                         raDec3.getObservedLineOfSight(frame), raDec3.getDate(),
                                                         rhoInit1, rhoInit3));

        Assertions.assertEquals(orbit1.getA(), orbit2.getA(), 1.0e-6 * orbit2.getA());
        Assertions.assertEquals(orbit1.getE(), orbit2.getE(), 1.0e-6 * orbit2.getE());
        Assertions.assertEquals(orbit1.getI(), orbit2.getI(), 1.0e-6 * orbit2.getI());
        Assertions.assertEquals(orbit1.getRightAscensionOfAscendingNode(), orbit2.getRightAscensionOfAscendingNode(), 1.0e-6 * orbit2.getRightAscensionOfAscendingNode());
        Assertions.assertEquals(orbit1.getPerigeeArgument(), orbit2.getPerigeeArgument(), FastMath.abs(1.0e-6 * orbit2.getPerigeeArgument()));
        Assertions.assertEquals(orbit1.getMeanAnomaly(), orbit2.getMeanAnomaly(), 1.0e-6 * orbit2.getMeanAnomaly());
        Assertions.assertEquals(orbit1.getMeanAnomaly(), orbit2.getMeanAnomaly(), 1.0e-6 * orbit2.getMeanAnomaly());
    }

    @Test
    public void testIssue1216() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
            context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
            propagatorBuilder);

        final List<ObservedMeasurement<?>> measurements =
            EstimationTestUtils.createMeasurements(propagator,
                new AngularAzElMeasurementCreator(context),
                0.0, 1.0, 60.0);

        // Angular measurements
        final AngularAzEl azEl1 = (AngularAzEl) measurements.get(0);
        final AngularAzEl azEl2 = (AngularAzEl) measurements.get(20);
        final AngularAzEl azEl3 = (AngularAzEl) measurements.get(40);

        // Range estimations
        final double rhoInit1 = 1.3127847998082995E7;
        final double rhoInit3 = 1.3950296648518201E7;

        // instantiate the IOD method
        final IodGooding iod = new IodGooding(mu);

        final KeplerianOrbit orbit1 = new KeplerianOrbit(iod.estimate(frame, azEl1, azEl2, azEl3, rhoInit1, rhoInit3));
        final KeplerianOrbit orbit2 = new KeplerianOrbit(iod.estimate(frame,
            azEl1.getGroundStationPosition(frame),
            azEl2.getGroundStationPosition(frame),
            azEl3.getGroundStationPosition(frame),
            azEl1.getObservedLineOfSight(frame), azEl1.getDate(),
            azEl2.getObservedLineOfSight(frame), azEl2.getDate(),
            azEl3.getObservedLineOfSight(frame), azEl3.getDate(),
            rhoInit1, rhoInit3));

        Assertions.assertEquals(orbit1.getA(), orbit2.getA(), 1.0e-6 * orbit2.getA());
        Assertions.assertEquals(orbit1.getE(), orbit2.getE(), 1.0e-6 * orbit2.getE());
        Assertions.assertEquals(orbit1.getI(), orbit2.getI(), 1.0e-6 * orbit2.getI());
        Assertions.assertEquals(orbit1.getRightAscensionOfAscendingNode(), orbit2.getRightAscensionOfAscendingNode(), 1.0e-6 * orbit2.getRightAscensionOfAscendingNode());
        Assertions.assertEquals(orbit1.getPerigeeArgument(), orbit2.getPerigeeArgument(), FastMath.abs(1.0e-6 * orbit2.getPerigeeArgument()));
        Assertions.assertEquals(orbit1.getMeanAnomaly(), orbit2.getMeanAnomaly(), 1.0e-6 * orbit2.getMeanAnomaly());
        Assertions.assertEquals(orbit1.getMeanAnomaly(), orbit2.getMeanAnomaly(), 1.0e-6 * orbit2.getMeanAnomaly());
    }

}
