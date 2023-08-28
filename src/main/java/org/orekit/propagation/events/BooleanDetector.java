/* Contributed in the public domain.
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.hipparchus.util.FastMath;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * This class provides AND and OR operations for event detectors. This class treats
 * positive values of the g function as true and negative values as false.
 *
 * <p> One example for an imaging satellite might be to only detect events when a
 * satellite is overhead (elevation &gt; 0) AND when the ground point is sunlit (Sun
 * elevation &gt; 0). Another slightly contrived example using the OR operator would be to
 * detect access to a set of ground stations and only report events when the satellite
 * enters or leaves the field of view of the set, but not hand-offs between the ground
 * stations.
 *
 * <p> For the BooleanDetector is important that the sign of the g function of the
 * underlying event detector is not arbitrary, but has a semantic meaning, e.g. in or out,
 * true or false. This class works well with event detectors that detect entry to or exit
 * from a region, e.g. {@link EclipseDetector}, {@link ElevationDetector}, {@link
 * LatitudeCrossingDetector}. Using this detector with detectors that are not based on
 * entry to or exit from a region, e.g. {@link DateDetector}, {@link
 * LongitudeCrossingDetector}, will likely lead to unexpected results. To apply conditions
 * to this latter type of event detectors a {@link EventEnablingPredicateFilter} is
 * usually more appropriate.
 *
 * @author Evan Ward
 * @see #andCombine(Collection)
 * @see #orCombine(Collection)
 * @see #notCombine(EventDetector)
 * @see EventEnablingPredicateFilter
 * @see EventSlopeFilter
 */
public class BooleanDetector extends AbstractDetector<BooleanDetector> {

    /** Original detectors: the operands. */
    private final List<EventDetector> detectors;

    /** The composition function. Should be associative for predictable behavior. */
    private final Operator operator;

    /**
     * Private constructor with all the parameters.
     *
     * @param detectors    the operands.
     * @param operator     reduction operator to apply to value of the g function of the
     *                     operands.
     * @param newMaxCheck  max check interval in seconds.
     * @param newThreshold convergence threshold in seconds.
     * @param newMaxIter   max iterations.
     * @param newHandler   event handler.
     */
    protected BooleanDetector(final List<EventDetector> detectors,
                              final Operator operator,
                              final AdaptableInterval newMaxCheck,
                              final double newThreshold,
                              final int newMaxIter,
                              final EventHandler newHandler) {
        super(newMaxCheck, newThreshold, newMaxIter, newHandler);
        this.detectors = detectors;
        this.operator = operator;
    }

    /**
     * Create a new event detector that is the logical AND of the given event detectors.
     *
     * <p> The created event detector's g function is positive if and only if the g
     * functions of all detectors in {@code detectors} are positive.
     *
     * <p> The starting interval, threshold, and iteration count are set to the most
     * stringent (minimum) of all the {@code detectors}. The event handlers of the
     * underlying {@code detectors} are not used, instead the default handler is {@link
     * ContinueOnEvent}.
     *
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical AND of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see BooleanDetector
     * @see #andCombine(Collection)
     * @see #orCombine(EventDetector...)
     * @see #notCombine(EventDetector)
     */
    public static BooleanDetector andCombine(final EventDetector... detectors) {
        return andCombine(Arrays.asList(detectors));
    }

    /**
     * Create a new event detector that is the logical AND of the given event detectors.
     *
     * <p> The created event detector's g function is positive if and only if the g
     * functions of all detectors in {@code detectors} are positive.
     *
     * <p> The starting interval, threshold, and iteration count are set to the most
     * stringent (minimum) of the {@code detectors}. The event handlers of the
     * underlying {@code detectors} are not used, instead the default handler is {@link
     * ContinueOnEvent}.
     *
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical AND of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see BooleanDetector
     * @see #andCombine(EventDetector...)
     * @see #orCombine(Collection)
     * @see #notCombine(EventDetector)
     */
    public static BooleanDetector andCombine(final Collection<? extends EventDetector> detectors) {

        return new BooleanDetector(new ArrayList<>(detectors), // copy for immutability
                Operator.AND,
                s -> {
                    double minInterval = Double.POSITIVE_INFINITY;
                    for (final EventDetector detector : detectors) {
                        minInterval = FastMath.min(minInterval, detector.getMaxCheckInterval().currentInterval(s));
                    }
                    return minInterval;
                },
                detectors.stream().map(EventDetector::getThreshold).min(Double::compareTo).get(),
                detectors.stream().map(EventDetector::getMaxIterationCount).min(Integer::compareTo).get(),
                new ContinueOnEvent());
    }

    /**
     * Create a new event detector that is the logical OR of the given event detectors.
     *
     * <p> The created event detector's g function is positive if and only if at least
     * one of g functions of the event detectors in {@code detectors} is positive.
     *
     * <p> The starting interval, threshold, and iteration count are set to the most
     * stringent (minimum) of the {@code detectors}. The event handlers of the
     * underlying EventDetectors are not used, instead the default handler is {@link
     * ContinueOnEvent}.
     *
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical OR of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see BooleanDetector
     * @see #orCombine(Collection)
     * @see #andCombine(EventDetector...)
     * @see #notCombine(EventDetector)
     */
    public static BooleanDetector orCombine(final EventDetector... detectors) {
        return orCombine(Arrays.asList(detectors));
    }

    /**
     * Create a new event detector that is the logical OR of the given event detectors.
     *
     * <p> The created event detector's g function is positive if and only if at least
     * one of g functions of the event detectors in {@code detectors} is positive.
     *
     * <p> The starting interval, threshold, and iteration count are set to the most
     * stringent (minimum) of the {@code detectors}. The event handlers of the
     * underlying EventDetectors are not used, instead the default handler is {@link
     * ContinueOnEvent}.
     *
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical OR of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see BooleanDetector
     * @see #orCombine(EventDetector...)
     * @see #andCombine(Collection)
     * @see #notCombine(EventDetector)
     */
    public static BooleanDetector orCombine(final Collection<? extends EventDetector> detectors) {

        return new BooleanDetector(new ArrayList<>(detectors), // copy for immutability
                Operator.OR,
                s -> {
                    double minInterval = Double.POSITIVE_INFINITY;
                    for (final EventDetector detector : detectors) {
                        minInterval = FastMath.min(minInterval, detector.getMaxCheckInterval().currentInterval(s));
                    }
                    return minInterval;
                },
                detectors.stream().map(EventDetector::getThreshold).min(Double::compareTo).get(),
                detectors.stream().map(EventDetector::getMaxIterationCount).min(Integer::compareTo).get(),
                new ContinueOnEvent());
    }

    /**
     * Create a new event detector that negates the g function of another detector.
     *
     * <p> This detector will be initialized with the same {@link
     * EventDetector#getMaxCheckInterval()}, {@link EventDetector#getThreshold()}, and
     * {@link EventDetector#getMaxIterationCount()} as {@code detector}. The event handler
     * of the underlying detector is not used, instead the default handler is {@link
     * ContinueOnEvent}.
     *
     * @param detector to negate.
     * @return an new event detector whose g function is the same magnitude but opposite
     * sign of {@code detector}.
     * @see #andCombine(Collection)
     * @see #orCombine(Collection)
     * @see BooleanDetector
     */
    public static NegateDetector notCombine(final EventDetector detector) {
        return new NegateDetector(detector);
    }

    @Override
    public double g(final SpacecraftState s) {
        // can't use stream/lambda here because g(s) throws a checked exception
        // so write out and combine the map and reduce loops
        double ret = Double.NaN; // return value
        boolean first = true;
        for (final EventDetector detector : detectors) {
            if (first) {
                ret = detector.g(s);
                first = false;
            } else {
                ret = operator.combine(ret, detector.g(s));
            }
        }
        // return the result of applying the operator to all operands
        return ret;
    }

    @Override
    protected BooleanDetector create(final AdaptableInterval newMaxCheck,
                                     final double newThreshold,
                                     final int newMaxIter,
                                     final EventHandler newHandler) {
        return new BooleanDetector(detectors, operator, newMaxCheck, newThreshold,
                newMaxIter, newHandler);
    }

    @Override
    public void init(final SpacecraftState s0,
                     final AbsoluteDate t) {
        super.init(s0, t);
        for (final EventDetector detector : detectors) {
            detector.init(s0, t);
        }
    }

    /**
     * Get the list of original detectors.
     * @return the list of original detectors
     * @since 10.2
     */
    public List<EventDetector> getDetectors() {
        return new ArrayList<EventDetector>(detectors);
    }

    /** Local class for operator. */
    private enum Operator {

        /** And operator. */
        AND() {

            @Override
            /** {@inheritDoc} */
            public double combine(final double g1, final double g2) {
                return FastMath.min(g1, g2);
            }

        },

        /** Or operator. */
        OR() {

            @Override
            /** {@inheritDoc} */
            public double combine(final double g1, final double g2) {
                return FastMath.max(g1, g2);
            }

        };

        /** Combine two g functions evaluations.
         * @param g1 first evaluation
         * @param g2 second evaluation
         * @return combined evaluation
         */
        public abstract double combine(double g1, double g2);

    };

}
