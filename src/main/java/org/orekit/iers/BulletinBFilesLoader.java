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


/** Loader for bulletin B files.
 * <p>Bulletin B files contain {@link EarthOrientationParameters
 * Earth Orientation Parameters} for a few months periods.</p>
 * <p>The bulletin B files are recognized thanks to their base names,
 * which must match the pattern <code>bulletinb_IAU2000-###.txt</code>
 * (or <code>bulletinb_IAU2000-###.txt.gz</code> for gzip-compressed files)
 * where # stands for a digit character.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class BulletinBFilesLoader extends IERSFileCrawler {

    /** Conversion factor. */
    private static final double ARC_SECONDS_TO_RADIANS = 2 * Math.PI / 1296000;

    /** Section header pattern. */
    private final Pattern sectionHeaderPattern;

    /** Data line pattern in section 1. */
    private final Pattern section1DataPattern;

    /** Pattern for line introducing the final bulletin B values. */
    private final Pattern finalValuesStartPattern;

    /** Pattern for line introducing the bulletin B preliminary extension. */
    private final Pattern finalValuesEndPattern;

    /** Data line pattern in section 2. */
    private final Pattern section2DataPattern;

    /** Earth Orientation Parameters entries. */
    private TreeSet<TimeStamped> eop;

    /** Create a loader for IERS bulletin B files.
     * @param eop set where to <em>add</em> EOP data
     * (pre-existing data is preserved)
     */
    public BulletinBFilesLoader(final TreeSet<TimeStamped> eop) {

        super("^bulletinb_IAU2000-(\\d\\d\\d)\\.txt(?:\\.gz)?$");
        this.eop = eop;

        // the section headers lines in the bulletin B monthly data files have
        // the following form (the indentation discrepancy for section 6 is
        // really present in the available files):
        // 1 - EARTH ORIENTATION PARAMETERS (IERS evaluation).
        // 2 - SMOOTHED VALUES OF x, y, UT1, D, dX, dY (IERS EVALUATION)
        // 3 - NORMAL VALUES OF THE EARTH ORIENTATION PARAMETERS AT FIVE-DAY INTERVALS
        // 4 - DURATION OF THE DAY AND ANGULAR VELOCITY OF THE EARTH (IERS evaluation).
        // 5 - INFORMATION ON TIME SCALES
        //       6 - SUMMARY OF CONTRIBUTED EARTH ORIENTATION PARAMETERS SERIES
        sectionHeaderPattern =
            Pattern.compile("^ +([123456]) - \\p{Upper}+ \\p{Upper}+ \\p{Upper}+.*");

        // the markers bracketing the final values in section 1 have the following form:
        //  Final Bulletin B values.
        //   ...
        //  Preliminary extension, to be updated weekly in Bulletin A and monthly
        //  in Bulletin B.
        finalValuesStartPattern = Pattern.compile("^\\p{Blank}+Final Bulletin B values.*");
        finalValuesEndPattern   = Pattern.compile("^\\p{Blank}+Preliminary extension,.*");

        // the data lines in the bulletin B monthly data files have the following form:
        // in section 1:
        // FEB   3  53769   0.05025  0.38417  0.322391  -32.677609    0.06   -0.31
        // FEB   8  53774   0.05153  0.38430  0.317918  -32.682082    0.28   -0.41
        // in section 2:
        // FEB   3   53769  0.05025  0.38417  0.321173 -1.218   1.507   0.06  -0.31
        // FEB   4   53770  0.05015  0.38454  0.319753 -1.748   1.275   0.03  -0.35
        final String monthField       = "^\\p{Blank}*\\p{Upper}\\p{Upper}\\p{Upper}";
        final String dayField         = "\\p{Blank}+[ 0-9]\\p{Digit}";
        final String mjdField         = "\\p{Blank}+(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";
        final String storedRealField  = "\\p{Blank}+(-?\\p{Digit}+\\.(?:\\p{Digit})+)";
        final String ignoredRealField = "\\p{Blank}+-?\\p{Digit}+\\.(?:\\p{Digit})+";
        final String finalBlanks      = "\\p{Blank}*$";
        section1DataPattern = Pattern.compile(monthField + dayField + mjdField +
                                              ignoredRealField + ignoredRealField + ignoredRealField +
                                              ignoredRealField + ignoredRealField + ignoredRealField +
                                              finalBlanks);
        section2DataPattern = Pattern.compile(monthField + dayField + mjdField +
                                              storedRealField  + storedRealField  + storedRealField +
                                              ignoredRealField + ignoredRealField +
                                              ignoredRealField + ignoredRealField +
                                              finalBlanks);

    }

    /** Load Earth Orientation Parameters.
     * <p>The data is concatenated from all bulletin B data files
     * which can be found in the configured IERS directory.</p>
     * @exception OrekitException if some data can't be read or some
     * file content is corrupted
     */
    public void loadEOP() throws OrekitException {
        new IERSDirectoryCrawler().crawl(this);
    }

    /** {@inheritDoc} */
    protected void visit(final BufferedReader reader)
        throws OrekitException, IOException {

        // Extract mjd bounds from section 1
        int mjdMin = -1;
        int mjdMax = -1;
        boolean inFinalValuesPart = false;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            Matcher matcher = finalValuesStartPattern.matcher(line);
            if (matcher.matches()) {
                // we are entering final values part (in section 1)
                inFinalValuesPart = true;
            } else if (inFinalValuesPart) {
                matcher = section1DataPattern.matcher(line);
                if (matcher.matches()) {
                    // this is a data line, build an entry from the extracted fields
                    final int mjd = Integer.parseInt(matcher.group(1));
                    if (mjdMin < 0) {
                        mjdMin = mjd;
                    } else {
                        mjdMax = mjd;
                    }
                } else {
                    matcher = finalValuesEndPattern.matcher(line);
                    if (matcher.matches()) {
                        // we leave final values part
                        break;
                    }
                }
            }
        }

        // read the data lines in the final values part inside section 2
        boolean inSection2 = false;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            Matcher matcher = sectionHeaderPattern.matcher(line);
            if (matcher.matches() && "2".equals(matcher.group(1))) {
                // we are entering section 2
                inSection2 = true;
            } else if (inSection2) {
                matcher = section2DataPattern.matcher(line);
                if (matcher.matches()) {
                    // this is a data line, build an entry from the extracted fields
                    final int    date = Integer.parseInt(matcher.group(1));
                    final double x    = Double.parseDouble(matcher.group(2)) * ARC_SECONDS_TO_RADIANS;
                    final double y    = Double.parseDouble(matcher.group(3)) * ARC_SECONDS_TO_RADIANS;
                    final double dtu1 = Double.parseDouble(matcher.group(4));
                    if (date >= mjdMin) {
                        eop.add(new EarthOrientationParameters(date, dtu1, new PoleCorrection(x, y)));
                        if (date >= mjdMax) {
                            // don't bother reading the rest of the file
                            return;
                        }
                    }
                }
            }
        }

    }

}
