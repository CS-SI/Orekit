/* Copyright 2002-2023 CS GROUP
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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;
import org.orekit.files.ccsds.ndm.odm.KeplerianElements;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class gathers the informations present in the Orbital Parameter Message (OPM).
 * @author sports
 * @since 6.1
 */
public class Opm extends NdmConstituent<OdmHeader, Segment<OdmCommonMetadata, OpmData>> implements TimeStamped {

    /** Root element for XML files. */
    public static final String ROOT = "opm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_OPM_VERS";

    /** Gravitational coefficient to use for building Cartesian/Keplerian orbits. */
    private final double mu;

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     * @param mu gravitational coefficient to use for building Cartesian/Keplerian orbits
     */
    public Opm(final OdmHeader header, final List<Segment<OdmCommonMetadata, OpmData>> segments,
               final IERSConventions conventions, final DataContext dataContext,
               final double mu) {
        super(header, segments, conventions, dataContext);
        this.mu = mu;
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public OdmCommonMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the file data.
     * @return file data
     */
    public OpmData getData() {
        return getSegments().get(0).getData();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return getData().getStateVectorBlock().getEpoch();
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
    public List<Maneuver> getManeuvers() {
        return getData().getManeuvers();
    }

    /** Get a maneuver.
     * @param index maneuver index, counting from 0
     * @return maneuver
     */
    public Maneuver getManeuver(final int index) {
        return getData().getManeuver(index);
    }

    /** check whether the OPM contains at least one maneuver.
     * @return true if OPM contains at least one maneuver false otherwise
     */
    public boolean hasManeuvers() {
        return getData().hasManeuvers();
    }

    /** Get the position/velocity coordinates contained in the OPM.
     * @return the position/velocity coordinates contained in the OPM
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return getData().getStateVectorBlock().toTimeStampedPVCoordinates();
    }

    /** Generate a Cartesian orbit.
     * @return generated orbit
     */
    public CartesianOrbit generateCartesianOrbit() {
        return new CartesianOrbit(getPVCoordinates(), getMetadata().getFrame(),
                                  getData().getStateVectorBlock().getEpoch(),
                                  mu);
    }

    /** Generate a keplerian orbit.
     * @return generated orbit
     */
    public KeplerianOrbit generateKeplerianOrbit() {
        final OdmCommonMetadata metadata = getMetadata();
        final OpmData        data     = getData();
        final KeplerianElements keplerianElements = data.getKeplerianElementsBlock();
        if (keplerianElements != null) {
            return keplerianElements.generateKeplerianOrbit(metadata.getFrame());
        } else {
            return new KeplerianOrbit(getPVCoordinates(), metadata.getFrame(),
                                      data.getStateVectorBlock().getEpoch(),
                                      mu);
        }
    }

    /** Generate spacecraft state from the {@link CartesianOrbit} generated by generateCartesianOrbit.
     * @return the spacecraft state of the OPM
     */
    public SpacecraftState generateSpacecraftState() {
        return new SpacecraftState(generateCartesianOrbit(), getData().getMass());
    }

}

