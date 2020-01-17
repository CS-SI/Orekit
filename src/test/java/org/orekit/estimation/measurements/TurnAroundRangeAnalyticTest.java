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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.stat.descriptive.moment.Mean;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.modifiers.TurnAroundRangeTroposphericDelayModifier;
import org.orekit.models.earth.troposphere.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.StateFunction;

public class TurnAroundRangeAnalyticTest {


    /**
     * Test the values of the TAR (Turn-Around Range) comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Analytical Values\n");
        }
        // Run test
        genericTestValues(printResults);
    }

    /**
     * Test the values of the analytic state derivatives computation
     * using TurnAroundRange function as a comparison
     */
    @Test
    public void testStateDerivatives() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Analytical State Derivatives - Derivative Structure comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences  = false;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    1.4e-6, 1.4e-6, 2.6e-6, 9.7e-7, 1.0e-6, 2.1e-6);
    }

    /**
     * Test the values of the analytic state derivatives
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testStateDerivativesFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Analytical State Derivatives - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences  = true;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    5.9e-9, 2.0e-8, 3.5e-7, 7.3e-5, 3.4e-4, 1.2e-2);
    }

    /**
     * Test the values of the state derivatives with modifier
     * using TurnAroundRange function as a comparison
     */
    @Test
    public void testStateDerivativesWithModifier() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Analytical State Derivatives with Modifier - Derivative Structure comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences  = false;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    1.5e-6, 2.0e-6, 2.3e-5, 1.0e-6, 1.0e-6, 2.1e-6);
    }

    /**
     * Test the values of the state derivatives with modifier
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testStateDerivativesWithModifierFiniteDifferences() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Analytical State Derivatives with Modifier - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences  = true;
        genericTestStateDerivatives(isModifier, isFiniteDifferences, printResults,
                                    3.1e-8, 9.9e-8, 1.8e-6, 7.5e-5, 2.6e-4, 0.5e-2);
    }

    /**
     * Test the values of the analytic parameters derivatives computation
     * using TurnAroundRange function as a comparison
     */
    @Test
    public void testParameterDerivatives() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Analytical Parameter Derivatives - Derivative Structure comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences  = false;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults,
                                        4.4e-06, 6.9e-06, 1.3e-04, 3.4e-6, 4.9e-6, 3.6e-5);

    }

    /**
     * Test the values of the analytic parameters derivatives
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivativesFiniteDifferences() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Analytical Parameter Derivatives - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = false;
        boolean isFiniteDifferences  = true;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults,
                                        3.0e-06, 5.9e-06, 1.3e-04, 2.9e-6, 5.0e-6, 3.9e-5);

    }

    /**
     * Test the values of the analytic parameters derivatives with modifiers computation
     * using TurnAroundRange function as a comparison
     */
    @Test
    public void testParameterDerivativesWithModifier() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Analytical Parameter Derivatives with Modifier - Derivative Structure comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences  = false;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults,
                                        4.4e-06, 6.9e-06, 1.3e-04, 3.4e-6, 4.9e-6, 3.6e-5);

    }

    /**
     * Test the values of the analytic parameters derivatives with modifiers computation
     * using a numerical finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivativesWithModifierFiniteDifferences() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Analytical Parameter Derivatives with Modifier - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = true;
        boolean isFiniteDifferences  = true;
        genericTestParameterDerivatives(isModifier, isFiniteDifferences, printResults,
                                        3.0e-06, 5.9e-06, 1.3e-04, 2.9e-6, 5.0e-6, 3.9e-5);

    }

    /**
     * Generic test function for values of the TAR
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults)
                    {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        //Context context = EstimationTestUtils.geoStationnaryContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TurnAroundRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        double[] absoluteErrors = new double[measurements.size()];
        double[] relativeErrors = new double[measurements.size()];
        int index = 0;

        // Print the results ? Header
        if (printResults) {
           System.out.format(Locale.US, "%-15s  %-15s  %-23s  %-23s  %17s  %17s  %13s %13s%n",
                              "Master Station", "Slave Station",
                              "Measurement Date", "State Date",
                              "TAR observed [m]", "TAR estimated [m]",
                              "|ΔTAR| [m]", "rel |ΔTAR|");
        }

        // Loop on the measurements
        for (final ObservedMeasurement<?> measurement : measurements) {

            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(meanDelay);
            final SpacecraftState state     = propagator.propagate(date);

            // Values of the TAR & errors
            final double TARobserved  = measurement.getObservedValue()[0];
            final double TARestimated = new TurnAroundRangeAnalytic((TurnAroundRange) measurement).
                                                                    theoreticalEvaluationAnalytic(0, 0, propagator.getInitialState(), state).getEstimatedValue()[0];

            absoluteErrors[index] = TARestimated-TARobserved;
            relativeErrors[index] = FastMath.abs(absoluteErrors[index])/FastMath.abs(TARobserved);
            index++;

            // Print results ? Values
            if (printResults) {
                final AbsoluteDate measurementDate = measurement.getDate();

                String masterStationName = ((TurnAroundRange) measurement).getMasterStation().getBaseFrame().getName();
                String slaveStationName = ((TurnAroundRange) measurement).getSlaveStation().getBaseFrame().getName();
                System.out.format(Locale.US, "%-15s  %-15s  %-23s  %-23s  %17.6f  %17.6f  %13.6e %13.6e%n",
                                  masterStationName, slaveStationName, measurementDate, date,
                                 TARobserved, TARestimated,
                                 FastMath.abs(TARestimated-TARobserved),
                                 FastMath.abs((TARestimated-TARobserved)/TARobserved));
            }

        }

        // Compute some statistics
        final double absErrorsMedian = new Median().evaluate(absoluteErrors);
        final double absErrorsMin    = new Min().evaluate(absoluteErrors);
        final double absErrorsMax    = new Max().evaluate(absoluteErrors);
        final double relErrorsMedian = new Median().evaluate(relativeErrors);
        final double relErrorsMax    = new Max().evaluate(relativeErrors);

        // Print the results on console ? Final results
        if (printResults) {
            System.out.println();
            System.out.println("Absolute errors median: " +  absErrorsMedian);
            System.out.println("Absolute errors min   : " +  absErrorsMin);
            System.out.println("Absolute errors max   : " +  absErrorsMax);
            System.out.println("Relative errors median: " +  relErrorsMedian);
            System.out.println("Relative errors max   : " +  relErrorsMax);
        }

        // Assert statistical errors
        Assert.assertEquals(0.0, absErrorsMedian, 8.4e-08);
        Assert.assertEquals(0.0, absErrorsMin,    9.0e-08);
        Assert.assertEquals(0.0, absErrorsMax,    2.0e-07);
        Assert.assertEquals(0.0, relErrorsMedian, 5.1e-15);
        Assert.assertEquals(0.0, relErrorsMax,    1.2e-14);
    }

    /**
     * Generic test function for derivatives with respect to state
     * @param isModifier Use of atmospheric modifiers
     * @param isFiniteDifferences Finite differences reference calculation if true, TurnAroundRange class otherwise
     * @param printResults Print the results ?
      */
    void genericTestStateDerivatives(final boolean isModifier, final boolean isFiniteDifferences, final boolean printResults,
                                     final double refErrorsPMedian, final double refErrorsPMean,
                                     final double refErrorsPMax, final double refErrorsVMedian,
                                     final double refErrorsVMean, final double refErrorsVMax)
                    {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");
        //Context context = EstimationTestUtils.geoStationnaryContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // create perfect range2 measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TurnAroundRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        double[] errorsP = new double[3 * measurements.size()];
        double[] errorsV = new double[3 * measurements.size()];
        int indexP = 0;
        int indexV = 0;

        // Print the results ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-15s  %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Master Station", "Slave Station",
                            "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Loop on the measurements
        for (final ObservedMeasurement<?> measurement : measurements) {

            // Add modifiers if test implies it
            final TurnAroundRangeTroposphericDelayModifier modifier =
                            new TurnAroundRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());
            if (isModifier) {
                ((TurnAroundRange) measurement).addModifier(modifier);
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
            final SpacecraftState state     = propagator.propagate(date);
            final EstimatedMeasurement<TurnAroundRange> TAR = new TurnAroundRangeAnalytic((TurnAroundRange)measurement).
                            theoreticalEvaluationAnalytic(0, 0, propagator.getInitialState(), state);
            if (isModifier) {
                modifier.modify(TAR);
            }
            final double[][] jacobian  = TAR.getStateDerivatives(0);

            // Jacobian reference value
            final double[][] jacobianRef;

            if (isFiniteDifferences) {
                // Compute a reference value using finite differences
                jacobianRef = Differentiation.differentiate(new StateFunction() {
                    public double[] value(final SpacecraftState state) {
                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue();
                    }
                }, measurement.getDimension(), propagator.getAttitudeProvider(),
                   OrbitType.CARTESIAN, PositionAngle.TRUE, 2.0, 3).value(state);
            } else {
                // Compute a reference value using TurnAroundRange class function
                jacobianRef = ((TurnAroundRange) measurement).theoreticalEvaluation(0, 0, new SpacecraftState[] { state }).getStateDerivatives(0);
            }

//            //Test: Test point by point with the debugger
//            if (!isFiniteDifferences && !isModifier) {
//                final EstimatedMeasurement<TurnAroundRange> test =
//                                new TurnAroundRangeAnalytic((TurnAroundRange)measurement).theoreticalEvaluationValidation(0, 0, state);
//            }
//            //Test

            Assert.assertEquals(jacobianRef.length, jacobian.length);
            Assert.assertEquals(jacobianRef[0].length, jacobian[0].length);

            double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
            double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                    dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                    if (j < 3) {
                        errorsP[indexP++] = dJacobianRelative[i][j];
                    } else {
                        errorsV[indexV++] = dJacobianRelative[i][j];
                    }
                }
            }
            // Print results on the console ? Print the Jacobian
            if (printResults) {
                String masterStationName = ((TurnAroundRange) measurement).getMasterStation().getBaseFrame().getName();
                String slaveStationName  = ((TurnAroundRange) measurement).getSlaveStation().getBaseFrame().getName();
                System.out.format(Locale.US, "%-15s  %-15s  %-23s  %-23s  " +
                                "%10.3e  %10.3e  %10.3e  " +
                                "%10.3e  %10.3e  %10.3e  " +
                                "%10.3e  %10.3e  %10.3e  " +
                                "%10.3e  %10.3e  %10.3e%n",
                                masterStationName, slaveStationName, measurement.getDate(), date,
                                dJacobian[0][0], dJacobian[0][1], dJacobian[0][2],
                                dJacobian[0][3], dJacobian[0][4], dJacobian[0][5],
                                dJacobianRelative[0][0], dJacobianRelative[0][1], dJacobianRelative[0][2],
                                dJacobianRelative[0][3], dJacobianRelative[0][4], dJacobianRelative[0][5]);
            }
        } // End loop on the measurements

        // Compute some statistics
        final double errorsPMedian = new Median().evaluate(errorsP);
        final double errorsPMean   = new Mean().evaluate(errorsP);
        final double errorsPMax    = new Max().evaluate(errorsP);
        final double errorsVMedian = new Median().evaluate(errorsV);
        final double errorsVMean   = new Mean().evaluate(errorsV);
        final double errorsVMax    = new Max().evaluate(errorsV);


        // Print the results on console ? Final results
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dR/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dR/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        // Assert the results / max values depend on the test
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
     * @param isFiniteDifferences Finite differences reference calculation if true, TurnAroundRange class otherwise
     * @param printResults Print the results ?
     */
    void genericTestParameterDerivatives(final boolean isModifier, final boolean isFiniteDifferences, final boolean printResults,
                                         final double refErrorQMMedian, final double refErrorQMMean, final double refErrorQMMax,
                                         final double refErrorQSMedian, final double refErrorQSMean, final double refErrorQSMax)
                    {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect TAR measurements
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            final GroundStation    masterStation = entry.getKey();
            final GroundStation    slaveStation  = entry.getValue();
            masterStation.getClockOffsetDriver().setSelected(false);
            masterStation.getEastOffsetDriver().setSelected(true);
            masterStation.getNorthOffsetDriver().setSelected(true);
            masterStation.getZenithOffsetDriver().setSelected(true);
            slaveStation.getClockOffsetDriver().setSelected(false);
            slaveStation.getEastOffsetDriver().setSelected(true);
            slaveStation.getNorthOffsetDriver().setSelected(true);
            slaveStation.getZenithOffsetDriver().setSelected(true);
        }
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new TurnAroundRangeMeasurementCreator(context),
                                                               1.0, 3.0, 300.0);
        propagator.setSlaveMode();

        // Print results on console ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s %-15s %-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Master Station", "Slave Station",
                            "Measurement Date", "State Date",
                            "ΔdQMx", "rel ΔdQMx",
                            "ΔdQMy", "rel ΔdQMy",
                            "ΔdQMz", "rel ΔdQMz",
                            "ΔdQSx", "rel ΔdQSx",
                            "ΔdQSy", "rel ΔdQSy",
                            "ΔdQSz", "rel ΔdQSz");
         }

        // List to store the results for master and slave station
        final List<Double> relErrorQMList = new ArrayList<Double>();
        final List<Double> relErrorQSList = new ArrayList<Double>();

        // Loop on the measurements
        for (final ObservedMeasurement<?> measurement : measurements) {

            // Add modifiers if test implies it
            final TurnAroundRangeTroposphericDelayModifier modifier = new TurnAroundRangeTroposphericDelayModifier(SaastamoinenModel.getStandardModel());
            if (isModifier) {
                ((TurnAroundRange) measurement).addModifier(modifier);
            }

            // parameter corresponding to station position offset
            final GroundStation masterStationParameter = ((TurnAroundRange) measurement).getMasterStation();
            final GroundStation slaveStationParameter = ((TurnAroundRange) measurement).getSlaveStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
            final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
            final SpacecraftState state     = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                                                                     masterStationParameter.getEastOffsetDriver(),
                                                                     masterStationParameter.getNorthOffsetDriver(),
                                                                     masterStationParameter.getZenithOffsetDriver(),
                                                                     slaveStationParameter.getEastOffsetDriver(),
                                                                     slaveStationParameter.getNorthOffsetDriver(),
                                                                     slaveStationParameter.getZenithOffsetDriver(),
            };

            // Print results on console ? Stations' names
            if (printResults) {
                String masterStationName  = masterStationParameter.getBaseFrame().getName();
                String slaveStationName  = slaveStationParameter.getBaseFrame().getName();
                System.out.format(Locale.US, "%-15s %-15s %-23s  %-23s  ",
                                  masterStationName, slaveStationName, measurement.getDate(), date);
            }

            // Loop on the parameters
            for (int i = 0; i < 6; ++i) {
                // Analytical computation of the parameters derivatives
                final EstimatedMeasurement<TurnAroundRange> TAR =
                                new TurnAroundRangeAnalytic((TurnAroundRange) measurement).
                                theoreticalEvaluationAnalytic(0, 0, propagator.getInitialState(), state);
                // Optional modifier addition
                if (isModifier) {
                  modifier.modify(TAR);
                }
                final double[] gradient  = TAR.getParameterDerivatives(drivers[i]);

                Assert.assertEquals(1, measurement.getDimension());
                Assert.assertEquals(1, gradient.length);

                // Reference value
                double ref;
                if (isFiniteDifferences) {
                    // Compute a reference value using finite differences
                    final ParameterFunction dMkdP =
                                    Differentiation.differentiate(new ParameterFunction() {
                                        /** {@inheritDoc} */
                                        @Override
                                        public double value(final ParameterDriver parameterDriver) {
                                            return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                        }
                                    }, 3, 20.0 * drivers[i].getScale());
                    ref = dMkdP.value(drivers[i]);
                } else {
                    // Compute a reference value using TurnAroundRange function
                    ref = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i])[0];
                }

                // Deltas
                double dGradient         = gradient[0] - ref;
                double dGradientRelative = FastMath.abs(dGradient/ref);

                // Print results on console ? Gradient difference
                if (printResults) {
                    System.out.format(Locale.US, "%10.3e  %10.3e  ", dGradient, dGradientRelative);
                }

                // Add relative error to the list
                if (i<3) { relErrorQMList.add(dGradientRelative);}
                else     { relErrorQSList.add(dGradientRelative);}

            } // End for loop on the parameters
            if (printResults) {
                System.out.format(Locale.US, "%n");
            }
        } // End for loop on the measurements

        // Convert error list to double[]
        final double relErrorQM[] = relErrorQMList.stream().mapToDouble(Double::doubleValue).toArray();
        final double relErrorQS[] = relErrorQSList.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute statistics
        final double relErrorsQMMedian = new Median().evaluate(relErrorQM);
        final double relErrorsQMMean   = new Mean().evaluate(relErrorQM);
        final double relErrorsQMMax    = new Max().evaluate(relErrorQM);

        final double relErrorsQSMedian = new Median().evaluate(relErrorQS);
        final double relErrorsQSMean   = new Mean().evaluate(relErrorQS);
        final double relErrorsQSMax    = new Max().evaluate(relErrorQS);


        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dR/dQ master station -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              relErrorsQMMedian, relErrorsQMMean, relErrorsQMMax);
            System.out.format(Locale.US, "Relative errors dR/dQ slave station  -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              relErrorsQSMedian, relErrorsQSMean, relErrorsQSMax);
        }

        // Check values
        Assert.assertEquals(0.0, relErrorsQMMedian, refErrorQMMedian);
        Assert.assertEquals(0.0, relErrorsQMMean, refErrorQMMean);
        Assert.assertEquals(0.0, relErrorsQMMax, refErrorQMMax);
        Assert.assertEquals(0.0, relErrorsQSMedian, refErrorQSMedian);
        Assert.assertEquals(0.0, relErrorsQSMean, refErrorQSMean);
        Assert.assertEquals(0.0, relErrorsQSMax, refErrorQSMax);

    }

}
