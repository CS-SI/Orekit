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
package org.orekit.models.earth.ionosphere.nequick;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Reference values for the tests are from : "European Union (2016). European GNSS (Galileo)
 * Open Service-Ionospheric Correction Algorithm for Galileo Single Frequency Users. 1.2."
 */
public class NeQuickGalileoTest {

    private double[] medium;
    private double[] high;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        high = new double[] {
            236.831641, -0.39362878, 0.00402826613
        };
        medium = new double[] {
            121.129893, 0.351254133, 0.0134635348
        };

    }

    @Test
    public void testHighSolarActivityGalileo() {

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(high);

        // Getters
        Assertions.assertEquals(236.831641,    model.getAlpha()[0], 1.0e-6);
        Assertions.assertEquals(-0.39362878,   model.getAlpha()[1], 1.0e-8);
        Assertions.assertEquals(0.00402826613, model.getAlpha()[2], 1.0e-11);
        Assertions.assertEquals("UTC", model.getUtc().getName());

        // Geodetic points
        final GeodeticPoint recP = new GeodeticPoint(FastMath.toRadians(82.49), FastMath.toRadians(297.66), 78.11);
        final GeodeticPoint satP = new GeodeticPoint(FastMath.toRadians(54.29), FastMath.toRadians(8.23), 20281546.18);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2018, 4, 2, 0, 0, 0, TimeScalesFactory.getUTC());

        // STEC
        final double stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.319, stec, 1.0e-3);
    }

    @Test
    public void testFieldHighSolarActivityGalileo() {
        doTestFieldHighSolarActivityGalileo(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldHighSolarActivityGalileo(final Field<T> field) {

        // Zero
        final T zero = field.getZero();

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(high);

        // Geodetic points
        final FieldGeodeticPoint<T> recP = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(82.49)),
                                                                    zero.newInstance(FastMath.toRadians(297.66)),
                                                                    zero.newInstance(78.11));
        final FieldGeodeticPoint<T> satP = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(54.29)),
                                                                    zero.newInstance(FastMath.toRadians(8.23)),
                                                                    zero.newInstance(20281546.18));

        // Date
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 4, 2, 0, 0, 0, TimeScalesFactory.getUTC());

        // STEC
        final T stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.319, stec.getReal(), 1.0e-3);
    }

    @Test
    public void testMediumSolarActivityGalileo() {

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Geodetic points
        final GeodeticPoint recP = new GeodeticPoint(FastMath.toRadians(-31.80), FastMath.toRadians(115.89), 12.78);
        final GeodeticPoint satP = new GeodeticPoint(FastMath.toRadians(-14.31), FastMath.toRadians(124.09), 20100697.90);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2018, 1, 2, 16, 0, 0, TimeScalesFactory.getUTC());

        // STEC
        final double stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.481, stec, 1.0e-3);
    }

    @Test
    public void testFieldMediumSolarActivityGalileo() {
        doTestFieldMediumSolarActivityGalileo(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldMediumSolarActivityGalileo(final Field<T> field) {

        // Zero
        final T zero = field.getZero();

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Geodetic points
        final FieldGeodeticPoint<T> recP = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(-31.80)),
                                                                    zero.newInstance(FastMath.toRadians(115.89)),
                                                                    zero.newInstance(12.78));
        final FieldGeodeticPoint<T> satP = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(-14.31)),
                                                                    zero.newInstance(FastMath.toRadians(124.09)),
                                                                    zero.newInstance(20100697.90));

        // Date
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 1, 2, 16, 0, 0, TimeScalesFactory.getUTC());

        // STEC
        final T stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.481, stec.getReal(), 1.0e-3);
    }

    @Test
    public void testDelay() {

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Geodetic points
        final GeodeticPoint recP = new GeodeticPoint(FastMath.toRadians(-31.80), FastMath.toRadians(115.89), 12.78);
        final GeodeticPoint satP = new GeodeticPoint(FastMath.toRadians(-14.31), FastMath.toRadians(124.09), 20100697.90);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2018, 4, 2, 16, 0, 0, TimeScalesFactory.getUTC());

        // Earth
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Satellite position
        final Vector3D satPosInITRF    = ellipsoid.transform(satP);
        final Vector3D satPosInEME2000 = ellipsoid.getBodyFrame().getStaticTransformTo(FramesFactory.getEME2000(), date).transformPosition(satPosInITRF);

        // Spacecraft state
        final PVCoordinates   pv      = new PVCoordinates(satPosInEME2000, new Vector3D(1.0, 1.0, 1.0));
        final Orbit           orbit   = new CartesianOrbit(pv, FramesFactory.getEME2000(), date, Constants.WGS84_EARTH_MU);
        final SpacecraftState state   = new SpacecraftState(orbit);

        final double delay = model.pathDelay(state, new TopocentricFrame(ellipsoid, recP, null),
                                             PredefinedGnssSignal.G01.getFrequency(), model.getParameters());
       
        // Verify
        Assertions.assertEquals(1.13, delay, 0.01);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelay(final Field<T> field) {

        // Zero and One
        final T zero = field.getZero();
        final T one  = field.getOne();

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Geodetic points
        final double recLat = FastMath.toRadians(-31.80);
        final double recLon = FastMath.toRadians(115.89);
        final double recAlt = 12.78;
        final GeodeticPoint         recP = new GeodeticPoint(recLat, recLon, recAlt);
        final FieldGeodeticPoint<T> satP = new FieldGeodeticPoint<>(zero.newInstance(FastMath.toRadians(-14.31)),
                        zero.newInstance(FastMath.toRadians(124.09)), zero.newInstance(20100697.90));

        // Date
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2018, 4, 2, 16, 0, 0, TimeScalesFactory.getUTC());

        // Earth
        final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Satellite position
        final FieldVector3D<T> satPosInITRF    = ellipsoid.transform(satP);
        final FieldVector3D<T> satPosInEME2000 = ellipsoid.getBodyFrame().getStaticTransformTo(FramesFactory.getEME2000(), date).transformPosition(satPosInITRF);

        // Spacecraft state
        final FieldPVCoordinates<T>   pv      = new FieldPVCoordinates<>(satPosInEME2000, new FieldVector3D<>(one, one, one));
        final FieldOrbit<T>           orbit   = new FieldCartesianOrbit<>(pv, FramesFactory.getEME2000(), date, zero.newInstance(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> state   = new FieldSpacecraftState<>(orbit);

        final T delay = model.pathDelay(state, new TopocentricFrame(ellipsoid, recP, null),
                                        PredefinedGnssSignal.G01.getFrequency(), model.getParameters(field));
       
        // Verify
        Assertions.assertEquals(1.13, delay.getReal(), 0.01);
    }

    @Test
    public void testAntiMeridian() {

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2018,  11,  2, 16, 0, 0, TimeScalesFactory.getUTC());

        // Geodetic points
        final GeodeticPoint recP = new GeodeticPoint(FastMath.toRadians(-31.80), FastMath.toRadians(-179.99), 12.78);
        final GeodeticPoint satP = new GeodeticPoint(FastMath.toRadians(-14.31), FastMath.toRadians(-177.43), 20100697.90);
        double stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.471, stec, 0.001);

    }

    @Test
    public void testFieldAntiMeridian() {
        doTestFieldAntiMeridian(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldAntiMeridian(final Field<T> field) {

        final T zero = field.getZero();

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Date
        final FieldAbsoluteDate<T> date =
                        new FieldAbsoluteDate<>(field,
                                                new AbsoluteDate(2018,  11,  2, 16, 0, 0, TimeScalesFactory.getUTC()));

        // Geodetic points
        final FieldGeodeticPoint<T> recP = new FieldGeodeticPoint<>(FastMath.toRadians(zero.newInstance(-31.80)),
                                                                    FastMath.toRadians(zero.newInstance(-179.99)),
                                                                    zero.newInstance(12.78));
        final FieldGeodeticPoint<T> satP = new FieldGeodeticPoint<>(FastMath.toRadians(zero.newInstance(-14.31)),
                                                                    FastMath.toRadians(zero.newInstance(-177.43)),
                                                                    zero.newInstance(20100697.90));
        T stec = model.stec(date, recP, satP);
        Assertions.assertEquals(20.471, stec.getReal(), 0.001);

    }

    @Test
    public void testZenith() {

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2018,  4,  2, 16, 0, 0, TimeScalesFactory.getUTC());

        // Geodetic points
        final GeodeticPoint recP = new GeodeticPoint(FastMath.toRadians(51.678), FastMath.toRadians(-9.448), 0.0);
        final GeodeticPoint satP = new GeodeticPoint(FastMath.toRadians(51.678), FastMath.toRadians(-9.448), 600000.0);
        double stec = model.stec(date, recP, satP);
        Assertions.assertEquals(26.346, stec, 0.001);

    }

    @Test
    public void testFieldZenith() {
        doTestFieldZenith(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldZenith(final Field<T> field) {

        final T zero = field.getZero();

        // Model
        final NeQuickGalileo model = new NeQuickGalileo(medium);

        // Date
        final FieldAbsoluteDate<T> date =
                new FieldAbsoluteDate<>(field,
                                        new AbsoluteDate(2018,  4,  2, 16, 0, 0, TimeScalesFactory.getUTC()));

        // Geodetic points
        final FieldGeodeticPoint<T> recP = new FieldGeodeticPoint<>(FastMath.toRadians(zero.newInstance(51.678)),
                                                                    FastMath.toRadians(zero.newInstance(-9.448)),
                                                                    zero.newInstance(0.0));
        final FieldGeodeticPoint<T> satP = new FieldGeodeticPoint<>(FastMath.toRadians(zero.newInstance(51.678)),
                                                                    FastMath.toRadians(zero.newInstance(-9.448)),
                                                                    zero.newInstance(600000.0));
        T stec = model.stec(date, recP, satP);
        Assertions.assertEquals(26.346, stec.getReal(), 0.001);

    }

}
