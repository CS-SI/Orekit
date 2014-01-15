/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/** Spacecraft frame.
 * <p>Frame associated to a satellite body, taking into account orbit and attitude.</p>
 * <p>
 * Use of this frame is not recommended, and it will probably be withdrawn in a future version.
 * In many cases, rather than using this frame and its {@link #getTransformTo(Frame, AbsoluteDate)
 * getTransformTo} method, users should directly get a {@link Transform} using
 * {@link org.orekit.propagation.SpacecraftState#toTransform()}.
 * </p>
 * <p>
 * Note that despite it extends {@link Frame}, this frame is <em>NOT</em> serializable,
 * as it relies on {@link Propagator}.
 * </p>
 * @deprecated as of 6.0 replaced by {@link org.orekit.propagation.SpacecraftState#toTransform()}
 * @author Luc Maisonobe
 */
@Deprecated
public class SpacecraftFrame extends Frame implements PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6012707827832395314L;

    /** Simple constructor.
     * @param propagator orbit/attitude propagator computing spacecraft state evolution
     * @param name name of the frame
     */
    public SpacecraftFrame(final Propagator propagator, final String name) {
        super(propagator.getFrame(), new LocalProvider(propagator), name, false);
    }

    /** Get the underlying propagator.
     * @return underlying propagator
     */
    public Propagator getPropagator() {
        return ((LocalProvider) getTransformProvider()).getPropagator();
    }

    /** Get the {@link PVCoordinates} of the spacecraft frame origin in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position/velocity of the spacecraft frame origin (m and m/s)
     * @exception OrekitException if position cannot be computed in given frame
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return getPropagator().getPVCoordinates(date, frame);
    }

    /** Local provider for transforms. */
    private static class LocalProvider implements TransformProvider, Externalizable {

        /** Serializable UID. */
        private static final long serialVersionUID = 386815086579675823L;

        /** Propagator to use. */
        private final transient Propagator propagator;

        /** Simple constructor.
         * @param propagator orbit/attitude propagator computing spacecraft state evolution
         */
        public LocalProvider(final Propagator propagator) {
            this.propagator = propagator;
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) throws OrekitException {
            return propagator.propagate(date).toTransform();
        }

        /** Get the underlying propagator.
         * @return underlying propagator
         */
        public Propagator getPropagator() {
            return propagator;
        }

        /** {@inheritDoc} */
        public void readExternal(final ObjectInput input) throws IOException {
            throw new NotSerializableException();
        }

        /** {@inheritDoc} */
        public void writeExternal(final ObjectOutput output) throws IOException {
            throw new NotSerializableException();
        }

    }

}
