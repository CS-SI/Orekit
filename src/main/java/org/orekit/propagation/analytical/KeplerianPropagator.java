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
package org.orekit.propagation.analytical;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @see Orbit
 * @author Guylaine Prat
 */
public class KeplerianPropagator extends AbstractAnalyticalPropagator implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20151117L;

    /** Initial state. */
    private SpacecraftState initialState;

    /** Build a propagator from orbit only.
     * <p>The central attraction coefficient μ is set to the same value used
     * for the initial orbit definition. Mass and attitude provider are set to
     * unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit and central attraction coefficient μ.
     * <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     * @param initialOrbit initial orbit
     * @param mu central attraction coefficient (m³/s²)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final double mu)
        throws PropagationException {
        this(initialOrbit, DEFAULT_LAW, mu, DEFAULT_MASS);
    }

    /** Build a propagator from orbit and attitude provider.
     * <p>The central attraction coefficient μ is set to the same value
     * used for the initial orbit definition. Mass is set to an unspecified
     * non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv  attitude provider
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv)
        throws PropagationException {
        this(initialOrbit, attitudeProv, initialOrbit.getMu(), DEFAULT_MASS);
    }

    /** Build a propagator from orbit, attitude provider and central attraction
     * coefficient μ.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit,
                               final AttitudeProvider attitudeProv,
                               final double mu)
        throws PropagationException {
        this(initialOrbit, attitudeProv, mu, DEFAULT_MASS);
    }

    /** Build propagator from orbit, attitude provider, central attraction
     * coefficient μ and mass.
     * @param initialOrbit initial orbit
     * @param attitudeProv attitude provider
     * @param mu central attraction coefficient (m³/s²)
     * @param mass spacecraft mass (kg)
     * @exception PropagationException if initial attitude cannot be computed
     */
    public KeplerianPropagator(final Orbit initialOrbit, final AttitudeProvider attitudeProv,
                               final double mu, final double mass)
        throws PropagationException {

        super(attitudeProv);

        try {

            // ensure the orbit use the specified mu
            final OrbitType type = initialOrbit.getType();
            final double[] stateVector = new double[6];
            type.mapOrbitToArray(initialOrbit, PositionAngle.TRUE, stateVector);
            final Orbit orbit = type.mapArrayToOrbit(stateVector, PositionAngle.TRUE,
                                                     initialOrbit.getDate(), mu, initialOrbit.getFrame());

            resetInitialState(new SpacecraftState(orbit,
                                                   getAttitudeProvider().getAttitude(orbit,
                                                                                     orbit.getDate(),
                                                                                     orbit.getFrame()),
                                                   mass));

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        super.resetInitialState(state);
        initialState   = state;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date)
        throws PropagationException {

        // propagate orbit
        Orbit orbit = initialState.getOrbit();
        do {
            // we use a loop here to compensate for very small date shifts error
            // that occur with long propagation time
            orbit = orbit.shiftedBy(date.durationFrom(orbit.getDate()));
        } while(!date.equals(orbit.getDate()));

        return orbit;

    }

    /** {@inheritDoc}*/
    protected double getMass(final AbsoluteDate date) {
        return initialState.getMass();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     * @exception NotSerializableException if an additional state provider is not serializable
     */
    private Object writeReplace() throws NotSerializableException {
        try {

            // managed states providers
            final List<AdditionalStateProvider> serializableProviders = new ArrayList<AdditionalStateProvider>();
            for (final AdditionalStateProvider provider : getAdditionalStateProviders()) {
                if (provider instanceof Serializable) {
                    serializableProviders.add(provider);
                } else {
                    throw new NotSerializableException(provider.getClass().getName());
                }
            }

            return new DataTransferObject(getInitialState().getOrbit(), getAttitudeProvider(),
                                          getInitialState().getMu(), getInitialState().getMass(),
                                          serializableProviders.toArray(new AdditionalStateProvider[serializableProviders.size()]));
        } catch (OrekitException orekitException) {
            // this should never happen
            throw new OrekitInternalError(null);
        }

    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151117L;

        /** Initial orbit. */
        private final Orbit orbit;

        /** Attitude provider. */
        private final AttitudeProvider attitudeProvider;

        /** Central attraction coefficient (m³/s²). */
        private final double mu;

        /** Spacecraft mass (kg). */
        private final double mass;

        /** Providers for additional states. */
        private final AdditionalStateProvider[] providers;

        /** Simple constructor.
         * @param orbit initial orbit
         * @param attitudeProvider attitude provider
         * @param mu central attraction coefficient (m³/s²)
         * @param mass spacecraft mass (kg)
         * @param providers providers for additional states
         */
        DataTransferObject(final Orbit orbit,
                           final AttitudeProvider attitudeProvider,
                           final double mu, final double mass,
                           final AdditionalStateProvider[] providers) {
            this.orbit            = orbit;
            this.attitudeProvider = attitudeProvider;
            this.mu               = mu;
            this.mass             = mass;
            this.providers        = providers;
        }

        /** Replace the deserialized data transfer object with a {@link KeplerianPropagator}.
         * @return replacement {@link KeplerianPropagator}
         */
        private Object readResolve() {
            try {
                final KeplerianPropagator propagator =
                                new KeplerianPropagator(orbit, attitudeProvider, mu, mass);
                for (final AdditionalStateProvider provider : providers) {
                    propagator.addAdditionalStateProvider(provider);
                }
                return propagator;
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
