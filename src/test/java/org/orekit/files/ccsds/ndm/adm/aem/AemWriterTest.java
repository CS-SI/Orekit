/* Copyright 2002-2024 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.ndm.AbstractWriterTest;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class AemWriterTest extends AbstractWriterTest<AdmHeader, AemSegment, Aem> {

    protected AemParser getParser() {
        return new ParserBuilder().
               withParsedUnitsBehavior(ParsedUnitsBehavior.STRICT_COMPLIANCE).
               buildAemParser();
    }

    protected AemWriter getWriter() {
        return new WriterBuilder().buildAemWriter();
    }

    @Test
    public void testWriteExample01() {
        doTest("/ccsds/adm/aem/AEMExample01.txt");
    }

    @Test
    public void testWriteExample02() {
        doTest("/ccsds/adm/aem/AEMExample02.txt");
    }

    @Test
    public void testWriteKvnExample03() {
        doTest("/ccsds/adm/aem/AEMExample03.txt");
    }

    @Test
    public void testWriteXmlExample03() {
        doTest("/ccsds/adm/aem/AEMExample03.xml");
    }

    @Test
    public void testWriteExample04() {
        doTest("/ccsds/adm/aem/AEMExample04.txt");
    }

    @Test
    public void testWriteExample05() {
        doTest("/ccsds/adm/aem/AEMExample05.txt");
    }

    @Test
    public void testWriteExample06a() {
        doTest("/ccsds/adm/aem/AEMExample06a.txt");
    }

    @Test
    public void testWriteExample06b() {
        doTest("/ccsds/adm/aem/AEMExample06b.txt");
    }

    @Test
    public void testWriteExample07() {
        doTest("/ccsds/adm/aem/AEMExample07.txt");
    }

    @Test
    public void testWriteExample08() {
        doTest("/ccsds/adm/aem/AEMExample08.txt");
    }

    @Test
    public void testWriteExample09() {
        doTest("/ccsds/adm/aem/AEMExample09.txt");
    }

    @Test
    public void testWriteExample10() {
        doTest("/ccsds/adm/aem/AEMExample10.txt");
    }

    @Test
    public void testWriteExample11() {
        doTest("/ccsds/adm/aem/AEMExample11.xml");
    }

    @Test
    public void testWriteExample12() {
        doTest("/ccsds/adm/aem/AEMExample12.txt");
    }

    @Test
    public void testWriteExample13() {
        doTest("/ccsds/adm/aem/AEMExample13.xml");
    }

    @Test
    public void testWriteExample14() {
        doTest("/ccsds/adm/aem/AEMExample14.txt");
    }

    @Test
    public void testWriteExample15() {
        doTest("/ccsds/adm/aem/AEMExample15.txt");
    }

    @Test
    public void testWriteExample16() {
        doTest("/ccsds/adm/aem/AEMExample16.txt");
    }

    @Test
    public void testWriteExample17() {
        doTest("/ccsds/adm/aem/AEMExample17.txt");
    }

    @Test
    public void testIssue1453() throws IOException {
        // GIVEN
        // Load orekit data
        Utils.setDataRoot("regular-data");

        // Create writer
        final AemWriter writer = new AemWriter(IERSConventions.IERS_2010, DataContext.getDefault(), null);

        // Create mock xml generator
        final XmlGenerator mockXmlGenerator = mock(XmlGenerator.class);

        // Create mock epoch
        final AbsoluteDate mockAbsoluteDate = mock(AbsoluteDate.class);

        // Create fake data
        final String[] data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8"};

        // WHEN & THEN
        // Write quaternion method
        writer.writeQuaternion(mockXmlGenerator, 1, true, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[0], Unit.ONE, false);
        reset(mockXmlGenerator);

        writer.writeQuaternion(mockXmlGenerator, 1, false, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[3], Unit.ONE, false);
        reset(mockXmlGenerator);

        // Write quaternion derivatives method
        writer.writeQuaternionDerivative(mockXmlGenerator, 1, true, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[0], Unit.ONE, true);
        verify(mockXmlGenerator).enterSection(AttitudeEntryKey.quaternionRate.name());
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC_DOT.name(), data[4], Units.ONE_PER_S, true);
        reset(mockXmlGenerator);

        writer.writeQuaternionDerivative(mockXmlGenerator, 1, false, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[3], Unit.ONE, true);
        verify(mockXmlGenerator).enterSection(AttitudeEntryKey.quaternionRate.name());
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC_DOT.name(), data[7], Units.ONE_PER_S, true);
        reset(mockXmlGenerator);

        // Write quaternion euler rates method
        writer.writeQuaternionEulerRates(mockXmlGenerator, true, RotationOrder.XYZ, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[0], Unit.ONE, true);
        reset(mockXmlGenerator);

        writer.writeQuaternionEulerRates(mockXmlGenerator, false, RotationOrder.XYZ, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.QC.name(), data[3], Unit.ONE, true);
        reset(mockXmlGenerator);

        // Write euler angle method
        writer.writeEulerAngle(mockXmlGenerator, 2, RotationOrder.XYZ, mockAbsoluteDate, data);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.ANGLE_1.name(), data[0], Unit.DEGREE, true);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.ANGLE_2.name(), data[1], Unit.DEGREE, true);
        verify(mockXmlGenerator).writeEntry(AttitudeEntryKey.ANGLE_3.name(), data[2], Unit.DEGREE, true);
    }
}
