/* Copyright 2002-2023 CS GROUP
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
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.OccultationEngine;
import org.orekit.utils.PVCoordinatesProvider;

/** Finder for satellite eclipse related events.
 * <p>This class finds eclipse events, i.e. satellite within umbra (total
 * eclipse) or penumbra (partial eclipse).</p>
 * <p>The occulted body is given through a {@link PVCoordinatesProvider} and its radius in meters. It is modeled as a sphere.
 * </p>
 * <p>Since v10.0 the occulting body is a {@link OneAxisEllipsoid}, before it was modeled as a  sphere.
 * <br>It was changed to precisely model Solar eclipses by the Earth, especially for Low Earth Orbits.
 * <br>If you want eclipses by a spherical occulting body, set its flattening to 0. when defining its OneAxisEllipsoid model..
 * </p>
 * <p>The {@link #withUmbra} or {@link #withPenumbra} methods will tell you if the event is triggered when complete umbra/lighting
 * is achieved or when entering/living the penumbra zone.
 * <br>The default behavior is detecting complete umbra/lighting events.
 * <br>If you want to have both, you'll need to set up two distinct detectors.
 * </p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation when entering the eclipse and to {@link Action#STOP stop} propagation
 * when exiting the eclipse.
 * <br>This can be changed by calling {@link #withHandler(EventHandler)} after construction.
 * </p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
public class EclipseDetector extends AbstractDetector<EclipseDetector> {

    /** Occultation engine.
     * @since 12.0
     */
    private final OccultationEngine occultationEngine;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private final boolean totalEclipse;

    /** Margin to apply to eclipse angle. */
    private final double margin;

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @since 12.0
     */
    public EclipseDetector(final ExtendedPVCoordinatesProvider occulted,  final double occultedRadius,
                           final OneAxisEllipsoid occulting) {
        this(new OccultationEngine(occulted, occultedRadius, occulting));
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param occultationEngine occultation engine
     * @since 12.0
     */
    public EclipseDetector(final OccultationEngine occultationEngine) {
        this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
             new StopOnIncreasing(),
             occultationEngine, 0.0, true);
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
     * @param occultationEngine occultation engine
     * @param margin to apply to eclipse angle (rad)
     * @param totalEclipse umbra (true) or penumbra (false) detection flag
     * @since 12.0
     */
    protected EclipseDetector(final AdaptableInterval maxCheck, final double threshold,
                              final int maxIter, final EventHandler handler,
                              final OccultationEngine occultationEngine, final double margin, final boolean totalEclipse) {
        super(maxCheck, threshold, maxIter, handler);
        this.occultationEngine = occultationEngine;
        this.margin            = margin;
        this.totalEclipse      = totalEclipse;
    }

    /** {@inheritDoc} */
    @Override
    protected EclipseDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                     final int nawMaxIter, final EventHandler newHandler) {
        return new EclipseDetector(newMaxCheck, newThreshold, nawMaxIter, newHandler,
                                   occultationEngine, margin, totalEclipse);
    }

    /**
     * Setup the detector to full umbra detection.
     * <p>
     * This will override a penumbra/umbra flag if it has been configured previously.
     * </p>
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #withPenumbra()
     * @since 6.1
     */
    public EclipseDetector withUmbra() {
        return new EclipseDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                   occultationEngine, margin, true);
    }

    /**
     * Setup the detector to penumbra detection.
     * <p>
     * This will override a penumbra/umbra flag if it has been configured previously.
     * </p>
     * @return a new detector with updated configuration (the instance is not changed)
     * @see #withUmbra()
     * @since 6.1
     */
    public EclipseDetector withPenumbra() {
        return new EclipseDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                   occultationEngine, margin, false);
    }

    /**
     * Setup a margin to angle detection.
     * <p>
     * A positive margin implies eclipses are "larger" hence entry occurs earlier and exit occurs later
     * than a detector with 0 margin.
     * </p>
     * @param newMargin angular margin to apply to eclipse detection (rad)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public EclipseDetector withMargin(final double newMargin) {
        return new EclipseDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                   occultationEngine, newMargin, totalEclipse);
    }

    /** Get the angular margin used for eclipse detection.
     * @return angular margin used for eclipse detection (rad)
     * @since 12.0
     */
    public double getMargin() {
        return margin;
    }

    /** Get the occultation engine.
     * @return occultation engine
     * @since 12.0
     */
    public OccultationEngine getOccultationEngine() {
        return occultationEngine;
    }

    /** Get the total eclipse detection flag.
     * @return the total eclipse detection flag (true for umbra events detection,
     * false for penumbra events detection)
     */
    public boolean getTotalEclipse() {
        return totalEclipse;
    }

    /** Compute the value of the switching function.
     * This function becomes negative when entering the region of shadow
     * and positive when exiting.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
        final OccultationEngine.OccultationAngles angles = occultationEngine.angles(s);
        return totalEclipse ?
               (angles.getSeparation() - angles.getLimbRadius() + angles.getOccultedApparentRadius() + margin) :
               (angles.getSeparation() - angles.getLimbRadius() - angles.getOccultedApparentRadius() + margin);
    }

}
