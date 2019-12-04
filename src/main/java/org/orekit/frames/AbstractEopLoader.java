/* Contributed in the public domain.
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

import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.time.TimeScale;

/**
 * Base class for EOP loaders.
 *
 * @author Evan Ward
 */
public class AbstractEopLoader extends AbstractSelfFeedingLoader {


    /** UTC time scale used for parsing files. */
    private final TimeScale utc;

    /**
     * Simple constructor.
     *
     * @param supportedNames regular expression for supported files names.
     * @param manager        provides access to the EOP files.
     * @param utc            UTC time scale.
     */
    public AbstractEopLoader(final String supportedNames,
                             final DataProvidersManager manager,
                             final TimeScale utc) {
        super(supportedNames, manager);
        this.utc = utc;
    }

    /**
     * Get the UTC time scale.
     *
     * @return UTC time scale.
     */
    protected TimeScale getUtc() {
        return utc;
    }

}
