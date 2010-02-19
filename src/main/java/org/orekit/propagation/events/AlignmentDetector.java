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
package org.orekit.propagation.events;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for satellite/body alignment events.
 * <p>This class finds alignment events.</p>
 * <p>Alignment means the conjunction, with some threshold angle, between the satellite
 * position and the projection in the orbital plane of some body position.</p>
 * <p>The default implementation behavior is to {@link #STOP stop} propagation when
 * alignment is reached. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class AlignmentDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -5512125598111644915L;

    /** Body to align. */
    private final PVCoordinatesProvider body;

    /** Alignment angle (rad). */
    private final double alignAngle;

    /** Cosinus of alignment angle. */
    private final double cosAlignAngle;

    /** Sinus of alignment angle. */
    private final double sinAlignAngle;

    /** Build a new alignment detector.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3 and to set the convergence threshold according to orbit size.</p>
     * @param orbit initial orbit
     * @param body the body to align
     * @param alignAngle the alignment angle (rad)
     */
    public AlignmentDetector(final Orbit orbit,
                             final PVCoordinatesProvider body,
                             final double alignAngle) {
        super(orbit.getKeplerianPeriod() / 3, 1.0e-13 * orbit.getKeplerianPeriod());
        this.body = body;
        this.alignAngle = alignAngle;
        this.cosAlignAngle = Math.cos(alignAngle);
        this.sinAlignAngle = Math.sin(alignAngle);
    }

    /** Build a new alignment detector.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3.</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     * @param body the body to align
     * @param alignAngle the alignment angle (rad)
     */
    public AlignmentDetector(final double threshold,
                             final Orbit orbit,
                             final PVCoordinatesProvider body,
                             final double alignAngle) {
        super(orbit.getKeplerianPeriod() / 3, threshold);
        this.body = body;
        this.alignAngle = alignAngle;
        this.cosAlignAngle = Math.cos(alignAngle);
        this.sinAlignAngle = Math.sin(alignAngle);
    }

    /** Get the body to align.
     * @return the body to align
     */
    public PVCoordinatesProvider getPVCoordinatesProvider() {
        return body;
    }

    /** Get the alignment angle (rad).
     * @return the alignment angle
     */
    public double getAlignAngle() {
        return alignAngle;
    }

    /** Handle an alignment event and choose what to do next.
     * <p>The default implementation behavior is to {@link #STOP stop} propagation
     * when alignment is reached.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event.
     * @return {@link #STOP} or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? STOP : CONTINUE;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the alignment angle and the
     * angle between the satellite position and the body position projection in the
     * orbital plane.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final PVCoordinates pv = s.getPVCoordinates();
        final Vector3D a  = pv.getPosition().normalize();
        final Vector3D z  = pv.getMomentum().negate().normalize();
        final Vector3D b  = Vector3D.crossProduct(a, z).normalize();
        final Vector3D x  = new Vector3D(cosAlignAngle, a,  sinAlignAngle, b);
        final Vector3D y  = new Vector3D(sinAlignAngle, a, -cosAlignAngle, b);
        final Vector3D pb = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final double beta = Math.atan2(Vector3D.dotProduct(pb, y), Vector3D.dotProduct(pb, x));
        final double betm = -Math.PI - beta;
        final double betp =  Math.PI - beta;
        if (beta < betm) {
            return betm;
        } else if (beta < betp) {
            return beta;
        } else {
            return betp;
        }
    }

}
