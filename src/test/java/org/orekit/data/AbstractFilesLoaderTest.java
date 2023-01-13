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
package org.orekit.data;

import org.hipparchus.util.FastMath;
import org.orekit.Utils;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;

import java.util.SortedSet;

public abstract class AbstractFilesLoaderTest {

    protected DataProvidersManager manager =
            DataContext.getDefault().getDataProvidersManager();

    protected TimeScale utc;

    protected void setRoot(String directoryName) {
        Utils.setDataRoot(directoryName);
        manager = DataContext.getDefault().getDataProvidersManager();
        utc = DataContext.getDefault().getTimeScales().getUTC();
    }

    protected int getMaxGap(SortedSet<? extends TimeStamped> history) {
        double maxGap = 0;
        TimeStamped previous = null;
        for (final TimeStamped current : history) {
            if (previous != null) {
                maxGap = FastMath.max(maxGap, current.getDate().durationFrom(previous.getDate()));
            }
            previous = current;
        }
        return (int) FastMath.round(maxGap / Constants.JULIAN_DAY);
    }

}
