/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.maneuvers.SmallManeuverAnalyticalModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Orbit propagator that adapts an underlying propagator, adding {@link
 * SmallManeuverAnalyticalModel small maneuvers}.
 * <p>
 * This propagator is used when a reference propagator does not handle
 * some maneuvers that we need. A typical example would be an ephemeris
 * that was computed for a reference orbit, and we want to compute a
 * station-keeping maneuver on top of this ephemeris, changing its
 * final state. The principal is to add one or more {@link
 * SmallManeuverAnalyticalModel small maneuvers} to it and use it as a
 * new propagator, which takes the maneuvers into account.
 * </p>
 * <p>
 * From a space flight dynamics point of view, this is a differential
 * correction approach. From a computer science point of view, this is
 * a use of the decorator design pattern.
 * </p>
 * @see Propagator
 * @see SmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 */
public class ManeuverAdapterPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -3844765709317636783L;

    /** Underlying reference propagator. */
    private Propagator reference;

    /** Maneuvers to add. */
    private List<SmallManeuverAnalyticalModel> maneuvers;

    /** Build a propagator from an underlying reference propagator.
     * <p>The reference propagator can be almost anything, numerical,
     * anlytical, and even an ephemeris. It may already take some maneuvers
     * into account.</p>
     * @param reference reference propagator
     */
    public ManeuverAdapterPropagator(final Propagator reference) {
        super(reference.getAttitudeProvider());
        this.reference = reference;
        this.maneuvers = new ArrayList<SmallManeuverAnalyticalModel>();
    }

    /** Add a maneuver defined in spacecraft frame.
     * @param date date at which the maneuver is performed
     * @param dV velocity increment in spacecraft frame
     * @param isp engine specific impulse (s)
     * @exception OrekitException if spacecraft reference state cannot
     * be determined at maneuver date
     */
    
    public void addManeuver(final AbsoluteDate date, final Vector3D dV, final double isp)
        throws OrekitException {
        maneuvers.add(new SmallManeuverAnalyticalModel(reference.propagate(date),
                                                       dV, isp));
    }

    /** Add a maneuver defined in user-specified frame.
     * @param frame frame in which velocity increment is defined
     * @param date date at which the maneuver is performed
     * @param dV velocity increment in specified frame
     * @param isp engine specific impulse (s)
     * @exception OrekitException if spacecraft reference state cannot
     * be determined at maneuver date
     */
    
    public void addManeuver(final Frame frame, final AbsoluteDate date,
                            final Vector3D dV, final double isp)
        throws OrekitException {
        maneuvers.add(new SmallManeuverAnalyticalModel(reference.propagate(date),
                                                       frame, dV, isp));
    }

    /** Get the maneuvers models.
     * @return maneuvers models, as an unmodifiable list
     */
    public List<SmallManeuverAnalyticalModel> getManeuvers() {
        return Collections.unmodifiableList(maneuvers);
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() throws OrekitException {
        return reference.getInitialState();
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        reference.resetInitialState(state);
    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {

        // compute reference state
        SpacecraftState state = reference.propagate(date);

        // add the effect of all maneuvers
        for (final SmallManeuverAnalyticalModel maneuver : maneuvers) {
            state = maneuver.applyManeuver(state);
        }

        return state;

    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {
        return basicPropagate(date).getOrbit();
    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date)
        throws PropagationException {
        return basicPropagate(date).getMass();
    }

}
