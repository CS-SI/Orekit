/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.stat.descriptive.moment.Mean;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.EstimationUtils;
import org.orekit.estimation.ParameterFunction;
import org.orekit.estimation.StateFunction;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

public class RangeTest {


    /**
     * Test the values of the range comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     * @throws OrekitException
     */
    @Test
    public void testValues() throws OrekitException {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Values\n");
        }
        // Run test
        boolean isModifier = false;
        this.genericTestValues(isModifier, printResults);
    }

    /**
     * Test the values of the state derivatives using an analytical computation
     * @throws OrekitException
     */
    @Test
    public void testStateDerivativesAnalytic() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range State Derivatives - Analytical comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isNumeric  = false;
        this.genericTestStateDerivatives(isModifier, isNumeric, printResults);
    }

    /**
     * Test the values of the state derivatives using a numerical computation
     * @throws OrekitException
     */
    @Test
    public void testStateDerivativesNumeric() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range State Derivatives - Numerical comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isNumeric  = true;
        this.genericTestStateDerivatives(isModifier, isNumeric, printResults);
    }

    /**
     * Test the values of the state derivatives with modifier using an analytical method
     * @throws OrekitException
     */
    @Test
    public void testStateDerivativesWithModifierAnalytic() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range State Derivatives with Modifier - Analytical comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isNumeric  = false;
        this.genericTestStateDerivatives(isModifier, isNumeric, printResults);
    }

    /**
     * Test the values of the state derivatives with modifier using a numerical method
     * @throws OrekitException
     */
    @Test
    public void testStateDerivativesWithModifierNumeric() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range State Derivatives with Modifier - Numerical comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isNumeric  = true;
        this.genericTestStateDerivatives(isModifier, isNumeric, printResults);
    }

    @Test
    public void testParameterDerivativesAnalytic() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Parameter Derivatives - Analytical comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isNumeric  = false;
        this.genericTestParameterDerivatives(isModifier, isNumeric, printResults);

    }

    @Test
    public void testParameterDerivativesNumeric() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Parameter Derivatives - Numerical comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isNumeric  = true;
        this.genericTestParameterDerivatives(isModifier, isNumeric, printResults);

    }

    @Test
    public void testParameterDerivativesWithModifierAnalytic() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Parameter Derivatives with Modifier - Analytical comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isNumeric  = false;
        this.genericTestParameterDerivatives(isModifier, isNumeric, printResults);

    }

    @Test
    public void testParameterDerivativesWithModifierNumeric() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Parameter Derivatives with Modifier - Numerical comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isNumeric  = true;
        this.genericTestParameterDerivatives(isModifier, isNumeric, printResults);

    }

    /**
     * Generic test function for values of the range
     * @param isModifier Use of atmospheric modifiers
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestValues(final boolean isModifier, final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> absoluteErrors = new ArrayList<Double>();
        final List<Double> relativeErrors = new ArrayList<Double>();

        // Set master mode
        // Use a lambda function to implement "handleStep" function
        propagator.setMasterMode((OrekitStepInterpolator interpolator, boolean isLast) -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {

                    // Add modifiers if test implies it
                    if (isModifier) {
                        final RangeTroposphericDelayModifier modifier = new RangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());
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

                    // Values of the Range & errors
                    final double RangeObserved  = measurement.getObservedValue()[0];
                    final double RangeEstimated = measurement.estimate(0, 0, state).getEstimatedValue()[0];
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
                              "Station","Measurement Date","State Date",
                              "Range observed [m]","Range estimated [m]",
                              "ΔRange [m]","rel ΔRange");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(new ChronologicalComparator());

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

        Assert.assertEquals(0.0, absErrorsMedian, 1e-8);
        Assert.assertEquals(0.0, absErrorsMin, 2e-7);
        Assert.assertEquals(0.0, absErrorsMax, 2e-7);
        Assert.assertEquals(0.0, relErrorsMedian, 1e-14);
        Assert.assertEquals(0.0, relErrorsMax, 2e-14);


    }

    /**
     * Generic test function for derivatives with respect to state
     * @param isModifier Use of atmospheric modifiers
     * @param isNumeric Numerical comparison if true, analytical otherwise
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestStateDerivatives(final boolean isModifier, final boolean isNumeric, final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<Double>();
        final List<Double> errorsV = new ArrayList<Double>();

        // Set master mode
        // Use a lambda function to implement "handleStep" function
        propagator.setMasterMode((OrekitStepInterpolator interpolator, boolean isLast) -> {

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
                    final double[][]      jacobian  = measurement.estimate(0, 0, state).getStateDerivatives();

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    if (isNumeric) {
                        // Compute a reference value using finite differences
                        jacobianRef = EstimationUtils.differentiate(new StateFunction() {
                            public double[] value(final SpacecraftState state) throws OrekitException {
                                return measurement.estimate(0, 0, state).getEstimatedValue();
                            }
                        }, measurement.getDimension(), OrbitType.CARTESIAN,
                                                                    PositionAngle.TRUE, 1.0, 3).value(state);
                    } else {
                        // Compute a reference value using analytical formulas
                        final EstimatedMeasurement<Range> rangeAnalytic = ((Range) measurement).theoreticalEvaluationAnalytic(0, 0, state);
                        if (isModifier) {
                            modifier.modify(rangeAnalytic);
                        }
                        jacobianRef = rangeAnalytic.getStateDerivatives();
                    }

                    Assert.assertEquals(jacobianRef.length, jacobian.length);
                    Assert.assertEquals(jacobianRef[0].length, jacobian[0].length);

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
                                        dJacobian[0][0],dJacobian[0][1],dJacobian[0][2],
                                        dJacobian[0][3],dJacobian[0][4],dJacobian[0][5],
                                        dJacobianRelative[0][0],dJacobianRelative[0][1],dJacobianRelative[0][2],
                                        dJacobianRelative[0][3],dJacobianRelative[0][4],dJacobianRelative[0][5]);
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
                            "Station","Measurement Date","State Date",
                            "ΔdPx","ΔdPy","ΔdPz","ΔdVx","ΔdVy","ΔdVz",
                            "rel ΔdPx","rel ΔdPy","rel ΔdPz",
                            "rel ΔdVx","rel ΔdVy","rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(new ChronologicalComparator());

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
            System.out.format(Locale.US,"Relative errors dR/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US,"Relative errors dR/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        // Assert the results / max values depend on the test
        double refErrorsPMedian, refErrorsPMean, refErrorsPMax;
        double refErrorsVMedian, refErrorsVMean, refErrorsVMax;
        // Analytic
        if (!isNumeric) {
            refErrorsPMedian = 4.7e-11;
            refErrorsPMean   = 4.7e-11;
            refErrorsPMax    = 5.1e-11;

            refErrorsVMedian = refErrorsPMedian;
            refErrorsVMean   = refErrorsPMean;
            refErrorsVMax    = refErrorsPMax;
        }
        // Numeric
        else {
            refErrorsVMedian = 3.1e-04;
            refErrorsVMean   = 1.7e-03;
            refErrorsVMax    = 8.1e-02;

            // Without modifier
            if (!isModifier) {
                refErrorsPMedian = 1.2e-09;
                refErrorsPMean   = 8.8e-09;
                refErrorsPMax    = 3.1e-07;
            }
            // With modifier
            else {
                refErrorsPMedian = 8.2e-07;
                refErrorsPMean   = 3.6e-06;
                refErrorsPMax    = 1.3e-04;
            }
        }

        Assert.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assert.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assert.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assert.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assert.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assert.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    /**
     * Generic test function for derivatives with respect to parameters (station's position in station's topocentric frame)
     * @param isModifier Use of atmospheric modifiers
     * @param isNumeric Numerical comparison if true, analytical otherwise
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestParameterDerivatives(final boolean isModifier, final boolean isNumeric, final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new RangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);

        // List to store the results
        final List<Double> relErrorList = new ArrayList<Double>();

        // Set master mode
        // Use a lambda function to implement "handleStep" function
        propagator.setMasterMode((OrekitStepInterpolator interpolator, boolean isLast) -> {

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

                    for (int i = 0; i < 3; ++i) {
                        final double[] gradient  = measurement.estimate(0, 0, state).getParameterDerivatives(drivers[i]);
                        Assert.assertEquals(1, measurement.getDimension());
                        Assert.assertEquals(1, gradient.length);

                        double ref;
                        if (isNumeric) {
                             // Compute a reference value using finite differences
                            final ParameterFunction dMkdP =
                                            EstimationUtils.differentiate(new ParameterFunction() {
                                                /** {@inheritDoc} */
                                                @Override
                                                public double value(final ParameterDriver parameterDriver) throws OrekitException {
                                                    return measurement.estimate(0, 0, state).getEstimatedValue()[0];
                                                }
                                            }, drivers[i], 3, 20.0);
                            ref = dMkdP.value(drivers[i]);
                        } else {
                            // Compute a reference value using analytical formulas
                            final EstimatedMeasurement<Range> rangeAnalytic = ((Range) measurement).theoreticalEvaluationAnalytic(0, 0, state);
                            if (isModifier) {
                                modifier.modify(rangeAnalytic);
                            }
                            ref = rangeAnalytic.getParameterDerivatives(drivers[i])[0];
                        }

                        if (printResults) {
                            System.out.format(Locale.US,"%10.3e  %10.3e  ",gradient[0]-ref,FastMath.abs((gradient[0]-ref)/ref));
                        }

                        final double relError = FastMath.abs((ref-gradient[0])/ref);
                        relErrorList.add(relError);
//                        Assert.assertEquals(ref, gradient[0], 6.1e-5 * FastMath.abs(ref));
                    }
                    if (printResults) {
                        System.out.format(Locale.US,"%n");
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(new ChronologicalComparator());

        // Print results ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Station","Measurement Date","State Date",
                            "ΔdQx","rel ΔdQx",
                            "ΔdQy","rel ΔdQy",
                            "ΔdQz","rel ΔdQz");
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
            System.out.format(Locale.US,"Relative errors dR/dQ -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              relErrorsMedian, relErrorsMean, relErrorsMax);
        }

        // Assert the results / max values depend on the test
        double refErrorsMedian, refErrorsMean, refErrorsMax;
        // Analytic
        if (!isNumeric) {
            refErrorsMedian = 1.55e-06;
            refErrorsMean   = 3.64e-06;
            refErrorsMax    = 6.1e-05;
        }
        // Numeric
        else {
            refErrorsMedian = 8.7e-11;
            refErrorsMean   = 3.4e-10;
            refErrorsMax    = 1.23e-08;
        }

        Assert.assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        Assert.assertEquals(0.0, relErrorsMean, refErrorsMean);
        Assert.assertEquals(0.0, relErrorsMax, refErrorsMax);
    }
}
