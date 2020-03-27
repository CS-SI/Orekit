/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.util.FastMath;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.time.AbsoluteDate;


/** Single frequency cycle slip detectors.
 * The detector is based the algorithm given in <a
 * href="https://gssc.esa.int/navipedia/index.php/Examples_of_single_frequency_Cycle-Slip_Detectors">
 * Examples of single frequency Cycle-Slip Detectors</a> by Zornoza and M. Hernández-Pajares. Within this class
 * a polynomial is used to smooth the data. We consider a cycle_slip occurring if the current measurement is  too
 * far from the one predicted with the polynomial (algorithm 1 on Navipedia).
 * <p>
 * For building the detector, one should give a RINEX file a threshold, a gap time limit, and an integer.
 * After construction of the detectors, one can have access to a List of CycleData. Each CycleDate represents
 * a link between the station (define by the RINEX file) and a satellite at a specific frequency. For each cycle data,
 * one has access to the begin and end of availability, and a sorted set which contains all the date at which
 * cycle-slip have been detected
 * </p>
 * <p>
 * @author David Soulard
 *
 */
public class PolynomialSingleFrequencyCycleSlipDetector extends AbstractCycleSlipDetector {

    /** Order of the polynomial used for fitting. */
    private final int order;

    /** Polynomial single frequency cycle-slip detector Constructor.
     * @param obserDataSets observationDataSet list from a RINEX file
     * @param dt time gap threshold between two consecutive measurement (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param threshold threshold above which cycle-slip occurs
     * @param n number of measurement before starting
     * @param m polynomial order
     */
    public PolynomialSingleFrequencyCycleSlipDetector(final List<ObservationDataSet> obserDataSets, final double dt, final double threshold, final int n, final int m) {
        super(obserDataSets, dt, threshold, n);
        this.order = m;
        for (ObservationDataSet obser: obserDataSets) {
            setStationName(obser.getHeader().getMarkerName());
            manageData(obser, threshold);
        }
    }

    /** {@inheritDoc}. */
    @Override
    protected int cycleSlipDetection(final String nameSat, final AbsoluteDate currentDate, final double value, final Frequency freq, final double threshold) {
        final List<CycleSlipDetectorResults> data = getResults();
        final List<Map<Frequency, DataForDetection>> stuff = getStuffReference();
        if (data != null) {
            for (CycleSlipDetectorResults d: data) {
                //found the right cycle data
                if (d.getSatelliteName().compareTo(nameSat) == 0 && d.getCycleSlipMap().containsKey(freq)) {
                    final Map<Frequency, DataForDetection> values = stuff.get(data.indexOf(d));
                    final DataForDetection v = values.get(freq);
                    //Check the time gap condition
                    if (FastMath.abs(currentDate.durationFrom(v.getFiguresReference()[v.getWrite()].getDate())) > getMaxTimeBeetween2Measurement()) {
                        d.addCycleSlipDate(freq, currentDate);
                        v.resetFigures( new SlipComputationData[getMinMeasurementNumber()], value, currentDate);
                        d.setDate(freq, currentDate);
                        return 0;
                    }
                    //Compute the fitting polynamil if there are enough measurement since last cycle-slip
                    if (v.getCanBeComputed() >= getMinMeasurementNumber()) {
                        final List<WeightedObservedPoint> xy = new ArrayList<>();
                        for (int i = 0; i < getMinMeasurementNumber(); i++) {
                            final SlipComputationData current = v.getFiguresReference()[i];
                                //System.out.println("delat T = " + current.date.durationFrom(currentDate));
                                //System.out.println("d = " + current.value);
                            xy.add(new WeightedObservedPoint(1.0, current.getDate().durationFrom(currentDate),
                                                                 current.getValue()));
                        }
                        final PolynomialCurveFitter fitting = PolynomialCurveFitter.create(order);
                        //Check if there is a cycle_slip
                        if (FastMath.abs(fitting.fit(xy)[0] - value) > threshold) {
                            d.addCycleSlipDate(freq, currentDate);
                            v.resetFigures( new SlipComputationData[getMinMeasurementNumber()], value, currentDate);
                            d.setDate(freq, currentDate);
                            return 0;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return 1;
    }
}
