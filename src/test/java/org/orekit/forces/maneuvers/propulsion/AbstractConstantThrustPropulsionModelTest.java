/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces.maneuvers.propulsion;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Test for AbstractConstantThrustPropulsionModel class and its sub-classes. */
public class AbstractConstantThrustPropulsionModelTest {

    /** Test non-abstract methods of constant thrust model. */
    @Test
    public void testNonAbstractMethods() {

        final double thrust = 1.;
        final double isp = 300.;
        final Vector3D direction = Vector3D.PLUS_I;
        final String name = "man";

        final Vector3D thrustVector = direction.scalarMultiply(thrust);
        final Control3DVectorCostType control3DVectorCostType = Control3DVectorCostType.INF_NORM;
        final double flowRate = -control3DVectorCostType.evaluate(thrustVector) / (Constants.G0_STANDARD_GRAVITY * isp);

        // A simple model without any parameter driver
        final AbstractConstantThrustPropulsionModel model =
                        new AbstractConstantThrustPropulsionModel(thrust, isp, direction, control3DVectorCostType, name) {

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(T[] parameters) {
                return new FieldVector3D<T>(parameters[0].getField(), thrustVector);
            }

            @Override
            public Vector3D getThrustVector(double[] parameters) {
                return thrustVector;
            }

            @Override
            public Vector3D getThrustVector(AbsoluteDate date) {
                return thrustVector;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T getFlowRate(T[] parameters) {
                return parameters[0].getField().getZero().add(flowRate);
            }

            @Override
            public double getFlowRate(double[] parameters) {
                return flowRate;
            }

            @Override
            public double getFlowRate(AbsoluteDate date) {
                return flowRate;
            }

			@Override
			public Vector3D getThrustVector() {
				return thrustVector;
			}

			@Override
			public double getFlowRate() {
				return flowRate;
			}

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return Collections.emptyList();
            }
        };

        // Test non-abstract methods
        Assertions.assertEquals(0, model.getParametersDrivers().size());
        Assertions.assertEquals(name, model.getName());
        Assertions.assertEquals(isp, model.getIsp(), 0.);
        Assertions.assertArrayEquals(direction.toArray(), model.getDirection().toArray(), 0.);
        Assertions.assertEquals(thrust, model.getThrustMagnitude(), 0.);

        // Dummy spacecraft state
        Orbit orbit =  new CircularOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                         FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);

        Assertions.assertArrayEquals(thrustVector.toArray(), model.getThrustVector(s).toArray(), 0.);
        Assertions.assertArrayEquals(thrustVector.toArray(), model.getThrustVector(s, new double[] {1.}).toArray(), 0.);
        Assertions.assertEquals(flowRate, model.getFlowRate(s), 0.);
        Assertions.assertEquals(flowRate, model.getFlowRate(s, new double[] {0.}), 0.);

        // Dummy DS factory
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure ds = factory.build(1., 1.);
        DerivativeStructure[] dsArray = new DerivativeStructure[] {ds};
        FieldSpacecraftState<DerivativeStructure> fs = new FieldSpacecraftState<>(ds.getField(), s);

        // Thrust DS
        Assertions.assertEquals(thrustVector.getX(), model.getThrustVector(fs, dsArray).getX().getReal(), 0.);
        Assertions.assertEquals(0., model.getThrustVector(fs, dsArray).getX().getPartialDerivative(1), 0.);
        Assertions.assertEquals(thrustVector.getY(), model.getThrustVector(fs, dsArray).getY().getReal(), 0.);
        Assertions.assertEquals(0., model.getThrustVector(fs, dsArray).getY().getPartialDerivative(1), 0.);
        Assertions.assertEquals(thrustVector.getZ(), model.getThrustVector(fs, dsArray).getZ().getReal(), 0.);
        Assertions.assertEquals(0., model.getThrustVector(fs, dsArray).getZ().getPartialDerivative(1), 0.);

        // Flow rate DS
        Assertions.assertEquals(flowRate, model.getFlowRate(fs, dsArray).getReal(), 0.);
        Assertions.assertEquals(0., model.getFlowRate(fs, dsArray).getPartialDerivative(1), 0.);
    }

    /** Test the 1-dimensional constant thrust model. */
    @Test
    public void testConstantThrust1D() {

        final double thrust = 1.;
        final double isp = 300.;
        final Vector3D direction = new Vector3D(FastMath.PI / 3., FastMath.PI / 4);
        final String name = "man-1";

        // 1D constant thrust model
        final Control3DVectorCostType control3DVectorCostType = Control3DVectorCostType.ONE_NORM;
        final BasicConstantThrustPropulsionModel model =
                        new BasicConstantThrustPropulsionModel(thrust, isp, direction, control3DVectorCostType, name);
        List<ParameterDriver> drivers = model.getParametersDrivers();

        // References
        final Vector3D refThrustVector = direction.scalarMultiply(thrust);
        final double refFlowRate = -control3DVectorCostType.evaluate(refThrustVector) / (Constants.G0_STANDARD_GRAVITY * isp);


        // Thrust & flow rate
        final double mult = 10.;
        Assertions.assertArrayEquals(direction.toArray(), model.getDirection().toArray(), 0.);
        Assertions.assertArrayEquals(refThrustVector.toArray(), model.getThrustVector().toArray(), 0.);
        Assertions.assertArrayEquals(refThrustVector.scalarMultiply(mult).toArray(),
                model.getThrustVector(new double[] {mult * thrust, mult * refFlowRate}).toArray(), 0.);
        Assertions.assertEquals(refFlowRate, model.getFlowRate(), 0.);
        Assertions.assertEquals(mult * refFlowRate, model.getFlowRate(new double[] {mult * thrust, mult * refFlowRate}),
                0.);

        // Drivers
        Assertions.assertEquals(2, drivers.size());
        Assertions.assertEquals(name + BasicConstantThrustPropulsionModel.THRUST, drivers.get(0).getName());
        Assertions.assertEquals(name + BasicConstantThrustPropulsionModel.FLOW_RATE, drivers.get(1).getName());
        Assertions.assertEquals(thrust, drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(refFlowRate, drivers.get(1).getValue(), 0.);

        // Thrust DS
        final DSFactory factory = new DSFactory(2, 1);
        DerivativeStructure t = factory.build(mult * thrust, 1., 0.);
        DerivativeStructure f = factory.build(mult * refFlowRate, 0., 1.);

        DerivativeStructure[] dsArray = new DerivativeStructure[] {t, f};

        Assertions.assertEquals(t.getReal() * direction.getX(), model.getThrustVector(dsArray).getX().getReal(), 0.);
        Assertions.assertEquals(direction.getX(), model.getThrustVector(dsArray).getX().getPartialDerivative(1, 0), 0.);
        Assertions.assertEquals(0., model.getThrustVector(dsArray).getX().getPartialDerivative(0, 1), 0.);

        Assertions.assertEquals(t.getReal() * direction.getY(), model.getThrustVector(dsArray).getY().getReal(), 0.);
        Assertions.assertEquals(direction.getY(), model.getThrustVector(dsArray).getY().getPartialDerivative(1, 0), 0.);
        Assertions.assertEquals(0., model.getThrustVector(dsArray).getY().getPartialDerivative(0, 1), 0.);

        Assertions.assertEquals(t.getReal() * direction.getZ(), model.getThrustVector(dsArray).getZ().getReal(), 0.);
        Assertions.assertEquals(direction.getZ(), model.getThrustVector(dsArray).getZ().getPartialDerivative(1, 0), 0.);
        Assertions.assertEquals(0., model.getThrustVector(dsArray).getZ().getPartialDerivative(0, 1), 0.);

        // Flow rate DS
        Assertions.assertEquals(f.getReal(), model.getFlowRate(dsArray).getReal(), 0.);
        Assertions.assertEquals(0., model.getFlowRate(dsArray).getPartialDerivative(1, 0), 0.);
        Assertions.assertEquals(1., model.getFlowRate(dsArray).getPartialDerivative(0, 1), 0.);
    }

  /** Test the 3-dimensional "scaled" constant thrust model. */
  @Test
  public void testConstantThrust3DScaled() {

      final double thrust = 1.;
      final double isp = 300.;
      final Vector3D direction = new Vector3D(FastMath.PI / 3., FastMath.PI / 4);
      final String name = "man-1";

      // 3D "scaled" constant thrust model
      final ScaledConstantThrustPropulsionModel mod1 =
                      new ScaledConstantThrustPropulsionModel(thrust, isp, direction, name);
      List<ParameterDriver> drivers = mod1.getParametersDrivers();

      // References
      final Vector3D refThrustVector = direction.scalarMultiply(thrust);
      final Control3DVectorCostType control3DVectorCostType = Control3DVectorCostType.TWO_NORM;
      final double refFlowRate = -control3DVectorCostType.evaluate(refThrustVector) / (Constants.G0_STANDARD_GRAVITY * isp);


      // Thrust & flow rate
      Assertions.assertArrayEquals(direction.toArray(), mod1.getDirection().toArray(), 0.);
      Assertions.assertArrayEquals(refThrustVector.toArray(), mod1.getThrustVector().toArray(), 0.);
      Assertions.assertArrayEquals(refThrustVector.toArray(), mod1.getThrustVector(new double[] {1., 1., 1.}).toArray(),
              0.);
      Assertions.assertEquals(refFlowRate, mod1.getFlowRate(), 0.);
      Assertions.assertEquals(refFlowRate, mod1.getFlowRate(new double[] {0.}), 0.);

      // Drivers
      Assertions.assertEquals(3, drivers.size());
      Assertions.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTX_SCALE_FACTOR,
              drivers.get(0).getName());
      Assertions.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTY_SCALE_FACTOR,
              drivers.get(1).getName());
      Assertions.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTZ_SCALE_FACTOR,
              drivers.get(2).getName());
      Assertions.assertEquals(1., drivers.get(0).getValue(), 0.);
      Assertions.assertEquals(1., drivers.get(1).getValue(), 0.);
      Assertions.assertEquals(1., drivers.get(2).getValue(), 0.);

      // Thrust DS
      final DSFactory factory = new DSFactory(3, 1);
      DerivativeStructure fx = factory.build(1., 1., 0., 0.);
      DerivativeStructure fy = factory.build(0.5, 0., 1., 0.);
      DerivativeStructure fz = factory.build(1.5, 0., 0., 1.);

      DerivativeStructure[] fArray = new DerivativeStructure[] {fx, fy, fz};

      Assertions.assertEquals(fx.getReal() * refThrustVector.getX(), mod1.getThrustVector(fArray).getX().getReal(), 0.);
      Assertions.assertEquals(refThrustVector.getX(), mod1.getThrustVector(fArray).getX().getPartialDerivative(1, 0, 0),
              0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getX().getPartialDerivative(0, 1, 0), 0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getX().getPartialDerivative(0, 0, 1), 0.);

      Assertions.assertEquals(fy.getReal() * refThrustVector.getY(), mod1.getThrustVector(fArray).getY().getReal(), 0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getY().getPartialDerivative(1, 0, 0), 0.);
      Assertions.assertEquals(refThrustVector.getY(), mod1.getThrustVector(fArray).getY().getPartialDerivative(0, 1, 0),
              0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getY().getPartialDerivative(0, 0, 1), 0.);

      Assertions.assertEquals(fz.getReal() * refThrustVector.getZ(), mod1.getThrustVector(fArray).getZ().getReal(), 0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getZ().getPartialDerivative(1, 0, 0), 0.);
      Assertions.assertEquals(0., mod1.getThrustVector(fArray).getZ().getPartialDerivative(0, 1, 0), 0.);
      Assertions.assertEquals(refThrustVector.getZ(), mod1.getThrustVector(fArray).getZ().getPartialDerivative(0, 0, 1),
              0.);

      // Flow rate DS
      Assertions.assertEquals(refFlowRate, mod1.getFlowRate(fArray).getReal(), 0.);
      Assertions.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(1, 0, 0), 0.);
      Assertions.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(0, 1, 0), 0.);
      Assertions.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(0, 1, 0), 0.);
  }
}
