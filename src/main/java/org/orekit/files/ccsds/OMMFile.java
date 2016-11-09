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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;

/**
 * This class gathers the informations present in the Orbital Mean-Elements Message (OMM),
 * and contains methods to generate a {@link CartesianOrbit}, a {@link KeplerianOrbit},
 * a {@link SpacecraftState} and, eventually, a {@link TLE}.
 * @author sports
 * @since 6.1
 */
public class OMMFile extends OGMFile {

    /** Meta-data. */
    private final OMMMetaData metaData;

    /** Mean motion (the Keplerian Mean motion in revolutions per day). To be used instead of semi-major
     * axis if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private double meanMotion;

    /** Ephemeris Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the coding for
     * the EPHEMERIS_TYPE keyword: 1 = SGP, 2 = SGP4, 3 = SDP4, 4 = SGP8, 5 = SDP8. Default value = 0.
     */
    private int ephemerisType;

    /** Classification Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the
     *  following coding for the CLASSIFICATION_TYPE keyword: U = unclassified, S = secret. Default value = U.
     */
    private char classificationType;

    /** NORAD Catalog Number ("Satellite Number"), an integer of up to nine digits. */
    private Integer noradID;

    /** Element set number for this satellite, only required if MEAN_ELEMENT_THEORY = SGP/SGP4.
     * Normally incremented sequentially, but may be out of sync if it is generated from a backup source.
     * Used to distinguish different TLEs, and therefore only meaningful if TLE based data is being exchanged. */
    private String elementSetNo;

    /** Revolution Number, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private int revAtEpoch;

    /** SGP/SGP4 drag-like coefficient (in units 1/[Earth radii]), only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private Double bStar;

    /** First Time Derivative of the Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private Double meanMotionDot;

    /** Second Time Derivative of Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private Double meanMotionDotDot;

    /** TLE related parameters comments. The list contains a string for each line of comment. */
    private List<String> dataTleRelatedParametersComment;

    /** Create a new OMM file object. */
    OMMFile() {
        metaData = new OMMMetaData(this);
    };

    /** Get the meta data.
     * @return meta data
     */
    @Override
    public OMMMetaData getMetaData() {
        return metaData;
    }

    /** Get the orbit mean motion.
     * @return the orbit mean motion
     */
    public double getMeanMotion() {
        return meanMotion;
    }

    /** Set the orbit mean motion.
     * @param motion the mean motion to be set
     */
    void setMeanMotion(final double motion) {
        this.meanMotion = motion;
    }

    /** Get the ephemeris type.
     * @return the ephemerisType
     */
    public int getEphemerisType() {
        return ephemerisType;
    }

    /** Set the ephemeris type.
     * @param ephemerisType the ephemeris type to be set
     */
    void setEphemerisType(final int ephemerisType) {
        this.ephemerisType = ephemerisType;
    }

    /** Get the classification type.
     * @return the classificationType
     */
    public char getClassificationType() {
        return classificationType;
    }

    /** Set the classification type.
     * @param classificationType the classification type to be set
     */
    void setClassificationType(final char classificationType) {
        this.classificationType = classificationType;
    }

    /** Get the NORAD Catalog Number ("Satellite Number").
     * @return the NORAD Catalog Number
     */
    public Integer getNoradID() {
        return noradID;
    }

    /** Set the NORAD Catalog Number ("Satellite Number").
     * @param noradID the element set number to be set
     */
    void setNoradID(final Integer noradID) {
        this.noradID = noradID;
    }

    /** Get the element set number for this satellite.
     * @return the element set number for this satellite
     */
    public String getElementSetNumber() {
        return elementSetNo;
    }

    /** Set the element set number for this satellite.
     * @param elementSetNo the element set number to be set
     */
    void setElementSetNo(final String elementSetNo) {
        this.elementSetNo = elementSetNo;
    }

    /** Get the revolution rumber.
     * @return the revolution rumber
     */
    public int getRevAtEpoch() {
        return revAtEpoch;
    }

    /** Set the revolution rumber.
     * @param revAtEpoch the Revolution Number to be set
     */
    void setRevAtEpoch(final int revAtEpoch) {
        this.revAtEpoch = revAtEpoch;
    }

    /** Get the SGP/SGP4 drag-like coefficient.
     * @return the SGP/SGP4 drag-like coefficient
     */
    public double getBStar() {
        return bStar;
    }

    /** Set the SGP/SGP4 drag-like coefficient.
     * @param bStar the SGP/SGP4 drag-like coefficient to be set
     */
    void setbStar(final double bStar) {
        this.bStar = bStar;
    }

    /** Get the first time derivative of the mean motion.
     * @return the first time derivative of the mean motion
     */
    public double getMeanMotionDot() {
        return meanMotionDot;
    }

    /** Set the first time derivative of the mean motion.
     * @param meanMotionDot the first time derivative of the mean motion to be set
     */
    void setMeanMotionDot(final double meanMotionDot) {
        this.meanMotionDot = meanMotionDot;
    }

    /** Get the second time derivative of the mean motion.
     * @return the second time derivative of the mean motion
     */
    public double getMeanMotionDotDot() {
        return meanMotionDotDot;
    }

    /** Set the second time derivative of the mean motion.
     * @param meanMotionDotDot the second time derivative of the mean motion to be set
     */
    void setMeanMotionDotDot(final double meanMotionDotDot) {
        this.meanMotionDotDot = meanMotionDotDot;
    }

    /** Get the comment for TLE related parameters.
     * @return comment for TLE related parameters
     */
    public List<String> getTLERelatedParametersComment() {
        return Collections.unmodifiableList(dataTleRelatedParametersComment);
    }

    /** Set the comment for TLE related parameters.
     * @param comment comment to set
     */
    void setTLERelatedParametersComment(final List<String> comment) {
        dataTleRelatedParametersComment = new ArrayList<String>(comment);
    }

    /** Generate a {@link KeplerianOrbit} based on the OMM mean keplerian elements.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the OMM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OMM
     */
    public KeplerianOrbit generateKeplerianOrbit() throws OrekitException {
        setMuUsed();
        final double a;
        if (Double.isNaN(getA())) {
            a = FastMath.cbrt(getMuUsed() / (meanMotion * meanMotion));
        } else {
            a = getA();
        }
        return new KeplerianOrbit(a, getE(), getI(), getPa(), getRaan(), getAnomaly(),
                                  PositionAngle.MEAN, metaData.getFrame(), getEpoch(), getMuUsed());
    }

    /** Generate a {@link CartesianOrbit} from the {@link KeplerianOrbit}.
     * @return the {@link CartesianOrbit} generated from the OPM information
     * @throws OrekitException if the Keplerian Orbit cannot be generated
     */
    public CartesianOrbit generateCartesianOrbit() throws OrekitException {
        return new CartesianOrbit(generateKeplerianOrbit());
    }

    /** Generate spacecraft state from the {@link KeplerianOrbit} generated by generateKeplerianOrbit.
     *  Raises an exception if OMM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OMM
     * @exception OrekitException if there is no spacecraft mass associated with the OMM
     */
    public SpacecraftState generateSpacecraftState()
        throws OrekitException {
        return new SpacecraftState(generateKeplerianOrbit(), getMass());
    }

    /** Generate TLE from OMM file. Launch Year, Launch Day and Launch Piece are not present in the
     * OMM file, they have to be set manually by the user with the AdditionalData static class.
     * @return the tle
     */
    public TLE generateTLE() {
        return new TLE(noradID, classificationType,
                       metaData.getLaunchYear(), metaData.getLaunchNumber(), metaData.getLaunchPiece(),
                       ephemerisType, Integer.parseInt(elementSetNo), getEpoch(),
                       meanMotion, meanMotionDot, meanMotionDotDot,
                       getE(), getI(), getPa(), getRaan(), getAnomaly(), revAtEpoch, bStar);
    }

    public static class OMMMetaData extends ODMMetaData {

        /** Description of the Mean Element Theory. Indicates the proper method to employ
         * to propagate the state. */
        private String meanElementTheory;

        /** Create a new meta-data.
         * @param ommFile OMM file to which these meta-data belongs
         */
        OMMMetaData(final OMMFile ommFile) {
            super(ommFile);
        }

        /** Get the description of the Mean Element Theory.
         * @return the mean element theory
         */
        public String getMeanElementTheory() {
            return meanElementTheory;
        }

        /** Set the description of the Mean Element Theory.
         * @param meanElementTheory the mean element theory to be set
         */
        void setMeanElementTheory(final String meanElementTheory) {
            this.meanElementTheory = meanElementTheory;
        }

    }

}
