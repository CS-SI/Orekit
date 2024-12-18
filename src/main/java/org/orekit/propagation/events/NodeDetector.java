/* Copyright 2002-2024 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;

/** Finder for node crossing events.
 * <p>This class finds equator crossing events (i.e. ascending
 * or descending node crossing).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation at descending node crossing and to {@link Action#STOP stop} propagation
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

    /** Default max check interval. */
    private static final double DEFAULT_MAX_CHECK = 1800.0;

    /** Default convergence threshold. */
    private static final double DEFAULT_THRESHOLD = 1.0e-3;

    /** Frame in which the equator is defined. */
    private final Frame frame;

    /** Build a new instance.
     * <p>The default {@link #getMaxCheckInterval() max check interval}
     * is set to 1800s, it can be changed using {@link #withMaxCheck(double)}
     * in the fluent API. The default {@link #getThreshold() convergence threshold}
     * is set to 1.0e-3s, it can be changed using {@link #withThreshold(double)}
     * in the fluent API.</p>
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     * @since 10.3
     */
    public NodeDetector(final Frame frame) {
        this(new EventDetectionSettings(AdaptableInterval.of(DEFAULT_MAX_CHECK), DEFAULT_THRESHOLD, DEFAULT_MAX_ITER),
             new StopOnIncreasing(), frame);
    }

    /** Build a new instance.
     * <p>The orbit is used only to set an upper bound for the max check interval
     * to a value related to nodes separation (as computed by a Keplerian model)
     * and to set the convergence threshold according to orbit size.</p>
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
     * to a value related to nodes separation (as computed by a Keplerian model).</p>
     * @param threshold convergence threshold (s)
     * @param orbit initial orbit
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     */
    public NodeDetector(final double threshold, final Orbit orbit, final Frame frame) {
        this(new EventDetectionSettings(AdaptableInterval.of(2 * estimateNodesTimeSeparation(orbit) / 3), threshold,
             DEFAULT_MAX_ITER), new StopOnIncreasing(),
             frame);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     * @since 6.1
     * @deprecated as of 12.2
     */
    @Deprecated
    protected NodeDetector(final AdaptableInterval maxCheck, final double threshold,
                           final int maxIter, final EventHandler handler,
                           final Frame frame) {
        this(new EventDetectionSettings(maxCheck, threshold, maxIter), handler, frame);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param detectionSettings detection settings
     * @param handler event handler to call at event occurrences
     * @param frame frame in which the equator is defined (typical
     * values are {@link org.orekit.frames.FramesFactory#getEME2000() EME<sub>2000</sub>} or
     * {@link org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF})
     * @since 12.2
     */
    protected NodeDetector(final EventDetectionSettings detectionSettings, final EventHandler handler,
                           final Frame frame) {
        super(detectionSettings, handler);
        this.frame = frame;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                  final int newMaxIter, final EventHandler newHandler) {
        return new NodeDetector(new EventDetectionSettings(newMaxCheck, newThreshold, newMaxIter), newHandler, frame);
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
                                                       -keplerian.getPerigeeArgument(), PositionAngleType.TRUE,
                                                       keplerian.getFrame(), keplerian.getDate(),
                                                       keplerian.getMu()).getMeanAnomaly();

        // mean anomaly of descending node
        final double descendingM =  new KeplerianOrbit(keplerian.getA(), keplerian.getE(),
                                                       keplerian.getI(),
                                                       keplerian.getPerigeeArgument(),
                                                       keplerian.getRightAscensionOfAscendingNode(),
                                                       FastMath.PI - keplerian.getPerigeeArgument(), PositionAngleType.TRUE,
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
     */
    public double g(final SpacecraftState s) {
        return s.getPosition(frame).getZ();
    }

}
