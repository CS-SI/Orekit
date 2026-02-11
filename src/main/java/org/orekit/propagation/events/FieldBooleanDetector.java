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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.BooleanEventFunction;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.FieldAbsoluteDate;

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
 * <p> For the FieldBooleanDetector is important that the sign of the g function of the
 * underlying event detector is not arbitrary, but has a semantic meaning, e.g. in or out,
 * true or false. This class works well with event detectors that detect entry to or exit
 * from a region, e.g. {@link FieldEclipseDetector}, {@link FieldElevationDetector}, {@link
 * FieldLatitudeCrossingDetector}. Using this detector with detectors that are not based on
 * entry to or exit from a region, e.g. {@link FieldDateDetector}, will likely lead to
 * unexpected results. To apply conditions to this latter type of event detectors a
 * {@link FieldEventEnablingPredicateFilter} is usually more appropriate.
 *
 * @param <T> type of the field elements
 * @since 12.0
 * @author Evan Ward
 * @author luc Luc Maisonobe
 * @see #andCombine(Collection)
 * @see #orCombine(Collection)
 * @see #notCombine(FieldEventDetector)
 * @see EventEnablingPredicateFilter
 * @see EventSlopeFilter
 */
public class FieldBooleanDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldBooleanDetector<T>, T> {

    /** Original detectors: the operands. */
    private final List<FieldEventDetector<T>> detectors;

    /**
     * Private constructor with all the parameters.
     *
     * @param detectors    the operands.
     * @param eventFunction reduced event function.
     * @param detectionSettings event detection settings.
     * @param newHandler   event handler.
     * @since 14.0
     */
    protected FieldBooleanDetector(final List<FieldEventDetector<T>> detectors,
                                   final BooleanEventFunction eventFunction,
                                   final FieldEventDetectionSettings<T> detectionSettings,
                                   final FieldEventHandler<T> newHandler) {
        super(eventFunction, detectionSettings, newHandler);
        this.detectors = detectors;
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
     * FieldContinueOnEvent}.
     *
     * @param <T> type of the field elements
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical AND of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see FieldBooleanDetector
     * @see #andCombine(Collection)
     * @see #orCombine(FieldEventDetector...)
     * @see #notCombine(FieldEventDetector)
     */
    @SafeVarargs
    public static <T extends CalculusFieldElement<T>> FieldBooleanDetector<T> andCombine(final FieldEventDetector<T>... detectors) {
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
     * FieldContinueOnEvent}.
     *
     * @param <T> type of the field elements
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical AND of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see FieldBooleanDetector
     * @see #andCombine(FieldEventDetector...)
     * @see #orCombine(Collection)
     * @see #notCombine(FieldEventDetector)
     */
    public static <T extends CalculusFieldElement<T>> FieldBooleanDetector<T> andCombine(final Collection<? extends FieldEventDetector<T>> detectors) {
        @SuppressWarnings("unchecked")
        final FieldAdaptableInterval<T> fai = FieldAdaptableInterval.of(Double.POSITIVE_INFINITY,
                                                                        detectors.stream()
                                                                                 .map(FieldEventDetector::getMaxCheckInterval)
                                                                                 .toArray(FieldAdaptableInterval[]::new));
        final T       threshold = detectors.stream().map(FieldEventDetector::getThreshold).min(new FieldComparator<>()).get();
        final Integer maxIters  = detectors.stream().map(FieldEventDetector::getMaxIterationCount).min(Integer::compareTo).get();
        return new FieldBooleanDetector<>(new ArrayList<>(detectors), // copy for immutability
                                          BooleanEventFunction.andCombine(detectors.stream().map(FieldEventDetector::getEventFunction).collect(Collectors.toList())),
                                          new FieldEventDetectionSettings<>(fai, threshold, maxIters),
                                          new FieldContinueOnEvent<>());
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
     * FieldContinueOnEvent}.
     *
     * @param <T> type of the field elements
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical OR of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see FieldBooleanDetector
     * @see #orCombine(Collection)
     * @see #andCombine(FieldEventDetector...)
     * @see #notCombine(FieldEventDetector)
     */
    @SafeVarargs
    public static <T extends CalculusFieldElement<T>> FieldBooleanDetector<T> orCombine(final FieldEventDetector<T>... detectors) {
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
     * FieldContinueOnEvent}.
     *
     * @param <T> type of the field elements
     * @param detectors the operands. Must contain at least one detector.
     * @return a new event detector that is the logical OR of the operands.
     * @throws NoSuchElementException if {@code detectors} is empty.
     * @see FieldBooleanDetector
     * @see #orCombine(FieldEventDetector...)
     * @see #andCombine(Collection)
     * @see #notCombine(FieldEventDetector)
     */
    public static <T extends CalculusFieldElement<T>> FieldBooleanDetector<T> orCombine(final Collection<? extends FieldEventDetector<T>> detectors) {
        @SuppressWarnings("unchecked")
        final FieldAdaptableInterval<T> fai = FieldAdaptableInterval.of(Double.POSITIVE_INFINITY,
                                                                        detectors.stream()
                                                                                 .map(FieldEventDetector::getMaxCheckInterval)
                                                                                 .toArray(FieldAdaptableInterval[]::new));
        final T       threshold = detectors.stream().map(FieldEventDetector::getThreshold).min(new FieldComparator<>()).get();
        final Integer maxIters  = detectors.stream().map(FieldEventDetector::getMaxIterationCount).min(Integer::compareTo).get();
        return new FieldBooleanDetector<>(new ArrayList<>(detectors), // copy for immutability
                BooleanEventFunction.orCombine(detectors.stream().map(FieldEventDetector::getEventFunction).collect(Collectors.toList())),
                                          new FieldEventDetectionSettings<>(fai, threshold, maxIters),
                                          new FieldContinueOnEvent<>());
    }

    /**
     * Create a new event detector that negates the g function of another detector.
     *
     * <p> This detector will be initialized with the same {@link
     * FieldEventDetector#getMaxCheckInterval()}, {@link FieldEventDetector#getThreshold()}, and
     * {@link FieldEventDetector#getMaxIterationCount()} as {@code detector}. The event handler
     * of the underlying detector is not used, instead the default handler is {@link
     * FieldContinueOnEvent}.
     *
     * @param <T> type of the field elements
     * @param detector to negate.
     * @return an new event detector whose g function is the same magnitude but opposite
     * sign of {@code detector}.
     * @see #andCombine(Collection)
     * @see #orCombine(Collection)
     * @see FieldBooleanDetector
     */
    public static <T extends CalculusFieldElement<T>> FieldNegateDetector<T> notCombine(final FieldEventDetector<T> detector) {
        return new FieldNegateDetector<>(detector);
    }

    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
    }

    @Override
    protected FieldBooleanDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                             final FieldEventHandler<T> newHandler) {
        return new FieldBooleanDetector<>(detectors, (BooleanEventFunction) getEventFunction(), detectionSettings, newHandler);
    }

    @Override
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        super.init(s0, t);
        for (final FieldEventDetector<T> detector : detectors) {
            detector.init(s0, t);
        }
    }

    @Override
    public void reset(final FieldSpacecraftState<T> state, final FieldAbsoluteDate<T> target) {
        super.reset(state, target);
        for (final FieldEventDetector<T> detector : detectors) {
            detector.reset(state, target);
        }
    }

    @Override
    public void finish(final FieldSpacecraftState<T> state) {
        super.finish(state);
        for (final FieldEventDetector<T> detector : detectors) {
            detector.finish(state);
        }
    }

    /**
     * Get the list of original detectors.
     * @return the list of original detectors
     */
    public List<FieldEventDetector<T>> getDetectors() {
        return new ArrayList<>(detectors);
    }

    /** Comparator for field elements.
     * @param <T> type of the field elements
     */
    private static class FieldComparator<T extends CalculusFieldElement<T>> implements Comparator<T>, Serializable {
        public int compare(final T t1, final T t2) {
            return Double.compare(t1.getReal(), t2.getReal());
        }
    }

}
