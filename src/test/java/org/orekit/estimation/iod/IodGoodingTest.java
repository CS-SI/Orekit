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

package org.orekit.estimation.iod;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularAzElMeasurementCreator;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.AngularRaDecMeasurementCreator;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;

/**
 *
 * Source: <a href="http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.html">...</a>
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodGoodingTest extends AbstractIodTest {

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
