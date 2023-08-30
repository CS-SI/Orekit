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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectorsProvider;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeSpanMap.Span;

/** This interface represents a force modifying spacecraft motion for a {@link
 *  org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  Objects implementing this interface are intended to be added to a {@link
 *  org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSST propagator}
 *  before the propagation is started.
 *  </p>
 *  <p>
 *  The propagator will call at the very beginning of a propagation the {@link
 *  #initializeShortPeriodTerms(AuxiliaryElements, PropagationType, double[])} method allowing
 *  preliminary computation such as truncation if needed.
 *  </p>
 *  <p>
 *  Then the propagator will call at each step:
 *  <ol>
 *  <li>the {@link #getMeanElementRate(SpacecraftState, AuxiliaryElements, double[])} method.
 *  The force model instance will extract all the state data needed to compute
 *  the mean element rates that contribute to the mean state derivative.</li>
 *  <li>the {@link #updateShortPeriodTerms(double[], SpacecraftState...)} method,
 *  if osculating parameters are desired, on a sample of points within the
 *  last step.</li>
 *  </ol>
 *
 * @author Romain Di Constanzo
 * @author Pascal Parraud
 */
public interface DSSTForceModel extends ParameterDriversProvider, EventDetectorsProvider {

    /**
     * Initialize the force model at the start of propagation.
     * <p> The default implementation of this method does nothing.</p>
     *
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     * @since 11.0
     */
    default void init(SpacecraftState initialState, AbsoluteDate target) {
    }

    /**
     * Initialize the force model at the start of propagation.
     * <p> The default implementation of this method does nothing.</p>
     *
     * @param initialState spacecraft state at the start of propagation.
     * @param target       date of propagation. Not equal to {@code initialState.getDate()}.
     * @param <T> type of the elements
     * @since 11.1
     */
    default <T extends CalculusFieldElement<T>> void init(FieldSpacecraftState<T> initialState, FieldAbsoluteDate<T> target) {
        init(initialState.toSpacecraftState(), target.toAbsoluteDate());
    }

    /** {@inheritDoc}.*/
    @Override
    default Stream<EventDetector> getEventDetectors() {
        return getEventDetectors(getParametersDrivers());
    }

    /** {@inheritDoc}.*/
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return getFieldEventDetectors(field, getParametersDrivers());
    }

    /** Performs initialization prior to propagation for the current force model.
     *  <p>
     *  This method aims at being called at the very beginning of a propagation.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param type type of the elements used during the propagation
     *  @param parameters values of the force model parameters for specific date
     *  (1 value only per parameter driver) obtained for example by calling
     *  {@link #getParameters(AbsoluteDate)} on force model.
     *  @return a list of objects that will hold short period terms (the objects
     *  are also retained by the force model, which will update them during propagation)
     */
    List<ShortPeriodTerms> initializeShortPeriodTerms(AuxiliaryElements auxiliaryElements,
                                                      PropagationType type, double[] parameters);

    /** Performs initialization prior to propagation for the current force model.
     *  <p>
     *  This method aims at being called at the very beginning of a propagation.
     *  </p>
     *  @param <T> type of the elements
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param type type of the elements used during the propagation
     *  @param parameters values of the force model parameters for specific date
     *  (1 value only per parameter driver) obtained for example by calling
     *  {@link #getParameters(AbsoluteDate)} on force model or
     *  {@link AbstractGradientConverter#getParametersAtStateDate(FieldSpacecraftState, ParameterDriversProvider)}
     *  on gradient converter.
     *  @return a list of objects that will hold short period terms (the objects
     *  are also retained by the force model, which will update them during propagation)
     */
    <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                                  PropagationType type, T[] parameters);

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #updateShortPeriodTerms(double[], SpacecraftState...) updateShortPeriodTerms} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array containing all span values of all drivers
     * from which the parameter values at date date wants to be extracted
     * @param date the date
     * @return the parameters given the date
     */
    default double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final double[] outParameters = new double[allParameters.size()];
        int index = 0;
        int paramIndex = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final ParameterDriver driver = allParameters.get(i);
            final String driverNameforDate = driver.getNameSpan(date);
            // Loop on the spans
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                // Add all the parameter drivers of the span
                if (span.getData().equals(driverNameforDate)) {
                    outParameters[index++] = parameters[paramIndex];
                }
                paramIndex++;
            }
        }
        return outParameters;
    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #updateShortPeriodTerms(CalculusFieldElement[], FieldSpacecraftState...)
     * updateShortPeriodTerms} method. Parameters are filtered given an input date.
     * @param parameters the input parameters array containing all span values of all drivers
     * from which the parameter values at date date wants to be extracted
     * @param date the date
     * @param <T> extends CalculusFieldElement
     * @return the parameters given the date
     */
    default <T extends CalculusFieldElement<T>> T[] extractParameters(final T[] parameters,
                                                                      final FieldAbsoluteDate<T> date) {

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), allParameters.size());
        int index = 0;
        int paramIndex = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final ParameterDriver driver = allParameters.get(i);
            final String driverNameforDate = driver.getNameSpan(date.toAbsoluteDate());
            // Loop on the spans
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                // Add all the parameter drivers of the span
                if (span.getData().equals(driverNameforDate)) {
                    outParameters[index++] = parameters[paramIndex];
                }
                ++paramIndex;
            }
        }
        return outParameters;
    }


    /** Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     *  @param state current state information: date, kinematics, attitude
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters at state date (only 1 span for
     *  each parameter driver) obtained for example by calling {@link #getParameters(AbsoluteDate)}
     *  on force model.
     *  @return the mean element rates dai/dt
     */
    double[] getMeanElementRate(SpacecraftState state,
                                AuxiliaryElements auxiliaryElements, double[] parameters);

    /** Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     *  @param <T> type of the elements
     *  @param state current state information: date, kinematics, attitude
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters at state date (only 1 span for
     *  each parameter driver) obtained for example by calling {@link #getParameters(Field, FieldAbsoluteDate)}
     *  on force model  or
     *  {@link AbstractGradientConverter#getParametersAtStateDate(FieldSpacecraftState, ParameterDriversProvider)}
     *  on gradient converter.
     *  @return the mean element rates dai/dt
     */
    <T extends CalculusFieldElement<T>> T[] getMeanElementRate(FieldSpacecraftState<T> state,
                                                               FieldAuxiliaryElements<T> auxiliaryElements, T[] parameters);

    /** Register an attitude provider.
     * <p>
     * Register an attitude provider that can be used by the force model.
     * </p>
     * @param provider the {@link AttitudeProvider}
     */
    void registerAttitudeProvider(AttitudeProvider provider);

    /** Update the short period terms.
     * <p>
     * The {@link ShortPeriodTerms short period terms} that will be updated
     * are the ones that were returned during the call to {@link
     * #initializeShortPeriodTerms(AuxiliaryElements, PropagationType, double[])}.
     * </p>
     * @param parameters values of the force model parameters (all span values for each parameters)
     * obtained for example by calling
     * {@link #getParametersAllValues()}
     * on force model. The extract parameter method {@link #extractParameters(double[], AbsoluteDate)} is called in
     * the method to select the right parameter corresponding to the mean state date.
     * @param meanStates mean states information: date, kinematics, attitude
     */
    void updateShortPeriodTerms(double[] parameters, SpacecraftState... meanStates);

    /** Update the short period terms.
     * <p>
     * The {@link ShortPeriodTerms short period terms} that will be updated
     * are the ones that were returned during the call to {@link
     * #initializeShortPeriodTerms(AuxiliaryElements, PropagationType, double[])}.
     * </p>
     * @param <T> type of the elements
     * @param parameters values of the force model parameters (all span values for each parameters)
     * obtained for example by calling {@link #getParametersAllValues(Field)} on force model or
     *  {@link AbstractGradientConverter#getParameters(FieldSpacecraftState, ParameterDriversProvider)}
     *  on gradient converter. The extract parameter method
     *  {@link #extractParameters(CalculusFieldElement[], FieldAbsoluteDate)} is called in
     * the method to select the right parameter.
     * @param meanStates mean states information: date, kinematics, attitude
     */
    @SuppressWarnings("unchecked")
    <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(T[] parameters, FieldSpacecraftState<T>... meanStates);

}
