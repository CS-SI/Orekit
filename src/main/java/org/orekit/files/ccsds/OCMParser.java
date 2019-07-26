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
package org.orekit.files.ccsds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OCM (Orbit Comprehensive Message).
 * @author Luc Maisonobe
 * @since 10.1
 */
public class OCMParser extends ODMParser {

    /** Mandatory keywords.
     * @since 10.1
     */
    private static final Keyword[] MANDATORY_KEYWORDS = {
        Keyword.CCSDS_OCM_VERS, Keyword.CREATION_DATE, Keyword.ORIGINATOR,
        Keyword.DEF_EPOCH_TZERO, Keyword.DEF_TIME_SYSTEM,
        Keyword.META_START, Keyword.META_STOP
    };

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
     */
    public OCMParser() {
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null, true, 0, 0, "");
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param launchYear launch year for TLEs
     * @param launchNumber launch number for TLEs
     * @param launchPiece piece of launch (from "A" to "ZZZ") for TLEs
     */
    private OCMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber, final String launchPiece) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber, launchPiece);
    }

    /** {@inheritDoc} */
    public OCMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OCMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OCMParser withMu(final double newMu) {
        return new OCMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OCMParser withConventions(final IERSConventions newConventions) {
        return new OCMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OCMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new OCMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OCMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new OCMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece);
    }

    /** {@inheritDoc} */
    @Override
    public OCMFile parse(final String fileName) {
        return (OCMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public OCMFile parse(final InputStream stream) {
        return (OCMFile) super.parse(stream);
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
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final OCMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMuSet(getMu());
            pi.file.setConventions(getConventions());
            pi.file.getMetaData().setLaunchYear(getLaunchYear());
            pi.file.getMetaData().setLaunchNumber(getLaunchNumber());
            pi.file.getMetaData().setLaunchPiece(getLaunchPiece());

            Section section = Section.HEADER;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }

                declareFound(pi.keyValue.getKeyword());

                switch (pi.keyValue.getKeyword()) {

                    case CCSDS_OCM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case META_START :
                        if (section == Section.HEADER) {
                            section = Section.META_DATA;
                        } else {
                            // only one metadata section is allowed, just after header
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                                      pi.lineNumber, pi.fileName, line);
                        }
                        break;

                    case META_STOP :
                        if (!pi.commentTmp.isEmpty()) {
                            pi.file.getMetaData().setComment(pi.commentTmp);
                            pi.commentTmp.clear();
                        }
                        section = Section.UNDEFINED;
                        break;

                    case ORB_START :
                        section = Section.ORBIT;
                        break;

                    case ORB_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case PHYS_START :
                        section = Section.PHYSICS;
                        break;

                    case PHYS_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case COV_START :
                        section = Section.COVARIANCE;
                        break;

                    case COV_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case STM_START :
                        section = Section.STM;
                        break;

                    case STM_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case MAN_START :
                        section = Section.MANEUVER;
                        break;

                    case MAN_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case PERT_START :
                        section = Section.PERTURBATIONS;
                        break;

                    case PERT_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case OD_START :
                        section = Section.ORBIT_DETERMINATION;
                        break;

                    case OD_STOP :
                        section = Section.UNDEFINED;
                        break;

                    case USER_START :
                        section = Section.USER_DEFINED;
                        break;

                    case USER_STOP :
                        section = Section.UNDEFINED;
                        break;

                    default:
                        if (pi.keyValue.getKeyword() == Keyword.COMMENT) {
                            parseComment(pi.keyValue, pi.commentTmp);
                        } else if (!section.parseEntry(this, pi)) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                                      pi.lineNumber, pi.fileName, line);
                        }
                }

            }

            // check all mandatory keywords have been found
            checkExpected(fileName);

            return file;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean parseMetaDataEntry(final KeyValue keyValue,
                                         final ODMMetaData metaData, final List<String> comment) {
        final OCMFile.OCMMetaData cMetaData = (OCMFile.OCMMetaData) metaData;
        switch (keyValue.getKeyword()) {
            case ORIGINATOR:
                // special case for OCM: ORIGINATOR belongs to metadata section
                // rather than to header section
                metaData.getODMFile().setOriginator(keyValue.getValue());
                break;

            case ORIGINATOR_POC:
                cMetaData.setOriginatorPOC(keyValue.getValue());
                break;

            case ORIGINATOR_POSITION:
                cMetaData.setOriginatorPosition(keyValue.getValue());
                break;

            case ORIGINATOR_PHONE:
                cMetaData.setOriginatorPhone(keyValue.getValue());
                break;

            case ORIGINATOR_ADDRESS:
                cMetaData.setOriginatorAddress(keyValue.getValue());
                break;

            case TECH_ORG:
                cMetaData.setTechOrg(keyValue.getValue());
                break;

            case TECH_POC:
                cMetaData.setTechPOC(keyValue.getValue());
                break;

            case TECH_POSITION:
                cMetaData.setTechPosition(keyValue.getValue());
                break;

            case TECH_PHONE:
                cMetaData.setTechPhone(keyValue.getValue());
                break;

            case TECH_ADDRESS:
                cMetaData.setTechAddress(keyValue.getValue());
                break;

            case PREV_MESSAGE_ID:
                cMetaData.setPrevMessageID(keyValue.getValue());
                break;

            case PREV_MESSAGE_EPOCH:
                cMetaData.setPrevMessageEpoch(keyValue.getValue());
                break;

            case NEXT_MESSAGE_ID:
                cMetaData.setNextMessageID(keyValue.getValue());
                break;

            case NEXT_MESSAGE_EPOCH:
                cMetaData.setNextMessageEpoch(keyValue.getValue());
                break;

            case ATT_MESSAGE_LINK:
                cMetaData.setAttMessageLink(keyValue.getListValue());
                break;

            case CDM_MESSAGE_LINK:
                cMetaData.setCdmMessageLink(keyValue.getListValue());
                break;

            case PRM_MESSAGE_LINK:
                cMetaData.setPrmMessageLink(keyValue.getListValue());
                break;

            case RDM_MESSAGE_LINK:
                cMetaData.setRdmMessageLink(keyValue.getListValue());
                break;

            case TDM_MESSAGE_LINK:
                cMetaData.setTdmMessageLink(keyValue.getListValue());
                break;

            case OBJECT_NAME:
                cMetaData.setObjectName(keyValue.getValue());
                break;

            case INTERNATIONAL_DESIGNATOR:
                cMetaData.setInternationalDesignator(keyValue.getValue());
                break;

            case OPERATOR:
                cMetaData.setOperator(keyValue.getValue());
                break;

            case OWNER:
                cMetaData.setOwner(keyValue.getValue());
                break;

            case MISSION:
                cMetaData.setMission(keyValue.getValue());
                break;

            case CONSTELLATION:
                cMetaData.setConstellation(keyValue.getValue());
                break;

            case LAUNCH_EPOCH:
                cMetaData.setLaunchEpoch(keyValue.getValue());
                break;

            case LAUNCH_COUNTRY:
                cMetaData.setLaunchCountry(keyValue.getValue());
                break;

            case LAUNCH_SITE:
                cMetaData.setLaunchSite(keyValue.getValue());
                break;

            case LAUNCH_PROVIDER:
                cMetaData.setLaunchProvider(keyValue.getValue());
                break;

            case LAUNCH_INTEGRATOR:
                cMetaData.setLaunchIntegrator(keyValue.getValue());
                break;

            case LAUNCH_PAD:
                cMetaData.setLaunchPad(keyValue.getValue());
                break;

            case LAUNCH_PLATFORM:
                cMetaData.setLaunchPlatform(keyValue.getValue());
                break;

            case RELEASE_EPOCH:
                cMetaData.setReleaseEpoch(keyValue.getValue());
                break;

            case MISSION_START_EPOCH:
                cMetaData.setMissionStartEpoch(keyValue.getValue());
                break;

            case MISSION_END_EPOCH:
                cMetaData.setMissionEndEpoch(keyValue.getValue());
                break;

            case REENTRY_EPOCH:
                cMetaData.setReentryEpoch(keyValue.getValue());
                break;

            case LIFETIME:
                cMetaData.setLifetime(keyValue.getDoubleValue() * Constants.JULIAN_DAY);
                break;

            case CATALOG_NAME:
                cMetaData.setCatalogName(keyValue.getValue());
                break;

            case OBJECT_TYPE:
                cMetaData.setObjectType(CCSDSObjectType.valueOf(keyValue.getValue()));
                break;

            case OPS_STATUS:
                cMetaData.setOpsStatus(CCSDSOpsStatus.valueOf(keyValue.getValue()));
                break;

            case ORBIT_TYPE:
                cMetaData.setOrbitType(CCSDSOrbitType.valueOf(keyValue.getValue()));
                break;

            case OCM_DATA_ELEMENTS:
                cMetaData.setOcmDataElements(keyValue.getListValue());
                break;

            case DEF_EPOCH_TZERO:
                cMetaData.setDefEpochT0(keyValue.getValue());
                break;

            case DEF_TIME_SYSTEM:
                if (!CcsdsTimeScale.contains(keyValue.getValue())) {
                    throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED,
                                              keyValue.getValue());
                }
                cMetaData.setDefTimeSystem(CcsdsTimeScale.valueOf(keyValue.getValue()));
                break;

            case SEC_CLK_PER_SI_SEC:
                cMetaData.setSecClockPerSISecond(keyValue.getDoubleValue());
                break;

            case SEC_PER_DAY:
                cMetaData.setSecPerDay(keyValue.getDoubleValue());
                break;

            case EARLIEST_TIME:
                cMetaData.setEarliestTime(filterAbsoluteOrRelative(keyValue));
                break;

            case LATEST_TIME:
                cMetaData.setLatestTime(filterAbsoluteOrRelative(keyValue));
                break;

            case TIME_SPAN:
                cMetaData.setTimeSpan(keyValue.getDoubleValue() * Constants.JULIAN_DAY);
                break;

            case TAIMUTC_AT_TZERO:
                cMetaData.setTaimutcT0(keyValue.getDoubleValue());
                break;

            case UT1MUTC_AT_TZERO:
                cMetaData.setUt1mutcT0(keyValue.getDoubleValue());
                break;

            case EOP_SOURCE:
                cMetaData.setEopSource(keyValue.getValue());
                break;

            case INTERP_METHOD_EOP:
                cMetaData.setInterpMethodEOP(keyValue.getValue());
                break;

            default:
                return super.parseMetaDataEntry(keyValue, metaData, comment);
        }
        return true;
    }

    /** Filter absolute or relative date.
     * @param keyValue key-value pair
     * @return absolute or relative string
     */
    private String filterAbsoluteOrRelative(final KeyValue keyValue) {
        if (keyValue.getValue().startsWith("DT=")) {
            // this is a relative date
            return keyValue.getValue().substring(3);
        } else if (keyValue.getValue().startsWith("T=")) {
            // this is an absolute date
            return keyValue.getValue().substring(2);
        } else {
            throw keyValue.generateException();
        }
    }

    /** Private class used to stock OCM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OCM file being read. */
        private OCMFile file;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            file       = new OCMFile();
            lineNumber = 0;
            commentTmp = new ArrayList<String>();
        }
    }

    /** Enumerate for various data sections. */
    private enum Section {

        /** Header section. */
        HEADER {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                return parser.parseHeaderEntry(pi.keyValue, pi.file, pi.commentTmp);
            }
        },

        /** Metadata section. */
        META_DATA {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                return parser.parseMetaDataEntry(pi.keyValue, pi.file.getMetaData(), pi.commentTmp);
            }
        },

        /** Orbit data section. */
        ORBIT {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Physical characteristics data section. */
        PHYSICS {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Covariance data section. */
        COVARIANCE {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** State transition matrix data section. */
        STM {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Maneuver data section. */
        MANEUVER {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Perturbations data section. */
        PERTURBATIONS {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Orbit determination data section. */
        ORBIT_DETERMINATION {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** User-defined parameters data section. */
        USER_DEFINED {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
                // TODO
                return false;
            }
        },

        /** Undefined section. */
        UNDEFINED {
            /** {@inheritDoc}*/
            @Override
            protected boolean parseEntry(final OCMParser parser, final ParseInfo pi) {
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
