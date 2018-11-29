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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

public class EstimatedTroposphericModelTest {

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testMendesPavlis() {

        // Site:   McDonald Observatory
        //         latitude:  30.67166667°
        //         height:    2075 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final AbsoluteDate date = new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(30.67166667);
        final double height      = 2075;
        final double pressure    = 798.4188;
        final double temperature = 300.15;
        final double humidity    = 0.4;
        final double lambda      = 0.532;
        
        final double elevation        = FastMath.toRadians(15.0);
        // Expected mapping factor: 3.80024367 (Ref)
        final double[] expectedMapping = new double[] {
            3.80024367,
            3.80024367
        };
        
        // Test for the second constructor
        final MendesPavlisModel model = new MendesPavlisModel(temperature, pressure,
                                                               humidity, latitude, lambda);

        doTestMappingFactor(expectedMapping, model, height, elevation, date, 5.0e-8);
    }

    @Test
    public void testGMF() {

        // Site (NRAO, Green Bank, WV): latitude:  0.6708665767 radians
        //                              longitude: -1.393397187 radians
        //                              height:    844.715 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)
        //
        // Expected mapping factors : hydrostatic -> 3.425246 (Ref)
        //                                    wet -> 3.449589 (Ref)

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());
        
        final double latitude    = 0.6708665767;
        final double longitude   = -1.393397187;
        final double height      = 844.715;

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425246;
        final double expectedWet   = 3.449589;

        final double[] expectedMappingFactors = new double[] {
            expectedHydro,
            expectedWet
        };

        final MappingFunction model = new GlobalMappingFunctionModel(latitude, longitude);

        doTestMappingFactor(expectedMappingFactors, model, height, elevation, date, 1.0e-6);

    }
    
    @Test
    public void testVMF1() {

        // Site (NRAO, Green Bank, WV): latitude:  38°
        //                              longitude: 280°
        //                              height:    824.17 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref for the inputs:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //                        IERS Technical Note No. 36, BKG (2010)
        //
        // Values: ah  = 0.00127683
        //         aw  = 0.00060955
        //         zhd = 2.0966 m
        //         zwd = 0.2140 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/VMF1_OP/2009/VMFG_20090812.H00
        //
        // Expected mapping factors : hydrostatic -> 3.425088
        //                                    wet -> 3.448300
        //
        // Expected outputs are obtained by performing the Matlab script vmf1_ht.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(38.0);
        final double height      = 824.17;

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425088;
        final double expectedWet   = 3.448300;
        
        final double[] expectedMappingFactors = new double[] {
            expectedHydro,
            expectedWet
        };
        
        final double[] a = { 0.00127683, 0.00060955 };
        final double[] z = {2.0966, 0.2140};
        
        final ViennaOneModel model = new ViennaOneModel(a, z, latitude);

        doTestMappingFactor(expectedMappingFactors, model, height, elevation, date, 4.1e-6);

    }

    private void doTestMappingFactor(final double[] expectedMappingFactors, final MappingFunction model,
                                     final double height, final double elevation, final AbsoluteDate date,
                                     final double precision) {

        final double[] computedMappingFactors = model.mappingFactors(elevation, height, model.getParameters(), date);

        Assert.assertEquals(expectedMappingFactors[0],   computedMappingFactors[0], precision);
        Assert.assertEquals(expectedMappingFactors[1],   computedMappingFactors[1], precision);
    }

    @Test
    public void testZHDParameterDerivative() {
        doTestParametersDerivatives("hydrostatic" + EstimatedTroposphericModel.ZENITH_DELAY, 2.3e-16);
    }

    @Test
    public void testZWDParameterDerivative() {
        doTestParametersDerivatives("wet" + EstimatedTroposphericModel.ZENITH_DELAY, 1.2e-14);
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
        final MappingFunction gmf = new GlobalMappingFunctionModel(latitude, longitude);
        final DiscreteTroposphericModel model = new EstimatedTroposphericModel(gmf, 2.0966, 0.2140);

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

    @Test
    public void testStateDerivativesGMF() {
        final double latitude     = FastMath.toRadians(45.0);
        final double longitude    = FastMath.toRadians(45.0);
        final MappingFunction gmf = new GlobalMappingFunctionModel(latitude, longitude);
        doTestDelayStateDerivatives(gmf, latitude, longitude, 4.8e-11);
    }

    @Test
    public void testStateDerivativesNMF() {
        final double latitude     = FastMath.toRadians(45.0);
        final double longitude    = FastMath.toRadians(45.0);
        final MappingFunction nmf = new NiellMappingFunctionModel(latitude);
        doTestDelayStateDerivatives(nmf, latitude, longitude, 1.7e-11);
    }


    @Test
    public void testStateDerivativesMendesPavlis() {
        final double latitude    = FastMath.toRadians(45.0);
        final double longitude   = FastMath.toRadians(45.0);
        final double pressure    = 798.4188;
        final double temperature = 300.15;
        final double humidity    = 0.4;
        final double lambda      = 0.532;
        final MappingFunction mendesPavlis = new MendesPavlisModel(temperature, pressure, humidity, latitude, lambda);
        doTestDelayStateDerivatives(mendesPavlis, latitude, longitude, 2.4e-11);
    }

    private void doTestDelayStateDerivatives(final MappingFunction func, final double latitude,
                                             final double longitude, final double tolerance) {

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Station
        final GroundStation station = new GroundStation(baseFrame);
        
        // Tropospheric model
        final DiscreteTroposphericModel model = new EstimatedTroposphericModel(func, 2.0966, 0.2140);

        // Derivative Structure
        final DSFactory factory = new DSFactory(6, 1);
        final DerivativeStructure a0       = factory.variable(0, 24464560.0);
        final DerivativeStructure e0       = factory.variable(1, 0.05);
        final DerivativeStructure i0       = factory.variable(2, 0.122138);
        final DerivativeStructure pa0      = factory.variable(3, 3.10686);
        final DerivativeStructure raan0    = factory.variable(4, 1.00681);
        final DerivativeStructure anomaly0 = factory.variable(5, 0.048363);
        final Field<DerivativeStructure> field = a0.getField();
        final DerivativeStructure zero = field.getZero();

        // Field Date
        final FieldAbsoluteDate<DerivativeStructure> dsDate = new FieldAbsoluteDate<>(field);
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

        // Compute Delay with state derivatives
        final DerivativeStructure delay = model.pathDelay(dsElevation, zero, model.getParameters(field), dsDate);

        final double[] compDeriv = delay.getAllDerivatives();

        // Field -> non-field
        final Orbit orbit = dsOrbit.toOrbit();
        final SpacecraftState state = dsState.toSpacecraftState();

        // Finite differences for reference values
        final double[][] refDeriv = new double[1][6];
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngle angleType = PositionAngle.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {
            SpacecraftState stateM4 = shiftState(state, orbitType, angleType, -4 * steps[i], i);
            final Vector3D positionM4 = stateM4.getPVCoordinates().getPosition();
            final double elevationM4  = station.getBaseFrame().getElevation(positionM4, stateM4.getFrame(), stateM4.getDate());
            double  delayM4 = model.pathDelay(elevationM4, height, model.getParameters(), stateM4.getDate());
            
            SpacecraftState stateM3 = shiftState(state, orbitType, angleType, -3 * steps[i], i);
            final Vector3D positionM3 = stateM3.getPVCoordinates().getPosition();
            final double elevationM3  = station.getBaseFrame().getElevation(positionM3, stateM3.getFrame(), stateM3.getDate());
            double  delayM3 = model.pathDelay(elevationM3, height, model.getParameters(), stateM3.getDate());
            
            SpacecraftState stateM2 = shiftState(state, orbitType, angleType, -2 * steps[i], i);
            final Vector3D positionM2 = stateM2.getPVCoordinates().getPosition();
            final double elevationM2  = station.getBaseFrame().getElevation(positionM2, stateM2.getFrame(), stateM2.getDate());
            double  delayM2 = model.pathDelay(elevationM2, height, model.getParameters(), stateM2.getDate());
 
            SpacecraftState stateM1 = shiftState(state, orbitType, angleType, -1 * steps[i], i);
            final Vector3D positionM1 = stateM1.getPVCoordinates().getPosition();
            final double elevationM1  = station.getBaseFrame().getElevation(positionM1, stateM1.getFrame(), stateM1.getDate());
            double  delayM1 = model.pathDelay(elevationM1, height, model.getParameters(), stateM1.getDate());
           
            SpacecraftState stateP1 = shiftState(state, orbitType, angleType, 1 * steps[i], i);
            final Vector3D positionP1 = stateP1.getPVCoordinates().getPosition();
            final double elevationP1  = station.getBaseFrame().getElevation(positionP1, stateP1.getFrame(), stateP1.getDate());
            double  delayP1 = model.pathDelay(elevationP1, height, model.getParameters(), stateP1.getDate());
            
            SpacecraftState stateP2 = shiftState(state, orbitType, angleType, 2 * steps[i], i);
            final Vector3D positionP2 = stateP2.getPVCoordinates().getPosition();
            final double elevationP2  = station.getBaseFrame().getElevation(positionP2, stateP2.getFrame(), stateP2.getDate());
            double  delayP2 = model.pathDelay(elevationP2, height, model.getParameters(), stateP2.getDate());
            
            SpacecraftState stateP3 = shiftState(state, orbitType, angleType, 3 * steps[i], i);
            final Vector3D positionP3 = stateP3.getPVCoordinates().getPosition();
            final double elevationP3  = station.getBaseFrame().getElevation(positionP3, stateP3.getFrame(), stateP3.getDate());
            double  delayP3 = model.pathDelay(elevationP3, height, model.getParameters(), stateP3.getDate());
            
            SpacecraftState stateP4 = shiftState(state, orbitType, angleType, 4 * steps[i], i);
            final Vector3D positionP4 = stateP4.getPVCoordinates().getPosition();
            final double elevationP4  = station.getBaseFrame().getElevation(positionP4, stateP4.getFrame(), stateP4.getDate());
            double  delayP4 = model.pathDelay(elevationP4, height, model.getParameters(), stateP4.getDate());
            
            fillJacobianColumn(refDeriv, i, orbitType, angleType, steps[i],
                               delayM4, delayM3, delayM2, delayM1,
                               delayP1, delayP2, delayP3, delayP4);
        }

        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(compDeriv[i + 1], refDeriv[0][i], tolerance);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType, true);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                  boolean withMass) {
        double[][] array = new double[2][withMass ? 7 : 6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        if (withMass) {
            array[0][6] = state.getMass();
        }
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngle angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[0][6]);
    }

}
