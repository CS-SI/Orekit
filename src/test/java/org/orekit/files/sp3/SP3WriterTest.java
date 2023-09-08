/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.time.TimeScalesFactory;

public class SP3WriterTest {

    @Test
    public void testRoundtripExampleA1() {
        doTestRoundtrip("/sp3/example-a-1.sp3");
    }

    @Test
    public void testRoundtripExampleA2() {
        doTestRoundtrip("/sp3/example-a-2.sp3");
    }

    @Test
    public void testRoundtripExampleC1() {
        doTestRoundtrip("/sp3/example-c-1.sp3");
    }

    @Test
    public void testRoundtripExampleC2() {
        doTestRoundtrip("/sp3/example-c-2.sp3");
    }

    @Test
    public void testRoundtripExampleD1() {
        doTestRoundtrip("/sp3/example-d-1.sp3");
    }

    @Test
    public void testRoundtripExampleD2() {
        doTestRoundtrip("/sp3/example-d-2.sp3");
    }

    @Test
    public void testRoundtripExampleD3() {
        doTestRoundtrip("/sp3/example-d-3.sp3");
    }

    @Test
    public void testRoundtripExampleD4() {
        doTestRoundtrip("/sp3/example-d-4.sp3");
    }

    @Test
    public void testRoundtripEsaBHN() {
        doTestRoundtrip("/sp3/esaBHN.sp3.Z");
    }

    @Test
    public void testRoundtripEsaPRO() {
        doTestRoundtrip("/sp3/esaPRO.sp3.Z");
    }

    @Test
    public void testRoundtripGbm18432() {
        doTestRoundtrip("/sp3/gbm18432.sp3.Z");
    }

    @Test
    public void testRoundtripGbm19500AfterDrop() {
        doTestRoundtrip("/sp3/gbm19500_after_drop.sp3");
    }

    @Test
    public void testRoundtripGbm19500AfterNoDrop() {
        doTestRoundtrip("/sp3/gbm19500_after_no_drop.sp3");
    }

    @Test
    public void testRoundtripGbm19500LargeGap() {
        doTestRoundtrip("/sp3/gbm19500_large_gap.sp3");
    }

    @Test
    public void testRoundtripIssue895Clock() {
        doTestRoundtrip("/sp3/issue895-clock-record.sp3");
    }

    @Test
    public void testRoundtripIssue895HEaderComment() {
        doTestRoundtrip("/sp3/issue895-header-comment.sp3");
    }

    @Test
    public void testRoundtripIssue895HoursIncrement() {
        doTestRoundtrip("/sp3/issue895-hours-increment.sp3");
    }

    @Test
    public void testRoundtripIssue895MinutesIncrement() {
        doTestRoundtrip("/sp3/issue895-minutes-increment.sp3");
    }

    @Test
    public void testRoundtripIssue895NoEOF() {
        doTestRoundtrip("/sp3/issue895-no-eof.sp3");
    }

    @Test
    public void testRoundtripIssue895SecondDigits() {
        doTestRoundtrip("/sp3/issue895-second-digits.sp3");
    }

    @Test
    public void testRoundtripLageos() {
        doTestRoundtrip("/sp3/truncated-nsgf.orb.lageos2.160305.v35.sp3");
    }

    private  void doTestRoundtrip(final String name) {
        try {
            DataSource source1 = new DataSource(name, () -> getClass().getResourceAsStream(name));
            if (name.endsWith(".Z")) {
                source1 = new UnixCompressFilter().filter(source1);

            }
            final SP3 original = new SP3Parser().parse(source1);

            // write the parsed file back to a characters array
            final CharArrayWriter caw = new CharArrayWriter();
            new SP3Writer(caw, "rebuilt-" + name, TimeScalesFactory.getTimeScales()).write(original);

            // reparse the written file
            final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
            final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
            final SP3        rebuilt = new SP3Parser().parse(source2);

            SP3TestUtils.checkEquals(original, rebuilt);

        } catch (IOException ioe) {
            Assertions.fail(ioe.getLocalizedMessage());
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
