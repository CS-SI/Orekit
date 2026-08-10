/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.orekit.frames.Frame;
import org.orekit.gnss.TimeSystem;
import org.orekit.utils.units.Unit;

import java.util.List;
/** Container for ORBEX file description block.
 * @param description         description
 * @param createdBy           name of agency which created the file
 * @param inputData           input used to generate this file
 * @param contact             E-mail address of the relevant contact person
 * @param timeSystem          time system
 * @param epochInterval       number of seconds between each epoch (NaN if irregular)
 * @param coordinateSystem    name of reference frame
 * @param frameType           frame type
 * @param orbitType           orbit type
 * @param recordTypes         recordTypes
 * @param orbitReference      orbit reference
 * @param positionUnit        unit for position
 * @param velocityUnit        unit for velocity
 * @param clockCorrectionUnit unit for clock correction
 * @param clockRateUnit       unit for clock rate
 * @author Luc Maisonobe
 * @since 14.0
 */
public record Description(String description, String createdBy, String inputData,
                          String contact, TimeSystem timeSystem, double epochInterval,
                          Frame coordinateSystem, String frameType, String orbitType,
                          List<EphemerisDataPredicate> recordTypes, String orbitReference,
                          Unit positionUnit, Unit velocityUnit,
                          Unit clockCorrectionUnit, Unit clockRateUnit) {
    // nothing to do
}
