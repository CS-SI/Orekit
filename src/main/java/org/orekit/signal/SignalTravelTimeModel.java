/* Copyright 2022-2026 Romain Serra
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
package org.orekit.signal;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.optim.FieldScalarConvergenceCheckerProvider;
import org.hipparchus.util.FastMath;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Full model for signal travel time in vacuum (adjustable receiver/emitter with fixed emission/reception),
 * compatible with Field.
 * @since 14.0
 * @author Romain Serra
 */
public class SignalTravelTimeModel {

    /** Convergence checker for standard values. */
    private final ConvergenceChecker<Double> convergenceChecker;

    /** Convergence checker provider for Field values. */
    private final FieldScalarConvergenceCheckerProvider fieldConvergenceCheckerProvider;

    /**
     * Constructor.
     * @param convergenceChecker convergence settings for standard values
     * @param fieldConvergenceCheckerProvider convergence settings for Field values
     */
    public SignalTravelTimeModel(final ConvergenceChecker<Double> convergenceChecker,
                                 final FieldScalarConvergenceCheckerProvider fieldConvergenceCheckerProvider) {
        this.convergenceChecker = convergenceChecker;
        this.fieldConvergenceCheckerProvider = fieldConvergenceCheckerProvider;
    }

    /**
     * Constructor.
     * @param convergenceChecker convergence settings for standard values
     */
    public SignalTravelTimeModel(final ConvergenceChecker<Double> convergenceChecker) {
        this(convergenceChecker, new FieldScalarConvergenceCheckerProvider() {
            @Override
            public <T extends CalculusFieldElement<T>> ConvergenceChecker<T> getChecker(final Field<T> field) {
                return (iteration, previous, current) -> iteration != 0 && (iteration > AbstractSignalTravelTime.DEFAULT_MAX_ITER ||
                        (previous.subtract(current)).norm() <= 2 * FastMath.ulp(current).getReal());
            }
        });
    }

    /**
     * Constructor.
     */
    public SignalTravelTimeModel() {
        this(AbstractSignalTravelTime.getDefaultConvergenceChecker());
    }

    /**
     * Getter for the convergence checker.
     * @return checker
     */
    public ConvergenceChecker<Double> getConvergenceChecker() {
        return convergenceChecker;
    }

    /**
     * Getter for the Field convergence checker provider.
     * @return provider
     */
    public FieldScalarConvergenceCheckerProvider getFieldConvergenceCheckerProvider() {
        return fieldConvergenceCheckerProvider;
    }

    /**
     * Method returning a model assuming an iteration of the fixed point algorithm has already been performed.
     * @return warmed-up signal model
     */
    public SignalTravelTimeModel getWarmedUpModel() {
        return new SignalTravelTimeModel((iteration, previous, current) -> convergenceChecker.converged(iteration + 1, previous, current),
                new FieldScalarConvergenceCheckerProvider() {
                    @Override
                    public <T extends CalculusFieldElement<T>> ConvergenceChecker<T> getChecker(final Field<T> field) {
                        return (iteration, previous, current) -> fieldConvergenceCheckerProvider.getChecker(field)
                                .converged(iteration + 1, previous, current);
                    }
                });
    }

    /**
     * Method constructing a delay computer with input emitter.
     * @param emitter signal emitter
     * @return (positive) time delay
     */
    public AdjustableEmitterSignalTimer getAdjustableEmitterComputer(final PVCoordinatesProvider emitter) {
        return new AdjustableEmitterSignalTimer(emitter, convergenceChecker);
    }

    /**
     * Method constructing a delay computer with input receiver.
     * @param receiver signal emitter
     * @return (positive) time delay
     */
    public AdjustableReceiverSignalTimer getAdjustableReceiverComputer(final PVCoordinatesProvider receiver) {
        return new AdjustableReceiverSignalTimer(receiver, convergenceChecker);
    }

    /**
     * Method constructing a delay computer with input emitter.
     * @param <T> field type
     * @param field field
     * @param emitter signal emitter
     * @return (positive) time delay
     */
    public <T extends CalculusFieldElement<T>> FieldAdjustableEmitterSignalTimer<T> getFieldAdjustableEmitterComputer(final Field<T> field,
                                                                                                                      final FieldPVCoordinatesProvider<T> emitter) {
        return new FieldAdjustableEmitterSignalTimer<>(emitter, fieldConvergenceCheckerProvider.getChecker(field));
    }

    /**
     * Method constructing a delay computer with input receiver.
     * @param <T> field type
     * @param field field
     * @param receiver signal receiver
     * @return (positive) time delay
     */
    public <T extends CalculusFieldElement<T>> FieldAdjustableReceiverSignalTimer<T> getFieldAdjustableReceiverComputer(final Field<T> field,
                                                                                                                        final FieldPVCoordinatesProvider<T> receiver) {
        return new FieldAdjustableReceiverSignalTimer<>(receiver, fieldConvergenceCheckerProvider.getChecker(field));
    }
}
