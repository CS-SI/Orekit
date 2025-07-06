/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.clock;

/** Clock data type.
 * In case of a DR type, clock data are in the sense of clock value after discontinuity minus prior.
 * In other cases, clock data are in the sense of reported station/satellite clock minus reference clock value. */
public enum ClockDataType {

    /** Data analysis for receiver clocks. Clock Data are */
    AR,

    /** Data analysis for satellite clocks. */
    AS,

    /** Calibration measurement for a single GPS receiver. */
    CR,

    /** Discontinuity measurements for a single GPS receiver. */
    DR

}

