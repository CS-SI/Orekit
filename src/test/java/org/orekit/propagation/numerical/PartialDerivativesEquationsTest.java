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
package org.orekit.propagation.numerical;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
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

/** Unit tests for {@link PartialDerivativesEquations}. */
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
     *
     * @throws OrekitException on error
     */
    @Before
    public void setUp() throws OrekitException {
        propagator = new NumericalPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new MockForceModel();
        propagator.addForceModel(forceModel);
        pde = new PartialDerivativesEquations("pde", propagator);
        Vector3D p = new Vector3D(7378137, 0, 0);
        Vector3D v = new Vector3D(0, 7500, 0);
        pv = new PVCoordinates(p, v);
        state = new SpacecraftState(new CartesianOrbit(pv, eci, date, gm))
                .addAdditionalState("pde", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state, 6);

    }

    /**
     * check {@link PartialDerivativesEquations#computeDerivatives(SpacecraftState,
     * double[])} correctly sets the satellite velocity.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testComputeDerivativesStateVelocity() throws OrekitException {
        //setup
        double[] pdot = new double[36];

        //action
        pde.computeDerivatives(state, pdot);

        //verify
        assertThat(forceModel.accelerationDerivativesPosition.toVector3D(), is(pv.getPosition()));
        assertThat(forceModel.accelerationDerivativesVelocity.toVector3D(), is(pv.getVelocity()));

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

        @Override
        public void addContribution(SpacecraftState s, TimeDerivativesEquations adder) throws OrekitException {
        }

        @Override
        public <T extends RealFieldElement<T>> void
            addContribution(FieldSpacecraftState<T> s,
                            FieldTimeDerivativesEquations<T> adder) {
        }

        @Override
        public FieldVector3D<DerivativeStructure> accelerationDerivatives(AbsoluteDate date,
                                                                          Frame frame,
                                                                          FieldVector3D<DerivativeStructure> position,
                                                                          FieldVector3D<DerivativeStructure> velocity,
                                                                          FieldRotation<DerivativeStructure> rotation,
                                                                          DerivativeStructure mass)
            throws OrekitException {
            this.accelerationDerivativesPosition = position;
            this.accelerationDerivativesVelocity = velocity;
            return position;
        }

        @Override
        public FieldVector3D<DerivativeStructure> accelerationDerivatives(SpacecraftState s, String paramName) throws OrekitException {
            return null;
        }

        @Override
        public Stream<EventDetector> getEventsDetectors() {
            return Stream.empty();
        }

        @Override
        public ParameterDriver[] getParametersDrivers() {
            return new ParameterDriver[0];
        }

        @Override
        public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
            return Stream.empty();
        }

    }

}
