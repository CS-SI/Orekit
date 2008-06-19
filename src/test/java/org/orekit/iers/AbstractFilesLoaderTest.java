/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

public abstract class AbstractFilesLoaderTest extends TestCase {

    protected TreeSet<EarthOrientationParameters> eop;

    protected void setRoot(String directoryName) throws OrekitException {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, directoryName);
        eop = new TreeSet<EarthOrientationParameters>();
    }

    protected int getMaxGap() {
        int maxGap = 0;
        EarthOrientationParameters previous = null;
        for (final EarthOrientationParameters current : eop) {
            if (previous != null) {
                maxGap = Math.max(maxGap, current.getMjd() - previous.getMjd());
            }
            previous = current;
        }
        return maxGap;
    }

    public void tearDown() {
        eop = null;
    }

}
