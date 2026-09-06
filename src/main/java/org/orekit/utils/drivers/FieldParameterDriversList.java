/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.utils.drivers;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Class managing several {@link FieldParameterDriver parameter drivers},
 * taking care of duplicated names.
 * <p>
 * Once parameter drivers sharing the same name have been added to
 * an instance of this class, they are permanently bound together and
 * also bound to the {@link #getDrivers() delegating driver} that
 * manages them. This means that if drivers {@code d1}, {@code d2}...
 * {@code dn} are added to the list and both correspond to parameter
 * name "P", then {@link #getDrivers()} will return a list containing
 * a delegating driver {@code delegateD} for the same name "P".
 * Afterward, whenever either {@link FieldParameterDriver#setValue(CalculusFieldElement)}
 * or {@link FieldParameterDriver#setReferenceDate(FieldAbsoluteDate)} is called
 * on any of the {@code n+1} instances {@code d1}, {@code d2}... {@code dn}
 * or {@code delegateD}, the call will be automatically forwarded to the
 * {@code n} remaining instances, hence ensuring they remain consistent
 * with each other.
 * </p>
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 14.0
 */
public class FieldParameterDriversList<T extends CalculusFieldElement<T>> {

    /** Managed drivers. */
    private final List<FieldDelegatingDriver<T>> delegating;

    /** Creates an empty list. */
    public FieldParameterDriversList() {
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
     * @param driver driver to add
     */
    public void add(final FieldParameterDriver<T> driver) {

        final FieldDelegatingDriver<T> existingHere = findByName(driver.getName());
        final FieldDelegatingDriver<T> alreadyBound = getAssociatedFieldDelegatindDriver(driver);

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
                delegating.add(new FieldDelegatingDriver<>(this, driver));
            }
        }

    }

    /** Get a {@link FieldDelegatingDriver delegating driver} bound to a driver.
     * @param driver driver to check
     * @return a {@link FieldDelegatingDriver delegating driver} bound to a driver, or
     * null if this driver is not associated with any {@link FieldDelegatingDriver delegating driver}
     */
    private FieldDelegatingDriver<T> getAssociatedFieldDelegatindDriver(final FieldParameterDriver<T> driver) {
        for (final FieldParameterObserver<T> observer : driver.getObservers()) {
            if (observer instanceof FieldChangesForwarder<T> forwarder) {
                return forwarder.getFieldDelegatindDriver();
            }
        }
        return null;
    }

    /** Replace a {@link FieldDelegatingDriver delegating driver}.
     * @param oldDelegating delegating driver to replace
     * @param newDelegating new delegating driver to use
     */
    private void replaceDelegating(final FieldDelegatingDriver<T> oldDelegating,
                                   final FieldDelegatingDriver<T> newDelegating) {
        for (int i = 0; i < delegating.size(); ++i) {
            if (delegating.get(i) == oldDelegating) {
                delegating.set(i, newDelegating);
            }
        }
    }

    /** Find  a {@link FieldDelegatingDriver delegating driver} by name.
     * @param name name to check
     * @return a {@link FieldDelegatingDriver delegating driver} managing this parameter name
     */
    public FieldDelegatingDriver<T> findByName(final String name) {
        for (final FieldDelegatingDriver<T> d : delegating) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return null;
    }

    /** Sort the parameters lexicographically.
     */
    public void sort() {
        delegating.sort(Comparator.comparing(FieldParameterDriver::getName));
    }

    /** Filter parameters to keep only one type of selection status.
     * @param selected if true, only {@link ParameterDriver#isSelected()
     * selected} parameters will be kept, the other ones will be removed
     */
    public void filter(final boolean selected) {
        for (final Iterator<FieldDelegatingDriver<T>> iterator = delegating.iterator(); iterator.hasNext();) {
            final FieldDelegatingDriver<T> FieldDelegatindDriver = iterator.next();
            if (FieldDelegatindDriver.isSelected() != selected) {
                iterator.remove();
                FieldDelegatindDriver.removeOwner(this);
            }
        }
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
     * @return unmodifiable view of the list of delegating drivers
     */
    public List<FieldDelegatingDriver<T>> getDrivers() {
        return Collections.unmodifiableList(delegating);
    }

    /** Specialized driver delegating to several other managing
     * the same parameter name.
     * @param <T> type of the field elements
     */
    public static class FieldDelegatingDriver<T extends CalculusFieldElement<T>> extends FieldParameterDriver<T> {

        /** Lists owning this delegating driver. */
        private final List<FieldParameterDriversList<T>> owners;

        /** Observer for propagating changes between all drivers. */
        private FieldChangesForwarder<T> forwarder;

        /** Simple constructor.
         * @param owner list owning this delegating driver
         * @param driver first driver in the series
         */
        FieldDelegatingDriver(final FieldParameterDriversList<T> owner, final FieldParameterDriver<T> driver) {
            super(driver.getName(), driver.getReferenceValue(),
                  driver.getScale(), driver.getMinValue(), driver.getMaxValue(),
                  driver.getValidity());

            owners = new ArrayList<>();
            addOwner(owner);

            setValue(driver.getValue());
            setReferenceDate(driver.getReferenceDate());
            setSelected(driver.isSelected());

            // set up a change forwarder observing both the raw driver and the delegating driver
            this.forwarder = new FieldChangesForwarder<>(this, driver);
            addObserver(forwarder);
            driver.addObserver(forwarder);

        }

        /** Add an owner for this delegating driver.
         * @param owner owner to add
         */
        void addOwner(final FieldParameterDriversList<T> owner) {
            owners.add(owner);
        }

        /** Remove one owner of this driver.
         * @param owner owner to remove delegating driver from
         */
        private void removeOwner(final FieldParameterDriversList<T> owner) {
            owners.removeIf(parameterDriversList -> parameterDriversList == owner);
        }

        /** Add a driver.
         * <p>
         * Warning, by doing this operation all the delegated drivers present in the
         * parameterDriverList will be overwritten with the attributes of the driver
         * given in argument.
         * <p>
         * @param driver driver to add
         */
        private void add(final FieldParameterDriver<T> driver) {

            setValue(driver.getValue());
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
         * @param other instance to merge
         */
        private void merge(final FieldDelegatingDriver<T> other) {

            if (other.forwarder == forwarder) {
                // we are attempting to merge an instance with either itself
                // or an already embedded one, just ignore the request
                return;
            }

            // synchronize parameter
            setReferenceDate(other.getReferenceDate());
            if (isSelected()) {
                other.setSelected(true);
            } else {
                setSelected(other.isSelected());
            }

            // move around drivers
            for (final FieldParameterDriver<T> otherDriver : other.forwarder.getDrivers()) {
                // as drivers are added one at a time and always refer back to a single
                // FieldDelegatindDriver (through the FieldChangesForwarder), they cannot be
                // referenced by two different FieldDelegatindDriver. We can blindly move
                // around all drivers, there cannot be any duplicates
                forwarder.add(otherDriver);
                otherDriver.replaceObserver(other.forwarder, forwarder);
            }

            // forwarding is now delegated to current instance
            other.replaceObserver(other.forwarder, forwarder);
            other.forwarder = forwarder;

            // replace merged instance with current instance in former owners
            for (final FieldParameterDriversList<T> otherOwner : other.owners) {
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
        public List<FieldParameterDriver<T>> getRawDrivers() {
            return Collections.unmodifiableList(forwarder.getDrivers());
        }

    }

    /** Local observer for propagating changes, avoiding infinite recursion. */
    private static class FieldChangesForwarder<T extends CalculusFieldElement<T>> implements FieldParameterObserver<T> {

        /** FieldDelegatindDriver we are associated with. */
        private final FieldDelegatingDriver<T> delegating;

        /** Drivers synchronized together by the instance. */
        private final List<FieldParameterDriver<T>> drivers;

        /** Root of the current update chain. */
        private FieldParameterDriver<T> root;

        /** Depth of the current update chain. */
        private int depth;

        /** Simple constructor.
         * @param delegating FieldDelegatindDriver we are associated with
         * @param driver first driver in the series
         */
        FieldChangesForwarder(final FieldDelegatingDriver<T> delegating, final FieldParameterDriver<T> driver) {
            this.delegating = delegating;
            this.drivers    = new ArrayList<>();
            drivers.add(driver);
        }

        /** Get the {@link FieldDelegatingDriver} associated with this instance.
         * @return {@link FieldDelegatingDriver} associated with this instance
         */
        FieldDelegatingDriver<T> getFieldDelegatindDriver() {
            return delegating;
        }

        /** Add a driver to the list synchronized together by the instance.
         * @param driver driver to add
         */
        void add(final FieldParameterDriver<T> driver) {
            drivers.add(driver);
        }

        /** Get the drivers synchronized together by the instance.
         * @return drivers synchronized together by the instance.
         */
        public List<FieldParameterDriver<T>> getDrivers() {
            return drivers;
        }

        /** {@inheritDoc} */
        @Override
        public void valueChanged(final T previousValue, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setValue(driver.getValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void referenceDateChanged(final FieldAbsoluteDate<T> previousReferenceDate,
                                         final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setReferenceDate(driver.getReferenceDate()));
        }

        /** {@inheritDoc} */
        @Override
        public void nameChanged(final String previousName, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setName(driver.getName()));
        }

        /** {@inheritDoc} */
        @Override
        public void selectionChanged(final boolean previousSelection, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setSelected(driver.isSelected()));
        }

        /** {@inheritDoc} */
        @Override
        public void referenceValueChanged(final T previousReferenceValue, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setReferenceValue(driver.getReferenceValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void minValueChanged(final double previousMinValue, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setMinValue(driver.getMinValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void maxValueChanged(final double previousMaxValue, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setMaxValue(driver.getMaxValue()));
        }

        /** {@inheritDoc} */
        @Override
        public void scaleChanged(final double previousScale, final FieldParameterDriver<T> driver) {
            updateAll(driver, d -> d.setScale(driver.getScale()));
        }

        /** Update all bound parameters.
         * @param driver driver triggering the update
         * @param updater updater to use
         */
        private void updateAll(final FieldParameterDriver<T> driver, final FieldUpdater<T> updater) {

            final boolean firstCall = depth++ == 0;
            if (firstCall) {
                root = driver;
            }

            if (driver == getFieldDelegatindDriver()) {
                // propagate change downwards, which will trigger recursive calls
                for (final FieldParameterDriver<T> d : drivers) {
                    if (d != root) {
                        updater.update(d);
                    }
                }
            } else if (firstCall) {
                // first call started from an underlying driver, propagate change upwards
                updater.update(getFieldDelegatindDriver());
            }

            if (--depth == 0) {
                // this is the end of the root call
                root = null;
            }

        }

    }

    /** Interface for updating parameters. */
    @FunctionalInterface
    private interface FieldUpdater<T extends CalculusFieldElement<T>> {
        /** Update a driver.
         * @param driver driver to update
         */
        void update(FieldParameterDriver<T> driver);
    }

}
