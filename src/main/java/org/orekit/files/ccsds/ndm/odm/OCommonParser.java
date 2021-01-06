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
package org.orekit.files.ccsds.ndm.odm;

import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodies;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Common parser for Orbit Parameter/Ephemeris/Mean Message files.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class OCommonParser<T extends ODMFile<?>> extends ODMParser<T> {

    /** Pattern for dash. */
    private static final Pattern DASH = Pattern.compile("-");

    /** Reference date for Mission Elapsed Time or Mission Relative Time time systems. */
    private final AbsoluteDate missionReferenceDate;

    /** Gravitational coefficient. */
    private final  double mu;

    /** Complete constructor.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @since 10.1
     */
    protected OCommonParser(final AbsoluteDate missionReferenceDate, final double mu,
                            final IERSConventions conventions, final boolean simpleEOP,
                            final DataContext dataContext) {
        super(conventions, simpleEOP, dataContext);
        this.missionReferenceDate = missionReferenceDate;
        this.mu                   = mu;
    }

    /** Set initial date.
     * @param newMissionReferenceDate mission reference date to use while parsing
     * @return a new instance, with mission reference date replaced
     * @see #getMissionReferenceDate()
     */
    public abstract OCommonParser<T> withMissionReferenceDate(AbsoluteDate newMissionReferenceDate);

    /** Get initial date.
     * @return mission reference date to use while parsing
     * @see #withMissionReferenceDate(AbsoluteDate)
     */
    public AbsoluteDate getMissionReferenceDate() {
        return missionReferenceDate;
    }

    /** Set gravitational coefficient.
     * @param newMu gravitational coefficient to use while parsing
     * @return a new instance, with gravitational coefficient value replaced
     * @see #getMu()
     */
    public abstract OCommonParser<T> withMu(double newMu);

    /** Get gravitational coefficient.
     * @return gravitational coefficient to use while parsing
     * @see #withMu(double)
     */
    public double getMu() {
        return mu;
    }

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

    /** Parse a meta-data key = value entry.
     * @param keyValue key = value pair
     * @param metaData instance to update with parsed entry
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseMetaDataEntry(final KeyValue keyValue, final OCommonMetadata metaData) {
        if (super.parseMetaDataEntry(keyValue, metaData)) {
            return true;
        } else {
            switch (keyValue.getKeyword()) {

                case OBJECT_ID: {
                    metaData.setObjectID(keyValue.getValue());
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
                            metaData.setMuCreated(c.getCelestialBody(celestialBodies).getGM());
                        }
                    }
                    return true;

                case REF_FRAME:
                    metaData.setFrameString(keyValue.getValue());
                    metaData.setRefFrame(parseCCSDSFrame(keyValue.getValue())
                                         .getFrame(getConventions(), isSimpleEOP(), getDataContext()));
                    return true;

                case REF_FRAME_EPOCH:
                    metaData.setFrameEpochString(keyValue.getValue());
                    return true;

                case TIME_SYSTEM:
                    final CcsdsTimeScale timeSystem = CcsdsTimeScale.parse(keyValue.getValue());
                    metaData.setTimeSystem(timeSystem);
                    if (metaData.getFrameEpochString() != null) {
                        metaData.setFrameEpoch(parseDate(metaData.getFrameEpochString(), timeSystem));
                    }
                    return true;

                default:
                    return false;
            }
        }
    }

    /** Parse a general state data key = value entry.
     * @param keyValue key = value pair
     * @param general instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseGeneralStateDataEntry(final KeyValue keyValue,
                                                 final T general, final List<String> comment) {
        switch (keyValue.getKeyword()) {

            case EPOCH:
                general.setEpochComment(comment);
                comment.clear();
                general.setEpoch(parseDate(keyValue.getValue(), general.getMetadata().getTimeSystem()));
                return true;

            case SEMI_MAJOR_AXIS:
                // as we have found semi major axis we don't expect mean motion anymore
                declareFound(Keyword.MEAN_MOTION);
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
                    general.setCovRefFrame(covFrame
                            .getFrame(getConventions(), isSimpleEOP(), getDataContext()));
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
        return CCSDSFrame.valueOf(DASH.matcher(frameName).replaceAll(""));
    }

    /** Parse a date.
     * @param date date to parse, as the value of a CCSDS key=value line
     * @param timeSystem time system to use
     * @return parsed date
     */
    protected AbsoluteDate parseDate(final String date, final CcsdsTimeScale timeSystem) {
        return timeSystem.parseDate(date, getConventions(), missionReferenceDate,
                getDataContext().getTimeScales());
    }

}
