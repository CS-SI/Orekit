/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss.attitude;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public abstract class AbstractGNSSAttitudeProviderTest {

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:gnss");
    }

    protected abstract GNSSAttitudeProvider createProvider(final AbsoluteDate validityStart,
                                                           final AbsoluteDate validityEnd,
                                                           final PVCoordinatesProvider sun,
                                                           final Frame inertialFrame,
                                                           final int prnNumber);

    protected abstract String getSuffix();

    @Test
    public void testLargeNegativeBeta() throws OrekitException {
        doTest("beta-large-negative-" + getSuffix() + ".txt");
    }

    @Test
    public void testSmallNegativeBeta() throws OrekitException {
        doTest("beta-small-negative-" + getSuffix() + ".txt");
    }

    @Test
    public void testCrossingBeta() throws OrekitException {
        doTest("beta-crossing-" + getSuffix() + ".txt");
    }

    @Test
    public void testSmallPositiveBeta() throws OrekitException {
        doTest("beta-small-positive-" + getSuffix() + ".txt");
    }

    @Test
    public void testLargePositiveBeta() throws OrekitException {
        doTest("beta-large-positive-" + getSuffix() + ".txt");
    }

    private void doTest(final String fileName) throws OrekitException {

        if (getClass().getResource("/gnss/" + fileName) != null) {

            // the transforms between EME2000 and ITRF will not really be correct here
            // because the corresponding EOP are not present in the resources used
            // however, this is not a problem because we rely only on data generated
            // in ITRF and fully consistent (both EOP and Sun ephemeris were used at
            // data generation phase). The test performed here will convert back
            // to EME2000 (which will be slightly offset due to missing EOP), but
            // Sun/Earth/spacecraft relative geometry will remain consistent
            final Frame eme2000 = FramesFactory.getEME2000();
            final Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
            final List<List<ParsedLine>> dataBlocks = parseFile(fileName, eme2000, itrf);
            for (final List<ParsedLine> dataBlock : dataBlocks) {
                final AbsoluteDate validityStart = dataBlock.get(0).date;
                final AbsoluteDate validityEnd   = dataBlock.get(dataBlock.size() - 1).date;
                final int          prnNumber     = dataBlock.get(0).prnNumber;
                final PVCoordinatesProvider fakedSun = (date, frame) ->
                TimeStampedPVCoordinates.interpolate(date,
                                                     CartesianDerivativesFilter.USE_P,
                                                     dataBlock.stream().
                                                     filter(parsedLine ->
                                                     FastMath.abs(parsedLine.date.durationFrom(date)) < 300).
                                                     map(parsedLine ->
                                                     new TimeStampedPVCoordinates(parsedLine.date,
                                                                                  parsedLine.sunP,
                                                                                  Vector3D.ZERO,
                                                                                  Vector3D.ZERO)));
                final GNSSAttitudeProvider attitudeProvider =
                                createProvider(validityStart, validityEnd, fakedSun, eme2000, prnNumber);
                Assert.assertEquals(attitudeProvider.validityStart(), dataBlock.get(0).date);
                Assert.assertEquals(attitudeProvider.validityEnd(), dataBlock.get(dataBlock.size() - 1).date);

                for (final ParsedLine parsedLine : dataBlock) {
                    final Attitude attitude = attitudeProvider.getAttitude(parsedLine.orbit, parsedLine.date, parsedLine.orbit.getFrame());
                    if (attitude.getOrientation() == null) {
                        final Attitude attitude2 = attitudeProvider.getAttitude(parsedLine.orbit, parsedLine.date, parsedLine.orbit.getFrame());
                    }
                    final Vector3D xSat = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
                    System.out.println(parsedLine.date + " " + FastMath.toDegrees(Vector3D.angle(xSat, parsedLine.eclipsX)));
                    Assert.assertEquals(0.0, Vector3D.angle(xSat, parsedLine.eclipsX), 4.0e-14);
                }

            }
        }

    }

    private List<List<ParsedLine>> parseFile(final String fileName, final Frame eme2000, final Frame itrf)
        throws OrekitException {
        final List<List<ParsedLine>> dataBlocks = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/gnss/" + fileName);
             Reader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
             BufferedReader br = new BufferedReader(reader)) {

            // parse the reference data file into contiguous blocks
            dataBlocks.add(new ArrayList<>());
            ParsedLine parsedLine = null;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                final ParsedLine previous = parsedLine;
                parsedLine = new ParsedLine(line, eme2000, itrf);
                if (previous != null &&
                    (parsedLine.prnNumber != previous.prnNumber ||
                     parsedLine.date.durationFrom(previous.date) > 3600)) {
                    dataBlocks.add(new ArrayList<>());
                }
                dataBlocks.get(dataBlocks.size() - 1).add(parsedLine);
            }

        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }

        return dataBlocks;

    }

    private static class ParsedLine {

        final AbsoluteDate date;
        final int          prnNumber;
        final Orbit        orbit;
        final Vector3D     sunP;
        final double       beta;
        final double       delta;
        final Vector3D     nominalX;
        final double       nominalPsi;
        final Vector3D     eclipsX;
        final double       eclipsPsi;

        ParsedLine(final String line, final Frame eme2000, final Frame itrf) throws OrekitException {
            final String[] fields = line.split("\\s+");
            date       = AbsoluteDate.createGPSDate(Integer.parseInt(fields[1]),
                                                    Double.parseDouble(fields[2]));
            final Transform t = itrf.getTransformTo(eme2000, date);
            prnNumber  = Integer.parseInt(fields[3].substring(1));
            orbit      = new CartesianOrbit(new TimeStampedPVCoordinates(date,
                                                                         t.transformPosition(new Vector3D(Double.parseDouble(fields[ 6]),
                                                                                                          Double.parseDouble(fields[ 7]),
                                                                                                          Double.parseDouble(fields[ 8]))),
                                                                         t.transformVector(new Vector3D(Double.parseDouble(fields[ 9]),
                                                                                                        Double.parseDouble(fields[10]),
                                                                                                        Double.parseDouble(fields[11])))),
                                            eme2000, Constants.EIGEN5C_EARTH_MU);
            sunP       = t.transformPosition(new Vector3D(Double.parseDouble(fields[12]),
                                                          Double.parseDouble(fields[13]),
                                                          Double.parseDouble(fields[14])));
            beta       = FastMath.toRadians(Double.parseDouble(fields[15]));
            delta      = FastMath.toRadians(Double.parseDouble(fields[16]));
            nominalX   = t.transformVector(new Vector3D(Double.parseDouble(fields[17]),
                                                        Double.parseDouble(fields[18]),
                                                        Double.parseDouble(fields[19])));
            nominalPsi = FastMath.toRadians(Double.parseDouble(fields[20]));
            eclipsX    = t.transformVector(new Vector3D(Double.parseDouble(fields[21]),
                                                        Double.parseDouble(fields[22]),
                                                        Double.parseDouble(fields[23])));
            eclipsPsi  = FastMath.toRadians(Double.parseDouble(fields[24]));
        }

    }

}
