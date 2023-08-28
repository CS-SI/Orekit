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
package org.orekit.gnss;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Observation time scales.
 * @since 12.0
 */
public enum ObservationTimeScale {

    /** GPS time scale. */
    GPS {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getGPS();
        }
    },

    /** Galileo time scale. */
    GAL {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getGST();
        }
    },

    /** GLONASS time scale. */
    GLO {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getGLONASS();
        }
    },

    /** QZSS time scale. */
    QZS {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getQZSS();
        }
    },

    /** Beidou time scale. */
    BDT {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getBDT();
        }
    },

    /** IRNSS time scale. */
    IRN {
        public TimeScale getTimeScale(final TimeScales timeScales) {
            return timeScales.getIRNSS();
        }
    };

    /** Get time scale.
     * @param timeScales time scales factory
     * @return time scale
     */
    public abstract TimeScale getTimeScale(TimeScales timeScales);

}
