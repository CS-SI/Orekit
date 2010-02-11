/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

/** Loader for bulletin B files.
 * <p>Bulletin B files contain {@link TimeStampedEntry
 * Earth Orientation Parameters} for a few months periods.</p>
 * <p>The bulletin B files are recognized thanks to their base names,
 * which must match one of the the patterns <code>bulletinb_IAU2000-###.txt</code>,
 * <code>bulletinb_IAU2000.###</code>, <code>bulletinb-###.txt</code> or
 * <code>bulletinb.###</code> (or the same ending with <code>.gz</code>
 * for gzip-compressed files) where # stands for a digit character.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
class BulletinBFilesLoader implements EOP1980HistoryLoader, EOP2000HistoryLoader {

    /** Conversion factor. */
    private static final double ARC_SECONDS_TO_RADIANS = 2 * Math.PI / 1296000;

    /** Conversion factor. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = 2 * Math.PI / 1296000000;

    /** Conversion factor. */
    private static final double MILLI_SECONDS_TO_SECONDS = 1.e-3;

    /** Regular expression for supported files names. */
    private final String supportedNames;

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

    /** History entries for IAU1980. */
    private EOP1980History history1980;

    /** History entries for IAU2000. */
    private EOP2000History history2000;

    /** Build a loader for IERS bulletins B files.
    * @param supportedNames regular expression for supported files names
    */
    public BulletinBFilesLoader(final String supportedNames) {

        this.supportedNames = supportedNames;

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
                                              ignoredRealField +
                                              storedRealField + storedRealField + storedRealField +
                                              finalBlanks);

    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

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
        synchronized (this) {

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
                        final int    date  = Integer.parseInt(matcher.group(1));
                        final double x     = Double.parseDouble(matcher.group(2)) * ARC_SECONDS_TO_RADIANS;
                        final double y     = Double.parseDouble(matcher.group(3)) * ARC_SECONDS_TO_RADIANS;
                        final double dtu1  = Double.parseDouble(matcher.group(4));
                        final double lod   = Double.parseDouble(matcher.group(5)) * MILLI_SECONDS_TO_SECONDS;
                        final double dpsi  = Double.parseDouble(matcher.group(6)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        final double deps  = Double.parseDouble(matcher.group(7)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        if (date >= mjdMin) {
                            if (history1980 != null) {
                                history1980.addEntry(new EOP1980Entry(date, dtu1, lod, dpsi, deps));
                            }
                            if (history2000 != null) {
                                history2000.addEntry(new EOP2000Entry(date, dtu1, lod, x, y));
                            }
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

    /** {@inheritDoc} */
    public void fillHistory(final EOP1980History history)
        throws OrekitException {
        synchronized (this) {
            history1980 = history;
            history2000 = null;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }

    /** {@inheritDoc} */
    public void fillHistory(final EOP2000History history)
        throws OrekitException {
        synchronized (this) {
            history1980 = null;
            history2000 = history;
            DataProvidersManager.getInstance().feed(supportedNames, this);
        }
    }

}
