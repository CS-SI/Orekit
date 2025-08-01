/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinearKeplerianCovarianceHandlerTest {

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testPropagationLof(final OrbitType orbitType) {
        testPropagationTemplate(LOFType.QSW, orbitType);
    }

    @ParameterizedTest
    @EnumSource(value = LOFType.class, names = {"QSW", "TNW", "NTW", "LVLH"})
    void testPropagationLof(final LOFType lofType) {
        testPropagationTemplate(lofType, OrbitType.CARTESIAN);
    }

    private void testPropagationTemplate(final LOFType lofType, final OrbitType orbitType) {
        // GIVEN
        final Orbit initialOrbit = new EquinoctialOrbit(7e6, 0.0001, 0., 1., 2., 3., PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final NumericalPropagator propagator = buildPropagator(initialOrbit, orbitType);
        final AbsoluteDate targetDate = initialOrbit.getDate().shiftedBy(1e4);
        final RealMatrix initialMatrix = MatrixUtils.createRealIdentityMatrix(6);
        initialMatrix.setSubMatrix(MatrixUtils.createRealIdentityMatrix(3).scalarMultiply(1e-2).getData(), 3, 3);
        final StateCovariance initialCovariance = new StateCovariance(initialMatrix, initialOrbit.getDate(), lofType);
        final LinearKeplerianCovarianceHandler covarianceHandler = new LinearKeplerianCovarianceHandler(initialCovariance);
        propagator.setStepHandler(covarianceHandler.toOrekitStepHandler());
        // WHEN
        propagator.propagate(targetDate);
        final List<StateCovariance> covariances = covarianceHandler.getStatesCovariances();
        final StateCovariance actualTerminalCovariance = covariances.get(covariances.size() - 1);
        // THEN
        final NumericalPropagator otherPropagator = buildPropagator(initialOrbit, orbitType);
        final String stmName = "stm";
        final MatricesHarvester harvester = otherPropagator.setupMatricesComputation(stmName, null, null);
        final StateCovarianceMatrixProvider covarianceMatrixProvider = new StateCovarianceMatrixProvider("cov",
                stmName, harvester, initialCovariance.changeCovarianceFrame(initialOrbit, otherPropagator.getFrame()));
        otherPropagator.addAdditionalDataProvider(covarianceMatrixProvider);
        final SpacecraftState terminalState = otherPropagator.propagate(targetDate);
        final StateCovariance terminalCovariance = covarianceMatrixProvider.getStateCovariance(terminalState);
        final StateCovariance expectedCovariance = terminalCovariance.changeCovarianceFrame(terminalState.getOrbit(), lofType);
        final RealMatrix difference = expectedCovariance.getMatrix().subtract(actualTerminalCovariance.getMatrix());
        assertEquals(0., difference.getNorm1(),expectedCovariance.getMatrix().getNorm1() * 1e-6);
    }

    private static NumericalPropagator buildPropagator(final Orbit initialOrbit, final OrbitType orbitType) {
        final ToleranceProvider toleranceProvider = ToleranceProvider.getDefaultToleranceProvider(1e-3);
        final DormandPrince54IntegratorBuilder integratorBuilder = new DormandPrince54IntegratorBuilder(1e-3, 1e2, toleranceProvider);
        final DormandPrince54Integrator integrator = integratorBuilder.buildIntegrator(initialOrbit, orbitType);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(PositionAngleType.MEAN);
        return propagator;
    }
}
