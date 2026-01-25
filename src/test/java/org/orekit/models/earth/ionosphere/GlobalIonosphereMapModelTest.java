/* Copyright 2002-2026 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.data.DirectoryCrawlerTest;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class GlobalIonosphereMapModelTest {

    private static final double epsilonParser = 1.0e-16;
    private static final double epsilonDelay  = 0.001;
    private SpacecraftState state;

    /** Earth model. */
    private OneAxisEllipsoid earth;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:ionex");
        final Orbit orbit = new KeplerianOrbit(24464560.0, 0.0, 1.122138, 1.10686, 1.00681,
                                               0.048363, PositionAngleType.MEAN,
                                               FramesFactory.getEME2000(),
                                               new AbsoluteDate(2019, 1, 14, 23, 59, 59.0, TimeScalesFactory.getUTC()),
                                               Constants.WGS84_EARTH_MU);
        state = new SpacecraftState(orbit);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                              Constants.WGS84_EARTH_FLATTENING,
                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    @Test
    public void testDelayAtIPP() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
        try {
            final Method pathDelay = GlobalIonosphereMapModel.class.getDeclaredMethod("pathDelayAtIPP",
                                                                                      AbsoluteDate.class,
                                                                                      GeodeticPoint.class,
                                                                                      Double.TYPE, Double.TYPE);
            pathDelay.setAccessible(true);
            final double delay = (Double) pathDelay.invoke(model,
                                                           new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                                           new GeodeticPoint(latitude, longitude, 0.0),
                                                           0.5 * FastMath.PI, PredefinedGnssSignal.G01.getFrequency());
            Assertions.assertEquals(1.557, delay, epsilonDelay);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testFieldDelayAtIPP() {
        doTestFieldDelayAtIPP(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelayAtIPP(final Field<T> field) {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final T zero = field.getZero();
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
        try {
            final Method pathDelay = GlobalIonosphereMapModel.class.getDeclaredMethod("pathDelayAtIPP",
                                                                                      FieldAbsoluteDate.class,
                                                                                      FieldGeodeticPoint.class,
                                                                                      CalculusFieldElement.class,
                                                                                      Double.TYPE);
            pathDelay.setAccessible(true);
            @SuppressWarnings("unchecked")
            final T delay = (T) pathDelay.invoke(model,
                                                 new FieldAbsoluteDate<>(field, 2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                                 new FieldGeodeticPoint<>(zero.newInstance(latitude),
                                                                          zero.newInstance(longitude),
                                                                          zero),
                                                 zero.add(0.5 * FastMath.PI),
                                                 PredefinedGnssSignal.G01.getFrequency());
            Assertions.assertEquals(1.557, delay.getReal(), epsilonDelay);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testSpacecraftState() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = FastMath.toRadians(98.01);
        double omega = FastMath.toRadians(131.88);
        double OMEGA = FastMath.toRadians(252.24);
        double lv = FastMath.toRadians(250.00);

        AbsoluteDate date = new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC());
        final SpacecraftState state =
                        new SpacecraftState(new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), date,
                                                               Constants.EIGEN5C_EARTH_MU));
        final TopocentricFrame topo = new TopocentricFrame(earth,
                                                           new GeodeticPoint(FastMath.toRadians(20.5236),
                                                                             FastMath.toRadians(85.7881),
                                                                             36.0),
                                                           "Cuttack");
        final double delay = model.pathDelay(state, topo, PredefinedGnssSignal.G01.getFrequency(), null);
        Assertions.assertEquals(2.811, delay, epsilonDelay);

        // the delay at station longitude is different, due to IPP
        try {
            final Method pathDelay = GlobalIonosphereMapModel.class.getDeclaredMethod("pathDelayAtIPP",
                                                                                      AbsoluteDate.class,
                                                                                      GeodeticPoint.class,
                                                                                      Double.TYPE, Double.TYPE);
            pathDelay.setAccessible(true);
            final double delayIPP = (Double) pathDelay.invoke(model, date, topo.getPoint(),
                                                              0.5 * FastMath.PI,
                                                              PredefinedGnssSignal.G01.getFrequency());
            Assertions.assertEquals(2.173, delayIPP, epsilonDelay);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testAboveIono() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = FastMath.toRadians(98.01);
        double omega = FastMath.toRadians(131.88);
        double OMEGA = FastMath.toRadians(252.24);
        double lv = FastMath.toRadians(250.00);

        AbsoluteDate date = new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC());
        final SpacecraftState state =
                        new SpacecraftState(new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), date,
                                                               Constants.EIGEN5C_EARTH_MU));
        final TopocentricFrame topo = new TopocentricFrame(earth,
                                                           new GeodeticPoint(FastMath.toRadians(20.5236),
                                                                             FastMath.toRadians(85.7881),
                                                                             650000.0),
                                                           "very-high");
        final double delay = model.pathDelay(state, topo, PredefinedGnssSignal.G01.getFrequency(), null);
        Assertions.assertEquals(0.0, delay, epsilonDelay);

    }

    @Test
    public void testSpacecraftStateField() {
        doTestSpacecraftStateField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSpacecraftStateField(final Field<T> field) {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final T zero = field.getZero();
        T a = zero.newInstance(7187990.1979844316);
        T e = zero.newInstance(0.5e-4);
        T i = FastMath.toRadians(zero.newInstance(98.01));
        T omega = FastMath.toRadians(zero.newInstance(131.88));
        T OMEGA = FastMath.toRadians(zero.newInstance(252.24));
        T lv = FastMath.toRadians(zero.newInstance(250.00));

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()));
        final FieldSpacecraftState<T> state =
                        new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                                             FramesFactory.getEME2000(), date,
                                                                             zero.newInstance(Constants.EIGEN5C_EARTH_MU)));
        final TopocentricFrame topo = new TopocentricFrame(earth,
                                                           new GeodeticPoint(FastMath.toRadians(20.5236),
                                                                             FastMath.toRadians(85.7881),
                                                                             36.0),
                                                           "Cuttack");
        final T delay = model.pathDelay(state, topo, PredefinedGnssSignal.G01.getFrequency(), null);
        Assertions.assertEquals(2.811, delay.getReal(), epsilonDelay);

        // the delay at station longitude is different, due to IPP
        try {
            final Method pathDelay = GlobalIonosphereMapModel.class.getDeclaredMethod("pathDelayAtIPP",
                                                                                      FieldAbsoluteDate.class,
                                                                                      FieldGeodeticPoint.class,
                                                                                      CalculusFieldElement.class,
                                                                                      Double.TYPE);
            pathDelay.setAccessible(true);
            @SuppressWarnings("unchecked")
            final T delayIPP = (T) pathDelay.invoke(model, date,
                                                    new FieldGeodeticPoint<>(field, topo.getPoint()),
                                                    zero.newInstance(0.5 * FastMath.PI),
                                                    PredefinedGnssSignal.G01.getFrequency());
            Assertions.assertEquals(2.173, delayIPP.getReal(), epsilonDelay);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testAboveIonoField() {
        doTestAboveIonoField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestAboveIonoField(final Field<T> field) {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final T zero = field.getZero();
        T a = zero.newInstance(7187990.1979844316);
        T e = zero.newInstance(0.5e-4);
        T i = FastMath.toRadians(zero.newInstance(98.01));
        T omega = FastMath.toRadians(zero.newInstance(131.88));
        T OMEGA = FastMath.toRadians(zero.newInstance(252.24));
        T lv = FastMath.toRadians(zero.newInstance(250.00));

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()));
        final FieldSpacecraftState<T> state =
                        new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                                                             FramesFactory.getEME2000(), date,
                                                                             zero.newInstance(Constants.EIGEN5C_EARTH_MU)));
        final TopocentricFrame topo = new TopocentricFrame(earth,
                                                           new GeodeticPoint(FastMath.toRadians(20.5236),
                                                                             FastMath.toRadians(85.7881),
                                                                             650000.0),
                                                           "very-high");
        final T delay = model.pathDelay(state, topo, PredefinedGnssSignal.G01.getFrequency(), null);
        Assertions.assertEquals(0.0, delay.getReal(), epsilonDelay);

    }

    @Test
    public void testParser() throws URISyntaxException {

        Utils.setDataRoot("regular-data");
        URL url1 = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/split-1.19i");
        DataSource ds1 = new DataSource(Paths.get(url1.toURI()).toString());
        URL url2 = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/split-2.19i");
        DataSource ds2 = new DataSource(Paths.get(url2.toURI()).toString());
        URL url3 = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/split-3.19i");
        DataSource ds3 = new DataSource(Paths.get(url3.toURI()).toString());
        GlobalIonosphereMapModel model =
            new GlobalIonosphereMapModel(earth, TimeScalesFactory.getUTC(),
                                         GlobalIonosphereMapModel.TimeInterpolator.SIMPLE_LINEAR,
                                         ds1, ds2, ds3);

        // Commons parameters
        final double latitude = FastMath.toRadians(45.0);

        double longitude1;
        double longitude2;

        try {
            final Method pathDelay = GlobalIonosphereMapModel.class.getDeclaredMethod("pathDelayAtIPP",
                                                                                      AbsoluteDate.class,
                                                                                      GeodeticPoint.class,
                                                                                      Double.TYPE, Double.TYPE);
            pathDelay.setAccessible(true);

            // Test longitude = 181° and longitude = -179°
            longitude1 = FastMath.toRadians(181.0);
            longitude2 = FastMath.toRadians(-179.0);
            AbsoluteDate date1 = new AbsoluteDate(2019, 1, 15, 1, 0, 0.0, TimeScalesFactory.getUTC());
            Assertions.assertEquals((Double) pathDelay.invoke(model, date1, new GeodeticPoint(latitude, longitude1, 0.0),
                                                              0.01, PredefinedGnssSignal.G01.getFrequency()),
                                    ((Double) pathDelay.invoke(model, date1, new GeodeticPoint(latitude, longitude2, 0.0),
                                                               0.01, PredefinedGnssSignal.G01.getFrequency())),
                                    epsilonParser);

            // Test longitude = 180° and longitude = -180°
            AbsoluteDate date2 = new AbsoluteDate(2019, 1, 15, 3, 0, 0.0, TimeScalesFactory.getUTC());
            longitude1 = FastMath.toRadians(180.0);
            longitude2 = FastMath.toRadians(-180.0);

            Assertions.assertEquals(((Double) pathDelay.invoke(model, date2, new GeodeticPoint(latitude, longitude1, 0.0),
                                                               0.01, PredefinedGnssSignal.G01.getFrequency())),
                                    ((Double) pathDelay.invoke(model, date2, new GeodeticPoint(latitude, longitude2, 0.0),
                                                               0.01, PredefinedGnssSignal.G01.getFrequency())),
                                    epsilonParser);

            // Test longitude = 0° and longitude = 360°
            AbsoluteDate date3 = new AbsoluteDate(2019, 1, 15, 5, 0, 0.0, TimeScalesFactory.getUTC());
            longitude1 =  FastMath.toRadians(0.);
            longitude2 =  FastMath.toRadians(360.0);

            Assertions.assertEquals(((Double) pathDelay.invoke(model, date3, new GeodeticPoint(latitude, longitude1, 0.0),
                                                               0.01, PredefinedGnssSignal.G01.getFrequency())),
                                    ((Double) pathDelay.invoke(model, date3, new GeodeticPoint(latitude, longitude2, 0.0),
                                                               0.01, PredefinedGnssSignal.G01.getFrequency())),
                                    epsilonParser);

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testCorruptedFileBadData() {
        final String fileName = "corrupted-bad-data-gpsg0150.19i";

        try {
            new GlobalIonosphereMapModel(earth, fileName);
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testEarlierDate() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(state, new TopocentricFrame(earth, point, null),
                            PredefinedGnssSignal.G01.getFrequency(),
                            model.getParameters(new AbsoluteDate()));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE, oe.getSpecifier());
        }
    }

    @Test
    public void testFieldEarlierDate() {
        doTestFieldEarlierDate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldEarlierDate(final Field<T> field) {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(new FieldSpacecraftState<>(field, state), new TopocentricFrame(earth, point, null),
                            PredefinedGnssSignal.G01.getFrequency(),
                            model.getParameters(field, new FieldAbsoluteDate<>(field)));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE, oe.getSpecifier());
        }
    }

    @Deprecated
    @Test
    public void testDeprecated() throws URISyntaxException {
        final TimeScale utc = DataContext.getDefault().getTimeScales().getUTC();
        Assertions.assertEquals(GlobalIonosphereMapModel.TimeInterpolator.SIMPLE_LINEAR,
                                new GlobalIonosphereMapModel(earth, "gpsg0150.19i",
                                                             DataContext.getDefault().getDataProvidersManager(),
                                                             utc).getInterpolator());
        final URL        url = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/gpsg0150.19i");
        final DataSource ds  = new DataSource(url.toURI());
        Assertions.assertEquals(GlobalIonosphereMapModel.TimeInterpolator.SIMPLE_LINEAR,
                                new GlobalIonosphereMapModel(earth, utc, ds).getInterpolator());
    }

    @Test
    public void testTimeInterpolation() throws URISyntaxException {
        final TimeScale  utc = DataContext.getDefault().getTimeScales().getUTC();
        final URI        uri = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/gpsg0150.19i").toURI();
        final OneAxisEllipsoid sphericalEarth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                     0.0,
                                                                     FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                           true));

        final GlobalIonosphereMapModel nearest =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.NEAREST_MAP,
                                         new DataSource(uri));
        final GlobalIonosphereMapModel simple =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.SIMPLE_LINEAR,
                                         new DataSource(uri));
        final GlobalIonosphereMapModel rotated =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.ROTATED_LINEAR,
                                         new DataSource(uri));
        final TopocentricFrame topo = new TopocentricFrame(sphericalEarth,
                                                           new GeodeticPoint(FastMath.toRadians(55.0),
                                                                             FastMath.toRadians(15.0),
                                                                             0.0),
                                                           null);
        final double        frequency = PredefinedGnssSignal.G01.getFrequency();
        final Frame         inertial  = FramesFactory.getEME2000();
        final PVCoordinates above     = new PVCoordinates(new Vector3D(0, 0, 1.5e6), new Vector3D(6.4e3, 0, 0));
        final double        scale     = 0.1;
        final double        factor    = scale * 40.3e16 / (frequency * frequency);

        // check at one exact sampling date and for a vertical line of sight
        final AbsoluteDate    tSampling     = new AbsoluteDate(2019, 1, 15, 14, 0, 0.0, utc);
        final Orbit           oSampling     = new CartesianOrbit(topo.getTransformTo(inertial, tSampling).
                                                                 transformPVCoordinates(above),
                                                                 inertial, tSampling, Constants.WGS84_EARTH_MU);
        final SpacecraftState sSampling     = new SpacecraftState(oSampling);
        final double          gridValue     = 103.0;

        // at exact sampling dates, all interpolation methods should return the same value
        Assertions.assertEquals(gridValue * factor,
                                nearest.pathDelay(sSampling, topo, frequency, nearest.getParameters(tSampling)),
                                1.0e-15);
        Assertions.assertEquals(gridValue * factor,
                                simple.pathDelay(sSampling, topo, frequency, simple.getParameters(tSampling)),
                                1.0e-15);
        Assertions.assertEquals(gridValue * factor,
                                rotated.pathDelay(sSampling, topo, frequency, rotated.getParameters(tSampling)),
                                1.0e-15);

        // check at midtime between sampling dates and for a vertical line of sight
        // between sampling dates, interpolation methods should be different
        final double halfStep = 1800.0;
        final AbsoluteDate    tInterp   = tSampling.shiftedBy(halfStep);

        final Orbit           oInterp   = new CartesianOrbit(topo.getTransformTo(inertial, tInterp).
                                                             transformPVCoordinates(above),
                                                             inertial, tInterp, Constants.WGS84_EARTH_MU);
        final SpacecraftState sInterp   = new SpacecraftState(oInterp);
        final double          gridValue1 = 103.0;
        final double          gridValue2 = 101.0;
        Assertions.assertEquals(gridValue1 * factor,
                                nearest.pathDelay(sInterp, topo, frequency, nearest.getParameters(tInterp)),
                                1.0e-15);
        Assertions.assertEquals((0.5 * gridValue1 + 0.5 * gridValue2) * factor,
                                simple.pathDelay(sInterp, topo, frequency, simple.getParameters(tInterp)),
                                1.0e-15);

        // the half hour offsets is about 7.52 degrees, so maps rotates about 1.504 cell size
        // it is also exactly at mid-time between the two TEC maps
        final double siderealDay   = MathUtils.TWO_PI / Constants.WGS84_EARTH_ANGULAR_VELOCITY;
        final double cellDriftRate = 72.0 / siderealDay;
        final double cellRatio     = halfStep * cellDriftRate - 1.0; // very slightly above 0.504
        final double interpMapA    = ((1 - cellRatio) * 104.0 + cellRatio * 105.0) * factor;
        final double interpMapB    = ((1 - cellRatio) * 101.0 + cellRatio * 100.0) * factor;
        Assertions.assertEquals(0.5 * (interpMapA + interpMapB),
                                rotated.pathDelay(sInterp, topo, frequency, rotated.getParameters(tInterp)),
                                1.0e-15);

    }

    @Test
    public void testFieldTimeInterpolation() throws URISyntaxException {
        doTestFieldTimeInterpolation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldTimeInterpolation(Field<T> field) throws URISyntaxException {
        final T zero = field.getZero();
        final TimeScale  utc = DataContext.getDefault().getTimeScales().getUTC();
        final URI        uri = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/gpsg0150.19i").toURI();

        final OneAxisEllipsoid sphericalEarth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                     0.0,
                                                                     FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                           true));
        final GlobalIonosphereMapModel nearest =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.NEAREST_MAP,
                                         new DataSource(uri));
        final GlobalIonosphereMapModel simple =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.SIMPLE_LINEAR,
                                         new DataSource(uri));
        final GlobalIonosphereMapModel rotated =
            new GlobalIonosphereMapModel(sphericalEarth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.ROTATED_LINEAR,
                                         new DataSource(uri));
        final TopocentricFrame topo = new TopocentricFrame(sphericalEarth,
                                                           new GeodeticPoint(FastMath.toRadians(55.0),
                                                                             FastMath.toRadians(15.0),
                                                                             0.0),
                                                           null);
        final double        frequency = PredefinedGnssSignal.G01.getFrequency();
        final Frame         inertial  = FramesFactory.getEME2000();
        final FieldPVCoordinates<T> above     = new FieldPVCoordinates<>(new FieldVector3D<>(zero,
                                                                                             zero,
                                                                                             zero.newInstance(1.5e6)),
                                                                         new FieldVector3D<>(zero.newInstance(6.4e3),
                                                                                             zero,
                                                                                             zero));
        final double        scale     = 0.1;
        final double        factor    = scale * 40.3e16 / (frequency * frequency);

        // check at one exact sampling date and for a vertical line of sight
        final FieldAbsoluteDate<T>    tSampling     = new FieldAbsoluteDate<>(field,
                                                                              new AbsoluteDate(2019, 1, 15, 14, 0, 0.0, utc));
        final FieldOrbit<T>           oSampling     = new FieldCartesianOrbit<>(topo.getTransformTo(inertial, tSampling).
                                                                                transformPVCoordinates(above),
                                                                                inertial, tSampling,
                                                                                zero.newInstance(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> sSampling     = new FieldSpacecraftState<>(oSampling);
        final double          gridValue     = 103.0;

        // at exact sampling dates, all interpolation methods should return the same value
        Assertions.assertEquals(gridValue * factor,
                                nearest.pathDelay(sSampling, topo, frequency, nearest.getParameters(field, tSampling)).getReal(),
                                1.0e-15);
        Assertions.assertEquals(gridValue * factor,
                                simple.pathDelay(sSampling, topo, frequency, simple.getParameters(field, tSampling)).getReal(),
                                1.0e-15);
        Assertions.assertEquals(gridValue * factor,
                                rotated.pathDelay(sSampling, topo, frequency, rotated.getParameters(field, tSampling)).getReal(),
                                1.0e-15);

        // check at midtime between sampling dates and for a vertical line of sight
        // between sampling dates, interpolation methods should be different
        final double halfStep = 1800.0;
        final FieldAbsoluteDate<T>    tInterp   = tSampling.shiftedBy(halfStep);

        final FieldOrbit<T>           oInterp   = new FieldCartesianOrbit<>(topo.getTransformTo(inertial, tInterp).
                                                                            transformPVCoordinates(above),
                                                                            inertial, tInterp,
                                                                            zero.newInstance(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> sInterp   = new FieldSpacecraftState<>(oInterp);
        final double          gridValue1 = 103.0;
        final double          gridValue2 = 101.0;
        Assertions.assertEquals(gridValue1 * factor,
                                nearest.pathDelay(sInterp, topo, frequency, nearest.getParameters(field, tInterp)).getReal(),
                                1.0e-15);
        Assertions.assertEquals((0.5 * gridValue1 + 0.5 * gridValue2) * factor,
                                simple.pathDelay(sInterp, topo, frequency, simple.getParameters(field, tInterp)).getReal(),
                                1.0e-15);

        // the half hour offsets is about 7.52 degrees, so maps rotates about 1.504 cell size
        // it is also exactly at mid-time between the two TEC maps
        final double siderealDay   = MathUtils.TWO_PI / Constants.WGS84_EARTH_ANGULAR_VELOCITY;
        final double cellDriftRate = 72.0 / siderealDay;
        final double cellRatio     = halfStep * cellDriftRate - 1.0; // very slightly above 0.504
        final double interpMapA    = ((1 - cellRatio) * 104.0 + cellRatio * 105.0) * factor;
        final double interpMapB    = ((1 - cellRatio) * 101.0 + cellRatio * 100.0) * factor;
        Assertions.assertEquals(0.5 * (interpMapA + interpMapB),
                                rotated.pathDelay(sInterp, topo, frequency, rotated.getParameters(field, tInterp)).getReal(),
                                1.0e-15);

    }

    /* @Test
    public void testNotEllipsoid() throws URISyntaxException {
        // NOTE: This test is no longer valid and should be moved to the creation of the IonosphericModel itself, 
        // since what it is testing is the proper plugging in of the OneAxisEllipsoid data, which has been moved.
        Utils.setDataRoot("regular-data:ionex:potential");
        final DataContext dataContext = DataContext.getDefault();
        final TimeScale   utc         = dataContext.getTimeScales().getUTC();
        final URI         uri         = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/gpsg0150.19i").toURI();
        final GlobalIonosphereMapModel gim =
            new GlobalIonosphereMapModel(earth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.ROTATED_LINEAR,
                                         new DataSource(uri));
        try {
            final SpacecraftState shifted = state.shiftedBy(2000.0);
            final Frame       itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            final Geoid       geoid       = new Geoid(dataContext.getGravityFields().getNormalizedProvider(2, 0),
                                                      ReferenceEllipsoid.getWgs84(itrf));
            gim.pathDelay(shifted,
                          new TopocentricFrame(geoid, new GeodeticPoint(0, 0, 0.0), "dummy"),
                          PredefinedGnssSignal.G01.getFrequency(), gim.getParameters(shifted.getDate()));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.BODY_SHAPE_MUST_BE_A_ONE_AXIS_ELLIPSOID, oe.getSpecifier());
        }
    } */

    /*
   @Test
    public void testFieldNotEllipsoid() throws URISyntaxException {
        doTestFieldNotEllipsoid(Binary64Field.getInstance());
   }

   private <T extends CalculusFieldElement<T>> void doTestFieldNotEllipsoid(final Field<T> field)
        // NOTE: This test is no longer valid and should be moved to the creation of the IonosphericModel itself, 
        // since what it is testing is the proper plugging in of the OneAxisEllipsoid data, which has been moved.
       throws URISyntaxException {
        Utils.setDataRoot("regular-data:ionex:potential");
        final DataContext dataContext = DataContext.getDefault();
        final TimeScale   utc         = dataContext.getTimeScales().getUTC();
        final URI         uri         = DirectoryCrawlerTest.class.getClassLoader().getResource("ionex/gpsg0150.19i").toURI();
        final GlobalIonosphereMapModel gim =
            new GlobalIonosphereMapModel(earth, utc,
                                         GlobalIonosphereMapModel.TimeInterpolator.ROTATED_LINEAR,
                                         new DataSource(uri));
        try {
            final FieldSpacecraftState<T> shifted = new FieldSpacecraftState<>(field, state.shiftedBy(2000.0));
            final Frame       itrf        = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            final Geoid       geoid       = new Geoid(dataContext.getGravityFields().getNormalizedProvider(2, 0),
                                                      ReferenceEllipsoid.getWgs84(itrf));
            gim.pathDelay(shifted,
                          new TopocentricFrame(geoid, new GeodeticPoint(0, 0, 0.0), "dummy"),
                          PredefinedGnssSignal.G01.getFrequency(), gim.getParameters(field, shifted.getDate()));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.BODY_SHAPE_MUST_BE_A_ONE_AXIS_ELLIPSOID, oe.getSpecifier());
        }
    }
         */

    @Test
    public void testLaterDate() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(state, new TopocentricFrame(earth, point, null),
                            PredefinedGnssSignal.G01.getFrequency(),
                            model.getParameters(new AbsoluteDate()));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE, oe.getSpecifier());
        }

    }

    @Test
    public void testFieldLaterDate() {
        doTestFieldLaterDate(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldLaterDate(final Field<T> field) {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);
        try {
            model.pathDelay(new FieldSpacecraftState<>(field, state), new TopocentricFrame(earth, point, null),
                            PredefinedGnssSignal.G01.getFrequency(),
                            model.getParameters(field, new FieldAbsoluteDate<>(field)));
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE, oe.getSpecifier());
        }

    }

    @Test
    public void testLastDate() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);
        final Orbit orbit = new KeplerianOrbit(24464560.0, 0.0, 1.122138, 1.10686, 1.00681,
                                               0.048363, PositionAngleType.MEAN,
                                               FramesFactory.getEME2000(),
                                               new AbsoluteDate(2019, 1, 16, TimeScalesFactory.getUTC()),
                                               Constants.WGS84_EARTH_MU);
        final double delay = model.pathDelay(new SpacecraftState(orbit), new TopocentricFrame(earth, point, null),
                                             PredefinedGnssSignal.G01.getFrequency(),
                                             model.getParameters(orbit.getDate()));
        Assertions.assertEquals(3.156, delay, 1.0e-3);
    }

    @Test
    public void testLastDateField() {
        GlobalIonosphereMapModel model = new GlobalIonosphereMapModel(earth, "gpsg0150.19i");
        final Field<Binary64> field = Binary64Field.getInstance();
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);
        final FieldOrbit<Binary64> orbit =
                new FieldKeplerianOrbit<>(field,
                                          new KeplerianOrbit(24464560.0, 0.0, 1.122138, 1.10686, 1.00681,
                                                              0.048363, PositionAngleType.MEAN,
                                                              FramesFactory.getEME2000(),
                                                              new AbsoluteDate(2019, 1, 16, TimeScalesFactory.getUTC()),
                                                              Constants.WGS84_EARTH_MU));
        final Binary64 delay = model.pathDelay(new FieldSpacecraftState<>(orbit),
                                               new TopocentricFrame(earth, point, null),
                                               PredefinedGnssSignal.G01.getFrequency(),
                                               model.getParameters(field, orbit.getDate()));
        Assertions.assertEquals(3.156, delay.getReal(), 1.0e-3);
    }

    @Test
    /**
     * The goal of this test is to verify if an OrekitException is thrown when latitude or longitude
     * boundaries are not present in the header section of the Global Ionosphere Map.
     */
    public void testIssue621() {
        final String fileName  = "missing-lat-lon-header-gpsg0150.19i";

        try {
            new GlobalIonosphereMapModel(earth, fileName);
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_LATITUDE_LONGITUDE_BOUNDARIES_IN_IONEX_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testJammedFields() {
        Assertions.assertNotNull(new GlobalIonosphereMapModel(earth, "jammed-fields.14i"));
    }

    @Test
    public void testMissingEpochInHeader() {
        final String fileName  = "missing-epoch-header-gpsg0150.19i";

        try {
            new GlobalIonosphereMapModel(earth, fileName);
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_EPOCH_IN_IONEX_HEADER, oe.getSpecifier());
        }

    }

}
