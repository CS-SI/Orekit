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
import java.util.Map;

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
import org.orekit.estimation.measurements.modifiers.TurnAroundRangeTroposphericDelayModifier;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

public class TurnAroundRangeTest {


    /**
     * Test the values of the TAR (Turn-Around Range) comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     * @throws OrekitException
     */
    @Test
    public void testValues() throws OrekitException {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR Values\n");
        }
        // Run test
        this.genericTestValues(printResults);
    }

    /**
     * Test the values of the state derivatives
     * using a numerical finite differences calculation as a reference
     * @throws OrekitException
     */
    @Test
    public void testStateDerivatives() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR State Derivatives - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = false;
        this.genericTestStateDerivatives(isModifier, printResults);
    }

    /**
     * Test the values of the state derivatives with modifier
     * using a numerical finite differences calculation as a reference
     * @throws OrekitException
     */
    @Test
    public void testStateDerivativesWithModifier() throws OrekitException {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest TAR State Derivatives with Modifier - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = true;
        this.genericTestStateDerivatives(isModifier, printResults);
    }

    /**
     * Test the values of the parameter derivatives
     * using a numerical finite differences calculation as a reference
     * @throws OrekitException
     */
    @Test
    public void testParameterDerivatives() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Parameter Derivatives - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = false;
        this.genericTestParameterDerivatives(isModifier, printResults);

    }
    /**
     * Test the values of the parameter derivatives with modifier
     * using a numerical finite differences calculation as a reference
     * @throws OrekitException
     */
    @Test
    public void testParameterDerivativesWithModifier() throws OrekitException {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest TAR Parameter Derivatives with Modifier - Finite Differences comparison\n");
        }
        // Run test
        boolean isModifier = true;
        this.genericTestParameterDerivatives(isModifier, printResults);
    }

    /**
     * Generic test function for values of the TAR
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestValues(final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();
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
            final double TARestimated = measurement.estimate(0, 0, state).getEstimatedValue()[0];


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
        Assert.assertEquals(0.0, absErrorsMedian, 1e-8);
        Assert.assertEquals(0.0, absErrorsMin, 2.5e-7);
        Assert.assertEquals(0.0, absErrorsMax, 2.5e-7);
        Assert.assertEquals(0.0, relErrorsMedian, 5.5e-15);
        Assert.assertEquals(0.0, relErrorsMax , 2e-14);
    }

    /**
     * Generic test function for derivatives with respect to state
     * @param isModifier Use of atmospheric modifiers
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestStateDerivatives(final boolean isModifier, final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();
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
            final double[][]      jacobian  = measurement.estimate(0, 0, state).getStateDerivatives();

            // Jacobian reference value
            final double[][] jacobianRef;

            // Compute a reference value using finite differences
            jacobianRef = EstimationUtils.differentiate(new StateFunction() {
                public double[] value(final SpacecraftState state) throws OrekitException {
                    return measurement.estimate(0, 0, state).getEstimatedValue();
                }
            }, measurement.getDimension(), OrbitType.CARTESIAN, PositionAngle.TRUE, 1.0, 3).value(state);

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
        double refErrorsPMedian, refErrorsPMean, refErrorsPMax;
        double refErrorsVMedian, refErrorsVMean, refErrorsVMax;

        // Finite differences reference comparison
        refErrorsPMedian = 1.1e-06;
        refErrorsPMean   = 1.1e-06;
        refErrorsPMax    = 2.3e-06;

        refErrorsVMedian = 1.5e-04;
        refErrorsVMean   = 4.7e-04;
        refErrorsVMax    = 5.3e-03;

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
     * @param printResults Print the results ?
     * @throws OrekitException
     */
    void genericTestParameterDerivatives(final boolean isModifier, final boolean printResults)
                    throws OrekitException {

        Context context = EstimationTestUtils.eccentricContext();

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect TAR measurements
        for (Map.Entry<GroundStation, GroundStation> entry : context.TARstations.entrySet()) {
            final GroundStation    masterStation = entry.getKey();
            final GroundStation    slaveStation  = entry.getValue();
            masterStation.getEastOffsetDriver().setSelected(true);
            masterStation.getNorthOffsetDriver().setSelected(true);
            masterStation.getZenithOffsetDriver().setSelected(true);
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
                final double[] gradient  = measurement.estimate(0, 0, state).getParameterDerivatives(drivers[i]);
                Assert.assertEquals(1, measurement.getDimension());
                Assert.assertEquals(1, gradient.length);

                // Compute a reference value using finite differences
                final ParameterFunction dMkdP =
                                EstimationUtils.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver) throws OrekitException {
                                        return measurement.estimate(0, 0, state).getEstimatedValue()[0];
                                    }
                                }, drivers[i], 3, 20.0);
                final double ref = dMkdP.value(drivers[i]);

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

        // Assert the results / max values depend on the test
        double refErrorQMMedian, refErrorQMMean, refErrorQMMax;
        double refErrorQSMedian, refErrorQSMean, refErrorQSMax;

        // Finite differences reference values
        refErrorQMMedian = 1.3e-10;
        refErrorQMMean   = 2.3e-10;
        refErrorQMMax    = 3.3e-09;

        refErrorQSMedian = 2.1e-06;
        refErrorQSMean   = 2.1e-06;
        refErrorQSMax    = 4.1e-06;

        // Check values
        Assert.assertEquals(0.0, relErrorsQMMedian, refErrorQMMedian);
        Assert.assertEquals(0.0, relErrorsQMMean, refErrorQMMean);
        Assert.assertEquals(0.0, relErrorsQMMax, refErrorQMMax);
        Assert.assertEquals(0.0, relErrorsQSMedian, refErrorQSMedian);
        Assert.assertEquals(0.0, relErrorsQSMean, refErrorQSMean);
        Assert.assertEquals(0.0, relErrorsQSMax, refErrorQSMax);

    }
}
