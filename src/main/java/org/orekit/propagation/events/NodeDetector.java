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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Finder for node crossing events.
 * <p>This class finds equator crossing events (i.e. ascending
 * or descending node crossing).</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#CONTINUE continue}
 * propagation at descending node crossing and to {@link
 * org.orekit.propagation.events.handlers.EventHandler.Action#STOP stop} propagation
 * at ascending node crossing. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * <p>Beware that node detection will fail for almost equatorial orbits. If
 * for example a node detector is used to trigger an {@link
 * org.orekit.forces.maneuvers.ImpulseManeuver ImpulseManeuver} and the maneuver
 * turn the orbit plane to equator, then the detector may completely fail just
 * after the maneuver has been performed! This is a real case that has been
 * encountered during validation ...</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public class NodeDetector extends AbstractDetector<NodeDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131118L;

    /** Frame in which the equator is defined. */
    private final Frame frame;

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3 and to set the convergence threshold according to orbit size.</p>
     * @param orbit initial orbit
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     */
    public NodeDetector(final Orbit orbit, final Frame frame) {
        this(1.0e-13 * orbit.getKeplerianPeriod(), orbit, frame);
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to period/3.</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     */
    public NodeDetector(final double threshold, final Orbit orbit, final Frame frame) {
        this(2 * estimateNodesTimeSeparation(orbit) / 3, threshold,
             DEFAULT_MAX_ITER, new StopOnIncreasing<NodeDetector>(),
             frame);
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
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     * @since 6.1
     */
    private NodeDetector(final double maxCheck, final double threshold,
                         final int maxIter, final EventHandler<? super NodeDetector> handler,
                         final Frame frame) {
        super(maxCheck, threshold, maxIter, handler);
        this.frame = frame;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDetector create(final double newMaxCheck, final double newThreshold,
                                  final int newMaxIter, final EventHandler<? super NodeDetector> newHandler) {
        return new NodeDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, frame);
    }

    /** Find time separation between nodes.
     * <p>
     * The estimation of time separation is based on Keplerian motion, it is only
     * used as a rough guess for a safe setting of default max check interval for
     * event detection.
     * </p>
     * @param orbit initial orbit
     * @return minimum time separation between nodes
     */
    private static double estimateNodesTimeSeparation(final Orbit orbit) {

        final KeplerianOrbit keplerian = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

        // mean anomaly of ascending node
        final double ascendingM  =  new KeplerianOrbit(keplerian.getA(), keplerian.getE(),
                                                       keplerian.getI(),
                                                       keplerian.getPerigeeArgument(),
                                                       keplerian.getRightAscensionOfAscendingNode(),
                                                       -keplerian.getPerigeeArgument(), PositionAngle.TRUE,
                                                       keplerian.getFrame(), keplerian.getDate(),
                                                       keplerian.getMu()).getMeanAnomaly();

        // mean anomaly of descending node
        final double descendingM =  new KeplerianOrbit(keplerian.getA(), keplerian.getE(),
                                                       keplerian.getI(),
                                                       keplerian.getPerigeeArgument(),
                                                       keplerian.getRightAscensionOfAscendingNode(),
                                                       FastMath.PI - keplerian.getPerigeeArgument(), PositionAngle.TRUE,
                                                       keplerian.getFrame(), keplerian.getDate(),
                                                       keplerian.getMu()).getMeanAnomaly();

        // differences between mean anomalies
        final double delta1 = MathUtils.normalizeAngle(ascendingM, descendingM + FastMath.PI) - descendingM;
        final double delta2 = 2 * FastMath.PI - delta1;

        // minimum time separation between the two nodes
        return FastMath.min(delta1, delta2) / keplerian.getKeplerianMeanMotion();

    }

    /** Get the frame in which the equator is defined.
     * @return the frame in which the equator is defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** Compute the value of the switching function.
     * This function computes the Z position in the defined frame.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        return s.getPVCoordinates(frame).getPosition().getZ();
    }

}
