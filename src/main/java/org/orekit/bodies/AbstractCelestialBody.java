/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.bodies;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
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
 */
public abstract class AbstractCelestialBody implements CelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -8225707171826328799L;

    /** J2000 frame, cached. */
    private static final Frame eme2000 = FramesFactory.getEME2000();

    /** Name of the body. */
    private final String name;

    /** Attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>). */
    private final double gm;

    /** IAU pole. */
    private final IAUPole iauPole;

    /** Inertially oriented, body-centered frame. */
    private final Frame inertialFrame;

    /** Body oriented, body-centered frame. */
    private final Frame bodyFrame;

    /** Build an instance and the underlying frame.
     * @param name name of the body
     * @param gm attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param iauPole IAU pole implementation
     * @param definingFrame frame in which celestial body coordinates are defined
     * @param inertialFrameName name to use for inertially oriented body centered frame
     * @param bodyFrameName name to use for body oriented body centered frame
     */
    protected AbstractCelestialBody(final String name, final double gm,
                                    final IAUPole iauPole, final Frame definingFrame,
                                    final String inertialFrameName, final String bodyFrameName) {
        this.name          = name;
        this.gm            = gm;
        this.iauPole       = iauPole;
        this.inertialFrame =
                new Frame(definingFrame, new InertiallyOriented(definingFrame), inertialFrameName, true);
        this.bodyFrame     =
                new Frame(inertialFrame, new BodyOriented(), bodyFrameName, false);
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
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


    /** Provider for inertially oriented body centered frame transform. */
    private class InertiallyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** Frame in which celestial body coordinates are defined. */
        private final Frame definingFrame;

        /** Simple constructor.
         * @param definingFrame frame in which celestial body coordinates are defined
         */
        public InertiallyOriented(final Frame definingFrame) {
            this.definingFrame = definingFrame;
        }

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) throws OrekitException {

            // compute translation from parent frame to self
            final PVCoordinates pv = getPVCoordinates(date, definingFrame);
            final Transform translation = new Transform(date, pv.negate());

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
            if (qNode.getNormSq() < Precision.SAFE_MIN) {
                qNode = Vector3D.PLUS_I;
            }
            final Rotation r2000 = new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I);

            // compute rotation from parent frame to self
            final Transform t  = definingFrame.getTransformTo(eme2000, date);
            final Transform rotation = new Transform(date, r2000.applyTo(t.getRotation()));

            // update transform from parent to self
            return new Transform(date, translation, rotation);

        }

    }

    /** Provider for body oriented body centered frame transform. */
    private class BodyOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -1859795611761959145L;

        /** {@inheritDoc} */
        public Transform getTransform(final AbsoluteDate date) throws OrekitException {
            final double dt = 10.0;
            final double w0 = iauPole.getPrimeMeridianAngle(date);
            final double w1 = iauPole.getPrimeMeridianAngle(date.shiftedBy(dt));
            return new Transform(date,
                                 new Rotation(Vector3D.PLUS_K, -w0),
                                 new Vector3D((w1 - w0) / dt, Vector3D.PLUS_K));
        }

    }

}
