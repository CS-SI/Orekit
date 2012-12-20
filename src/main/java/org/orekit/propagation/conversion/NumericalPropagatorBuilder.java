/* Copyright 2002-2012 CS Systèmes d'Information
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
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder implements PropagatorBuilder {

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Frame in which the orbit is propagated. */
    private final Frame frame;

    /** First order integrator builder for propagation. */
    private final FirstOrderIntegratorBuilder builder;

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** Current mass for initial state (kg). */
    private double mass;

    /** Attitude provider. */
    private AttitudeProvider attProvider;

    /** List of the free parameters names. */
    private Collection<String> freeParameters;

    /** Build a new instance.
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param builder first order integrator builder
     */
    public NumericalPropagatorBuilder(final double mu,
                                      final Frame frame,
                                      final FirstOrderIntegratorBuilder builder) {
        this.mu          = mu;
        this.frame       = frame;
        this.builder     = builder;
        this.forceModels = new ArrayList<ForceModel>();
        this.mass        = Propagator.DEFAULT_MASS;
        this.attProvider = Propagator.DEFAULT_LAW;
    }

    /** Set the attitude provider.
     * @param attitudeProvider attitude provider
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attProvider = attitudeProvider;
    }

    /** Set the initial mass.
     * @param mass the mass (kg)
     */
    public void setMass(final double mass) {
        this.mass = mass;
    }

    /** Add a force model to the global perturbation model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a keplerian evolution only.</p>
     * @param model perturbing {@link ForceModel} to add
     */
    public void addForceModel(final ForceModel model) {
        forceModels.add(model);
    }

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {

        if (parameters.length != (freeParameters.size() + 6)) {
            throw OrekitException.createIllegalArgumentException(LocalizedFormats.DIMENSIONS_MISMATCH);
        }

        final Orbit orb = new CartesianOrbit(new PVCoordinates(new Vector3D(parameters[0],
                                                                            parameters[1],
                                                                            parameters[2]),
                                                               new Vector3D(parameters[3],
                                                                            parameters[4],
                                                                            parameters[5])),
                                              frame, date, mu);

        final Attitude attitude = attProvider.getAttitude(orb, date, frame);

        final SpacecraftState state = new SpacecraftState(orb, attitude, mass);

        for (int i = 6; i < parameters.length; i++) {
            for (String free : freeParameters) {
                for (String available : getParametersNames()) {
                    if (free.equals(available)) {
                        setParameter(free, parameters[i]);
                    }
                }
            }
        }

        final NumericalPropagator propagator = new NumericalPropagator(builder.buildIntegrator(orb));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setAttitudeProvider(attProvider);
        for (ForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        propagator.resetInitialState(state);

        return propagator;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    public void setFreeParameters(final Collection<String> parameters)
        throws IllegalArgumentException {
        freeParameters = new ArrayList<String>();
        for (String name : parameters) {
            if (!isSupported(name)) {
                throw OrekitException.createIllegalArgumentException(LocalizedFormats.UNKNOWN_PARAMETER, name);
            }
        }
        freeParameters.addAll(parameters);
    }

    /** {@inheritDoc} */
    public Collection<String> getParametersNames() {
        final Collection<String> parametersNames = new ArrayList<String>();
        for (ForceModel model : forceModels) {
            parametersNames.addAll(model.getParametersNames());
        }
        return parametersNames;
    }

    /** {@inheritDoc} */
    public boolean isSupported(final String name) {
        for (ForceModel model : forceModels) {
            if (model.isSupported(name)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        for (ForceModel model : forceModels) {
            if (model.isSupported(name)) {
                return model.getParameter(name);
            }
        }
        throw new IllegalArgumentException(name);
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        for (ForceModel model : forceModels) {
            if (model.isSupported(name)) {
                model.setParameter(name, value);
                break;
            }
        }
    }

}
