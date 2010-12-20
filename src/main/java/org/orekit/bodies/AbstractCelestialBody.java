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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Abstract implementation of the {@link CelestialBody} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared
 * by most implementations of the {@link CelestialBody} interface. It holds
 * the attraction coefficient and build the body-centered frames automatically
 * using the definitions of pole and prime meridianspecified by the IAU/IAG Working
 * Group on Cartographic Coordinates and Rotational Elements of the Planets and
 * Satellites (WGCCRE).
 * </p>
 * @see IAUPole
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public abstract class AbstractCelestialBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = 6769512376971866660L;

    /** Attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>). */
    private final double gm;

    /** IAU pole. */
    private final IAUPole iauPole;

    /** Frame in which celestial body coordinates are defined. */
    private final Frame definingFrame;

    /** Inertially oriented, body-centered frame. */
    private final Frame inertialFrame;

    /** Body oriented, body-centered frame. */
    private final Frame bodyFrame;

    /** Build an instance and the underlying frame.
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param iauPole IAU pole implementation
     * @param definingFrame frame in which celestial body coordinates are defined
     * @param inertialFrameName name to use for inertially oriented body centered frame
     * @param bodyFrameName name to use for body oriented body centered frame
     */
    protected AbstractCelestialBody(final double gm, final IAUPole iauPole,
                                    final Frame definingFrame,
                                    final String inertialFrameName, String bodyFrameName) {
        this.gm            = gm;
        this.iauPole       = iauPole;
        this.definingFrame = definingFrame;
        this.inertialFrame = new InertiallyOrientedFrame(inertialFrameName);
        this.bodyFrame     = new BodyOrientedFrame(bodyFrameName);
    }

    /** {@inheritDoc} */
    public double getGM() {
        return gm;
    }

    /** {@inheritDoc} */
    @Deprecated
    public Frame getFrame() {
        return inertialFrame;
    }

    /** {@inheritDoc} */
    public Frame getInertiallyOrientedFrame() {
        return inertialFrame;
    }

    /** {@inheritDoc} */
    public Frame getBodyOrientedFrame() {
        return bodyFrame;
    }

    /** {@inheritDoc} */
    public abstract PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame)
        throws OrekitException;


    /** Inertially oriented body centered frame. */
    private class InertiallyOrientedFrame extends Frame {

        /** Serializable UID. */
        private static final long serialVersionUID = 3361119898885468867L;

        /** Build a new instance.
         * @param name of the frame
         */
        public InertiallyOrientedFrame(final String name) {
            super(definingFrame, null, name, true);
        }

        /** {@inheritDoc} */
        protected void updateFrame(final AbsoluteDate date) throws OrekitException {

            // compute translation from parent frame to self
            final PVCoordinates pv = getPVCoordinates(date, getParent());
            final Transform translation =
                new Transform(pv.getPosition().negate(), pv.getVelocity().negate());

            // compute rotation from EME2000 frame to self,
            // as per the "Report of the IAU/IAG Working Group on Cartographic
            // Coordinates and Rotational Elements of the Planets and Satellites"
            // These definitions are common for all recent versions of this report
            // published every three years, the precise values of pole direction
            // and W angle coefficients may vary from publication year as models are
            // adjusted. These coefficients are not in this class, they are in the
            // specialized classes that do implement the getPole and getPrimeMeridianAngle
            // methods
            final Vector3D pole  = iauPole.getPole(date);
            Vector3D qNode = Vector3D.crossProduct(Vector3D.PLUS_K, pole);
            if (qNode.getNormSq() < MathUtils.SAFE_MIN) {
                qNode = Vector3D.PLUS_I;
            }
            final Rotation r2000 = new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I);

            // compute rotation from parent frame to self
            final Transform t  = definingFrame.getTransformTo(FramesFactory.getEME2000(), date);
            final Transform rotation = new Transform(r2000.applyTo(t.getRotation()));

            // update transform from parent to self
            setTransform(new Transform(translation, rotation));

        }

    }

    /** Body oriented body centered frame. */
    private class BodyOrientedFrame extends Frame {

        /** Serializable UID. */
        private static final long serialVersionUID = 4254134424697573764L;

        /** Build a new instance.
         * @param name of the frame
         */
        public BodyOrientedFrame(final String name) {
            super(inertialFrame, null, name, true);
        }

        /** {@inheritDoc} */
        protected void updateFrame(final AbsoluteDate date) throws OrekitException {
            final double dt = 10.0;
            final double w0 = iauPole.getPrimeMeridianAngle(date);
            final double w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -w0),
                                       new Vector3D((w1 - w0) / dt, Vector3D.PLUS_K)));
        }

    }

}
