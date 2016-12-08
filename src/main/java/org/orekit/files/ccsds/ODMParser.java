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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

/**
 * Base class for all CCSDS Orbit Data Message parsers.
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
 * @author Luc Maisonobe
 * @since 6.1
 */
public abstract class ODMParser {

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
     */
    protected ODMParser(final AbsoluteDate missionReferenceDate, final double mu,
                        final IERSConventions conventions, final boolean simpleEOP,
                        final int launchYear, final int launchNumber, final String launchPiece) {
        this.missionReferenceDate = missionReferenceDate;
        this.mu                   = mu;
        this.conventions          = conventions;
        this.simpleEOP            = simpleEOP;
        this.launchYear           = launchYear;
        this.launchNumber         = launchNumber;
        this.launchPiece          = launchPiece;
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public abstract ODMParser withMissionReferenceDate(AbsoluteDate newMissionReferenceDate);

    /** Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient date replaced
     * @see #getMu()
     */
    public abstract ODMParser withMu(double newMu);

    /** Get gravitational coefficient.
     * @return gravitational coefficient to use while parsing
     * @see #withMu(double)
     */
    public double getMu() {
        return mu;
    }

    /** Set IERS conventions.
     * @param newConventions IERS conventions to use while parsing
     * @return a new instance, with IERS conventions replaced
     * @see #getConventions()
     */
    public abstract ODMParser withConventions(IERSConventions newConventions);

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Set EOP interpolation method.
     * @param newSimpleEOP if true, tidal effects are ignored when interpolating EOP
     * @return a new instance, with EOP interpolation method replaced
     * @see #isSimpleEOP()
     */
    public abstract ODMParser withSimpleEOP(boolean newSimpleEOP);

    /** Get EOP interpolation method.
     * @return true if tidal effects are ignored when interpolating EOP
     * @see #withSimpleEOP(boolean)
     */
    public boolean isSimpleEOP() {
        return simpleEOP;
    }

    /** Set international designator.
     * <p>
     * This method may be used to ensure the launch year number and pieces are
     * correctly set if they are not present in the CCSDS file header in the
     * OBJECT_ID in the form YYYY-NNN-P{PP}. If they are already in the header,
     * they will be parsed automatically regardless of this method being called
     * or not (i.e. header information override information set here).
     * </p>
     * @param newLaunchYear launch year
     * @param newLaunchNumber launch number
     * @param newLaunchPiece piece of launch (from "A" to "ZZZ")
     * @return a new instance, with TLE settings replaced
     */
    public abstract ODMParser withInternationalDesignator(int newLaunchYear,
                                                          int newLaunchNumber,
                                                          String newLaunchPiece);

    /** Get the launch year.
     * @return launch year
     */
    public int getLaunchYear() {
        return launchYear;
    }

    /** Get the launch number.
     * @return launch number
     */
    public int getLaunchNumber() {
        return launchNumber;
    }

    /** Get the piece of launch.
     * @return piece of launch
     */
    public String getLaunchPiece() {
        return launchPiece;
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param fileName name of the file containing the message
     * @return parsed orbit
     * @exception OrekitException if orbit message cannot be parsed
     */
    public ODMFile parse(final String fileName)
        throws OrekitException {
        try (InputStream stream = new FileInputStream(fileName)) {
            return parse(stream, fileName);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @return parsed orbit
     * @exception OrekitException if orbit message cannot be parsed
     */
    public ODMFile parse(final InputStream stream)
        throws OrekitException {
        return parse(stream, "<unknown>");
    }

    /** Parse a CCSDS Orbit Data Message.
     * @param stream stream containing message
     * @param fileName name of the file containing the message (for error messages)
     * @return parsed orbit
     * @exception OrekitException if orbit message cannot be parsed
     */
    public abstract ODMFile parse(InputStream stream, String fileName)
        throws OrekitException;

    /** Parse a comment line.
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

    /** Parse an entry from the header.
     * @param keyValue key = value pair
     * @param odmFile instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a header keyword and has been parsed
     * @exception OrekitException if UTC time scale cannot be retrieved to parse creation date
     */
    protected boolean parseHeaderEntry(final KeyValue keyValue,
                                       final ODMFile odmFile, final List<String> comment)
        throws OrekitException {
        switch (keyValue.getKeyword()) {

            case CREATION_DATE:
                if (!comment.isEmpty()) {
                    odmFile.setHeaderComment(comment);
                    comment.clear();
                }
                odmFile.setCreationDate(new AbsoluteDate(keyValue.getValue(), TimeScalesFactory.getUTC()));
                return true;

            case ORIGINATOR:
                odmFile.setOriginator(keyValue.getValue());
                return true;

            default:
                return false;

        }

    }

    /** Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     * @exception OrekitException if center body or frame cannot be retrieved
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue,
                                         final ODMMetaData metaData, final List<String> comment)
        throws OrekitException {
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
                        metaData.setCenterBody(c.getCelestialBody());
                        metaData.getODMFile().setMuCreated(c.getCelestialBody().getGM());
                    }
                }
                return true;

            case REF_FRAME:
                metaData.setFrameString(keyValue.getValue());
                metaData.setRefFrame(parseCCSDSFrame(keyValue.getValue()).getFrame(getConventions(), isSimpleEOP()));
                return true;

            case REF_FRAME_EPOCH:
                metaData.setFrameEpochString(keyValue.getValue());
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
                if (metaData.getFrameEpochString() != null) {
                    metaData.setFrameEpoch(parseDate(metaData.getFrameEpochString(), timeSystem));
                }
                return true;

            default:
                return false;
        }
    }

    /** Parse a general state data key = value entry.
     * @param keyValue key = value pair
     * @param general instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     * @exception OrekitException if center body or frame cannot be retrieved
     */
    protected boolean parseGeneralStateDataEntry(final KeyValue keyValue,
                                                 final OGMFile general, final List<String> comment)
        throws OrekitException {
        switch (keyValue.getKeyword()) {

            case EPOCH:
                general.setEpochComment(comment);
                comment.clear();
                general.setEpoch(parseDate(keyValue.getValue(), general.getMetaData().getTimeSystem()));
                return true;

            case SEMI_MAJOR_AXIS:
                general.setKeplerianElementsComment(comment);
                comment.clear();
                general.setA(keyValue.getDoubleValue() * 1000);
                general.setHasKeplerianElements(true);
                return true;

            case ECCENTRICITY:
                general.setE(keyValue.getDoubleValue());
                return true;

            case INCLINATION:
                general.setI(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case RA_OF_ASC_NODE:
                general.setRaan(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case ARG_OF_PERICENTER:
                general.setPa(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case TRUE_ANOMALY:
                general.setAnomalyType("TRUE");
                general.setAnomaly(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case MEAN_ANOMALY:
                general.setAnomalyType("MEAN");
                general.setAnomaly(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case GM:
                general.setMuParsed(keyValue.getDoubleValue() * 1e9);
                return true;

            case MASS:
                comment.addAll(0, general.getSpacecraftComment());
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setMass(keyValue.getDoubleValue());
                return true;

            case SOLAR_RAD_AREA:
                comment.addAll(0, general.getSpacecraftComment());
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setSolarRadArea(keyValue.getDoubleValue());
                return true;

            case SOLAR_RAD_COEFF:
                comment.addAll(0, general.getSpacecraftComment());
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setSolarRadCoeff(keyValue.getDoubleValue());
                return true;

            case DRAG_AREA:
                comment.addAll(0, general.getSpacecraftComment());
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setDragArea(keyValue.getDoubleValue());
                return true;

            case DRAG_COEFF:
                comment.addAll(0, general.getSpacecraftComment());
                general.setSpacecraftComment(comment);
                comment.clear();
                general.setDragCoeff(keyValue.getDoubleValue());
                return true;

            case COV_REF_FRAME:
                general.setCovarianceComment(comment);
                comment.clear();
                final CCSDSFrame covFrame = parseCCSDSFrame(keyValue.getValue());
                if (covFrame.isLof()) {
                    general.setCovRefLofType(covFrame.getLofType());
                } else {
                    general.setCovRefFrame(covFrame.getFrame(getConventions(), isSimpleEOP()));
                }
                return true;

            case CX_X:
                general.createCovarianceMatrix();
                general.setCovarianceMatrixEntry(0, 0, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_X:
                general.setCovarianceMatrixEntry(0, 1, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_Y:
                general.setCovarianceMatrixEntry(1, 1, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_X:
                general.setCovarianceMatrixEntry(0, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_Y:
                general.setCovarianceMatrixEntry(1, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_Z:
                general.setCovarianceMatrixEntry(2, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_X:
                general.setCovarianceMatrixEntry(0, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_Y:
                general.setCovarianceMatrixEntry(1, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_Z:
                general.setCovarianceMatrixEntry(2, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_X_DOT:
                general.setCovarianceMatrixEntry(3, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_X:
                general.setCovarianceMatrixEntry(0, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Y:
                general.setCovarianceMatrixEntry(1, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Z:
                general.setCovarianceMatrixEntry(2, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_X_DOT:
                general.setCovarianceMatrixEntry(3, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Y_DOT:
                general.setCovarianceMatrixEntry(4, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_X:
                general.setCovarianceMatrixEntry(0, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Y:
                general.setCovarianceMatrixEntry(1, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Z:
                general.setCovarianceMatrixEntry(2, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_X_DOT:
                general.setCovarianceMatrixEntry(3, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Y_DOT:
                general.setCovarianceMatrixEntry(4, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Z_DOT:
                general.setCovarianceMatrixEntry(5, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case USER_DEFINED_X:
                general.setUserDefinedParameters(keyValue.getKey(), keyValue.getValue());
                return true;

            default:
                return false;
        }
    }

    /** Parse a CCSDS frame.
     * @param frameName name of the frame, as the value of a CCSDS key=value line
     * @return CCSDS frame corresponding to the name
     */
    protected CCSDSFrame parseCCSDSFrame(final String frameName) {
        return CCSDSFrame.valueOf(frameName.replaceAll("-", ""));
    }

    /** Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @return parsed date
     * @exception OrekitException if some time scale cannot be retrieved
     */
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem)
        throws OrekitException {
        return timeSystem.parseDate(date, conventions, missionReferenceDate);
    }

}
