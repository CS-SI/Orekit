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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.Map;

/** Measurements types. */
abstract class MeasurementsParser<T extends ObservedMeasurement<T>> {

    /** Parse the fields of a measurements line.
     * @param fields measurements line fields
     * @param stations name to stations data map
     * @param pvData PV measurements data
     * @param satellite satellite reference
     * @param satRangeBias range bias due to transponder delay
     * @param weight base weights for measurements
     * @param line complete line
     * @param lineNumber line number
     * @param fileName file name
     * @return parsed measurement
     */
    public abstract T parseFields(String[] fields,
                                  Map<String, StationData> stations,
                                  PVData pvData, ObservableSatellite satellite,
                                  Bias<Range> satRangeBias, Weights weight,
                                  String line, int lineNumber, String fileName);

    /** Check the number of fields.
     * @param expected expected number of fields
     * @param fields measurements line fields
     * @param line complete line
     * @param lineNumber line number
     * @param fileName file name
     */
    protected void checkFields(final int expected, final String[] fields,
                               final String line, final int lineNumber, final String fileName) {
        if (fields.length != expected) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, fileName, line);
        }
    }

    /** Get the date for the line.
     * @param date date field
     * @param line complete line
     * @param lineNumber line number
     * @param fileName file name
     * @return parsed measurement
     */
    protected AbsoluteDate getDate(final String date,
                                   final String line, final int lineNumber, final String fileName) {
        try {
            return new AbsoluteDate(date, TimeScalesFactory.getUTC());
        } catch (OrekitException oe) {
            throw generateException("wrong date " + date, line, lineNumber, fileName);
        }
    }

    /** Get the station data for the line.
     * @param stationName name of the station
     * @param stations name to stations data map
     * @param line complete line
     * @param lineNumber line number
     * @param fileName file name
     * @return parsed measurement
     */
    protected StationData getStationData(final String stationName,
                                         final Map<String, StationData> stations,
                                         final String line, final int lineNumber, final String fileName) {
        final StationData stationData = stations.get(stationName);
        if (stationData == null) {
            throw generateException("unknown station " + stationName, line, lineNumber, fileName);
        }
        return stationData;
    }

    /** Generate an exception.
     * @param detail message detail
     * @param line complete line
     * @param lineNumber line number
     * @param fileName file name
     * @return generated exception
     */
    private static OrekitException generateException(final String detail, final String line,
                                                     final int lineNumber, final String fileName) {
        return new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                   detail +
                                   " at line " + lineNumber +
                                   " in file " + fileName +
                                   "\n" + line);
    }

}
