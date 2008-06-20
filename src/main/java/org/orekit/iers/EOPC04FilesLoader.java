/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.iers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.frames.PoleCorrection;
import org.orekit.time.TimeStamped;


/** Loader for EOP C 04 files.
 * <p>EOP C 04 files contain {@link EarthOrientationParameters
 * Earth Orientation Parameters} for one year periods.</p>
 * <p>The EOP C 04 files are recognized thanks to their base names,
 * which must match the pattern <code>eopc04_IAU2000.##</code>
 * (or <code>eopc04_IAU2000.##.gz</code> for gzip-compressed files)
 * where # stands for a digit character.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class EOPC04FilesLoader extends IERSFileCrawler {

    /** Conversion factor. */
    private static final double ARC_SECONDS_TO_RADIANS = 2 * Math.PI / 1296000;

    /** Data line pattern. */
    private final Pattern dataPattern;

    /** Earth Orientation Parameters entries. */
    private TreeSet<TimeStamped> eop;

    /** Build a loader for IERS EOPC 04 files.
     * @param eop set where to <em>add</em> EOP data
     * (pre-existing data is preserved)
     */
    public EOPC04FilesLoader(final TreeSet<TimeStamped> eop) {

        super("^eopc04_IAU2000\\.(\\d\\d)(?:\\.gz)?$");
        this.eop = eop;

        // the data lines in the EOP C 04 yearly data files have the following fixed form:
        //   JAN   1  52275-0.176980 0.293952-0.1158223   0.0008163    0.00044  0.00071
        //   JAN   2  52276-0.177500 0.297468-0.1166973   0.0009382    0.00030  0.00043
        // the corresponding fortran format is:
        //  2X,A4,I3,2X,I5,2F9.6,F10.7,2X,F10.7,2X,2F9.5
        final String yearField  = "\\p{Upper}\\p{Upper}\\p{Upper}\\p{Blank}";
        final String dayField   = "\\p{Blank}[ 0-9]\\p{Digit}";
        final String mjdField   = "(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";
        final String poleField  = "(.........)";
        final String dtU1Field  = "(..........)";
        final String lodField   = "..........";
        final String deltaField = ".........";
        final String twoSpaces  = "  ";
        dataPattern = Pattern.compile("^  " + yearField + dayField + twoSpaces +
                                      mjdField + poleField + poleField +
                                      dtU1Field + twoSpaces + lodField +
                                      twoSpaces + deltaField + deltaField + "\\p{Blank}*$");

    }

    /** Load Earth Orientation Parameters.
     * <p>The data is concatenated from all EOP C 04 data files
     * which can be found in the configured IERS directory.</p>
     * @exception OrekitException if some data can't be read or some
     * file content is corrupted
     */
    public void loadEOP() throws OrekitException {
        new IERSDirectoryCrawler().crawl(this);
    }

    /** {@inheritDoc} */
    protected void visit(final BufferedReader reader)
        throws IOException, OrekitException {

        // read all file, ignoring header
        int lineNumber = 0;
        boolean inHeader = true;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            boolean parsed = false;
            final Matcher matcher = dataPattern.matcher(line);
            if (matcher.matches()) {
                inHeader = false;
                try {
                    // this is a data line, build an entry from the extracted fields
                    final int    date = Integer.parseInt(matcher.group(1));
                    final double x    = Double.parseDouble(matcher.group(2)) * ARC_SECONDS_TO_RADIANS;
                    final double y    = Double.parseDouble(matcher.group(3)) * ARC_SECONDS_TO_RADIANS;
                    final double dtu1 = Double.parseDouble(matcher.group(4));
                    eop.add(new EarthOrientationParameters(date, dtu1, new PoleCorrection(x, y)));
                    parsed = true;
                } catch (NumberFormatException nfe) {
                    // ignored, will be handled by the parsed boolean
                }
            }
            if (!(inHeader || parsed)) {
                throw new OrekitException("unable to parse line {0} in IERS data file {1}",
                                          new Object[] {
                                              Integer.valueOf(lineNumber),
                                              getFile().getAbsolutePath()
                                          });
            }
        }

        // check if we have read something
        if (inHeader) {
            throw new OrekitException("file {0} is not an IERS data file",
                                      new Object[] {
                                          getFile().getAbsolutePath()
                                      });
        }

    }

}
