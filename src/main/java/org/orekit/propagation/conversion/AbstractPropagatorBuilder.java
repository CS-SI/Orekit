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
package org.orekit.propagation.conversion;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.ParameterObserver;

/** Base class for propagator builders.
 * @author Pascal Parraud
 * @since 7.1
 */
public abstract class AbstractPropagatorBuilder implements PropagatorBuilder {

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Date of the initial orbit. */
    private final AbsoluteDate initialOrbitDate;

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** Drivers for orbital parameters. */
    private final ParameterDriversList orbitalDrivers;

    /** List of the supported parameters. */
    private ParameterDriversList propagationDrivers;

    /** Orbit type to use. */
    private final OrbitType orbitType;

    /** Position angle type to use. */
    private final PositionAngle positionAngle;

    /** Build a new instance.
     * <p>
     * The template orbit is used as a model to {@link
     * #createInitialOrbit() create initial orbit}. It defines the
     * inertial frame, the central attraction coefficient, the orbit type, and is also
     * used together with the {@code positionScale} to convert from the {@link
     * ParameterDriver#setNormalizedValue(double) normalized} parameters used by the
     * callers of this builder to the real orbital parameters.
     * </p>
     * <p>
     * By default, all the {@link #getOrbitalParametersDrivers() orbital parameters drivers}
     * are selected, which means that if the builder is used for orbit determination or
     * propagator conversion, all orbital parameters will be estimated. If only a subset
     * of the orbital parameters must be estimated, caller must retrieve the orbital
     * parameters by calling {@link #getOrbitalParametersDrivers()} and then call
     * {@link ParameterDriver#setSelected(boolean) setSelected(false)}.
     * </p>
     * @param templateOrbit reference orbit from which real orbits will be built
     * @param positionAngle position angle type to use
     * @param positionScale scaling factor used for orbital parameters normalization
     * (typically set to the expected standard deviation of the position)
     * @exception OrekitException if parameters drivers cannot be scaled
     * @since 8.0
     */
    protected AbstractPropagatorBuilder(final Orbit templateOrbit, final PositionAngle positionAngle,
                                        final double positionScale)
        throws OrekitException {

        this.initialOrbitDate    = templateOrbit.getDate();
        this.frame               = templateOrbit.getFrame();
        this.mu                  = templateOrbit.getMu();
        this.propagationDrivers = new ParameterDriversList();
        this.orbitType           = templateOrbit.getType();
        this.positionAngle       = positionAngle;
        this.orbitalDrivers      = orbitType.getDrivers(positionScale, templateOrbit, positionAngle);
        for (final DelegatingDriver driver : orbitalDrivers.getDrivers()) {
            driver.setSelected(true);
        }

        final ParameterDriver muDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                             mu, MU_SCALE, 0, Double.POSITIVE_INFINITY);
        muDriver.addObserver(new ParameterObserver() {
            /** {@inheridDoc} */
            @Override
            public void valueChanged(final double previousValue, final ParameterDriver driver) {
                AbstractPropagatorBuilder.this.mu = driver.getValue();
            }
        });
        propagationDrivers.add(muDriver);

    }

    /** {@inheritDoc} */
    public OrbitType getOrbitType() {
        return orbitType;
    }

    /** {@inheritDoc} */
    public PositionAngle getPositionAngle() {
        return positionAngle;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getInitialOrbitDate() {
        return initialOrbitDate;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public ParameterDriversList getOrbitalParametersDrivers() {
        return orbitalDrivers;
    }

    /** {@inheritDoc} */
    public ParameterDriversList getPropagationParametersDrivers() {
        return propagationDrivers;
    }

    /** Get the number of selected parameters.
     * @return number of selected parameters
     */
    private int getNbSelected() {

        int count = 0;

        // count orbital parameters
        for (final ParameterDriver driver : orbitalDrivers.getDrivers()) {
            if (driver.isSelected()) {
                ++count;
            }
        }

        // count propagation parameters
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {
            if (driver.isSelected()) {
                ++count;
            }
        }

        return count;

    }

    /** {@inheritDoc} */
    public double[] getSelectedNormalizedParameters() {

        // allocate array
        final double[] selected = new double[getNbSelected()];

        // fill data
        int index = 0;
        for (final ParameterDriver driver : orbitalDrivers.getDrivers()) {
            if (driver.isSelected()) {
                selected[index++] = driver.getNormalizedValue();
            }
        }
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {
            if (driver.isSelected()) {
                selected[index++] = driver.getNormalizedValue();
            }
        }

        return selected;

    }

    /** Build an initial orbit using the current selected parameters.
     * <p>
     * This method is a stripped down version of {@link #buildPropagator(double[])}
     * that only builds the initial orbit and not the full propagator.
     * </p>
     * @return an initial orbit
     * @since 8.0
     */
    protected Orbit createInitialOrbit() {
        final double[] unNormalized = new double[orbitalDrivers.getNbParams()];
        for (int i = 0; i < unNormalized.length; ++i) {
            unNormalized[i] = orbitalDrivers.getDrivers().get(i).getValue();
        }
        return getOrbitType().mapArrayToOrbit(unNormalized, positionAngle, initialOrbitDate, mu, frame);
    }

    /** Set the selected parameters.
     * @param normalizedParameters normalized values for the selected parameters
     * @exception OrekitException if some parameter cannot be set to the specified value
     * @exception OrekitIllegalArgumentException if the number of parameters is not the
     * number of selected parameters (adding orbits and models parameters)
     */
    protected void setParameters(final double[] normalizedParameters)
        throws OrekitException, OrekitIllegalArgumentException {


        if (normalizedParameters.length != getNbSelected()) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                     normalizedParameters.length,
                                                     getNbSelected());
        }

        int index = 0;

        // manage orbital parameters
        for (final ParameterDriver driver : orbitalDrivers.getDrivers()) {
            if (driver.isSelected()) {
                driver.setNormalizedValue(normalizedParameters[index++]);
            }
        }

        // manage propagation parameters
        for (final ParameterDriver driver : propagationDrivers.getDrivers()) {
            if (driver.isSelected()) {
                driver.setNormalizedValue(normalizedParameters[index++]);
            }
        }

    }

    /** Add a supported parameter.
     * @param driver driver for the parameter
     * @exception OrekitException if the name is already supported
     */
    protected void addSupportedParameter(final ParameterDriver driver)
        throws OrekitException {
        propagationDrivers.add(driver);
        propagationDrivers.sort();
    }

}
