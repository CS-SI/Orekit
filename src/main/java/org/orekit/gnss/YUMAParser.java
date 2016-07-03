/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.gnss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.Pair;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/**
 * This class reads Yuma almanac files and provides {@link GPSAlmanac GPS almanacs}.
 *
 * <p>The definition of a Yuma almanac comes from the
 * <a href="http://www.navcen.uscg.gov/?pageName=gpsYuma">U.S. COAST GUARD NAVIGATION CENTER</a>.</p>
 *
 * <p>The format of the files holding Yuma almanacs is not precisely specified,
 * so the parsing rules have been deduced from the downloadable files at
 * <a href="http://www.navcen.uscg.gov/?pageName=gpsAlmanacs">NAVCEN</a>
 * and at <a href="http://celestrak.com/GPS/almanac/Yuma/">CelesTrak</a>.</p>
 *
 * @author Pascal Parraud
 * @since 8.0
 *
 */
public class YUMAParser implements DataLoader {

    // Constants
    /** The source of the almanacs. */
    private static final String SOURCE = "YUMA";

    /** the useful keys in the YUMA file. */
    private static final String[] KEY = {
        "id", // ID
        "health", // Health
        "eccentricity", // Eccentricity
        "time", // Time of Applicability(s)
        "orbital", // Orbital Inclination(rad)
        "rate", // Rate of Right Ascen(r/s)
        "sqrt", // SQRT(A)  (m 1/2)
        "right", // Right Ascen at Week(rad)
        "argument", // Argument of Perigee(rad)
        "mean", // Mean Anom(rad)
        "af0", // Af0(s)
        "af1", // Af1(s/s)
        "week" // week
    };

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.alm$";

    // Fields
    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** the list of all the almanacs read from the file. */
    private final List<GPSAlmanac> almanacs;

    /** the list of all the PRN numbers of all the almanacs read from the file. */
    private final List<Integer> prnList;

    /** Simple constructor.
    *
    * <p>This constructor does not load any data by itself. Data must be loaded
    * later on by calling one of the {@link #loadData() loadData()} method or
    * the {@link #loadData(InputStream, String) loadData(inputStream, fileName)}
    * method.</p>
     *
     * <p>The supported files names are used when getting data from the
     * {@link #loadData() loadData()} method that relies on the
     * {@link DataProvidersManager data providers manager}. They are useless when
     * getting data from the {@link #loadData(InputStream, String) loadData(input, name)}
     * method.</p>
     *
     * @param supportedNames regular expression for supported files names
     * (if null, a default pattern matching files with a ".alm" extension will be used)
     * @see #loadData()
    */
    public YUMAParser(final String supportedNames) {
        this.supportedNames = (supportedNames == null) ? DEFAULT_SUPPORTED_NAMES : supportedNames;
        this.almanacs =  new ArrayList<GPSAlmanac>();
        this.prnList = new ArrayList<Integer>();
    }

    /**
     * Loads almanacs.
     *
     * <p>The almanacs already loaded in the instance will be discarded
     * and replaced by the newly loaded data.</p>
     * <p>This feature is useful when the file selection is already set up by
     * the {@link DataProvidersManager data providers manager} configuration.</p>
     *
     * @exception OrekitException if some data can't be read, some
     * file content is corrupted or no GPS almanac is available.
     */
    public void loadData() throws OrekitException {
        // load the data from the configured data providers
        DataProvidersManager.getInstance().feed(supportedNames, this);
        if (almanacs.isEmpty()) {
            throw new OrekitException(OrekitMessages.NO_YUMA_ALMANAC_AVAILABLE);
        }
    }

    @Override
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // Clears the lists
        almanacs.clear();
        prnList.clear();

        // Creates the reader
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

        try {
            // Gathers data to create one GPSAlmanac from 13 consecutive lines
            final List<Pair<String, String>> entries =
                new ArrayList<Pair<String, String>>(KEY.length);

            // Reads the data one line at a time
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                // Try to split the line into 2 tokens as key:value
                final String[] token = line.trim().split(":");
                // If the line is made of 2 tokens
                if (token.length == 2) {
                    // Adds these tokens as an entry to the entries
                    entries.add(new Pair<String, String>(token[0].trim(), token[1].trim()));
                }
                // If the number of entries equals the expected number
                if (entries.size() == KEY.length) {
                    // Gets a GPSAlmanac from the entries
                    final GPSAlmanac almanac = getAlmanac(entries, name);
                    // Adds the GPSAlmanac to the list
                    almanacs.add(almanac);
                    // Adds the PRN number of the GPSAlmanac to the list
                    prnList.add(almanac.getPRN());
                    // Clears the entries
                    entries.clear();
                }
            }
        } catch (IOException ioe) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_YUMA_ALMANAC_FILE,
                                      name);
        }
    }

    @Override
    public boolean stillAcceptsData() {
        return almanacs.isEmpty();
    }

    /** Get the supported names for data files.
     * @return regular expression for the supported names for data files
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /**
     * Gets all the {@link GPSAlmanac GPS almanacs} read from the file.
     *
     * @return the list of {@link GPSAlmanac} from the file
     */
    public List<GPSAlmanac> getAlmanacs() {
        return almanacs;
    }

    /**
     * Gets the PRN numbers of all the {@link GPSAlmanac GPS almanacs} read from the file.
     *
     * @return the PRN numbers of all the {@link GPSAlmanac GPS almanacs} read from the file
     */
    public List<Integer> getPRNNumbers() {
        return prnList;
    }

    /**
     * Builds a {@link GPSAlmanac GPS almanac} from data read in the file.
     *
     * @param entries the data read from the file
     * @param name name of the file
     * @return a {@link GPSAlmanac GPS almanac}
     * @throws OrekitException if a GPSAlmanac can't be built from the gathered entries
     */
    private GPSAlmanac getAlmanac(final List<Pair<String, String>> entries, final String name)
        throws OrekitException {
        try {
            // Initializes fields
            int prn = 0;
            int health = 0;
            int week = 0;
            double ecc = 0;
            double toa = 0;
            double inc = 0;
            double dom = 0;
            double sqa = 0;
            double om0 = 0;
            double aop = 0;
            double anom = 0;
            double af0 = 0;
            double af1 = 0;
            // Initializes checks
            final boolean[] checks = new boolean[KEY.length];
            // Loop over entries
            for (Pair<String, String> entry: entries) {
                if (entry.getKey().toLowerCase().startsWith(KEY[0])) {
                    // Gets the PRN of the SVN
                    prn = Integer.parseInt(entry.getValue());
                    checks[0] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[1])) {
                    // Gets the Health status
                    health = Integer.parseInt(entry.getValue());
                    checks[1] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[2])) {
                    // Gets the eccentricity
                    ecc = Double.parseDouble(entry.getValue());
                    checks[2] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[3])) {
                    // Gets the Time of Applicability
                    toa = Double.parseDouble(entry.getValue());
                    checks[3] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[4])) {
                    // Gets the Inclination
                    inc = Double.parseDouble(entry.getValue());
                    checks[4] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[5])) {
                    // Gets the Rate of Right Ascension
                    dom = Double.parseDouble(entry.getValue());
                    checks[5] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[6])) {
                    // Gets the square root of the semi-major axis
                    sqa = Double.parseDouble(entry.getValue());
                    checks[6] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[7])) {
                    // Gets the Right Ascension of Ascending Node
                    om0 = Double.parseDouble(entry.getValue());
                    checks[7] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[8])) {
                    // Gets the Argument of Perigee
                    aop = Double.parseDouble(entry.getValue());
                    checks[8] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[9])) {
                    // Gets the Mean Anomalie
                    anom = Double.parseDouble(entry.getValue());
                    checks[9] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[10])) {
                    // Gets the SV clock bias
                    af0 = Double.parseDouble(entry.getValue());
                    checks[10] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[11])) {
                    // Gets the SV clock Drift
                    af1 = Double.parseDouble(entry.getValue());
                    checks[11] = true;
                } else if (entry.getKey().toLowerCase().startsWith(KEY[12])) {
                    // Gets the week number
                    week = Integer.parseInt(entry.getValue());
                    checks[12] = true;
                } else {
                    // Unknown entry: the file is not a YUMA file
                    throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_YUMA_ALMANAC_FILE,
                                              name);
                }
            }

            // If all expected fields have been read
            if (readOK(checks)) {
                // Returns a GPSAlmanac built from the entries
                return new GPSAlmanac(SOURCE, prn, -1, week, toa, sqa, ecc, inc, om0, dom,
                                      aop, anom, af0, af1, health, -1, -1);
            } else {
                // The file is not a YUMA file
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_YUMA_ALMANAC_FILE,
                                          name);
            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_YUMA_ALMANAC_FILE,
                                      name);
        }
    }

    /** Checks if all expected fields have been read.
     * @param checks flags for read fields
     * @return true if all expected fields have been read, false if not
     */
    private boolean readOK(final boolean[] checks) {
        for (boolean check: checks) {
            if (!check) {
                return false;
            }
        };
        return true;
    }
}
