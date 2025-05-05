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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.adm.aem.Aem;
import org.orekit.files.ccsds.ndm.adm.aem.AemSatelliteEphemeris;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregateBoundedAttitudeProviderTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:ccsds");
    }

    @Test
    void testEmptyList() {
        try {
            new AggregateBoundedAttitudeProvider(Collections.emptyList());
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_ATTITUDE_PROVIDERS, oe.getSpecifier());
        }
    }

    @Test
    @DefaultDataContext
    void testAEM() {

        final String ex = "/ccsds/adm/aem/AEMExample10.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // Verify dates
        Assertions.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getStart()), 1.0e-10);
        Assertions.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getStop()),  1.0e-10);
        Assertions.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getSegments().get(0).getStart()), 1.0e-10);
        Assertions.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getSegments().get(1).getStop()), 1.0e-10);

        // Verify computation with data in first segment
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:04.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assertions.assertEquals(0.45652, rotation.getQ0(), 0.00001);
        Assertions.assertEquals(-0.84532, rotation.getQ1(), 0.00001);
        Assertions.assertEquals(0.26974, rotation.getQ2(), 0.00001);
        Assertions.assertEquals(-0.06532, rotation.getQ3(), 0.00001);

    }

    @Test
    @DefaultDataContext
    void testFieldAEM() {
        doTestFieldAEM(Binary64Field.getInstance());
    }

    @DefaultDataContext
    private <T extends CalculusFieldElement<T>> void doTestFieldAEM(final Field<T> field) {

        final String ex = "/ccsds/adm/aem/AEMExample10.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().buildAemParser().parseMessage(source);

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // Verify dates
        Assertions.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getStart()), 1.0e-10);
        Assertions.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getStop()),  1.0e-10);
        Assertions.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getSegments().get(0).getStart()), 1.0e-10);
        Assertions.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getSegments().get(1).getStop()), 1.0e-10);

        // Verify computation with data in first segment
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                new AbsoluteDate("1996-11-28T22:08:04.555", TimeScalesFactory.getUTC()));
        FieldAttitude<T> attitude = provider.getAttitude(null, date, null);
        FieldRotation<T> rotation = attitude.getRotation();
        Assertions.assertEquals(0.45652, rotation.getQ0().getReal(), 0.00001);
        Assertions.assertEquals(-0.84532, rotation.getQ1().getReal(), 0.00001);
        Assertions.assertEquals(0.26974, rotation.getQ2().getReal(), 0.00001);
        Assertions.assertEquals(-0.06532, rotation.getQ3().getReal(), 0.00001);

        // Verify getAttitudeRotation
        FieldRotation<T> rotation2 = provider.getAttitudeRotation(null, date, null);
        Assertions.assertEquals(0., Rotation.distance(rotation.toRotation(), rotation2.toRotation()));
    }

    @Test
    @DefaultDataContext
    void testOutsideBounds() {

        final String ex = "/ccsds/adm/aem/AEMExample10.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().withSimpleEOP(true).buildAemParser().parseMessage(source);

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // before bound of first attitude provider
        try {
            provider.getAttitude(null, provider.getMinDate().shiftedBy(-60.0), null);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, oe.getSpecifier());
        }

        // after bound of last attitude provider
        try {
            provider.getAttitude(null, provider.getMaxDate().shiftedBy(60.0), null);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, oe.getSpecifier());
        }

    }

    @Test
    @DefaultDataContext
    void testFieldOutsideBounds() {
        doTestFieldOutsideBounds(Binary64Field.getInstance());
    }

    @DefaultDataContext
    private <T extends CalculusFieldElement<T>> void doTestFieldOutsideBounds(final Field<T> field) {

        final String ex = "/ccsds/adm/aem/AEMExample10.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Aem file = new ParserBuilder().withSimpleEOP(true).buildAemParser().parseMessage(source);

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // before bound of first attitude provider
        try {
            provider.getAttitude(null, new FieldAbsoluteDate<>(provider.getMinDate(), field.getZero().subtract(60.0)), null);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, oe.getSpecifier());
        }

        // after bound of last attitude provider
        try {
            provider.getAttitude(null, new FieldAbsoluteDate<>(provider.getMinDate(), field.getZero().add(60.0)), null);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, oe.getSpecifier());
        }

    }

    @Test
    void testGetEventDetectorsList() {
        // GIVEN
        final TestBoundedAttitudeProvider boundedAttitudeProvider = new TestBoundedAttitudeProvider();
        final List<BoundedAttitudeProvider> boundedAttitudeProviderList = new ArrayList<>();
        boundedAttitudeProviderList.add(boundedAttitudeProvider);
        final AggregateBoundedAttitudeProvider aggregateBoundedAttitudeProvider = new AggregateBoundedAttitudeProvider(
                boundedAttitudeProviderList);
        // WHEN & THEN
        final int expectedSize = 0;
        Assertions.assertEquals(expectedSize, aggregateBoundedAttitudeProvider.getEventDetectors(new ArrayList<>()).count());
        Assertions.assertEquals(expectedSize, aggregateBoundedAttitudeProvider.getFieldEventDetectors(Binary64Field.getInstance(),
                new ArrayList<>()).count());
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final TestBoundedAttitudeProvider boundedAttitudeProvider = new TestBoundedAttitudeProvider();
        final List<BoundedAttitudeProvider> boundedAttitudeProviderList = new ArrayList<>();
        boundedAttitudeProviderList.add(boundedAttitudeProvider);
        final AggregateBoundedAttitudeProvider aggregateBoundedAttitudeProvider = new AggregateBoundedAttitudeProvider(
                boundedAttitudeProviderList);
        // WHEN
        final List<ParameterDriver> parameterDrivers = aggregateBoundedAttitudeProvider.getParametersDrivers();
        // THEN
        Assertions.assertEquals(boundedAttitudeProvider.getParametersDrivers().size(), parameterDrivers.size());
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final TestBoundedAttitudeProvider boundedAttitudeProvider = new TestBoundedAttitudeProvider();
        final List<BoundedAttitudeProvider> boundedAttitudeProviderList = new ArrayList<>();
        boundedAttitudeProviderList.add(boundedAttitudeProvider);
        final AggregateBoundedAttitudeProvider aggregateBoundedAttitudeProvider = new AggregateBoundedAttitudeProvider(
                boundedAttitudeProviderList);
        // WHEN
        final Stream<EventDetector> eventDetectorStream = aggregateBoundedAttitudeProvider.getEventDetectors();
        // THEN
        final List<EventDetector> eventDetectorList = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(1, eventDetectorList.size());
        Assertions.assertInstanceOf(DateDetector.class, eventDetectorList.get(0));
        Assertions.assertInstanceOf(ResetDerivativesOnEvent.class, eventDetectorList.get(0).getHandler());
        final DateDetector dateDetector = (DateDetector) eventDetectorList.get(0);
        Assertions.assertEquals(boundedAttitudeProvider.getMinDate(), dateDetector.getDates().get(0).getDate());
    }

    @Test
    void testGetFieldEventDetectors() {
        // GIVEN
        final TestBoundedAttitudeProvider boundedAttitudeProvider = new TestBoundedAttitudeProvider();
        final List<BoundedAttitudeProvider> boundedAttitudeProviderList = new ArrayList<>();
        boundedAttitudeProviderList.add(boundedAttitudeProvider);
        final AggregateBoundedAttitudeProvider aggregateBoundedAttitudeProvider = new AggregateBoundedAttitudeProvider(
                boundedAttitudeProviderList);
        // WHEN
        final Stream<FieldEventDetector<Binary64>> fieldEventDetectorStream = aggregateBoundedAttitudeProvider
                .getFieldEventDetectors(Binary64Field.getInstance());
        // THEN
        final List<FieldEventDetector<Binary64>> fieldEventDetectorList = fieldEventDetectorStream.collect(Collectors.toList());
        final Stream<EventDetector> eventDetectorStream = aggregateBoundedAttitudeProvider.getEventDetectors();
        final List<EventDetector> eventDetectorList = eventDetectorStream.collect(Collectors.toList());
        Assertions.assertEquals(eventDetectorList.size(), fieldEventDetectorList.size());
        Assertions.assertInstanceOf(FieldDateDetector.class, fieldEventDetectorList.get(0));
        Assertions.assertInstanceOf(FieldResetDerivativesOnEvent.class, fieldEventDetectorList.get(0).getHandler());
    }

    @Test
    void testGetAttitudeRotation() {
        // GIVEN
        final TestBoundedAttitudeProvider boundedAttitudeProvider = new TestBoundedAttitudeProvider();
        final List<BoundedAttitudeProvider> boundedAttitudeProviderList = new ArrayList<>();
        boundedAttitudeProviderList.add(boundedAttitudeProvider);
        final AggregateBoundedAttitudeProvider aggregateBoundedAttitudeProvider = new AggregateBoundedAttitudeProvider(
                boundedAttitudeProviderList);
        final PVCoordinatesProvider mockPvCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final Frame mockFrame = Mockito.mock(Frame.class);
        final AbsoluteDate minDate = boundedAttitudeProvider.getMinDate();
        // WHEN
        final Rotation actualRotation = aggregateBoundedAttitudeProvider.getAttitudeRotation(mockPvCoordinatesProvider,
                minDate, mockFrame);
        // THEN
        final Rotation expectedRotation = aggregateBoundedAttitudeProvider.getAttitude(mockPvCoordinatesProvider,
                minDate, mockFrame).getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    private static class TestBoundedAttitudeProvider implements BoundedAttitudeProvider {

        TestBoundedAttitudeProvider() {
            // nothing to do
        }

        @Override
        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new Attitude(date, frame, new AngularCoordinates());
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                                                                                FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldAttitude<>(date.getField(), new Attitude(date.toAbsoluteDate(), frame,
                    new AngularCoordinates()));
        }

        @Override
        public AbsoluteDate getMinDate() {
            return AbsoluteDate.ARBITRARY_EPOCH;
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return getMinDate().shiftedBy(100000.);
        }

    }

}
