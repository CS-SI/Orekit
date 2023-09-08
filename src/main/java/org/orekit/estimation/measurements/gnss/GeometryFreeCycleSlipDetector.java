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
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;


/**
 * Geometry free cycle slip detectors.
 * The detector is based the algorithm given in <a
 * href="https://gssc.esa.int/navipedia/index.php/Detector_based_in_carrier_phase_data:_The_geometry-free_combination">
 * Detector based in carrier phase data: The geometry-free combination</a> by Zornoza and M. Hernández-Pajares. Within this class
 * a second order polynomial is used to smooth the data. We consider a cycle-slip occurring if the current measurement is  too
 * far from the one predicted with the polynomial.
 * <p>
 * For building the detector, one should give a threshold and a gap time limit.
 * After construction of the detectors, one can have access to a List of CycleData. Each CycleDate represents
 * a link between the station (define by the RINEX file) and a satellite at a specific frequency. For each cycle data,
 * one has access to the begin and end of availability, and a sorted set which contains all the date at which
 * cycle-slip have been detected
 * </p>
 * @author David Soulard
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class GeometryFreeCycleSlipDetector extends AbstractCycleSlipDetector {

    /** Threshold above which cycle-slip occurs. */
    private final double threshold;

    /**
     * Constructor.
     * @param dt time gap threshold between two consecutive measurement (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param threshold threshold above which cycle-slip occurs
     * @param n number of measurement before starting
     */
    public GeometryFreeCycleSlipDetector(final double dt, final double threshold, final int n) {
        super(dt, n);
        this.threshold = threshold;
    }


    /** {@inheritDoc} */
    @Override
    protected void manageData(final ObservationDataSet observation) {

        // Extract observation data
        final int             prn    = observation.getSatellite().getPRN();
        final AbsoluteDate    date   = observation.getDate();
        final SatelliteSystem system = observation.getSatellite().getSystem();

        // Geometry-free combination of measurements
        final GeometryFreeCombination geometryFree = MeasurementCombinationFactory.getGeometryFreeCombination(system);
        final CombinedObservationDataSet cods = geometryFree.combine(observation);

        // Initialize list of measurements
        final List<CombinedObservationData> phasesGF = new ArrayList<>();

        // Loop on observation data to fill lists
        for (final CombinedObservationData cod : cods.getObservationData()) {
            if (!Double.isNaN(cod.getValue()) && cod.getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                phasesGF.add(cod);
            }
        }

        // Loop on Geometry-free phase measurements
        for (CombinedObservationData cod : phasesGF) {
            final String nameSat = setName(prn, observation.getSatellite().getSystem());
            // Check for cycle-slip detection
            final Frequency frequency = cod.getUsedObservationData().get(0).getObservationType().getFrequency(system);
            final boolean slip = cycleSlipDetection(nameSat, date, cod.getValue(), frequency);
            if (!slip) {
                // Update cycle slip data
                cycleSlipDataSet(nameSat, date, cod.getValue(), cod.getUsedObservationData().get(0).getObservationType().getFrequency(system));
            }
        }

    }

    /**
     * Compute if there is a cycle slip at an specific date.
     * @param nameSat name of the satellite, on the pre-defined format (e.g.: GPS - 07 for satellite 7 of GPS constellation)
     * @param currentDate the date at which we check if a cycle-slip occurs
     * @param valueGF geometry free measurement
     * @param frequency frequency used
     * @return true if a cycle slip has been detected.
     */
    private boolean cycleSlipDetection(final String nameSat, final AbsoluteDate currentDate,
                                       final double valueGF, final Frequency frequency) {

        // Access the cycle slip results to know if a cycle-slip already occurred
        final List<CycleSlipDetectorResults>         data  = getResults();
        final List<Map<Frequency, DataForDetection>> stuff = getStuffReference();

        // If a cycle-slip already occurred
        if (data != null) {

            // Loop on cycle-slip results
            for (CycleSlipDetectorResults resultGF : data) {

                // Found the right cycle data
                if (resultGF.getSatelliteName().compareTo(nameSat) == 0 && resultGF.getCycleSlipMap().containsKey(frequency)) {
                    final Map<Frequency, DataForDetection> values = stuff.get(data.indexOf(resultGF));
                    final DataForDetection dataForDetection = values.get(frequency);

                    // Check the time gap condition
                    final double deltaT = FastMath.abs(currentDate.durationFrom(dataForDetection.getFiguresReference()[dataForDetection.getWrite()].getDate()));
                    if (deltaT > getMaxTimeBeetween2Measurement()) {
                        resultGF.addCycleSlipDate(frequency, currentDate);
                        dataForDetection.resetFigures(new SlipComputationData[getMinMeasurementNumber()], valueGF, currentDate);
                        resultGF.setDate(frequency, currentDate);
                        return true;
                    }

                    // Compute the fitting polynomial if there are enough measurement since last cycle-slip
                    if (dataForDetection.getCanBeComputed() >= getMinMeasurementNumber()) {
                        final List<WeightedObservedPoint> xy = new ArrayList<>();
                        for (int i = 0; i < getMinMeasurementNumber(); i++) {
                            final SlipComputationData current = dataForDetection.getFiguresReference()[i];
                            xy.add(new WeightedObservedPoint(1.0, current.getDate().durationFrom(currentDate),
                                                             current.getValue()));
                        }

                        final PolynomialCurveFitter fitting = PolynomialCurveFitter.create(2);
                        // Check if there is a cycle_slip
                        if (FastMath.abs(fitting.fit(xy)[0] - valueGF) > threshold) {
                            resultGF.addCycleSlipDate(frequency, currentDate);
                            dataForDetection.resetFigures(new SlipComputationData[getMinMeasurementNumber()], valueGF, currentDate);
                            resultGF.setDate(frequency, currentDate);
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
