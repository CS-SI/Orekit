/* Contributed in the public domain.
 * Licensed to CS Group (CS) under one or more
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

import java.util.function.Function;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;

/**
 * A detector that implements the {@link #g(FieldSpacecraftState)} function using a lambda
 * that can be set using {@link #withFunction(Function)}.
 *
 * <p>For example, to create a simple date detector use:
 *
 * <pre>
 * FieldFunctionalDetector&lt;T&gt; d = new FieldFunctionalDetector&lt;&gt;(field)
 *     .withGFunction((s) -&gt; s.getDate().durationFrom(triggerDate))
 *     .withMaxCheck(field.getZero().add(1e10));
 * </pre>
 *
 * @param <T> the type of numbers this detector uses.
 * @author Evan Ward
 * @since 10.2
 */
public class FieldFunctionalDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldFunctionalDetector<T>, T> {

    /** The g function. */
    private final Function<FieldSpacecraftState<T>, T> function;

    /**
     * Create an event detector with the default values. These are {@link
     * #DEFAULT_MAXCHECK}, {@link #DEFAULT_THRESHOLD}, {@link #DEFAULT_MAX_ITER}, {@link
     * ContinueOnEvent}, and a g function that is identically unity.
     *
     * @param field on which this detector is defined.
     */
    public FieldFunctionalDetector(final Field<T> field) {
        this(s -> DEFAULT_MAXCHECK,
             field.getZero().add(DEFAULT_THRESHOLD),
             DEFAULT_MAX_ITER,
             new FieldContinueOnEvent<>(), value -> field.getOne());
    }

    /**
     * Private constructor.
     *
     * @param maxCheck  maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter   maximum number of iterations in the event time search
     * @param handler   event handler to call at event occurrences
     * @param function  the switching function.
     */
    protected FieldFunctionalDetector(
            final FieldAdaptableInterval<T> maxCheck,
            final T threshold,
            final int maxIter,
            final FieldEventHandler<T> handler,
            final Function<FieldSpacecraftState<T>, T> function) {
        super(maxCheck, threshold, maxIter, handler);
        this.function = function;
    }


    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return function.apply(s);
    }

    @Override
    protected FieldFunctionalDetector<T> create(
            final FieldAdaptableInterval<T> newMaxCheck,
            final T newThreshold,
            final int newMaxIter,
            final FieldEventHandler<T> newHandler) {

        return new FieldFunctionalDetector<>(newMaxCheck, newThreshold, newMaxIter,
                newHandler, function);
    }

    /**
     * Create a new event detector with a new g function, keeping all other attributes the
     * same. It is recommended to use {@link #withMaxCheck(FieldAdaptableInterval)} and {@link
     * #withThreshold(CalculusFieldElement)} to set appropriate values for this g function.
     *
     * @param newGFunction the new g function.
     * @return a new detector with the new g function.
     */
    public FieldFunctionalDetector<T> withFunction(
            final Function<FieldSpacecraftState<T>, T> newGFunction) {
        return new FieldFunctionalDetector<>(getMaxCheckInterval(), getThreshold(),
                getMaxIterationCount(), getHandler(), newGFunction);
    }

    /**
     * Get the switching function.
     *
     * @return the function used in {@link #g(FieldSpacecraftState)}.
     */
    public Function<FieldSpacecraftState<T>, T> getFunction() {
        return function;
    }

}
