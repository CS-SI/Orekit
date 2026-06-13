/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.conversion;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitalParameterFactory;
import org.orekit.orbits.OrbitalParameters;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.ArrayList;
import java.util.List;

/** Base class for propagator builders.
 * @param <T> type of the propagator
 * @author Pascal Parraud
 * @since 7.1
 */
public abstract class AbstractPropagatorBuilder<T extends AbstractPropagator> implements PropagatorBuilder {

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Factory for initial orbit.
     * @since 14.0
     */
    private final OrbitalParameterFactory<OrbitalParameters> factory;

    /** Initial mass. */
    private double mass;

    /** List of the supported parameters. */
    private final ParameterDriversList propagationDrivers;

    /** Attitude provider for the propagator. */
    private AttitudeProvider attitudeProvider;

    /** Additional derivatives providers.
     * @since 11.1
     */
    private final List<AdditionalDerivativesProvider> additionalDerivativesProviders;

    /** Build a new instance.
     * <p>
     * By default, all the orbital parameters drivers
     * are selected, which means that if the builder is used for orbit determination or
     * propagator conversion, all orbital parameters will be estimated. If only a subset
     * of the orbital parameters must be estimated, caller must retrieve the orbital
     * parameters by calling {@link #getOrbitalParameterFactory()}.{@link OrbitalParameterFactory#getDrivers()}
     * and then call {@link ParameterDriver#setSelected(boolean) setSelected(false)}.
     * </p>
     * @param factory factory for initial orbit
     * @param addDriverForCentralAttraction if true, a {@link ParameterDriver} should
     * be set up for central attraction coefficient
     * @since 14.0
     */
    protected AbstractPropagatorBuilder(final OrbitalParameterFactory<OrbitalParameters> factory,
                                        final boolean addDriverForCentralAttraction) {
        this(factory, addDriverForCentralAttraction,
             new FrameAlignedProvider(factory.getFrame()), Propagator.DEFAULT_MASS);
    }
    /** Build a new instance.
     * <p>
     * By default, all the orbital parameters drivers
     * are selected, which means that if the builder is used for orbit determination or
     * propagator conversion, all orbital parameters will be estimated. If only a subset
     * of the orbital parameters must be estimated, caller must retrieve the orbital
     * parameters by calling {@link #getOrbitalParameterFactory()}.{@link OrbitalParameterFactory#getDrivers()}
     * and then call {@link ParameterDriver#setSelected(boolean) setSelected(false)}.
     * </p>
     * @param factory factory for initial orbit
     * @param addDriverForCentralAttraction if true, a {@link ParameterDriver} should
     * be set up for central attraction coefficient
     * @param attitudeProvider for the propagator.
     * @since 14.0
     */
    protected AbstractPropagatorBuilder(final OrbitalParameterFactory<OrbitalParameters> factory,
                                        final boolean addDriverForCentralAttraction,
                                        final AttitudeProvider attitudeProvider) {
        this(factory, addDriverForCentralAttraction, attitudeProvider,
                Propagator.DEFAULT_MASS);
    }

    /** Build a new instance.
     * <p>
     * By default, all the orbital parameters drivers
     * are selected, which means that if the builder is used for orbit determination or
     * propagator conversion, all orbital parameters will be estimated. If only a subset
     * of the orbital parameters must be estimated, caller must retrieve the orbital
     * parameters by calling {@link #getOrbitalParameterFactory()}.{@link OrbitalParameterFactory#getDrivers()}
     * and then call {@link ParameterDriver#setSelected(boolean) setSelected(false)}.
     * </p>
     * @param factory factory for initial orbit
     * @param addDriverForCentralAttraction if true, a {@link ParameterDriver} should
     * be set up for central attraction coefficient
     * @param attitudeProvider for the propagator.
     * @param initialMass mass
     * @since 14.0
     */
    protected AbstractPropagatorBuilder(final OrbitalParameterFactory<OrbitalParameters> factory,
                                        final boolean addDriverForCentralAttraction,
                                        final AttitudeProvider attitudeProvider, final double initialMass) {

        this.factory             = factory;
        this.propagationDrivers  = new ParameterDriversList();
        this.attitudeProvider    = attitudeProvider;
        this.mass                = initialMass;
        for (final DelegatingDriver driver : factory.getDrivers().getDrivers()) {
            driver.setSelected(true);
        }

        this.additionalDerivativesProviders = new ArrayList<>();

        if (addDriverForCentralAttraction) {
            final ParameterDriver muDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                                 factory.getMu(), MU_SCALE, 0, Double.POSITIVE_INFINITY);
            muDriver.addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                    // getValue(), can be called without argument as mu driver should have only one span
                    factory.setMu(driver.getValue());
                }

                @Override
                public void valueSpanMapChanged(final TimeSpanMap<Double> previousValueSpanMap, final ParameterDriver driver) {
                    // getValue(), can be called without argument as mu driver should have only one span
                    factory.setMu(driver.getValue());
                }
            });
            propagationDrivers.add(muDriver);
        }
    }

    /** Get the mass.
     * @return the mass (kg)
     * @since 9.2
     */
    public double getMass()
    {
        return mass;
    }

    /** Set the initial mass.
     * @param mass the mass (kg)
     */
    public void setMass(final double mass) {
        this.mass = mass;
    }

    /** {@inheritDoc} */
    public OrbitalParameterFactory<OrbitalParameters> getOrbitalParameterFactory() {
        return factory;
    }

    /** {@inheritDoc} */
    public ParameterDriversList getPropagationParametersDrivers() {
        return propagationDrivers;
    }

    /** {@inheritDoc}. */
    @Override
    @SuppressWarnings("unchecked")
    public AbstractPropagatorBuilder<T> clone() {
        try {
            return (AbstractPropagatorBuilder<T>) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new OrekitException(OrekitMessages.PROPAGATOR_BUILDER_NOT_CLONEABLE);
        }
    }

    /**
     * Get the attitude provider.
     *
     * @return the attitude provider
     * @since 10.1
     */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /**
     * Set the attitude provider.
     *
     * @param attitudeProvider attitude provider
     * @since 10.1
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
    }

    /** Get the number of estimated values for selected parameters.
     * @return number of estimated values for selected parameters
     */
    private int getNbValuesForSelected() {

        int count = 0;

        // count orbital parameters
        for (final ParameterDriver driver : factory.getDrivers().getDrivers()) {
            if (driver.isSelected()) {
                count += driver.getNbOfValues();
            }
        }

        // count propagation parameters
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {
            if (driver.isSelected()) {
                count += driver.getNbOfValues();
            }
        }

        return count;

    }

    /** {@inheritDoc} */
    public double[] getSelectedNormalizedParameters() {

        // allocate array
        final double[] selected = new double[getNbValuesForSelected()];

        // fill data
        int index = 0;
        for (final ParameterDriver driver : factory.getDrivers().getDrivers()) {
            if (driver.isSelected()) {
                for (int spanNumber = 0; spanNumber < driver.getNbOfValues(); ++spanNumber ) {
                    selected[index++] = driver.getNormalizedValue(AbsoluteDate.ARBITRARY_EPOCH);
                }
            }
        }
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {
            if (driver.isSelected()) {
                for (int spanNumber = 0; spanNumber < driver.getNbOfValues(); ++spanNumber ) {
                    selected[index++] = driver.getNormalizedValue(AbsoluteDate.ARBITRARY_EPOCH);
                }
            }
        }

        return selected;

    }

    /** {@inheritDoc} */
    @Override
    public abstract T buildPropagator(double[] normalizedParameters);

    /** {@inheritDoc} */
    @Override
    public T buildPropagator() {
        return buildPropagator(getSelectedNormalizedParameters());
    }

    /** Set the selected parameters.
     * @param normalizedParameters normalized values for the selected parameters
     */
    protected void setParameters(final double[] normalizedParameters) {


        if (normalizedParameters.length != getNbValuesForSelected()) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                     normalizedParameters.length,
                                                     getNbValuesForSelected());
        }

        int index = 0;

        // manage orbital parameters
        for (final ParameterDriver driver : factory.getDrivers().getDrivers()) {
            if (driver.isSelected()) {
                // If the parameter driver contains only 1 value to estimate over the all time range, which
                // is normally always the case for orbital drivers
                if (driver.getNbOfValues() == 1) {
                    driver.setNormalizedValue(normalizedParameters[index++], null);

                } else {

                    for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                        driver.setNormalizedValue(normalizedParameters[index++], span.getStart());
                    }
                }
            }
        }

        // manage propagation parameters
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {

            if (driver.isSelected()) {

                for (Span<Double> span = driver.getValueSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    driver.setNormalizedValue(normalizedParameters[index++], span.getStart());
                }
            }
        }
    }

    /**
     * Add supported parameters.
     *
     * @param drivers drivers for the parameters
     */
    protected void addSupportedParameters(final List<ParameterDriver> drivers) {
        drivers.forEach(propagationDrivers::add);
        propagationDrivers.sort();
    }

    /** Reset the orbit in the propagator builder.
     * @param newOrbit New orbit to set in the propagator builder
     */
    public void resetOrbit(final Orbit newOrbit) {

        // Map the new orbit in an array of double
        final Orbit orbitInCorrectFrame = (newOrbit.getFrame() == factory.getFrame()) ?
                                          newOrbit :
                                          newOrbit.inFrame(factory.getFrame());
        final double[] orbitArray = factory.toArray(orbitInCorrectFrame);

        // Update all the orbital drivers, selected or unselected
        // Reset values and reference values
        final List<DelegatingDriver> orbitalDriversList = factory.getDrivers().getDrivers();
        int i = 0;
        for (DelegatingDriver driver : orbitalDriversList) {
            driver.setReferenceValue(orbitArray[i]);
            driver.setValue(orbitArray[i++], newOrbit.getDate());
        }

        // Change the initial orbit date in the builder
        factory.setDate(newOrbit.getDate());
    }

    /** Add a set of user-specified equations to be integrated along with the orbit propagation (author Shiva Iyer).
     * @param provider provider for additional derivatives
     * @since 11.1
     */
    public void addAdditionalDerivativesProvider(final AdditionalDerivativesProvider provider) {
        additionalDerivativesProviders.add(provider);
    }

    /** Get the list of additional equations.
     * @return the list of additional equations
     * @since 11.1
     */
    protected List<AdditionalDerivativesProvider> getAdditionalDerivativesProviders() {
        return additionalDerivativesProviders;
    }

    /** Deselects orbital and propagation drivers. */
    public void deselectDynamicParameters() {
        for (ParameterDriver driver : getPropagationParametersDrivers().getDrivers()) {
            driver.setSelected(false);
        }
        for (ParameterDriver driver : factory.getDrivers().getDrivers()) {
            driver.setSelected(false);
        }
    }
}
