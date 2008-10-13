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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.data.DataFileLoader;
import org.orekit.errors.OrekitException;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeStamped;

/** Loader for EOP 05 C04 files.
 * <p>EOP 05 C04 files contain {@link EarthOrientationParameters
 * Earth Orientation Parameters} consistent with ITRF2005 for one year periods.</p>
 * <p>The EOP 05 C04 files are recognized thanks to their base names,
 * which must match the pattern <code>eopc04_IAU2000.##</code>
 * (or <code>eopc04_IAU2000.##.gz</code> for gzip-compressed files)
 * where # stands for a digit character.</p>
 * <p>Between 2002 and 2007, another series of Earth Orientation Parameters was
 * in use: EOPC04 (without the 05). These parameters were consistent with the
 * previous ITRS realization: ITRF2000. These files are no longer provided by IERS
 * and only 6 files covering the range 2002 to 2007 were generated. The content of
 * these files is not the same as the content of the new files supported by this class,
 * however IERS uses the same file naming convention for both. If a file from the older
 * series is found by this class, a parse error will be triggered. Users must remove
 * such files to avoid being lured in believing they do have EOP data.</p>
 * <p>Files containing old data (back to 1962) have been regenerated in the new file
 * format and are available at IERS web site: <a
 * href="http://www.iers.org/MainDisp.csl?pid=36-25788&prodid=179">EOP 05 C04 (IAU2000)
 * yearly files - all available version</a>.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
class EOP05C04FilesLoader implements DataFileLoader {

    /** Conversion factor. */
    private static final double ARC_SECONDS_TO_RADIANS = 2 * Math.PI / 1296000;

    /** Year field. */
    private static final int YEAR_FIELD = 0;

    /** Month field. */
    private static final int MONTH_FIELD = 1;

    /** Day field. */
    private static final int DAY_FIELD = 2;

    /** MJD field. */
    private static final int MJD_FIELD = 3;

    /** X component of pole motion field. */
    private static final int POLE_X_FIELD = 4;

    /** Y component of pole motion field. */
    private static final int POLE_Y_FIELD = 5;

    /** UT1-UTC field. */
    private static final int UT1_UTC_FIELD = 6;

    /** Pattern for data lines.
     * <p>
     * The data lines in the EOP 05 C04 yearly data files have the following fixed form:
     * <p>
     * <pre>
     * year month day MJD ...12 floating values fields in decimal format...
     * 2000   1   1  51544   0.043157   0.377872   0.3555456   ...
     * 2000   1   2  51545   0.043475   0.377738   0.3547352   ...
     * 2000   1   3  51546   0.043627   0.377507   0.3538988   ...
     * </pre>
     * <p>the corresponding fortran format is:
     *  3(I4),I7,2(F11.6),2(F12.7),2(F12.6),2(F11.6),2(F12.7),2F12.6</p>
     */
    private static final Pattern LINE_PATTERN =
        Pattern.compile("^\\d+ +\\d+ +\\d+ +\\d+(?: +-?\\d+\\.\\d+){12}$");

    /** Supported files name pattern. */
    private Pattern namePattern;

    /** Earth Orientation Parameters entries. */
    private SortedSet<TimeStamped> eop;

    /** Build a loader for IERS EOP 05 C04 files.
     * @param eop set where to <em>add</em> EOP data
     * (pre-existing data is preserved)
     */
    public EOP05C04FilesLoader(final SortedSet<TimeStamped> eop) {
        namePattern = Pattern.compile("^eopc04_IAU2000\\.(\\d\\d)$");
        this.eop = eop;
    }

    /** Load Earth Orientation Parameters.
     * <p>The data is concatenated from all bulletin B data files
     * which can be found in the configured IERS directory.</p>
     * @exception OrekitException if some data can't be read or some
     * file content is corrupted
     */
    public void loadEOP() throws OrekitException {
        new DataDirectoryCrawler().crawl(this);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, OrekitException {

        // set up a reader for line-oriented bulletin B files
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        // read all file, ignoring header
        int lineNumber = 0;
        boolean inHeader = true;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            boolean parsed = false;

            if (LINE_PATTERN.matcher(line).matches()) {
                inHeader = false;
                // this is a data line, build an entry from the extracted fields
                final String[] fields = line.split(" +");
                final int    year  = Integer.parseInt(fields[YEAR_FIELD]);
                final int    month = Integer.parseInt(fields[MONTH_FIELD]);
                final int    day   = Integer.parseInt(fields[DAY_FIELD]);
                final int    mjd   = Integer.parseInt(fields[MJD_FIELD]);
                if (new DateComponents(year, month, day).getMJD() == mjd) {
                    // the first four fields are consistent with the expected format
                    final double x     = Double.parseDouble(fields[POLE_X_FIELD]) * ARC_SECONDS_TO_RADIANS;
                    final double y     = Double.parseDouble(fields[POLE_Y_FIELD]) * ARC_SECONDS_TO_RADIANS;
                    final double dtu1  = Double.parseDouble(fields[UT1_UTC_FIELD]);
                    eop.add(new EarthOrientationParameters(mjd, dtu1, new PoleCorrection(x, y)));
                    parsed = true;
                }
            }
            if (!(inHeader || parsed)) {
                throw new OrekitException("unable to parse line {0} in IERS data file {1}",
                                          new Object[] {
                                              Integer.valueOf(lineNumber), name
                                          });
            }
        }

        // check if we have read something
        if (inHeader) {
            throw new OrekitException("file {0} is not a supported IERS data file",
                                      new Object[] {
                                          name
                                      });
        }

    }

    /** {@inheritDoc} */
    public boolean fileIsSupported(final String fileName) {
        return namePattern.matcher(fileName).matches();
    }

}
