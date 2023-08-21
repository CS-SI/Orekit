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
package org.orekit.utils;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.UnsupportedParameterException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap.Span;

/** Provider for {@link ParameterDriver parameters drivers}.
 * @author Luc Maisonobe
 * @author Melina Vanel
 * @author Maxime Journot
 * @since 11.2
 */
public interface ParameterDriversProvider {

    /** Get the drivers for parameters.
     * @return drivers for parameters
     */
    List<ParameterDriver> getParametersDrivers();

    /** Get total number of spans for all the parameters driver.
     * @return total number of span to be estimated
     * @since 12.0
     */
    default int getNbParametersDriversValue() {
        int totalSpan = 0;
        final List<ParameterDriver> allParameters = getParametersDrivers();
        for (ParameterDriver driver : allParameters) {
            totalSpan += driver.getNbOfValues();
        }
        return totalSpan;
    }

    /** Get model parameters.
     * @return model parameters, will throw an
     * exception if one PDriver has several values driven. If
     * it's the case (if at least 1 PDriver of the model has several values
     * driven) the method {@link #getParameters(AbsoluteDate)} must be used.
     * @since 12.0
     */
    default double[] getParameters() {

        final List<ParameterDriver> drivers = getParametersDrivers();
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue();
        }
        return parameters;
    }

    /** Get model parameters.
     * @param date date at which the parameters want to be known, can
     * be new AbsoluteDate() if all the parameters have no validity period
     * that is to say that they have only 1 estimated value over the all
     * interval
     * @return model parameters
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

    /** Get model parameters, return a list a all span values
     * of all parameters.
     * @return model parameters
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

    /** Get model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return model parameters
     * @since 9.0
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

    /** Get model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @return model parameters, will throw an
     * exception if one PDriver of the has several values driven. If
     * it's the case (if at least 1 PDriver of the model has several values
     * driven) the method {@link #getParameters(Field, FieldAbsoluteDate)} must be used.
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

    /** Get model parameters.
     * @param field field to which the elements belong
     * @param <T> type of the elements
     * @param date field date at which the parameters want to be known, can
     * be new AbsoluteDate() if all the parameters have no validity period.
     * @return model parameters
     * @since 9.0
     */
    default <T extends CalculusFieldElement<T>> T[] getParameters(final Field<T> field, final FieldAbsoluteDate<T> date) {
        final List<ParameterDriver> drivers = getParametersDrivers();
        final T[] parameters = MathArrays.buildArray(field, drivers.size());
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = field.getZero().add(drivers.get(i).getValue(date.toAbsoluteDate()));
        }
        return parameters;
    }

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @since 8.0
     */
    default ParameterDriver getParameterDriver(final String name) {

        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return driver;
            }
        }
        throw new UnsupportedParameterException(name, getParametersDrivers());
    }

    /** Check if a parameter is supported.
     * <p>Supported parameters are those listed by {@link #getParametersDrivers()}.</p>
     * @param name parameter name to check
     * @return true if the parameter is supported
     * @see #getParametersDrivers()
     * @since 8.0
     */
    default boolean isSupported(String name) {
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (name.equals(driver.getName())) {
                // we have found a parameter with that name
                return true;
            }
        }
        // the parameter is not supported
        return false;
    }
}
