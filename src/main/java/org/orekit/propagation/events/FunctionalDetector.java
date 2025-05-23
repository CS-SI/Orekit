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

import java.util.function.ToDoubleFunction;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.intervals.AdaptableInterval;

/**
 * A detector that implements the {@link #g(SpacecraftState)} function using a lambda that
 * can be set using {@link #withFunction(ToDoubleFunction)}.
 *
 * <p>For example, to create a simple date detector use:
 *
 * <pre>
 * FunctionalDetector d = new FunctionalDetector()
 *     .withGFunction((s) -&gt; s.getDate().durationFrom(triggerDate))
 *     .withMaxCheck(1e10);
 * </pre>
 *
 * @author Evan Ward
 * @since 9.2
 */
public class FunctionalDetector extends AbstractDetector<FunctionalDetector> {

    /** The g function. */
    private final ToDoubleFunction<SpacecraftState> function;

    /**
     * Create an event detector with the default values. These are {@link
     * #DEFAULT_MAX_CHECK}, {@link #DEFAULT_THRESHOLD}, {@link #DEFAULT_MAX_ITER}, {@link
     * ContinueOnEvent}, and a g function that is identically unity.
     */
    public FunctionalDetector() {
        this(EventDetectionSettings.getDefaultEventDetectionSettings(), new ContinueOnEvent(), value -> 1.0);
    }

    /**
     * Private constructor.
     *
     * @param detectionSettings event detection settings
     * @param handler   event handler to call at event occurrences
     * @param function  the switching function.
     * @since 13.0
     */
    protected FunctionalDetector(final EventDetectionSettings detectionSettings,
                                 final EventHandler handler,
                                 final ToDoubleFunction<SpacecraftState> function) {
        super(detectionSettings, handler);
        this.function = function;
    }


    @Override
    public double g(final SpacecraftState s) {
        return function.applyAsDouble(s);
    }

    @Override
    protected FunctionalDetector create(final EventDetectionSettings detectionSettings, final EventHandler newHandler) {

        return new FunctionalDetector(detectionSettings, newHandler, function);
    }

    /**
     * Create a new event detector with a new g function, keeping all other attributes the
     * same. It is recommended to use {@link #withMaxCheck(AdaptableInterval)} and {@link
     * #withThreshold(double)} to set appropriate values for this g function.
     *
     * @param newGFunction the new g function.
     * @return a new detector with the new g function.
     */
    public FunctionalDetector withFunction(final ToDoubleFunction<SpacecraftState> newGFunction) {
        return new FunctionalDetector(getDetectionSettings(), getHandler(), newGFunction);
    }

    /**
     * Get the switching function.
     *
     * @return the function used in {@link #g(SpacecraftState)}.
     */
    public ToDoubleFunction<SpacecraftState> getFunction() {
        return function;
    }

}
