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
package org.orekit.propagation.events.functions;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

import java.util.List;

/**
 * This class provides AND and OR operations for event functions. This class treats
 * positive values of the g function as true and negative values as false.
 *
 * @since 14.0
 * @author Evan Ward
 * @author Romain Serra
 */
public class BooleanEventFunction implements EventFunction {

    /** Original functions: the operands. */
    private final List<EventFunction> eventFunctions;

    /** The composition function. Should be associative for predictable behavior. */
    private final Operator operator;

    /** Flag for time dependence. */
    private final boolean flagTime;

    /** Flag for main variables dependence. */
    private final boolean flagMainVariables;

    /**
     * Protected constructor with all the parameters.
     *
     * @param eventFunctions    the operands.
     * @param operator     reduction operator to apply to value of the event function of the
     *                     operands.
     */
    private BooleanEventFunction(final List<EventFunction> eventFunctions, final Operator operator) {
        this.eventFunctions = eventFunctions;
        this.operator = operator;
        this.flagTime = eventFunctions.stream().allMatch(EventFunction::dependsOnTimeOnly);
        this.flagMainVariables = eventFunctions.stream().allMatch(EventFunction::dependsOnMainVariablesOnly);
    }

    /**
     * Builds an OR instance.
     * @param eventFunctions functions to reduce
     * @return reduced event function
     */
    public static BooleanEventFunction orCombine(final List<EventFunction> eventFunctions) {
        return new BooleanEventFunction(eventFunctions, Operator.OR);
    }

    /**
     * Builds an AND instance.
     * @param eventFunctions functions to reduce
     * @return reduced event function
     */
    public static BooleanEventFunction andCombine(final List<EventFunction> eventFunctions) {
        return new BooleanEventFunction(eventFunctions, Operator.AND);
    }

    @Override
    public double value(final SpacecraftState s) {
        // can't use stream/lambda here because g(s) throws a checked exception
        // so write out and combine the map and reduce loops
        double ret = Double.NaN; // return value
        boolean first = true;
        for (final EventFunction function : eventFunctions) {
            if (first) {
                ret = function.value(s);
                first = false;
            } else {
                ret = operator.combine(ret, function.value(s));
            }
        }
        // return the result of applying the operator to all operands
        return ret;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        // can't use stream/lambda here because g(s) throws a checked exception
        // so write out and combine the map and reduce loops
        T ret = fieldState.getDate().getField().getZero().newInstance(Double.NaN); // return value
        boolean first = true;
        for (final EventFunction function : eventFunctions) {
            if (first) {
                ret = function.value(fieldState);
                first = false;
            } else {
                ret = operator.combine(ret, function.value(fieldState));
            }
        }
        // return the result of applying the operator to all operands
        return ret;
    }

    @Override
    public boolean dependsOnTimeOnly() {
        return flagTime;
    }

    @Override
    public boolean dependsOnMainVariablesOnly() {
        return flagMainVariables;
    }

    /** Local class for operator. */
    private enum Operator {

        /** And operator. */
        AND() {
            double combine(final double g1, final double g2) {
                return FastMath.min(g1, g2);
            }

            <T extends CalculusFieldElement<T>> T combine(final T g1, final T g2) {
                return FastMath.min(g1, g2);
            }
        },

        /** Or operator. */
        OR() {
            double combine(final double g1, final double g2) {
                return FastMath.max(g1, g2);
            }

            <T extends CalculusFieldElement<T>> T combine(final T g1, final T g2) {
                return FastMath.max(g1, g2);
            }
        };

        /** Combine two functions evaluations.
         * @param g1 first evaluation
         * @param g2 second evaluation
         * @return combined evaluation
         */
        abstract double combine(double g1, double g2);

        /** Combine two functions evaluations.
         * @param g1 first evaluation
         * @param g2 second evaluation
         * @param <T> field type
         * @return combined evaluation
         */
        abstract <T extends CalculusFieldElement<T>> T combine(T g1, T g2);
    }

}
