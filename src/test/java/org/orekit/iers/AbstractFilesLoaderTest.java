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
package org.orekit.iers;

import java.util.TreeSet;

import junit.framework.TestCase;

import org.orekit.errors.OrekitException;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

public abstract class AbstractFilesLoaderTest extends TestCase {

    protected TreeSet<TimeStamped> eop;

    protected void setRoot(String directoryName) throws OrekitException {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, directoryName);
        eop = new TreeSet<TimeStamped>(ChronologicalComparator.getInstance());
    }

    protected int getMaxGap() {
        double maxGap = 0;
        TimeStamped previous = null;
        for (final TimeStamped current : eop) {
            if (previous != null) {
                maxGap = Math.max(maxGap, current.getDate().minus(previous.getDate()));
            }
            previous = current;
        }
        return (int) Math.round(maxGap / 86400.0);
    }

    public void tearDown() {
        eop = null;
    }

}
