/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.adm.aem.AEMMetadata;
import org.orekit.files.ccsds.ndm.odm.OCommonParser;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.files.general.EphemerisFileParser;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * A parser for the CCSDS OEM (Orbit Ephemeris Message).
 * @author sports
 * @since 6.1
 */
public class OEMParser extends OCommonParser<OEMFile, OEMParser> implements EphemerisFileParser {

    /** Mandatory keywords.
     * @since 11.0
     */
    private static final Keyword[] MANDATORY_KEYWORDS = {
        Keyword.CCSDS_OEM_VERS, Keyword.CREATION_DATE, Keyword.ORIGINATOR,
        Keyword.OBJECT_NAME, Keyword.OBJECT_ID, Keyword.CENTER_NAME,
        Keyword.REF_FRAME, Keyword.TIME_SYSTEM,
        Keyword.START_TIME, Keyword.STOP_TIME, Keyword.META_START, Keyword.META_STOP
    };

    /** Default interpolation degree. */
    private int interpolationDegree;

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as reference date for Mission Elapsed Time or
     * Mission Relative Time time systems, or the gravitational coefficient or
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
     * </p>
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The gravitational coefficient is not set here. If it is needed in order
     * to parse Cartesian orbits where the value is not set in the CCSDS file, it must
     * be initialized before parsing by calling {@link #withMu(double)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     * <p>
     * The international designator parameters (launch year, launch number and
     * launch piece) are not set here. If they are needed, they must be initialized before
     * parsing by calling {@link #withInternationalDesignator(int, int, String)}
     * </p>
     * <p>
     * The default interpolation degree is not set here. It is set to one by default. If another value
     * is needed it must be initialized before parsing by calling {@link #withInterpolationDegree(int)}
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public OEMParser() {
        this(DataContext.getDefault());
    }

    /** Constructor with data context.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as reference date for Mission Elapsed Time or
     * Mission Relative Time time systems, or the gravitational coefficient or
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
     * </p>
     * <p>
     * The initial date for Mission Elapsed Time and Mission Relative Time time systems is not set here.
     * If such time systems are used, it must be initialized before parsing by calling {@link
     * #withMissionReferenceDate(AbsoluteDate)}.
     * </p>
     * <p>
     * The gravitational coefficient is not set here. If it is needed in order
     * to parse Cartesian orbits where the value is not set in the CCSDS file, it must
     * be initialized before parsing by calling {@link #withMu(double)}.
     * </p>
     * <p>
     * The IERS conventions to use is not set here. If it is needed in order to
     * parse some reference frames or UT1 time scale, it must be initialized before
     * parsing by calling {@link #withConventions(IERSConventions)}.
     * </p>
     * <p>
     * The international designator parameters (launch year, launch number and
     * launch piece) are not set here. If they are needed, they must be initialized before
     * parsing by calling {@link #withInternationalDesignator(int, int, String)}
     * </p>
     * <p>
     * The default interpolation degree is not set here. It is set to one by default. If another value
     * is needed it must be initialized before parsing by calling {@link #withInterpolationDegree(int)}
     * </p>
     *
     * @param dataContext used by the parser.
     *
     * @see #OEMParser()
     * @see #withDataContext(DataContext)
     * @since 10.1
     */
    public OEMParser(final DataContext dataContext) {
        this(null, true, dataContext, AbsoluteDate.FUTURE_INFINITY, Double.NaN, 1);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param interpolationDegree interpolation degree
     */
    private OEMParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                      final AbsoluteDate missionReferenceDate, final double mu,
                      final int interpolationDegree) {
        super(conventions, simpleEOP, dataContext, missionReferenceDate, mu);
        this.interpolationDegree = interpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    protected OEMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final AbsoluteDate newMissionReferenceDate,
                               final double newMu) {
        return create(newConventions, newSimpleEOP, newDataContext,
                      newMissionReferenceDate, newMu,
                      interpolationDegree);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @param newMu gravitational coefficient to use while parsing
     * @param newInterpolationDegree interpolation degree
     * @return a new instance with changed parameters
     */
    protected OEMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final AbsoluteDate newMissionReferenceDate,
                               final double newMu,
                               final int newInterpolationDegree) {
        return new OEMParser(newConventions, newSimpleEOP, newDataContext,
                             newMissionReferenceDate, newMu,
                             newInterpolationDegree);
    }

    /** Set default interpolation degree.
     * <p>
     * This method may be used to set a default interpolation degree which will be used
     * when no interpolation degree is parsed in the meta-data of the file. Upon instantiation
     * with {@link #OEMParser(DataContext)} the default interpolation degree is one.
     * </p>
     * @param newInterpolationDegree default interpolation degree to use while parsing
     * @return a new instance, with interpolation degree data replaced
     * @see #getInterpolationDegree()
     * @since 11.0
     */
    public OEMParser withInterpolationDegree(final int newInterpolationDegree) {
        return new OEMParser(getConventions(), isSimpleEOP(), getDataContext(), getMissionReferenceDate(),
                             getMuSet(), newInterpolationDegree);
    }

    /** Get default interpolation degree.
     * @return interpolationDegree default interpolation degree to use while parsing
     * @see #withInterpolationDegree(int)
     * @since 11.0
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public OEMFile parse(final InputStream stream, final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, fileName);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public OEMFile parse(final BufferedReader reader, final String fileName) {

        // declare the mandatory keywords as expected
        for (final Keyword keyword : MANDATORY_KEYWORDS) {
            declareExpected(keyword);
        }

        try {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo(getConventions(), getDataContext());
            pi.fileName      = fileName;
            pi.parsingHeader = true;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    if (pi.parsingData) {
                        parseEphemeridesDataLine(line, pi);
                        continue;
                    } else {
                        throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                    }
                }

                declareFound(pi.keyValue.getKeyword());

                switch (pi.keyValue.getKeyword()) {
                    case CCSDS_OEM_VERS:
                        pi.file.getHeader().setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case META_START:
                        // Indicate the start of meta-data parsing for this block
                        pi.metadata          = new OEMMetadata(getConventions(), getDataContext());
                        pi.parsingHeader     = false;
                        pi.parsingMetaData   = true;
                        pi.parsingData       = false;
                        pi.parsingCovariance = false;
                        pi.metadata.setInterpolationDegree(getInterpolationDegree());
                        break;

                    case START_TIME:
                        pi.metadata.setStartTime(parseDate(pi.keyValue.getValue(),
                                                           pi.metadata.getTimeSystem()));
                        break;

                    case USEABLE_START_TIME:
                        pi.metadata.setUseableStartTime(parseDate(pi.keyValue.getValue(),
                                                                  pi.metadata.getTimeSystem()));
                        break;

                    case USEABLE_STOP_TIME:
                        pi.metadata.setUseableStopTime(parseDate(pi.keyValue.getValue(), pi.metadata.getTimeSystem()));
                        break;

                    case STOP_TIME:
                        pi.metadata.setStopTime(parseDate(pi.keyValue.getValue(), pi.metadata.getTimeSystem()));
                        break;

                    case INTERPOLATION:
                        pi.metadata.setInterpolationMethod(pi.keyValue.getValue());
                        break;

                    case INTERPOLATION_DEGREE:
                        pi.metadata.setInterpolationDegree(Integer.parseInt(pi.keyValue.getValue()));
                        break;

                    case META_STOP:
                        pi.parsingMetaData = false;
                        pi.parsingData     = true;
                        break;

                    case COVARIANCE_START:
                        pi.parsingData       = false;
                        pi.parsingCovariance = true;
                        break;

                    case COVARIANCE_STOP:
                        pi.parsingCovariance = false;
                        pi.parsingData       = true;
                        break;

                    default:
                        final boolean parsed;
                        if (pi.parsingHeader) {
                            parsed = parseHeaderEntry(pi.keyValue, pi.file);
                        } else if (pi.parsingMetaData) {
                            parsed = parseMetaDataEntry(pi.keyValue, pi.metadata);
                        } else if (pi.parsingData && pi.keyValue.getKeyword() == Keyword.COMMENT) {
                            pi.currentEphemeridesBlock.addComment(pi.keyValue.getValue());
                            parsed = true;
                        } else if (pi.parsingCovariance && pi.keyValue.getKeyword() == Keyword.COMMENT) {
                            pi.currentEphemeridesBlock.addComment(pi.keyValue.getValue());
                            parsed = true;
                        } else {
                            parsed = false;
                        }
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                        }
                }
            }

            // check all mandatory keywords have been found
            checkExpected(fileName);

            pi.file.checkTimeSystems();
            return pi.file;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Parse an ephemeris data line and add its content to the ephemerides
     * block.
     *
     * @param reader the reader
     * @param pi the parser info
     * @exception IOException if an error occurs while reading from the stream
     */
    private void parseEphemeridesDataLines(final BufferedReader reader,  final ParseInfo pi)
        throws IOException {

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.trim().length() > 0) {
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    try (Scanner sc = new Scanner(line)) {
                        final AbsoluteDate date = parseDate(sc.next(), pi.lastEphemeridesBlock.getMetadata().getTimeSystem());
                        final Vector3D position = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000);
                        final Vector3D velocity = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000,
                                                               Double.parseDouble(sc.next()) * 1000);
                        Vector3D acceleration = Vector3D.NaN;
                        boolean hasAcceleration = false;
                        if (sc.hasNext()) {
                            acceleration = new Vector3D(Double.parseDouble(sc.next()) * 1000,
                                                        Double.parseDouble(sc.next()) * 1000,
                                                        Double.parseDouble(sc.next()) * 1000);
                            hasAcceleration = true;
                        }
                        final TimeStampedPVCoordinates epDataLine;
                        if (hasAcceleration) {
                            epDataLine = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
                        } else {
                            epDataLine = new TimeStampedPVCoordinates(date, position, velocity);
                        }
                        pi.lastEphemeridesBlock.getEphemeridesDataLines().add(epDataLine);
                        pi.lastEphemeridesBlock.updateHasAcceleration(hasAcceleration);
                    } catch (NumberFormatException nfe) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.fileName, line);
                    }
                } else {
                    switch (pi.keyValue.getKeyword()) {
                        case META_START:
                            pi.lastEphemeridesBlock.setEphemeridesDataLinesComment(pi.commentTmp);
                            pi.commentTmp.clear();
                            pi.lineNumber--;
                            reader.reset();
                            return;
                        case COVARIANCE_START:
                            pi.lastEphemeridesBlock.setEphemeridesDataLinesComment(pi.commentTmp);
                            pi.commentTmp.clear();
                            pi.lineNumber--;
                            reader.reset();
                            return;
                        case COMMENT:
                            pi.commentTmp.add(pi.keyValue.getValue());
                            break;
                        default :
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                    }
                }
            }
            reader.mark(300);

        }
    }

    /**
     * Parse the covariance data lines, create a set of CovarianceMatrix objects
     * and add them in the covarianceMatrices list of the ephemerides block.
     *
     * @param reader the reader
     * @param pi the parser info
     * @throws IOException if an error occurs while reading from the stream
     */
    private void parseCovarianceDataLines(final BufferedReader reader, final ParseInfo pi)
        throws IOException {
        int i = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {

            ++pi.lineNumber;
            if (line.trim().length() == 0) {
                continue;
            }
            pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
            if (pi.keyValue.getKeyword() == null) {
                try (Scanner sc = new Scanner(line)) {
                    for (int j = 0; j < i + 1; j++) {
                        pi.lastMatrix.addToEntry(i, j, Double.parseDouble(sc.next()));
                        if (j != i) {
                            pi.lastMatrix.addToEntry(j, i, pi.lastMatrix.getEntry(i, j));
                        }
                    }
                    if (i == 5) {
                        final OEMFile.CovarianceMatrix cm =
                                new OEMFile.CovarianceMatrix(pi.epoch, pi.covRefLofType, pi.covRefFrame, pi.lastMatrix);
                        pi.lastEphemeridesBlock.getCovarianceMatrices().add(cm);
                    }
                    i++;
                } catch (NumberFormatException nfe) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              pi.lineNumber, pi.fileName, line);
                }
            } else {
                switch (pi.keyValue.getKeyword()) {
                    case EPOCH :
                        i                = 0;
                        pi.covRefLofType = null;
                        pi.covRefFrame   = null;
                        pi.lastMatrix    = MatrixUtils.createRealMatrix(6, 6);
                        pi.epoch         = parseDate(pi.keyValue.getValue(), pi.lastEphemeridesBlock.getMetadata().getTimeSystem());
                        break;
                    case COV_REF_FRAME :
                        final CCSDSFrame frame = parseCCSDSFrame(pi.keyValue.getValue());
                        if (frame.isLof()) {
                            pi.covRefLofType = frame.getLofType();
                            pi.covRefFrame   = null;
                        } else {
                            pi.covRefLofType = null;
                            pi.covRefFrame   = frame.getFrame(getConventions(), isSimpleEOP(), getDataContext());
                        }
                        break;
                    case COVARIANCE_STOP :
                        return;
                    default :
                        throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }
            }
        }
    }

    /** Private class used to store OEM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OEM file being read. */
        private OEMFile file;

        /** OEM metadata being read. */
        private OEMMetadata metadata;

        /** OEM data being read. */
        private OEMData data;

        /** Boolean indicating if the parser is currently parsing a header block. */
        private boolean parsingHeader;

        /** Boolean indicating if the parser is currently parsing a meta-data block. */
        private boolean parsingMetaData;

        /** Boolean indicating if the parser is currently parsing a data block. */
        private boolean parsingData;

        /** Boolean indicating if the parser is currently parsing a covariance block. */
        private boolean parsingCovariance;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Stored epoch. */
        private AbsoluteDate epoch;

        /** Covariance reference type of Local Orbital Frame. */
        private LOFType covRefLofType;

        /** Covariance reference frame. */
        private Frame covRefFrame;
        /** Stored matrix. */
        private RealMatrix lastMatrix;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object.
         * @param conventions IERS conventions to use
         * @param dataContext data context to use
         */
        protected ParseInfo(final IERSConventions conventions, final DataContext dataContext) {
            file            = new OEMFile();
            file.setConventions(conventions);
            file.setDataContext(dataContext);
            metadata        = new OEMMetadata(conventions, dataContext);
            data            = new OEMData();
            parsingHeader   = false;
            parsingMetaData = false;
            parsingData     = false;
            lineNumber      = 0;
            commentTmp      = new ArrayList<String>();
        }
    }
}
