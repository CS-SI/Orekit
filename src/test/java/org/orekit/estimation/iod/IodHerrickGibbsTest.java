/* Copyright 2022-2025 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

class IodHerrickGibbsTest {

    @Test
    public void testCompareWithVallado() {

        // Test extracted from Vallado D., Fundamentals of astrodynamics & applications, 4rd Edition, Example 7-4, p467.

        // Initialisation
        final IodHerrickGibbs gibbs = new IodHerrickGibbs(398600.4418);

        // Observable satellite to initialize measurements
        final ObservableSatellite satellite = new ObservableSatellite(0);

        // Observations vector (EME2000)
        final Vector3D r1 = new Vector3D(3419.85564, 6019.82602, 2784.60022);
        final Vector3D r2 = new Vector3D(2935.91195, 6326.18324, 2660.59584);
        final Vector3D r3 = new Vector3D(2434.95202, 6597.38674, 2521.52311);

        // Epoch corresponding to the observation vector
        AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        AbsoluteDate t2 = t1.shiftedBy(76.48);
        AbsoluteDate t3 = t1.shiftedBy(153.04);

        // Reference result (from Vallado's example)
        //
        // IMPORTANT: Reference results in the 4th Edition of the book are not correct! The reference below comes from
        // Vallado's validation test available in its Github repository (see test_hgibbs()):
        // https://github.com/CelesTrak/fundamentals-of-astrodynamics/blob/main/software/python/tests/astro/iod/test_utils.py
        final Vector3D referenceV2 = new Vector3D(-6.441557227511062, 3.777559606719521, -1.7205675602414345);

        // Herrick-Gibbs IOD
        final Orbit orbit = gibbs.estimate(FramesFactory.getEME2000(),
                                           new Position(t1, r1, 1.0, 1.0, satellite),
                                           new Position(t2, r2, 1.0, 1.0, satellite),
                                           new Position(t3, r3, 1.0, 1.0, satellite));

        // Verify
        Assertions.assertEquals(0.0, orbit.durationFrom(t2));
        Assertions.assertEquals(r2.getNorm(), orbit.getPosition().getNorm(), 1.0e-10);
        Assertions.assertEquals(referenceV2.getNorm(), orbit.getPVCoordinates().getVelocity().getNorm(),  1.0e-10);

    }

    @Test
    public void testCompareWithValladoField() {
        doTestCompareWithValladoField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestCompareWithValladoField(Field<T> field) {

        // Test extracted from Vallado D., Fundamentals of astrodynamics & applications, 4rd Edition, Example 7-4, p467.

        // Initialisation
        final IodHerrickGibbs gibbs = new IodHerrickGibbs(398600.4418);

        // Observations vector (EME2000)
        final FieldVector3D<T> r1 = new FieldVector3D<>(field, new Vector3D(3419.85564, 6019.82602, 2784.60022));
        final FieldVector3D<T> r2 = new FieldVector3D<>(field, new Vector3D(2935.91195, 6326.18324, 2660.59584));
        final FieldVector3D<T> r3 = new FieldVector3D<>(field, new Vector3D(2434.95202, 6597.38674, 2521.52311));

        // Epoch corresponding to the observation vector
        FieldAbsoluteDate<T> t1 = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldAbsoluteDate<T> t2 = t1.shiftedBy(76.48);
        FieldAbsoluteDate<T> t3 = t1.shiftedBy(153.04);

        // Reference result (from Vallado's example)
        //
        // IMPORTANT: Reference results in the 4th Edition of the book are not correct! The reference below comes from
        // Vallado's validation test available in its Github repository (see test_hgibbs()):
        // https://github.com/CelesTrak/fundamentals-of-astrodynamics/blob/main/software/python/tests/astro/iod/test_utils.py
        final Vector3D referenceV2 = new Vector3D(-6.441557227511062, 3.777559606719521, -1.7205675602414345);

        // Herrick-Gibbs IOD
        final FieldOrbit<T> orbit = gibbs.estimate(FramesFactory.getEME2000(), r1, t1, r2, t2, r3, t3);

        // Verify
        Assertions.assertEquals(0.0, orbit.durationFrom(t2).getReal());
        Assertions.assertEquals(r2.getNorm().getReal(), orbit.getPosition().getNorm().getReal(), 1.0e-10);
        Assertions.assertEquals(referenceV2.getNorm(), orbit.getPVCoordinates().getVelocity().getNorm().getReal(),  1.0e-10);

    }

    @Test
    public void testNonDifferentDatesForObservations() {
        // Initialization
        final AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, TimeScalesFactory.getUTC());
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final PV pv1 = new PV(date, Vector3D.PLUS_I, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv2 = new PV(date.shiftedBy(1.0), Vector3D.PLUS_J, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv3 = new PV(date, Vector3D.PLUS_K, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        // Action
        try {
            new IodHerrickGibbs(Constants.WGS84_EARTH_MU).estimate(FramesFactory.getEME2000(), pv1, pv2, pv3);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_DIFFERENT_DATES_FOR_OBSERVATIONS, oe.getSpecifier());
        }
    }

    @Test
    public void testNonDifferentDatesForObservationsField() {
        doTestNonDifferentDatesForObservationsField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNonDifferentDatesForObservationsField(Field<T> field) {
        // Initialization
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        // Action
        try {
            new IodHerrickGibbs(Constants.WGS84_EARTH_MU).estimate(FramesFactory.getEME2000(), FieldVector3D.getPlusI(field), date,
                                                                   FieldVector3D.getPlusJ(field), date,
                                                                   FieldVector3D.getPlusK(field), date);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_DIFFERENT_DATES_FOR_OBSERVATIONS, oe.getSpecifier());
        }
    }

    @Test
    public void testNonCoplanarPoints() {
        // Initialization
        final AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 11, 35, 47.0, TimeScalesFactory.getUTC());
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final PV pv1 = new PV(date, Vector3D.PLUS_I, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv2 = new PV(date.shiftedBy(1.0), Vector3D.PLUS_J, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        final PV pv3 = new PV(date.shiftedBy(2.0), Vector3D.PLUS_K, Vector3D.ZERO, 1.0, 1.0, 1.0, satellite);
        // Action
        try {
            new IodHerrickGibbs(Constants.WGS84_EARTH_MU).estimate(FramesFactory.getEME2000(), pv1, pv2, pv3);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_COPLANAR_POINTS, oe.getSpecifier());
        }
    }

    @Test
    public void testNonCoplanarPointsField() {
        doTestNonCoplanarPointsField(Binary64Field.getInstance());
    }

    private  <T extends CalculusFieldElement<T>> void doTestNonCoplanarPointsField(Field<T> field) {
        // Initialization
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);
        // Action
        try {
            new IodHerrickGibbs(Constants.WGS84_EARTH_MU).estimate(FramesFactory.getEME2000(), FieldVector3D.getPlusI(field), date,
                                                                   FieldVector3D.getPlusJ(field), date.shiftedBy(1.0),
                                                                   FieldVector3D.getPlusK(field), date.shiftedBy(2.0));
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_COPLANAR_POINTS, oe.getSpecifier());
        }
    }

    @BeforeAll
    public static void init() {
        Utils.setDataRoot("regular-data:potential:tides");
    }
}