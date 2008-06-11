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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.orekit.errors.OrekitException;
import org.orekit.iers.EarthOrientationParameters;
import org.orekit.iers.IERSDirectoryCrawler;

import junit.framework.TestCase;

public abstract class AbstractFilesLoaderTest extends TestCase {

    protected void setRoot(String directoryName) throws OrekitException {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, directoryName);
        eop = new TreeSet(new ChronologicalEOPComparator());
    }

    protected int getMaxGap() {
        int maxGap = 0;
        EarthOrientationParameters current = null;
        for (Iterator iterator = eop.iterator(); iterator.hasNext();) {
            EarthOrientationParameters previous = current;
            current = (EarthOrientationParameters) iterator.next();
            if (previous != null) {
                maxGap = Math.max(maxGap, current.getMjd() - previous.getMjd());
            }
        }
        return maxGap;
    }

    public void tearDown() {
        eop = null;
    }

    private static class ChronologicalEOPComparator
        implements Comparator, Serializable {
        /** Serializable UID. */
        private static final long serialVersionUID = -5473993886829759423L;

        /** {@inheritDoc} */
        public int compare(Object o1, Object o2) {
            EarthOrientationParameters eop1 = (EarthOrientationParameters) o1;
            EarthOrientationParameters eop2 = (EarthOrientationParameters) o2;
            return eop1.getMjd() - eop2.getMjd();
        }
    }

    protected TreeSet eop;

}
