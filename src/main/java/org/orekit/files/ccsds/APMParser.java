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
package org.orekit.files.ccsds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS APM (Attitude Parameter Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMParser extends ADMParser {

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
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}. See
     * {@link #withDataContext(DataContext)}.
     */
    @DefaultDataContext
    public APMParser() {
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
     *
     * @param dataContext used by the parser.
     *
     * @see #APMParser()
     * @see #withDataContext(DataContext)
     */
    public APMParser(final DataContext dataContext) {
        this(AbsoluteDate.FUTURE_INFINITY, Double.NaN, null, true, 0, 0, "", dataContext);
    }

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param launchYear launch year for TLEs
     * @param launchNumber launch number for TLEs
     * @param launchPiece piece of launch (from "A" to "ZZZ") for TLEs
     * @param dataContext used to retrieve frames, time scales, etc.
     */
    private APMParser(final AbsoluteDate missionReferenceDate, final double mu,
                      final IERSConventions conventions, final boolean simpleEOP,
                      final int launchYear, final int launchNumber,
                      final String launchPiece, final DataContext dataContext) {
        super(missionReferenceDate, mu, conventions, simpleEOP, launchYear, launchNumber,
                launchPiece, dataContext);
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate) {
        return new APMParser(newMissionReferenceDate, getMu(), getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withMu(final double newMu) {
        return new APMParser(getMissionReferenceDate(), newMu, getConventions(), isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withConventions(final IERSConventions newConventions) {
        return new APMParser(getMissionReferenceDate(), getMu(), newConventions, isSimpleEOP(),
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withSimpleEOP(final boolean newSimpleEOP) {
        return new APMParser(getMissionReferenceDate(), getMu(), getConventions(), newSimpleEOP,
                             getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                             getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withInternationalDesignator(final int newLaunchYear,
                                                 final int newLaunchNumber,
                                                 final String newLaunchPiece) {
        return new APMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                             newLaunchYear, newLaunchNumber, newLaunchPiece,
                             getDataContext());
    }

    /** {@inheritDoc} */
    @Override
    public APMParser withDataContext(final DataContext dataContext) {
        return new APMParser(getMissionReferenceDate(), getMu(), getConventions(), isSimpleEOP(),
                getLaunchYear(), getLaunchNumber(), getLaunchPiece(),
                dataContext);
    }

    /** {@inheritDoc} */
    @Override
    public APMFile parse(final String fileName) {
        return (APMFile) super.parse(fileName);
    }

    /** {@inheritDoc} */
    @Override
    public APMFile parse(final InputStream stream) {
        return (APMFile) super.parse(stream);
    }

    /** {@inheritDoc} */
    @Override
    public APMFile parse(final InputStream stream, final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo();
            pi.fileName = fileName;
            final APMFile file = pi.file;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setMu(getMu());
            pi.file.setConventions(getConventions());
            pi.file.setDataContext(getDataContext());
            pi.file.getMetaData().setLaunchYear(getLaunchYear());
            pi.file.getMetaData().setLaunchNumber(getLaunchNumber());
            pi.file.getMetaData().setLaunchPiece(getLaunchPiece());

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++pi.lineNumber;
                if (line.trim().length() == 0) {
                    // Blank line
                    continue;
                }
                pi.keyValue = new KeyValue(line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    // Unexpected keyword. An exception is thrown.
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, pi.lineNumber, pi.fileName, line);
                }

                switch (pi.keyValue.getKeyword()) {

                    case CCSDS_APM_VERS:
                        file.setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case Q_FRAME_A:
                        file.setQuaternionFrameAString(pi.keyValue.getValue());
                        break;

                    case Q_FRAME_B:
                        file.setQuaternionFrameBString(pi.keyValue.getValue());
                        break;

                    case Q_DIR:
                        file.setAttitudeQuaternionDirection(pi.keyValue.getValue());
                        break;

                    case QC:
                        pi.q0 = pi.keyValue.getDoubleValue();
                        break;

                    case Q1:
                        pi.q1 = pi.keyValue.getDoubleValue();
                        break;

                    case Q2:
                        pi.q2 = pi.keyValue.getDoubleValue();
                        break;

                    case Q3:
                        pi.q3 = pi.keyValue.getDoubleValue();
                        break;

                    case MAN_EPOCH_START:
                        if (pi.maneuver != null) {
                            file.addManeuver(pi.maneuver);
                        }
                        pi.maneuver = new APMFile.APMManeuver();
                        pi.maneuver.setEpochStart(parseDate(pi.keyValue.getValue(), file.getMetaData().getTimeSystem()));
                        if (!pi.commentTmp.isEmpty()) {
                            pi.maneuver.setComment(pi.commentTmp);
                            pi.commentTmp.clear();
                        }
                        break;

                    case MAN_DURATION:
                        pi.maneuver.setDuration(pi.keyValue.getDoubleValue());
                        break;

                    case MAN_REF_FRAME:
                        pi.maneuver.setRefFrameString(pi.keyValue.getValue());
                        break;

                    case MAN_TOR_1:
                        pi.maneuver.setTorque(new Vector3D(pi.keyValue.getDoubleValue(),
                                                           pi.maneuver.getTorque().getY(),
                                                           pi.maneuver.getTorque().getZ()));
                        break;

                    case MAN_TOR_2:
                        pi.maneuver.setTorque(new Vector3D(pi.maneuver.getTorque().getX(),
                                                           pi.keyValue.getDoubleValue(),
                                                           pi.maneuver.getTorque().getZ()));
                        break;

                    case MAN_TOR_3:
                        pi.maneuver.setTorque(new Vector3D(pi.maneuver.getTorque().getX(),
                                                           pi.maneuver.getTorque().getY(),
                                                           pi.keyValue.getDoubleValue()));
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

            file.setQuaternion(new Quaternion(pi.q0, pi.q1, pi.q2, pi.q3));
            if (pi.maneuver != null) {
                file.addManeuver(pi.maneuver);
            }
            return file;

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Private class used to stock APM parsing info. */
    private static class ParseInfo {

        /** APM file being read. */
        private APMFile file;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Key value of the line being read. */
        private KeyValue keyValue;

        /** Stored comments. */
        private List<String> commentTmp;

        /** Scalar coordinate of the quaternion. */
        private double q0;

        /** First component of the quaternion vector. */
        private double q1;

        /** Second component of the quaternion vector.. */
        private double q2;

        /** Third component of the quaternion vector.. */
        private double q3;

        /** Current maneuver. */
        private APMFile.APMManeuver maneuver;

        /** Create a new {@link ParseInfo} object. */
        protected ParseInfo() {
            file       = new APMFile();
            lineNumber = 0;
            commentTmp = new ArrayList<String>();
            maneuver   = null;
        }
    }

}
