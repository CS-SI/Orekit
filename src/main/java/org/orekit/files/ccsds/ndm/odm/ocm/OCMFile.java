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

import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.odm.ODMFile;
import org.orekit.files.ccsds.ndm.odm.ODMMetadata;
import org.orekit.files.ccsds.utils.CCSDSFrame;
import org.orekit.files.ccsds.utils.CCSDSUnit;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;

/** This class gathers the informations present in the Orbit Comprehensive Message (OCM), and contains
 * methods to generate {@link CartesianOrbit}, {@link KeplerianOrbit}, {@link SpacecraftState}
 * or a full {@link Propagator}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OCMFile extends ODMFile<NDMSegment<OCMMetadata, OCMData>> {

    /** Meta-data. */
    private final OCMMetaData metaData;

    /** Orbit state time history. */
    private final List<OrbitStateHistory> orbitStateHistories;

    /** Create a new OCM file object. */
    OCMFile() {
        metaData    = new OCMMetaData(this);
        orbitStateHistories = new ArrayList<>();
    }

    /** Get the meta data.
     * @return meta data
     */
    @Override
    public OCMMetaData getMetadata() {
        return metaData;
    }

    /** Get the comment for meta-data.
     * @return comment for meta-data
     */
    public List<String> getMetadataComment() {
        return metaData.getComments();
    }

    /** Get the orbit state time histories.
     * @return orbit state time histories
     */
    public List<OrbitStateHistory> getOrbitStateTimeHistories() {
        return orbitStateHistories;
    }

    /** Orbit state history. */
    public class OrbitStateHistory {

        /** Orbit state comments.
         * <p>
         * The list contains a string for each line of comment.
         * </p>
         */
        private List<String> comment;

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
        private String orbEpochT0;

        /** Time system for {@link #ORB_EPOCH_TZERO}. */
        private CcsdsTimeScale orbTimeSystem;

        /** Origin of reference frame. */
        private String centerName;

        /** Reference frame of the orbit. */
        private CCSDSFrame orbRefFrame;

        /** Epoch of the {@link #ORB_REF_FRAME orbit reference frame}. */
        private String orbFrameEpoch;

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
        OrbitStateHistory(final String defT0, final CcsdsTimeScale defTimeSystem) {
            this.comment = Collections.emptyList();
            this.states  = new ArrayList<>();
            setOrbEpochT0(defT0);
            setOrbTimeSystem(defTimeSystem);
            setOrbAveraging("OSCULATING");
            setCenterName("EARTH");
            setOrbRefFrame(CCSDSFrame.ITRF2000);
            setOrbType(ElementsType.CARTPV);
        }

        /** Get the meta-data comment.
         * @return meta-data comment
         */
        public List<String> getComment() {
            return Collections.unmodifiableList(comment);
        }

        /** Set the meta-data comment.
         * @param comment comment to set
         */
        void setComment(final List<String> comment) {
            this.comment = new ArrayList<String>(comment);
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
            return absoluteToEpoch(orbEpochT0);
        }

        /** Set reference epoch for all relative times in the orbit state block.
         * @param orbEpochT0 reference epoch for all relative times in the orbit state block
         */
        void setOrbEpochT0(final String orbEpochT0) {
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
            return absoluteToEpoch(orbFrameEpoch);
        }

        /** Set epoch of the {@link #getOrbRefFrame() orbit reference frame}.
         * @param orbFrameEpoch epoch of the {@link #getOrbRefFrame() orbit reference frame}
         */
        void setOrbFrameEpoch(final String orbFrameEpoch) {
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

        /** Add one orbit with relative date.
         * @param date orbit date
         * @param elements orbit elements
         * @return true if elements have been parsed properly
         */
        boolean addStateRelative(final String date, final String[] elements) {
            return addState(getOrbEpochT0().shiftedBy(Double.parseDouble(date)), elements);
        }

        /** Add one orbit with relative date.
         * @param date orbit date
         * @param elements orbit elements
         * @return true if elements have been parsed properly
         */
        boolean addStateAbsolute(final String date, final String[] elements) {
            return addState(absoluteToEpoch(date), elements);
        }

        /** Add one orbit with relative date.
         * @param date orbit date
         * @param elements orbit elements
         * @return true if elements have been parsed properly
         */
        private boolean addState(final AbsoluteDate date, final String[] elements) {
            if (elements.length != units.length) {
                // we don't have the expected number of elements
                return false;
            }
            states.add(new OrbitalState(date, elements, units));
            return true;
        }

        /** Convert a string to an epoch.
         * @param value string to convert
         * @return converted epoch
         */
        private AbsoluteDate absoluteToEpoch(final String value) {
            return orbTimeSystem.parseDate(value,
                                           getConventions(),
                                           getMissionReferenceDate());
        }

    }

    /** Orbital state entry. */
    public static class OrbitalState implements TimeStamped {

        /** Entry date. */
        private final AbsoluteDate date;

        /** Orbital elements. */
        private final double[] elements;

        /** Simple constructor.
         * @param date entry date
         * @param fields orbital elements
         * @param units units to use for parsing
         */
        OrbitalState(final AbsoluteDate date, final String[] fields, final CCSDSUnit[] units) {
            this.date     = date;
            this.elements = new double[units.length];
            for (int i = 0; i < elements.length; ++i) {
                elements[i] = units[i].toSI(Double.parseDouble(fields[i]));
            }
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get orbital elements.
         * @return orbital elements
         */
        public double[] getElements() {
            return elements.clone();
        }

    }

}
