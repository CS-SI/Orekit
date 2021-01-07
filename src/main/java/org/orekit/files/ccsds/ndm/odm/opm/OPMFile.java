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

package org.orekit.files.ccsds.ndm.odm.opm;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NDMSegment;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.ODMFile;
import org.orekit.files.ccsds.ndm.odm.ODMHeader;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** This class gathers the informations present in the Orbital Parameter Message (OPM), and contains
 * methods to generate {@link CartesianOrbit}, {@link KeplerianOrbit} or {@link SpacecraftState}.
 * @author sports
 * @since 6.1
 */
public class OPMFile extends ODMFile<NDMSegment<OCommonMetadata, OPMData>> {

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private double mu;

    /** Create a new OPM file object.
     */
    OPMFile() {
        super(new ODMHeader());
        mu = Double.NaN;
    }

    /** Set the gravitational coefficient to use for building Cartesian/Keplerian orbits.
     * @param mu gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    void setMu(final double mu) {
        this.mu = mu;
    }

    /** Get the gravitational coefficient to use for building Cartesian/Keplerian orbits.
     * <p>
     * This methiod throws an exception if the gravitational coefficient has not been set properly
     * </p>
     * @return gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    public double getMu() {
        if (Double.isNaN(mu)) {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
        return mu;
    }

    /** Get position vector.
     * @return the position vector
     */
    public Vector3D getPosition() {
        return getSegments().get(0).getData().getPosition();
    }

    /** Get velocity vector.
     * @return the velocity vector
     */
    public Vector3D getVelocity() {
        return getSegments().get(0).getData().getVelocity();
    }

    /** Get the number of maneuvers present in the OPM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return getSegments().get(0).getData().getNbManeuvers();
    }

    /** Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<OPMManeuver> getManeuvers() {
        return getSegments().get(0).getData().getManeuvers();
    }

    /** Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public OPMManeuver getManeuver(final int index) {
        return getSegments().get(0).getData().getManeuver(index);
    }

    /** Get boolean testing whether the OPM contains at least one maneuver.
     * @return true if OPM contains at least one maneuver
     *         false otherwise */
    public boolean hasManeuver() {
        return getSegments().get(0).getData().hasManeuver();
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
     */
    public CartesianOrbit generateCartesianOrbit() {
        return new CartesianOrbit(getPVCoordinates(), getSegments().get(0).getMetadata().getFrame(),
                                  getSegments().get(0).getData().getEpoch(), getMu());
    }

    /** Generate a {@link KeplerianOrbit} from the OPM Keplerian elements if hasKeplerianElements is true,
     * or from the state vector data otherwise.
     * If the reference frame is not pseudo-inertial, an exception is raised.
     * @return the {@link KeplerianOrbit} generated from the OPM information
     */
    public KeplerianOrbit generateKeplerianOrbit() {
        final OPMData data = getSegments().get(0).getData();
        if (data.hasKeplerianElements()) {
            return new KeplerianOrbit(data.getA(), data.getE(), data.getI(),
                                      data.getPa(), data.getRaan(),
                                      data.getAnomaly(), data.getAnomalyType(),
                                      getSegments().get(0).getMetadata().getFrame(),
                                      data.getEpoch(), getMu());
        } else {
            return new KeplerianOrbit(getPVCoordinates(), getSegments().get(0).getMetadata().getFrame(),
                                      getSegments().get(0).getData().getEpoch(), getMu());
        }
    }

    /** Generate spacecraft state from the {@link CartesianOrbit} generated by generateCartesianOrbit.
     *  Raises an exception if OPM doesn't contain spacecraft mass information.
     * @return the spacecraft state of the OPM
     */
    public SpacecraftState generateSpacecraftState() {
        return new SpacecraftState(generateCartesianOrbit(), getSegments().get(0).getData().getMass());
    }

}

