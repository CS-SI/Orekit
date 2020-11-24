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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Attitude Data Message parsers.
 *
 * <p> This base class is immutable, and hence thread safe. When parts must be
 * changed, such as reference date for Mission Elapsed Time or Mission Relative
 * Time time systems, or the gravitational coefficient or the IERS conventions,
 * the various {@code withXxx} methods must be called, which create a new
 * immutable instance with the new parameters. This is a combination of the <a
 * href="https://en.wikipedia.org/wiki/Builder_pattern">builder design
 * pattern</a> and a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 *
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class ADMParser {

    /** Pattern for international designator. */
    private static final Pattern INTERNATIONAL_DESIGNATOR = Pattern.compile("(\\p{Digit}{4})-(\\p{Digit}{3})(\\p{Upper}{1,3})");

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient. */
    private final  double mu;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Indicator for simple or accurate EOP interpolation. */
    private final  boolean simpleEOP;

    /** Data context used for obtain frames and time scales. */
    private final DataContext dataContext;

    /** Launch Year. */
    private int launchYear;

    /** Launch number. */
    private int launchNumber;

    /** Piece of launch (from "A" to "ZZZ"). */
    private String launchPiece;

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param launchYear launch year for TLEs
     * @param launchNumber launch number for TLEs
     * @param launchPiece piece of launch (from "A" to "ZZZ") for TLEs
     * @param dataContext used to retrieve frames and time scales.
     */
    protected ADMParser(final AbsoluteDate missionReferenceDate, final double mu,
                        final IERSConventions conventions, final boolean simpleEOP,
                        final int launchYear, final int launchNumber,
                        final String launchPiece,
                        final DataContext dataContext) {
        this.missionReferenceDate = missionReferenceDate;
        this.mu                   = mu;
        this.conventions          = conventions;
        this.simpleEOP            = simpleEOP;
        this.launchYear           = launchYear;
        this.launchNumber         = launchNumber;
        this.launchPiece          = launchPiece;
        this.dataContext          = dataContext;
    }

    /**
     * Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public abstract ADMParser withMissionReferenceDate(AbsoluteDate newMissionReferenceDate);

    /**
     * Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /**
     * Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient date replaced
     * @see #getMu()
     */
    public abstract ADMParser withMu(double newMu);

    /**
     * Get gravitational coefficient.
     * @return gravitational coefficient to use while parsing
     * @see #withMu(double)
     */
    public double getMu() {
        return mu;
    }

    /**
     * Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public abstract ADMParser withConventions(IERSConventions newConventions);

    /**
     * Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /**
     * Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public abstract ADMParser withSimpleEOP(boolean newSimpleEOP);

    /**
     * Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /**
     * Set international designator.
     * <p>
     * This method may be used to ensure the launch year number and pieces are
     * correctly set if they are not present in the CCSDS file header in the
     * OBJECT_ID in the form YYYY-NNNP{PP}. If they are already in the header,
     * they will be parsed automatically regardless of this method being called
     * or not (i.e. header information override information set here).
     * </p>
     * @param newLaunchYear launch year
     * @param newLaunchNumber launch number
     * @param newLaunchPiece piece of launch (from "A" to "ZZZ")
     * @return a new instance, with TLE settings replaced
     */
    public abstract ADMParser withInternationalDesignator(int newLaunchYear,
                                                          int newLaunchNumber,
                                                          String newLaunchPiece);

    /**
     * Get the launch year.
     * @return launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /**
     * Get the launch number.
     * @return launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /**
     * Get the piece of launch.
     * @return piece of launch
     */
    public String getLaunchPiece() {
        return launchPiece;
    }

    /**
     * Get the data context used for getting frames, time scales, and celestial bodies.
     * @return the data context.
     */
    public DataContext getDataContext() {
        return dataContext;
    }

    /**
     * Set the data context.
     * @param newDataContext used for frames, time scales, and celestial bodies.
     * @return a new instance with the data context replaced.
     */
    public abstract ADMParser withDataContext(DataContext newDataContext);

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param fileName name of the file containing the message
     * @return parsed ADM file
     */
    public ADMFile parse(final String fileName) {
        try (InputStream stream = new FileInputStream(fileName)) {
            return parse(stream, fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param stream stream containing message
     * @return parsed ADM file
     */
    public ADMFile parse(final InputStream stream) {
        return parse(stream, "<unknown>");
    }

    /**
     * Parse a CCSDS Attitude Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed ADM file
     */
    public abstract ADMFile parse(InputStream stream, String fileName);

    /**
     * Parse a comment line.
     * @param keyValue key=value pair containing the comment
     * @param comment placeholder where the current comment line should be added
     * @return true if the line was a comment line and was parsed
     */
    protected boolean parseComment(final KeyValue keyValue, final List<String> comment) {
        if (keyValue.getKeyword() == Keyword.COMMENT) {
            comment.add(keyValue.getValue());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parse an entry from the header.
     * @param keyValue key = value pair
     * @param admFile instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a header keyword and has been parsed
     */
    protected boolean parseHeaderEntry(final KeyValue keyValue,
                                       final ADMFile admFile, final List<String> comment) {
        switch (keyValue.getKeyword()) {

            case CREATION_DATE:
                if (!comment.isEmpty()) {
                    admFile.setHeaderComment(comment);
                    comment.clear();
                }
                admFile.setCreationDate(new AbsoluteDate(
                        keyValue.getValue(),
                        dataContext.getTimeScales().getUTC()));
                return true;

            case ORIGINATOR:
                admFile.setOriginator(keyValue.getValue());
                return true;

            default:
                return false;

        }

    }

    /**
     * Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue,
                                         final ADMMetaData metaData, final List<String> comment) {
        switch (keyValue.getKeyword()) {
            case OBJECT_NAME:
                if (!comment.isEmpty()) {
                    metaData.setComment(comment);
                    comment.clear();
                }
                metaData.setObjectName(keyValue.getValue());
                return true;

            case OBJECT_ID: {
                metaData.setObjectID(keyValue.getValue());
                final Matcher matcher = INTERNATIONAL_DESIGNATOR.matcher(keyValue.getValue());
                if (matcher.matches()) {
                    metaData.setLaunchYear(Integer.parseInt(matcher.group(1)));
                    metaData.setLaunchNumber(Integer.parseInt(matcher.group(2)));
                    metaData.setLaunchPiece(matcher.group(3));
                }
                return true;
            }

            case CENTER_NAME:
                metaData.setCenterName(keyValue.getValue());
                final String canonicalValue;
                if (keyValue.getValue().equals("SOLAR SYSTEM BARYCENTER") || keyValue.getValue().equals("SSB")) {
                    canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
                } else if (keyValue.getValue().equals("EARTH MOON BARYCENTER") || keyValue.getValue().equals("EARTH-MOON BARYCENTER") ||
                        keyValue.getValue().equals("EARTH BARYCENTER") || keyValue.getValue().equals("EMB")) {
                    canonicalValue = "EARTH_MOON";
                } else {
                    canonicalValue = keyValue.getValue();
                }
                for (final CenterName c : CenterName.values()) {
                    if (c.name().equals(canonicalValue)) {
                        metaData.setHasCreatableBody(true);
                        final CelestialBodies celestialBodies =
                                getDataContext().getCelestialBodies();
                        metaData.setCenterBody(c.getCelestialBody(celestialBodies));
                        metaData.getADMFile().setMu( c.getCelestialBody(celestialBodies).getGM());
                    }
                }
                return true;

            case TIME_SYSTEM:
                if (!CcsdsTimeScale.contains(keyValue.getValue())) {
                    throw new OrekitException(
                            OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED,
                            keyValue.getValue());
                }
                final CcsdsTimeScale timeSystem =
                        CcsdsTimeScale.valueOf(keyValue.getValue());
                metaData.setTimeSystem(timeSystem);
                return true;

            default:
                return false;
        }
    }

    /**
     * Parse a general state data key = value entry.
     * @param keyValue key = value pair
     * @param general instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseGeneralStateDataEntry(final KeyValue keyValue,
                                                 final APMFile general, final List<String> comment) {
        switch (keyValue.getKeyword()) {

            case EPOCH:
                general.setEpochComment(comment);
                comment.clear();
                general.setEpoch(parseDate(keyValue.getValue(), general.getMetaData().getTimeSystem()));
                return true;

            case QC_DOT:
                general.setQuaternionDot(new Quaternion(keyValue.getDoubleValue(),
                                                        general.getQuaternionDot().getQ1(),
                                                        general.getQuaternionDot().getQ2(),
                                                        general.getQuaternionDot().getQ3()));
                return true;

            case Q1_DOT:
                general.setQuaternionDot(new Quaternion(general.getQuaternionDot().getQ0(),
                                                        keyValue.getDoubleValue(),
                                                        general.getQuaternionDot().getQ2(),
                                                        general.getQuaternionDot().getQ3()));
                return true;

            case Q2_DOT:
                general.setQuaternionDot(new Quaternion(general.getQuaternionDot().getQ0(),
                                                        general.getQuaternionDot().getQ1(),
                                                        keyValue.getDoubleValue(),
                                                        general.getQuaternionDot().getQ3()));
                return true;

            case Q3_DOT:
                general.setQuaternionDot(new Quaternion(general.getQuaternionDot().getQ0(),
                                                        general.getQuaternionDot().getQ1(),
                                                        general.getQuaternionDot().getQ2(),
                                                        keyValue.getDoubleValue()));
                return true;

            case EULER_FRAME_A:
                general.setEulerComment(comment);
                comment.clear();
                general.setEulerFrameAString(keyValue.getValue());
                return true;

            case EULER_FRAME_B:
                general.setEulerFrameBString(keyValue.getValue());
                return true;

            case EULER_DIR:
                general.setEulerDirection(keyValue.getValue());
                return true;

            case EULER_ROT_SEQ:
                general.setEulerRotSeq(keyValue.getValue());
                return true;

            case RATE_FRAME:
                general.setRateFrameString(keyValue.getValue());
                return true;

            case X_ANGLE:
                general.setRotationAngles(new Vector3D(toRadians(keyValue),
                                                       general.getRotationAngles().getY(),
                                                       general.getRotationAngles().getZ()));
                return true;

            case Y_ANGLE:
                general.setRotationAngles(new Vector3D(general.getRotationAngles().getX(),
                                                       toRadians(keyValue),
                                                       general.getRotationAngles().getZ()));
                return true;

            case Z_ANGLE:
                general.setRotationAngles(new Vector3D(general.getRotationAngles().getX(),
                                                       general.getRotationAngles().getY(),
                                                       toRadians(keyValue)));
                return true;

            case X_RATE:
                general.setRotationRates(new Vector3D(toRadians(keyValue),
                                                      general.getRotationRates().getY(),
                                                      general.getRotationRates().getZ()));
                return true;

            case Y_RATE:
                general.setRotationRates(new Vector3D(general.getRotationRates().getX(),
                                                      toRadians(keyValue),
                                                      general.getRotationRates().getZ()));
                return true;

            case Z_RATE:
                general.setRotationRates(new Vector3D(general.getRotationRates().getX(),
                                                      general.getRotationRates().getY(),
                                                      toRadians(keyValue)));
                return true;

            case SPIN_FRAME_A:
                general.setSpinComment(comment);
                comment.clear();
                general.setSpinFrameAString(keyValue.getValue());
                return true;

            case SPIN_FRAME_B:
                general.setSpinFrameBString(keyValue.getValue());
                return true;

            case SPIN_DIR:
                general.setSpinDirection(keyValue.getValue());
                return true;

            case SPIN_ALPHA:
                general.setSpinAlpha(toRadians(keyValue));
                return true;

            case SPIN_DELTA:
                general.setSpinDelta(toRadians(keyValue));
                return true;

            case SPIN_ANGLE:
                general.setSpinAngle(toRadians(keyValue));
                return true;

            case SPIN_ANGLE_VEL:
                general.setSpinAngleVel(toRadians(keyValue));
                return true;

            case NUTATION:
                general.setNutation(toRadians(keyValue));
                return true;

            case NUTATION_PER:
                general.setNutationPeriod(keyValue.getDoubleValue());
                return true;

            case NUTATION_PHASE:
                general.setNutationPhase(toRadians(keyValue));
                return true;

            case INERTIA_REF_FRAME:
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setInertiaRefFrameString(keyValue.getValue());
                return true;

            case I11:
                general.setI11(keyValue.getDoubleValue());
                return true;

            case I22:
                general.setI22(keyValue.getDoubleValue());
                return true;

            case I33:
                general.setI33(keyValue.getDoubleValue());
                return true;

            case I12:
                general.setI12(keyValue.getDoubleValue());
                return true;

            case I13:
                general.setI13(keyValue.getDoubleValue());
                return true;

            case I23:
                general.setI23(keyValue.getDoubleValue());
                return true;

            default:
                return false;

        }

    }

    /**
     * Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @return parsed date
     */
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem) {
        return timeSystem.parseDate(date, conventions, missionReferenceDate,
                getDataContext().getTimeScales());
    }

    /**
     * Convert a {@link KeyValue} in degrees to a real value in randians.
     * @param keyValue key value
     * @return the value in radians
     */
    protected double toRadians(final KeyValue keyValue) {
        return FastMath.toRadians(keyValue.getDoubleValue());
    }

}
