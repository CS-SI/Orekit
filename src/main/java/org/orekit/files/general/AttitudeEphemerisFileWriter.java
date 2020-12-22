/* Copyright 2016 Applied Defense Solutions (ADS)
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * ADS licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.files.general;

import java.io.IOException;

/**
 * An interface for writing out ephemeris files to disk.
 *
 * <p>
 * An attitude ephemeris consists of one or more satellites each an ID unique
 * within the file. The ephemeris for each satellite consists of one or more
 * segments.
 *
 * <p>
 * Ephemeris file formats may have additional settings that need to be
 * configured to be compliant with their formats.
 *
 * @author Raphaël Fermé
 * @since 10.3
 *
 */
public interface AttitudeEphemerisFileWriter {

    /**
     * Write file header.
     *
     * @param writer a configured Appendable to feed with text
     * @throws IOException if any buffer writing operations fail
     */
    void writeHeader(Appendable writer) throws IOException;

    /**
     * Write one segment.
     *
     * @param writer a configured Appendable to feed with text
     * @param metadata segment metadata
     * @param data segment data
     * @throws IOException if any buffer writing operations fail
     */
    void writeSegment(Appendable writer,
                      AttitudeEphemerisFile.AttitudeEphemerisSegmentMetadata metadata,
                      AttitudeEphemerisFile.AttitudeEphemerisSegmentData data)
        throws IOException;

}
