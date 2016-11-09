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
package org.orekit.files.ccsds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** A parser for the CCSDS OMM (Orbiter Mean-Elements Message).
 * @author sports
 * @since 6.1
 */
public class OMMParser extends ODMParser {

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
     */
    public OMMParser() {
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
    private OMMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber, final String launchPiece) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber, launchPiece);
    }

    /** {@inheritDoc} */
    public OMMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new OMMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OMMParser withMu(final double newMu) {
        return new OMMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OMMParser withConventions(final IERSConventions newConventions) {
        return new OMMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OMMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new OMMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece());
    }

    /** {@inheritDoc} */
    public OMMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new OMMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece);
    }

    /** {@inheritDoc} */
    @Override
    public OMMFile parse(final String fileName) throws OrekitException {
        return (OMMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public OMMFile parse(final InputStream stream) throws OrekitException {
        return (OMMFile) super.parse(stream);
    }

    /** {@inheritDoc} */
    public OMMFile parse(final InputStream stream, final String fileName) throws OrekitException {

        try {

            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final OMMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMuSet(getMu());
            pi.file.setConventions(getConventions());
            pi.file.getMetaData().setLaunchYear(getLaunchYear());
            pi.file.getMetaData().setLaunchNumber(getLaunchNumber());
            pi.file.getMetaData().setLaunchPiece(getLaunchPiece());

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }
                switch (pi.keyValue.getKeyword()) {
                    case CCSDS_OMM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case MEAN_ELEMENT_THEORY:
                        file.getMetaData().setMeanElementTheory(pi.keyValue.getValue());
                        break;

                    case MEAN_MOTION:
                        file.setMeanMotion(pi.keyValue.getDoubleValue() * FastMath.PI / 43200.0);
                        break;

                    case EPHEMERIS_TYPE:
                        file.setTLERelatedParametersComment(pi.commentTmp);
                        pi.commentTmp.clear();
                        file.setEphemerisType(Integer.parseInt(pi.keyValue.getValue()));
                        break;

                    case CLASSIFICATION_TYPE:
                        file.setClassificationType(pi.keyValue.getValue().charAt(0));
                        break;

                    case NORAD_CAT_ID:
                        file.setNoradID(Integer.parseInt(pi.keyValue.getValue()));
                        break;

                    case ELEMENT_SET_NO:
                        file.setElementSetNo(pi.keyValue.getValue());
                        break;

                    case REV_AT_EPOCH:
                        file.setRevAtEpoch(Integer.parseInt(pi.keyValue.getValue()));
                        break;

                    case BSTAR:
                        file.setbStar(pi.keyValue.getDoubleValue());
                        break;

                    case MEAN_MOTION_DOT:
                        file.setMeanMotionDot(pi.keyValue.getDoubleValue() * FastMath.PI / 1.86624e9);
                        break;

                    case MEAN_MOTION_DDOT:
                        file.setMeanMotionDotDot(pi.keyValue.getDoubleValue() *
                                                 FastMath.PI / 5.3747712e13);
                        break;

                    default:
                        boolean parsed = false;
                        parsed = parsed || parseComment(pi.keyValue, pi.commentTmp);
                        parsed = parsed || parseHeaderEntry(pi.keyValue, file, pi.commentTmp);
                        parsed = parsed || parseMetaDataEntry(pi.keyValue, file.getMetaData(), pi.commentTmp);
                        parsed = parsed || parseGeneralStateDataEntry(pi.keyValue, file, pi.commentTmp);
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                        }
                }
            }
            reader.close();
            return file;
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Private class used to stock OMM parsing info.
     * @author sports
     */
    private static class ParseInfo {

        /** OMM file being read. */
        private OMMFile file;

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
            lineNumber = 0;
            file = new OMMFile();
            commentTmp = new ArrayList<String>();
        }
    }
}
