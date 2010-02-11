/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/** Spacecraft frame.
 * <p>Frame associated to a satellite body, taking into account orbit and attitude.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class SpacecraftFrame extends Frame implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6012707827832395314L;

    /** Propagator to use. */
    private final Propagator propagator;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Simple constructor.
     * @param propagator orbit/attitude propagator computing spacecraft state evolution
     * @param name name of the frame
     */
    public SpacecraftFrame(final Propagator propagator, final String name) {
        super(propagator.getInitialState().getFrame(), null, name, false);
        this.propagator = propagator;
    }

    /** Get the underlying propagator.
     * @return underlying propagator
     */
    public Propagator getPropagator() {
        return propagator;
    }

    /** Get the {@link PVCoordinates} of the spacecraft frame origin in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position/velocity of the spacecraft frame origin (m and m/s)
     * @exception OrekitException if position cannot be computed in given frame
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagator.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {
        if ((cachedDate == null) || !cachedDate.equals(date)) {
            setTransform(propagator.propagate(date).toTransform());
            cachedDate = date;
        }
    }

}
