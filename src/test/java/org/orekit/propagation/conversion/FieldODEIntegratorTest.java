/* Contributed in the public domain.
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

import org.hipparchus.Field;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.nonstiff.AdamsBashforthFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdamsMoultonFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.EulerFieldIntegrator;
import org.hipparchus.ode.nonstiff.GillFieldIntegrator;
import org.hipparchus.ode.nonstiff.HighamHall54FieldIntegrator;
import org.hipparchus.ode.nonstiff.LutherFieldIntegrator;
import org.hipparchus.ode.nonstiff.MidpointFieldIntegrator;
import org.hipparchus.ode.nonstiff.ThreeEighthesFieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

public class FieldODEIntegratorTest {

    @Test
    @DisplayName("Test FieldODEIntegrator builders")
    void testFieldODEIntegratorBuilders() {

        // Given

        // Load Orekit data
        Utils.setDataRoot("regular-data");

        final Field<Binary64>      field = Binary64Field.getInstance();
        final FieldOrbit<Binary64> orbit = getReferenceOrbit();

        final double   step      = 100;
        final Binary64 fieldStep = new Binary64(step);

        final int    nSteps  = 2;
        final double minStep = 1;
        final double maxStep = 300;
        final double dP      = 1;

        final AdamsBashforthFieldIntegratorBuilder<Binary64> integratorBuilder01 =
                new AdamsBashforthFieldIntegratorBuilder<>(nSteps, minStep, maxStep, dP);

        final AdamsMoultonFieldIntegratorBuilder<Binary64> integratorBuilder02 =
                new AdamsMoultonFieldIntegratorBuilder<>(nSteps, minStep, maxStep, dP);

        final ClassicalRungeKuttaFieldIntegratorBuilder<Binary64> integratorBuilder03 =
                new ClassicalRungeKuttaFieldIntegratorBuilder<>(fieldStep);

        final ClassicalRungeKuttaFieldIntegratorBuilder<Binary64> integratorBuilder03Bis =
                new ClassicalRungeKuttaFieldIntegratorBuilder<>(step);

        final DormandPrince54FieldIntegratorBuilder<Binary64> integratorBuilder04 =
                new DormandPrince54FieldIntegratorBuilder<>(minStep, maxStep, dP);

        final DormandPrince853FieldIntegratorBuilder<Binary64> integratorBuilder05 =
                new DormandPrince853FieldIntegratorBuilder<>(minStep, maxStep, dP);

        final EulerFieldIntegratorBuilder<Binary64> integratorBuilder06 =
                new EulerFieldIntegratorBuilder<>(fieldStep);

        final EulerFieldIntegratorBuilder<Binary64> integratorBuilder06Bis =
                new EulerFieldIntegratorBuilder<>(step);

        final GillFieldIntegratorBuilder<Binary64> integratorBuilder07 =
                new GillFieldIntegratorBuilder<>(fieldStep);

        final GillFieldIntegratorBuilder<Binary64> integratorBuilder07Bis =
                new GillFieldIntegratorBuilder<>(step);

        final HighamHall54FieldIntegratorBuilder<Binary64> integratorBuilder08 =
                new HighamHall54FieldIntegratorBuilder<>(minStep, maxStep, dP);

        final LutherFieldIntegratorBuilder<Binary64> integratorBuilder09 =
                new LutherFieldIntegratorBuilder<>(fieldStep);

        final LutherFieldIntegratorBuilder<Binary64> integratorBuilder09Bis =
                new LutherFieldIntegratorBuilder<>(step);

        final MidpointFieldIntegratorBuilder<Binary64> integratorBuilder10 =
                new MidpointFieldIntegratorBuilder<>(fieldStep);

        final MidpointFieldIntegratorBuilder<Binary64> integratorBuilder10Bis =
                new MidpointFieldIntegratorBuilder<>(step);

        final ThreeEighthesFieldIntegratorBuilder<Binary64> integratorBuilder11 =
                new ThreeEighthesFieldIntegratorBuilder<>(fieldStep);

        final ThreeEighthesFieldIntegratorBuilder<Binary64> integratorBuilder11Bis =
                new ThreeEighthesFieldIntegratorBuilder<>(step);

        // When
        final FieldODEIntegrator<Binary64> builtIntegrator01 =
                integratorBuilder01.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator02 =
                integratorBuilder02.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator03 =
                integratorBuilder03.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator03Bis =
                integratorBuilder03Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator04 =
                integratorBuilder04.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator05 =
                integratorBuilder05.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator06 =
                integratorBuilder06.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator06Bis =
                integratorBuilder06Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator07 =
                integratorBuilder07.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator07Bis =
                integratorBuilder07Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator08 =
                integratorBuilder08.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator09 =
                integratorBuilder09.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator09Bis =
                integratorBuilder09Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator10 =
                integratorBuilder10.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator10Bis =
                integratorBuilder10Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator11 =
                integratorBuilder11.buildIntegrator(orbit, OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> builtIntegrator11Bis =
                integratorBuilder11Bis.buildIntegrator(orbit, OrbitType.CARTESIAN);

        // Then

        // Creating reference integrators
        final double[][] tolerances = NumericalPropagator.tolerances(dP, orbit.toOrbit(), OrbitType.CARTESIAN);

        final FieldODEIntegrator<Binary64> referenceIntegrator01 =
                new AdamsBashforthFieldIntegrator<>(field, nSteps, minStep, maxStep, tolerances[0], tolerances[1]);

        final FieldODEIntegrator<Binary64> referenceIntegrator02 =
                new AdamsMoultonFieldIntegrator<>(field, nSteps, minStep, maxStep, tolerances[0], tolerances[1]);

        final FieldODEIntegrator<Binary64> referenceIntegrator03 =
                new ClassicalRungeKuttaFieldIntegrator<>(field, fieldStep);

        final FieldODEIntegrator<Binary64> referenceIntegrator04 =
                new DormandPrince54FieldIntegrator<>(field, minStep, maxStep, tolerances[0], tolerances[1]);

        final FieldODEIntegrator<Binary64> referenceIntegrator05 =
                new DormandPrince853FieldIntegrator<>(field, minStep, maxStep, tolerances[0], tolerances[1]);

        final FieldODEIntegrator<Binary64> referenceIntegrator06 =
                new EulerFieldIntegrator<>(field, fieldStep);

        final FieldODEIntegrator<Binary64> referenceIntegrator07 =
                new GillFieldIntegrator<>(field, fieldStep);

        final FieldODEIntegrator<Binary64> referenceIntegrator08 =
                new HighamHall54FieldIntegrator<>(field, minStep, maxStep, tolerances[0], tolerances[1]);

        final FieldODEIntegrator<Binary64> referenceIntegrator09 =
                new LutherFieldIntegrator<>(field, fieldStep);

        final FieldODEIntegrator<Binary64> referenceIntegrator10 =
                new MidpointFieldIntegrator<>(field, fieldStep);

        final FieldODEIntegrator<Binary64> referenceIntegrator11 =
                new ThreeEighthesFieldIntegrator<>(field, fieldStep);

        assertBuiltIntegrator(referenceIntegrator01, builtIntegrator01, orbit);
        assertBuiltIntegrator(referenceIntegrator02, builtIntegrator02, orbit);
        assertBuiltIntegrator(referenceIntegrator03, builtIntegrator03, orbit);
        assertBuiltIntegrator(referenceIntegrator03, builtIntegrator03Bis, orbit);
        assertBuiltIntegrator(referenceIntegrator04, builtIntegrator04, orbit);
        assertBuiltIntegrator(referenceIntegrator05, builtIntegrator05, orbit);
        assertBuiltIntegrator(referenceIntegrator06, builtIntegrator06, orbit);
        assertBuiltIntegrator(referenceIntegrator06, builtIntegrator06Bis, orbit);
        assertBuiltIntegrator(referenceIntegrator07, builtIntegrator07, orbit);
        assertBuiltIntegrator(referenceIntegrator07, builtIntegrator07Bis, orbit);
        assertBuiltIntegrator(referenceIntegrator08, builtIntegrator08, orbit);
        assertBuiltIntegrator(referenceIntegrator09, builtIntegrator09, orbit);
        assertBuiltIntegrator(referenceIntegrator09, builtIntegrator09Bis, orbit);
        assertBuiltIntegrator(referenceIntegrator10, builtIntegrator10, orbit);
        assertBuiltIntegrator(referenceIntegrator10, builtIntegrator10Bis, orbit);
        assertBuiltIntegrator(referenceIntegrator11, builtIntegrator11, orbit);
        assertBuiltIntegrator(referenceIntegrator11, builtIntegrator11Bis, orbit);
    }

    @Test
    @DisplayName("Test that an error is thrown if given zero as step size")
    void testErrorThrownWhenGivenZeroStepSize() {
        // Given
        final double step = 0;

        // When & Then
        Assertions.assertThrows(MathIllegalArgumentException.class, () -> new MidpointFieldIntegratorBuilder<>(step));
    }

    private void assertBuiltIntegrator(final FieldODEIntegrator<Binary64> referenceIntegrator,
                                       final FieldODEIntegrator<Binary64> builtIntegrator,
                                       final FieldOrbit<Binary64> initialOrbit) {

        final Field<Binary64> field               = Binary64Field.getInstance();
        final double          propagationDuration = 1200;
        final ForceModel      moonAttraction      = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
        final ForceModel      sunAttraction       = new ThirdBodyAttraction(CelestialBodyFactory.getSun());

        // Create initial state from given initial orbit
        final FieldSpacecraftState<Binary64> initialState = new FieldSpacecraftState<>(initialOrbit);

        // Create propagator with reference integrator and initialize its state
        final FieldNumericalPropagator<Binary64> referencePropagator =
                new FieldNumericalPropagator<>(field, referenceIntegrator);
        referencePropagator.setInitialState(initialState);
        referencePropagator.addForceModel(sunAttraction);
        referencePropagator.addForceModel(moonAttraction);

        // Create propagator with built integrator and initialize its state
        final FieldNumericalPropagator<Binary64> builtPropagator =
                new FieldNumericalPropagator<>(field, builtIntegrator);
        builtPropagator.setInitialState(initialState);
        builtPropagator.addForceModel(sunAttraction);
        builtPropagator.addForceModel(moonAttraction);

        // Propagate
        final FieldSpacecraftState<Binary64> referencePropagatedState =
                referencePropagator.propagate(initialOrbit.getDate().shiftedBy(propagationDuration));
        final FieldOrbit<Binary64> referencePropagatedOrbit = referencePropagatedState.getOrbit();

        final FieldSpacecraftState<Binary64> builtPropagatedState =
                builtPropagator.propagate(initialOrbit.getDate().shiftedBy(propagationDuration));
        final FieldOrbit<Binary64> builtPropagatedOrbit = builtPropagatedState.getOrbit();

        // Assert that results given with reference and built integrators are the same
        final double assertionTolerance = 1e-15;
        Assertions.assertEquals(referencePropagatedOrbit.getA().getReal(), builtPropagatedOrbit.getA().getReal(),
                                assertionTolerance);
        Assertions.assertEquals(referencePropagatedOrbit.getEquinoctialEx().getReal(),
                                builtPropagatedOrbit.getEquinoctialEx().getReal(), assertionTolerance);
        Assertions.assertEquals(referencePropagatedOrbit.getEquinoctialEy().getReal(),
                                builtPropagatedOrbit.getEquinoctialEy().getReal(), assertionTolerance);
        Assertions.assertEquals(referencePropagatedOrbit.getHx().getReal(), builtPropagatedOrbit.getHx().getReal(),
                                assertionTolerance);
        Assertions.assertEquals(referencePropagatedOrbit.getHy().getReal(), builtPropagatedOrbit.getHy().getReal(),
                                assertionTolerance);
        Assertions.assertEquals(referencePropagatedOrbit.getLM().getReal(), builtPropagatedOrbit.getLM().getReal(),
                                assertionTolerance);
    }

    private FieldOrbit<Binary64> getReferenceOrbit() {

        final Field<Binary64> field = Binary64Field.getInstance();

        final FieldAbsoluteDate<Binary64> date  = new FieldAbsoluteDate<>(field);
        final Frame                       frame = FramesFactory.getGCRF();
        final Binary64                    mu    = new Binary64(398600e9);

        final FieldVector3D<Binary64> position = new FieldVector3D<>(new Binary64(6378000 + 400000),
                                                                     new Binary64(0),
                                                                     new Binary64(0));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(new Binary64(5500),
                                                                     new Binary64(5500),
                                                                     new Binary64(0));
        final FieldPVCoordinates<Binary64> pv = new FieldPVCoordinates<>(position, velocity);

        return new FieldCartesianOrbit<>(pv, frame, date, mu);
    }
}
