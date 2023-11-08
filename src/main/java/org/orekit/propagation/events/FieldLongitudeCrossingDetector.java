/* Copyright 2023-2023 Alberto Ferrero
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Alberto Ferrero licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnIncreasing;
import org.orekit.time.FieldAbsoluteDate;

/** Detector for geographic longitude crossing.
 * <p>This detector identifies when a spacecraft crosses a fixed
 * longitude with respect to a central body.</p>
 * @author Alberto Ferrero
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldLongitudeCrossingDetector <T extends CalculusFieldElement<T>>
    extends FieldAbstractDetector<FieldLongitudeCrossingDetector<T>, T> {

    /**
    * Body on which the longitude is defined.
    */
    private OneAxisEllipsoid body;

    /**
    * Fixed longitude to be crossed.
    */
    private final double longitude;

    /**
    * Filtering detector.
    */
    private final FieldEventEnablingPredicateFilter<T> filtering;

    /**
    * Build a new detector.
    * <p>The new instance uses default values for maximal checking interval
    * ({@link #DEFAULT_MAXCHECK}) and convergence threshold ({@link
    * #DEFAULT_THRESHOLD}).</p>
    *
    * @param field     the type of numbers to use.
    * @param body      body on which the longitude is defined
    * @param longitude longitude to be crossed
    */
    public FieldLongitudeCrossingDetector(final Field<T> field, final OneAxisEllipsoid body, final double longitude) {
        this(s -> DEFAULT_MAXCHECK,
            field.getZero().add(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER, new FieldStopOnIncreasing<>(), body, longitude);
    }

    /**
    * Build a detector.
    *
    * @param maxCheck  maximal checking interval (s)
    * @param threshold convergence threshold (s)
    * @param body      body on which the longitude is defined
    * @param longitude longitude to be crossed
    */
    public FieldLongitudeCrossingDetector(final T maxCheck,
                                          final T threshold,
                                          final OneAxisEllipsoid body,
                                          final double longitude) {
        this(s -> maxCheck.getReal(), threshold, DEFAULT_MAX_ITER, new FieldStopOnIncreasing<>(), body, longitude);
    }

    /**
    * Protected constructor with full parameters.
    * <p>
    * This constructor is not public as users are expected to use the builder
    * API with the various {@code withXxx()} methods to set up the instance
    * in a readable manner without using a huge amount of parameters.
    * </p>
    *
    * @param maxCheck  maximum checking interval
    * @param threshold convergence threshold (s)
    * @param maxIter   maximum number of iterations in the event time search
    * @param handler   event handler to call at event occurrences
    * @param body      body on which the longitude is defined
    * @param longitude longitude to be crossed
    */
    protected FieldLongitudeCrossingDetector(
        final FieldAdaptableInterval<T> maxCheck,
        final T threshold,
        final int maxIter,
        final FieldEventHandler<T> handler,
        final OneAxisEllipsoid body,
        final double longitude) {

        super(maxCheck, threshold, maxIter, handler);

        this.body = body;
        this.longitude = longitude;

        // we filter out spurious longitude crossings occurring at the antimeridian
        final FieldRawLongitudeCrossingDetector<T> raw = new FieldRawLongitudeCrossingDetector<>(maxCheck, threshold, maxIter,
            new FieldContinueOnEvent<>());
        final FieldEnablingPredicate<T> predicate =
            (state, detector, g) -> FastMath.abs(g).getReal() < 0.5 * FastMath.PI;
        this.filtering = new FieldEventEnablingPredicateFilter<T>(raw, predicate);

    }

    /**
    * {@inheritDoc}
    */
    @Override
    protected FieldLongitudeCrossingDetector<T> create(
        final FieldAdaptableInterval<T> newMaxCheck,
        final T newThreshold,
        final int newMaxIter,
        final FieldEventHandler<T> newHandler) {
        return new FieldLongitudeCrossingDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
            body, longitude);
    }

    /**
    * Get the body on which the geographic zone is defined.
    *
    * @return body on which the geographic zone is defined
    */
    public OneAxisEllipsoid getBody() {
        return body;
    }

    /**
    * Get the fixed longitude to be crossed (radians).
    *
    * @return fixed longitude to be crossed (radians)
    */
    public double getLongitude() {
        return longitude;
    }

    /**
    * {@inheritDoc}
    */
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        filtering.init(s0, t);
    }

    /**
    * Compute the value of the detection function.
    * <p>
    * The value is the longitude difference between the spacecraft and the fixed
    * longitude to be crossed, with some sign tweaks to ensure continuity.
    * These tweaks imply the {@code increasing} flag in events detection becomes
    * irrelevant here! As an example, the longitude of a prograde spacecraft
    * will always increase, but this g function will increase and decrease so it
    * will cross the zero value once per orbit, in increasing and decreasing
    * directions on alternate orbits. If eastwards and westwards crossing have to
    * be distinguished, the velocity direction has to be checked instead of looking
    * at the {@code increasing} flag.
    * </p>
    *
    * @param s the current state information: date, kinematics, attitude
    * @return longitude difference between the spacecraft and the fixed
    * longitude, with some sign tweaks to ensure continuity
    */
    public T g(final FieldSpacecraftState<T> s) {
        return filtering.g(s);
    }

    private class FieldRawLongitudeCrossingDetector <TT extends CalculusFieldElement<TT>>
        extends FieldAbstractDetector<FieldRawLongitudeCrossingDetector<TT>, TT> {

        /**
        * Protected constructor with full parameters.
        * <p>
        * This constructor is not public as users are expected to use the builder
        * API with the various {@code withXxx()} methods to set up the instance
        * in a readable manner without using a huge amount of parameters.
        * </p>
        *
        * @param maxCheck  maximum checking interval
        * @param threshold convergence threshold (s)
        * @param maxIter   maximum number of iterations in the event time search
        * @param handler   event handler to call at event occurrences
        */
        protected FieldRawLongitudeCrossingDetector(
            final FieldAdaptableInterval<TT> maxCheck,
            final TT threshold,
            final int maxIter,
            final FieldEventHandler<TT> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        /**
        * {@inheritDoc}
        */
        @Override
        protected FieldRawLongitudeCrossingDetector<TT> create(
            final FieldAdaptableInterval<TT> newMaxCheck,
            final TT newThreshold,
            final int newMaxIter,
            final FieldEventHandler<TT> newHandler) {
            return new FieldRawLongitudeCrossingDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        /**
        * Compute the value of the detection function.
        * <p>
        * The value is the longitude difference between the spacecraft and the fixed
        * longitude to be crossed, and it <em>does</em> change sign twice around
        * the central body: once at expected longitude and once at antimeridian.
        * The second sign change is a spurious one and is filtered out by the
        * outer class.
        * </p>
        *
        * @param s the current state information: date, kinematics, attitude
        * @return longitude difference between the spacecraft and the fixed
        * longitude
        */
        public TT g(final FieldSpacecraftState<TT> s) {

            // convert state to geodetic coordinates
            final FieldGeodeticPoint<TT> gp = body.transform(s.getPosition(),
                s.getFrame(), s.getDate());

            // longitude difference
            final TT zero = gp.getLongitude().getField().getZero();
            return MathUtils.normalizeAngle(gp.getLongitude().subtract(longitude), zero);

        }

    }

}
