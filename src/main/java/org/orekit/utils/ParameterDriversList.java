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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.errors.OrekitException;


/** Class managing several {@link ParameterDriver parameter drivers},
 * taking care of duplicated names.
 * @author Luc Maisonobe
 * @since 8.0
 */
public class ParameterDriversList {

    /** Managed drivers. */
    private final List<DelegatingDriver> delegating;

    /** Creates an empty list.
     */
    public ParameterDriversList() {
        this.delegating = new ArrayList<DelegatingDriver>();
    }

    /** Add a driver.
     * <p>
     * If the driver is already present, it will not be added.
     * If another driver managing the same parameter is present,
     * both drivers will be managed together, existing drivers
     * being set to the value of the last driver added (i.e.
     * each addition overrides the parameter value).
     * </p>
     * @param driver driver to add
     * @exception OrekitException if an existing driver for the
     * same parameter throws one when its value is reset using the
     * new driver value
     */
    public void add(final ParameterDriver driver) throws OrekitException {

        for (final DelegatingDriver d : delegating) {
            if (d.getName().equals(driver.getName())) {
                // the parameter is already managed by existing drivers

                for (final ParameterDriver existing : d.drivers) {
                    if (existing == driver) {
                        // the driver is already known, don't duplicate it
                        return;
                    }
                }

                // this is a new driver for an already managed parameter
                d.add(driver);
                return;

            }
        }

        // this is the first driver we have for this parameter name
        delegating.add(new DelegatingDriver(driver));

    }

    /** Get the number of parameters with different names.
     * @return number of parameters with different names
     */
    public int getNbParams() {
        return delegating.size();
    }

    /** Get delegating drivers for all parameters.
     * <p>
     * The delegating drivers are <em>not</em> the same as
     * the drivers added to the list, but they delegate to them.
     * </p>
     * <p>
     * All delegating drivers manage parameters with different names.
     * </p>
     * <p>
     * The list is in the same order as the calls to the
     * {@link #add(ParameterDriver) add} method, with parameters
     * names duplications removed
     * </p>
     * @return list of delegating drivers
     */
    public List<DelegatingDriver> getDrivers() {
        return delegating;
    }

    /** Specialized driver delegating to several other managing
     * the same parameter name.
     */
    public static class DelegatingDriver extends ParameterDriver {

        /** Drivers managing the same parameter. */
        private final List<ParameterDriver> drivers;

        /** Simple constructor.
         * @param driver first driver in the series
         * @exception OrekitException if first drivers throws one
         */
        DelegatingDriver(final ParameterDriver driver) throws OrekitException {
            super(driver.getName(), driver.getInitialValue(), driver.getScale());
            setValue(driver.getValue());
            setSelected(driver.isSelected());
            drivers = new ArrayList<ParameterDriver>();
            drivers.add(driver);
        }

        /** Add a driver.
         * @param driver driver to add
         * @exception OrekitException if an existing drivers cannot be set to the same value
         */
        private void add(final ParameterDriver driver)
            throws OrekitException {

            setValue(driver.getValue());

            // if any of the drivers is selected, all must be selected
            if (isSelected()) {
                driver.setSelected(true);
            } else {
                setSelected(driver.isSelected());
            }

            drivers.add(driver);

        }

        /** {@inheritDoc} */
        @Override
        protected void valueChanged(final double newValue)
            throws OrekitException {
            if (drivers != null) { // the drivers list will be null during construction
                for (final ParameterDriver driver : drivers) {
                    driver.setValue(newValue);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void setSelected(final boolean selected) {
            super.setSelected(selected);
            if (drivers != null) { // the drivers list will be null during construction
                for (final ParameterDriver driver : drivers) {
                    driver.setSelected(selected);
                }
            }
        }

        /** Get the raw drivers to which this one delegates.
         * <p>
         * These raw drivers all manage the same parameter name.
         * </p>
         * @return raw drivers to which this one delegates
         */
        public List<ParameterDriver> getRawDrivers() {
            return Collections.unmodifiableList(drivers);
        }

    }

}
