/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.estimation.measurements.generation;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.time.AbsoluteDate;


/** Interface for subscribing to generated {@link ObservedMeasurement measurements} events.
 * @author Luc Maisonobe
 * @since 12.0
 */
public interface GeneratedMeasurementSubscriber {

    /** Initialize subscriber at the start of a measurements generation.
     * <p>
     * This method is called once at the start of the measurements generation. It
     * may be used by the subscriber to initialize some internal data
     * if needed.
     * </p>
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     */
    void init(AbsoluteDate start, AbsoluteDate end);

    /** Handle a generated measurement.
     * @param measurement measurements that has just been generated
     */
    void handleGeneratedMeasurement(ObservedMeasurement<?> measurement);

}
