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

import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Finder for satellite altitude crossing events.
 * <p>This class finds altitude events (i.e. satellite crossing
 * a predefined altitude level above ground).</p>
 * <p>The default implementation behavior is to {@link
 * EventDetector#CONTINUE continue} propagation when ascending and to
 * {@link EventDetector#STOP stop} propagation
 * when descending. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class AltitudeDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -1552109617025755015L;

    /** Threshold altitude value (m). */
    private final double altitude;

    /** Body shape with respect to which altitude should be evaluated. */
    private final BodyShape bodyShape;

    /** Build a new altitude detector.
     * <p>This simple constructor takes default values for maximal checking
     *  interval ({@link #DEFAULT_MAXCHECK}) and convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * @param altitude threshold altitude value
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double altitude, final BodyShape bodyShape) {
        super(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD);
        this.altitude  = altitude;
        this.bodyShape = bodyShape;
    }

    /** Build a new altitude detector.
     * <p>This simple constructor takes default value for convergence threshold
     * ({@link #DEFAULT_THRESHOLD}).</p>
     * <p>The maximal interval between altitude checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param altitude threshold altitude value (m)
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double maxCheck,
            final double altitude,
            final BodyShape bodyShape) {
        super(maxCheck, DEFAULT_THRESHOLD);
        this.altitude  = altitude;
        this.bodyShape = bodyShape;
    }

    /** Build a new altitude detector.
     * <p>The maximal interval between altitude checks should
     * be smaller than the half duration of the minimal pass to handle,
     * otherwise some short passes could be missed.</p>
     * @param maxCheck maximal checking interval (s)
     * @param threshold convergence threshold (s)
     * @param altitude threshold altitude value (m)
     * @param bodyShape body shape with respect to which altitude should be evaluated
     */
    public AltitudeDetector(final double maxCheck,
            final double threshold,
            final double altitude,
            final BodyShape bodyShape) {
        super(maxCheck, threshold);
        this.altitude  = altitude;
        this.bodyShape = bodyShape;
    }

    /** Get the threshold altitude value.
     * @return the threshold altitude value (m)
     */
    public double getAltitude() {
        return altitude;
    }

    /** Get the body shape.
     * @return the body shape
     */
    public BodyShape getBodyShape() {
        return bodyShape;
    }

    /** Handle an altitude event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#CONTINUE continue} propagation when ascending and to
     * {@link EventDetector#STOP stop} propagationwhen descending.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event
     * @return {@link #STOP} or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return increasing ? CONTINUE : STOP;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current altitude
     * and the threshold altitude.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        final Frame bodyFrame      = bodyShape.getBodyFrame();
        final PVCoordinates pvBody = s.getPVCoordinates(bodyFrame);
        final GeodeticPoint point  = bodyShape.transform(pvBody.getPosition(),
                                                         bodyFrame, s.getDate());
        return point.getAltitude() - altitude;
    }

}
