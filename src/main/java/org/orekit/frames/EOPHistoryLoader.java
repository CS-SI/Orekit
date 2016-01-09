/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.frames;

import java.util.SortedSet;

import org.orekit.errors.OrekitException;
import org.orekit.utils.IERSConventions;

/** Interface for loading Earth Orientation Parameters history.
 * @author Luc Maisonobe
 * @since 6.1
 */
public interface EOPHistoryLoader {

    /** Load celestial body.
     * @param converter converter to use for nutation corrections
     * @param history history to fill up
     * @throws OrekitException if the history cannot be loaded
     */
    void fillHistory(IERSConventions.NutationCorrectionConverter converter,
                     SortedSet<EOPEntry> history)
        throws OrekitException;

}
