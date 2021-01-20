/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

/** Container for local spacecraft bofy frames.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class LocalSpacecraftBodyFrame {

    /** Frame type. */
    public enum Type {

        /** Actuator reference frame: could denote reaction wheels, solar arrays, thrusters, etc.. */
        ACTUATOR,

        /** Coarse Sun Sensor. */
        CSS,

        /** Digital Sun Sensor. */
        DSS,

        /** Gyroscope Reference Frame. */
        GYRO,

        /** Instrument reference frame. */
        INSTRUMENT,

        /** Spacecraft Body Frame. */
        SC_BODY,

        /** Sensor reference frame. */
        SENSOR,

        /** Star Tracker Reference. */
        STARTRACKER,

        /** Three Axis Magnetometer Reference Frame. */
        TAM;

    }

    /** Frame type. */
    private final Type type;

    /** Frame label. */
    private final String label;

    /** Simple constructor.
     * @param type frame type
     * @param label frame label
     */
    public LocalSpacecraftBodyFrame(final Type type, final String label) {
        this.type = type;
        this.label = label;
    }

    /** Get the frame type.
     * @return frame type
     */
    public Type getType() {
        return type;
    }

    /** get the frame label.
     * @return frame label
     */
    public String getLabel() {
        return label;
    }

}
