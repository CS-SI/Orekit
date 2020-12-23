/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

/**
 * The ADMFile (Attitude Data Message) class represents any of the two attitude
 * messages used by the CCSDS, (i.e. the Attitude Parameter Message (APM),
 * and the Attitude Ephemeris Message (AEM). It contains the information of the message's
 * header and configuration data (set in the parser).
 * <p>
 * This class has no specific methods. It is used, in particular, to distinguish
 * between the Orbit Data Message and the Attitude Data Message.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.2
 */
public abstract class ADMFile extends NDMFile {

    /**
     * Constructor.
     */
    public ADMFile() {
        // Do nothing
    }

}
