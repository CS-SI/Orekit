/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.files.general.OrbitFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Base class for all CCSDS Orbit Data Message parsers.
 * <p>
 * This base class is immutable, and hence thread safe. When parts
 * must be changed, such as reference date for Mission Elapsed Time or
 * Mission Relative Time time systems, or the gravitational coefficient or
 * the IERS conventions, the various {@code withXxx} methods must be called,
 * which create a new immutable instance with the new parameters. This
 * is a combination of the <a href="">builder design pattern</a> and
 * a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent
 * interface</a>.
 * </p>
 * @author Luc Maisonobe
 * @since 6.1
 */
public abstract class ODMParser {

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient. */
    private final  double mu;

    /** IERS Conventions. */
    private final  IERSConventions conventions;

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     */
    protected ODMParser(final AbsoluteDate missionReferenceDate, final double mu, final IERSConventions conventions) {
        this.missionReferenceDate = missionReferenceDate;
        this.mu                   = mu;
        this.conventions          = conventions;
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public abstract ODMParser withMissionReferenceDate(final AbsoluteDate newMissionReferenceDate);

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
    public abstract ODMParser withMu(final double newMu);

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
    public abstract ODMParser withConventions(final IERSConventions newConventions);

    /** Get IERS conventions.
     * @return IERS conventions to use while parsing
     * @see #withConventions(IERSConventions)
     */
    public IERSConventions getConventions() {
        return conventions;
    }

    /** Parse a comment line.
     * @param line complete line entry
     * @param keyword key part of the key = value entry
     * @param comment placeholder where the current comment line should be added
     * @return true if the line was a comment line and was parsed
     */
    protected boolean parseComment(final String line, final Keyword keyword, final List<String> comment) {
        if (keyword == Keyword.COMMENT) {
            comment.add(line.split(" +", 2)[1]);
            return true;
        } else {
            return false;
        }
    }

    /** Parse an entry from the header.
     * @param keyword key part of the key = value entry
     * @param value value part of the key = value entry
     * @param odmFile instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a header keyword and has been parsed
     * @exception OrekitException if UTC time scale cannot be retrieved to parse creation date
     */
    protected boolean parseHeaderEntry(final Keyword keyword, final String value,
                                       final ODMFile odmFile, final List<String> comment)
        throws OrekitException {
        switch (keyword) {

        case CREATION_DATE:
            if (!comment.isEmpty()) {
                odmFile.setHeaderComment(comment);
                comment.clear();
            }
            odmFile.setCreationDate(new AbsoluteDate(value, TimeScalesFactory.getUTC()));
            return true;

        case ORIGINATOR:
            odmFile.setOriginator(value);
            return true;

        default:
            return false;

        }

    }

    /** Parse a meta-data key = value entry.
     * @param line complete line entry
     * @param keyword key part of the key = value entry
     * @param value value part of the key = value entry
     * @param metaData instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     * @exception OrekitException if center body or frame cannot be retrieved
     */
    protected boolean parseMetaDataEntry(final String line, final Keyword keyword, final String value,
                                         final ODMMetaData metaData, final List<String> comment)
        throws OrekitException {
        switch (keyword) {
        case OBJECT_NAME:
            if (!comment.isEmpty()) {
                metaData.setComment(comment);
                comment.clear();
            }
            metaData.setObjectName(line.split("=", 2)[1].trim());
            return true;

        case OBJECT_ID:
            metaData.setObjectID(value);
            return true;

        case CENTER_NAME:
            metaData.setCenterName(value);
            final String canonicalValue;
            if (value.equals("SOLAR SYSTEM BARYCENTER") || value.equals("SSB")) {
                canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
            } else if (value.equals("EARTH MOON BARYCENTER") || value.equals("EARTH-MOON BARYCENTER") ||
                       value.equals("EARTH BARYCENTER") || value.equals("EMB")) {
                canonicalValue = "EARTH_MOON";
            } else {
                canonicalValue = value;
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
            metaData.setRefFrame(parseCCSDSFrame(value).getFrame(getConventions()));
            return true;

        case REF_FRAME_EPOCH:
            metaData.setFrameEpochString(value);
            return true;

        case TIME_SYSTEM:
            final OrbitFile.TimeSystem timeSystem = OrbitFile.TimeSystem.valueOf(value);
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
     * @param line complete line entry
     * @param keyword key part of the key = value entry
     * @param value value part of the key = value entry
     * @param general instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @param userDefinedKeyword user defined keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     * @exception OrekitException if center body or frame cannot be retrieved
     */
    protected boolean parseGeneralStateDataEntry(final String line, final Keyword keyword, final String value,
                                                 final OGMFile general, final List<String> comment,
                                                 final String userDefinedKeyword)
        throws OrekitException {
        switch (keyword) {

        case EPOCH:
            general.setEpochComment(comment);
            comment.clear();
            general.setEpoch(parseDate(value, general.getTimeSystem()));
            return true;

        case SEMI_MAJOR_AXIS:
            general.setKeplerianElementsComment(comment);
            comment.clear();
            general.setA(Double.parseDouble(value) * 1000);
            general.setHasKeplerianElements(true);
            return true;

        case ECCENTRICITY:
            general.setE(Double.parseDouble(value));
            return true;

        case INCLINATION:
            general.setI(FastMath.toRadians(Double.parseDouble(value)));
            return true;

        case RA_OF_ASC_NODE:
            general.setRaan(FastMath.toRadians(Double.parseDouble(value)));
            return true;

        case ARG_OF_PERICENTER:
            general.setPa(FastMath.toRadians(Double.parseDouble(value)));
            return true;

        case TRUE_ANOMALY:
            general.setAnomalyType("TRUE");
            general.setAnomaly(FastMath.toRadians(Double.parseDouble(value)));
            return true;

        case MEAN_ANOMALY:
            general.setAnomalyType("MEAN");
            general.setAnomaly(FastMath.toRadians(Double.parseDouble(value)));
            return true;

        case GM:
            general.setMuParsed(Double.parseDouble(value) * 1e9);
            return true;

        case MASS:
            comment.addAll(0, general.getSpacecraftComment());
            general.setSpacecraftComment(comment);
            comment.clear();
            general.setMass(Double.parseDouble(value));
            return true;

        case SOLAR_RAD_AREA:
            comment.addAll(0, general.getSpacecraftComment());
            general.setSpacecraftComment(comment);
            comment.clear();
            general.setSolarRadArea(Double.parseDouble(value));
            return true;

        case SOLAR_RAD_COEFF:
            comment.addAll(0, general.getSpacecraftComment());
            general.setSpacecraftComment(comment);
            comment.clear();
            general.setSolarRadCoeff(Double.parseDouble(value));
            return true;

        case DRAG_AREA:
            comment.addAll(0, general.getSpacecraftComment());
            general.setSpacecraftComment(comment);
            comment.clear();
            general.setDragArea(Double.parseDouble(value));
            return true;

        case DRAG_COEFF:
            comment.addAll(0, general.getSpacecraftComment());
            general.setSpacecraftComment(comment);
            comment.clear();
            general.setDragCoeff(Double.parseDouble(value));
            return true;

        case COV_REF_FRAME:
            general.setCovarianceComment(comment);
            comment.clear();
            final CCSDSFrame covFrame = parseCCSDSFrame(value);
            if (covFrame.isLof()) {
                general.setCovRefLofType(covFrame.getLofType());
            } else {
                general.setCovRefFrame(covFrame.getFrame(getConventions()));
            }
            return true;

        case CX_X:
            general.createCovarianceMatrix();
            general.setCovarianceMatrixEntry(0, 0, Double.parseDouble(value));
            return true;

        case CY_X:
            general.setCovarianceMatrixEntry(0, 1, Double.parseDouble(value));
            return true;

        case CY_Y:
            general.setCovarianceMatrixEntry(1, 1, Double.parseDouble(value));
            return true;

        case CZ_X:
            general.setCovarianceMatrixEntry(0, 2, Double.parseDouble(value));
            return true;

        case CZ_Y:
            general.setCovarianceMatrixEntry(1, 2, Double.parseDouble(value));
            return true;

        case CZ_Z:
            general.setCovarianceMatrixEntry(2, 2, Double.parseDouble(value));
            return true;

        case CX_DOT_X:
            general.setCovarianceMatrixEntry(0, 3, Double.parseDouble(value));
            return true;

        case CX_DOT_Y:
            general.setCovarianceMatrixEntry(1, 3, Double.parseDouble(value));
            return true;

        case CX_DOT_Z:
            general.setCovarianceMatrixEntry(2, 3, Double.parseDouble(value));
            return true;

        case CX_DOT_X_DOT:
            general.setCovarianceMatrixEntry(3, 3, Double.parseDouble(value));
            return true;

        case CY_DOT_X:
            general.setCovarianceMatrixEntry(0, 4, Double.parseDouble(value));
            return true;

        case CY_DOT_Y:
            general.setCovarianceMatrixEntry(1, 4, Double.parseDouble(value));
            return true;

        case CY_DOT_Z:
            general.setCovarianceMatrixEntry(2, 4, Double.parseDouble(value));
            return true;

        case CY_DOT_X_DOT:
            general.setCovarianceMatrixEntry(3, 4, Double.parseDouble(value));
            return true;

        case CY_DOT_Y_DOT:
            general.setCovarianceMatrixEntry(4, 4, Double.parseDouble(value));
            return true;

        case CZ_DOT_X:
            general.setCovarianceMatrixEntry(0, 5, Double.parseDouble(value));
            return true;

        case CZ_DOT_Y:
            general.setCovarianceMatrixEntry(1, 5, Double.parseDouble(value));
            return true;

        case CZ_DOT_Z:
            general.setCovarianceMatrixEntry(2, 5, Double.parseDouble(value));
            return true;

        case CZ_DOT_X_DOT:
            general.setCovarianceMatrixEntry(3, 5, Double.parseDouble(value));
            return true;

        case CZ_DOT_Y_DOT:
            general.setCovarianceMatrixEntry(4, 5, Double.parseDouble(value));
            return true;

        case CZ_DOT_Z_DOT:
            general.setCovarianceMatrixEntry(5, 5, Double.parseDouble(value));
            return true;

        case USER_DEFINED_X:
            general.setUserDefinedParameters(userDefinedKeyword, value);
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
    protected AbsoluteDate parseDate(final String date, final OrbitFile.TimeSystem timeSystem)
        throws OrekitException {
        switch (timeSystem) {
        case GMST:
            return new AbsoluteDate(date, TimeScalesFactory.getGMST());
        case GPS:
            return new AbsoluteDate(date, TimeScalesFactory.getGPS());
        case TAI:
            return new AbsoluteDate(date, TimeScalesFactory.getTAI());
        case TCB:
            return new AbsoluteDate(date, TimeScalesFactory.getTCB());
        case TDB:
            return new AbsoluteDate(date, TimeScalesFactory.getTDB());
        case TCG:
            return new AbsoluteDate(date, TimeScalesFactory.getTCG());
        case TT:
            return new AbsoluteDate(date, TimeScalesFactory.getTT());
        case UT1:
            return new AbsoluteDate(date, TimeScalesFactory.getUT1(conventions));
        case UTC:
            return new AbsoluteDate(date, TimeScalesFactory.getUTC());
        case MET: {
            final DateTimeComponents clock = DateTimeComponents.parseDateTime(date);
            final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                    clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                    clock.getTime().getSecondsInDay();
            return missionReferenceDate.shiftedBy(offset);
        }
        case MRT: {
            final DateTimeComponents clock = DateTimeComponents.parseDateTime(date);
            final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                    clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                    clock.getTime().getSecondsInDay();
            return missionReferenceDate.shiftedBy(offset);
        }
        default:
            throw OrekitException.createInternalError(null);
        }
    }

}
