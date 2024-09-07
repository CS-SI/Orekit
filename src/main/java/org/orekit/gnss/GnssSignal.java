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
package org.orekit.gnss;

/** Intermediate level interface for radio waves related to GNSS common frequency.
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public interface GnssSignal extends RadioWave {

    /** Common frequency F0 in Hz (10.23 MHz). */
    double F0 = 10230000.0;

    /** Get the ratio f/f0, where {@link #F0 f0} is the common frequency.
     * @return ratio f/f0, where {@link #F0 f0} is the common frequency
     * @see #F0
     * @see #getFrequency()
     */
    double getRatio();

}
