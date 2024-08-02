/* Copyright 2002-2024 Thales Alenia Space
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.moment.Mean;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class InterSatellitesOneWayRangeRateTest {

    /**
     * Test the values of the range rate comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     */
    @Test
    void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest inter-satellites Range Rate Values\n");
        }
        // Run test
        this.genericTestValues(printResults);
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    void testStateDerivativesEmitter() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest inter-satellites Range Rate State Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsPMedian = 7.1e-10;
        double refErrorsPMean   = 7.1e-09;
        double refErrorsPMax    = 1.8e-06;
        double refErrorsVMedian = 2.4e-10;
        double refErrorsVMean   = 5.2e-10;
        double refErrorsVMax    = 2.5e-08;
        this.genericTestStateDerivatives(printResults, 0,
                                         refErrorsPMedian, refErrorsPMean, refErrorsPMax,
                                         refErrorsVMedian, refErrorsVMean, refErrorsVMax);
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    void testStateDerivativesTransit() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest inter-satellites Range Rate State Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsPMedian = 7.1e-10;
        double refErrorsPMean   = 7.1e-09;
        double refErrorsPMax    = 1.8e-06;
        double refErrorsVMedian = 2.6e-010;
        double refErrorsVMean   = 4.9e-10;
        double refErrorsVMax    = 1.1e-08;
        this.genericTestStateDerivatives(printResults, 1,
                                         refErrorsPMedian, refErrorsPMean, refErrorsPMax,
                                         refErrorsVMedian, refErrorsVMean, refErrorsVMax);
    }

    /**
     * Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    void testParameterDerivatives() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest Range Rate Parameter Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsMedian = 2.2e-16;
        double refErrorsMean   = 1.2e-7;
        double refErrorsMax    = 2.9e-6;
        this.genericTestParameterDerivatives(printResults,
                                             refErrorsMedian, refErrorsMean, refErrorsMax);

    }

    /**
     * Generic test function for values of the inter-satellites range rate
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect inter-satellites range rate measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final double localClockOffset        =   0.14e-06;
        final double localClockRate          =  -0.12e-10;
        final double localClockAcceleration  =   1.4e-13;
        final double remoteClockOffset       = 469.0e-06;
        final double remoteClockRate         =  33.0e-10;
        final double remoteClockAcceleration =   0.5e-13;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new InterSatellitesOneWayRangeRateMeasurementCreator(ephemeris,
                                                                                                                    localClockOffset, localClockRate, localClockAcceleration,
                                                                                                                    remoteClockOffset, remoteClockRate, remoteClockAcceleration),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> absoluteErrors = new ArrayList<>();
        final List<Double> relativeErrors = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if (measurement.getDate().isAfter(interpolator.getPreviousState()) &&
                    measurement.getDate().isBeforeOrEqualTo(interpolator.getCurrentState())) {
                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);

                    // Values of the range rate & errors
                    final double rangeRateObserved  = measurement.getObservedValue()[0];
                    final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(new SpacecraftState[] {
                                                                                                             state,
                                                                                                             ephemeris.propagate(state.getDate())
                                                                                                         });

                    final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
                    assertEquals(2, participants.length);
                    final PVCoordinates delta = new PVCoordinates(participants[0], participants[1]);
                    final double radialVelocity = Vector3D.dotProduct(delta.getVelocity(), delta.getPosition().normalize());
                    final AbsoluteDate t0 = measurement.getSatellites().get(0).getClockOffsetDriver().getReferenceDate();
                    final double dtLocal    = measurement.getDate().durationFrom(t0);
                    final double localRate  = 2 * localClockAcceleration * dtLocal + localClockRate;
                    final double dtRemote   = participants[0].getDate().durationFrom(t0);
                    final double remoteRate = 2 * remoteClockAcceleration * dtRemote + remoteClockRate;
                    assertEquals(radialVelocity + Constants.SPEED_OF_LIGHT * (localRate - remoteRate),
                                        estimated.getEstimatedValue()[0],
                                        1.0e-7);

                    final double rangeRateEstimated = estimated.getEstimatedValue()[0];
                    final double absoluteError = rangeRateEstimated-rangeRateObserved;
                    absoluteErrors.add(absoluteError);
                    relativeErrors.add(FastMath.abs(absoluteError)/FastMath.abs(rangeRateObserved));

                    // Print results on console ?
                    if (printResults) {
                        final AbsoluteDate measurementDate = measurement.getDate();

                        System.out.format(Locale.US, "%-23s  %-23s  %19.6f  %19.6f  %13.6e  %13.6e%n",
                                         measurementDate.toStringWithoutUtcOffset(context.utc, 3),
                                         date.toStringWithoutUtcOffset(context.utc, 3),
                                         rangeRateObserved, rangeRateEstimated,
                                         FastMath.abs(rangeRateEstimated-rangeRateObserved),
                                         FastMath.abs((rangeRateEstimated-rangeRateObserved)/rangeRateObserved));
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        }); // End lambda function handlestep

        // Print results on console ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-23s  %-23s  %19s  %19s  %19s  %19s%n",
                              "Measurement Date", "State Date",
                              "range rate observed [m/s]", "range rate estimated [m/s]",
                              "Δrange rate [m/s]", "rel Δrange rate");
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

        assertEquals(0.0, absErrorsMedian, 3.7e-09);
        assertEquals(0.0, absErrorsMin,    2.6e-11);
        assertEquals(0.0, absErrorsMax,    1.5e-08);
        assertEquals(0.0, relErrorsMedian, 9.9e-10);
        assertEquals(0.0, relErrorsMax,    5.7e-8);

        // Test measurement type
        assertEquals(InterSatellitesOneWayRangeRate.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    void genericTestStateDerivatives(final boolean printResults, final int index,
                                     final double refErrorsPMedian, final double refErrorsPMean, final double refErrorsPMax,
                                     final double refErrorsVMedian, final double refErrorsVMean, final double refErrorsVMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect inter-satellites range rate measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final double localClockOffset        =   0.14e-06;
        final double localClockRate          =  -0.12e-10;
        final double localClockAcceleration  =   1.4e-13;
        final double remoteClockOffset       = 469.0e-06;
        final double remoteClockRate         =  33.0e-10;
        final double remoteClockAcceleration =   0.5e-13;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new InterSatellitesOneWayRangeRateMeasurementCreator(ephemeris,
                                                                                                                    localClockOffset, localClockRate, localClockAcceleration,
                                                                                                                    remoteClockOffset, remoteClockRate, remoteClockAcceleration),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<>();
        final List<Double> errorsV = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if (measurement.getDate().isAfter(interpolator.getPreviousState()) &&
                    measurement.getDate().isBeforeOrEqualTo(interpolator.getCurrentState())) {

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity.
                    final double            meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate      date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState[] states    = {
                        interpolator.getInterpolatedState(date),
                        ephemeris.propagate(date)
                    };
                    final double[][]      jacobian  = measurement.estimate(0, 0, states).getStateDerivatives(index);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(state -> {
                        final SpacecraftState[] s = states.clone();
                        s[index] = state;
                        return measurement.estimateWithoutDerivatives(s).getEstimatedValue();
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                    OrbitType.CARTESIAN, PositionAngleType.TRUE, 2.0, 3).value(states[index]);

                    assertEquals(jacobianRef.length, jacobian.length);
                    assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the Jacobian
                    double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
                    double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
                    for (int i = 0; i < jacobian.length; ++i) {
                        for (int j = 0; j < jacobian[i].length; ++j) {
                            dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                            dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                            if (j < 3) {
                                errorsP.add(dJacobianRelative[i][j]);
                            } else {
                                errorsV.add(dJacobianRelative[i][j]);
                            }
                        }
                    }
                    // Print values in console ?
                    if (printResults) {
                        System.out.format(Locale.US, "%-23s  %-23s  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e%n",
                                        measurement.getDate().toStringWithoutUtcOffset(context.utc, 3),
                                        date.toStringWithoutUtcOffset(context.utc, 3),
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
            System.out.format(Locale.US, "%-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements, primarily chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double[] and evaluate some statistics
        final double[] relErrorsP = errorsP.stream().mapToDouble(Double::doubleValue).toArray();
        final double[] relErrorsV = errorsV.stream().mapToDouble(Double::doubleValue).toArray();

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

        assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        assertEquals(0.0, errorsPMean, refErrorsPMean);
        assertEquals(0.0, errorsPMax, refErrorsPMax);
        assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        assertEquals(0.0, errorsVMean, refErrorsVMean);
        assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    void genericTestParameterDerivatives(final boolean printResults,
                                         final double refErrorsMedian, final double refErrorsMean, final double refErrorsMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect inter-satellites range rate measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit, propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // Create perfect range rate measurements
        final double localClockOffset        =   0.14e-06;
        final double localClockRate          =  -0.12e-10;
        final double localClockAcceleration  =   1.4e-13;
        final double remoteClockOffset       = 469.0e-06;
        final double remoteClockRate         =  33.0e-10;
        final double remoteClockAcceleration =   0.5e-13;
        final InterSatellitesOneWayRangeRateMeasurementCreator creator =
            new InterSatellitesOneWayRangeRateMeasurementCreator(ephemeris,
                                                                 localClockOffset, localClockRate, localClockAcceleration,
                                                                 remoteClockOffset, remoteClockRate, remoteClockAcceleration);
        creator.getLocalSatellite().getClockOffsetDriver().setSelected(true);
        creator.getLocalSatellite().getClockDriftDriver().setSelected(true);
        creator.getLocalSatellite().getClockAccelerationDriver().setSelected(true);
        creator.getRemoteSatellite().getClockOffsetDriver().setSelected(true);
        creator.getRemoteSatellite().getClockDriftDriver().setSelected(true);
        creator.getRemoteSatellite().getClockAccelerationDriver().setSelected(true);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator, creator, 1.0, 3.0, 300.0);

        // List to store the results
        final List<Double> relErrorList = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if (measurement.getDate().isAfter(interpolator.getPreviousState()) &&
                    measurement.getDate().isBeforeOrEqualTo(interpolator.getCurrentState())) {

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // range rate would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState[] states    = {
                        interpolator.getInterpolatedState(date),
                        ephemeris.propagate(date)
                    };
                    final ParameterDriver[] drivers = new ParameterDriver[] {
                        measurement.getSatellites().get(0).getClockOffsetDriver(),
                        measurement.getSatellites().get(0).getClockDriftDriver(),
                        measurement.getSatellites().get(0).getClockAccelerationDriver(),
                        measurement.getSatellites().get(1).getClockOffsetDriver(),
                        measurement.getSatellites().get(1).getClockDriftDriver(),
                        measurement.getSatellites().get(1).getClockAccelerationDriver()
                    };

                    for (final ParameterDriver driver : drivers) {
                        for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                            final double[] gradient  = measurement.estimate(0, 0, states).getParameterDerivatives(driver, span.getStart());
                            assertEquals(1, measurement.getDimension());
                            assertEquals(1, gradient.length);

                            // Compute a reference value using finite differences
                            final ParameterFunction dMkdP =
                                            Differentiation.differentiate(new ParameterFunction() {
                                                /** {@inheritDoc} */
                                                @Override
                                                public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                                                    return measurement.
                                                           estimateWithoutDerivatives(states).
                                                           getEstimatedValue()[0];
                                                }
                                            }, 3, 20.0 * driver.getScale());
                            final double ref = dMkdP.value(driver, span.getStart());

                            final double  relError;
                            if (ref == 0.0) {
                                // this protection is because range rate is completely independent for remote clock offset
                                // (it depends only on remote clock rate and acceleration), so ref is exactly 0.0
                                // so here we compute an absolute error and not a relative error (anyway, it is 0)
                                relError = ref - gradient[0];
                            } else {
                                relError = FastMath.abs((ref - gradient[0]) / ref);
                            }
                            relErrorList.add(relError);

                            if (printResults) {
                                System.out.format(Locale.US, "%10.3e  %10.3e  ", gradient[0]-ref, relError);
                            }

                        }
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
            System.out.format(Locale.US, "%-22s  %-22s  %-22s %-22s  %-22s  %-22s%n" +
                              "%10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s%n",
                              "local offset", "local rate", "local acceleration",
                              "remote offset", "remote rate", "remote acceleration",
                              "Δoₗ", "rel Δoₗ", "Δrₗ", "rel Δrₗ", "Δaₗ", "rel Δaₗ",
                              "Δoᵣ", "rel Δoᵣ", "Δrᵣ", "rel Δrᵣ", "Δaᵣ", "rel Δaᵣ");
         }

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert error list to double[]
        final double[] relErrors = relErrorList.stream().mapToDouble(Double::doubleValue).toArray();

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

        assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        assertEquals(0.0, relErrorsMean, refErrorsMean);
        assertEquals(0.0, relErrorsMax, refErrorsMax);

    }

}
