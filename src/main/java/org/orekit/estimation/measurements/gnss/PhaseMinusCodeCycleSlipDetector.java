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
import java.util.List;
import java.util.Map;

import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Phase minus code cycle slip detectors.
 * The detector is based the algorithm given in <a
 * href="https://gssc.esa.int/navipedia/index.php/Examples_of_single_frequency_Cycle-Slip_Detectors">
 * Examples of single frequency Cycle-Slip Detectors</a> by Zornoza and M. Hernández-Pajares. Within this class
 * a polynomial is used to smooth the data. We consider a cycle_slip occurring if the current measurement is  too
 * far from the one predicted with the polynomial (algorithm 1 on Navipedia).
 * <p>
 * For building the detector, one should give a threshold and a gap time limit.
 * After construction of the detectors, one can have access to a List of CycleData. Each CycleDate represents
 * a link between the station (define by the RINEX file) and a satellite at a specific frequency. For each cycle data,
 * one has access to the begin and end of availability, and a sorted set which contains all the date at which
 * cycle-slip have been detected
 * </p>
 * @author David Soulard
 * @since 10.2
 */
public class PhaseMinusCodeCycleSlipDetector extends AbstractCycleSlipDetector {

    /** Mega Hertz to Hertz conversion factor. */
    private static final double MHZ_TO_HZ = 1.0e6;

    /** Order of the polynomial used for fitting. */
    private final int order;

    /** Threshold above which cycle-slip occurs. */
    private final double threshold;

    /** Polynomial single frequency cycle-slip detector Constructor.
     * @param dt time gap threshold between two consecutive measurement (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param threshold threshold above which cycle-slip occurs
     * @param n number of measurement before starting
     * @param order polynomial order
     */
    public PhaseMinusCodeCycleSlipDetector(final double dt, final double threshold,
                                           final int n, final int order) {
        super(dt, n);
        this.threshold = threshold;
        this.order     = order;
    }

    /** {@inheritDoc} */
    @Override
    protected void manageData(final ObservationDataSet observation) {

        // Extract observation data
        final SatelliteSystem system = observation.getSatellite().getSystem();
        final int             prn    = observation.getSatellite().getPRN();
        final AbsoluteDate    date   = observation.getDate();

        // Initialize list of measurements
        final List<ObservationData> pseudoRanges = new ArrayList<>();
        final List<ObservationData> phases       = new ArrayList<>();

        // Loop on observation data to fill lists
        for (final ObservationData od : observation.getObservationData()) {
            if (!Double.isNaN(od.getValue())) {
                if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                    pseudoRanges.add(od);
                } else if (od.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                    phases.add(od);
                }
            }
        }

        // Loop on phase measurements
        for (final ObservationData phase : phases) {
            // Loop on range measurement
            for (final ObservationData pseudoRange : pseudoRanges) {
                // Change unit of phase measurement
                final double frequency = phase.getObservationType().getFrequency(system).getMHzFrequency() * MHZ_TO_HZ;
                final double cOverF    = Constants.SPEED_OF_LIGHT / frequency;
                final ObservationData phaseInMeters = new ObservationData(phase.getObservationType(),
                                                                          cOverF * phase.getValue(),
                                                                          phase.getLossOfLockIndicator(),
                                                                          phase.getSignalStrength());

                // Check if measurement frequencies are the same
                if (phase.getObservationType().getFrequency(system) == pseudoRange.getObservationType().getFrequency(system)) {
                    // Phase minus Code combination
                    final PhaseMinusCodeCombination phaseMinusCode = MeasurementCombinationFactory.getPhaseMinusCodeCombination(system);
                    final CombinedObservationData cod = phaseMinusCode.combine(phaseInMeters, pseudoRange);
                    final String nameSat = setName(prn, observation.getSatellite().getSystem());

                    // Check for cycle-slip detection
                    final boolean slip = cycleSlipDetection(nameSat, date, cod.getValue(), phase.getObservationType().getFrequency(system));
                    if (!slip) {
                        // Update cycle slip data
                        cycleSlipDataSet(nameSat, date, cod.getValue(), phase.getObservationType().getFrequency(system));
                    }
                }
            }
        }

    }

    /**
     * Compute if there is a cycle slip at a specific date.
     * @param nameSat name of the satellite, on the predefined format (e.g. GPS - 07 for satellite 7 of GPS constellation)
     * @param currentDate the date at which we check if a cycle-slip occurs
     * @param phaseMinusCode phase measurement minus code measurement
     * @param frequency frequency used
     * @return true if a cycle slip has been detected.
     */
    private boolean cycleSlipDetection(final String nameSat, final AbsoluteDate currentDate,
                                       final double phaseMinusCode, final Frequency frequency) {

        // Access the cycle slip results to know if a cycle-slip already occurred
        final List<CycleSlipDetectorResults>         data  = getResults();
        final List<Map<Frequency, DataForDetection>> stuff = getStuffReference();

        // If a cycle-slip already occurred
        if (data != null) {

            // Loop on cycle-slip results
            for (CycleSlipDetectorResults resultPmC : data) {

                // Found the right cycle data
                if (resultPmC.getSatelliteName().compareTo(nameSat) == 0 && resultPmC.getCycleSlipMap().containsKey(frequency)) {
                    final Map<Frequency, DataForDetection> values = stuff.get(data.indexOf(resultPmC));
                    final DataForDetection v = values.get(frequency);

                    // Check the time gap condition
                    if (FastMath.abs(currentDate.durationFrom(v.getFiguresReference()[v.getWrite()].getDate())) > getMaxTimeBeetween2Measurement()) {
                        resultPmC.addCycleSlipDate(frequency, currentDate);
                        v.resetFigures( new SlipComputationData[getMinMeasurementNumber()], phaseMinusCode, currentDate);
                        resultPmC.setDate(frequency, currentDate);
                        return true;
                    }

                    // Compute the fitting polynomial if there are enough measurement since last cycle-slip
                    if (v.getCanBeComputed() >= getMinMeasurementNumber()) {
                        final List<WeightedObservedPoint> xy = new ArrayList<>();
                        for (int i = 0; i < getMinMeasurementNumber(); i++) {
                            final SlipComputationData current = v.getFiguresReference()[i];
                            xy.add(new WeightedObservedPoint(1.0, current.getDate().durationFrom(currentDate),
                                                                 current.getValue()));
                        }

                        final PolynomialCurveFitter fitting = PolynomialCurveFitter.create(order);
                        // Check if there is a cycle_slip
                        if (FastMath.abs(fitting.fit(xy)[0] - phaseMinusCode) > threshold) {
                            resultPmC.addCycleSlipDate(frequency, currentDate);
                            v.resetFigures( new SlipComputationData[getMinMeasurementNumber()], phaseMinusCode, currentDate);
                            resultPmC.setDate(frequency, currentDate);
                            return true;
                        }

                    } else {
                        break;
                    }

                }

            }

        }

        // No cycle-slip
        return false;
    }

}
