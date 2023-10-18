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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.OccultationEngine;

/** Finder for satellite eclipse related events.
 * <p>This class finds eclipse events, i.e. satellite within umbra (total
 * eclipse) or penumbra (partial eclipse).</p>
 * <p>The default implementation behavior is to {@link Action#CONTINUE continue}
 * propagation when entering the eclipse and to {@link Action#STOP stop} propagation
 * when exiting the eclipse. This can be changed by calling {@link
 * #withHandler(FieldEventHandler)} after construction.</p>
 * @param <T> the type of the field elements
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Pascal Parraud
 */
public class FieldEclipseDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldEclipseDetector<T>, T> {

    /** Occultation engine.
     * @since 12.0
     */
    private final OccultationEngine occultationEngine;

    /** Umbra, if true, or penumbra, if false, detection flag. */
    private boolean totalEclipse;

    /** Margin to apply to eclipse angle. */
    private final T margin;

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param field field used by default
     * @param occulted the body to be occulted
     * @param occultedRadius the radius of the body to be occulted (m)
     * @param occulting the occulting body
     * @since 12.0
     */
    public FieldEclipseDetector(final Field<T> field,
                                final ExtendedPVCoordinatesProvider occulted, final double occultedRadius,
                                final OneAxisEllipsoid occulting) {
        this(field, new OccultationEngine(occulted, occultedRadius, occulting));
    }

    /** Build a new eclipse detector.
     * <p>The new instance is a total eclipse (umbra) detector with default
     * values for maximal checking interval ({@link #DEFAULT_MAXCHECK})
     * and convergence threshold ({@link #DEFAULT_THRESHOLD}).</p>
     * @param field field used by default
     * @param occultationEngine occultation engine
     * @since 12.0
     */
    public FieldEclipseDetector(final Field<T> field, final OccultationEngine occultationEngine) {
        this(s -> DEFAULT_MAXCHECK, field.getZero().newInstance(DEFAULT_THRESHOLD),
             DEFAULT_MAX_ITER, new FieldStopOnIncreasing<>(),
             occultationEngine, field.getZero(), true);
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
    protected FieldEclipseDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold,
                                   final int maxIter, final FieldEventHandler<T> handler,
                                   final OccultationEngine occultationEngine, final T margin, final boolean totalEclipse) {
        super(maxCheck, threshold, maxIter, handler);
        this.occultationEngine = occultationEngine;
        this.margin            = margin;
        this.totalEclipse      = totalEclipse;
    }

    /** {@inheritDoc} */
    @Override
    protected FieldEclipseDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold, final int nawMaxIter,
                                             final FieldEventHandler<T> newHandler) {
        return new FieldEclipseDetector<>(newMaxCheck, newThreshold, nawMaxIter, newHandler,
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
    public FieldEclipseDetector<T> withUmbra() {
        return new FieldEclipseDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
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
    public FieldEclipseDetector<T> withPenumbra() {
        return new FieldEclipseDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
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
    public FieldEclipseDetector<T> withMargin(final T newMargin) {
        return new FieldEclipseDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), getHandler(),
                                          occultationEngine, newMargin, totalEclipse);
    }

    /** Get the angular margin used for eclipse detection.
     * @return angular margin used for eclipse detection (rad)
     * @since 12.0
     */
    public T getMargin() {
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
    public T g(final FieldSpacecraftState<T> s) {
        final OccultationEngine.FieldOccultationAngles<T> angles = occultationEngine.angles(s);
        return totalEclipse ?
               angles.getSeparation().subtract(angles.getLimbRadius()).add(angles.getOccultedApparentRadius().add(margin)) :
               angles.getSeparation().subtract(angles.getLimbRadius()).subtract(angles.getOccultedApparentRadius().add(margin));
    }

}
