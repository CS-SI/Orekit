/* Contributed in the public domain.
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
package org.orekit.frames;

import org.orekit.frames.ITRFVersionLoader.ITRFVersionConfiguration;

/**
 * Interface for retrieving the ITRF version for a given set of EOP data.
 *
 * @author Luc Maisonobe
 * @author Evan Ward
 * @see ITRFVersionLoader
 * @since 10.1
 */
public interface ItrfVersionProvider {

    /**
     * Get the ITRF version configuration defined by a given file at specified date.
     *
     * @param name EOP file name
     * @param mjd  date of the EOP in modified Julian day
     * @return configuration valid around specified date in the file
     */
    ITRFVersionConfiguration getConfiguration(String name, int mjd);

}
