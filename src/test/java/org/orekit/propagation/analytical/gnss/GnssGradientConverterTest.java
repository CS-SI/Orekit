/* Copyright 2002-2024 Luc Maisonobe
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;

public class GnssGradientConverterTest {

    private GNSSPropagator propagator;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final GalileoNavigationMessage goe = new GalileoNavigationMessage(DataContext.getDefault().getTimeScales(), SatelliteSystem.GALILEO);
        goe.setPRN(4);
        goe.setWeek(1024);
        goe.setTime(293400.0);
        goe.setSqrtA(5440.602949142456);
        goe.setDeltaN(3.7394414770330066E-9);
        goe.setE(2.4088891223073006E-4);
        goe.setI0(0.9531656087278083);
        goe.setIDot(-2.36081262303612E-10);
        goe.setOmega0(-0.36639513583951266);
        goe.setOmegaDot(-5.7695260382035525E-9);
        goe.setPa(-1.6870064194345724);
        goe.setM0(-0.38716557650888);
        goe.setCuc(-8.903443813323975E-7);
        goe.setCus(6.61797821521759E-6);
        goe.setCrc(194.0625);
        goe.setCrs(-18.78125);
        goe.setCic(3.166496753692627E-8);
        goe.setCis(-1.862645149230957E-8);
        propagator = new GNSSPropagatorBuilder(goe, DataContext.getDefault().getFrames()).build();
    }

    @Test
    public void testInitialStateStmNoSelectedParameters() {
        final FieldGnssPropagator<Gradient> gPropagator = new GnssGradientConverter(propagator).getPropagator();
        Assertions.assertEquals(9, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals(0, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(6, gPropagator.getInitialState().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    @Test
    public void testInitialStateStmAllParametersSelected() {
        propagator.getOrbitalElements().getParametersDrivers().forEach(p -> p.setSelected(true));
        final FieldGnssPropagator<Gradient> gPropagator = new GnssGradientConverter(propagator).getPropagator();
        Assertions.assertEquals(9, gPropagator.getParametersDrivers().size());
        Assertions.assertEquals( 9, gPropagator.getParametersDrivers().stream().filter(ParameterDriver::isSelected).count());
        Assertions.assertEquals(15, gPropagator.getInitialState().getA().getFreeParameters());
        checkUnitaryInitialSTM(gPropagator.getInitialState());
    }

    private void checkUnitaryInitialSTM(final FieldSpacecraftState<Gradient> initialState) {
        final FieldPVCoordinates<Gradient> pv0 = initialState.getPVCoordinates();
        checkUnitary(pv0.getPosition().getX().getGradient(), 0, 2.0e-12, 2.0e-8);
        checkUnitary(pv0.getPosition().getY().getGradient(), 1, 2.0e-12, 2.0e-8);
        checkUnitary(pv0.getPosition().getZ().getGradient(), 2, 2.0e-12, 2.0e-8);
        checkUnitary(pv0.getVelocity().getX().getGradient(), 3, 2.0e-12, 2.0e-8);
        checkUnitary(pv0.getVelocity().getY().getGradient(), 4, 2.0e-12, 2.0e-8);
    }

    private void checkUnitary(final double[] gradient, final int index,
                              final double tolDiag, final double tolNonDiag) {
        // beware! we intentionally check only the first 6 parameters
        // the next ones correspond to non-Keplerian parameters,
        // derivatives are NOT unitary for these extra parameters
        for (int i = 0; i < 6; i++) {
            if (i == index) {
                // diagonal element
                Assertions.assertEquals(1.0, gradient[i], tolDiag);
            } else {
                // non-diagonal element
                Assertions.assertEquals(0.0, gradient[i], tolNonDiag);
            }
        }
    }

}
