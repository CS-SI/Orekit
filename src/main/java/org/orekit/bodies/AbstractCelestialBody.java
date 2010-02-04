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
package org.orekit.bodies;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Abstract implementation of the {@link CelestialBody} interface.
 * <p>This abstract implementation provides basic services that can be shared
 * by most implementations of the {@link CelestialBody} interface. It holds
 * the attraction coefficient and the body-centered frame, and also provides a
 * way to build this frame automatically if not already provided by sub-classes.
 * The only method that still needs to be implemented by sub-classes is the
 * {@link #getPVCoordinates(AbsoluteDate, Frame) getPVCoordinates} method.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public abstract class AbstractCelestialBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -8952752516350848089L;

    /** Attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>). */
    private final double gm;

    /** Inertial body-centered frame. */
    private final Frame bodyCenteredFrame;

    /** Build an instance using an existing body-centered frame.
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param frame existing body-centered frame to use
     */
    protected AbstractCelestialBody(final double gm, final Frame frame) {
        this.gm = gm;
        this.bodyCenteredFrame = frame;
    }

    /** Build an instance and the underlying frame.
     * <p>The underlying body-centered frame built is a direct child of {@link
     * org.orekit.frames.FramesFactory#getEME2000() EME2000}. Its defining transform is a simple linear
     * {@link Transform#Transform(org.apache.commons.math.geometry.Vector3D,
     * org.apache.commons.math.geometry.Vector3D) translation/velocity} transform
     * without any rotation. The frame axes are therefore always parallel to
     * {@link org.orekit.frames.FramesFactory#getEME2000() EME2000} frame axes.</p>
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param frameName frame name to use
     * @param definingFrame frame in which celestial body coordinates are defined
     */
    protected AbstractCelestialBody(final double gm,
                                    final String frameName, final Frame definingFrame) {
        this.gm    = gm;
        this.bodyCenteredFrame = new BodyCenteredFrame(frameName, definingFrame);
    }

    /** {@inheritDoc} */
    public double getGM() {
        return gm;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return bodyCenteredFrame;
    }

    /** {@inheritDoc} */
    public abstract PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** Inertially oriented body-centered frame aligned with EME2000. */
    private class BodyCenteredFrame extends Frame {

        /** Serializable UID. */
        private static final long serialVersionUID = -5218165259985961031L;

        /** Build a new instance.
         * @param name of the frame
         * @param definingFrame frame in which celestial body coordinates are defined
         */
        public BodyCenteredFrame(final String name, final Frame definingFrame) {
            super(definingFrame, null, name, true);
        }

        /** {@inheritDoc} */
        protected void updateFrame(final AbsoluteDate date) throws OrekitException {

            // compute position velocity with respect to parent frame
            final PVCoordinates pv = getPVCoordinates(date, getParent());

            // update transform from parent to self
            setTransform(new Transform(pv.getPosition().negate(),
                                       pv.getVelocity().negate()));

        }

    }

}
