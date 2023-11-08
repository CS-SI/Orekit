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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
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

import java.util.List;

public class EstimatedIonosphericModelTest {

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testL1GPS() {
        // Model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction(0.0);
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 1.0);
        // Delay
        final double delay = model.pathDelay(0.5 * FastMath.PI,
                                             Frequency.G01.getMHzFrequency() * 1.0e6,
                                             model.getParameters(new AbsoluteDate()));
        // Verify
        Assertions.assertEquals(0.162, delay, 0.001);
    }

    @Test
    public void testFieldL1GPS() {
        doTestFieldL1GPS(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldL1GPS(final Field<T> field) {
        // Zero
        final T zero = field.getZero();
        // Model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction(0.0);
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 1.0);
        // Delay
        final T delay = model.pathDelay(zero.add(0.5 * FastMath.PI),
                                        Frequency.G01.getMHzFrequency() * 1.0e6,
                                        model.getParameters(field));
        // Verify
        Assertions.assertEquals(0.162, delay.getReal(), 0.001);
    }

    @Test
    public void testDelay() {
        final double elevation = 70.;

        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 50.0);
        
        // the pamater driver has no validity period, so only 1 values estimated over
        // the all period, that is why getParameters is called with no argument
        double delayMeters = model.pathDelay(FastMath.toRadians(elevation),
                                             Frequency.G01.getMHzFrequency() * 1.0e6,
                                             model.getParameters());

        Assertions.assertTrue(Precision.compareTo(delayMeters, 12., 1.0e-6) < 0);
        Assertions.assertTrue(Precision.compareTo(delayMeters, 0.,  1.0e-6) > 0);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final double elevation = 70.;

        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 50.0);
        T zero = field.getZero();
        T delayMeters = model.pathDelay(zero.add(FastMath.toRadians(elevation)),
                                             Frequency.G01.getMHzFrequency() * 1.0e6,
                                             model.getParameters(field));

        Assertions.assertTrue(Precision.compareTo(delayMeters.getReal(), 12., 1.0e-6) < 0);
        Assertions.assertTrue(Precision.compareTo(delayMeters.getReal(), 0.,  1.0e-6) > 0);
    }

    @Test
    public void testZeroDelay() {
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 10.0);

        // Spacecraft state
        final AbsoluteDate    date    = AbsoluteDate.J2000_EPOCH;
        final Frame           frame   = FramesFactory.getEME2000();
        final Orbit           orbit   = new KeplerianOrbit(24464560.0, 0.05, 0.122138,
                                               3.10686, 1.00681, 0.048363,
                                               PositionAngleType.MEAN, frame, date, Constants.WGS84_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit);

        // Delay
        final double delay = model.pathDelay(state, baseFrame, frequency, model.getParameters());
        Assertions.assertEquals(0.0, delay, Double.MIN_VALUE);
    }

    @Test
    public void testFieldZeroDelay() {
        doTestFieldZeroDelay(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldZeroDelay(final Field<T> field) {
        final T zero = field.getZero();
        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Ionospheric model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 10.0);

        // Spacecraft state
        final FieldAbsoluteDate<T>    date    = FieldAbsoluteDate.getJ2000Epoch(field);
        final Frame                   frame   = FramesFactory.getEME2000();
        final FieldOrbit<T>           orbit   = new FieldKeplerianOrbit<>(zero.add(24464560.0), zero.add(0.05), zero.add(0.122138),
                                                                          zero.add(3.10686), zero.add(1.00681), zero.add(0.048363),
                                                                          PositionAngleType.MEAN, frame, date, zero.add(Constants.WGS84_EARTH_MU));
        final FieldSpacecraftState<T> state = new FieldSpacecraftState<>(orbit);

        // Delay
        final T delay = model.pathDelay(state, baseFrame, frequency, model.getParameters(field));
        Assertions.assertEquals(0.0, delay.getReal(), Double.MIN_VALUE);
    }

    @Test
    public void testEquality() {
        doTestEquality(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEquality(final Field<T> field) {
        final double elevation = 70.;

        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 50.0);
        T zero = field.getZero();
        T delayMetersF = model.pathDelay(zero.add(FastMath.toRadians(elevation)),
                                             Frequency.G01.getMHzFrequency() * 1.0e6,
                                             model.getParameters(field));

        double delayMetersR = model.pathDelay(FastMath.toRadians(elevation),
                                             Frequency.G01.getMHzFrequency() * 1.0e6,
                                             model.getParameters());

        Assertions.assertEquals(delayMetersR, delayMetersF.getReal(), 1.0e-15);
    }

    @Test
    public void testDelayStateDerivatives() {

        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

        // Geodetic point
        final double height       = 0.0;
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(0.0), FastMath.toRadians(0.0), height);
        // Body: earth
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Topocentric frame
        final TopocentricFrame baseFrame = new TopocentricFrame(earth, point, "topo");

        // Station
        final GroundStation station = new GroundStation(baseFrame);

        // Ionospheric model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 10.0);

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
                                                                                  PositionAngleType.MEAN, frame,
                                                                                  dsDate, zero.add(3.9860047e14));
        // Field State
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<>(dsOrbit);

        // Initial satellite elevation
        final FieldVector3D<DerivativeStructure> position = dsState.getPosition();
        final DerivativeStructure dsElevation = baseFrame.getTrackingCoordinates(position, frame, dsDate).getElevation();

        // Set drivers reference date
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            driver.setReferenceDate(dsDate.toAbsoluteDate());
        }

        // Verify delay equality
        final double delayR = model.pathDelay(dsState.toSpacecraftState(), baseFrame, frequency, model.getParameters());
        final DerivativeStructure delayD = model.pathDelay(dsState, baseFrame, frequency, model.getParameters(field));
        Assertions.assertEquals(delayR, delayD.getValue(), 5e-15);

        // Compute Delay with state derivatives
        final DerivativeStructure delay = model.pathDelay(dsElevation, frequency, model.getParameters(field));

        final double[] compDeriv = delay.getAllDerivatives();

        // Field -> non-field
        final Orbit orbit = dsOrbit.toOrbit();
        final SpacecraftState state = dsState.toSpacecraftState();

        // Finite differences for reference values
        final double[][] refDeriv = new double[1][6];
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        final PositionAngleType angleType = PositionAngleType.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        for (int i = 0; i < 6; i++) {
            SpacecraftState stateM4 = shiftState(state, orbitType, angleType, -4 * steps[i], i);
            final Vector3D positionM4 = stateM4.getPosition();
            final double elevationM4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM4, stateM4.getFrame(), stateM4.getDate()).
                                        getElevation();
            double  delayM4 = model.pathDelay(elevationM4, frequency, model.getParameters());
            
            SpacecraftState stateM3 = shiftState(state, orbitType, angleType, -3 * steps[i], i);
            final Vector3D positionM3 = stateM3.getPosition();
            final double elevationM3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM3, stateM3.getFrame(), stateM3.getDate()).
                                        getElevation();
            double  delayM3 = model.pathDelay(elevationM3, frequency, model.getParameters());
            
            SpacecraftState stateM2 = shiftState(state, orbitType, angleType, -2 * steps[i], i);
            final Vector3D positionM2 = stateM2.getPosition();
            final double elevationM2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM2, stateM2.getFrame(), stateM2.getDate()).
                                        getElevation();
            double  delayM2 = model.pathDelay(elevationM2, frequency, model.getParameters());
 
            SpacecraftState stateM1 = shiftState(state, orbitType, angleType, -1 * steps[i], i);
            final Vector3D positionM1 = stateM1.getPosition();
            final double elevationM1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionM1, stateM1.getFrame(), stateM1.getDate()).
                                        getElevation();
            double  delayM1 = model.pathDelay(elevationM1, frequency, model.getParameters());
           
            SpacecraftState stateP1 = shiftState(state, orbitType, angleType, 1 * steps[i], i);
            final Vector3D positionP1 = stateP1.getPosition();
            final double elevationP1  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP1, stateP1.getFrame(), stateP1.getDate()).
                                        getElevation();
            double  delayP1 = model.pathDelay(elevationP1, frequency, model.getParameters());
            
            SpacecraftState stateP2 = shiftState(state, orbitType, angleType, 2 * steps[i], i);
            final Vector3D positionP2 = stateP2.getPosition();
            final double elevationP2  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP2, stateP2.getFrame(), stateP2.getDate()).
                                        getElevation();
            double  delayP2 = model.pathDelay(elevationP2, frequency, model.getParameters());
            
            SpacecraftState stateP3 = shiftState(state, orbitType, angleType, 3 * steps[i], i);
            final Vector3D positionP3 = stateP3.getPosition();
            final double elevationP3  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP3, stateP3.getFrame(), stateP3.getDate()).
                                        getElevation();
            double  delayP3 = model.pathDelay(elevationP3, frequency, model.getParameters());
            
            SpacecraftState stateP4 = shiftState(state, orbitType, angleType, 4 * steps[i], i);
            final Vector3D positionP4 = stateP4.getPosition();
            final double elevationP4  = station.getBaseFrame().
                                        getTrackingCoordinates(positionP4, stateP4.getFrame(), stateP4.getDate()).
                                        getElevation();
            double  delayP4 = model.pathDelay(elevationP4, frequency, model.getParameters());
            
            fillJacobianColumn(refDeriv, i, steps[i],
                               delayM4, delayM3, delayM2, delayM1,
                               delayP1, delayP2, delayP3, delayP4);
        }

        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(compDeriv[i + 1], refDeriv[0][i], 8.3e-11);
        }
    }

    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType, true);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }

    @Test
    public void testParametersDerivatives() {

        // Frequency
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;

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

        // Ionospheric model
        final IonosphericMappingFunction mapping = new SingleLayerModelMappingFunction();
        final EstimatedIonosphericModel model = new EstimatedIonosphericModel(mapping, 50.0);

        // Set Parameter Driver
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            driver.setValue(driver.getReferenceValue());
            driver.setSelected(driver.getName().equals(EstimatedIonosphericModel.VERTICAL_TOTAL_ELECTRON_CONTENT));
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

        // Set drivers reference date
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            driver.setReferenceDate(dsDate.toAbsoluteDate());
        }

        // Field Orbit
        final Frame frame = FramesFactory.getEME2000();
        final FieldOrbit<DerivativeStructure> dsOrbit = new FieldKeplerianOrbit<>(a0, e0, i0, pa0, raan0, anomaly0,
                                                                                  PositionAngleType.MEAN, frame,
                                                                                  dsDate, zero.add(3.9860047e14));

        // Field State
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<>(dsOrbit);

        // Initial satellite elevation
        final FieldVector3D<DerivativeStructure> position = dsState.getPosition();
        final DerivativeStructure dsElevation = baseFrame.getTrackingCoordinates(position, frame, dsState.getDate()).getElevation();

        // Add parameter as a variable
        final List<ParameterDriver> drivers = model.getParametersDrivers();
        final DerivativeStructure[] parameters = new DerivativeStructure[drivers.size()];
        int index = 6;
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).isSelected() ?
                            factory.variable(index++, drivers.get(i).getValue()) :
                            factory.constant(drivers.get(i).getValue());
        }

        // Compute delay state derivatives
        final DerivativeStructure delay = model.pathDelay(dsElevation, frequency, parameters);

        final double[] compDeriv = delay.getAllDerivatives();

        // Field -> non-field
        final double elevation = dsElevation.getReal();

        // Finite differences for reference values
        final double[][] refDeriv = new double[1][1];
        ParameterDriversList bound = new ParameterDriversList();
        for (final ParameterDriver driver : model.getParametersDrivers()) {
            if (driver.getName().equals(EstimatedIonosphericModel.VERTICAL_TOTAL_ELECTRON_CONTENT)) {
                driver.setSelected(true);
                bound.add(driver);
            } else {
                driver.setSelected(false);
            }
        }
        ParameterDriver selected = bound.getDrivers().get(0);
        double p0 = selected.getReferenceValue();
        double h  = selected.getScale();

        selected.setValue(p0 - 4 * h);
        double  delayM4 = model.pathDelay(elevation, frequency, model.getParameters());
        
        selected.setValue(p0 - 3 * h);
        double  delayM3 = model.pathDelay(elevation, frequency, model.getParameters());
        
        selected.setValue(p0 - 2 * h);
        double  delayM2 = model.pathDelay(elevation, frequency, model.getParameters());

        selected.setValue(p0 - 1 * h);
        double  delayM1 = model.pathDelay(elevation, frequency, model.getParameters());

        selected.setValue(p0 + 1 * h);
        double  delayP1 = model.pathDelay(elevation, frequency, model.getParameters());

        selected.setValue(p0 + 2 * h);
        double  delayP2 = model.pathDelay(elevation, frequency, model.getParameters());

        selected.setValue(p0 + 3 * h);
        double  delayP3 = model.pathDelay(elevation, frequency, model.getParameters());

        selected.setValue(p0 + 4 * h);
        double  delayP4 = model.pathDelay(elevation, frequency, model.getParameters());
            
        fillJacobianColumn(refDeriv, 0, h,
                           delayM4, delayM3, delayM2, delayM1,
                           delayP1, delayP2, delayP3, delayP4);

        Assertions.assertEquals(compDeriv[7], refDeriv[0][0], 1.0e-15);

    }

    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngleType angleType,
                                  boolean withMass) {
        double[][] array = new double[2][withMass ? 7 : 6];
        orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
        if (withMass) {
            array[0][6] = state.getMass();
        }
        return array;
    }

    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngleType angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[0][6]);
    }

    private void fillJacobianColumn(double[][] jacobian, int column, double h,
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
