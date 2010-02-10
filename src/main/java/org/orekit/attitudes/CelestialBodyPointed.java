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
package org.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles a celestial body pointed attitude law.
 * <p>The celestial body pointed law is defined by two main elements:
 * <ul>
 *   <li>a celestial body towards which some satellite axis is exactly aimed</li>
 *   <li>a phasing reference defining the rotation around the pointing axis</li>
 * </ul>
 * </p>
 * <p>
 * The celestial body implicitly defines two of the three degrees of freedom
 * and the phasing reference defines the last degree of freedom. This definition
 * can be represented as first aligning exactly the satellite pointing axis to
 * the current direction of the celestial body, and then to find the rotation
 * around this axis such that the satellite phasing axis is in the half-plane
 * defined by a cut line on the pointing axis and containing the celestial
 * phasing reference.
 * </p>
 * <p>
 * In order for this definition to work, the user must ensure that the phasing
 * references are <strong>never</strong> aligned with the pointing references.
 * Since the pointed body moves as the date changes, this should be ensured
 * regardless of the date. A simple way to do this for Sun, Moon or any planet
 * pointing is to choose a phasing reference far from the ecliptic plane. Using
 * <code>Vector3D.PLUS_K</code>, the equatorial pole, is perfect in these cases.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class CelestialBodyPointed implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = 6222161082155807729L;

    /** Frame in which {@link #pointedBody} and {@link #phasingCel} are defined. */
    private final Frame celestialFrame;

    /** Celestial body to point at. */
    private final PVCoordinatesProvider pointedBody;

    /** Phasing reference, in celestial frame. */
    private final Vector3D phasingCel;

    /** Satellite axis aiming at the pointed body, in satellite frame. */
    private final Vector3D pointingSat;

    /** Phasing reference, in satellite frame. */
    private final Vector3D phasingSat;

    /** Creates new instance.
     * @param celestialFrame frame in which <code>pointedBody</code>
     * and <code>phasingCel</code> are defined
     * @param pointedBody celestial body to point at
     * @param phasingCel phasing reference, in celestial frame
     * @param pointingSat satellite vector defining the pointing direction
     * @param phasingSat phasing reference, in satellite frame
     */
    public CelestialBodyPointed(final Frame celestialFrame,
                                final PVCoordinatesProvider pointedBody,
                                final Vector3D phasingCel,
                                final Vector3D pointingSat,
                                final Vector3D phasingSat) {
        this.celestialFrame = celestialFrame;
        this.pointedBody    = pointedBody;
        this.phasingCel     = phasingCel;
        this.pointingSat    = pointingSat;
        this.phasingSat     = phasingSat;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final Orbit orbit)
        throws OrekitException {

        final AbsoluteDate date = orbit.getDate();
        final PVCoordinates pv = orbit.getPVCoordinates();
        final Frame frame = orbit.getFrame();

        // compute celestial references at the specified date
        final PVCoordinates bodyPV    = pointedBody.getPVCoordinates(date, celestialFrame);
        final PVCoordinates satCel    = frame.getTransformTo(celestialFrame, date).transformPVCoordinates(pv);
        final PVCoordinates pointing  = new PVCoordinates(satCel, bodyPV);
        final Vector3D      pointingP = pointing.getPosition();
        final double r2 = Vector3D.dotProduct(pointingP, pointingP);

        // evaluate instant rotation axis
        final Vector3D rotAxisCel =
            new Vector3D(1 / r2, Vector3D.crossProduct(pointingP, pointing.getVelocity()));

        // compute transform from celestial frame to satellite frame
        final Rotation celToSatRotation =
            new Rotation(pointingP, phasingCel, pointingSat, phasingSat);
        final Vector3D celToSatSpin = celToSatRotation.applyTo(rotAxisCel);
        Transform transform = new Transform(celToSatRotation, celToSatSpin);

        if (frame != celestialFrame) {
            // prepend transform from specified frame to celestial frame
            transform = new Transform(frame.getTransformTo(celestialFrame, date), transform);
        }

        // build the attitude
        return new Attitude(date, frame, transform.getRotation(), transform.getRotationRate());

    }

}
