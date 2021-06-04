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

package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.files.ccsds.section.CommentsContainer;

/** Container for TLE data.
 * @author sports
 * @since 6.1
 */
public class OmmTle extends CommentsContainer {

    /** Ephemeris Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the coding for
     * the EPHEMERIS_TYPE keyword: 1 = SGP, 2 = SGP4, 3 = SDP4, 4 = SGP8, 5 = SDP8. Default value = 0.
     */
    private int ephemerisType;

    /** Classification Type, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. Some sources suggest the
     *  following coding for the CLASSIFICATION_TYPE keyword: U = unclassified, S = secret. Default value = U.
     */
    private char classificationType;

    /** NORAD Catalog Number ("Satellite Number"), an integer of up to nine digits. */
    private int noradID;

    /** Element set number for this satellite, only required if MEAN_ELEMENT_THEORY = SGP/SGP4.
     * Normally incremented sequentially, but may be out of sync if it is generated from a backup source.
     * Used to distinguish different TLEs, and therefore only meaningful if TLE based data is being exchanged. */
    private int elementSetNo;

    /** Revolution Number, only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private int revAtEpoch;

    /** SGP/SGP4 drag-like coefficient (in units 1/[Earth radii]), only required if MEAN_ELEMENT_THEORY = SGP/SGP4. */
    private double bStar;

    /** First Time Derivative of the Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private double meanMotionDot;

    /** Second Time Derivative of Mean Motion, only required if MEAN_ELEMENT_THEORY = SGP. */
    private double meanMotionDotDot;

    /** Create an empty data set.
     */
    public OmmTle() {
        ephemerisType      = 0;
        classificationType = 'U';
        noradID            = -1;
        elementSetNo       = -1;
        revAtEpoch         = -1;
        bStar              =  Double.NaN;
        meanMotionDot      =  Double.NaN;
        meanMotionDotDot   =  Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNaN(meanMotionDot,     OmmTleKey.MEAN_MOTION_DOT);
        checkNotNaN(meanMotionDotDot,  OmmTleKey.MEAN_MOTION_DDOT);
        checkNotNegative(noradID,      OmmTleKey.NORAD_CAT_ID);
        checkNotNegative(elementSetNo, OmmTleKey.ELEMENT_SET_NO);
        checkNotNegative(revAtEpoch,   OmmTleKey.REV_AT_EPOCH);
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
    public void setEphemerisType(final int ephemerisType) {
        refuseFurtherComments();
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
    public void setClassificationType(final char classificationType) {
        refuseFurtherComments();
        this.classificationType = classificationType;
    }

    /** Get the NORAD Catalog Number ("Satellite Number").
     * @return the NORAD Catalog Number
     */
    public int getNoradID() {
        return noradID;
    }

    /** Set the NORAD Catalog Number ("Satellite Number").
     * @param noradID the element set number to be set
     */
    public void setNoradID(final int noradID) {
        refuseFurtherComments();
        this.noradID = noradID;
    }

    /** Get the element set number for this satellite.
     * @return the element set number for this satellite
     */
    public int getElementSetNumber() {
        return elementSetNo;
    }

    /** Set the element set number for this satellite.
     * @param elementSetNo the element set number to be set
     */
    public void setElementSetNo(final int elementSetNo) {
        refuseFurtherComments();
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
    public void setRevAtEpoch(final int revAtEpoch) {
        refuseFurtherComments();
        this.revAtEpoch = revAtEpoch;
    }

    /** Get the SGP/SGP4 drag-like coefficient.
     * @return the SGP/SGP4 drag-like coefficient
     */
    public double getBStar() {
        return bStar;
    }

    /** Set the SGP/SGP4 drag-like coefficient.
     * @param bstar the SGP/SGP4 drag-like coefficient to be set
     */
    public void setBStar(final double bstar) {
        refuseFurtherComments();
        this.bStar = bstar;
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
    public void setMeanMotionDot(final double meanMotionDot) {
        refuseFurtherComments();
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
    public void setMeanMotionDotDot(final double meanMotionDotDot) {
        refuseFurtherComments();
        this.meanMotionDotDot = meanMotionDotDot;
    }

}
