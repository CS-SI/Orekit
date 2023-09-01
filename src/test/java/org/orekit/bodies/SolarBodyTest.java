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
package org.orekit.bodies;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

public class SolarBodyTest {

    @Test
    public void testNaif() throws UnsupportedEncodingException, IOException {
        Utils.setDataRoot("regular-data");
        final Frame refFrame = FramesFactory.getICRF();
        final TimeScale tdb = TimeScalesFactory.getTDB();
        final InputStream inEntry = getClass().getResourceAsStream("/naif/DE431-ephemeris-NAIF.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inEntry, StandardCharsets.UTF_8));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {

                // extract reference data from Naif
                String[] fields = line.split("\\s+");
                final AbsoluteDate date1 = new AbsoluteDate(fields[0], tdb);
                final AbsoluteDate date2 = new AbsoluteDate(AbsoluteDate.J2000_EPOCH,
                                                            Double.parseDouble(fields[1]),
                                                            tdb);
                String name       = fields[2];
                final String barycenter = fields[3];
                final Vector3D pRef     = new Vector3D(Double.parseDouble(fields[4]) * 1000.0,
                                                       Double.parseDouble(fields[5]) * 1000.0,
                                                       Double.parseDouble(fields[6]) * 1000.0);
                final Vector3D vRef     = new Vector3D(Double.parseDouble(fields[7]) * 1000.0,
                                                       Double.parseDouble(fields[8]) * 1000.0,
                                                       Double.parseDouble(fields[9]) * 1000.0);

                // check position-velocity
                Assertions.assertEquals("BARYCENTER", barycenter);
                if (name.equals("EARTH")) {
                    name = "EARTH-MOON BARYCENTER";
                }
                Assertions.assertEquals(0.0, date2.durationFrom(date1), 8.0e-5);
                final PVCoordinates pv = CelestialBodyFactory.getBody(name).getPVCoordinates(date2,
                                                                                             refFrame);

                Assertions.assertEquals(0.0, Vector3D.distance(pRef, pv.getPosition()), 15.0);
                Assertions.assertEquals(0.0, Vector3D.distance(vRef, pv.getVelocity()), 1.0e-5);
            }
        }
    }

    @Test
    public void testPO405() {

        Utils.setDataRoot("regular-data");
        double threshold = 4.0e-11;

        // extracts from ftp://ssd.jpl.nasa.gov/pub/eph/planets/test-data/testpo.405

        // part of the file covered by Orekit test file unxp0000.405
        //        405  1969.06.01 2440373.5  6  8  2      28.3804268378833
        //        405  1969.07.01 2440403.5 10  4  3       0.2067143944892
        //        405  1969.08.01 2440434.5 11  5  6       0.0028060503833
        //        405  1969.09.01 2440465.5 11  8  4      -0.0026546031502
        //        405  1969.10.01 2440495.5 13  9  2       1.3012071081901
        testPOCoordinate(1969,  6, 1,  6, 8, 2, 28.3804268378833, threshold);
        testPOCoordinate(1969,  7, 1, 10, 4, 3,  0.2067143944892, threshold);
        testPOCoordinate(1969,  8, 1, 11, 5, 6,  0.0028060503833, threshold);
        testPOCoordinate(1969,  9, 1, 11, 8, 4, -0.0026546031502, threshold);
        testPOCoordinate(1969, 10, 1, 13, 9, 2,  1.3012071081901, threshold);

        // part of the file covered by Orekit test file unxp0001.405
        //        405  1970.01.01 2440587.5  3  2  4      -0.0372587543468
        //        405  1970.02.01 2440618.5 12 11  4       0.0000020665428
        //        405  1970.03.01 2440646.5  8  7  1       2.7844971346089
        //        405  1970.04.01 2440677.5  2  8  6       0.0067657404049
        testPOCoordinate(1970, 1, 1,  3,  2, 4, -0.0372587543468, threshold);
        testPOCoordinate(1970, 2, 1, 12, 11, 4,  0.0000020665428, threshold);
        testPOCoordinate(1970, 3, 1,  8,  7, 1,  2.7844971346089, threshold);
        testPOCoordinate(1970, 4, 1,  2,  8, 6,  0.0067657404049, threshold);

        // part of the file covered by Orekit test file unxp0002.405
        //        405  1970.07.01 2440768.5 15  0  4       0.0002194918273
        //        405  1970.08.01 2440799.5  7 12  5      -0.0037257497269
        testPOCoordinate(1970, 7, 1, 15,  0, 4,  0.0002194918273, threshold);
        testPOCoordinate(1970, 8, 1,  7, 12, 5, -0.0037257497269, threshold);

        // part of the file covered by Orekit test file unxp0003.405
        //        405  2003.01.01 2452640.5 12  9  5       0.0008047877725
        //        405  2003.02.01 2452671.5 14  0  1      -0.0000669416537
        //        405  2003.03.01 2452699.5 10  5  3      -1.4209598221498
        //        405  2003.04.01 2452730.5 14  0  3      -0.0000007196601
        //        405  2003.05.01 2452760.5  7 12  3      -4.2775692622201
        //        405  2003.06.01 2452791.5  3  8  3       8.5963291192940
        //        405  2003.07.01 2452821.5  9  6  1      -5.4895120744145
        //        405  2003.08.01 2452852.5  4  5  2      -3.4742823298269
        //        405  2003.09.01 2452883.5  2  5  5      -0.0124587966663
        //        405  2003.10.01 2452913.5 10  9  6       0.0078155256966
        //        405  2003.11.01 2452944.5 10  7  3       4.2838045135189
        //        405  2003.12.01 2452974.5  5  3  6      -0.0050290663372
        //        405  2004.01.01 2453005.5 11  6  5       0.0009776712155
        //        405  2004.02.01 2453036.5  2  7  4      -0.0175274499718
        testPOCoordinate(2003,  1, 1, 12,  9,  5,  0.0008047877725, threshold);
        testPOCoordinate(2003,  2, 1, 14,  0,  1, -0.0000669416537, threshold);
        testPOCoordinate(2003,  3, 1, 10,  5,  3, -1.4209598221498, threshold);
        testPOCoordinate(2003,  4, 1, 14,  0,  3, -0.0000007196601, threshold);
        testPOCoordinate(2003,  5, 1,  7, 12,  3, -4.2775692622201, threshold);
        testPOCoordinate(2003,  6, 1,  3,  8,  3,  8.5963291192940, threshold);
        testPOCoordinate(2003,  7, 1,  9,  6,  1, -5.4895120744145, threshold);
        testPOCoordinate(2003,  8, 1,  4,  5,  2, -3.4742823298269, threshold);
        testPOCoordinate(2003,  9, 1,  2,  5,  5, -0.0124587966663, threshold);
        testPOCoordinate(2003, 10, 1, 10,  9,  6,  0.0078155256966, threshold);
        testPOCoordinate(2003, 11, 1, 10,  7,  3,  4.2838045135189, threshold);
        testPOCoordinate(2003, 12, 1,  5,  3,  6, -0.0050290663372, threshold);
        testPOCoordinate(2004,  1, 1, 11,  6,  5,  0.0009776712155, threshold);
        testPOCoordinate(2004,  2, 1,  2,  7,  4, -0.0175274499718, threshold);

    }

    @Test
    public void testPO406() {

        Utils.setDataRoot("regular-data");
        double threshold = 2.0e-13;

        // extracts from ftp://ssd.jpl.nasa.gov/pub/eph/planets/test-data/testpo.406

        // part of the file covered by Orekit test file unxp0000.406
        //        406  2964.08.01 2803851.5  9 12  6      -0.0011511788059
        //        406  2964.10.01 2803912.5  6  8  5       0.0046432313657
        //        406  2964.12.01 2803973.5 10 11  5       0.0090766356095
        testPOCoordinate(2964,  8, 1,  9, 12, 6, -0.0011511788059, threshold);
        testPOCoordinate(2964, 10, 1,  6,  8, 5,  0.0046432313657, threshold);
        testPOCoordinate(2964, 12, 1, 10, 11, 5,  0.0090766356095, threshold);

    }

    private void testPOCoordinate(final int year, final int month, final int day,
                                  final int targetNumber, final int centerNumber,
                                  final int coordinateNumber, final double coordinateValue,
                                  final double threshold)
        {
        final AbsoluteDate date = new AbsoluteDate(year,  month, day, TimeScalesFactory.getTDB());
        final CelestialBody target = getBody(targetNumber);
        final CelestialBody center = getBody(centerNumber);
        if (target != null && center != null) {
            final PVCoordinates relativePV =
                            new PVCoordinates(center.getPVCoordinates(date, FramesFactory.getICRF()),
                                              target.getPVCoordinates(date, FramesFactory.getICRF()));
            Assertions.assertEquals(coordinateValue, getCoordinate(coordinateNumber, relativePV), threshold);
        }
    }

    private CelestialBody getBody(final int number) {
        switch (number) {
            case 1 :
                return CelestialBodyFactory.getMercury();
            case 2 :
                return CelestialBodyFactory.getVenus();
            case 3 :
                return CelestialBodyFactory.getEarth();
            case 4 :
                return CelestialBodyFactory.getMars();
            case 5 :
                return CelestialBodyFactory.getJupiter();
            case 6 :
                return CelestialBodyFactory.getSaturn();
            case 7 :
                return CelestialBodyFactory.getUranus();
            case 8 :
                return CelestialBodyFactory.getNeptune();
            case 9 :
                return CelestialBodyFactory.getPluto();
            case 10 :
                return CelestialBodyFactory.getMoon();
            case 11 :
                return CelestialBodyFactory.getSun();
            case 12 :
                return CelestialBodyFactory.getSolarSystemBarycenter();
            case 13 :
                return CelestialBodyFactory.getEarthMoonBarycenter();
            default :
                return null;
        }
    }

    public double getCoordinate(final int number, final PVCoordinates pv) {
        switch (number) {
            case 1 :
                return pv.getPosition().getX() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            case 2 :
                return pv.getPosition().getY() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            case 3 :
                return pv.getPosition().getZ() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            case 4 :
                return pv.getVelocity().getX() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            case 5 :
                return pv.getVelocity().getY() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            case 6 :
                return pv.getVelocity().getZ() * Constants.JULIAN_DAY / Constants.JPL_SSD_ASTRONOMICAL_UNIT;
            default :
                return Double.NaN;
        }
    }

    @Test
    public void noMercury() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getMercury();
        });
    }

    @Test
    public void noVenus() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getVenus();
        });
    }

    @Test
    public void noEarthMoonBarycenter() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getEarthMoonBarycenter();
        });
    }

    @Test
    public void noMars() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getMars();
        });
    }

    @Test
    public void noJupiter() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getJupiter();
        });
    }

    @Test
    public void noSaturn() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getSaturn();
        });
    }

    @Test
    public void noUranus() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getUranus();
        });
    }

    @Test
    public void noNeptune() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getNeptune();
        });
    }

    @Test
    public void noPluto() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getPluto();
        });
    }

    @Test
    public void noMoon() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getMoon();
        });
    }

    @Test
    public void noSun() {
        Assertions.assertThrows(OrekitException.class, () -> {
            Utils.setDataRoot("no-data");
            CelestialBodyFactory.getSun();
        });
    }

    @Test
    public void testFrameShift() {
        Utils.setDataRoot("regular-data");
        final Frame moon  = CelestialBodyFactory.getMoon().getBodyOrientedFrame();
        final Frame earth = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final AbsoluteDate date0 = new AbsoluteDate(1969, 06, 25, TimeScalesFactory.getTDB());

        for (double t = 0; t < Constants.JULIAN_DAY; t += 3600) {
            final AbsoluteDate date = date0.shiftedBy(t);
            final Transform transform = earth.getTransformTo(moon, date);
            for (double dt = -10; dt < 10; dt += 0.125) {
                final Transform shifted  = transform.shiftedBy(dt);
                final Transform computed = earth.getTransformTo(moon, transform.getDate().shiftedBy(dt));
                final Transform error    = new Transform(computed.getDate(), computed, shifted.getInverse());
                Assertions.assertEquals(0.0, error.getTranslation().getNorm(),   100.0);
                Assertions.assertEquals(0.0, error.getVelocity().getNorm(),       20.0);
                Assertions.assertEquals(0.0, error.getRotation().getAngle(),    4.0e-8);
                Assertions.assertEquals(0.0, error.getRotationRate().getNorm(), 8.0e-10);
            }
        }
    }

    @Test
    public void testPropagationVsEphemeris() {

        Utils.setDataRoot("regular-data");

        //Creation of the celestial bodies of the solar system
        final CelestialBody sun     = CelestialBodyFactory.getSun();
        final CelestialBody mercury = CelestialBodyFactory.getMercury();
        final CelestialBody venus   = CelestialBodyFactory.getVenus();
        final CelestialBody earth   = CelestialBodyFactory.getEarth();
        final CelestialBody mars    = CelestialBodyFactory.getMars();
        final CelestialBody jupiter = CelestialBodyFactory.getJupiter();
        final CelestialBody saturn  = CelestialBodyFactory.getSaturn();
        final CelestialBody uranus  = CelestialBodyFactory.getUranus();
        final CelestialBody neptune = CelestialBodyFactory.getNeptune();
        final CelestialBody pluto   = CelestialBodyFactory.getPluto();

        //Starting and end dates
        final AbsoluteDate startingDate = new AbsoluteDate(2000, 1, 2, TimeScalesFactory.getUTC());
        AbsoluteDate endDate = startingDate.shiftedBy(30 * Constants.JULIAN_DAY);

        final Frame icrf = FramesFactory.getICRF();

        // fake orbit around negligible point mass at solar system barycenter
        double negligibleMu = 1.0e-3;
        SpacecraftState initialState = new SpacecraftState(new CartesianOrbit(venus.getPVCoordinates(startingDate, icrf),
                                                           icrf, startingDate, negligibleMu));

        //Creation of the numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1000, initialState.getOrbit(), OrbitType.CARTESIAN);
        AbstractIntegrator dop1 = new DormandPrince853Integrator(1.0, 1.0e5, tol[0], tol[1]);
        NumericalPropagator propag = new NumericalPropagator(dop1);
        propag.setOrbitType(OrbitType.CARTESIAN);
        propag.setInitialState(initialState);
        propag.setMu(negligibleMu);

        //Creation of the ForceModels
        propag.addForceModel(new BodyAttraction(sun));
        propag.addForceModel(new BodyAttraction(mercury));
        propag.addForceModel(new BodyAttraction(earth));
        propag.addForceModel(new BodyAttraction(mars));
        propag.addForceModel(new BodyAttraction(jupiter));
        propag.addForceModel(new BodyAttraction(saturn));
        propag.addForceModel(new BodyAttraction(uranus));
        propag.addForceModel(new BodyAttraction(neptune));
        propag.addForceModel(new BodyAttraction(pluto));

        // checks are done within the step handler
        propag.setStepHandler(1000.0, currentState -> {
                // propagated position should remain within 1400m of ephemeris for one month
                Vector3D propagatedP = currentState.getPosition(icrf);
                Vector3D ephemerisP  = venus.getPosition(currentState.getDate(), icrf);
                Assertions.assertEquals(0, Vector3D.distance(propagatedP, ephemerisP), 1400.0);
            });

        propag.propagate(startingDate, endDate);
    }

    private static class BodyAttraction implements ForceModel {

        /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
        public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

        /** Driver for force model parameter. */
        private final ParameterDriver parameterDriver;

        /** The body to consider. */
        private final CelestialBody body;

        /** Simple constructor.
         * @param body the third body to consider
         * (ex: {@link org.orekit.bodies.CelestialBodyFactory#getSun()} or
         * {@link org.orekit.bodies.CelestialBodyFactory#getMoon()})
         */
        public BodyAttraction(final CelestialBody body) {
            parameterDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                                                    body.getGM(), 1.0e-5 * body.getGM(),
                                                    0.0, Double.POSITIVE_INFINITY);
            this.body = body;
        }

        /** {@inheritDoc} */
        @Override
        public boolean dependsOnPositionOnly() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
            {

            final double gm = parameters[0];

            // compute bodies separation vectors and squared norm
            final Vector3D centralToBody = body.getPosition(s.getDate(), s.getFrame());
            final Vector3D satToBody     = centralToBody.subtract(s.getPosition());
            final double   r2Sat         = satToBody.getNormSq();

            // compute relative acceleration
            return new Vector3D(gm / (r2Sat * FastMath.sqrt(r2Sat)), satToBody);

        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {

            final T gm = parameters[0];

            // compute bodies separation vectors and squared norm
            final FieldVector3D<T> centralToBody = body.getPosition(s.getDate(), s.getFrame());
            final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPosition());
            final T                r2Sat         = satToBody.getNormSq();

            // compute absolute acceleration
            return new FieldVector3D<>(r2Sat.multiply(r2Sat.sqrt()).reciprocal().multiply(gm), satToBody);

        }

        /** {@inheritDoc} */
        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
            return Stream.empty();
        }

        /** {@inheritDoc} */
        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.singletonList(parameterDriver);
        }

    }

    @Test
    public void testKepler() {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate(1969, 06, 28, TimeScalesFactory.getTT());
        final double au = 149597870691.0;
        checkKepler(CelestialBodyFactory.getMoon(),    CelestialBodyFactory.getEarth(), date, 3.844e8, 0.012);
        checkKepler(CelestialBodyFactory.getMercury(), CelestialBodyFactory.getSun(),   date,  0.387 * au, 4.0e-9);
        checkKepler(CelestialBodyFactory.getVenus(),   CelestialBodyFactory.getSun(),   date,  0.723 * au, 8.0e-9);
        checkKepler(CelestialBodyFactory.getEarth(),   CelestialBodyFactory.getSun(),   date,  1.000 * au, 2.0e-5);
        checkKepler(CelestialBodyFactory.getMars(),    CelestialBodyFactory.getSun(),   date,  1.52  * au, 2.0e-7);
        checkKepler(CelestialBodyFactory.getJupiter(), CelestialBodyFactory.getSun(),   date,  5.20  * au, 2.0e-6);
        checkKepler(CelestialBodyFactory.getSaturn(),  CelestialBodyFactory.getSun(),   date,  9.58  * au, 8.0e-7);
        checkKepler(CelestialBodyFactory.getUranus(),  CelestialBodyFactory.getSun(),   date, 19.20  * au, 6.0e-7);
        checkKepler(CelestialBodyFactory.getNeptune(), CelestialBodyFactory.getSun(),   date, 30.05  * au, 4.0e-7);
        checkKepler(CelestialBodyFactory.getPluto(),   CelestialBodyFactory.getSun(),   date, 39.24  * au, 3.0e-7);
    }

    private void checkKepler(final PVCoordinatesProvider orbiting, final CelestialBody central,
                             final AbsoluteDate start, final double a, final double epsilon)
        {

        // set up Keplerian orbit of orbiting body around central body
        Orbit orbit = new KeplerianOrbit(orbiting.getPVCoordinates(start, central.getInertiallyOrientedFrame()),
                                         central.getInertiallyOrientedFrame(), start, central.getGM());
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        Assertions.assertEquals(a, orbit.getA(), 0.02 * a);
        double duration = FastMath.min(50 * Constants.JULIAN_DAY, 0.01 * orbit.getKeplerianPeriod());

        double max = 0;
        for (AbsoluteDate date = start; date.durationFrom(start) < duration; date = date.shiftedBy(duration / 100)) {
            PVCoordinates ephemPV = orbiting.getPVCoordinates(date, central.getInertiallyOrientedFrame());
            PVCoordinates keplerPV = propagator.propagate(date).getPVCoordinates();
            Vector3D error = keplerPV.getPosition().subtract(ephemPV.getPosition());
            max = FastMath.max(max, error.getNorm());
        }
        Assertions.assertTrue(max < epsilon * a);
    }
    
    /** Test added for issue  <a href="https://gitlab.orekit.org/orekit/orekit/-/issues/1151">1151</a>.
     * 
     *  Test implementation of {@link JPLCelestialBody#getPosition(AbsoluteDate, Frame)} method.
     */
    @Test
    public void testGetPosition() {
        Utils.setDataRoot("regular-data");
     
        // double test: Given
        // -----------
        
        // J2000 date and frame
        final Frame j2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate();
        
        // Loop on bodies
        for (int iBody = 1; iBody <= 13; iBody++) {
            // When
            final CelestialBody body = getBody(iBody);
            final double dP = Vector3D.distance(body.getPosition(date, j2000),
                                                body.getPosition(date, j2000));
            // Then
            Assertions.assertEquals(0., dP, 0.);
        }
        
        // Field test: given
        // -----------
        
        // Fielded date
        final FieldAbsoluteDate<Binary64> fDate = new FieldAbsoluteDate<>(Binary64Field.getInstance());
        
        // Loop on bodies
        for (int iBody = 1; iBody <= 13; iBody++) {
            // When
            final CelestialBody body = getBody(iBody);
            final Binary64 dP = FieldVector3D.distance(body.getPVCoordinates(fDate, j2000).getPosition(),
                                                body.getPosition(fDate, j2000));
            // Then
            Assertions.assertEquals(0., dP.getReal(), 0.);
        }
    }

}
