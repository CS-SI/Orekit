/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.attitude.GenericGNSS;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.KeplerianParameters;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class GNSSExtendedPositionProviderTest {

    private GNSSPropagator<GalileoNavigationMessage> propagator;

    @DefaultDataContext
    @BeforeEach
    void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        final DataContext context = DataContext.getDefault();
        final GNSSDate date = new GNSSDate(1024, 293400.0,SatelliteSystem.GALILEO );
        final double sqrtA = 5440.602949142456;
        final KeplerianOrbit orbit = new KeplerianOrbit(new KeplerianParameters(sqrtA * sqrtA,
                                                                                2.4088891223073006E-4,
                                                                                0.9531656087278083,
                                                                                -1.6870064194345724,
                                                                                -0.36639513583951266,
                                                                                -0.38716557650888,
                                                                                PositionAngleType.MEAN),
                                                        context.getFrames().getGCRF(),
                                                        date.getDate(), GNSSConstants.GALILEO_MU);
        final GalileoNavigationMessage goe =
            new GalileoNavigationMessage(context.getTimeScales(), GalileoNavigationMessage.FNAV,
                                         4, date, orbit,
                                         0.0, 3.7394414770330066E-9, 0.0,
                                         -2.36081262303612E-10, -5.7695260382035525E-9,
                                         -8.903443813323975E-7, 6.61797821521759E-6,
                                         194.0625, -18.78125,
                                         3.166496753692627E-8, -1.862645149230957E-8,
                                         0.0, 0.0, 0.0, 0.0,
                                         new GNSSDate(1024, 0.0, SatelliteSystem.GALILEO).getDate(),
                                         0.0, 0, 0, 0.0, 0.0, 0, 0.0);
        propagator = new GNSSPropagator<>(goe, context.getFrames().getGCRF(),
                                          context.getFrames().getFrame(Predefined.GTOD_CONVENTIONS_1996_ACCURATE_EOP),
                                          new GenericGNSS(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                          context.getCelestialBodies().getSun(),
                                                          context.getFrames().getGCRF()),
                                          Propagator.DEFAULT_MASS);
    }

    @Test
    void testGetPosition() {
        // GIVEN
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D position = positionProvider.getPosition(date, frame);
        // THEN
        assertArrayEquals(propagator.getPosition(date, frame).toArray(), position.toArray(), 1e-6);
    }

    @Test
    void testGetVelocity() {
        // GIVEN
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final Vector3D velocity = positionProvider.getVelocity(date, frame);
        // THEN
        assertArrayEquals(propagator.getVelocity(date, frame).toArray(), velocity.toArray(), 1e-6);
    }

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final Frame frame = FramesFactory.getGTOD(false);
        // WHEN
        final TimeStampedPVCoordinates actualPV = positionProvider.getPVCoordinates(date, frame);
        // THEN
        final TimeStampedPVCoordinates expectedPV = propagator.getPVCoordinates(date, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Test
    void testFieldGetPosition() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> position = positionProvider.getPosition(fieldDate, frame);
        // THEN

        final FieldGnssPropagator<Binary64, GalileoNavigationMessage> fieldPropagator =
            new FieldGnssPropagator<>(propagator.getOrbitalElements().toField(field),
                                      propagator.getECI(), propagator.getECEF(),
                                      propagator.getAttitudeProvider(),
                                      new Binary64(propagator.getInitialState().getMass()));
        final FieldVector3D<Binary64> expectedPosition = fieldPropagator.getPosition(fieldDate, frame);
        assertEquals(expectedPosition.getX().getReal(), position.getX().getReal(), 1e-6);
        assertEquals(expectedPosition.getY().getReal(), position.getY().getReal(), 1e-6);
        assertEquals(expectedPosition.getZ().getReal(), position.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetVelocity() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final FieldVector3D<Binary64> velocity = positionProvider.getVelocity(fieldDate, frame);
        // THEN
        final FieldGnssPropagator<Binary64, GalileoNavigationMessage> fieldGnssPropagator =
            new FieldGnssPropagator<>(propagator.getOrbitalElements().toField(field),
                                      propagator.getECI(), propagator.getECEF(),
                                      propagator.getAttitudeProvider(),
                                      new Binary64(propagator.getInitialState().getMass()));
        final FieldVector3D<Binary64> expectedVelocity = fieldGnssPropagator.getVelocity(fieldDate, frame);
        assertEquals(expectedVelocity.getX().getReal(), velocity.getX().getReal(), 1e-6);
        assertEquals(expectedVelocity.getY().getReal(), velocity.getY().getReal(), 1e-6);
        assertEquals(expectedVelocity.getZ().getReal(), velocity.getZ().getReal(), 1e-6);
    }

    @Test
    void testFieldGetPVCoordinates() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final GNSSExtendedPositionProvider<GalileoNavigationMessage> positionProvider =
            new GNSSExtendedPositionProvider<>(propagator.getOrbitalElements(),
                                               propagator.getECI(), propagator.getECEF(),
                                               propagator.getAttitudeProvider(),
                                               propagator.getMass(AbsoluteDate.ARBITRARY_EPOCH));
        final AbsoluteDate date = propagator.getInitialState().getDate().shiftedBy(1e4);
        final FieldAbsoluteDate<Binary64> fieldDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final Frame frame = FramesFactory.getEME2000();
        // WHEN
        final TimeStampedFieldPVCoordinates<Binary64> actualPV = positionProvider.getPVCoordinates(fieldDate, frame);
        // THEN
        final FieldGnssPropagator<Binary64, GalileoNavigationMessage> fieldPropagator =
            new FieldGnssPropagator<>(propagator.getOrbitalElements().toField(field),
                                      propagator.getECI(), propagator.getECEF(),
                                      propagator.getAttitudeProvider(),
                                      new Binary64(propagator.getInitialState().getMass()));
        final TimeStampedFieldPVCoordinates<Binary64> expectedPV = fieldPropagator.getPVCoordinates(fieldDate, frame);
        assertEquals(expectedPV.getDate(), actualPV.getDate());
        assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }
}
