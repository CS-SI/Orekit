/* Copyright 2022-2025 Luc Maisonobe
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
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

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

    @Test
    public void testRoundtripIssue1327FullLine() {
        doTestRoundtrip("/sp3/issue1327-136-sats.sp3");
    }

    @Test
    public void testChangeFrameItrf96PositionOnly() {
        doTestChangeFrame("/sp3/gbm18432.sp3.Z",
                          FramesFactory.getITRF(ITRFVersion.ITRF_1996,
                                                IERSConventions.IERS_1996,
                                                false));
    }

    @Test
    public void testChangeFrameItrf05PositionOnly() {
        doTestChangeFrame("/sp3/gbm18432.sp3.Z",
                          FramesFactory.getITRF(ITRFVersion.ITRF_2005,
                                                IERSConventions.IERS_2003,
                                                false));
    }

    @Test
    public void testChangeFrameItrf20PositionOnly() {
        doTestChangeFrame("/sp3/gbm18432.sp3.Z",
                          FramesFactory.getITRF(ITRFVersion.ITRF_2020,
                                                IERSConventions.IERS_2010,
                                                false));
    }

    @Test
    public void testChangeFrameGcrfPositionOnly() {
        doTestChangeFrame("/sp3/gbm18432.sp3.Z", FramesFactory.getGCRF());
    }

    @Test
    public void testChangeFrameEme2000PositionOnly() {
        doTestChangeFrame("/sp3/gbm18432.sp3.Z", FramesFactory.getEME2000());
    }

    @Test
    public void testChangeFrameItrf96PositionVelocity() {
        doTestChangeFrame("/sp3/example-a-2.sp3",
                          FramesFactory.getITRF(ITRFVersion.ITRF_1996,
                                                IERSConventions.IERS_1996,
                                                false));
    }

    @Test
    public void testChangeFrameItrf05PositionVelocity() {
        doTestChangeFrame("/sp3/example-a-2.sp3",
                          FramesFactory.getITRF(ITRFVersion.ITRF_2005,
                                                IERSConventions.IERS_2003,
                                                false));
    }

    @Test
    public void testChangeFrameItrf20PositionVelocity() {
        doTestChangeFrame("/sp3/example-a-2.sp3",
                          FramesFactory.getITRF(ITRFVersion.ITRF_2020,
                                                IERSConventions.IERS_2010,
                                                false));
    }

    @Test
    public void testChangeFrameGcrfPositionVelocity() {
        doTestChangeFrame("/sp3/example-a-2.sp3", FramesFactory.getGCRF());
    }

    @Test
    public void testChangeFrameEme2000PositionVelocity() {
        doTestChangeFrame("/sp3/example-a-2.sp3", FramesFactory.getEME2000());
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

    private  void doTestChangeFrame(final String name, final Frame newFrame) {
        try {
            DataSource source1 = new DataSource(name, () -> getClass().getResourceAsStream(name));
            if (name.endsWith(".Z")) {
                source1 = new UnixCompressFilter().filter(source1);

            }
            final SP3 original = new SP3Parser().parse(source1);
            final Frame originalFrame = original.getEphemeris(0).getFrame();

            // change frame
            final SP3 changed = SP3.changeFrame(original, newFrame);

            // write the parsed file back to a characters array
            final CharArrayWriter caw = new CharArrayWriter();
            new SP3Writer(caw, name + "-changed", TimeScalesFactory.getTimeScales()).write(changed);

            // reparse the written file
            final byte[]     bytes   = caw.toString().getBytes(StandardCharsets.UTF_8);
            final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
            final SP3        rebuilt = new SP3Parser().parse(source2);

            Assertions.assertEquals(newFrame.getName(), rebuilt.getEphemeris(0).getFrame().getName());
            Assertions.assertEquals(original.getSatelliteCount(), rebuilt.getSatelliteCount());
            double maxErrorP = 0;
            double maxErrorV = 0;
            for (int i = 0; i < original.getSatelliteCount(); ++i) {
                final List<SP3Segment> originalSegments = original.getEphemeris(i).getSegments();
                final List<SP3Segment> rebuiltSegments  = rebuilt.getEphemeris(i).getSegments();
                Assertions.assertEquals(originalSegments.size(), rebuiltSegments.size());
                for (int j = 0; j < originalSegments.size(); ++j) {
                    final List<SP3Coordinate> originalCoordinates = originalSegments.get(j).getCoordinates();
                    final List<SP3Coordinate> rebuiltCoordinates  = rebuiltSegments.get(j).getCoordinates();
                    Assertions.assertEquals(originalCoordinates.size(), rebuiltCoordinates.size());
                    for (int k = 0; k < originalCoordinates.size(); ++k) {
                        final SP3Coordinate ok = originalCoordinates.get(k);
                        final SP3Coordinate rk = rebuiltCoordinates.get(k);
                        final PVCoordinates pv = newFrame.
                                                 getTransformTo(originalFrame, ok.getDate()).
                                                 transformPVCoordinates(rk);
                        maxErrorP = FastMath.max(maxErrorP, Vector3D.distance(ok.getPosition(), pv.getPosition()));
                        maxErrorV = FastMath.max(maxErrorV,
                                                 Vector3D.distance(ok.getVelocity(),
                                                                   ok.getVelocity().getNorm() < 1.0e-15 ?
                                                                   rk.getVelocity() :
                                                                   pv.getVelocity()));
                    }
                }
            }

            // tolerances are limited to SP3 file format accuracy
            // as we have written and parsed again a file
            Assertions.assertEquals(0, maxErrorP, 1.0e-3);
            Assertions.assertEquals(0, maxErrorV, 1.0e-6);

        } catch (IOException ioe) {
            Assertions.fail(ioe.getLocalizedMessage());
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
