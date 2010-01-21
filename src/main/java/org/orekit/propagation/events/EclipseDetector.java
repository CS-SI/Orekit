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
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for satellite eclipse related events.
 * <p>This class finds eclipse events, i.e. satellite within umbra (total
 * eclipse) or penumbra (partial eclipse).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation when entering the eclipse and to
 * {@link EventDetector#STOP stop} propagation when exiting the eclipse.
 * This can be changed by overriding the {@link
 * #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class EclipseDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -541311550206363031L;

    /** Occulting body. */
    private final PVCoordinatesProvider occulting;

    /** Occulting body radius (m). */
    private final double occultingRadius;

    /** Occulted body. */
    private final PVCoordinatesProvider occulted;

    /** Occulted body radius (m). */
    private final double occultedRadius;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private boolean totalEclipse;

    /** Build a new eclipse detector.
     * <p>The new instance is either an umbra detector or a penumbra detector
     * with default values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param totalEclipse umbra (true) or penumbra (false) detection flag
     */
    public EclipseDetector(final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius,
            final boolean totalEclipse) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = totalEclipse;
    }

    /** Build a new eclipse detector.
     * <p>The new instance is either an umbra detector or a penumbra detector
     * with default value for convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param totalEclipse umbra (true) or penumbra (false) detection flag
     */
    public EclipseDetector(final double maxCheck,
            final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius,
            final boolean totalEclipse) {
        super(maxCheck, DEFAULT_THRESHOLD);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = totalEclipse;
    }

    /** Build a new eclipse detector .
     * <p>The new instance is either an umbra detector or a penumbra detector.</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     * @param totalEclipse umbra (true) or penumbra (false) detection flag
     */
    public EclipseDetector(final double maxCheck,
            final double threshold,
            final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius,
            final boolean totalEclipse) {
        super(maxCheck, threshold);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = totalEclipse;
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius (m)
     */
    public EclipseDetector(final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = true;
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * value for convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted in meters
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius in meters
     */
    public EclipseDetector(final double maxCheck,
            final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius) {
        super(maxCheck, DEFAULT_THRESHOLD);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = true;
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector.</p>
     * <p>The maximal interval between eclipse checks should be smaller than
     * the half duration of the minimal pass to handle, otherwise some short
     * passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted in meters
     * @param occulting the occulting body
     * @param occultingRadius the occulting body radius in meters
     */
    public EclipseDetector(final double maxCheck,
            final double threshold,
            final PVCoordinatesProvider occulted,
            final double occultedRadius,
            final PVCoordinatesProvider occulting,
            final double occultingRadius) {
        super(maxCheck, threshold);
        this.occulted = occulted;
        this.occultedRadius = Math.abs(occultedRadius);
        this.occulting = occulting;
        this.occultingRadius = Math.abs(occultingRadius);
        this.totalEclipse = true;
    }

    /** Get the occulting body.
     * @return the occulting body
     */
    public PVCoordinatesProvider getOcculting() {
        return occulting;
    }

    /** Get the occulting body radius (m).
     * @return the occulting body radius
     */
    public double getOccultingRadius() {
        return occultingRadius;
    }

    /** Get the occulted body.
     * @return the occulted body
     */
    public PVCoordinatesProvider getOcculted() {
        return occulted;
    }

    /** Get the occulted body radius (m).
     * @return the occulted body radius
     */
    public double getOccultedRadius() {
        return occultedRadius;
    }


    /** Get the total eclipse detection flag.
     * @return the total eclipse detection flag (true for umbra events detection,
     * false for penumbra events detection)
     */
    public boolean geTotaltEclipse() {
        return totalEclipse;
    }

    /** Handle an eclipse event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation when entering the eclipse and to
     * {@link EventDetector#STOP stop} propagation when exiting the eclipse.</p>
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
     * This function becomes negative when entering the region of shadow
     * and positive when exiting.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final Vector3D pted = occulted.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D ping = occulting.getPVCoordinates(s.getDate(), s.getFrame()).getPosition();
        final Vector3D psat = s.getPVCoordinates().getPosition();
        final Vector3D ps   = pted.subtract(psat);
        final Vector3D po   = ping.subtract(psat);
        final double angle  = Vector3D.angle(ps, po);
        final double rs     = Math.asin(occultedRadius / ps.getNorm());
        final double ro     = Math.asin(occultingRadius / po.getNorm());
        return totalEclipse ? (angle - ro + rs) : (angle - ro - rs);
    }

}
