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
package org.orekit.frames;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.stat.descriptive.rank.Percentile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.utils.IERSConventions;


public class EopCsvFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testEopc04Rates() {
        EOPHistory history = load("eopc04_20.2022-now.csv");
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        Assertions.assertEquals(new AbsoluteDate(2022, 1, 1, utc), history.getStartDate());
        Assertions.assertEquals(new AbsoluteDate(2023, 8, 28, utc), history.getEndDate());
        checkRatesConsistency(history, 0.049, 0.072, 0.063, 0.046);
    }

    @Test
    public void testBulletinAWithoutRates() {
        EOPHistory history = load("bulletina-xxxvi-037.csv");
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        Assertions.assertEquals(new AbsoluteDate(2023, 8, 24, utc), history.getStartDate());
        Assertions.assertEquals(new AbsoluteDate(2024, 9, 13, utc), history.getEndDate());
    }

    @Test
    public void testBulletinAWithRatesOnlyInHeader() {
        EOPHistory history = load("bulletina-xxxvi-038.csv");
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        Assertions.assertEquals(new AbsoluteDate(2023, 8, 30, utc), history.getStartDate());
        Assertions.assertEquals(new AbsoluteDate(2024, 9, 20, utc), history.getEndDate());
    }

    @Test
    public void testBulletinBWithoutRates() {
        EOPHistory history = load("bulletinb-423.csv");
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        Assertions.assertEquals(new AbsoluteDate(2023, 3, 2, utc), history.getStartDate());
        Assertions.assertEquals(new AbsoluteDate(2023, 5, 1, utc), history.getEndDate());
    }

    @Test
    public void testBulletinBWithRatesOnlyInHeader() {
        EOPHistory history = load("bulletinb-427.csv");
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        Assertions.assertEquals(new AbsoluteDate(2023, 7, 2, utc), history.getStartDate());
        Assertions.assertEquals(new AbsoluteDate(2023, 9, 1, utc), history.getEndDate());
    }

    private EOPHistory load(final String name) {
        IERSConventions.NutationCorrectionConverter converter =
                        IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopCsvFilesLoader("^" + name + "$", manager, () -> utc).
        fillHistory(converter, data);
        return new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE, data, true);
    }

    private final void checkRatesConsistency(final EOPHistory history,
                                             final double tol10X, final double tol90X,
                                             final double tol10Y, final double tol90Y) {
        
        final List<EOPEntry> entries = history.getEntries();
        double[] sampleX = new double[entries.size() - 2];
        double[] sampleY = new double[entries.size() - 2];
        for (int i = 1; i < entries.size() - 1; ++i) {
            final EOPEntry previous = entries.get(i - 1);
            final EOPEntry current  = entries.get(i);
            final EOPEntry next     = entries.get(i + 1);
            final double xRate = (next.getX() - previous.getX()) / next.durationFrom(previous);
            final double yRate = (next.getY() - previous.getY()) / next.durationFrom(previous);
            sampleX[i - 1] = (xRate - current.getXRate()) / xRate;
            sampleY[i - 1] = (yRate - current.getYRate()) / yRate;
        }

        Assertions.assertEquals(0.0, new Percentile(10.0).evaluate(sampleX), tol10X);
        Assertions.assertEquals(0.0, new Percentile(90.0).evaluate(sampleX), tol90X);
        Assertions.assertEquals(0.0, new Percentile(10.0).evaluate(sampleY), tol10Y);
        Assertions.assertEquals(0.0, new Percentile(90.0).evaluate(sampleY), tol90Y);

    }

    @BeforeEach
    public void setUp() {
        setRoot("eop-csv");
    }

}
