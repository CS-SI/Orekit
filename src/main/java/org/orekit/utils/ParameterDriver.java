/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.utils;

import java.util.Collection;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Class allowing to drive the value of a parameter.
 * <p>
 * This class is typically used as a bridge between an estimation
 * algorithm (typically orbit determination or optimizer) and an
 * internal parameter in a physical model that needs to be tuned.
 * The physical model will expose to the estimation algorithm a
 * set of instances of this class so the algorithm can call the
 * {@link #setValue(double[]) setValue} method to update the
 * parameter value. Each time the calue is set, the physical model
 * will be notified as it will implement a specialized version of
 * the {@link #valueChanged(double[]) valueChanged} method.
 * </p>
 * <p>
 * This design has two major goals. First, it allows an estimation
 * algorithm to drive parameters almost anonymously, as it only
 * needs to get a list of instances of this class, without knowing
 * what they really drive. Second, it allows the physical model to
 * not expose directly setters methods for its parameters. In order
 * to be able to modify the parameter value, the algorithm
 * <em>must</em> retrieve a parameter driver.
 * </p>
 * @author Luc Maisonobe
 * @since 7.2
 */
public abstract class ParameterDriver {

    /** Name of the parameter. */
    private final String name;

    /** Initial value. */
    private double[] initialValue;

    /** Current value. */
    private double[] value;

    /** Estimated/fixed status. */
    private boolean estimated;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> estimated.
     * </p>
     * @param name name of the parameter
     * @param initialValue initial value of the parameter
     * @exception OrekitException if value is invalid for the driven model
     */
    protected ParameterDriver(final String name, final double[] initialValue)
        throws OrekitException {
        this.name         = name;
        this.initialValue = initialValue.clone();
        this.value        = initialValue.clone();
        this.estimated    = false;

        // ensure the physical model known about the initial value
        valueChanged(this.value);

    }

    /** Get name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /** Get the dimension of the parameter.
     * @return dimension of the parameter
     */
    public int getDimension() {
        return initialValue.length;
    }

    /** Get initial parameter value.
     * @return initial parameter value
     */
    public double[] getInitialValue() {
        return initialValue.clone();
    }

    /** Get current parameter value.
     * @return current parameter value
     */
    public double[] getValue() {
        return value.clone();
    }

    /** Set parameter value.
     * @param value new value
     * @exception OrekitException if value is invalid
     */
    public void setValue(final double[] value) throws OrekitException {
        System.arraycopy(value, 0, this.value, 0, value.length);
        valueChanged(this.value);
    }

    /** Notify that the values has been changed.
     * @param newValue new value
     * @exception OrekitException if value is invalid for the driven model
     */
    protected abstract void valueChanged(final double[] newValue) throws OrekitException;

    /** Configure a parameter estimation status.
     * @param estimated if true the parameter will be estimated,
     * otherwise it will be fixed
     */
    public void setEstimated(final boolean estimated) {
        this.estimated = estimated;
    }

    /** Check if parameter is estimated.
     * @return true if parameter is estimated, false if it is
     * fixed
     */
    public boolean isEstimated() {
        return estimated;
    }

    /** Add instance to collection if it does not conflict.
     * <p>
     * This methods allows to safely add a parameter driver to a collection.
     * There are three possible cases:
     * <ul>
     *   <li>if the parameter is already in the collection, the collection is left untouched</li>
     *   <li>if the parameter is not in the collection but a <em>different</em> parameter
     *       with the same name is present, an exception is triggered</li>
     *   <li>otherwise the parameter is added to the collection</li>
     * </ul>
     * @param collection collection to add the instance to
     * @exception OrekitException if a parameter with the same name already exists
     * </p>
     */
    public void checkAndAddSelf(final Collection<ParameterDriver> collection)
        throws OrekitException {

        // compare against existing parameters
        for (final ParameterDriver existing : collection) {
            if (existing.getName().equals(getName())) {
                if (existing == this) {
                    // the parameter was already known
                    return;
                } else {
                    // we have two different parameters sharing the same name
                    throw new OrekitException(OrekitMessages.DUPLICATED_PARAMETER_NAME,
                                              getName());
                }
            }
        }

        // no conflicts found
        collection.add(this);

    }

}
