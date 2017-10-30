/* Copyright 2002-2017 CS Systèmes d'Information
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
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

/**
 * Factory for GNSS antennas (both receiver and satellite).
 *
 * @author Luc Maisonobe
 * @since 9.1
 * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
 *
 */
public class AntennaFactory {

    /** Default supported files name pattern for antex files. */
    public static final String DEFAULT_ANTEX_SUPPORTED_NAMES = "^\\w{5}(?:_\\d{4})?\\.atx$";

    /** Satellites antennas. */
    private final List<SatelliteAntenna> satellitesAntennas;

    /** Receiver antennas. */
    private final List<ReceiverAntenna> receiversAntennas;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @exception OrekitException if no antex file can be read
     */
    public AntennaFactory(final String supportedNames)
        throws OrekitException {
        this.satellitesAntennas = new ArrayList<>();
        this.receiversAntennas  = new ArrayList<>();
        DataProvidersManager.getInstance().feed(supportedNames, new AntexParser());
    }

    /** Parser for antex files.
     * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
     */
    private class AntexParser implements DataLoader {

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** Supported format version. */
        private static final double FORMAT_VERSION = 1.4;

        /** Phase center eccentricities conversion factor. */
        private static final double MM_TO_M = 0.001;

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // we load all antex files we can find
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, OrekitException {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {

                // placeholders for parsed data
                int                  lineNumber           = 0;
                SatelliteSystem      satelliteSystem      = null;
                String               antennaType          = null;
                SatelliteAntennaCode satelliteAntennaCode = null;
                String               serialNumber         = null;
                int                  prnNumber            = -1;
                int                  satelliteCode        = -1;
                String               cosparID             = null;
                AbsoluteDate         validFrom            = AbsoluteDate.PAST_INFINITY;
                AbsoluteDate         validUntil           = AbsoluteDate.FUTURE_INFINITY;
                String               sinexCode            = null;
                Vector3D             eccentricities       = Vector3D.ZERO;
                Frequency            frequency            = null;
                boolean              inFrequency          = false;
                boolean              inRMS                = false;

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    switch(line.substring(LABEL_START).trim()) {
                        case "COMMENT" :
                            // nothing to do
                            break;
                        case "ANTEX VERSION / SYST" :
                            if (FastMath.abs(parseDouble(line, 0, 8) - FORMAT_VERSION) > 0.001) {
                                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                            }
                            // we parse the general setting for satellite system to check for format errors,
                            // but otherwise ignore it
                            SatelliteSystem.parseSatelliteSystem(parseString(line, 20, 1));
                            break;
                        case "PCV TYPE / REFANT" :
                            // TODO
                            break;
                        case "END OF HEADER" :
                            // nothing to do
                            break;
                        case "START OF ANTENNA" :
                            // reset antenna data
                            satelliteSystem      = null;
                            antennaType          = null;
                            satelliteAntennaCode = null;
                            serialNumber         = null;
                            prnNumber            = -1;
                            satelliteCode        = -1;
                            cosparID             = null;
                            validFrom            = AbsoluteDate.PAST_INFINITY;
                            validUntil           = AbsoluteDate.FUTURE_INFINITY;
                            eccentricities       = Vector3D.ZERO;
                            frequency            = null;
                            inFrequency          = false;
                            inRMS                = false;
                            break;
                        case "TYPE / SERIAL NO" :
                            antennaType = parseString(line, 0, 20);
                            try {
                                satelliteAntennaCode = SatelliteAntennaCode.parseSatelliteAntennaCode(antennaType);
                                final String satField = parseString(line, 20, 20);
                                if (satField.length() > 0) {
                                    satelliteSystem = SatelliteSystem.parseSatelliteSystem(satField);
                                    final int n = parseInt(satField, 1, 19);
                                    switch (satelliteSystem) {
                                        case GPS:
                                        case GLONASS:
                                        case GALILEO:
                                        case COMPASS:
                                            prnNumber = n;
                                            break;
                                        case QZSS:
                                            prnNumber = n + 192;
                                            break;
                                        case SBAS:
                                            prnNumber = n + 100;
                                            break;
                                        default:
                                            // MIXED satellite system is not allowed here
                                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                      lineNumber, name, line);
                                    }
                                    satelliteCode = parseInt(line, 41, 9); // we drop the system type
                                    cosparID      = parseString(line, 50, 10);
                                }
                            } catch (OrekitIllegalArgumentException oiae) {
                                // this is a receiver antenna, not a satellite antenna
                                serialNumber = parseString(line, 20, 20);
                            }
                            break;
                        case "METH / BY / # / DATE" :
                            // ignored
                            break;
                        case "DAZI" :
                            // TODO
                            break;
                        case "ZEN1 / ZEN2 / DZEN" :
                            // TODO
                            break;
                        case "# OF FREQUENCIES" :
                            // TODO
                            break;
                        case "VALID FROM" :
                            validFrom = new AbsoluteDate(parseInt(line,     0,  6),
                                                         parseInt(line,     6,  6),
                                                         parseInt(line,    12,  6),
                                                         parseInt(line,    18,  6),
                                                         parseInt(line,    24,  6),
                                                         parseDouble(line, 30, 13),
                                                         TimeScalesFactory.getGPS());
                            break;
                        case "VALID UNTIL" :
                            validUntil = new AbsoluteDate(parseInt(line,     0,  6),
                                                          parseInt(line,     6,  6),
                                                          parseInt(line,    12,  6),
                                                          parseInt(line,    18,  6),
                                                          parseInt(line,    24,  6),
                                                          parseDouble(line, 30, 13),
                                                          TimeScalesFactory.getGPS());
                            break;
                        case "SINEX CODE" :
                            sinexCode = parseString(line, 0, 10);
                            break;
                        case "START OF FREQUENCY" :
                            try {
                                frequency = Frequency.valueOf(parseString(line, 3, 3));
                            } catch (IllegalArgumentException iae) {
                                throw new OrekitException(OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                          parseString(line, 3, 3));
                            }
                            inFrequency = true;
                            break;
                        case "NORTH / EAST / UP" :
                            if (!inRMS) {
                                eccentricities = new Vector3D(parseDouble(line,  0, 10) * MM_TO_M,
                                                              parseDouble(line, 10, 10) * MM_TO_M,
                                                              parseDouble(line, 20, 10) * MM_TO_M);
                            }
                            break;
                        case "END OF FREQUENCY" :
                            inFrequency = false;
                            break;
                        case "START OF FREQ RMS" :
                            inRMS = true;
                            break;
                        case "END OF FREQ RMS" :
                            inRMS = false;
                            break;
                        case "END OF ANTENNA" :
                            if (satelliteAntennaCode == null) {
                                receiversAntennas.add(new ReceiverAntenna(antennaType, sinexCode, eccentricities, serialNumber));
                            } else {
                                satellitesAntennas.add(new SatelliteAntenna(antennaType, sinexCode, eccentricities,
                                                                            satelliteSystem, prnNumber, satelliteCode,
                                                                            cosparID, validFrom, validUntil));
                            }
                            break;
                        default :
                            if (inFrequency) {
                                // TODO
                            } else if (inRMS) {
                                // TODO
                            } else {
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, name, line);
                            }
                    }
                }

            }
        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final String line, final int start, final int length) {
            return line.substring(start, start + length).trim();
        }

        /** Extract an integer from a line.
         * @param line to parse
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed integer
         */
        private int parseInt(final String line, final int start, final int length) {
            return Integer.parseInt(parseString(line, start, length));
        }

        /** Extract a double from a line.
         * @param line to parse
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real
         */
        private double parseDouble(final String line, final int start, final int length) {
            return Double.parseDouble(parseString(line, start, length));
        }

    }

}
