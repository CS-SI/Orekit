/* Copyright 2002-2024 Thales Alenia Space
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.List;

/** Converter for GNSS propagator.
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssGradientConverter extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final GNSSPropagator propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    GnssGradientConverter(final GNSSPropagator propagator) {
        super(propagator, FREE_STATE_PARAMETERS);
        // Initialize fields
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldGnssPropagator<Gradient> getPropagator() {

        final GNSSOrbitalElements<?> oe = propagator.getOrbitalElements();

        // bootstrap model, with canonical derivatives with respect to orbital parameters only
        final int bootstrapParameters = 6;
        final FieldGnssOrbitalElements<Gradient, ?, ?> bootstrapElements = oe.toField(GradientField.getField(bootstrapParameters));
        bootstrapElements.setSma(Gradient.variable(bootstrapParameters,    0, oe.getSma()));
        bootstrapElements.setE(Gradient.variable(bootstrapParameters,      1, oe.getE()));
        bootstrapElements.setI0(Gradient.variable(bootstrapParameters,     2, oe.getI0()));
        bootstrapElements.setPa(Gradient.variable(bootstrapParameters,     3, oe.getPa()));
        bootstrapElements.setOmega0(Gradient.variable(bootstrapParameters, 4, oe.getOmega0()));
        bootstrapElements.setM0(Gradient.variable(bootstrapParameters,     5, oe.getM0()));
        final Gradient bootstrapMass = Gradient.constant(bootstrapParameters,
                                                         propagator.getMass(propagator.getInitialState().getDate()));
        final FieldGnssPropagator<Gradient> bootstrapPropagator =
            new FieldGnssPropagator<>(bootstrapElements, propagator.getECI(), propagator.getECEF(),
                                      propagator.getAttitudeProvider(), bootstrapMass);
        final FieldSpacecraftState<Gradient> bootstrapState =
            bootstrapPropagator.propagate(bootstrapPropagator.getInitialState().getDate());

        // compute conversion matrix for derivatives to get identity initial Cartesian state Jacobian
        final RealMatrix                              stateJacobian  = MatrixUtils.createRealMatrix(6, 6);
        final TimeStampedFieldPVCoordinates<Gradient> bootstrapWrtPV = bootstrapState.getPVCoordinates();
        final FieldVector3D<Gradient>                 bootstrapWrtP  = bootstrapWrtPV.getPosition();
        final FieldVector3D<Gradient>                 bootstrapWrtV  = bootstrapWrtPV.getVelocity();
        stateJacobian.setRow(0, bootstrapWrtP.getX().getGradient());
        stateJacobian.setRow(1, bootstrapWrtP.getY().getGradient());
        stateJacobian.setRow(2, bootstrapWrtP.getZ().getGradient());
        stateJacobian.setRow(3, bootstrapWrtV.getX().getGradient());
        stateJacobian.setRow(4, bootstrapWrtV.getY().getGradient());
        stateJacobian.setRow(5, bootstrapWrtV.getZ().getGradient());
        RealMatrix stateJacobianInverse = new QRDecomposer(1.0e-10).decompose(stateJacobian).getInverse();

        final List<ParameterDriver> drivers  = propagator.getOrbitalElements().getParametersDrivers();
        int freeParameters = bootstrapParameters;
        for (final ParameterDriver driver : drivers) {
            if (driver.isSelected()) {
                ++freeParameters;
            }
        }

        // regular parameters, with converted derivatives
        final Gradient convertedSma    = extend(new Gradient(oe.getSma(),    stateJacobianInverse.getRow(0)), freeParameters);
        final Gradient convertedE      = extend(new Gradient(oe.getE(),      stateJacobianInverse.getRow(1)), freeParameters);
        final Gradient convertedI0     = extend(new Gradient(oe.getI0(),     stateJacobianInverse.getRow(2)), freeParameters);
        final Gradient convertedPa     = extend(new Gradient(oe.getPa(),     stateJacobianInverse.getRow(3)), freeParameters);
        final Gradient convertedOmega0 = extend(new Gradient(oe.getOmega0(), stateJacobianInverse.getRow(4)), freeParameters);
        final Gradient convertedM0     = extend(new Gradient(oe.getM0(),     stateJacobianInverse.getRow(5)), freeParameters);

        final Gradient[] convertedNonKeplerianParameters = new Gradient[GNSSOrbitalElements.SIZE];
        int index = bootstrapParameters;
        for (int i = 0; i < convertedNonKeplerianParameters.length; ++i) {
            final ParameterDriver driver = drivers.get(i);
            convertedNonKeplerianParameters[i] = driver.isSelected() ?
                                                 Gradient.variable(freeParameters, index++, driver.getValue()) :
                                                 Gradient.constant(freeParameters, driver.getValue());
        }

        final int totalParameters = bootstrapParameters + freeParameters;
        final FieldGnssOrbitalElements<Gradient, ?, ?> convertedElements = oe.toField(GradientField.getField(totalParameters));
        convertedElements.setSma(Gradient.variable(totalParameters,    0, oe.getSma()));
        convertedElements.setE(Gradient.variable(totalParameters,      1, oe.getE()));
        convertedElements.setI0(Gradient.variable(totalParameters,     2, oe.getI0()));
        convertedElements.setPa(Gradient.variable(totalParameters,     3, oe.getPa()));
        convertedElements.setOmega0(Gradient.variable(totalParameters, 4, oe.getOmega0()));
        convertedElements.setM0(Gradient.variable(totalParameters,     5, oe.getM0()));
        final Gradient convertedMass =
            Gradient.constant(totalParameters, propagator.getMass(propagator.getInitialState().getDate()));

        // build a propagator with derivatives set up with respect to model Keplerian orbital parameters
        // that still has identity state Jacobian with respect to initial position-velocity
        final FieldGnssPropagator<Gradient> gPropagator = new FieldGnssPropagator<>(convertedElements,
                                                                                    propagator.getECI(),
                                                                                    propagator.getECEF(),
                                                                                    propagator.getAttitudeProvider(),
                                                                                    convertedMass);

        // set selection status as in the original propagator
        final List<ParameterDriver> gDrivers = gPropagator.getParametersDrivers();
        for (int i = 0; i < gDrivers.size(); ++i) {
           gDrivers.get(i).setSelected(drivers.get(i).isSelected());
        }

        return gPropagator;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return propagator.getOrbitalElements().getParametersDrivers();
    }

}
