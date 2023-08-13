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
package org.orekit.estimation.measurements;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeSpanMap.Span;

/** Class holding an estimated theoretical value associated to an {@link ObservedMeasurement observed measurement}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 8.0
 */
public class EstimatedMeasurement<T extends ObservedMeasurement<T>> extends EstimatedMeasurementBase<T> {

    /** Partial derivatives with respect to states. */
    private double[][][] stateDerivatives;

    /** Partial derivatives with respect to parameters. */
    private final Map<ParameterDriver, TimeSpanMap<double[]>> parametersDerivatives;

    /** Simple constructor.
     * @param observedMeasurement associated observed measurement
     * @param iteration iteration number
     * @param count evaluations counter
     * @param states states of the spacecrafts
     * @param participants coordinates of the participants in signal travel order
     * in inertial frame
     */
    public EstimatedMeasurement(final T observedMeasurement,
                                final int iteration, final int count,
                                final SpacecraftState[] states,
                                final TimeStampedPVCoordinates[] participants) {
        super(observedMeasurement, iteration, count, states, participants);
        this.stateDerivatives      = new double[states.length][][];
        this.parametersDerivatives = new IdentityHashMap<ParameterDriver, TimeSpanMap<double[]>>();
    }

    /** Get state size.
     * <p>
     * Warning, the {@link #setStateDerivatives(int, double[][])}
     * method must have been called before this method is called.
     * </p>
     * @return state size
     * @since 10.1
     */
    public int getStateSize() {
        return stateDerivatives[0][0].length;
    }

    /** Get the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param index index of the state, according to the {@code states}
     * passed at construction
     * @return partial derivatives of the simulated value (array of size
     * {@link ObservedMeasurement#getDimension() dimension} x 6)
     */
    public double[][] getStateDerivatives(final int index) {
        final double[][] sd = new double[getObservedMeasurement().getDimension()][];
        for (int i = 0; i < getObservedMeasurement().getDimension(); ++i) {
            sd[i] = stateDerivatives[index][i].clone();
        }
        return sd;
    }

    /** Set the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to state Cartesian coordinates.
     * @param index index of the state, according to the {@code states}
     * passed at construction
     * @param derivatives partial derivatives with respect to state
     */
    public void setStateDerivatives(final int index, final double[]... derivatives) {
        this.stateDerivatives[index] = new double[getObservedMeasurement().getDimension()][];
        for (int i = 0; i < getObservedMeasurement().getDimension(); ++i) {
            this.stateDerivatives[index][i] = derivatives[i].clone();
        }
    }

    /** Get all the drivers with set derivatives.
     * @return all the drivers with set derivatives
     * @since 9.0
     */
    public Stream<ParameterDriver> getDerivativesDrivers() {
        return parametersDerivatives.entrySet().stream().map(entry -> entry.getKey());
    }

    /** Get the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to a parameter.
     * @param driver name of the span of the driver for the parameter for which
     * the derivative wants to be known.
     * @return partial derivatives of the simulated value
     * @exception OrekitIllegalArgumentException if parameter is unknown or
     * OrekitIllegalStateException if this function is used on a PDriver having several
     * values driven, in this case the method
     * {@link #getParameterDerivatives(ParameterDriver, AbsoluteDate)} must be called
     */
    public double[] getParameterDerivatives(final ParameterDriver driver)
        throws OrekitIllegalArgumentException {
        if (driver.getNbOfValues() == 1) {
            final TimeSpanMap<double[]> p = parametersDerivatives.get(driver);
            if (p == null) {
                final StringBuilder builder = new StringBuilder();
                for (final Map.Entry<ParameterDriver, TimeSpanMap<double[]>> entry : parametersDerivatives.entrySet()) {
                    if (builder.length() > 0) {
                        builder.append(",  ");
                    }
                    builder.append(entry.getKey());
                }
                throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                         driver,
                                                         builder.length() > 0 ? builder.toString() : " <none>");
            }
            return p.get(AbsoluteDate.ARBITRARY_EPOCH);
        } else {
            throw new OrekitIllegalStateException(OrekitMessages.PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES, driver.getName(), "getParameterDerivatives(driver, date)");
        }
    }

    /** Get the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to a parameter.
     * @param driver name of the span of the driver for the parameter for which
     * the derivative wants to be known.
     * @param date date at which the parameter derivatives wants to be known
     * @return partial derivatives of the simulated value
     * @exception OrekitIllegalArgumentException if parameter is unknown
     */
    public double[] getParameterDerivatives(final ParameterDriver driver, final AbsoluteDate date)
        throws OrekitIllegalArgumentException {
        final TimeSpanMap<double[]> p = parametersDerivatives.get(driver);
        if (p == null) {
            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<ParameterDriver, TimeSpanMap<double[]>> entry : parametersDerivatives.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(entry.getKey());
            }
            throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                     driver,
                                                     builder.length() > 0 ? builder.toString() : "<none>");
        }
        return p.get(date);
    }

    /** Set the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to parameter.
     * @param driver name of the span of the driver for the parameter for which
     * the derivative wants to be known.
     * @param date date at which the parameterDerivative wants to be set
     * @param parameterDerivatives partial derivatives with respect to parameter
     */
    public void setParameterDerivatives(final ParameterDriver driver, final AbsoluteDate date, final double... parameterDerivatives) {
        if (!parametersDerivatives.containsKey(driver) || parametersDerivatives.get(driver) == null) {
            final TimeSpanMap<double[]> derivativeSpanMap = new TimeSpanMap<double[]>(parameterDerivatives);
            final TimeSpanMap<String> driverNameSpan = driver.getNamesSpanMap();
            for (Span<String> span = driverNameSpan.getSpan(driverNameSpan.getFirstSpan().getEnd()); span != null; span = span.next()) {
                derivativeSpanMap.addValidAfter(parameterDerivatives, span.getStart(), false);
            }
            parametersDerivatives.put(driver, derivativeSpanMap);

        } else {

            AbsoluteDate dateToAddAfter = driver.getNamesSpanMap().getSpan(date).getStart();
            if (dateToAddAfter.equals(AbsoluteDate.PAST_INFINITY)) {
                dateToAddAfter = driver.getNamesSpanMap().getSpan(date).getEnd();
                parametersDerivatives.get(driver).addValidBefore(parameterDerivatives, dateToAddAfter, false);
            } else {
                parametersDerivatives.get(driver).addValidAfter(parameterDerivatives, dateToAddAfter, false);
            }

        }

    }

    /** Set the partial derivatives of the {@link #getEstimatedValue()
     * simulated measurement} with respect to parameter.
     * @param driver driver for the parameter
     * @param parameterDerivativesMap partial derivatives with respect to parameter
     */
    public void setParameterDerivatives(final ParameterDriver driver, final TimeSpanMap<double[]> parameterDerivativesMap) {
        parametersDerivatives.put(driver, parameterDerivativesMap);
    }

}
