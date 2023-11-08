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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.hipparchus.stat.descriptive.moment.Mean;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.modifiers.PhaseIonosphericDelayModifier;
import org.orekit.estimation.measurements.modifiers.PhaseTroposphericDelayModifier;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.models.earth.troposphere.EstimatedTroposphericModel;
import org.orekit.models.earth.troposphere.NiellMappingFunctionModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.StateFunction;
import org.orekit.utils.TimeStampedPVCoordinates;

public class PhaseTest {

    /**
     * Test the values of the phase comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Phase Values\n");
        }
        // Run test
        this.genericTestValues(printResults);
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Phase Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsPMedian = 5.7e-10;
        double refErrorsPMean   = 4.0e-09;
        double refErrorsPMax    = 2.4e-07;
        double refErrorsVMedian = 2.0e-05;
        double refErrorsVMean   = 8.3e-05;
        double refErrorsVMax    = 4.6e-03;
        this.genericTestStateDerivatives(printResults,
                                         refErrorsPMedian, refErrorsPMean, refErrorsPMax,
                                         refErrorsVMedian, refErrorsVMean, refErrorsVMax);
    }

    /**
     * Test the values of the state derivatives with modifier using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivativesWithModifier() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Phase State Derivatives with Modifier - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsPMedian = 5.7e-10;
        double refErrorsPMean   = 4.0e-09;
        double refErrorsPMax    = 2.4e-07;
        double refErrorsVMedian = 2.0e-05;
        double refErrorsVMean   = 8.3e-05;
        double refErrorsVMax    = 4.6e-03;
        this.genericTestStateDerivatives(printResults,
                                         refErrorsPMedian, refErrorsPMean, refErrorsPMax,
                                         refErrorsVMedian, refErrorsVMean, refErrorsVMax);
    }

    /**
     * Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivatives() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Phase Parameter Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsMedian = 1.4e-10;
        double refErrorsMean   = 5.0e-8;
        double refErrorsMax    = 5.1e-6;
        this.genericTestParameterDerivatives(printResults,
                                             refErrorsMedian, refErrorsMean, refErrorsMax);

    }

    /**
     * Generic test function for values of the phase
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect phase measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 1234;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context,
                                                                                           Frequency.E01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> absoluteErrors = new ArrayList<>();
        final List<Double> relativeErrors = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {
                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);

                    // Values of the Phase & errors
                    final double phaseObserved  = measurement.getObservedValue()[0];
                    final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(0, 0,
                                                                                                         new SpacecraftState[] { state });

                    final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
                    Assertions.assertEquals(2, participants.length);
                    final double dt = participants[1].getDate().durationFrom(participants[0].getDate());
                    Assertions.assertEquals(1.0e6 * Frequency.E01.getMHzFrequency() * (dt + groundClockOffset - satClockOffset) + ambiguity,
                                        estimated.getEstimatedValue()[0],
                                        1.0e-7);

                    final double phaseEstimated = estimated.getEstimatedValue()[0];
                    final double absoluteError = phaseEstimated - phaseObserved;
                    absoluteErrors.add(absoluteError);
                    relativeErrors.add(FastMath.abs(absoluteError) / FastMath.abs(phaseObserved));

                    // Print results on console ?
                    if (printResults) {
                        final AbsoluteDate measurementDate = measurement.getDate();
                        String stationName = ((Phase) measurement).getStation().getBaseFrame().getName();

                        System.out.format(Locale.US, "%-15s  %-23s  %-23s     %19.6f      %19.6f    %13.6e   %13.6e%n",
                                         stationName, measurementDate, date,
                                         phaseObserved, phaseEstimated,
                                         FastMath.abs(phaseEstimated - phaseObserved),
                                         FastMath.abs((phaseEstimated - phaseObserved) / phaseObserved));
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        }); // End lambda function handlestep

        // Print results on console ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  %19s  %19s   %13s   %13s%n",
                              "Station", "Measurement Date", "State Date",
                              "Phase observed [cycle]", "Phase estimated [cycle]",
                              "ΔPhase [cycle]", "rel ΔPhase");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double array
        final double[] absErrors = absoluteErrors.stream().mapToDouble(Double::doubleValue).toArray();
        final double[] relErrors = relativeErrors.stream().mapToDouble(Double::doubleValue).toArray();

        // Statistics' assertion
        final double absErrorsMedian = new Median().evaluate(absErrors);
        final double absErrorsMin    = new Min().evaluate(absErrors);
        final double absErrorsMax    = new Max().evaluate(absErrors);
        final double relErrorsMedian = new Median().evaluate(relErrors);
        final double relErrorsMax    = new Max().evaluate(relErrors);

        // Print the results on console ? Final results
        if (printResults) {
            System.out.println();
            System.out.println("Absolute errors median: " +  absErrorsMedian);
            System.out.println("Absolute errors min   : " +  absErrorsMin);
            System.out.println("Absolute errors max   : " +  absErrorsMax);
            System.out.println("Relative errors median: " +  relErrorsMedian);
            System.out.println("Relative errors max   : " +  relErrorsMax);
        }

        Assertions.assertEquals(0.0, absErrorsMedian, 1.4e-7);
        Assertions.assertEquals(0.0, absErrorsMin,    1.3e-6);
        Assertions.assertEquals(0.0, absErrorsMax,    1.5e-6);
        Assertions.assertEquals(0.0, relErrorsMedian, 9.1e-15);
        Assertions.assertEquals(0.0, relErrorsMax,    2.8e-14);

        // Test measurement type
        Assertions.assertEquals(Phase.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    void genericTestStateDerivatives(final boolean printResults,
                                     final double refErrorsPMedian, final double refErrorsPMean, final double refErrorsPMax,
                                     final double refErrorsVMedian, final double refErrorsVMean, final double refErrorsVMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 1234;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context,
                                                                                           Frequency.E01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<Double>();
        final List<Double> errorsV = new ArrayList<Double>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);
                    final double[][]      jacobian  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(new StateFunction() {
                        public double[] value(final SpacecraftState state) {
                            return measurement.
                                   estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                   getEstimatedValue();
                        }
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                       OrbitType.CARTESIAN, PositionAngleType.TRUE, 2.0, 3).value(state);

                    Assertions.assertEquals(jacobianRef.length, jacobian.length);
                    Assertions.assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the Jacobian
                    double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
                    double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
                    for (int i = 0; i < jacobian.length; ++i) {
                        for (int j = 0; j < jacobian[i].length; ++j) {
                            dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                            dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                            if (j < 3) { errorsP.add(dJacobianRelative[i][j]);
                            } else { errorsV.add(dJacobianRelative[i][j]); }
                        }
                    }
                    // Print values in console ?
                    if (printResults) {
                        String stationName  = ((Phase) measurement).getStation().getBaseFrame().getName();
                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e%n",
                                          stationName, measurement.getDate(), date,
                                          dJacobian[0][0], dJacobian[0][1], dJacobian[0][2],
                                          dJacobian[0][3], dJacobian[0][4], dJacobian[0][5],
                                          dJacobianRelative[0][0], dJacobianRelative[0][1], dJacobianRelative[0][2],
                                          dJacobianRelative[0][3], dJacobianRelative[0][4], dJacobianRelative[0][5]);
                    }
                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Print results on console ?
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Station", "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double[] and evaluate some statistics
        final double relErrorsP[] = errorsP.stream().mapToDouble(Double::doubleValue).toArray();
        final double relErrorsV[] = errorsV.stream().mapToDouble(Double::doubleValue).toArray();

        final double errorsPMedian = new Median().evaluate(relErrorsP);
        final double errorsPMean   = new Mean().evaluate(relErrorsP);
        final double errorsPMax    = new Max().evaluate(relErrorsP);
        final double errorsVMedian = new Median().evaluate(relErrorsV);
        final double errorsVMean   = new Mean().evaluate(relErrorsV);
        final double errorsVMax    = new Max().evaluate(relErrorsV);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dΦ/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dΦ/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        Assertions.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assertions.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assertions.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assertions.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assertions.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assertions.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    void genericTestParameterDerivatives(final boolean printResults,
                                         final double refErrorsMedian, final double refErrorsMean, final double refErrorsMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 1234;
        final double satClockOffset    = 345.0e-6;
        final PhaseMeasurementCreator creator = new PhaseMeasurementCreator(context, Frequency.E01,
                                                                            ambiguity,
                                                                            satClockOffset);
        creator.getSatellite().getClockOffsetDriver().setSelected(true);
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(true);
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator, creator, 1.0, 3.0, 300.0);

        // List to store the results
        final List<Double> relErrorList = new ArrayList<Double>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {

                    // Parameter corresponding to station position offset
                    final GroundStation stationParameter = ((Phase) measurement).getStation();

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);
                    final ParameterDriver[] drivers = new ParameterDriver[] {
                        stationParameter.getClockOffsetDriver(),
                        stationParameter.getEastOffsetDriver(),
                        stationParameter.getNorthOffsetDriver(),
                        stationParameter.getZenithOffsetDriver(),
                        measurement.getSatellites().get(0).getClockOffsetDriver()
                    };

                    if (printResults) {
                        String stationName  = ((Phase) measurement).getStation().getBaseFrame().getName();
                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  ",
                                          stationName, measurement.getDate(), date);
                    }

                    for (int i = 0; i < drivers.length; ++i) {
                        final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                        Assertions.assertEquals(1, measurement.getDimension());
                        Assertions.assertEquals(1, gradient.length);

                        // Compute a reference value using finite differences
                        final ParameterFunction dMkdP =
                                        Differentiation.differentiate(new ParameterFunction() {
                                            /** {@inheritDoc} */
                                            @Override
                                            public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                                                return measurement.
                                                       estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                                       getEstimatedValue()[0];
                                            }
                                        }, 3, 20.0 * drivers[i].getScale());
                        final double ref = dMkdP.value(drivers[i], date);

                        if (printResults) {
                            System.out.format(Locale.US, "%10.3e  %10.3e  ", gradient[0]-ref, FastMath.abs((gradient[0]-ref)/ref));
                        }

                        final double relError = FastMath.abs((ref-gradient[0])/ref);
                        relErrorList.add(relError);
//                        Assertions.assertEquals(ref, gradient[0], 6.1e-5 * FastMath.abs(ref));
                    }
                    if (printResults) {
                        System.out.format(Locale.US, "%n");
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Print results ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                              "%10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s%n",
                              "Station", "Measurement Date", "State Date",
                              "Δt",    "rel Δt",
                              "ΔdQx",  "rel ΔdQx",
                              "ΔdQy",  "rel ΔdQy",
                              "ΔdQz",  "rel ΔdQz",
                              "Δtsat", "rel Δtsat");
         }

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert error list to double[]
        final double relErrors[] = relErrorList.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute statistics
        final double relErrorsMedian = new Median().evaluate(relErrors);
        final double relErrorsMean   = new Mean().evaluate(relErrors);
        final double relErrorsMax    = new Max().evaluate(relErrors);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dR/dQ -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              relErrorsMedian, relErrorsMean, relErrorsMax);
        }

        Assertions.assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        Assertions.assertEquals(0.0, relErrorsMean, refErrorsMean);
        Assertions.assertEquals(0.0, relErrorsMax, refErrorsMax);

    }

    @Test
    public void testStateDerivativesWithTroposphericModifier() {

        final boolean printResults     = false;
        final double  refErrorsPMedian = 5.9e-10;
        final double  refErrorsPMean   = 4.3e-9;
        final double  refErrorsPMax    = 3.8e-7;
        final double  refErrorsVMedian = 2.0e-5;
        final double  refErrorsVMean   = 8.3e-5;
        final double  refErrorsVMax    = 4.6e-3;

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 1234;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context,
                                                                                           Frequency.E01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<Double>();
        final List<Double> errorsV = new ArrayList<Double>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {

                    String stationName  = ((Phase) measurement).getStation().getBaseFrame().getName();

                    // Add modifier
                    final NiellMappingFunctionModel mappingFunction = new NiellMappingFunctionModel();
                    final EstimatedTroposphericModel tropoModel     = new EstimatedTroposphericModel(mappingFunction, 5.0);
                    final PhaseTroposphericDelayModifier modifier = new PhaseTroposphericDelayModifier(tropoModel);
                    final List<ParameterDriver> parameters = modifier.getParametersDrivers();
                    parameters.get(0).setName(stationName + "/" + EstimatedTroposphericModel.TOTAL_ZENITH_DELAY);
                    parameters.get(0).setSelected(true);
                    ((Phase) measurement).addModifier(modifier);

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);
                    final double[][]      jacobian  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(new StateFunction() {
                        public double[] value(final SpacecraftState state) {
                            return measurement.
                                   estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                   getEstimatedValue();
                        }
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                       OrbitType.CARTESIAN, PositionAngleType.TRUE, 2.0, 3).value(state);

                    Assertions.assertEquals(jacobianRef.length, jacobian.length);
                    Assertions.assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the Jacobian
                    double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
                    double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
                    for (int i = 0; i < jacobian.length; ++i) {
                        for (int j = 0; j < jacobian[i].length; ++j) {
                            dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                            dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                            if (j < 3) { errorsP.add(dJacobianRelative[i][j]);
                            } else { errorsV.add(dJacobianRelative[i][j]); }
                        }
                    }
                    // Print values in console ?
                    if (printResults) {
                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e%n",
                                          stationName, measurement.getDate(), date,
                                          dJacobian[0][0], dJacobian[0][1], dJacobian[0][2],
                                          dJacobian[0][3], dJacobian[0][4], dJacobian[0][5],
                                          dJacobianRelative[0][0], dJacobianRelative[0][1], dJacobianRelative[0][2],
                                          dJacobianRelative[0][3], dJacobianRelative[0][4], dJacobianRelative[0][5]);
                    }
                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Print results on console ?
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Station", "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double[] and evaluate some statistics
        final double relErrorsP[] = errorsP.stream().mapToDouble(Double::doubleValue).toArray();
        final double relErrorsV[] = errorsV.stream().mapToDouble(Double::doubleValue).toArray();

        final double errorsPMedian = new Median().evaluate(relErrorsP);
        final double errorsPMean   = new Mean().evaluate(relErrorsP);
        final double errorsPMax    = new Max().evaluate(relErrorsP);
        final double errorsVMedian = new Median().evaluate(relErrorsV);
        final double errorsVMean   = new Mean().evaluate(relErrorsV);
        final double errorsVMax    = new Max().evaluate(relErrorsV);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dΦ/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dΦ/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        Assertions.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assertions.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assertions.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assertions.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assertions.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assertions.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    @Test
    public void testStateDerivativesWithIonosphericModifier() {

        final boolean printResults = false;
        final double refErrorsPMedian = 5.6e-10;
        final double refErrorsPMean = 2.1e-9;
        final double refErrorsPMax = 7.1e-8;
        final double refErrorsVMedian = 1.5e-5;
        final double refErrorsVMean = 7.9e-5;
        final double refErrorsVMax = 4.6e-3;

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final double groundClockOffset =  12.0e-6;
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setValue(groundClockOffset);
        }
        final int    ambiguity         = 1234;
        final double satClockOffset    = 345.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PhaseMeasurementCreator(context,
                                                                                           Frequency.E01,
                                                                                           ambiguity,
                                                                                           satClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<Double>();
        final List<Double> errorsV = new ArrayList<Double>();

        final IonosphericModel model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06, 0},
                                                              new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});
        final double frequency = Frequency.G01.getMHzFrequency() * 1.0e6;
        final PhaseIonosphericDelayModifier modifier = new PhaseIonosphericDelayModifier(model, frequency);

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                final Phase phase = (Phase) measurement;
                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {

                    // Add modifier
                    phase.addModifier(modifier);

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);
                    final double[][]      jacobian  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(new StateFunction() {
                        public double[] value(final SpacecraftState state) {
                            return measurement.
                                   estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                   getEstimatedValue();
                        }
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                       OrbitType.CARTESIAN, PositionAngleType.TRUE, 2.0, 3).value(state);

                    Assertions.assertEquals(jacobianRef.length, jacobian.length);
                    Assertions.assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the Jacobian
                    double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
                    double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
                    for (int i = 0; i < jacobian.length; ++i) {
                        for (int j = 0; j < jacobian[i].length; ++j) {
                            dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                            dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                            if (j < 3) { errorsP.add(dJacobianRelative[i][j]);
                            } else { errorsV.add(dJacobianRelative[i][j]); }
                        }
                    }
                    // Print values in console ?
                    if (printResults) {
                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e  " +
                                          "%10.3e  %10.3e  %10.3e%n",
                                          phase.getStation().getBaseFrame().getName(), measurement.getDate(), date,
                                          dJacobian[0][0], dJacobian[0][1], dJacobian[0][2],
                                          dJacobian[0][3], dJacobian[0][4], dJacobian[0][5],
                                          dJacobianRelative[0][0], dJacobianRelative[0][1], dJacobianRelative[0][2],
                                          dJacobianRelative[0][3], dJacobianRelative[0][4], dJacobianRelative[0][5]);
                    }
                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Print results on console ?
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Station", "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double[] and evaluate some statistics
        final double relErrorsP[] = errorsP.stream().mapToDouble(Double::doubleValue).toArray();
        final double relErrorsV[] = errorsV.stream().mapToDouble(Double::doubleValue).toArray();

        final double errorsPMedian = new Median().evaluate(relErrorsP);
        final double errorsPMean   = new Mean().evaluate(relErrorsP);
        final double errorsPMax    = new Max().evaluate(relErrorsP);
        final double errorsVMedian = new Median().evaluate(relErrorsV);
        final double errorsVMean   = new Mean().evaluate(relErrorsV);
        final double errorsVMax    = new Max().evaluate(relErrorsV);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dΦ/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dΦ/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        Assertions.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assertions.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assertions.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assertions.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assertions.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assertions.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    @Test
    public void testIssue734() {

        Utils.setDataRoot("regular-data");

        // Create a ground station
        final OneAxisEllipsoid body = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                           FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo = new TopocentricFrame(body,
                                                           new GeodeticPoint(FastMath.toRadians(51.8), FastMath.toRadians(102.2), 811.2),
                                                           "BADG");
        final GroundStation station = new GroundStation(topo);

        // Create a phase measurement
        final Phase phase = new Phase(station, AbsoluteDate.J2000_EPOCH, 119866527.060, Frequency.G01.getWavelength(), 0.02, 1.0, new ObservableSatellite(0));

        // First check
        Assertions.assertEquals(0.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
        Assertions.assertFalse(phase.getAmbiguityDriver().isSelected());

        // Perform some changes in ambiguity driver
        phase.getAmbiguityDriver().setValue(1234.0);
        phase.getAmbiguityDriver().setSelected(true);

        // Second check
        Assertions.assertEquals(1234.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
        Assertions.assertTrue(phase.getAmbiguityDriver().isSelected());
        for (ParameterDriver driver : phase.getParametersDrivers()) {
            // Verify if the current driver corresponds to the phase ambiguity
            if (driver.getName() == Phase.AMBIGUITY_NAME) {
                Assertions.assertEquals(1234.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
                Assertions.assertTrue(phase.getAmbiguityDriver().isSelected());
            }
        }

    }

}
