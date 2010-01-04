/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;

/** Interface for loading Earth Orientation Parameters 1980 history.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface EOP1980HistoryLoader extends DataLoader {

    /** Load celestial body.
     * @param history history to fill up
     * @throws OrekitException if the history cannot be loaded
     */
    void fillHistory(EOP1980History history) throws OrekitException;

}
