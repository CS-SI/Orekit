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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap.Span;


/** Class managing several {@link ParameterDriver parameter drivers},
 * taking care of duplicated names.
 * <p>
 * Once parameter drivers sharing the same name have been added to
 * an instance of this class, they are permanently bound together and
 * also bound to the {@link #getDrivers() delegating driver} that
 * manages them. This means that if drivers {@code d1}, {@code d2}...
 * {@code dn} are added to the list and both correspond to parameter
 * name "P", then {@link #getDrivers()} will return a list containing
 * a delegating driver {@code delegateD} for the same name "P".
 * Afterwards, whenever either {@link ParameterDriver#setValue(double)}
 * or {@link ParameterDriver#setReferenceDate(AbsoluteDate)} is called
 * on any of the {@code n+1} instances {@code d1}, {@code d2}... {@code dn}
 * or {@code delegateD}, the call will be automatically forwarded to the
 * {@code n} remaining instances, hence ensuring they remain consistent
 * with each other.
 * </p>
 * @author Luc Maisonobe
 * @author Mélina Vanel
 * @since 8.0
 */
public class ParameterDriversList {

    /** Managed drivers. */
    private final List<DelegatingDriver> delegating;

    /** Creates an empty list.
     */
    public ParameterDriversList() {
        this.delegating = new ArrayList<>();
    }

    /** Add a driver.
     * <p>
     * If the driver is already present, it will not be added.
     * If another driver managing the same parameter is present,
     * both drivers will be managed together, existing drivers
     * being set to the value of the last driver added (i.e.
     * each addition overrides the parameter value).
     * </p>
     * <p>
     * Warning if a driver is added and a driver with the same name
     * was already added before, they should have the same validity
     * Period to avoid surprises. Whatever, all driver having
     * same name will have their valueSpanMap, nameSpanMap and validity period
     * overwritten with the last driver added attributes.
     * </p>
     * @param driver driver to add
     */
    public void add(final ParameterDriver driver) {

        final DelegatingDriver existingHere = findByName(driver.getName());
        final DelegatingDriver alreadyBound = getAssociatedDelegatingDriver(driver);

        if (existingHere != null) {
            if (alreadyBound != null) {
                // merge the two delegating drivers
                existingHere.merge(alreadyBound);
            } else {
                // this is a new driver for an already managed parameter
                existingHere.add(driver);
            }
        } else {
            if (alreadyBound != null) {
                // the driver is new here, but already bound to other drivers in other lists
                delegating.add(alreadyBound);
                alreadyBound.addOwner(this);
            } else {
                // this is the first driver we have for this parameter name
                delegating.add(new DelegatingDriver(this, driver));
            }
        }

    }

    /** Get a {@link DelegatingDriver delegating driver} bound to a driver.
     * @param driver driver to check
     * @return a {@link DelegatingDriver delegating driver} bound to a driver, or
     * null if this driver is not associated with any {@link DelegatingDriver delegating driver}
     * @since 9.1
     */
    private DelegatingDriver getAssociatedDelegatingDriver(final ParameterDriver driver) {
        for (final ParameterObserver observer : driver.getObservers()) {
            if (observer instanceof ChangesForwarder) {
                return ((ChangesForwarder) observer).getDelegatingDriver();
            }
        }
        return null;
    }

    /** Replace a {@link DelegatingDriver delegating driver}.
     * @param oldDelegating delegating driver to replace
     * @param newDelegating new delegating driver to use
     * @since 10.1
     */
    private void replaceDelegating(final DelegatingDriver oldDelegating, final DelegatingDriver newDelegating) {
        for (int i = 0; i < delegating.size(); ++i) {
            if (delegating.get(i) == oldDelegating) {
                delegating.set(i, newDelegating);
            }
        }
    }

    /** Find  a {@link DelegatingDriver delegating driver} by name.
     * @param name name to check
     * @return a {@link DelegatingDriver delegating driver} managing this parameter name
     * @since 9.1
     */
    public DelegatingDriver findByName(final String name) {
        for (final DelegatingDriver d : delegating) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return null;
    }

    /** Find  a {@link DelegatingDriver delegating driver} by name.
     * @param name name to check
     * @return a {@link DelegatingDriver delegating driver} managing this parameter name
     * @since 9.1
     */
    public String findDelegatingSpanNameBySpanName(final String name) {
        for (final DelegatingDriver d : delegating) {
            for (Span<String> span = d.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                if (span.getData().equals(name)) {
                    return span.getData();
                }
            }
        }
        return null;
    }


    /** Sort the parameters lexicographically.
     */
    public void sort() {
        Collections.sort(delegating, new Comparator<DelegatingDriver>() {
            /** {@inheritDoc} */
            @Override
            public int compare(final DelegatingDriver d1, final DelegatingDriver d2) {
                return d1.getName().compareTo(d2.getName());
            }
        });
    }

    /** Filter parameters to keep only one type of selection status.
     * @param selected if true, only {@link ParameterDriver#isSelected()
     * selected} parameters will be kept, the other ones will be removed
     */
    public void filter(final boolean selected) {
        for (final Iterator<DelegatingDriver> iterator = delegating.iterator(); iterator.hasNext();) {
            final DelegatingDriver delegatingDriver = iterator.next();
            if (delegatingDriver.isSelected() != selected) {
                iterator.remove();
                delegatingDriver.removeOwner(this);
            }
        }
    }

    /** Get the number of parameters with different names.
     * @return number of parameters with different names
     */
    public int getNbParams() {
        return delegating.size();
    }

    /** Get the number of values to estimate for parameters with different names.
     * @return number of values to estimate for parameters with different names
     */
    public int getNbValuesToEstimate() {
        int nbValuesToEstimate = 0;
        for (DelegatingDriver driver : delegating) {
            nbValuesToEstimate += driver.getNbOfValues();
        }
        return nbValuesToEstimate;
    }

    /** Get delegating drivers for all parameters.
     * <p>
     * The delegating drivers are <em>not</em> the same as
     * the drivers added to the list, but they delegate to them.
     * </p>
     * <p>
     * All delegating drivers manage parameters with different names.
     * </p>
     * @return unmodifiable view of the list of delegating drivers
     */
    public List<DelegatingDriver> getDrivers() {
        return Collections.unmodifiableList(delegating);
    }

    /** Specialized driver delegating to several other managing
     * the same parameter name.
     */
    public static class DelegatingDriver extends ParameterDriver {

        /** Lists owning this delegating driver. */
        private final List<ParameterDriversList> owners;

        /** Observer for propagating changes between all drivers. */
        private ChangesForwarder forwarder;

        /** Simple constructor.
         * @param owner list owning this delegating driver
         * @param driver first driver in the series
         */
        DelegatingDriver(final ParameterDriversList owner, final ParameterDriver driver) {
            super(driver.getName(), driver.getNamesSpanMap(),
                  driver.getValueSpanMap(), driver.getReferenceValue(),
                  driver.getScale(), driver.getMinValue(), driver.getMaxValue());

            owners = new ArrayList<>();
            addOwner(owner);

            setValueSpanMap(driver);
            setReferenceDate(driver.getReferenceDate());
            setSelected(driver.isSelected());

            // set up a change forwarder observing both the raw driver and the delegating driver
            this.forwarder = new ChangesForwarder(this, driver);
            addObserver(forwarder);
            driver.addObserver(forwarder);

        }

        /** Add an owner for this delegating driver.
         * @param owner owner to add
         */
        void addOwner(final ParameterDriversList owner) {
            owners.add(owner);
        }

        /** Remove one owner of this driver.
         * @param owner owner to remove delegating driver from
         * @since 10.1
         */
        private void removeOwner(final ParameterDriversList owner) {
            for (final Iterator<ParameterDriversList> iterator = owners.iterator(); iterator.hasNext();) {
                if (iterator.next() == owner) {
                    iterator.remove();
                }
            }
        }

        /** Add a driver. Warning, by doing this operation
         * all the delegated drivers present in the parameterDriverList
         * will be overwritten with the attributes of the driver given
         * in argument.
         * <p>
         * </p>
         * Warning if a driver is added and a driver with the same name
         * was already added before, they should have the same validity
         * Period (that is to say that the {@link ParameterDriver#setPeriods}
         * method should have been called with same arguments for all drivers
         * having the same name) to avoid surprises. Whatever, all driver having
         * same name will have their valueSpanMap, nameSpanMap and validity period
         * overwritten with the last driver added attributes.
         * @param driver driver to add
         */
        private void add(final ParameterDriver driver) {

            setValueSpanMap(driver);
            setReferenceDate(driver.getReferenceDate());

            // if any of the drivers is selected, all must be selected
            if (isSelected()) {
                driver.setSelected(true);
            } else {
                setSelected(driver.isSelected());
            }

            driver.addObserver(forwarder);
            forwarder.add(driver);

        }

        /** Merge another instance.
         * <p>
         * After merging, the other instance is merely empty and preserved
         * only as a child of the current instance. Changes are therefore
         * still forwarded to it, but it is itself not responsible anymore
         * for forwarding change.
         * <p>
         * </p>
         * Warning if a driver is added and a driver with the same name
         * was already added before, they should have the same validity
         * Period (that is to say that the {@link ParameterDriver#setPeriods}
         * method should have been called with same arguments for all drivers
         * having the same name) to avoid surprises. Whatever, all driver having
         * same name will have their valueSpanMap, nameSpanMap and validity period
         * overwritten with the last driver added attributes.
         * </p>
         * @param other instance to merge
         */
        private void merge(final DelegatingDriver other) {

            if (other.forwarder == forwarder) {
                // we are attempting to merge an instance with either itself
                // or an already embedded one, just ignore the request
                return;
            }

            // synchronize parameter
            setValueSpanMap(other);
            //setValue(other.getValue());
            setReferenceDate(other.getReferenceDate());
            if (isSelected()) {
                other.setSelected(true);
            } else {
                setSelected(other.isSelected());
            }

            // move around drivers
            for (final ParameterDriver otherDriver : other.forwarder.getDrivers()) {
                // as drivers are added one at a time and always refer back to a single
                // DelegatingDriver (through the ChangesForwarder), they cannot be
                // referenced by two different DelegatingDriver. We can blindly move
                // around all drivers, there cannot be any duplicates
                forwarder.add(otherDriver);
                otherDriver.replaceObserver(other.forwarder, forwarder);
            }

            // forwarding is now delegated to current instance
            other.replaceObserver(other.forwarder, forwarder);
            other.forwarder = forwarder;

            // replace merged instance with current instance in former owners
            for (final ParameterDriversList otherOwner : other.owners) {
                owners.add(otherOwner);
                otherOwner.replaceDelegating(other, this);
            }

        }

        /** Get the raw drivers to which this one delegates.
         * <p>
         * These raw drivers all manage the same parameter name.
         * </p>
         * @return raw drivers to which this one delegates
         */
        public List<ParameterDriver> getRawDrivers() {
            return Collections.unmodifiableList(forwarder.getDrivers());
        }

    }

    /** Local observer for propagating changes, avoiding infinite recursion. */
    private static class ChangesForwarder implements ParameterObserver {

        /** DelegatingDriver we are associated with. */
        private final DelegatingDriver delegating;

        /** Drivers synchronized together by the instance. */
        private final List<ParameterDriver> drivers;

        /** Root of the current update chain. */
        private ParameterDriver root;

        /** Depth of the current update chain. */
        private int depth;

        /** Simple constructor.
         * @param delegating delegatingDriver we are associated with
         * @param driver first driver in the series
         */
        ChangesForwarder(final DelegatingDriver delegating, final ParameterDriver driver) {
            this.delegating = delegating;
            this.drivers    = new ArrayList<>();
            drivers.add(driver);
        }

        /** Get the {@link DelegatingDriver} associated with this instance.
         * @return {@link DelegatingDriver} associated with this instance
         * @since 9.1
         */
        DelegatingDriver getDelegatingDriver() {
            return delegating;
        }

        /** Add a driver to the list synchronized together by the instance.
         * @param driver driver to add
         * @since 10.1
         */
        void add(final ParameterDriver driver) {
            drivers.add(driver);
        }

        /** Get the drivers synchronized together by the instance.
         * @return drivers synchronized together by the instance.
         * @since 10.1
         */
        public List<ParameterDriver> getDrivers() {
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap, final ParameterDriver driver) {
            updateAll(driver, d -> d.setValueSpanMap(driver));
        }

        /** {@inheritDoc} */
        @Override
        public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
            updateAll(driver, d -> d.setValue(driver.getValue(date), date));
        }

        /** {@inheritDoc} */
        @Override
        public void referenceDateChanged(final AbsoluteDate previousReferenceDate, final ParameterDriver driver) {
            updateAll(driver, d -> d.setReferenceDate(driver.getReferenceDate()));
        }

        /** {@inheritDoc} */
        @Override
        public void nameChanged(final String previousName, final ParameterDriver driver) {
            updateAll(driver, d -> d.setName(driver.getName()));
        }

        /** {@inheritDoc} */
        @Override
        public void selectionChanged(final boolean previousSelection, final ParameterDriver driver) {
            updateAll(driver, d -> d.setSelected(driver.isSelected()));
        }

        /** {@inheritDoc} */
        @Override
        public void estimationTypeChanged(final boolean previousSelection, final ParameterDriver driver) {
            updateAll(driver, d -> d.setContinuousEstimation(driver.isContinuousEstimation()));
        }

        /** {@inheritDoc} */
        @Override
        public void referenceValueChanged(final double previousReferenceValue, final ParameterDriver driver) {
            updateAll(driver, d -> d.setReferenceValue(driver.getReferenceValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void minValueChanged(final double previousMinValue, final ParameterDriver driver) {
            updateAll(driver, d -> d.setMinValue(driver.getMinValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void maxValueChanged(final double previousMaxValue, final ParameterDriver driver) {
            updateAll(driver, d -> d.setMaxValue(driver.getMaxValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void scaleChanged(final double previousScale, final ParameterDriver driver) {
            updateAll(driver, d -> d.setScale(driver.getScale()));
        }

        /** Update all bound parameters.
         * @param driver driver triggering the update
         * @param updater updater to use
         */
        private void updateAll(final ParameterDriver driver, final Updater updater) {

            final boolean firstCall = depth++ == 0;
            if (firstCall) {
                root = driver;
            }

            if (driver == getDelegatingDriver()) {
                // propagate change downwards, which will trigger recursive calls
                for (final ParameterDriver d : drivers) {
                    if (d != root) {
                        updater.update(d);
                    }
                }
            } else if (firstCall) {
                // first call started from an underlying driver, propagate change upwards
                updater.update(getDelegatingDriver());
            }

            if (--depth == 0) {
                // this is the end of the root call
                root = null;
            }

        }

    }

    /** Interface for updating parameters. */
    @FunctionalInterface
    private interface Updater {
        /** Update a driver.
         * @param driver driver to update
         */
        void update(ParameterDriver driver);
    }

}
