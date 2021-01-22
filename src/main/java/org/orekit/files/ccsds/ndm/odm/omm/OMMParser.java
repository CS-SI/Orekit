/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.omm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.OStateParser;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.files.ccsds.utils.state.ProcessingState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OMM (Orbiter Mean-Elements Message).
 * @author sports
 * @since 6.1
 */
public class OMMParser extends OStateParser<OMMFile, OMMParser> {

    /** Mandatory keywords.
     * @since 10.1
     */
    private static final Keyword[] MANDATORY_KEYWORDS = {
        Keyword.CCSDS_OMM_VERS, Keyword.CREATION_DATE, Keyword.ORIGINATOR,
        Keyword.OBJECT_NAME, Keyword.OBJECT_ID, Keyword.CENTER_NAME,
        Keyword.REF_FRAME, Keyword.TIME_SYSTEM, Keyword.MEAN_ELEMENT_THEORY,
        Keyword.EPOCH, Keyword.SEMI_MAJOR_AXIS, Keyword.MEAN_MOTION,
        Keyword.ECCENTRICITY, Keyword.INCLINATION, Keyword.RA_OF_ASC_NODE,
        Keyword.ARG_OF_PERICENTER, Keyword.MEAN_ANOMALY
    };

    /** Simple constructor.
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
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public OMMParser() {
        this(DataContext.getDefault());
    }

    /** Constructor with data context.
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
     *
     * @param dataContext used by the parser.
     *
     * @see #OMMParser()
     * @see #withDataContext(DataContext)
     * @since 10.1
     */
    public OMMParser(final DataContext dataContext) {
        this(null, true, dataContext, null, AbsoluteDate.FUTURE_INFINITY, Double.NaN);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param initialState initial parsing state
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     */
    private OMMParser(final IERSConventions conventions, final boolean simpleEOP,
                      final DataContext dataContext, final ProcessingState initialState,
                      final AbsoluteDate missionReferenceDate, final double mu) {
        super(conventions, simpleEOP, dataContext, initialState, missionReferenceDate, mu);
    }

    /** {@inheritDoc} */
    @Override
    protected OMMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final ProcessingState newInitialState,
                               final AbsoluteDate newMissionReferenceDate,
                               final double newMu) {
        return new OMMParser(newConventions, newSimpleEOP, newDataContext, newInitialState,
                             newMissionReferenceDate, newMu);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final OCommonMetadata metadata,
                                         final int lineNumber, final String fileName, final String line) {
        if (keyValue.getKeyword() == Keyword.MEAN_ELEMENT_THEORY) {
            ((OMMMetadata) metadata).setMeanElementTheory(keyValue.getValue());
            return true;
        } else {
            return super.parseMetaDataEntry(keyValue, metadata, lineNumber, fileName, line);
        }
    }

    /** {@inheritDoc} */
    @Override
    public OMMFile oldParse(final InputStream stream, final String fileName) {

        // declare the mandatory keywords as expected
        for (final Keyword keyword : MANDATORY_KEYWORDS) {
            declareExpected(keyword);
        }

        try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo(getConventions(), getDataContext());
            pi.fileName = fileName;
            pi.parsingHeader = true;

            for (pi.line = reader.readLine(); pi.line != null; pi.line = reader.readLine()) {
                ++pi.lineNumber;
                if (pi.line.trim().length() == 0) {
                    continue;
                }
                pi.entry = new KeyValue(pi.line, pi.lineNumber, pi.fileName);
                if (pi.entry.getKeyword() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                              pi.lineNumber, pi.fileName, pi.line);
                }

                declareFound(pi.entry.getKeyword());

                switch (pi.entry.getKeyword()) {

                    case COMMENT:
                        if (pi.file.getHeader().getCreationDate() == null) {
                            pi.file.getHeader().addComment(pi.entry.getValue());
                        } else if (pi.metadata.getObjectName() == null) {
                            pi.metadata.addComment(pi.entry.getValue());
                        } else {
                            pi.commentTmp.add(pi.entry.getValue());
                        }
                        break;

                    case CCSDS_OMM_VERS:
                        pi.file.getHeader().setFormatVersion(pi.entry.getDoubleValue());
                        break;

                    case MEAN_MOTION:
                        // as we have found mean motion, we don't expect semi majar axis anymore
                        declareFound(Keyword.SEMI_MAJOR_AXIS);
                        pi.data.setMeanMotion(pi.entry.getDoubleValue() * FastMath.PI / 43200.0);
                        break;

                    case EPHEMERIS_TYPE:
                        pi.data.setTLERelatedParametersComment(pi.commentTmp);
                        pi.commentTmp.clear();
                        pi.data.setEphemerisType(Integer.parseInt(pi.entry.getValue()));
                        break;

                    case CLASSIFICATION_TYPE:
                        pi.data.setClassificationType(pi.entry.getValue().charAt(0));
                        break;

                    case NORAD_CAT_ID:
                        pi.data.setNoradID(Integer.parseInt(pi.entry.getValue()));
                        break;

                    case ELEMENT_SET_NO:
                        pi.data.setElementSetNo(pi.entry.getValue());
                        break;

                    case REV_AT_EPOCH:
                        pi.data.setRevAtEpoch(Integer.parseInt(pi.entry.getValue()));
                        break;

                    case BSTAR:
                        pi.data.setbStar(pi.entry.getDoubleValue());
                        break;

                    case MEAN_MOTION_DOT:
                        pi.data.setMeanMotionDot(pi.entry.getDoubleValue() * FastMath.PI / 1.86624e9);
                        break;

                    case MEAN_MOTION_DDOT:
                        pi.data.setMeanMotionDotDot(pi.entry.getDoubleValue() *
                                                    FastMath.PI / 5.3747712e13);
                        break;

                    default:
                        boolean parsed = false;;
                        if (pi.parsingHeader) {
                            parsed = parseHeaderEntry(pi.entry, pi.file);
                            if (!parsed) {
                                pi.parsingHeader   = false;
                                pi.parsingMetaData = true;
                            }
                        }
                        if (pi.parsingMetaData) {
                            parsed = parseMetaDataEntry(pi.entry, pi.metadata,
                                                        pi.lineNumber, pi.fileName, pi.line);
                            if (!parsed) {
                                pi.parsingMetaData = false;
                                pi.parsingData     = true;
                            }
                        }
                        if (pi.parsingData) {
                            parsed = parseGeneralStateDataEntry(pi.entry, pi.metadata, pi.data, pi.commentTmp,
                                                                pi.lineNumber, pi.fileName, pi.line);
                        }
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                                      pi.lineNumber, pi.fileName, pi.line);
                        }
                }
            }

            // check all mandatory keywords have been found
            checkExpected(fileName);

            pi.file.addSegment(new NDMSegment<>(pi.metadata, pi.data));
            pi.file.setMu(getSelectedMu());

            return pi.file;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Private class used to store OMM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OMM file being read. */
        private OMMFile file;

        /** OMM metadata being read. */
        private OMMMetadata metadata;

        /** OMM data being read. */
        private OMMData data;

        /** Boolean indicating if the parser is currently parsing a header block. */
        private boolean parsingHeader;

        /** Boolean indicating if the parser is currently parsing a meta-data block. */
        private boolean parsingMetaData;

        /** Boolean indicating if the parser is currently parsing a data block. */
        private boolean parsingData;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Key value of the line being read. */
        private KeyValue entry;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object.
         * @param conventions IERS conventions to use
         * @param dataContext data context to use
         */
        protected ParseInfo(final IERSConventions conventions, final DataContext dataContext) {
            file            = new OMMFile(conventions, dataContext);
            metadata        = new OMMMetadata();
            data            = new OMMData();
            parsingHeader   = false;
            parsingMetaData = false;
            parsingData     = false;
            lineNumber = 0;
            commentTmp = new ArrayList<>();
        }
    }
}
