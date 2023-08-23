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

package org.orekit.estimation.common;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;

import java.util.Map;

/** Parser for PV measurements.
 * @author Luc Maisonobe
 */
class PVParser extends MeasurementsParser<PV> {
    /** {@inheritDoc} */
    @Override
    public PV parseFields(final String[] fields,
                          final Map<String, StationData> stations,
                          final PVData pvData,
                          final ObservableSatellite satellite,
                          final Bias<Range> satRangeBias,
                          final Weights weights,
                          final String line,
                          final int lineNumber,
                          final String fileName) {
        // field 2, which corresponds to stations in other measurements, is ignored
        // this allows the measurements files to be columns aligned
        // by inserting something like "----" instead of a station name
        checkFields(9, fields, line, lineNumber, fileName);
        return new org.orekit.estimation.measurements.PV(getDate(fields[0], line, lineNumber, fileName),
                                                         new Vector3D(Double.parseDouble(fields[3]) * 1000.0,
                                                                      Double.parseDouble(fields[4]) * 1000.0,
                                                                      Double.parseDouble(fields[5]) * 1000.0),
                                                         new Vector3D(Double.parseDouble(fields[6]) * 1000.0,
                                                                      Double.parseDouble(fields[7]) * 1000.0,
                                                                      Double.parseDouble(fields[8]) * 1000.0),
                                                         pvData.getPositionSigma(),
                                                         pvData.getVelocitySigma(),
                                                         weights.getPVBaseWeight(),
                                                         satellite);
    }
}
