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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.DSSTContext;
import org.orekit.estimation.DSSTEstimationTestUtils;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.StateFunction;
import org.orekit.utils.TimeStampedPVCoordinates;

public class DSSTRangeTest {

    /**
     * Test the values of the range comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest Range Values\n");
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
            System.out.println("\nTest Range State Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        double refErrorsPMedian = 5.1e-10;
        double refErrorsPMean   = 1.9e-09;
        double refErrorsPMax    = 4.3e-08;
        double refErrorsVMedian = 1.8e-04;
        double refErrorsVMean   = 6.1e-04;
        double refErrorsVMax    = 2.6e-02;
        this.genericTestStateDerivatives(isModifier, printResults,
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
            System.out.println("\nTest Range State Derivatives with Modifier - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = true;
        double refErrorsPMedian = 5.6e-10;
        double refErrorsPMean   = 2.2e-09;
        double refErrorsPMax    = 3.7e-08;
        double refErrorsVMedian = 1.8e-04;
        double refErrorsVMean   = 6.1e-04;
        double refErrorsVMax    = 2.6e-02;
        this.genericTestStateDerivatives(isModifier, printResults,
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
            System.out.println("\nTest Range Parameter Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = false;
        double refErrorsMedian = 1.2e-8;
        double refErrorsMean   = 7.9e-8;
        double refErrorsMax    = 3.7e-6;
        this.genericTestParameterDerivatives(isModifier, printResults,
                                             refErrorsMedian, refErrorsMean, refErrorsMax);

    }

    /**
     * Test the values of the parameters' derivatives with modifier, using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivativesWithModifier() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Parameter Derivatives with Modifier - Finite Differences Comparison\n");
        }
        // Run test
        boolean isModifier = true;
        double refErrorsMedian = 1.2e-8;
        double refErrorsMean   = 7.9e-8;
        double refErrorsMax    = 3.7e-6;
        this.genericTestParameterDerivatives(isModifier, printResults,
                                             refErrorsMedian, refErrorsMean, refErrorsMax);

    }

    /**
     * Generic test function for values of the range
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults) {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                               propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
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
                    final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });

                    final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
                    Assert.assertEquals(3, participants.length);
                    Assert.assertEquals(0.5 * Constants.SPEED_OF_LIGHT * participants[2].getDate().durationFrom(participants[0].getDate()),
                                        estimated.getEstimatedValue()[0],
                                        2.0e-8);

                    // the real state used for estimation is adjusted according to downlink delay
                    double adjustment = state.getDate().durationFrom(estimated.getStates()[0].getDate());
                    Assert.assertTrue(adjustment > 0.006);
                    Assert.assertTrue(adjustment < 0.010);

                    final double RangeEstimated = estimated.getEstimatedValue()[0];
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

        Assert.assertEquals(0.0, absErrorsMedian, 5.5e-8);
        Assert.assertEquals(0.0, absErrorsMin,    2.2e-7);
        Assert.assertEquals(0.0, absErrorsMax,    2.1e-7);
        Assert.assertEquals(0.0, relErrorsMedian, 1.0e-14);
        Assert.assertEquals(0.0, relErrorsMax,    2.2e-14);


    }

    void genericTestStateDerivatives(final boolean isModifier, final boolean printResults,
                                     final double refErrorsPMedian, final double refErrorsPMean, final double refErrorsPMax,
                                     final double refErrorsVMedian, final double refErrorsVMean, final double refErrorsVMax) {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        final Propagator propagator = DSSTEstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
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
                    final double[][]      jacobian  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(new StateFunction() {
                        public double[] value(final SpacecraftState state) {
                            return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                        }
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                       OrbitType.CARTESIAN, PositionAngle.TRUE, 2.0, 3).value(state);

                    Assert.assertEquals(jacobianRef.length, jacobian.length);
                    Assert.assertEquals(jacobianRef[0].length, jacobian[0].length);

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

        Assert.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assert.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assert.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assert.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assert.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assert.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    void genericTestParameterDerivatives(final boolean isModifier, final boolean printResults,
                                         final double refErrorsMedian, final double refErrorsMean, final double refErrorsMax) {

        DSSTContext context = DSSTEstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final DSSTPropagatorBuilder propagatorBuilder =
                        context.createBuilder(true, 1.0e-6, 60.0, 0.001);

        // Create perfect range measurements
        for (final GroundStation station : context.stations) {
            station.getEastOffsetDriver().setSelected(true);
            station.getNorthOffsetDriver().setSelected(true);
            station.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        DSSTEstimationTestUtils.createMeasurements(propagator,
                                                               new DSSTRangeMeasurementCreator(context),
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
                        final double[] gradient  = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i]);
                        Assert.assertEquals(1, measurement.getDimension());
                        Assert.assertEquals(1, gradient.length);

                        // Compute a reference value using finite differences
                        final ParameterFunction dMkdP =
                                        Differentiation.differentiate(new ParameterFunction() {
                                            /** {@inheritDoc} */
                                            @Override
                                            public double value(final ParameterDriver parameterDriver) {
                                                return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                            }
                                        }, 3, 20.0 * drivers[i].getScale());
                        final double ref = dMkdP.value(drivers[i]);

                        if (printResults) {
                            System.out.format(Locale.US, "%10.3e  %10.3e  ", gradient[0]-ref, FastMath.abs((gradient[0]-ref)/ref));
                        }

                        final double relError = FastMath.abs((ref-gradient[0])/ref);
                        relErrorList.add(relError);
//                        Assert.assertEquals(ref, gradient[0], 6.1e-5 * FastMath.abs(ref));
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

        Assert.assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        Assert.assertEquals(0.0, relErrorsMean, refErrorsMean);
        Assert.assertEquals(0.0, relErrorsMax, refErrorsMax);

    }

}
