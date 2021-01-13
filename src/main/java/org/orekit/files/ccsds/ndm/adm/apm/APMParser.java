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
package org.orekit.files.ccsds.ndm.adm.apm;

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
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.ADMMetadata;
import org.orekit.files.ccsds.ndm.adm.ADMParser;
import org.orekit.files.ccsds.ndm.adm.ADMSegment;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * A parser for the CCSDS APM (Attitude Parameter Message).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class APMParser extends ADMParser<APMFile, APMParser> {

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
        this(null, true, dataContext, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames, time scales, etc.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     */
    private APMParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                      final AbsoluteDate missionReferenceDate) {
        super(conventions, simpleEOP, dataContext, missionReferenceDate);
    }

    /** {@inheritDoc} */
    @Override
    protected APMParser create(final IERSConventions newConventions,
                               final boolean newSimpleEOP,
                               final DataContext newDataContext,
                               final AbsoluteDate newMissionReferenceDate) {
        return new APMParser(newConventions, newSimpleEOP, newDataContext, newMissionReferenceDate);
    }

    /** {@inheritDoc} */
    @Override
    public APMFile parse(final InputStream stream, final String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            // initialize internal data structures
            final ParseInfo pi = new ParseInfo(getConventions(), getDataContext());
            pi.fileName = fileName;

            // set the additional data that has been configured prior the parsing by the user.
            pi.file.setMissionReferenceDate(getMissionReferenceDate());
            pi.file.setConventions(getConventions());
            pi.file.setDataContext(getDataContext());

            pi.parsingHeader = true;
            for (pi.line = reader.readLine(); pi.line != null; pi.line = reader.readLine()) {
                ++pi.lineNumber;
                if (pi.line.trim().length() == 0) {
                    // Blank line
                    continue;
                }
                pi.keyValue = new KeyValue(pi.line, pi.lineNumber, pi.fileName);
                if (pi.keyValue.getKeyword() == null) {
                    // Unexpected keyword. An exception is thrown.
                    throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                              pi.lineNumber, pi.fileName, pi.line);
                }

                switch (pi.keyValue.getKeyword()) {

                    case CCSDS_APM_VERS:
                        pi.file.getHeader().setFormatVersion(pi.keyValue.getDoubleValue());
                        break;

                    case Q_FRAME_A:
                        pi.data.setQuaternionFrameAString(pi.keyValue.getValue());
                        break;

                    case Q_FRAME_B:
                        pi.data.setQuaternionFrameBString(pi.keyValue.getValue());
                        break;

                    case Q_DIR:
                        pi.data.setAttitudeQuaternionDirection(pi.keyValue.getValue());
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
                            pi.data.addManeuver(pi.maneuver);
                        }
                        pi.maneuver = new APMManeuver();
                        pi.maneuver.setEpochStart(parseDate(pi.keyValue.getValue(), pi.metadata.getTimeSystem(),
                                                            pi.lineNumber, pi.fileName, pi.line));
                        if (!pi.commentTmp.isEmpty()) {
                            pi.maneuver.setComments(pi.commentTmp);
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
                        final boolean parsed;
                        if (pi.parsingHeader) {
                            parsed = parseHeaderEntry(pi.keyValue, pi.file);
                            if (pi.file.getHeader().getOriginator() != null) {
                                // this was the end of the header part
                                pi.parsingHeader   = false;
                                pi.parsingMetaData = true;
                            }
                        } else if (pi.parsingMetaData) {
                            parsed = parseMetaDataEntry(pi.keyValue, pi.metadata);
                            if (pi.metadata.getTimeSystem() != null) {
                                // this was the end of the metadata part
                                pi.parsingMetaData = false;
                            }
                        } else {
                            parsed = parseGeneralStateDataEntry(pi);
                        }
                        if (!parsed) {
                            throw new OrekitException(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD,
                                                      pi.lineNumber, pi.fileName, pi.line);
                        }

                }

            }

            pi.data.setQuaternion(new Quaternion(pi.q0, pi.q1, pi.q2, pi.q3));
            if (pi.maneuver != null) {
                pi.data.addManeuver(pi.maneuver);
            }
            pi.file.addSegment(new ADMSegment<>(pi.metadata, pi.data));
            return pi.file;

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Parse a general state data key = value entry.
     * @param pi parser information
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    private boolean parseGeneralStateDataEntry(final ParseInfo pi) {
        switch (pi.keyValue.getKeyword()) {

            case COMMENT:
                pi.commentTmp.add(pi.keyValue.getValue());
                return true;

            case EPOCH:
                pi.data.setEpochComment(pi.commentTmp);
                pi.commentTmp.clear();
                pi.data.setEpoch(parseDate(pi.keyValue.getValue(), pi.metadata.getTimeSystem(),
                                        pi.lineNumber, pi.fileName, pi.line));
                return true;

            case QC_DOT:
                pi.data.setQuaternionDot(new Quaternion(pi.keyValue.getDoubleValue(),
                                                        pi.data.getQuaternionDot().getQ1(),
                                                        pi.data.getQuaternionDot().getQ2(),
                                                        pi.data.getQuaternionDot().getQ3()));
                return true;

            case Q1_DOT:
                pi.data.setQuaternionDot(new Quaternion(pi.data.getQuaternionDot().getQ0(),
                                                        pi.keyValue.getDoubleValue(),
                                                        pi.data.getQuaternionDot().getQ2(),
                                                        pi.data.getQuaternionDot().getQ3()));
                return true;

            case Q2_DOT:
                pi.data.setQuaternionDot(new Quaternion(pi.data.getQuaternionDot().getQ0(),
                                                        pi.data.getQuaternionDot().getQ1(),
                                                        pi.keyValue.getDoubleValue(),
                                                        pi.data.getQuaternionDot().getQ3()));
                return true;

            case Q3_DOT:
                pi.data.setQuaternionDot(new Quaternion(pi.data.getQuaternionDot().getQ0(),
                                                        pi.data.getQuaternionDot().getQ1(),
                                                        pi.data.getQuaternionDot().getQ2(),
                                                        pi.keyValue.getDoubleValue()));
                return true;

            case EULER_FRAME_A:
                pi.data.setEulerComment(pi.commentTmp);
                pi.commentTmp.clear();
                pi.data.setEulerFrameAString(pi.keyValue.getValue());
                return true;

            case EULER_FRAME_B:
                pi.data.setEulerFrameBString(pi.keyValue.getValue());
                return true;

            case EULER_DIR:
                pi.data.setEulerDirection(pi.keyValue.getValue());
                return true;

            case EULER_ROT_SEQ:
                pi.data.setEulerRotSeq(pi.keyValue.getValue());
                return true;

            case RATE_FRAME:
                pi.data.setRateFrameString(pi.keyValue.getValue());
                return true;

            case X_ANGLE:
                pi.data.setRotationAngles(new Vector3D(toRadians(pi.keyValue),
                                                       pi.data.getRotationAngles().getY(),
                                                       pi.data.getRotationAngles().getZ()));
                return true;

            case Y_ANGLE:
                pi.data.setRotationAngles(new Vector3D(pi.data.getRotationAngles().getX(),
                                                       toRadians(pi.keyValue),
                                                       pi.data.getRotationAngles().getZ()));
                return true;

            case Z_ANGLE:
                pi.data.setRotationAngles(new Vector3D(pi.data.getRotationAngles().getX(),
                                                       pi.data.getRotationAngles().getY(),
                                                       toRadians(pi.keyValue)));
                return true;

            case X_RATE:
                pi.data.setRotationRates(new Vector3D(toRadians(pi.keyValue),
                                                      pi.data.getRotationRates().getY(),
                                                      pi.data.getRotationRates().getZ()));
                return true;

            case Y_RATE:
                pi.data.setRotationRates(new Vector3D(pi.data.getRotationRates().getX(),
                                                      toRadians(pi.keyValue),
                                                      pi.data.getRotationRates().getZ()));
                return true;

            case Z_RATE:
                pi.data.setRotationRates(new Vector3D(pi.data.getRotationRates().getX(),
                                                      pi.data.getRotationRates().getY(),
                                                      toRadians(pi.keyValue)));
                return true;

            case SPIN_FRAME_A:
                pi.data.setSpinComment(pi.commentTmp);
                pi.commentTmp.clear();
                pi.data.setSpinFrameAString(pi.keyValue.getValue());
                return true;

            case SPIN_FRAME_B:
                pi.data.setSpinFrameBString(pi.keyValue.getValue());
                return true;

            case SPIN_DIR:
                pi.data.setSpinDirection(pi.keyValue.getValue());
                return true;

            case SPIN_ALPHA:
                pi.data.setSpinAlpha(toRadians(pi.keyValue));
                return true;

            case SPIN_DELTA:
                pi.data.setSpinDelta(toRadians(pi.keyValue));
                return true;

            case SPIN_ANGLE:
                pi.data.setSpinAngle(toRadians(pi.keyValue));
                return true;

            case SPIN_ANGLE_VEL:
                pi.data.setSpinAngleVel(toRadians(pi.keyValue));
                return true;

            case NUTATION:
                pi.data.setNutation(toRadians(pi.keyValue));
                return true;

            case NUTATION_PER:
                pi.data.setNutationPeriod(pi.keyValue.getDoubleValue());
                return true;

            case NUTATION_PHASE:
                pi.data.setNutationPhase(toRadians(pi.keyValue));
                return true;

            case INERTIA_REF_FRAME:
                pi.data.setSpacecraftComment(pi.commentTmp);
                pi.commentTmp.clear();
                pi.data.setInertiaRefFrameString(pi.keyValue.getValue());
                return true;

            case I11:
                pi.data.setI11(pi.keyValue.getDoubleValue());
                return true;

            case I22:
                pi.data.setI22(pi.keyValue.getDoubleValue());
                return true;

            case I33:
                pi.data.setI33(pi.keyValue.getDoubleValue());
                return true;

            case I12:
                pi.data.setI12(pi.keyValue.getDoubleValue());
                return true;

            case I13:
                pi.data.setI13(pi.keyValue.getDoubleValue());
                return true;

            case I23:
                pi.data.setI23(pi.keyValue.getDoubleValue());
                return true;

            default:
                return false;

        }

    }

    /**
     * Convert a {@link KeyValue} in degrees to a real value in randians.
     * @param keyValue key value
     * @return the value in radians
     */
    private double toRadians(final KeyValue keyValue) {
        return FastMath.toRadians(keyValue.getDoubleValue());
    }

    /** Private class used to store APM parsing info. */
    private static class ParseInfo {

        /** APM file being read. */
        private APMFile file;

        /** APM metadata being read. */
        private ADMMetadata metadata;

        /** APM data being read. */
        private APMData data;

        /** Name of the file. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

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
        private APMManeuver maneuver;

        /** Boolean indicating if the parser is currently parsing a header block. */
        private boolean parsingHeader;

        /** Boolean indicating if the parser is currently parsing a meta-data block. */
        private boolean parsingMetaData;

        /** Create a new {@link ParseInfo} object.
         * @param conventions IERS conventions to use
         * @param dataContext data context to use
         */
        ParseInfo(final IERSConventions conventions, final DataContext dataContext) {
            file            = new APMFile();
            metadata        = new ADMMetadata(conventions, dataContext);
            data            = new APMData();
            lineNumber      = 0;
            commentTmp      = new ArrayList<>();
            maneuver        = null;
            parsingHeader   = false;
            parsingMetaData = false;
        }
    }

}
