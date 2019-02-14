/* Contributed in the public domain.
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

import java.util.function.ToDoubleFunction;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * A detector that implements the {@link #g(SpacecraftState)} function using a lambda that
 * can be set using {@link #withFunction(ToDoubleFunction)}.
 *
 * <p>For example, to create a simple date detector use:
 *
 * <code><pre>
 * FunctionalDetector d = new FunctionalDetector()
 *     .withGFunction((s) -&gt; s.getDate().durationFrom(triggerDate))
 *     .withMaxCheck(1e10);
 * </pre></code>
 *
 * @author Evan Ward
 * @since 9.2
 */
public class FunctionalDetector extends AbstractDetector<FunctionalDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180525L;

    /** The g function. Deprecated in favor of the standard java.util interface. */
    @Deprecated
    private final GFunction gFunction;

    /** The g function. */
    private final ToDoubleFunction<SpacecraftState> function;

    /**
     * Create an event detector with the default values. These are {@link
     * #DEFAULT_MAXCHECK}, {@link #DEFAULT_THRESHOLD}, {@link #DEFAULT_MAX_ITER}, {@link
     * ContinueOnEvent}, and a g function that is identically unity.
     */
    public FunctionalDetector() {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                new ContinueOnEvent<>(),
                (ToDoubleFunction<SpacecraftState>) value -> 1.0);
    }

    /**
     * Private constructor.
     *
     * @param maxCheck  maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @param gFunction the switching function.
     * @deprecated Use {@link #FunctionalDetector(double, double, int, EventHandler,
     * ToDoubleFunction)} instead. Will be removed in the next major release.
     */
    @Deprecated
    private FunctionalDetector(final double maxCheck,
                               final double threshold,
                               final int maxIter,
                               final EventHandler<? super FunctionalDetector> handler,
                               final GFunction gFunction) {
        super(maxCheck, threshold, maxIter, handler);
        this.gFunction = gFunction;
        this.function = gFunction::apply;
    }

    /**
     * Private constructor.
     *
     * @param maxCheck  maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @param function  the switching function.
     */
    private FunctionalDetector(final double maxCheck,
                               final double threshold,
                               final int maxIter,
                               final EventHandler<? super FunctionalDetector> handler,
                               final ToDoubleFunction<SpacecraftState> function) {
        super(maxCheck, threshold, maxIter, handler);
        this.gFunction = function::applyAsDouble;
        this.function = function;
    }


    @Override
    public double g(final SpacecraftState s) {
        return gFunction.apply(s);
    }

    @Override
    protected FunctionalDetector create(
            final double newMaxCheck,
            final double newThreshold,
            final int newMaxIter,
            final EventHandler<? super FunctionalDetector> newHandler) {

        return new FunctionalDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                gFunction);
    }

    /**
     * Create a new event detector with a new g function, keeping all other attributes the
     * same. It is recommended to use {@link #withMaxCheck(double)} and {@link
     * #withThreshold(double)} to set appropriate values for this g function.
     *
     * @param newGFunction the new g function.
     * @return a new detector with the new g function.
     */
    public FunctionalDetector withFunction(
            final ToDoubleFunction<SpacecraftState> newGFunction) {
        return new FunctionalDetector(getMaxCheckInterval(), getThreshold(),
                getMaxIterationCount(), getHandler(), newGFunction);
    }

    /**
     * Create a new event detector with a new g function, keeping all other attributes the
     * same. It is recommended to use {@link #withMaxCheck(double)} and {@link
     * #withThreshold(double)} to set appropriate values for this g function.
     *
     * @param newGFunction the new g function.
     * @return a new detector with the new g function.
     * @deprecated Use {@link #withFunction(ToDoubleFunction)} instead. Will be removed in
     * next major release.
     */
    @Deprecated
    public FunctionalDetector withGFunction(final GFunction newGFunction) {
        return new FunctionalDetector(getMaxCheckInterval(), getThreshold(),
                getMaxIterationCount(), getHandler(), newGFunction);
    }

    /**
     * Get the switching function.
     *
     * @return the function used in {@link #g(SpacecraftState)}.
     * @deprecated use {@link #getFunction()} instead. Will be removed in next major
     * release.
     */
    @Deprecated
    public GFunction getGFunction() {
        return gFunction;
    }

    /**
     * Get the switching function.
     *
     * @return the function used in {@link #g(SpacecraftState)}.
     */
    public ToDoubleFunction<SpacecraftState> getFunction() {
        return function;
    }

    /**
     * A functional interface for the {@link #g(SpacecraftState)} function.
     *
     * @deprecated Use {@link ToDoubleFunction}<SpaceraftState> instead. Will be removed
     * in next major release.
     */
    @FunctionalInterface
    @Deprecated
    public interface GFunction {

        /**
         * Applies this function to the given argument.
         *
         * @param value the function argument
         * @return the function result
         * @deprecated Use {@link ToDoubleFunction#applyAsDouble(Object)} instead. Will be
         * removed in next major release.
         */
        @Deprecated
        double apply(SpacecraftState value);

    }

}
