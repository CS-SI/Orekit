/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.data;

import java.util.TreeSet;


import org.junit.After;
import org.orekit.errors.OrekitException;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

public abstract class AbstractFilesLoaderTest {

    protected TreeSet<TimeStamped> set;

    protected void setRoot(String directoryName) throws OrekitException {
        String root = getClass().getClassLoader().getResource(directoryName).getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
        DataProvidersManager.getInstance().clearProviders();
        set = new TreeSet<TimeStamped>(new ChronologicalComparator());
    }

    protected int getMaxGap() {
        double maxGap = 0;
        TimeStamped previous = null;
        for (final TimeStamped current : set) {
            if (previous != null) {
                maxGap = Math.max(maxGap, current.getDate().durationFrom(previous.getDate()));
            }
            previous = current;
        }
        return (int) Math.round(maxGap / 86400.0);
    }

    @After
    public void tearDown() {
        set = null;
    }

}
