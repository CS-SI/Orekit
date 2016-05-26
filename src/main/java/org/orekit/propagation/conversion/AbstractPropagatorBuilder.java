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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

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

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** Central attraction coefficient (m³/s²). */
    private double mu;

    /** List of the supported parameters. */
    private ParameterDriversList supportedParameters;

    /** Orbit type to use. */
    private final OrbitType orbitType;

    /** Position angle type to use. */
    private final PositionAngle positionAngle;

    /** Build a new instance.
     * @param frame the frame in which the orbit is propagated
     *        (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public AbstractPropagatorBuilder(final Frame frame, final double mu,
                                     final OrbitType orbitType, final PositionAngle positionAngle) {
        try {
            this.frame               = frame;
            this.mu                  = mu;
            this.supportedParameters = new ParameterDriversList();
            this.orbitType           = orbitType;
            this.positionAngle       = positionAngle;

            final ParameterDriver muDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                                 mu, MU_SCALE) {
                /** {@inheridDoc} */
                @Override
                protected void valueChanged(final double newValue) {
                    AbstractPropagatorBuilder.this.mu = newValue;
                }
            };
            supportedParameters.add(muDriver);

        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
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
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public ParameterDriversList getParametersDrivers() {
        return supportedParameters;
    }

    /** {@inheritDoc}
     * @deprecated as of 8.0, replaced with {@link #getParametersDrivers()}
     */
    @Deprecated
    public List<String> getSupportedParameters() {
        final List<String> supportedParametersNames = new ArrayList<String>();
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            supportedParametersNames.add(driver.getName());
        }
        return supportedParametersNames;
    }

    /** {@inheritDoc}
     * @deprecated as of 8.0, replaced with {@link #getParametersDrivers()} and
     * {@link ParameterDriver#setSelected(boolean)}
     */
    @Deprecated
    public void setFreeParameters(final List<String> parameters)
        throws OrekitIllegalArgumentException {

        // start by resetting all parameters to not estimated
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            driver.setSelected(false);
        }

        // estimate only the specified parameters
        for (final String name : parameters) {
            boolean found = false;
            for (final ParameterDriver driver : supportedParameters.getDrivers()) {
                if (driver.getName().equals(name)) {
                    found = true;
                    driver.setSelected(true);
                }
            }
            if (!found) {
                throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                         name, supportedAsString());
            }
        }

    }

    /** {@inheritDoc}
     * @deprecated as of 8.0, replaced with {@link #getParametersDrivers()} and
     * {@link ParameterDriver#isSelected()}
     */
    @Deprecated
    public List<String> getFreeParameters() {
        final List<String> freeParameters = new ArrayList<String>();
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            if (driver.isSelected()) {
                freeParameters.add(driver.getName());
            }
        }
        return freeParameters;
    }

    /** {@inheritDoc}
     * <p>
     * The abstract base class only supports {@link
     * NewtonianAttraction#CENTRAL_ATTRACTION_COEFFICIENT}, specialized propagator
     * builders may support more parameters.
     * </p>
     * @deprecated as of 8.0, replaced with {@link #getParametersDrivers()} and
     * {@link ParameterDriver#getValue()}
     */
    @Deprecated
    public double getParameter(final String name)
        throws OrekitIllegalArgumentException {
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            if (driver.getName().equals(name)) {
                return driver.getValue();
            }
        }
        throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                 name, supportedAsString());
    }

    /** {@inheritDoc}
     * <p>
     * The abstract base class only supports {@link
     * NewtonianAttraction#CENTRAL_ATTRACTION_COEFFICIENT}, specialized propagator
     * builders may support more parameters.
     * </p>
     * @deprecated as of 8.0, replaced with {@link #getParametersDrivers()} and
     * {@link ParameterDriver#setValue(double[])}
     */
    @Deprecated
    public void setParameter(final String name, final double value)
        throws OrekitIllegalArgumentException {
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            if (driver.getName().equals(name)) {
                try {
                    driver.setValue(value);
                    return;
                } catch (OrekitException oe) {
                    throw new OrekitIllegalArgumentException(oe.getSpecifier(), oe.getParts());
                }
            }
        }
        throw new OrekitIllegalArgumentException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                                 name, supportedAsString());
    }

    /** Crate the orbit for the first 6 parameters.
     * @param date date associated to the parameters to configure the initial state
     * @param parameters set of position/velocity(/free) parameters to configure the propagator
     * @return initial orbit
     */
    public Orbit createInitialOrbit(final AbsoluteDate date, final double[] parameters) {
        return getOrbitType().mapArrayToOrbit(parameters, positionAngle, date, mu, frame);
    }

    /** Check the size of the parameters array.
     * @param parameters to configure the propagator
     * @exception OrekitIllegalArgumentException if the number of
     * parameters is not 6 (for initial state) plus the number of free
     * parameters
     */
    protected void checkParameters(final double[] parameters)
        throws OrekitIllegalArgumentException {

        // compute required array size
        int size = 6;
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            if (driver.isSelected()) {
                ++size;
            }
        }

        if (parameters.length != size) {
            throw new OrekitIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                     parameters.length, size);
        }
    }

    /** Add a supported parameter.
     * @param driver driver for the parameter
     * @exception OrekitException if the name is already supported
     */
    protected void addSupportedParameter(final ParameterDriver driver)
        throws OrekitException {
        supportedParameters.add(driver);
    }

    /** Create a string with the list of supported parameters.
     * @return string with the list of supported parameters
     */
    private String supportedAsString() {
        final StringBuilder supported = new StringBuilder();
        for (final ParameterDriver driver : supportedParameters.getDrivers()) {
            if (supported.length() > 0) {
                supported.append(", ");
            }
            supported.append(driver.getName());
        }
        return supported.toString();
    }

}
