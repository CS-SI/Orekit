/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for satellite/body alignment events in orbital plane.
 * <p>This class finds alignment events.</p>
 * <p>Alignment means the conjunction, with some threshold angle, between the satellite
 * position and the projection in the orbital plane of some body position.</p>
 * <p>The default handler behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop}
 * propagation when alignment is reached. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 */
public class AlignmentDetector extends AbstractDetector<AlignmentDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

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
        this(1.0e-13 * orbit.getKeplerianPeriod(), orbit, body, alignAngle);
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
        this(orbit.getKeplerianPeriod() / 3, threshold, DEFAULT_MAX_ITER,
             new StopOnIncreasing<AlignmentDetector>(),
             body, alignAngle);
    }

    /** Private constructor with full parameters.
     * <p>
     * This constructor is private as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param body the body to align
     * @param alignAngle the alignment angle (rad)
     */
    private AlignmentDetector(final double maxCheck, final double threshold,
                              final int maxIter, final EventHandler<? super AlignmentDetector> handler,
                              final PVCoordinatesProvider body,
                              final double alignAngle) {
        super(maxCheck, threshold, maxIter, handler);
        this.body          = body;
        this.alignAngle    = alignAngle;
        this.cosAlignAngle = FastMath.cos(alignAngle);
        this.sinAlignAngle = FastMath.sin(alignAngle);
    }

    /** {@inheritDoc} */
    @Override
    protected AlignmentDetector create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<? super AlignmentDetector> newHandler) {
        return new AlignmentDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                     body, alignAngle);
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
        final Vector3D b  = Vector3D.crossProduct(pv.getMomentum(), a).normalize();
        final Vector3D x  = new Vector3D(cosAlignAngle, a,  sinAlignAngle, b);
        final Vector3D y  = new Vector3D(sinAlignAngle, a, -cosAlignAngle, b);
        final Vector3D pb = body.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final double beta = FastMath.atan2(Vector3D.dotProduct(pb, y), Vector3D.dotProduct(pb, x));
        final double betm = -FastMath.PI - beta;
        final double betp =  FastMath.PI - beta;
        if (beta < betm) {
            return betm;
        } else if (beta < betp) {
            return beta;
        } else {
            return betp;
        }
    }

}
