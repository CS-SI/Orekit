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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.util.List;

/**
 *
 * Source: http://ccar.colorado.edu/asen5050/projects/projects_2012/kemble/gibbs_derivation.htm
 *
 * @author Joris Olympio
 * @since 7.1
 *
 */
public class IodGibbsTest {

    @Test
    public void testGibbs1() {
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = context.initialOrbit.getMu();
        final Frame frame = context.initialOrbit.getFrame();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final ObservableSatellite satellite = new ObservableSatellite(0);

        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 60.0);

        final Vector3D position1 = new Vector3D(measurements.get(0).getObservedValue()[0],
                                                measurements.get(0).getObservedValue()[1],
                                                measurements.get(0).getObservedValue()[2]);
        final PV pv1 = new PV(measurements.get(0).getDate(), position1, Vector3D.ZERO, 0., 0., 1., satellite);

        final Vector3D position2 = new Vector3D(measurements.get(1).getObservedValue()[0],
                                                measurements.get(1).getObservedValue()[1],
                                                measurements.get(1).getObservedValue()[2]);
        final PV pv2 = new PV(measurements.get(1).getDate(), position2, Vector3D.ZERO, 0., 0., 1., satellite);

        final Vector3D position3 = new Vector3D(measurements.get(2).getObservedValue()[0],
                                                measurements.get(2).getObservedValue()[1],
                                                measurements.get(2).getObservedValue()[2]);
        final PV pv3 = new PV(measurements.get(2).getDate(), position3, Vector3D.ZERO, 0., 0., 1., satellite);

        // instantiate the IOD method
        final IodGibbs gibbs = new IodGibbs(mu);
        final Orbit    orbit = gibbs.estimate(frame, pv1, pv2, pv3);

        Assertions.assertEquals(context.initialOrbit.getA(), orbit.getA(), 1.0e-9 * context.initialOrbit.getA());
        Assertions.assertEquals(context.initialOrbit.getE(), orbit.getE(),  1.0e-9 * context.initialOrbit.getE());
        Assertions.assertEquals(context.initialOrbit.getI(),  orbit.getI(), 1.0e-9 * context.initialOrbit.getI());
    }

    @Test
    public void testGibbs2() {

        // test extracted from "Fundamentals of astrodynamics & applications", D. Vallado, 3rd ed, chap Initial Orbit Determination, Exple 7-3, p457

        //extraction of the context.
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = context.initialOrbit.getMu();

        //initialisation
        final IodGibbs gibbs = new IodGibbs(mu);

        // Observation  vector (EME2000)
        final Vector3D posR1= new Vector3D(0.0, 0.0, 6378137.0);
        final Vector3D posR2= new Vector3D(0.0, -4464696.0, -5102509.0);
        final Vector3D posR3= new Vector3D(0.0, 5740323.0, 3189068);

        //epoch corresponding to the observation vector
        AbsoluteDate dateRef = new AbsoluteDate(2000, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        AbsoluteDate date2 = dateRef.shiftedBy(76.48);
        AbsoluteDate date3 = dateRef.shiftedBy(153.04);

        // Reference result (cf. Vallado)
        final Vector3D velR2 = new Vector3D(0.0, 5531.148, -5191.806);

        //Gibbs IOD
        final Orbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                           posR1, dateRef, posR2, date2, posR3, date3);

        //test
        Assertions.assertEquals(0.0, orbit.getPVCoordinates().getVelocity().getNorm() - velR2.getNorm(), 1e-3);
    }

    @Test
    public void testGibbs3() {

        // test extracted from "Fundamentals of astrodynamics & applications", D. Vallado, 3rd ed, chap Initial Orbit Determination, Exple 7-4, p463
        // Remark: the test value in Vallado is performed with an Herrick-Gibbs methods but results are very close with Gibbs method.

        //extraction of context
        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        final double mu = context.initialOrbit.getMu();

        //Initialisation
        final IodGibbs gibbs = new IodGibbs(mu);

        // Observations vector (EME2000)
        final Vector3D posR1 = new Vector3D(3419855.64, 6019826.02, 2784600.22);
        final Vector3D posR2 = new Vector3D(2935911.95, 6326183.24, 2660595.84);
        final Vector3D posR3 = new Vector3D(2434952.02, 6597386.74, 2521523.11);

        //epoch corresponding to the observation vector
        AbsoluteDate dateRef = new AbsoluteDate(2000, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        AbsoluteDate date2 = dateRef.shiftedBy(76.48);
        AbsoluteDate date3 = dateRef.shiftedBy(153.04);

        // Reference result
        final Vector3D velR2 = new Vector3D(-6441.632, 3777.625, -1720.582);

        //Gibbs IOD
        final Orbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                           posR1, dateRef, posR2, date2, posR3, date3);

        //test for the norm of the velocity
        Assertions.assertEquals(0.0, orbit.getPVCoordinates().getVelocity().getNorm() - velR2.getNorm(),  1e-3);

    }

    @Test
    public void testIssue751() {

        // test extracted from "Fundamentals of astrodynamics & applications", D. Vallado, 3rd ed, chap Initial Orbit Determination, Exple 7-4, p463
        // Remark: the test value in Vallado is performed with an Herrick-Gibbs methods but results are very close with Gibbs method.

        Utils.setDataRoot("regular-data");

        // Initialisation
        final IodGibbs gibbs = new IodGibbs(Constants.WGS84_EARTH_MU);

        // Observable satellite to initialize measurements
        final ObservableSatellite satellite = new ObservableSatellite(0);

        // Observations vector (EME2000)
        final Vector3D posR1 = new Vector3D(3419855.64, 6019826.02, 2784600.22);
        final Vector3D posR2 = new Vector3D(2935911.95, 6326183.24, 2660595.84);
        final Vector3D posR3 = new Vector3D(2434952.02, 6597386.74, 2521523.11);

        // Epoch corresponding to the observation vector
        AbsoluteDate dateRef = new AbsoluteDate(2000, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        AbsoluteDate date2 = dateRef.shiftedBy(76.48);
        AbsoluteDate date3 = dateRef.shiftedBy(153.04);

        // Reference result
        final Vector3D velR2 = new Vector3D(-6441.632, 3777.625, -1720.582);

        // Gibbs IOD
        final Orbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                           new Position(dateRef, posR1, 1.0, 1.0, satellite),
                                           new Position(date2, posR2, 1.0, 1.0, satellite),
                                           new Position(date3, posR3, 1.0, 1.0, satellite));

        // Test for the norm of the velocity
        Assertions.assertEquals(0.0, orbit.getPVCoordinates().getVelocity().getNorm() - velR2.getNorm(),  1e-3);

    }

    @Test
    public void testNonDifferentDatesForObservations() {
        Utils.setDataRoot("regular-data:potential:tides");

        // Initialization
        final double mu = Constants.WGS84_EARTH_MU;
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, TimeScalesFactory.getUTC());
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final PV pv1 = new PV(date, Vector3D.PLUS_I, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv2 = new PV(date, Vector3D.PLUS_J, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv3 = new PV(date, Vector3D.PLUS_K, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);

        // Create the IOD method
        final IodGibbs gibbs = new IodGibbs(mu);

        try {
            gibbs.estimate(frame, pv1, pv2, pv3);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_DIFFERENT_DATES_FOR_OBSERVATIONS, oe.getSpecifier());
        }
    }

    @Test
    public void testNonCoplanarPoints() {
        Utils.setDataRoot("regular-data:potential:tides");

        // Initialization
        final double mu = Constants.WGS84_EARTH_MU;
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, TimeScalesFactory.getUTC());
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final PV pv1 = new PV(date, Vector3D.PLUS_I, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv2 = new PV(date.shiftedBy(1.0), Vector3D.PLUS_J, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv3 = new PV(date.shiftedBy(2.0), Vector3D.PLUS_K, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);

        // Create the IOD method
        final IodGibbs gibbs = new IodGibbs(mu);

        try {
            gibbs.estimate(frame, pv1, pv2, pv3);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_COPLANAR_POINTS, oe.getSpecifier());
        }
    }

}
