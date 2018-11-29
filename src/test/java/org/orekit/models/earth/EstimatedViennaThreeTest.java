/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.models.earth;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class EstimatedViennaThreeTest {

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testZHDParameterDerivative() {
        doTestParametersDerivatives("hydrostatic" + EstimatedViennaOneModel.ZENITH_DELAY, 2.3e-16);
    }

    @Test
    public void testZWDParameterDerivative() {
        doTestParametersDerivatives("wet" + EstimatedViennaOneModel.ZENITH_DELAY, 5.4e-15);
    }

    @Test
    public void testAHParameterDerivative() {
        doTestParametersDerivatives(EstimatedViennaOneModel.COEFFICIENTS + " ah", 3.7e-13);
    }

    @Test
    public void testAWParameterDerivative() {
        doTestParametersDerivatives(EstimatedViennaOneModel.COEFFICIENTS + " aw", 2.3e-12);
    }

    private void doTestParametersDerivatives(String parameterName, double tolerance) {

        // Geodetic point
        final double latitude     = FastMath.toRadians(45.0);
        final double longitude    = FastMath.toRadians(45.0);
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");
        
        // Tropospheric model
        final DiscreteTroposphericModel model = new EstimatedViennaThreeModel(2.0966, 0.2140, 0.00127683, 0.00060955, latitude, longitude);

        // Set Parameter Driver
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            driver.setValue(driver.getReferenceValue());
            driver.setSelected(driver.getName().equals(parameterName));
        }

        // Count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }

        // Derivative Structure
        final DSFactory factory = new DSFactory(6 + nbParams, 1);
        final DerivativeStructure a0       = factory.variable(0, 24464560.0);
        final DerivativeStructure e0       = factory.variable(1, 0.05);
        final DerivativeStructure i0       = factory.variable(2, 0.122138);
        final DerivativeStructure pa0      = factory.variable(3, 3.10686);
        final DerivativeStructure raan0    = factory.variable(4, 1.00681);
        final DerivativeStructure anomaly0 = factory.variable(5, 0.048363);
        final Field<DerivativeStructure> field = a0.getField();
        final DerivativeStructure zero = field.getZero();

        // Field Date
        final FieldAbsoluteDate<DerivativeStructure> dsDate = new FieldAbsoluteDate<>(field, 2018, 11, 19, 18, 0, 0.0,
                                                                                      TimeScalesFactory.getUTC());
        // Field Orbit
        final Frame frame = FramesFactory.getEME2000();
        final FieldOrbit<DerivativeStructure> dsOrbit = new FieldKeplerianOrbit<>(a0, e0, i0, pa0, raan0, anomaly0,
                                                                                  PositionAngle.MEAN, frame,
                                                                                  dsDate, 3.9860047e14);
        // Field State
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<>(dsOrbit);

        // Initial satellite elevation
        final FieldVector3D<DerivativeStructure> position = dsState.getPVCoordinates().getPosition();
        final FieldTransform<DerivativeStructure> t = dsState.getFrame().getTransformTo(baseFrame, dsDate);
        final FieldVector3D<DerivativeStructure> extPointTopo = t.transformPosition(position);
        final DerivativeStructure dsElevation = extPointTopo.getDelta();

        // Add parameter as a variable
        final ParameterDriver[] drivers = model.getParametersDrivers();
        final DerivativeStructure[] parameters = new DerivativeStructure[drivers.length];
        int index = 6;
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].isSelected() ?
                            factory.variable(index++, drivers[i].getValue()) :
                            factory.constant(drivers[i].getValue());
        }

        // Compute delay state derivatives
        final DerivativeStructure delay = model.pathDelay(dsElevation, zero, parameters, dsDate);

        final double[] compDeriv = delay.getAllDerivatives(); 

        // Field -> non-field
        final SpacecraftState state = dsState.toSpacecraftState();
        final double elevation = dsElevation.getReal();

        // Finite differences for reference values
        final double[][] refDeriv = new double[1][1];
        ParameterDriversList bound = new ParameterDriversList();
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            if (driver.getName().equals(parameterName)) {
                driver.setSelected(true);
                bound.add(driver);
            } else {
                driver.setSelected(false);
            }
        }
        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();

        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngle angleType = PositionAngle.MEAN;

        selected.setValue(p0 - 4 * h);
        double  delayM4 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());
        
        selected.setValue(p0 - 3 * h);
        double  delayM3 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());
        
        selected.setValue(p0 - 2 * h);
        double  delayM2 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());

        selected.setValue(p0 - 1 * h);
        double  delayM1 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());

        selected.setValue(p0 + 1 * h);
        double  delayP1 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());

        selected.setValue(p0 + 2 * h);
        double  delayP2 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());

        selected.setValue(p0 + 3 * h);
        double  delayP3 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());

        selected.setValue(p0 + 4 * h);
        double  delayP4 = model.pathDelay(elevation, height, model.getParameters(), state.getDate());
            
        fillJacobianColumn(refDeriv, 0, orbitType, angleType, h,
                           delayM4, delayM3, delayM2, delayM1,
                           delayP1, delayP2, delayP3, delayP4);

        Assert.assertEquals(compDeriv[7], refDeriv[0][0], tolerance);

    }

    private void fillJacobianColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngle angleType, double h,
                                    double sM4h, double sM3h,
                                    double sM2h, double sM1h,
                                    double sP1h, double sP2h,
                                    double sP3h, double sP4h) {

        jacobian[0][column] = ( -3 * (sP4h - sM4h) +
                                32 * (sP3h - sM3h) -
                               168 * (sP2h - sM2h) +
                               672 * (sP1h - sM1h)) / (840 * h);
    }

}
