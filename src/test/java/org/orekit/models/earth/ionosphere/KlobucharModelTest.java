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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class KlobucharModelTest {

    private static double epsilon = 1e-6;

    /** ionospheric model. */
    private KlobucharIonoModel model;

    private UTCScale utc;

    @BeforeEach
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06, 0},
                                       new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});

        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    @AfterEach
    public void tearDown() {
        utc = null;
    }

    @Test
    public void testDelay() {
        final double latitude = FastMath.toRadians(45);
        final double longitude = FastMath.toRadians(2);
        final double altitude = 500;
        final double elevation = 70.;
        final double azimuth = 10.;

        final AbsoluteDate date = new AbsoluteDate();

        final GeodeticPoint geo = new GeodeticPoint(latitude, longitude, altitude);

        double delayMeters = model.pathDelay(date, geo,
                                             FastMath.toRadians(elevation),
                                             FastMath.toRadians(azimuth),
                                             1575.42e6, model.getParameters(date));

        Assertions.assertTrue(Precision.compareTo(delayMeters, 12., epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(delayMeters, 0., epsilon) > 0);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final T zero = field.getZero();

        final T latitude  = zero.add(FastMath.toRadians(45));
        final T longitude = zero.add(FastMath.toRadians(2));
        final T altitude  = zero.add(500);
        final T elevation = zero.add(FastMath.toRadians(70.));
        final T azimuth   = zero.add(FastMath.toRadians(10.));

        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        final FieldGeodeticPoint<T> geo = new FieldGeodeticPoint<>(latitude, longitude, altitude);

        T delayMeters = model.pathDelay(date, geo,
                                        elevation, azimuth,
                                        1575.42e6, model.getParameters(field, date));

        Assertions.assertTrue(Precision.compareTo(delayMeters.getReal(), 12., epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(delayMeters.getReal(), 0., epsilon) > 0);
    }

    @Test
    public void compareExpectedValue() throws IllegalArgumentException, OrekitException {
        final double latitude = FastMath.toRadians(40);
        final double longitude = FastMath.toRadians(-100);
        final double altitude = 0.;
        final double elevation = 20.;
        final double azimuth = 210.;

        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2000, 1, 1,
                                                                          20, 45, 0),
                                                                          utc);

        final GeodeticPoint geo = new GeodeticPoint(latitude, longitude, altitude);

        final double delayMeters = model.pathDelay(date, geo,
                                                   FastMath.toRadians(elevation),
                                                   FastMath.toRadians(azimuth),
                                                   1575.42e6, model.getParameters(date));

        Assertions.assertEquals(23.784, delayMeters, 0.001);
    }

    @Test
    public <T extends CalculusFieldElement<T>> void compareFieldExpectedValue() {
        doCompareFieldExpectedValue(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doCompareFieldExpectedValue(final Field<T> field)
        throws IllegalArgumentException, OrekitException {
        final T zero = field.getZero();

        final T latitude  = zero.add(FastMath.toRadians(40));
        final T longitude = zero.add(FastMath.toRadians(-100));
        final T altitude  = zero;
        final T elevation = zero.add(FastMath.toRadians(20.));
        final T azimuth   = zero.add(FastMath.toRadians(210.));

        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2000, 1, 1,
                                                                          20, 45, 0),
                                                                          utc);
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<>(field, date);

        final FieldGeodeticPoint<T> geo = new FieldGeodeticPoint<>(latitude, longitude, altitude);

        final T delayMeters = model.pathDelay(fieldDate, geo,
                                              elevation, azimuth,
                                              1575.42e6, model.getParameters(field, fieldDate));

        Assertions.assertEquals(23.784, delayMeters.getReal(), 0.001);
    }

    @Test
    public void testEquality() {
        // Common parameters
        final AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 12, 35, 04.245, TimeScalesFactory.getUTC());

        GeodeticPoint point = new GeodeticPoint(0.389, -2.962, 0);

        // Delay using azimuth/elevation angles
        // Reference values are from EcksteinHechlerPropagatorTest class
        final double azimuth   = 1.70;
        final double elevation = 0.09;

        double delayAzEl = model.pathDelay(date, point, elevation, azimuth, 1575.42e6, model.getParameters(date));

        // Delay using SpacecraftState
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final Frame eci  = FramesFactory.getEME2000();

        // Reference values are from EcksteinHechlerPropagatorTest class
        final TimeStampedPVCoordinates pvaCoordinates = new TimeStampedPVCoordinates(date,
                                                                                     new Vector3D(-6809069.7032156205, 3568730.540549229, 2042703.5290696376),
                                                                                     new Vector3D(-3720.8926276445, -5588.6043568734, -2008.3138559626),
                                                                                     new Vector3D(5.3966003438, -2.8284419922, -1.6223614396));
        final Orbit orbit = new CartesianOrbit(pvaCoordinates, eci, 3.9860047E14);
        final SpacecraftState state = new SpacecraftState(orbit);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING, ecef);
        TopocentricFrame topo = new TopocentricFrame(earth, point, "Gstation");

        double delayState = model.pathDelay(state, topo, 1575.42e6, model.getParameters(date));

        // Verify
        Assertions.assertEquals(delayAzEl, delayState, 1.0e-6);
    }

    @Test
    public <T extends CalculusFieldElement<T>> void testFieldEquality() {
        doTestFieldEquality(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldEquality(final Field<T> field) {
        // Common parameters
        final T zero = field.getZero();
        final AbsoluteDate date = new AbsoluteDate(2000, 1, 1, 12, 35, 04.245, TimeScalesFactory.getUTC());

        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);

        FieldGeodeticPoint<T> point = new FieldGeodeticPoint<>(zero.add(0.389),
                                                               zero.add(-2.962),
                                                               zero);

        // Delay using azimuth/elevation angles
        // Reference values are from EcksteinHechlerPropagatorTest class
        final T azimuth   = zero.add(1.70);
        final T elevation = zero.add(0.09);

        T delayAzEl = model.pathDelay(dateF, point, elevation, azimuth, 1575.42e6, model.getParameters(field, dateF));

        // Delay using SpacecraftState
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final Frame eci  = FramesFactory.getEME2000();

        // Reference values are from EcksteinHechlerPropagatorTest class
        final TimeStampedFieldPVCoordinates<T> pvaCoordinates = new TimeStampedFieldPVCoordinates<>(dateF,
                                                                                     new FieldVector3D<>(zero.add(-6809069.7032156205), zero.add(3568730.540549229), zero.add(2042703.5290696376)),
                                                                                     new FieldVector3D<>(zero.add(-3720.8926276445), zero.add(-5588.6043568734), zero.add(-2008.3138559626)),
                                                                                     new FieldVector3D<>(zero.add(5.3966003438), zero.add(-2.8284419922), zero.add(-1.6223614396)));
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(pvaCoordinates, eci, zero.add(3.9860047E14));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);
        BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                               Constants.WGS84_EARTH_FLATTENING, ecef);
        TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(0.389, -2.962, 0), "Gstation");

        T delayState = model.pathDelay(state, topo, 1575.42e6, model.getParameters(field, dateF));

        // Verify
        Assertions.assertEquals(delayAzEl.getReal(), delayState.getReal(), 1.0e-6);
    }

}


