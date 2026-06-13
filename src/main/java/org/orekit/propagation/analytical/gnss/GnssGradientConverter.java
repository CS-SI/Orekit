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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

import java.util.List;

/** Converter for GNSS propagator.
 * @param <O> type of the orbital elements (non-field version)
 * @param <P> type of the orbital elements (field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssGradientConverter<O extends GNSSOrbitalElements<O>,
                            P extends FieldGnssOrbitalElements<Gradient, O, P>>
    extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final GNSSPropagator<O> propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    GnssGradientConverter(final GNSSPropagator<O> propagator) {
        super(propagator, FREE_STATE_PARAMETERS);
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldGnssPropagator<Gradient, O, P> getPropagator() {

        // count the required number of parameters
        int n = FREE_STATE_PARAMETERS;
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                n += driver.getNbOfValues();
            }
        }
        final int nbParams = n;

        // prepare orbit with proper derivatives
        final SpacecraftState s0    = propagator.getInitialState();
        final KeplerianOrbit  orbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s0.getOrbit());
        final FieldKeplerianOrbit<Gradient> gOrbit =
            new FieldKeplerianOrbit<>(Gradient.variable(nbParams, 0, orbit.getA()),
                                      Gradient.variable(nbParams, 1, orbit.getE()),
                                      Gradient.variable(nbParams, 2, orbit.getI()),
                                      Gradient.variable(nbParams, 3, orbit.getPerigeeArgument()),
                                      Gradient.variable(nbParams, 4, orbit.getRightAscensionOfAscendingNode()),
                                      Gradient.variable(nbParams, 5, orbit.getMeanAnomaly()),
                                      PositionAngleType.MEAN, PositionAngleType.MEAN,
                                      orbit.getFrame(),
                                      new FieldAbsoluteDate<>(GradientField.getField(nbParams), orbit.getDate()),
                                      Gradient.constant(nbParams, orbit.getMu()));

        // attitude
        final FieldAttitude<Gradient> gAttitude =
            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                new TimeStampedFieldAngularCoordinates<>(gOrbit.getDate().getField(),
                                                                         s0.getAttitude().getOrientation()));

        // mass
        final Gradient gMass = Gradient.constant(nbParams, s0.getMass());

        // completed state
        final FieldSpacecraftState<Gradient> gState =
            new FieldSpacecraftState<>(gOrbit, gAttitude).withMass(gMass);

        // prepare non-Keplerian elements with proper derivatives
        final Gradient[] parameters = propagator.getDriversFactory().toGradients(nbParams);

        // convert elements to support gradient
        final P elements = propagator.getOrbitalElements().toField(gOrbit,
                                                                   parameters,
                                                                   d  -> Gradient.constant(nbParams, d));

        // build propagator handling gradient
        final FieldGnssPropagator<Gradient, O, P> gPropagator =
            new FieldGnssPropagator<>(gState, elements,
                                      propagator.getECEF(), propagator.getAttitudeProvider(),
                                      gState.getMass());
        final List<ParameterDriver> gDrivers = gPropagator.getParametersDrivers();
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                gDrivers.
                    stream().
                    filter(gDriver -> driver.getName().equals(gDriver.getName())).
                    findFirst().
                    ifPresent(gDriver -> gDriver.setSelected(true));
            }
        }

        return gPropagator;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return propagator.getParametersDrivers();
    }

}
