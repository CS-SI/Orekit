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

import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.Keyword;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.KeyValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Parser for general state data present in both OPM and OMM files.
 * @param <T> type of the ODM file
 * @param <P> type of the parser
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class OStateParser<T extends ODMFile<?>, P extends ODMParser<T, ?>> extends OCommonParser<T, P> {

    /** Complete constructor.
     * @param conventions IERS Conventions
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext used to retrieve frames and time scales.
     * @param missionReferenceDate reference date for Mission Elapsed Time or Mission Relative Time time systems
     * @param mu gravitational coefficient
     */
    protected OStateParser(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext,
                           final AbsoluteDate missionReferenceDate, final double mu) {
        super(conventions, simpleEOP, dataContext, missionReferenceDate, mu);
    }

    /** Parse a general state data key = value entry.
     * @param keyValue key = value pair
     * @param metadata metadata used for interpreting data
     * @param data instance to update with parsed entry
     * @param comment previous comment lines, will be emptied if used by the keyword
     * @return true if the keyword was a meta-data keyword and has been parsed
     */
    protected boolean parseGeneralStateDataEntry(final KeyValue keyValue, final OCommonMetadata metadata,
                                                 final OStateData data, final List<String> comment) {
        switch (keyValue.getKeyword()) {

            case EPOCH:
                data.setEpochComment(comment);
                comment.clear();
                data.setEpoch(parseDate(keyValue.getValue(), metadata.getTimeSystem()));
                return true;

            case SEMI_MAJOR_AXIS:
                // as we have found semi major axis we don't expect mean motion anymore
                declareFound(Keyword.MEAN_MOTION);
                data.setKeplerianElementsComment(comment);
                comment.clear();
                data.setA(keyValue.getDoubleValue() * 1000);
                data.setHasKeplerianElements(true);
                return true;

            case ECCENTRICITY:
                data.setE(keyValue.getDoubleValue());
                return true;

            case INCLINATION:
                data.setI(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case RA_OF_ASC_NODE:
                data.setRaan(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case ARG_OF_PERICENTER:
                data.setPa(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case TRUE_ANOMALY:
                data.setAnomalyType("TRUE");
                data.setAnomaly(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case MEAN_ANOMALY:
                data.setAnomalyType("MEAN");
                data.setAnomaly(FastMath.toRadians(keyValue.getDoubleValue()));
                return true;

            case GM:
                data.setMu(keyValue.getDoubleValue() * 1.0e9);
                setMuParsed(data.getMu());
                return true;

            case MASS:
                comment.addAll(0, data.getSpacecraftComment());
                data.setSpacecraftComment(comment);
                comment.clear();
                data.setMass(keyValue.getDoubleValue());
                return true;

            case SOLAR_RAD_AREA:
                comment.addAll(0, data.getSpacecraftComment());
                data.setSpacecraftComment(comment);
                comment.clear();
                data.setSolarRadArea(keyValue.getDoubleValue());
                return true;

            case SOLAR_RAD_COEFF:
                comment.addAll(0, data.getSpacecraftComment());
                data.setSpacecraftComment(comment);
                comment.clear();
                data.setSolarRadCoeff(keyValue.getDoubleValue());
                return true;

            case DRAG_AREA:
                comment.addAll(0, data.getSpacecraftComment());
                data.setSpacecraftComment(comment);
                comment.clear();
                data.setDragArea(keyValue.getDoubleValue());
                return true;

            case DRAG_COEFF:
                comment.addAll(0, data.getSpacecraftComment());
                data.setSpacecraftComment(comment);
                comment.clear();
                data.setDragCoeff(keyValue.getDoubleValue());
                return true;

            case COV_REF_FRAME:
                data.setCovarianceComment(comment);
                comment.clear();
                final CCSDSFrame covFrame = parseCCSDSFrame(keyValue.getValue());
                if (covFrame.isLof()) {
                    data.setCovRefLofType(covFrame.getLofType());
                } else {
                    data.setCovRefFrame(covFrame
                            .getFrame(getConventions(), isSimpleEOP(), getDataContext()));
                }
                return true;

            case CX_X:
                data.createCovarianceMatrix();
                data.setCovarianceMatrixEntry(0, 0, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_X:
                data.setCovarianceMatrixEntry(0, 1, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_Y:
                data.setCovarianceMatrixEntry(1, 1, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_X:
                data.setCovarianceMatrixEntry(0, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_Y:
                data.setCovarianceMatrixEntry(1, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_Z:
                data.setCovarianceMatrixEntry(2, 2, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_X:
                data.setCovarianceMatrixEntry(0, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_Y:
                data.setCovarianceMatrixEntry(1, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_Z:
                data.setCovarianceMatrixEntry(2, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CX_DOT_X_DOT:
                data.setCovarianceMatrixEntry(3, 3, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_X:
                data.setCovarianceMatrixEntry(0, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Y:
                data.setCovarianceMatrixEntry(1, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Z:
                data.setCovarianceMatrixEntry(2, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_X_DOT:
                data.setCovarianceMatrixEntry(3, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CY_DOT_Y_DOT:
                data.setCovarianceMatrixEntry(4, 4, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_X:
                data.setCovarianceMatrixEntry(0, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Y:
                data.setCovarianceMatrixEntry(1, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Z:
                data.setCovarianceMatrixEntry(2, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_X_DOT:
                data.setCovarianceMatrixEntry(3, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Y_DOT:
                data.setCovarianceMatrixEntry(4, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case CZ_DOT_Z_DOT:
                data.setCovarianceMatrixEntry(5, 5, keyValue.getDoubleValue() * 1.0e6);
                return true;

            case USER_DEFINED_X:
                data.setUserDefinedParameters(keyValue.getKey(), keyValue.getValue());
                return true;

            default:
                return false;
        }
    }

}
