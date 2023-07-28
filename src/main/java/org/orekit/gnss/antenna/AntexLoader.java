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
package org.orekit.gnss.antenna;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.TimeSpanMap;

/**
 * Factory for GNSS antennas (both receiver and satellite).
 * <p>
 * The factory creates antennas by parsing an
 * <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX</a> file.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public class AntexLoader {

    /** Default supported files name pattern for antex files. */
    public static final String DEFAULT_ANTEX_SUPPORTED_NAMES = "^\\w{5}(?:_\\d{4})?\\.atx$";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Satellites antennas. */
    private final List<TimeSpanMap<SatelliteAntenna>> satellitesAntennas;

    /** Receivers antennas. */
    private final List<ReceiverAntenna> receiversAntennas;

    /** GPS time scale. */
    private final TimeScale gps;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     *
     * @param supportedNames regular expression for supported files names
     * @see #AntexLoader(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public AntexLoader(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getGPS());
    }

    /**
     * Construct a loader by specifying a {@link DataProvidersManager}.
     *
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param gps the GPS time scale to use when loading the ANTEX files.
     * @since 10.1
     */
    public AntexLoader(final String supportedNames,
                       final DataProvidersManager dataProvidersManager,
                       final TimeScale gps) {
        this.gps = gps;
        satellitesAntennas = new ArrayList<>();
        receiversAntennas  = new ArrayList<>();
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /**
     * Construct a loader by specifying the source of ANTEX auxiliary data files.
     *
     * @param source source for the ANTEX data
     * @param gps the GPS time scale to use when loading the ANTEX files.
     * @since 12.0
     */
    public AntexLoader(final DataSource source, final TimeScale gps) {
        try {
            this.gps = gps;
            satellitesAntennas = new ArrayList<>();
            receiversAntennas  = new ArrayList<>();
            try (InputStream         is  = source.getOpener().openStreamOnce();
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                new Parser().loadData(bis, source.getName());
            }
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Add a satellite antenna.
     * @param antenna satellite antenna to add
     */
    private void addSatelliteAntenna(final SatelliteAntenna antenna) {
        try {
            final TimeSpanMap<SatelliteAntenna> existing =
                            findSatelliteAntenna(antenna.getSatelliteSystem(), antenna.getPrnNumber());
            // this is an update for a satellite antenna, with new time span
            existing.addValidAfter(antenna, antenna.getValidFrom(), false);
        } catch (OrekitException oe) {
            // this is a new satellite antenna
            satellitesAntennas.add(new TimeSpanMap<>(antenna));
        }
    }

    /** Get parsed satellites antennas.
     * @return unmodifiable view of parsed satellites antennas
     */
    public List<TimeSpanMap<SatelliteAntenna>> getSatellitesAntennas() {
        return Collections.unmodifiableList(satellitesAntennas);
    }

    /** Find the time map for a specific satellite antenna.
     * @param satelliteSystem satellite system
     * @param prnNumber number within the satellite system
     * @return time map for the antenna
     */
    public TimeSpanMap<SatelliteAntenna> findSatelliteAntenna(final SatelliteSystem satelliteSystem,
                                                              final int prnNumber) {
        final Optional<TimeSpanMap<SatelliteAntenna>> existing =
                        satellitesAntennas.
                        stream().
                        filter(m -> {
                            final SatelliteAntenna first = m.getFirstSpan().getData();
                            return first.getSatelliteSystem() == satelliteSystem &&
                                   first.getPrnNumber() == prnNumber;
                        }).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        } else {
            throw new OrekitException(OrekitMessages.CANNOT_FIND_SATELLITE_IN_SYSTEM,
                                      prnNumber, satelliteSystem);
        }
    }

    /** Add a receiver antenna.
     * @param antenna receiver antenna to add
     */
    private void addReceiverAntenna(final ReceiverAntenna antenna) {
        receiversAntennas.add(antenna);
    }

    /** Get parsed receivers antennas.
     * @return unmodifiable view of parsed receivers antennas
     */
    public List<ReceiverAntenna> getReceiversAntennas() {
        return Collections.unmodifiableList(receiversAntennas);
    }

    /** Parser for antex files.
     * @see <a href="ftp://www.igs.org/pub/station/general/antex14.txt">ANTEX: The Antenna Exchange Format, Version 1.4</a>
     */
    private class Parser implements DataLoader {

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

            int                              lineNumber           = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                // placeholders for parsed data
                SatelliteSystem                  satelliteSystem      = null;
                String                           antennaType          = null;
                SatelliteType                    satelliteType        = null;
                String                           serialNumber         = null;
                int                              prnNumber            = -1;
                int                              satelliteCode        = -1;
                String                           cosparID             = null;
                AbsoluteDate                     validFrom            = AbsoluteDate.PAST_INFINITY;
                AbsoluteDate                     validUntil           = AbsoluteDate.FUTURE_INFINITY;
                String                           sinexCode            = null;
                double                           azimuthStep          = Double.NaN;
                double                           polarStart           = Double.NaN;
                double                           polarStop            = Double.NaN;
                double                           polarStep            = Double.NaN;
                double[]                         grid1D               = null;
                double[][]                       grid2D               = null;
                Vector3D                         eccentricities       = Vector3D.ZERO;
                int                              nbFrequencies        = -1;
                Frequency                        frequency            = null;
                Map<Frequency, FrequencyPattern> patterns             = null;
                boolean                          inFrequency          = false;
                boolean                          inRMS                = false;

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    switch (line.substring(LABEL_START).trim()) {
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
                            satelliteType        = null;
                            serialNumber         = null;
                            prnNumber            = -1;
                            satelliteCode        = -1;
                            cosparID             = null;
                            validFrom            = AbsoluteDate.PAST_INFINITY;
                            validUntil           = AbsoluteDate.FUTURE_INFINITY;
                            sinexCode            = null;
                            azimuthStep          = Double.NaN;
                            polarStart           = Double.NaN;
                            polarStop            = Double.NaN;
                            polarStep            = Double.NaN;
                            grid1D               = null;
                            grid2D               = null;
                            eccentricities       = Vector3D.ZERO;
                            nbFrequencies        = -1;
                            frequency            = null;
                            patterns             = null;
                            inFrequency          = false;
                            inRMS                = false;
                            break;
                        case "TYPE / SERIAL NO" :
                            antennaType = parseString(line, 0, 20);
                            try {
                                satelliteType = SatelliteType.parseSatelliteType(antennaType);
                                final String satField = parseString(line, 20, 20);
                                if (satField.length() > 0) {
                                    satelliteSystem = SatelliteSystem.parseSatelliteSystem(satField);
                                    final int n = parseInt(satField, 1, 19);
                                    switch (satelliteSystem) {
                                        case GPS:
                                        case GLONASS:
                                        case GALILEO:
                                        case BEIDOU:
                                        case IRNSS:
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
                            // ignoreds
                            break;
                        case "DAZI" :
                            azimuthStep = FastMath.toRadians(parseDouble(line,  2, 6));
                            break;
                        case "ZEN1 / ZEN2 / DZEN" :
                            polarStart = FastMath.toRadians(parseDouble(line,  2, 6));
                            polarStop  = FastMath.toRadians(parseDouble(line,  8, 6));
                            polarStep  = FastMath.toRadians(parseDouble(line, 14, 6));
                            break;
                        case "# OF FREQUENCIES" :
                            nbFrequencies = parseInt(line, 0, 6);
                            patterns      = new HashMap<>(nbFrequencies);
                            break;
                        case "VALID FROM" :
                            validFrom = new AbsoluteDate(parseInt(line,     0,  6),
                                                         parseInt(line,     6,  6),
                                                         parseInt(line,    12,  6),
                                                         parseInt(line,    18,  6),
                                                         parseInt(line,    24,  6),
                                                         parseDouble(line, 30, 13),
                                                         gps);
                            break;
                        case "VALID UNTIL" :
                            validUntil = new AbsoluteDate(parseInt(line,     0,  6),
                                                          parseInt(line,     6,  6),
                                                          parseInt(line,    12,  6),
                                                          parseInt(line,    18,  6),
                                                          parseInt(line,    24,  6),
                                                          parseDouble(line, 30, 13),
                                                          gps);
                            break;
                        case "SINEX CODE" :
                            sinexCode = parseString(line, 0, 10);
                            break;
                        case "START OF FREQUENCY" :
                            try {
                                frequency = Frequency.valueOf(parseString(line, 3, 3));
                                grid1D    = new double[1 + (int) FastMath.round((polarStop - polarStart) / polarStep)];
                                if (azimuthStep > 0.001) {
                                    grid2D = new double[1 + (int) FastMath.round(2 * FastMath.PI / azimuthStep)][grid1D.length];
                                }
                            } catch (IllegalArgumentException iae) {
                                throw new OrekitException(iae, OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                          parseString(line, 3, 3), name, lineNumber);
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
                        case "END OF FREQUENCY" : {
                            final String endFrequency = parseString(line, 3, 3);
                            if (frequency == null || !frequency.toString().equals(endFrequency)) {
                                throw new OrekitException(OrekitMessages.MISMATCHED_FREQUENCIES,
                                                          name, lineNumber, frequency, endFrequency);

                            }

                            // Check if the number of frequencies has been parsed
                            if (patterns == null) {
                                // null object, an OrekitException is thrown
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, name, line);
                            }

                            final PhaseCenterVariationFunction phaseCenterVariation;
                            if (grid2D == null) {
                                double max = 0;
                                for (final double v : grid1D) {
                                    max = FastMath.max(max, FastMath.abs(v));
                                }
                                if (max == 0.0) {
                                    // there are no known variations for this pattern
                                    phaseCenterVariation = (polarAngle, azimuthAngle) -> 0.0;
                                } else {
                                    phaseCenterVariation = new OneDVariation(polarStart, polarStep, grid1D);
                                }
                            } else {
                                phaseCenterVariation = new TwoDVariation(polarStart, polarStep, azimuthStep, grid2D);
                            }
                            patterns.put(frequency, new FrequencyPattern(eccentricities, phaseCenterVariation));
                            frequency   = null;
                            grid1D      = null;
                            grid2D      = null;
                            inFrequency = false;
                            break;
                        }
                        case "START OF FREQ RMS" :
                            inRMS = true;
                            break;
                        case "END OF FREQ RMS" :
                            inRMS = false;
                            break;
                        case "END OF ANTENNA" :
                            if (satelliteType == null) {
                                addReceiverAntenna(new ReceiverAntenna(antennaType, sinexCode, patterns, serialNumber));
                            } else {
                                addSatelliteAntenna(new SatelliteAntenna(antennaType, sinexCode, patterns,
                                                                         satelliteSystem, prnNumber,
                                                                         satelliteType, satelliteCode,
                                                                         cosparID, validFrom, validUntil));
                            }
                            break;
                        default :
                            if (inFrequency) {
                                final String[] fields = SEPARATOR.split(line.trim());
                                if (fields.length != grid1D.length + 1) {
                                    throw new OrekitException(OrekitMessages.WRONG_COLUMNS_NUMBER,
                                                              name, lineNumber, grid1D.length + 1, fields.length);
                                }
                                if ("NOAZI".equals(fields[0])) {
                                    // azimuth-independent phase
                                    for (int i = 0; i < grid1D.length; ++i) {
                                        grid1D[i] = Double.parseDouble(fields[i + 1]) * MM_TO_M;
                                    }

                                } else {
                                    // azimuth-dependent phase
                                    final int k = (int) FastMath.round(FastMath.toRadians(Double.parseDouble(fields[0])) / azimuthStep);
                                    for (int i = 0; i < grid2D[k].length; ++i) {
                                        grid2D[k][i] = Double.parseDouble(fields[i + 1]) * MM_TO_M;
                                    }
                                }
                            } else if (inRMS) {
                                // RMS section is ignored (furthermore there are no RMS sections in both igs08.atx and igs14.atx)
                            } else {
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, name, line);
                            }
                    }
                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, "tot");
            }

        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final String line, final int start, final int length) {
            return line.substring(start, FastMath.min(line.length(), start + length)).trim();
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
