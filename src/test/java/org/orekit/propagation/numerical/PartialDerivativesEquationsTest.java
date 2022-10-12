/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation.numerical;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;

/** Unit tests for {@link PartialDerivativesEquations}. */
@Deprecated
public class PartialDerivativesEquationsTest {

    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** Earth gravitational parameter */
    private static final double gm = Constants.EIGEN5C_EARTH_MU;
    /** arbitrary inertial frame */
    private static final Frame eci = FramesFactory.getGCRF();

    /** unused propagator */
    private NumericalPropagator propagator;
    /** mock force model */
    private MockForceModel forceModel;
    /** arbitrary PV */
    private PVCoordinates pv;
    /** arbitrary state */
    private SpacecraftState state;
    /** subject under test */
    private PartialDerivativesEquations pde;

    /**
     * set up {@link #pde} and dependencies.
     */
    @BeforeEach
    public void setUp() {
        propagator = new NumericalPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new MockForceModel();
        propagator.addForceModel(forceModel);
        pde = new PartialDerivativesEquations("pde", propagator);
        Vector3D p = new Vector3D(7378137, 0, 0);
        Vector3D v = new Vector3D(0, 7500, 0);
        pv = new PVCoordinates(p, v);
        state = new SpacecraftState(new CartesianOrbit(pv, eci, date, gm))
                .addAdditionalState("pde", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state);

    }

    /**
     * check {@link PartialDerivativesEquations#derivatives(SpacecraftState)} correctly sets the satellite velocity.
     */
    @Test
    public void testDerivativesStateVelocity() {
        //action
        pde.derivatives(state);

        //verify
        MatcherAssert.assertThat(forceModel.accelerationDerivativesPosition.toVector3D(), is(pv.getPosition()));
        MatcherAssert.assertThat(forceModel.accelerationDerivativesVelocity.toVector3D(), is(pv.getVelocity()));

    }

    /** Mock {@link ForceModel}. */
    private static class MockForceModel extends AbstractForceModel {

        /**
         * argument for {@link #accelerationDerivatives(AbsoluteDate, Frame,
         * FieldVector3D, FieldVector3D, FieldRotation, DerivativeStructure)}.
         */
        public FieldVector3D<DerivativeStructure> accelerationDerivativesPosition;
        /**
         * argument for {@link #accelerationDerivatives(AbsoluteDate, Frame,
         * FieldVector3D, FieldVector3D, FieldRotation, DerivativeStructure)}.
         */
        public FieldVector3D<DerivativeStructure> accelerationDerivativesVelocity;

        /** {@inheritDoc} */
        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public <T extends CalculusFieldElement<T>> void
            addContribution(FieldSpacecraftState<T> s,
                            FieldTimeDerivativesEquations<T> adder) {
        }

        @Override
        public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
            {
            return s.getPVCoordinates().getPosition();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {
            this.accelerationDerivativesPosition = (FieldVector3D<DerivativeStructure>) s.getPVCoordinates().getPosition();
            this.accelerationDerivativesVelocity = (FieldVector3D<DerivativeStructure>) s.getPVCoordinates().getVelocity();
            return s.getPVCoordinates().getPosition();
        }

        @Override
        public Stream<EventDetector> getEventsDetectors() {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
            return Stream.empty();
        }

    }

}
