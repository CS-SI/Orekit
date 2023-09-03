/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.sp3;

import java.io.IOException;

import org.orekit.annotation.DefaultDataContext;

/** Writer for SP3 file.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class SP3Writer {

    /** Destination of generated output. */
    private final Appendable output;

    /** Output name for error messages. */
    private final String outputName;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     */
    public SP3Writer(final Appendable output, final String outputName) {
        this.output        = output;
        this.outputName    = outputName;
    }

    /** Write a SP3 file.
     * @param sp3 SP3 file to write
     * @exception IOException if an I/O error occurs.
     */
    @DefaultDataContext
    public void write(final SP3 sp3)
        throws IOException {
        sp3.validate(false, outputName);
        writeHeader(sp3);
//        for (final ObservationDataSet observationDataSet : rinexObservation.getObservationDataSets()) {
//            writeObservationDataSet(observationDataSet);
//        }
    }

    /** Write header.
     * @param sp3 SP3 file containing the header to write
     * @exception IOException if an I/O error occurs.
     */
    private void writeHeader(final SP3 sp3)
        throws IOException {

    }

}
