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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParametersDriversProvider;
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
public interface DSSTForceModel extends ParametersDriversProvider {

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
     *  {@link AbstractGradientConverter#getParametersAtStateDate(FieldSpacecraftState, ParametersDriversProvider)}
     *  on gradient converter.
     *  @return a list of objects that will hold short period terms (the objects
     *  are also retained by the force model, which will update them during propagation)
     */
    <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                              PropagationType type, T[] parameters);

    /** Get total number of spans for all the parameters driver.
     * @return total number of span to be estimated
     * @since 12.0
     */
    default int getNbParametersDriversValue() {
        int totalSpan = 0;
        final List<ParameterDriver> allParameters = getParametersDrivers();
        for (ParameterDriver dragDriver : allParameters) {
            totalSpan += dragDriver.getNbOfValues();
        }
        return totalSpan;
    }

    /** Get force model parameters, return each span value for
     * each parameter driver. Different from {@link #getParameters(AbsoluteDate)}
     * which return the value of the parameter at a specific date (1 value
     * per parameter driver)
     * @return force model parameters
     * @since 12.0
     */
    default double[] getParametersAllValues() {

        final List<ParameterDriver> drivers = getParametersDrivers();
        final int nbParametersValues = getNbParametersDriversValue();
        final double[] parameters = new double[nbParametersValues];
        int paramIndex = 0;
        for (int i = 0; i < drivers.size(); ++i) {
            for (Span<Double> span = drivers.get(i).getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                parameters[paramIndex++] = span.getData();
            }

        }
        return parameters;
    }

    /** Get force model parameters, return each span value for
     * each parameter driver. Different from {@link #getParameters(Field, FieldAbsoluteDate)}
     * which return the value of the parameter at a specific date (1 value
     * per parameter driver)
     * @param <T> type of the elements
     * @param field field to which the elements belong
     * @return force model parameters
     * @since 12.0
     */
    default <T extends CalculusFieldElement<T>> T[] getParametersAllValues(final Field<T> field) {

        final List<ParameterDriver> drivers = getParametersDrivers();
        final int nbParametersValues = getNbParametersDriversValue();
        final T[] parameters = MathArrays.buildArray(field, nbParametersValues);
        int paramIndex = 0;
        for (int i = 0; i < drivers.size(); ++i) {
            for (Span<Double> span = drivers.get(i).getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                parameters[paramIndex++] = field.getZero().add(span.getData());
            }

        }
        return parameters;
    }

    /** Get force model parameters at specific date (1 value per parameter
     * driver. Different from {@link #getParametersAllValues()} which
     * returns all span values of all parameters.
     * @return force model parameters, will throw an exception if one
     * of the PDriver in the DSST force model have more than 1 value driven. In this
     * case (if one of the force PDriver has several values driven then the
     * {@link #getParameters(AbsoluteDate)} must be used.
     * @since 9.0
     */
    default double[] getParameters() {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get force model parameters at specific date (1 value per parameter
     * driver. Different from {@link #getParametersAllValues()} which
     * returns all span values of all parameters.
     * @param date date at which the parameters want to be known, can
     * be new AbsoluteDate() if all the parameters have no validity period
     * that is to say that they have only 1 estimated value over the all
     * interval.
     * @return force model parameters
     * @since 12.0
     */
    default double[] getParameters(AbsoluteDate date) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue(date);
        }
        return parameters;
    }

    /** Get force model parameters at specific date (1 value per parameter
     * driver. Different from {@link #getParametersAllValues(Field)} which
     * returns all span values of all parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return force model parameters, will throw an exception if one
     * of the PDriver in the DSST force model have more than 1 value driven. In this
     * case (if one of the force PDriver has several values driven then the
     * {@link #getParameters(Field, FieldAbsoluteDate)} must be used.
     * @since 9.0
     */
    default <T extends CalculusFieldElement<T>> T[] getParameters(final Field<T> field) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue());
        }
        return parameters;
    }

    /** Get force model parameters at specific date (1 value per parameter
     * driver. Different from {@link #getParametersAllValues(Field)} which
     * returns all span values of all parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @param date field date at which the parameters want to be known, can
     * be new AbsoluteDate() if all the parameters have no validity period
     * that is to say that they have only 1 estimated value over the all
     * interval.
     * @return force model parameters
     * @since 12.0
     */
    default <T extends CalculusFieldElement<T>> T[] getParameters(final Field<T> field, FieldAbsoluteDate<T> date) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue(date.toAbsoluteDate()));
        }
        return parameters;
    }

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
     *  {@link AbstractGradientConverter#getParametersAtStateDate(FieldSpacecraftState, ParametersDriversProvider)}
     *  on gradient converter.
     *  @return the mean element rates dai/dt
     */
    <T extends CalculusFieldElement<T>> T[] getMeanElementRate(FieldSpacecraftState<T> state,
                                                           FieldAuxiliaryElements<T> auxiliaryElements, T[] parameters);


    /** Get the discrete events related to the model.
     * @return array of events detectors or null if the model is not
     * related to any discrete events
     */
    default EventDetector[] getEventsDetectors() {
        // If force model does not have parameter Driver, an empty stream is given as results
        final ArrayList<AbsoluteDate> transitionDates = new ArrayList<>();
        for (ParameterDriver driver : getParametersDrivers()) {
            // Get the transitions' dates from the TimeSpanMap
            for (AbsoluteDate date : driver.getTransitionDates()) {
                transitionDates.add(date);
            }
        }
        // Either force model does not have any parameter driver or only contains parameter driver with only 1 span
        if (transitionDates.size() == 0) {
            return null;

        } else {
            transitionDates.sort(null);
            // Initialize the date detector
            final DateDetector datesDetector = new DateDetector(transitionDates.get(0)).
                    withMaxCheck(60.).
                    withHandler(( state, d, increasing) -> {
                        return Action.RESET_DERIVATIVES;
                    });
            // Add all transitions' dates to the date detector
            for (int i = 1; i < transitionDates.size(); i++) {
                datesDetector.addEventDate(transitionDates.get(i));
            }
            // Return the detector
            return (EventDetector[]) Stream.of(datesDetector).toArray();
        }
    }

    /** Get the discrete events related to the model.
     * @param <T> type of the elements
     * @param field field used by default
     * @return array of events detectors or null if the model is not
     * related to any discrete events
     */
    <T extends CalculusFieldElement<T>> FieldEventDetector<T>[] getFieldEventsDetectors(Field<T> field);

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
     *  {@link AbstractGradientConverter#getParameters(FieldSpacecraftState, ParametersDriversProvider)}
     *  on gradient converter. The extract parameter method
     *  {@link #extractParameters(CalculusFieldElement[], FieldAbsoluteDate)} is called in
     * the method to select the right parameter.
     * @param meanStates mean states information: date, kinematics, attitude
     */
    @SuppressWarnings("unchecked")
    <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(T[] parameters, FieldSpacecraftState<T>... meanStates);

}
