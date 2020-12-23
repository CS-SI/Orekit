/* Copyright 2002-2020 CS GROUP
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Test for AbstractConstantThrust class and its sub-classes. */
public class AbstractConstantThrustTest {

    /** Test non-abstract methods of constant thrust model. */
    @Test
    public void testNonAbstractMethods() {
        
        final double thrust = 1.;
        final double isp = 300.;
        final Vector3D direction = Vector3D.PLUS_I;
        final String name = "man";
        
        final Vector3D thrustVector = direction.scalarMultiply(thrust);
        final double flowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        
        // A simple model without any parameter driver
        final AbstractConstantThrustPropulsionModel model =
                        new AbstractConstantThrustPropulsionModel(thrust, isp, direction, name) {
            
            @Override
            public <T extends RealFieldElement<T>> FieldVector3D<T> getThrustVector(T[] parameters) {
                return new FieldVector3D<T>(parameters[0].getField(), thrustVector);
            }
            
            @Override
            public Vector3D getThrustVector(double[] parameters) {
                return thrustVector;
            }
            
            @Override
            public Vector3D getThrustVector() {
                return thrustVector;
            }
            
            @Override
            public <T extends RealFieldElement<T>> T getFlowRate(T[] parameters) {
                return parameters[0].getField().getZero().add(flowRate);
            }
            
            @Override
            public double getFlowRate(double[] parameters) {
                return flowRate;
            }
            
            @Override
            public double getFlowRate() {
                return flowRate;
            }
        };
        
        // Test non-abstract methods
        Assert.assertEquals(0, model.getParametersDrivers().length);
        Assert.assertEquals(name, model.getName());
        Assert.assertEquals(isp , model.getIsp(), 0.);
        Assert.assertArrayEquals(direction.toArray(), model.getDirection().toArray(), 0.);
        Assert.assertEquals(thrust, model.getThrust(), 0.);
        
        // Dummy spacecraft state        
        Orbit orbit =  new CircularOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                         FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState s = new SpacecraftState(orbit);

        Assert.assertArrayEquals(thrustVector.toArray(), model.getThrustVector(s).toArray(), 0.);
        Assert.assertArrayEquals(thrustVector.toArray(), model.getThrustVector(s, new double[] {1.}).toArray(), 0.);
        Assert.assertEquals(flowRate, model.getFlowRate(s), 0.);
        Assert.assertEquals(flowRate, model.getFlowRate(s, new double[] {0.}), 0.);
        
        // Dummy DS factory
        DSFactory factory = new DSFactory(1, 1);
        DerivativeStructure ds = factory.build(1., 1.);
        DerivativeStructure[] dsArray = new DerivativeStructure[] {ds};
        FieldSpacecraftState<DerivativeStructure> fs = new FieldSpacecraftState<>(ds.getField(), s);
        
        // Thrust DS        
        Assert.assertEquals(thrustVector.getX(), model.getThrustVector(fs, dsArray).getX().getReal(), 0.);
        Assert.assertEquals(0., model.getThrustVector(fs, dsArray).getX().getPartialDerivative(1), 0.);
        Assert.assertEquals(thrustVector.getY(), model.getThrustVector(fs, dsArray).getY().getReal(), 0.);
        Assert.assertEquals(0., model.getThrustVector(fs, dsArray).getY().getPartialDerivative(1), 0.);
        Assert.assertEquals(thrustVector.getZ(), model.getThrustVector(fs, dsArray).getZ().getReal(), 0.);
        Assert.assertEquals(0., model.getThrustVector(fs, dsArray).getZ().getPartialDerivative(1), 0.);
        
        // Flow rate DS      
        Assert.assertEquals(flowRate, model.getFlowRate(fs, dsArray).getReal(), 0.);
        Assert.assertEquals(0., model.getFlowRate(fs, dsArray).getPartialDerivative(1), 0.);
    }
    
    /** Test the 1-dimensional constant thrust model. */
    @Test
    public void testConstantThrust1D() {
        
        final double thrust = 1.;
        final double isp = 300.;
        final Vector3D direction = new Vector3D(FastMath.PI / 3., FastMath.PI / 4);
        final String name = "man-1";
        
        // 1D constant thrust model
        final BasicConstantThrustPropulsionModel model =
                        new BasicConstantThrustPropulsionModel(thrust, isp, direction, name);
        ParameterDriver[] drivers = model.getParametersDrivers();
        
        // References
        final Vector3D refThrustVector = direction.scalarMultiply(thrust);
        final double refFlowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        
        
        // Thrust & flow rate
        final double mult = 10.;
        Assert.assertArrayEquals(direction.toArray(), model.getDirection().toArray(), 0.);
        Assert.assertArrayEquals(refThrustVector.toArray(), model.getThrustVector().toArray(), 0.);
        Assert.assertArrayEquals(refThrustVector.scalarMultiply(mult).toArray(),
                                 model.getThrustVector(new double[] {mult * thrust, mult * refFlowRate}).toArray(), 0.);
        Assert.assertEquals(refFlowRate, model.getFlowRate(), 0.);
        Assert.assertEquals(mult * refFlowRate, model.getFlowRate(new double[] {mult * thrust, mult * refFlowRate}), 0.);
        
        // Drivers
        Assert.assertEquals(2, drivers.length);
        Assert.assertEquals(name + BasicConstantThrustPropulsionModel.THRUST, drivers[0].getName());
        Assert.assertEquals(name + BasicConstantThrustPropulsionModel.FLOW_RATE, drivers[1].getName());
        Assert.assertEquals(thrust, drivers[0].getValue(), 0.);
        Assert.assertEquals(refFlowRate, drivers[1].getValue(), 0.);
        
        // Thrust DS
        final DSFactory factory = new DSFactory(2, 1);
        DerivativeStructure t = factory.build(mult * thrust, 1., 0.);
        DerivativeStructure f = factory.build(mult * refFlowRate, 0., 1.);
        
        DerivativeStructure[] dsArray = new DerivativeStructure[] {t, f};

        Assert.assertEquals(t.getReal() * direction.getX(), model.getThrustVector(dsArray).getX().getReal(), 0.);
        Assert.assertEquals(direction.getX(), model.getThrustVector(dsArray).getX().getPartialDerivative(1, 0), 0.);
        Assert.assertEquals(0., model.getThrustVector(dsArray).getX().getPartialDerivative(0, 1), 0.);
        
        Assert.assertEquals(t.getReal() * direction.getY(), model.getThrustVector(dsArray).getY().getReal(), 0.);
        Assert.assertEquals(direction.getY(), model.getThrustVector(dsArray).getY().getPartialDerivative(1, 0), 0.);
        Assert.assertEquals(0., model.getThrustVector(dsArray).getY().getPartialDerivative(0, 1), 0.);
        
        Assert.assertEquals(t.getReal() * direction.getZ(), model.getThrustVector(dsArray).getZ().getReal(), 0.);
        Assert.assertEquals(direction.getZ(), model.getThrustVector(dsArray).getZ().getPartialDerivative(1, 0), 0.);
        Assert.assertEquals(0., model.getThrustVector(dsArray).getZ().getPartialDerivative(0, 1), 0.);
        
        // Flow rate DS      
        Assert.assertEquals(f.getReal(), model.getFlowRate(dsArray).getReal(), 0.);
        Assert.assertEquals(0., model.getFlowRate(dsArray).getPartialDerivative(1, 0), 0.);
        Assert.assertEquals(1., model.getFlowRate(dsArray).getPartialDerivative(0, 1), 0.);
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
      ParameterDriver[] drivers = mod1.getParametersDrivers();
      
      // References
      final Vector3D refThrustVector = direction.scalarMultiply(thrust);
      final double refFlowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
      
      
      // Thrust & flow rate
      Assert.assertArrayEquals(direction.toArray(), mod1.getDirection().toArray(), 0.);
      Assert.assertArrayEquals(refThrustVector.toArray(), mod1.getThrustVector().toArray(), 0.);
      Assert.assertArrayEquals(refThrustVector.toArray(), mod1.getThrustVector(new double[] {1., 1., 1.}).toArray(), 0.);
      Assert.assertEquals(refFlowRate, mod1.getFlowRate(), 0.);
      Assert.assertEquals(refFlowRate, mod1.getFlowRate(new double[] {0.}), 0.);
      
      // Drivers
      Assert.assertEquals(3, drivers.length);
      Assert.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTX_SCALE_FACTOR, drivers[0].getName());
      Assert.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTY_SCALE_FACTOR, drivers[1].getName());
      Assert.assertEquals(name + ScaledConstantThrustPropulsionModel.THRUSTZ_SCALE_FACTOR, drivers[2].getName());
      Assert.assertEquals(1., drivers[0].getValue(), 0.);
      Assert.assertEquals(1., drivers[1].getValue(), 0.);
      Assert.assertEquals(1., drivers[2].getValue(), 0.);
      
      // Thrust DS
      final DSFactory factory = new DSFactory(3, 1);
      DerivativeStructure fx = factory.build(1., 1., 0., 0.);
      DerivativeStructure fy = factory.build(0.5, 0., 1., 0.);
      DerivativeStructure fz = factory.build(1.5, 0., 0., 1.);
      
      DerivativeStructure[] fArray = new DerivativeStructure[] {fx, fy, fz};

      Assert.assertEquals(fx.getReal() * refThrustVector.getX(), mod1.getThrustVector(fArray).getX().getReal(), 0.);
      Assert.assertEquals(refThrustVector.getX(), mod1.getThrustVector(fArray).getX().getPartialDerivative(1, 0, 0), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getX().getPartialDerivative(0, 1, 0), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getX().getPartialDerivative(0, 0, 1), 0.);
      
      Assert.assertEquals(fy.getReal() * refThrustVector.getY(), mod1.getThrustVector(fArray).getY().getReal(), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getY().getPartialDerivative(1, 0, 0), 0.);
      Assert.assertEquals(refThrustVector.getY(), mod1.getThrustVector(fArray).getY().getPartialDerivative(0, 1, 0), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getY().getPartialDerivative(0, 0, 1), 0.);
      
      Assert.assertEquals(fz.getReal() * refThrustVector.getZ(), mod1.getThrustVector(fArray).getZ().getReal(), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getZ().getPartialDerivative(1, 0, 0), 0.);
      Assert.assertEquals(0., mod1.getThrustVector(fArray).getZ().getPartialDerivative(0, 1, 0), 0.);
      Assert.assertEquals(refThrustVector.getZ(), mod1.getThrustVector(fArray).getZ().getPartialDerivative(0, 0, 1), 0.);
      
      // Flow rate DS      
      Assert.assertEquals(refFlowRate, mod1.getFlowRate(fArray).getReal(), 0.);
      Assert.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(1, 0, 0), 0.);
      Assert.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(0, 1, 0), 0.);
      Assert.assertEquals(0., mod1.getFlowRate(fArray).getPartialDerivative(0, 1, 0), 0.);
  }
}
