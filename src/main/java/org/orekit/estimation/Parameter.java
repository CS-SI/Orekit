/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.estimation;

import org.orekit.errors.OrekitException;


/** Class representing a single parameter that can be estimated.
 * @author Luc Maisonobe
 * @since 7.1
 */
public abstract class Parameter {

    /** Name of the parameter. */
    private final String name;

    /** Current value. */
    private double[] value;

    /** Estimated/fixed status. */
    private boolean estimated;

    /** Simple constructor.
     * <p>
     * At construction, the parameter is configured as <em>not</em> estimated
     * and it is <em>not</em> initialized.
     * </p>
     * @param name name of the parameter
     */
    protected Parameter(final String name) {
        this.name      = name;
        this.estimated = false;
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
        return value.length;
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
    public void setValue(final double ... value) throws OrekitException {
        System.arraycopy(value, 0, this.value, 0, value.length);
        valueChanged(this.value);
    }

    /** Notify that the values has been changed.
     * @param newValue new value
     * @exception OrekitException if value is invalid
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

}
