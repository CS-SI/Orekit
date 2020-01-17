/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.util.MathArrays;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Unit tests for {@link DSSTPartialDerivativesEquations}. */
public class DSSTPartialDerivativesEquationsTest {

    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** Earth gravitational parameter */
    private static final double gm = Constants.EIGEN5C_EARTH_MU;
    /** arbitrary inertial frame */
    private static final Frame eci = FramesFactory.getGCRF();

    /** unused propagator */
    private DSSTPropagator propagator;
    /** mock force model */
    private MockForceModel forceModel;
    /** arbitrary state */
    private SpacecraftState state;
    /** subject under test */
    private DSSTPartialDerivativesEquations pde;

    /**
     * set up {@link #pde} and dependencies.
     *
     */
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        propagator = new DSSTPropagator(new DormandPrince54Integrator(1, 500, 0.001, 0.001));
        forceModel = new MockForceModel();
        propagator.addForceModel(forceModel);
        pde = new DSSTPartialDerivativesEquations("pde", propagator, PropagationType.MEAN);
        final Orbit orbit = new EquinoctialOrbit(4.2163393E7,
                                                 -0.25925449177598586,
                                                 -0.06946703170551687,
                                                 0.15995912655021305,
                                                 -0.5969755874197339,
                                                 15.47576793123677,
                                                 PositionAngle.MEAN,
                                                 eci, date, gm);
        state = new SpacecraftState(orbit).addAdditionalState("pde", new double[2 * 3 * 6]);
        pde.setInitialJacobians(state);

    }

    /**
     * check {@link DSSTPartialDerivativesEquations#computeDerivatives(SpacecraftState,
     * double[])}.
     *
     */
    @Test
    public void testComputeDerivatives() {
        //setup
        double[] pdot = new double[36];

        //action
        pde.computeDerivatives(state, pdot);

        //verify
        assertThat(forceModel.sma.getReal(), is(state.getA()));
        assertThat(forceModel.ex.getReal(),  is(state.getEquinoctialEx()));
        assertThat(forceModel.ey.getReal(),  is(state.getEquinoctialEy()));
        assertThat(forceModel.hx.getReal(),  is(state.getHx()));
        assertThat(forceModel.hy.getReal(),  is(state.getHy()));
        assertThat(forceModel.l.getReal(),   is(state.getLv()));

    }

    /** Mock {@link DSSTForceModel}. */
    private static class MockForceModel implements DSSTForceModel {

        /** semi major axis. */
        public DerivativeStructure sma;

        /**  first component of the eccentricity vector. */
        public DerivativeStructure ex;
        
        /** second component of the eccentricity vector. */
        public DerivativeStructure ey;
        
        /** first component of the inclination vector. */
        public DerivativeStructure hx;
        
        /** second component of the inclination vector. */
        public DerivativeStructure hy;
        
        /** true latitude argument. */
        public DerivativeStructure l;

        @Override
        public List<ShortPeriodTerms> initialize(AuxiliaryElements auxiliaryElements,
                                                 PropagationType type,
                                                 double[] parameters) {
            return new ArrayList<ShortPeriodTerms>();
        }

        @Override
        public <T extends RealFieldElement<T>> List<FieldShortPeriodTerms<T>> initialize(FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                         PropagationType type,
                                                                                         T[] parameters) {
            return new ArrayList<FieldShortPeriodTerms<T>>();
        }
        
        @Override
        public double[] getMeanElementRate(SpacecraftState state,
                                           AuxiliaryElements auxiliaryElements,
                                           double[] parameters) {
            return new double[] {state.getA(),
                                 state.getEquinoctialEx(),
                                 state.getEquinoctialEy(),
                                 state.getHx(),
                                 state.getHy(),
                                 state.getLv()};
        }

        @Override
        public <T extends RealFieldElement<T>> T[] getMeanElementRate(FieldSpacecraftState<T> state,
                                                                      FieldAuxiliaryElements<T> auxiliaryElements,
                                                                      T[] parameters) {
            
            final Field<T> field = state.getDate().getField();
            
            this.sma = (DerivativeStructure) state.getA();
            this.ex  = (DerivativeStructure) state.getEquinoctialEx();
            this.ey  = (DerivativeStructure) state.getEquinoctialEy();
            this.hx  = (DerivativeStructure) state.getHx();
            this.hy  = (DerivativeStructure) state.getHy();
            this.l   = (DerivativeStructure) state.getLv();
            
            final T[] elements = MathArrays.buildArray(field, 6);
            elements[0] = state.getA();
            elements[1] = state.getEquinoctialEx();
            elements[2] = state.getEquinoctialEy();
            elements[3] = state.getHx();
            elements[4] = state.getHy();
            elements[5] = state.getLv();

            return elements;

        }

        @Override
        public EventDetector[] getEventsDetectors() {
            return new EventDetector[0];
        }

        @Override
        public <T extends RealFieldElement<T>> FieldEventDetector<T>[] getFieldEventsDetectors(Field<T> field) {
            return null;
        }

        @Override
        public void registerAttitudeProvider(AttitudeProvider provider) {    
        }

        @Override
        public void updateShortPeriodTerms(double[] parameters, SpacecraftState... meanStates) {           
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T extends RealFieldElement<T>> void updateShortPeriodTerms(T[] parameters,
                                                                           FieldSpacecraftState<T>... meanStates) {
        }

        @Override
        public ParameterDriver[] getParametersDrivers() {
            return new ParameterDriver[0];
        }

    }
}
