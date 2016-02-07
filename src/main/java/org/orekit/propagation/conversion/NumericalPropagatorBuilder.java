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
import java.util.Iterator;
import java.util.List;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

/** Builder for numerical propagator.
 * @author Pascal Parraud
 * @since 6.0
 */
public class NumericalPropagatorBuilder extends AbstractPropagatorBuilder {

    /** First order integrator builder for propagation. */
    private final FirstOrderIntegratorBuilder builder;

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** Current mass for initial state (kg). */
    private double mass;

    /** Attitude provider. */
    private AttitudeProvider attProvider;

    /** Build a new instance.
     * @param mu central attraction coefficient (m³/s²)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param builder first order integrator builder
     * @deprecated as of 7.1, replaced with {@link #NumericalPropagatorBuilder(double,
     * Frame, FirstOrderIntegratorBuilder, OrbitType, PositionAngle)}
     */
    @Deprecated
    public NumericalPropagatorBuilder(final double mu,
                                      final Frame frame,
                                      final FirstOrderIntegratorBuilder builder) {
        this(mu, frame, builder, OrbitType.CARTESIAN, PositionAngle.TRUE);
    }

    /** Build a new instance.
     * @param mu central attraction coefficient (m³/s²)
     * @param frame the frame in which the orbit is propagated
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param builder first order integrator builder
     * @param orbitType orbit type to use
     * @param positionAngle position angle type to use
     * @since 7.1
     */
    public NumericalPropagatorBuilder(final double mu,
                                      final Frame frame,
                                      final FirstOrderIntegratorBuilder builder,
                                      final OrbitType orbitType, final PositionAngle positionAngle) {
        super(frame, mu, orbitType, positionAngle);
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
        for (final String name : model.getParametersNames()) {
            // add model parameters, taking care of
            // Newtonian central attraction which is already supported by base class
            if (NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT.equals(name)) {
                super.setParameter(name, model.getParameter(name));
            } else {
                addSupportedParameter(name);
            }
        }
    }

    /** {@inheritDoc} */
    public NumericalPropagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException {

        checkParameters(parameters);
        final Orbit orb = createInitialOrbit(date, parameters);

        final Attitude attitude = attProvider.getAttitude(orb, date, getFrame());

        final SpacecraftState state = new SpacecraftState(orb, attitude, mass);

        final Iterator<String> freeItr = getFreeParameters().iterator();
        for (int i = 6; i < parameters.length; i++) {
            final String free = freeItr.next();
            for (String available : getSupportedParameters()) {
                if (free.equals(available)) {
                    setParameter(free, parameters[i]);
                }
            }
        }

        final NumericalPropagator propagator = new NumericalPropagator(builder.buildIntegrator(orb, getOrbitType()));
        propagator.setOrbitType(getOrbitType());
        propagator.setPositionAngleType(getPositionAngle());
        propagator.setAttitudeProvider(attProvider);
        for (ForceModel model : forceModels) {
            propagator.addForceModel(model);
        }
        propagator.resetInitialState(state);

        return propagator;
    }

    /** {@inheritDoc} */
    @Override
    public double getParameter(final String name)
        throws OrekitIllegalArgumentException {
        for (ForceModel model : forceModels) {
            if (model.isSupported(name)) {
                return model.getParameter(name);
            }
        }
        return super.getParameter(name);
    }

    /** {@inheritDoc} */
    @Override
    public void setParameter(final String name, final double value)
        throws OrekitIllegalArgumentException {
        for (ForceModel model : forceModels) {
            if (model.isSupported(name)) {
                model.setParameter(name, value);
                if (NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT.equals(name)) {
                    // the central attraction must be configured
                    // in both the force model and the base class
                    super.setParameter(name, value);
                }
                return;
            }
        }
        super.setParameter(name, value);
    }

}
