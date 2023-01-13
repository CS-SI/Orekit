/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.YUMAParser;
import org.orekit.propagation.analytical.gnss.data.QZSSAlmanac;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class QZSSAlmanacTest {

    @Test
    public void testLoadData() throws IOException, ParseException, OrekitException {
        Utils.setDataRoot("regular-data");
        // the parser for reading Yuma files with a pattern
        QZSSYUMAParser reader = new QZSSYUMAParser(".*\\.yum$");
        // the YUMA file to read
        final String fileName = "/gnss/q2019034.alm";
        final InputStream in = getClass().getResourceAsStream(fileName);
        reader.loadData(in, fileName);

        Assertions.assertEquals(".*\\.yum$", reader.getSupportedNames());

        // Checks the whole file read
        Assertions.assertEquals(4, reader.getAlmanacs().size());
        Assertions.assertEquals(4, reader.getPRNNumbers().size());

        // Checks the last almanac read
        final QZSSAlmanac alm = reader.getAlmanacs().get(reader.getAlmanacs().size() - 1);
        Assertions.assertEquals(199, alm.getPRN());
        Assertions.assertEquals(1015, alm.getWeek());
        Assertions.assertEquals(262144.0, alm.getTime(), 0.);
        Assertions.assertEquals(6493.484863, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assertions.assertEquals(1.387596130E-04, alm.getE(), FastMath.ulp(8E-05));
        Assertions.assertEquals(0.0007490141,  alm.getI0(), 0.);
        Assertions.assertEquals(0., alm.getIDot(), 0.);
        Assertions.assertEquals(9.194173760E-01, alm.getOmega0(), 0.);
        Assertions.assertEquals(9.714690370E-10, alm.getOmegaDot(), FastMath.ulp(-8E-09));
        Assertions.assertEquals(2.722442515, alm.getPa(), 0.);
        Assertions.assertEquals(-1.158294811, alm.getM0(), 0.);
        Assertions.assertEquals(6.351470947E-04, alm.getAf0(), 0.);
        Assertions.assertEquals(0.0, alm.getAf1(), 0.);
        Assertions.assertEquals(0, alm.getHealth());
        Assertions.assertEquals("YUMA", alm.getSource());
        Assertions.assertTrue(alm.getDate().durationFrom(new GNSSDate(1015, 262144.0, SatelliteSystem.QZSS).getDate()) == 0);
        Assertions.assertEquals(0., alm.getCic(), 0.);
        Assertions.assertEquals(0., alm.getCis(), 0.);
        Assertions.assertEquals(0., alm.getCrc(), 0.);
        Assertions.assertEquals(0., alm.getCrs(), 0.);
        Assertions.assertEquals(0., alm.getCuc(), 0.);
        Assertions.assertEquals(0., alm.getCus(), 0.);
    }

    /**
     * This class reads Yuma almanac files and provides {@link QZSSAlmanac QZSS almanacs}.
     *
     * <p>This class is a rewrite of {@link YUMAParser} adapted to QZSS yuma files</p>
     *
     * @author Pascal Parraud
     *
     */
    private class QZSSYUMAParser implements DataLoader {

        // Constants
        /** The source of the almanacs. */
        private static final String SOURCE = "YUMA";

        /** the useful keys in the YUMA file. */
        private final String[] KEY = {
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
        private final List<QZSSAlmanac> almanacs;

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
        public QZSSYUMAParser(final String supportedNames) {
            this.supportedNames = (supportedNames == null) ? DEFAULT_SUPPORTED_NAMES : supportedNames;
            this.almanacs =  new ArrayList<QZSSAlmanac>();
            this.prnList = new ArrayList<Integer>();
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException, OrekitException {

            // Clears the lists
            almanacs.clear();
            prnList.clear();

            // Creates the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            try {
                // Gathers data to create one QZSSAlmanac from 13 consecutive lines
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
                        // Gets a QZSSAlmanac from the entries
                        final QZSSAlmanac almanac = getAlmanac(entries, name);
                        // Adds the QZSSAlmanac to the list
                        almanacs.add(almanac);
                        // Adds the PRN number of the QZSSAlmanac to the list
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
         * Gets all the {@link QZSSAlmanac QZSS Almanacs} read from the file.
         *
         * @return the list of {@link QZSSAlmanac} from the file
         */
        public List<QZSSAlmanac> getAlmanacs() {
            return almanacs;
        }

        /**
         * Gets the PRN numbers of all the {@link QZSSAlmanac QZSS Almanacs} read from the file.
         *
         * @return the PRN numbers of all the {@link QZSSAlmanac QZSS Almanacs} read from the file
         */
        public List<Integer> getPRNNumbers() {
            return prnList;
        }

        /**
         * Builds a {@link QZSSAlmanac QZSS Almanac} from data read in the file.
         *
         * @param entries the data read from the file
         * @param name name of the file
         * @return a {@link QZSSAlmanac QZSS Almanac}
         */
        private QZSSAlmanac getAlmanac(final List<Pair<String, String>> entries, final String name) {
            try {
                // Initializes almanac
                final QZSSAlmanac almanac = new QZSSAlmanac();
                almanac.setSource(SOURCE);

                // Initializes checks
                final boolean[] checks = new boolean[KEY.length];
                // Loop over entries
                for (Pair<String, String> entry: entries) {
                    if (entry.getKey().toLowerCase().startsWith(KEY[0])) {
                        // Gets the PRN of the SVN
                        almanac.setPRN(Integer.parseInt(entry.getValue()));
                        checks[0] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[1])) {
                        // Gets the Health status
                        almanac.setHealth(Integer.parseInt(entry.getValue()));
                        checks[1] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[2])) {
                        // Gets the eccentricity
                        almanac.setE(Double.parseDouble(entry.getValue()));
                        checks[2] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[3])) {
                        // Gets the Time of Applicability
                        almanac.setTime(Double.parseDouble(entry.getValue()));
                        checks[3] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[4])) {
                        // Gets the Inclination
                        almanac.setI0(Double.parseDouble(entry.getValue()));
                        checks[4] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[5])) {
                        // Gets the Rate of Right Ascension
                        almanac.setOmegaDot(Double.parseDouble(entry.getValue()));
                        checks[5] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[6])) {
                        // Gets the square root of the semi-major axis
                        almanac.setSqrtA(Double.parseDouble(entry.getValue()));
                        checks[6] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[7])) {
                        // Gets the Right Ascension of Ascending Node
                        almanac.setOmega0(Double.parseDouble(entry.getValue()));
                        checks[7] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[8])) {
                        // Gets the Argument of Perigee
                        almanac.setPa(Double.parseDouble(entry.getValue()));
                        checks[8] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[9])) {
                        // Gets the Mean Anomalie
                        almanac.setM0(Double.parseDouble(entry.getValue()));
                        checks[9] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[10])) {
                        // Gets the SV clock bias
                        almanac.setAf0(Double.parseDouble(entry.getValue()));
                        checks[10] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[11])) {
                        // Gets the SV clock Drift
                        almanac.setAf1(Double.parseDouble(entry.getValue()));
                        checks[11] = true;
                    } else if (entry.getKey().toLowerCase().startsWith(KEY[12])) {
                        // Gets the week number
                        almanac.setWeek(Integer.parseInt(entry.getValue()));
                        checks[12] = true;
                    } else {
                        // Unknown entry: the file is not a YUMA file
                        throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_YUMA_ALMANAC_FILE,
                                                  name);
                    }
                }

                // If all expected fields have been read
                if (readOK(checks)) {
                    // Returns a QZSSAlmanac built from the entries
                    final AbsoluteDate date = new GNSSDate(almanac.getWeek(), almanac.getTime(), SatelliteSystem.QZSS).getDate();
                    almanac.setDate(date);
                    return almanac;
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
            }
            return true;
        }
    }

}
