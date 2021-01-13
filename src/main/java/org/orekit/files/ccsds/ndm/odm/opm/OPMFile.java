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

package org.orekit.files.ccsds.ndm.odm.opm;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.OStateFile;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** This class gathers the informations present in the Orbital Parameter Message (OPM), and contains
 * methods to generate {@link CartesianOrbit}, {@link KeplerianOrbit} or {@link SpacecraftState}.
 * @author sports
 * @since 6.1
 */
public class OPMFile extends OStateFile<OCommonMetadata, OPMData> {

    /** Get position vector.
     * @return the position vector
     */
    public Vector3D getPosition() {
        return getData().getPosition();
    }

    /** Get velocity vector.
     * @return the velocity vector
     */
    public Vector3D getVelocity() {
        return getData().getVelocity();
    }

    /** Get the number of maneuvers present in the OPM.
     * @return the number of maneuvers
     */
    public int getNbManeuvers() {
        return getData().getNbManeuvers();
    }

    /** Get a list of all maneuvers.
     * @return unmodifiable list of all maneuvers.
     */
    public List<OPMManeuver> getManeuvers() {
        return getData().getManeuvers();
    }

    /** Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public OPMManeuver getManeuver(final int index) {
        return getData().getManeuver(index);
    }

    /** Get boolean testing whether the OPM contains at least one maneuver.
     * @return true if OPM contains at least one maneuver
     *         false otherwise */
    public boolean hasManeuver() {
        return getData().hasManeuver();
    }

    /** Get the position/velocity coordinates contained in the OPM.
     * @return the position/velocity coordinates contained in the OPM
     */
    public PVCoordinates getPVCoordinates() {
        return new PVCoordinates(getPosition(), getVelocity());
    }

    /** {@inheritDoc} */
    @Override
    public CartesianOrbit generateCartesianOrbit() {
        return new CartesianOrbit(getPVCoordinates(), getMetadata().getFrame(),
                                  getData().getEpoch(), getMu());
    }

    /** {@inheritDoc} */
    @Override
    public KeplerianOrbit generateKeplerianOrbit() {
        final OPMData data = getData();
        if (data.hasKeplerianElements()) {
            return new KeplerianOrbit(data.getA(), data.getE(), data.getI(),
                                      data.getPa(), data.getRaan(),
                                      data.getAnomaly(), data.getAnomalyType(),
                                      getMetadata().getFrame(),
                                      data.getEpoch(), getMu());
        } else {
            return new KeplerianOrbit(getPVCoordinates(), getMetadata().getFrame(),
                                      getData().getEpoch(), getMu());
        }
    }

}

