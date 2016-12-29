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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** This class gathers the informations present in the Orbital Parameter Message (OPM), and contains
 * methods to generate {@link CartesianOrbit}, {@link KeplerianOrbit} or {@link SpacecraftState}.
 * @author sports
 * @since 6.1
 */
public class OPMFile extends OGMFile {

    /** Meta-data. */
    private final ODMMetaData metaData;

    /** Position vector (m). */
    private Vector3D position;

    /** Velocity vector (m/s. */
    private Vector3D velocity;

    /** Maneuvers. */
    private List<Maneuver> maneuvers;

    /** Create a new OPM file object. */
    OPMFile() {
        metaData  = new ODMMetaData(this);
        maneuvers = new ArrayList<Maneuver>();
    };

    /** Get the meta data.
     * @return meta data
     */
    @Override
    public ODMMetaData getMetaData() {
        return metaData;
    }

    /** Get position vector.
     * @return the position vector
     */
    public Vector3D getPosition() {
        return position;
    }

    /** Set position vector.
     * @param position the position vector to be set
     */
    void setPosition(final Vector3D position) {
        this.position = position;
    }

    /** Get velocity vector.
     * @return the velocity vector
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /** Set velocity vector.
     * @param velocity the velocity vector to be set
     */
    void setVelocity(final Vector3D velocity) {
        this.velocity = velocity;
    }

    /** Get the number of maneuvers present in the OPM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return maneuvers.size();
    }

    /** Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<Maneuver> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /** Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return maneuvers.get(index);
    }

    /** Add a maneuver.
     * @param maneuver maneuver to be set
     */
    void addManeuver(final Maneuver maneuver) {
        maneuvers.add(maneuver);
    }

    /** Get boolean testing whether the OPM contains at least one maneuver.
     * @return true if OPM contains at least one maneuver
     *         false otherwise */
    public boolean getHasManeuver() {
        return !maneuvers.isEmpty();
    }

    /** Get the comment for meta-data.
     * @return comment for meta-data
     */
    public List<String> getMetaDataComment() {
        return metaData.getComment();
    }

    /** Get the position/velocity coordinates contained in the OPM.
     * @return the position/velocity coordinates contained in the OPM
     */
    public PVCoordinates getPVCoordinates() {
        return new PVCoordinates(getPosition(), getVelocity());
    }

    /**
     * Generate a {@link CartesianOrbit} from the OPM state vector data. If the reference frame is not
     * pseudo-inertial, an exception is raised.
     * @return the {@link CartesianOrbit} generated from the OPM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OPM
     */
    public CartesianOrbit generateCartesianOrbit()
        throws OrekitException {
        setMuUsed();
        return new CartesianOrbit(getPVCoordinates(), metaData.getFrame(), getEpoch(), getMuUsed());
    }

    /** Generate a {@link KeplerianOrbit} from the OPM keplerian elements if hasKeplerianElements is true,
     * or from the state vector data otherwise.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the OPM information
     * @exception OrekitException if the reference frame is not pseudo-inertial or if the central body
     * gravitational coefficient cannot be retrieved from the OPM
     */
    public KeplerianOrbit generateKeplerianOrbit() throws OrekitException {
        setMuUsed();
        if (hasKeplerianElements()) {
            return new KeplerianOrbit(getA(), getE(), getI(), getPa(), getRaan(), getAnomaly(),
                                      getAnomalyType(), metaData.getFrame(), getEpoch(), getMuUsed());
        } else {
            return new KeplerianOrbit(getPVCoordinates(), metaData.getFrame(), getEpoch(), getMuUsed());
        }
    }

    /** Generate spacecraft state from the {@link CartesianOrbit} generated by generateCartesianOrbit.
     *  Raises an exception if OPM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OPM
     * @exception OrekitException if there is no spacecraft mass associated with the OPM
     */
    public SpacecraftState generateSpacecraftState()
        throws OrekitException {
        return new SpacecraftState(generateCartesianOrbit(), getMass());
    }

    /** Maneuver in an OPM file.
     */
    public static class Maneuver {

        /** Epoch ignition. */
        private AbsoluteDate epochIgnition;

        /** Coordinate system for velocity increment vector, for Local Orbital Frames. */
        private LOFType refLofType;

        /** Coordinate system for velocity increment vector, for absolute frames. */
        private Frame refFrame;

        /** Duration (value is 0 for impulsive maneuver). */
        private double duration;

        /** Mass change during maneuver (value is < 0). */
        private double deltaMass;

        /** Velocity increment. */
        private Vector3D dV;

        /** Maneuvers data comment, each string in the list corresponds to one line of comment. */
        private List<String> comment;

        /** Simple constructor.
         */
        public Maneuver() {
            this.dV      = Vector3D.ZERO;
            this.comment = Collections.emptyList();
        }

        /** Get epoch ignition.
         * @return epoch ignition
         */
        public AbsoluteDate getEpochIgnition() {
            return epochIgnition;
        }

        /** Set epoch ignition.
         * @param epochIgnition epoch ignition
         */
        void setEpochIgnition(final AbsoluteDate epochIgnition) {
            this.epochIgnition = epochIgnition;
        }

        /** Get coordinate system for velocity increment vector, for Local Orbital Frames.
         * @return coordinate system for velocity increment vector, for Local Orbital Frames
         */
        public LOFType getRefLofType() {
            return refLofType;
        }

        /** Set coordinate system for velocity increment vector, for Local Orbital Frames.
         * @param refLofType coordinate system for velocity increment vector, for Local Orbital Frames
         */
        public void setRefLofType(final LOFType refLofType) {
            this.refLofType = refLofType;
            this.refFrame   = null;
        }

        /** Get Coordinate system for velocity increment vector, for absolute frames.
         * @return coordinate system for velocity increment vector, for absolute frames
         */
        public Frame getRefFrame() {
            return refFrame;
        }

        /** Set Coordinate system for velocity increment vector, for absolute frames.
         * @param refFrame coordinate system for velocity increment vector, for absolute frames
         */
        public void setRefFrame(final Frame refFrame) {
            this.refLofType = null;
            this.refFrame   = refFrame;
        }

        /** Get duration (value is 0 for impulsive maneuver).
         * @return duration (value is 0 for impulsive maneuver)
         */
        public double getDuration() {
            return duration;
        }

        /** Set duration (value is 0 for impulsive maneuver).
         * @param duration duration (value is 0 for impulsive maneuver)
         */
        public void setDuration(final double duration) {
            this.duration = duration;
        }

        /** Get mass change during maneuver (value is &lt; 0).
         * @return mass change during maneuver (value is &lt; 0)
         */
        public double getDeltaMass() {
            return deltaMass;
        }

        /** Set mass change during maneuver (value is &lt; 0).
         * @param deltaMass mass change during maneuver (value is &lt; 0)
         */
        public void setDeltaMass(final double deltaMass) {
            this.deltaMass = deltaMass;
        }

        /** Get velocity increment.
         * @return velocity increment
         */
        public Vector3D getDV() {
            return dV;
        }

        /** Set velocity increment.
         * @param dV velocity increment
         */
        public void setdV(final Vector3D dV) {
            this.dV = dV;
        }

        /** Get the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @return maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public List<String> getComment() {
            return Collections.unmodifiableList(comment);
        }

        /** Set the maneuvers data comment, each string in the list corresponds to one line of comment.
         * @param comment maneuvers data comment, each string in the list corresponds to one line of comment
         */
        public void setComment(final List<String> comment) {
            this.comment = new ArrayList<String>(comment);
        }

    }

}

