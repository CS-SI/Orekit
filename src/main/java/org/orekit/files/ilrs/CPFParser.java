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
package org.orekit.files.ilrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CPF orbit file format.
 * <p>
 * It supports both 1.0 and 2.0 versions
 * <p>
 * <b>Note:</b> Only required header keys are read. Furthermore, only position data are read.
 * Other keys are simply ignored
 * Contributions are welcome to support more fields in the format.
 * </p>
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2006/cpf_1.01.pdf">1.0 file format</a>
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2018/cpf_2.00h-1.pdf">2.0 file format</a>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CPFParser implements EphemerisFileParser<CPF> {

    /** File format. */
    private static final String FILE_FORMAT = "CPF";

    /** Miscroseconds to seconds converter. */
    private static final double MS_TO_S = 1.0e-6;

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Default number of sample for interpolating data (See: reference documents. */
    private static final int DEFAULT_INTERPOLATION_SAMPLE = 10;

    /** Standard gravitational parameter in m^3 / s^2. */
    private final double mu;

    /** Time scale used to define epochs in CPF file. */
    private final TimeScale timeScale;

    /** Set of frames. */
    private final Frames frames;

    /** Interpolation sample for data interpolating. */
    private final int interpolationSample;

    /** IERS convention for frames. */
    private final IERSConventions iersConvention;

    /**
     * Default constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}.
     */
    @DefaultDataContext
    public CPFParser() {
        this(Constants.EIGEN5C_EARTH_MU, DEFAULT_INTERPOLATION_SAMPLE,
             IERSConventions.IERS_2010, DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames());
    }

    /**
     * Constructor.
     * @param mu standard gravitational parameter to use for
     *           creating {@link org.orekit.orbits.Orbit Orbits} from
     *           the ephemeris data.
     * @param interpolationSamples number of samples to use when interpolating
     * @param iersConventions IERS convention for frames definition
     * @param utc time scale used to define epochs in CPF files (UTC)
     * @param frames set of frames for satellite coordinates
     */
    public CPFParser(final double mu,
                     final int interpolationSamples,
                     final IERSConventions iersConventions,
                     final TimeScale utc,
                     final Frames frames) {
        this.mu                  = mu;
        this.interpolationSample = interpolationSamples;
        this.iersConvention      = iersConventions;
        this.timeScale           = utc;
        this.frames              = frames;
    }

    /** {@inheritDoc} */
    @Override
    public CPF parse(final DataSource source) {

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = (reader == null) ? null : new BufferedReader(reader)) {

            if (br == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, source.getName());
            }

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();

            int lineNumber = 0;
            Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.H1);
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    for (final LineParser candidate : candidateParsers) {
                        if (candidate.canHandle(line)) {
                            try {

                                candidate.parse(line, pi);

                                if (pi.done) {
                                    pi.file.setFilter(pi.hasVelocityEntries ?
                                                      CartesianDerivativesFilter.USE_PV :
                                                      CartesianDerivativesFilter.USE_P);
                                    // Return file
                                    return pi.file;
                                }

                                candidateParsers = candidate.allowedNext();
                                continue nextLine;

                            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, source.getName(), line);
                            }
                        }
                    }

                }

            // We never reached the EOF marker
            throw new OrekitException(OrekitMessages.CPF_UNEXPECTED_END_OF_FILE, lineNumber);

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }

    }

    /** Transient data used for parsing a CPF file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a CPF file.</p>
     */
    private class ParseInfo {

        /** The corresponding CPF file. */
        private CPF file;

        /** IERS convention. */
        private IERSConventions convention;

        /** Set of frames. */
        private Frames frames;

        /** Frame for the ephemeris data. */
        private Frame frame;

        /** Time scale. */
        private TimeScale timeScale;

        /** Indicates if the SP3 file has velocity entries. */
        private boolean hasVelocityEntries;

        /** End Of File reached indicator. */
        private boolean done;

        /**
         * Constructor.
         */
        protected ParseInfo() {

            // Initialise file
            file = new CPF();

            // Time scale
            this.timeScale = CPFParser.this.timeScale;

            // Initialise fields
            file.setMu(mu);
            file.setInterpolationSample(interpolationSample);
            file.setTimeScale(timeScale);

            // Default values
            this.done               = false;
            this.hasVelocityEntries = false;

            // Default value for reference frame
            this.convention = CPFParser.this.iersConvention;
            this.frames     = CPFParser.this.frames;
            frame           = frames.getITRF(convention, false);

        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Header first line. */
        H1("H1") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Index for reading data.
                // Allow taking into consideration difference between 1.0 and 2.0 formats
                int index = 1;

                // Format
                final String format = values[index++];

                // Throw an exception if format is not equal to "CPF"
                if (!FILE_FORMAT.equals(format)) {
                    throw new OrekitException(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE, FILE_FORMAT, format);
                }

                // Fill first elements
                pi.file.getHeader().setFormat(format);
                pi.file.getHeader().setVersion(Integer.parseInt(values[index++]));
                pi.file.getHeader().setSource(values[index++]);

                // Epoch of ephemeris production
                final int year  = Integer.parseInt(values[index++]);
                final int month = Integer.parseInt(values[index++]);
                final int day   = Integer.parseInt(values[index++]);
                pi.file.getHeader().setProductionEpoch(new DateComponents(year, month, day));

                // Hour of ephemeris production
                pi.file.getHeader().setProductionHour(Integer.parseInt(values[index++]));

                // Ephemeris sequence number
                pi.file.getHeader().setSequenceNumber(Integer.parseInt(values[index++]));

                // Difference between version 1.0 and 2.0: sub-daily ephemeris sequence number
                if (pi.file.getHeader().getVersion() == 2) {
                    pi.file.getHeader().setSubDailySequenceNumber(Integer.parseInt(values[index++]));
                }

                // Target Name
                pi.file.getHeader().setName(values[index]);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H2, ZERO);
            }

        },

        /** Header second line. */
        H2("H2") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Identifiers
                pi.file.getHeader().setIlrsSatelliteId(values[1]);
                pi.file.getHeader().setSic(values[2]);
                pi.file.getHeader().setNoradId(values[3]);

                // Start epoch
                final int    yearS   = Integer.parseInt(values[4]);
                final int    monthS  = Integer.parseInt(values[5]);
                final int    dayS    = Integer.parseInt(values[6]);
                final int    hourS   = Integer.parseInt(values[7]);
                final int    minuteS = Integer.parseInt(values[8]);
                final double secondS = Integer.parseInt(values[9]);

                pi.file.getHeader().setStartEpoch(new AbsoluteDate(yearS, monthS, dayS,
                                                                   hourS, minuteS, secondS,
                                                                   pi.file.getTimeScale()));

                // End epoch
                final int    yearE   = Integer.parseInt(values[10]);
                final int    monthE  = Integer.parseInt(values[11]);
                final int    dayE    = Integer.parseInt(values[12]);
                final int    hourE   = Integer.parseInt(values[13]);
                final int    minuteE = Integer.parseInt(values[14]);
                final double secondE = Integer.parseInt(values[15]);

                pi.file.getHeader().setEndEpoch(new AbsoluteDate(yearE, monthE, dayE,
                                                                 hourE, minuteE, secondE,
                                                                 pi.file.getTimeScale()));

                // Time between table entries
                pi.file.getHeader().setStep(Integer.parseInt(values[16]));

                // Compatibility with TIVs
                pi.file.getHeader().setIsCompatibleWithTIVs(Integer.parseInt(values[17]) == 1);

                // Target class
                pi.file.getHeader().setTargetClass(Integer.parseInt(values[18]));

                // Reference frame
                final int frameId = Integer.parseInt(values[19]);
                switch (frameId) {
                    case 0:
                        pi.frame = pi.frames.getITRF(pi.convention, false);
                        break;
                    case 1:
                        pi.frame = pi.frames.getTOD(true);
                        break;
                    case 2:
                        pi.frame = pi.frames.getMOD(pi.convention);
                        break;
                    default:
                        pi.frame = pi.frames.getITRF(pi.convention, false);
                        break;
                }
                pi.file.getHeader().setRefFrame(pi.frame);
                pi.file.getHeader().setRefFrameId(frameId);

                // Last fields
                pi.file.getHeader().setRotationalAngleType(Integer.parseInt(values[20]));
                pi.file.getHeader().setIsCenterOfMassCorrectionApplied(Integer.parseInt(values[21]) == 1);
                if (pi.file.getHeader().getVersion() == 2) {
                    pi.file.getHeader().setTargetLocation(Integer.parseInt(values[22]));
                }

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, H9, ZERO);
            }

        },

        /** Header third line. */
        H3("H3") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H4, H5, H9, ZERO);
            }

        },

        /** Header fourth line. */
        H4("H4") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Pulse Repetition Frequency (PRF)
                pi.file.getHeader().setPrf(Double.parseDouble(values[1]));

                // Transponder information
                pi.file.getHeader().setTranspTransmitDelay(Double.parseDouble(values[2]) * MS_TO_S);
                pi.file.getHeader().setTranspUtcOffset(Double.parseDouble(values[3]) * MS_TO_S);
                pi.file.getHeader().setTranspOscDrift(Double.parseDouble(values[4]));
                if (pi.file.getHeader().getVersion() == 2) {
                    pi.file.getHeader().setTranspClkRef(Double.parseDouble(values[5]));
                }

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H5, H9, ZERO);
            }

        },

        /** Header fifth line. */
        H5("H5") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Approximate center of mass to reflector offset in meters
                final double offset = Double.parseDouble(SEPARATOR.split(line)[1]);
                pi.file.getHeader().setCenterOfMassOffset(offset);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H9, ZERO);
            }

        },

        /** Header last line. */
        H9("H9") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // End of header. Nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, ZERO);
            }

        },

        /** Position values. */
        TEN("10") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Epoch
                final int mjd           = Integer.parseInt(values[2]);
                final double secInDay   = Double.parseDouble(values[3]);
                final AbsoluteDate date = AbsoluteDate.createMJDDate(mjd, secInDay, pi.timeScale);

                // Leap second flag
                final int leap = Integer.parseInt(values[4]);

                // Coordinates
                final double x = Double.parseDouble(values[5]);
                final double y = Double.parseDouble(values[6]);
                final double z = Double.parseDouble(values[7]);
                final Vector3D position = new Vector3D(x, y, z);

                // CPF coordinate
                final CPF.CPFCoordinate coordinate = new CPF.CPFCoordinate(date, position, leap);
                pi.file.addSatelliteCoordinate(pi.file.getHeader().getIlrsSatelliteId(), coordinate);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Velocity values. */
        TWENTY("20") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Coordinates
                final double x = Double.parseDouble(values[2]);
                final double y = Double.parseDouble(values[3]);
                final double z = Double.parseDouble(values[4]);
                final Vector3D velocity = new Vector3D(x, y, z);

                // CPF coordinate
                pi.file.addSatelliteVelocityToCPFCoordinate(pi.file.getHeader().getIlrsSatelliteId(), velocity);
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Corrections. */
        THIRTY("30") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Transponder specific. */
        FORTY("40") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Offset from center of main body. */
        FIFTY("50") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Rotation angle of offset. */
        SIXTY("60") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Earth orientation. */
        SEVENTY("70") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Comments. */
        ZERO("00") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Comment
                final String comment = line.split(getIdentifier())[1].trim();
                pi.file.getComments().add(comment);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H1, H2, H3, H4, H5, H9,
                                     TEN, TWENTY, THIRTY, FORTY, FIFTY, SIXTY, SEVENTY, ZERO, EOF);
            }

        },

        /** Last record in ephemeris. */
        EOF("99") {

            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.done = true;
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(EOF);
            }

        };

        /** Pattern for identifying line. */
        private final Pattern pattern;

        /** Identifier. */
        private final String identifier;

        /** Simple constructor.
         * @param identifier regular expression for identifying line (i.e. first element)
         */
        LineParser(final String identifier) {
            this.identifier = identifier;
            pattern = Pattern.compile(identifier);
        }

        /**
         * Get the regular expression for identifying line.
         * @return the regular expression for identifying line
         */
        public String getIdentifier() {
            return identifier;
        }

        /** Parse a line.
         * @param line line to parse
         * @param pi holder for transient data
         */
        public abstract void parse(String line, ParseInfo pi);

        /** Get the allowed parsers for next line.
         * @return allowed parsers for next line
         */
        public abstract Iterable<LineParser> allowedNext();

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(SEPARATOR.split(line)[0]).matches();
        }

    }

}
