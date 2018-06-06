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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * A detector that implements the {@link #g(SpacecraftState)} function using a
 * {@link GFunction lambda} that can be set using {@link #withGFunction(GFunction)}.
 *
 * <p>For example, to create a simple date detector use:
 *
 * <code><pre>
 * FunctionalDetector d = new FunctionalDetector()
 *     .withGFunction((s)-> s.getDate().durationFrom(triggerDate))
 *     .withMaxCheck(1e10);
 * </pre></code>
 *
 * @author Evan Ward
 * @since 9.2
 */
public class FunctionalDetector extends AbstractDetector<FunctionalDetector> {

    /** Serializable UID. */
    private static final long serialVersionUID = 20180525L;

    /** The g function. */
    private final GFunction gFunction;

    /**
     * Create an event detector with the default values. These are {@link
     * #DEFAULT_MAXCHECK}, {@link #DEFAULT_THRESHOLD}, {@link #DEFAULT_MAX_ITER}, {@link
     * ContinueOnEvent}, and a g function that is identically unity.
     */
    public FunctionalDetector() {
        this(DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                new ContinueOnEvent<>(), a -> 1.0);
    }

    /**
     * Private constructor.
     *
     * @param maxCheck  maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @param gFunction the switching function.
     */
    private FunctionalDetector(final double maxCheck,
                               final double threshold,
                               final int maxIter,
                               final EventHandler<? super FunctionalDetector> handler,
                               final GFunction gFunction) {
        super(maxCheck, threshold, maxIter, handler);
        this.gFunction = gFunction;
    }

    @Override
    public double g(final SpacecraftState s) throws OrekitException {
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
     * same. It is recommended to use {@link #withMaxCheck(double)} and
     * {@link #withThreshold(double)} to set appropriate values for this g function.
     *
     * @param newGFunction the new g function.
     * @return a new detector with the new g function.
     */
    public FunctionalDetector withGFunction(final GFunction newGFunction) {
        return new FunctionalDetector(getMaxCheckInterval(), getThreshold(),
                getMaxIterationCount(), getHandler(), newGFunction);
    }

    /**
     * Get the switching function.
     *
     * @return the function used in {@link #g(SpacecraftState)}.
     */
    public GFunction getGFunction() {
        return gFunction;
    }

    /** A functional interface for the {@link #g(SpacecraftState)} function. */
    @FunctionalInterface
    public interface GFunction {

        /**
         * Applies this function to the given argument.
         *
         * @param value the function argument
         * @return the function result
         * @throws OrekitException if one is thrown while computing the result.
         */
        double apply(SpacecraftState value) throws OrekitException;

    }

}
