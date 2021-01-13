/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.odm.ODMParser;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OCM (Orbit Comprehensive Message).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OCMParser extends ODMParser<OCMFile, OCMParser> {

    /** Mandatory keywords. */
    private static final Keyword[] MANDATORY_KEYWORDS = {
        Keyword.CCSDS_OCM_VERS, Keyword.CREATION_DATE, Keyword.ORIGINATOR,
        Keyword.EPOCH_TZERO, Keyword.META_START, Keyword.META_STOP
    };

    /** Gravitational coefficient set by the user in the parser. */
    private double mu;

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as the gravitational coefficient or
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
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
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     * </p>
     */
    @DefaultDataContext
    public OCMParser() {
        this(DataContext.getDefault());
    }

    /** Simple constructor.
     * <p>
     * This class is immutable, and hence thread safe. When parts
     * must be changed, such as the gravitational coefficient or
     * the IERS conventions, the various {@code withXxx} methods must be called,
     * which create a new immutable instance with the new parameters. This
     * is a combination of the
     * <a href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
     * pattern</a> and a
     * <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
     * interface</a>.
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
     * @param dataContext used by the parser.
     * @see #withDataContext(DataContext)
     */
    public OCMParser(final DataContext dataContext) {
        this(null, true, dataContext, Double.NaN);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @param mu gravitational coefficient
     */
    private OCMParser(final IERSConventions conventions, final boolean simpleEOP,
                      final DataContext dataContext, final double mu) {
        super(conventions, simpleEOP, dataContext);
        this.mu = mu;
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient value replaced
     * @see #getMu()
     */
    public OCMParser withMu(final double newMu) {
        return create(getConventions(), isSimpleEOP(), getDataContext(), newMu);
    }

    /** {@inheritDoc} */
    @Override
    protected OCMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext) {
        return create(newConventions, newSimpleEOP, newDataContext, mu);
    }

    /** Build a new instance.
     * @param newConventions IERS conventions to use while parsing
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param newDataContext data context used for frames, time scales, and celestial bodies
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance with changed parameters
     */
    protected OCMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final double newMu) {
        return new OCMParser(newConventions, newSimpleEOP, newDataContext, newMu);
    }

    /** {@inheritDoc} */
    public OCMFile parse(final InputStream stream, final String fileName) {

        // declare the mandatory keywords as expected
        for (final Keyword keyword : MANDATORY_KEYWORDS) {
            declareExpected(keyword);
        }

        try (InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            // initialize internal data structures
            final ParseInfo pi = new ParseInfo(getConventions(), getDataContext());
            pi.fileName        = fileName;

            Section previousSection = Section.HEADER;
            Section section         = Section.HEADER;
            for (pi.line = reader.readLine(); pi.line != null; pi.line = reader.readLine()) {
                ++pi.lineNumber;
                if (pi.line.trim().length() == 0) {
                    continue;
                }
                pi.keyValue = new KeyValue(pi.line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    // we do not change section, maybe the current section can deal with null keywords
                    if (!section.parseEntry(this, pi)) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.fileName, pi.line);
                    }
                } else {
                    // there is a keyword in this line
                    declareFound(pi.keyValue.getKeyword());

                    switch (pi.keyValue.getKeyword()) {

                        case COMMENT:
                            switch (section) {
                                case HEADER:
                                    pi.file.getHeader().addComment(pi.keyValue.getValue());
                                    break;
                                case META_DATA :
                                    pi.metadata.addComment(pi.keyValue.getValue());
                                    break;
                                case ORBIT :
                                    pi.orbitalStateHistory.addComment(pi.keyValue.getValue());
                                    break;
                                case PHYSICS :
                                    // TODO
                                    break;
                                case COVARIANCE :
                                    // TODO
                                    break;
                                case MANEUVER :
                                    // TODO
                                    break;
                                case PERTURBATIONS :
                                    // TODO
                                    break;
                                case ORBIT_DETERMINATION :
                                    // TODO
                                    break;
                                case USER_DEFINED :
                                    // TODO
                                    break;
                                case UNDEFINED : default :
                                    // this should never happen
                                    throw new OrekitInternalError(null);
                            }
                            break;

                        case CCSDS_OCM_VERS:
                            pi.file.getHeader().setFormatVersion(pi.keyValue.getDoubleValue());
                            break;

                        case META_START :
                            checkAllowedSection(pi, previousSection, Section.HEADER);
                            previousSection = section;
                            section         = Section.META_DATA;
                            break;

                        case META_STOP :
                            checkAllowedSection(pi, section, Section.META_DATA);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case ORB_START : {
                            checkAllowedSection(pi, previousSection, Section.META_DATA, Section.ORBIT);
                            final AbsoluteDate defT0 = pi.metadata.getEpochT0();
                            final CcsdsTimeScale defTimSystem = pi.metadata.getTimeSystem();
                            pi.orbitalStateHistory = new OrbitalStateHistory(defT0, defTimSystem);
                            section = Section.ORBIT;
                            break;
                        }

                        case ORB_STOP : {
                            checkAllowedSection(pi, section, Section.ORBIT);
                            pi.data.add(pi.orbitalStateHistory);
                            pi.orbitalStateHistory = null;
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;
                        }

                        case PHYS_START :
                            checkAllowedSection(pi, previousSection, Section.META_DATA, Section.ORBIT);
                            section = Section.PHYSICS;
                            break;

                        case PHYS_STOP :
                            checkAllowedSection(pi, section, Section.PHYSICS);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case COV_START :
                            checkAllowedSection(pi, previousSection,
                                                Section.META_DATA, Section.ORBIT, Section.PHYSICS,
                                                Section.COVARIANCE);
                            section = Section.COVARIANCE;
                            break;

                        case COV_STOP :
                            checkAllowedSection(pi, section, Section.COVARIANCE);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case MAN_START :
                            checkAllowedSection(pi, previousSection,
                                                Section.META_DATA, Section.ORBIT, Section.PHYSICS,
                                                Section.COVARIANCE, Section.MANEUVER);
                            section = Section.MANEUVER;
                            break;

                        case MAN_STOP :
                            checkAllowedSection(pi, section, Section.MANEUVER);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case PERT_START :
                            checkAllowedSection(pi, previousSection,
                                                Section.META_DATA, Section.ORBIT, Section.PHYSICS,
                                                Section.COVARIANCE, Section.MANEUVER);
                            section = Section.PERTURBATIONS;
                            break;

                        case PERT_STOP :
                            checkAllowedSection(pi, section, Section.PERTURBATIONS);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case OD_START :
                            checkAllowedSection(pi, previousSection,
                                                Section.META_DATA, Section.ORBIT, Section.PHYSICS,
                                                Section.COVARIANCE, Section.MANEUVER,
                                                Section.PERTURBATIONS);
                            section = Section.ORBIT_DETERMINATION;
                            break;

                        case OD_STOP :
                            checkAllowedSection(pi, section, Section.ORBIT_DETERMINATION);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        case USER_START :
                            checkAllowedSection(pi, previousSection,
                                                Section.META_DATA, Section.ORBIT, Section.PHYSICS,
                                                Section.COVARIANCE, Section.MANEUVER,
                                                Section.PERTURBATIONS, Section.ORBIT_DETERMINATION);
                            section = Section.USER_DEFINED;
                            break;

                        case USER_STOP :
                            checkAllowedSection(pi, section, Section.USER_DEFINED);
                            previousSection = section;
                            section         = Section.UNDEFINED;
                            break;

                        default:
                            if (!section.parseEntry(this, pi)) {
                                throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                                          pi.lineNumber, pi.fileName, pi.line);
                            }
                    }

                }
            }

            // check all mandatory keywords have been found
            checkExpected(fileName);

            pi.file.addSegment(new NDMSegment<>(pi.metadata, pi.data));

            return pi.file;

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Check section is allowed at this place.
     * @param pi parse info
     * @param section current section
     * @param allowed allowed sections
     */
    private void checkAllowedSection(final ParseInfo pi, final Section section, final Section... allowed) {

        // compare with allowed sections
        for (final Section e : allowed) {
            if (section == e) {
                return;
            }
        }

        // the current section was not allowed here
        throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                  pi.lineNumber, pi.fileName, pi.keyValue.getLine());

    }

    /** Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metadata instance to update with parsed entry
     * @param lineNumber number of line being parsed
     * @param fileName name of the parsed file
     * @param line full parsed line
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final OCMMetadata metadata,
                                         final int lineNumber, final String fileName, final String line) {
        switch (keyValue.getKeyword()) {
            case CLASSIFICATION:
                metadata.setClassification(keyValue.getValue());
                return true;

            case ALTERNATE_NAMES:
                metadata.setAlternateNames(keyValue.getListValue());
                break;

            case OBJECT_DESIGNATOR:
                metadata.setObjectDesignator(keyValue.getValue());
                break;

            case INTERNATIONAL_DESIGNATOR:
                metadata.setInternationalDesignator(keyValue.getValue());
                break;

            case ORIGINATOR_POC:
                metadata.setOriginatorPOC(keyValue.getValue());
                break;

            case ORIGINATOR_POSITION:
                metadata.setOriginatorPosition(keyValue.getValue());
                break;

            case ORIGINATOR_PHONE:
                metadata.setOriginatorPhone(keyValue.getValue());
                break;

            case ORIGINATOR_ADDRESS:
                metadata.setOriginatorAddress(keyValue.getValue());
                break;

            case TECH_ORG:
                metadata.setTechOrg(keyValue.getValue());
                break;

            case TECH_POC:
                metadata.setTechPOC(keyValue.getValue());
                break;

            case TECH_POSITION:
                metadata.setTechPosition(keyValue.getValue());
                break;

            case TECH_PHONE:
                metadata.setTechPhone(keyValue.getValue());
                break;

            case TECH_ADDRESS:
                metadata.setTechAddress(keyValue.getValue());
                break;

            case PREVIOUS_MESSAGE_ID:
                metadata.setPreviousMessageID(keyValue.getValue());
                break;

            case NEXT_MESSAGE_ID:
                metadata.setNextMessageID(keyValue.getValue());
                break;

            case ADM_MESSAGE_LINK:
                metadata.setAttMessageLink(keyValue.getListValue());
                break;

            case CDM_MESSAGE_LINK:
                metadata.setCdmMessageLink(keyValue.getListValue());
                break;

            case PRM_MESSAGE_LINK:
                metadata.setPrmMessageLink(keyValue.getListValue());
                break;

            case RDM_MESSAGE_LINK:
                metadata.setRdmMessageLink(keyValue.getListValue());
                break;

            case CATALOG_NAME:
                metadata.setCatalogName(keyValue.getValue());
                break;

            case OPERATOR:
                metadata.setOperator(keyValue.getValue());
                break;

            case OWNER:
                metadata.setOwner(keyValue.getValue());
                break;

            case COUNTRY:
                metadata.setCountry(keyValue.getValue());
                break;

            case CONSTELLATION:
                metadata.setConstellation(keyValue.getValue());
                break;

            case OBJECT_TYPE:
                metadata.setObjectType(ObjectType.valueOf(keyValue.getValue()));
                break;

            case OPS_STATUS:
                metadata.setOpsStatus(OpsStatus.valueOf(keyValue.getValue()));
                break;

            case ORBIT_CATEGORY:
                metadata.setOrbitCategory(OrbitCategory.valueOf(keyValue.getValue()));
                break;

            case OCM_DATA_ELEMENTS:
                metadata.setOcmDataElements(keyValue.getListValue());
                break;

            case TIME_SYSTEM:
                metadata.setTimeSystem(CcsdsTimeScale.parse(keyValue.getValue()));
                break;

            case EPOCH_TZERO:
                metadata.setEpochT0(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                              lineNumber, fileName, line));
                break;

            case SCLK_EPOCH:
                metadata.setSclkEpoch(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                                lineNumber, fileName, line));
                break;

            case SCLK_SEC_PER_SI_SEC:
                metadata.setClockSecPerSISec(keyValue.getDoubleValue());
                break;

            case PREVIOUS_MESSAGE_EPOCH:
                metadata.setPreviousMessageEpoch(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                                           lineNumber, fileName, line));
                break;

            case NEXT_MESSAGE_EPOCH:
                metadata.setNextMessageEpoch(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                                       lineNumber, fileName, line));
                break;

            case START_TIME:
                metadata.setStartTime(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                                lineNumber, fileName, line));
                break;

            case STOP_TIME:
                metadata.setStopTime(parseDate(keyValue.getValue(), metadata.getTimeSystem(),
                                               lineNumber, fileName, line));
                break;

            case TIME_SPAN:
                metadata.setTimeSpan(keyValue.getDoubleValue() * Constants.JULIAN_DAY);
                break;

            case TAIMUTC_AT_TZERO:
                metadata.setTaimutcT0(keyValue.getDoubleValue());
                break;

            case UT1MUTC_AT_TZERO:
                metadata.setUt1mutcT0(keyValue.getDoubleValue());
                break;

            case EOP_SOURCE:
                metadata.setEopSource(keyValue.getValue());
                break;

            case INTERP_METHOD_EOP:
                metadata.setInterpMethodEOP(keyValue.getValue());
                break;

            case INTERP_METHOD_SW:
                metadata.setInterpMethodSW(keyValue.getValue());
                break;

            case CELESTIAL_SOURCE:
                metadata.setCelestialSource(keyValue.getValue());
                break;

            default:
                return super.parseMetaDataEntry(keyValue, metadata,
                                                lineNumber, fileName, line);
        }
        return true;
    }

    /** Private class used to store OCM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OCM file being read. */
        private OCMFile file;

        /** OCM metadata being read. */
        private OCMMetadata metadata;

        /** OCM data being read. */
        private OCMData data;

        /** Current orbital history. */
        private OrbitalStateHistory orbitalStateHistory;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** current line. */
        private String line;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Create a new {@link ParseInfo} object.
         * @param conventions IERS conventions to use
         * @param dataContext data context to use
         */
        ParseInfo(final IERSConventions conventions, final DataContext dataContext) {
            file                  = new OCMFile();
            file.setConventions(conventions);
            file.setDataContext(dataContext);
            metadata              = new OCMMetadata(conventions, dataContext);
            data                  = new OCMData();
            lineNumber            = 0;
        }

    }

    /** Enumerate for various data sections. */
    private enum Section {

        /** Header section. */
        HEADER {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                return parser.parseHeaderEntry(pi.keyValue, pi.file);
            }
        },

        /** Metadata section. */
        META_DATA {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                return parser.parseMetaDataEntry(pi.keyValue, pi.metadata,
                                                 pi.lineNumber, pi.fileName, pi.line);
            }
        },

        /** Orbit data section. */
        ORBIT {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                if (pi.keyValue.getKeyword() == null) {
                    // this is an orbital state line
                    final String[] elements = pi.keyValue.getLine().trim().split("\\s+");
                    final AbsoluteDate date;
                    if (elements.length > 0) {
                        if (elements[0].indexOf('T') > 0) {
                            // there is a 'T' in the first field, the date is absolute
                            date = parser.parseDate(elements[0],
                                                    pi.orbitalStateHistory.getOrbTimeSystem(),
                                                    pi.lineNumber, pi.fileName, pi.line);
                        } else {
                            // no 'T' in the first field, the date is relative
                            date = pi.metadata.getEpochT0().shiftedBy(Double.parseDouble(elements[0]));
                        }
                        return pi.orbitalStateHistory.addState(date, elements, 1);
                    } else {
                        return false;
                    }
                } else {
                    // this is a key=value line
                    switch (pi.keyValue.getKeyword()) {
                        case ORB_ID :
                            pi.orbitalStateHistory.setOrbID(pi.keyValue.getValue());
                            break;
                        case ORB_PREV_ID :
                            pi.orbitalStateHistory.setOrbPrevID(pi.keyValue.getValue());
                            break;
                        case ORB_NEXT_ID :
                            pi.orbitalStateHistory.setOrbNextID(pi.keyValue.getValue());
                            break;
                        case ORB_BASIS :
                            pi.orbitalStateHistory.setOrbBasis(OrbitBasis.valueOf(pi.keyValue.getValue()));
                            break;
                        case ORB_BASIS_ID :
                            pi.orbitalStateHistory.setOrbBasisID(pi.keyValue.getValue());
                            break;
                        case ORB_AVERAGING :
                            pi.orbitalStateHistory.setOrbAveraging(pi.keyValue.getValue());
                            break;
                        case ORB_EPOCH_TZERO :
                            pi.orbitalStateHistory.setOrbEpochT0(parser.parseDate(pi.keyValue.getValue(),
                                                                                  pi.orbitalStateHistory.getOrbTimeSystem(),
                                                                                  pi.lineNumber, pi.fileName, pi.line));
                            break;
                        case ORB_TIME_SYSTEM :
                            pi.orbitalStateHistory.setOrbTimeSystem(CcsdsTimeScale.parse(pi.keyValue.getValue()));
                            break;
                        case CENTER_NAME :
                            pi.orbitalStateHistory.setCenterName(pi.keyValue.getValue());
                            break;
                        case ORB_REF_FRAME :
                            pi.orbitalStateHistory.setOrbRefFrame(parser.parseCCSDSFrame(pi.keyValue.getValue()));
                            break;
                        case ORB_FRAME_EPOCH :
                            pi.orbitalStateHistory.setOrbFrameEpoch(parser.parseDate(pi.keyValue.getValue(),
                                                                                     pi.orbitalStateHistory.getOrbTimeSystem(),
                                                                                     pi.lineNumber, pi.fileName, pi.line));
                            break;
                        case ORB_TYPE :
                            pi.orbitalStateHistory.setOrbType(ElementsType.valueOf(pi.keyValue.getValue()));
                            break;
                        case ORB_N :
                            pi.orbitalStateHistory.setOrbN(Integer.parseInt(pi.keyValue.getValue()));
                            break;
                        case ORB_ELEMENTS :
                            pi.orbitalStateHistory.setOrbElements(pi.keyValue.getValue());
                            break;
                        default :
                            return false;
                    }
                    return true;
                }
            }
        },

        /** Physical characteristics data section. */
        PHYSICS {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return true;
            }
        },

        /** Covariance data section. */
        COVARIANCE {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                if (pi.keyValue.getKeyword() == null) {
                    // TODO
                    return true;
                } else {
                    // TODO
                    return true;
                }
            }
        },

        /** Maneuver data section. */
        MANEUVER {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                if (pi.keyValue.getKeyword() == null) {
                    // TODO
                    return true;
                } else {
                    // TODO
                    return true;
                }
            }
        },

        /** Perturbations data section. */
        PERTURBATIONS {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return true;
            }
        },

        /** Orbit determination data section. */
        ORBIT_DETERMINATION {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return true;
            }
        },

        /** User-defined parameters data section. */
        USER_DEFINED {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return true;
            }
        },

        /** Undefined section. */
        UNDEFINED {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // always return false to trigger an exception
                return false;
            }
        };

        /** Parse an entry from the section.
         * @param parser parser to use
         * @param pi parse information
         * @return true if the keyword was a section keyword and has been parsed
         */
        protected abstract boolean parseEntry(OCMParser parser, ParseInfo pi);

    }

}
