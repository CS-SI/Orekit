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
package org.orekit.estimation.measurements;

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
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.StateFunction;

public class RangeAnalyticTest {

    /**
     * Test the values of the range comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
      */
    @Test
    public void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical Values\n");
        }
        // Run test
        genericTestValues(printResults);
    }

    /**
     * Test the values of the analytic state derivatives computation
     * using Range function as a comparison
     */
    @Test
    public void testStateDerivatives() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical State Derivatives - Derivative Structure Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = false;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    3.5e-8, 1.4e-7, 7.8e-6, 3.5e-8, 1.4e-7, 7.8e-6);
    }

    /**
     * Test the values of the analytic state derivatives computation
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testStateDerivativesFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical State Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = true;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    3.5e-8, 1.4e-7, 7.4e-6, 3.3e-4, 1.7e-3, 8.1e-2);
    }

    /**
     * Test the values of the analytic state derivatives with modifier computation
     * using Range function as a comparison
     */
    @Test
    public void testStateDerivativesWithModifier() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical State Derivatives With Modifier - Derivative Structure Comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences = false;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    4.1e-7, 1.9e-6, 6.3e-5, 4.7e-11, 4.7e-11, 5.6e-11);
    }

    /**
     * Test the values of the analytic state derivatives with modifier computation
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testStateDerivativesWithModifierFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical State Derivatives With Modifier - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = true;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    3.5e-8, 1.4e-7, 7.4e-6, 3.3e-4, 1.7e-3, 8.1e-2);
    }

    /**
     * Test the values of the analytic parameter derivatives computation
     * using Range function as a comparison
     */
    @Test
    public void testParameterDerivatives() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical Parameter Derivatives - Derivative Structure Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = false;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults);
    }

    /**
     * Test the values of the analytic parameter derivatives computation
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivativesFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical Parameter Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = true;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults);
    }

    /**
     * Test the values of the analytic parameter derivatives with modifier computation
     * using Range function as a comparison
     */
    @Test
    public void testParameterDerivativesWithModifier() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical Parameter Derivatives With Modifier - Derivative Structure Comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences = false;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults);
    }

    /**
     * Test the values of the analytic parameter derivatives with modifier computation
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivativesWithModifierFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Analytical Parameter Derivatives With Modifier - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences = true;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults);
    }

    /**
     * Generic test function for values of the range
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults)
                    {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> absoluteErrors = new ArrayList<Double>();
        final List<Double> relativeErrors = new ArrayList<Double>();

        // Set step handler
        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {
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

                    // Values of the RangeAnalytic & errors
                    final double RangeObserved  = measurement.getObservedValue()[0];
                    final double RangeEstimated = new RangeAnalytic((Range) measurement).
                                    theoreticalEvaluationAnalytic(0, 0, state).getEstimatedValue()[0];
                    final double absoluteError = RangeEstimated-RangeObserved;
                    absoluteErrors.add(absoluteError);
                    relativeErrors.add(FastMath.abs(absoluteError)/FastMath.abs(RangeObserved));

                    // Print results on console ?
                    if (printResults) {
                        final AbsoluteDate measurementDate = measurement.getDate();
                        String stationName = ((Range) measurement).getStation().getBaseFrame().getName();

                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  %19.6f  %19.6f  %13.6e  %13.6e%n",
                                         stationName, measurementDate, date,
                                         RangeObserved, RangeEstimated,
                                         FastMath.abs(RangeEstimated-RangeObserved),
                                         FastMath.abs((RangeEstimated-RangeObserved)/RangeObserved));
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        }); // End lambda function handlestep

        // Print results on console ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  %19s  %19s  %13s  %13s%n",
                              "Station", "Measurement Date", "State Date",
                              "Range observed [m]", "Range estimated [m]",
                              "ΔRange [m]", "rel ΔRange");
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

        Assertions.assertEquals(0.0, absErrorsMedian, 4.0e-08);
        Assertions.assertEquals(0.0, absErrorsMin,    2.0e-07);
        Assertions.assertEquals(0.0, absErrorsMax,    2.3e-07);
        Assertions.assertEquals(0.0, relErrorsMedian, 6.5e-15);
        Assertions.assertEquals(0.0, relErrorsMax,    2.5e-14);

        // Test measurement type
        final RangeAnalytic rangeAnalytic = new RangeAnalytic((Range) measurements.get(0));
        Assertions.assertEquals(RangeAnalytic.MEASUREMENT_TYPE, rangeAnalytic.getMeasurementType());
    }

    /**
     * Generic test function for derivatives with respect to state
     * @param isModifier Use of atmospheric modifiers
     * @param isFiniteDifferences Finite differences reference calculation if true, Range class otherwise
     * @param printResults Print the results ?
     */
    void genericTestStateDerivatives(final boolean isModifier, final boolean isFiniteDifferences, final boolean printResults,
                                     final double refErrorsPMedian, final double refErrorsPMean, final double refErrorsPMax,
                                     final double refErrorsVMedian, final double refErrorsVMean, final double refErrorsVMax)
        {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<Double>();
        final List<Double> errorsV = new ArrayList<Double>();

        // Set step handler
        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {

                    // Add modifiers if test implies it
                    final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());
                    if (isModifier) {
                        ((Range) measurement).addModifier(modifier);
                    }

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

                    final EstimatedMeasurement<Range> range =
                                    new RangeAnalytic((Range)measurement).theoreticalEvaluationAnalytic(0, 0, state);
                    if (isModifier) {
                        modifier.modify(range);
                    }
                    final double[][] jacobian  = range.getStateDerivatives(0);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    if (isFiniteDifferences) {
                        // Compute a reference value using finite differences
                        jacobianRef = Differentiation.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) {
                                return measurement.
                                       estimateWithoutDerivatives(0, 0, new SpacecraftState[] { state }).
                                       getEstimatedValue();
                            }
                        }, measurement.getDimension(), propagator.getAttitudeProvider(),
                           OrbitType.CARTESIAN, PositionAngleType.TRUE, 2.0, 3).value(state);
                    } else {
                        // Compute a reference value using Range class function
                        jacobianRef = ((Range) measurement).theoreticalEvaluation(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);
                    }

//                    //Test: Test point by point with the debugger
//                    if (!isFiniteDifferences && !isModifier) {
//                        final EstimatedMeasurement<Range> test =
//                                        new RangeAnalytic((Range)measurement).theoreticalEvaluationValidation(0, 0, state);
//                    }
//                    //Test

                    Assertions.assertEquals(jacobianRef.length, jacobian.length);
                    Assertions.assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the jacobian
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
                        String stationName  = ((Range) measurement).getStation().getBaseFrame().getName();
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
            System.out.format(Locale.US, "Relative errors dR/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dR/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        // Reference comparison with Range class
        Assertions.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assertions.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assertions.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assertions.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assertions.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assertions.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    /**
     * Generic test function for derivatives with respect to parameters (station's position in station's topocentric frame)
     * @param isModifier Use of atmospheric modifiers
     * @param isFiniteDifferences Finite differences reference calculation if true, Range class otherwise
     * @param printResults Print the results ?
     */
    void genericTestParameterDerivatives(final boolean isModifier, final boolean isFiniteDifferences, final boolean printResults) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getClockOffsetDriver().setSelected(false);
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TwoWayRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // List to store the results
        final List<Double> relErrorList = new ArrayList<Double>();

        // Set step handler
        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {

                    // Add modifiers if test implies it
                    final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());
                    if (isModifier) {
                        ((Range) measurement).addModifier(modifier);
                    }

                    // Parameter corresponding to station position offset
                    final GroundStation stationParameter = ((Range) measurement).getStation();

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
                        stationParameter.getEastOffsetDriver(),
                        stationParameter.getNorthOffsetDriver(),
                        stationParameter.getZenithOffsetDriver()
                    };

                    if (printResults) {
                        String stationName  = ((Range) measurement).getStation().getBaseFrame().getName();
                        System.out.format(Locale.US, "%-15s  %-23s  %-23s  ",
                                          stationName, measurement.getDate(), date);
                    }

                    for (int i = 0; i < drivers.length; ++i) {
                        final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                        Assertions.assertEquals(1, measurement.getDimension());
                        Assertions.assertEquals(1, gradient.length);

                        // Compute a reference value using analytical formulas
                        final EstimatedMeasurement<Range> rangeAnalytic =
                                        new RangeAnalytic((Range)measurement).theoreticalEvaluationAnalytic(0, 0, state);
                        if (isModifier) {
                            modifier.modify(rangeAnalytic);
                        }
                        final double ref = rangeAnalytic.getParameterDerivatives(drivers[i])[0];

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
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Station", "Measurement Date", "State Date",
                            "ΔdQx", "rel ΔdQx",
                            "ΔdQy", "rel ΔdQy",
                            "ΔdQz", "rel ΔdQz");
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

        // Assert the results / max values depend on the test
        double refErrorsMedian, refErrorsMean, refErrorsMax;
        // Analytic references
        refErrorsMedian = 1.55e-06;
        refErrorsMean   = 3.64e-06;
        refErrorsMax    = 6.1e-05;

        Assertions.assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        Assertions.assertEquals(0.0, relErrorsMean, refErrorsMean);
        Assertions.assertEquals(0.0, relErrorsMax, refErrorsMax);
    }
}
