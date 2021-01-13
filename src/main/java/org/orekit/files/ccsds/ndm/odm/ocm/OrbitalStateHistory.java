/* Copyright 2002-2019 CS Systèmes d'Information
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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CCSDSUnit;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.time.AbsoluteDate;

/** Orbit state history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitalStateHistory {

    /** Orbital state comments.
     * <p>
     * The list contains a string for each line of comment.
     * </p>
     */
    private final List<String> comments;

    /** Orbit identification number. */
    private String orbID;

    /** Identification number of previous orbit. */
    private String orbPrevID;

    /** Identification number of next orbit. */
    private String orbNextID;

    /** Basis of this orbit state time history data. */
    private OrbitBasis orbBasis;

    /** Identification number of the orbit determination or simulation upon which this orbit is based. */
    private String orbBasisID;

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    private String orbAveraging;

    /** Reference epoch for all relative times in the orbit state block. */
    private AbsoluteDate orbEpochT0;

    /** Time system for {@link #ORB_EPOCH_TZERO}. */
    private CcsdsTimeScale orbTimeSystem;

    /** Origin of reference frame. */
    private String centerName;

    /** Reference frame of the orbit. */
    private CCSDSFrame orbRefFrame;

    /** Epoch of the {@link #ORB_REF_FRAME orbit reference frame}. */
    private AbsoluteDate orbFrameEpoch;

    /** Orbit element set type. */
    private ElementsType orbType;

    /** Units of orbit element set. */
    private CCSDSUnit[] units;

    /** Number of elements (excluding time) contain in the element set. */
    private int orbN;

    /** Definition of orbit elements. */
    private String orbElements;

    /** Orbital states. */
    private final List<OrbitalState> states;

    /** Simple constructor.
     * @param defT0 default epoch
     * @param defTimeSystem default time system
     */
    OrbitalStateHistory(final AbsoluteDate defT0, final CcsdsTimeScale defTimeSystem) {
        this.comments = new ArrayList<>();
        this.states   = new ArrayList<>();
        setOrbEpochT0(defT0);
        setOrbTimeSystem(defTimeSystem);
        setOrbAveraging("OSCULATING");
        setCenterName("EARTH");
        setOrbRefFrame(CCSDSFrame.ITRF2000);
        setOrbType(ElementsType.CARTPV);
    }

    /** Get the comments.
     * @return unmodifiable view of the comments
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /** Add a comment.
     * @param comment comment to add
     */
    void addComment(final String comment) {
        comments.add(comment);
    }

    /** Get orbit identification number.
     * @return orbit identification number
     */
    public String getOrbID() {
        return orbID;
    }

    /** Set orbit identification number.
     * @param orbID orbit identification number
     */
    void setOrbID(final String orbID) {
        this.orbID = orbID;
    }

    /** Get identification number of previous orbit.
     * @return identification number of previous orbit
     */
    public String getOrbPrevID() {
        return orbPrevID;
    }

    /** Set identification number of previous orbit.
     * @param orbPrevID identification number of previous orbit
     */
    void setOrbPrevID(final String orbPrevID) {
        this.orbPrevID = orbPrevID;
    }

    /** Get identification number of next orbit.
     * @return identification number of next orbit
     */
    public String getOrbNextID() {
        return orbNextID;
    }

    /** Set identification number of next orbit.
     * @param orbNextID identification number of next orbit
     */
    void setOrbNextID(final String orbNextID) {
        this.orbNextID = orbNextID;
    }

    /** Get basis of this orbit state time history data.
     * @return basis of this orbit state time history data
     */
    public OrbitBasis getOrbBasis() {
        return orbBasis;
    }

    /** Set basis of this orbit state time history data.
     * @param orbBasis basis of this orbit state time history data
     */
    void setOrbBasis(final OrbitBasis orbBasis) {
        this.orbBasis = orbBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this orbit is based.
     * @return identification number of the orbit determination or simulation upon which this orbit is based
     */
    public String getOrbBasisID() {
        return orbBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this orbit is based.
     * @param orbBasisID identification number of the orbit determination or simulation upon which this orbit is based
     */
    void setOrbBasisID(final String orbBasisID) {
        this.orbBasisID = orbBasisID;
    }

    /** Get type of averaging (Osculating, mean Brouwer, other.
     * @return type of averaging (Osculating, mean Brouwer, other
     .). */
    public String getOrbAveraging() {
        return orbAveraging;
    }

    /** Set type of averaging (Osculating, mean Brouwer, other.
     * @param orbAveraging type of averaging (Osculating, mean Brouwer, other
     .). */
    void setOrbAveraging(final String orbAveraging) {
        this.orbAveraging = orbAveraging;
    }

    /** Get reference epoch for all relative times in the orbit state block.
     * @return reference epoch for all relative times in the orbit state block
     */
    public AbsoluteDate getOrbEpochT0() {
        return orbEpochT0;
    }

    /** Set reference epoch for all relative times in the orbit state block.
     * @param orbEpochT0 reference epoch for all relative times in the orbit state block
     */
    void setOrbEpochT0(final AbsoluteDate orbEpochT0) {
        this.orbEpochT0 = orbEpochT0;
    }

    /** Get time system for {@link #getOrbEpochT0()}.
     * @return time system for {@link #getOrbEpochT0()}
     */
    public CcsdsTimeScale getOrbTimeSystem() {
        return orbTimeSystem;
    }

    /** Set time system for {@link #getOrbEpochT0()}.
     * @param orbTimeSystem time system for {@link #getOrbEpochT0()}
     */
    void setOrbTimeSystem(final CcsdsTimeScale orbTimeSystem) {
        this.orbTimeSystem = orbTimeSystem;
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Set the origin of reference frame.
     * @param centerName the origin of reference frame to be set
     */
    void setCenterName(final String centerName) {
        this.centerName = centerName;
    }

    /** Get reference frame of the orbit.
     * @return reference frame of the orbit
     */
    public CCSDSFrame getOrbRefFrame() {
        return orbRefFrame;
    }

    /** Set reference frame of the orbit.
     * @param orbRefFrame reference frame of the orbit
     */
    void setOrbRefFrame(final CCSDSFrame orbRefFrame) {
        this.orbRefFrame = orbRefFrame;
    }

    /** Get epoch of the {@link #getOrbRefFrame() orbit reference frame}.
     * @return epoch of the {@link #getOrbRefFrame() orbit reference frame}
     */
    public AbsoluteDate getOrbFrameEpoch() {
        return orbFrameEpoch;
    }

    /** Set epoch of the {@link #getOrbRefFrame() orbit reference frame}.
     * @param orbFrameEpoch epoch of the {@link #getOrbRefFrame() orbit reference frame}
     */
    void setOrbFrameEpoch(final AbsoluteDate orbFrameEpoch) {
        this.orbFrameEpoch = orbFrameEpoch;
    }

    /** Get orbit element set type.
     * @return orbit element set type
     */
    public ElementsType getOrbType() {
        return orbType;
    }

    /** Set orbit element set type.
     * @param orbType orbit element set type
     */
    void setOrbType(final ElementsType orbType) {
        this.orbType = orbType;
        this.units   = orbType.getUnits();
    }

    /** Get number of elements (excluding time) contain in the element set.
     * @return number of elements (excluding time) contain in the element set
     */
    public int getOrbN() {
        return orbN;
    }

    /** Set number of elements (excluding time) contain in the element set.
     * @param orbN number of elements (excluding time) contain in the element set
     */
    void setOrbN(final int orbN) {
        this.orbN = orbN;
    }

    /** Get definition of orbit elements.
     * @return definition of orbit elements
     */
    public String getOrbElements() {
        return orbElements;
    }

    /** Set definition of orbit elements.
     * @param orbElements definition of orbit elements
     */
    void setOrbElements(final String orbElements) {
        this.orbElements = orbElements;
    }

    /** Get the orbital states.
     * @return orbital states
     */
    public List<OrbitalState> getOrbitalStates() {
        return Collections.unmodifiableList(states);
    }

    /** Add one orbit.
     * @param date orbit date
     * @param elements orbit elements
     * @param first index of first field to consider
     * @return true if elements have been parsed properly
     */
    public boolean addState(final AbsoluteDate date, final String[] elements, final int first) {
        if ((elements.length - first) != units.length) {
            // we don't have the expected number of elements
            return false;
        }
        states.add(new OrbitalState(date, elements, first, units));
        return true;
    }

}
