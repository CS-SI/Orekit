/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.time;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;

public class BeidouDateTest {

    @Test
    public void testFromWeekAndMilli() {
        BeidouDate date = new BeidouDate(216, 515713000.0);
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2010, 2, 26),
                                             new TimeComponents(23, 15, 12),
                                             utc);
        Assert.assertEquals(216, date.getWeekNumber());
        Assert.assertEquals(515713000.0, date.getMilliInWeek(), 1.0e-15);
        Assert.assertEquals(0, date.getDate().durationFrom(ref), 1.0e-15);
    }

    @Test
    public void testFromAbsoluteDate() {
        BeidouDate date = new BeidouDate(new AbsoluteDate(new DateComponents(2010, 2, 26),
                                                          new TimeComponents(23, 15, 12),
                                                          utc));
        Assert.assertEquals(216, date.getWeekNumber());
        Assert.assertEquals(515713000.0, date.getMilliInWeek(), 1.0e-15);
    }

    @Test
    public void testZero() {
        BeidouDate date = new BeidouDate(AbsoluteDate.BEIDOU_EPOCH);
        Assert.assertEquals(0, date.getWeekNumber());
        Assert.assertEquals(0.0, date.getMilliInWeek(), 1.0e-15);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        BeidouDate date = new BeidouDate(216, 515713000.0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(date);

        Assert.assertTrue(bos.size() > 95);
        Assert.assertTrue(bos.size() < 106);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        BeidouDate deserialized  = (BeidouDate) ois.readObject();
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2010, 2, 26),
                                             new TimeComponents(23, 15, 12),
                                             utc);
        Assert.assertEquals(216, deserialized.getWeekNumber());
        Assert.assertEquals(515713000.0, deserialized.getMilliInWeek(), 1.0e-15);
        Assert.assertEquals(0, deserialized.getDate().durationFrom(ref), 1.0e-15);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    private TimeScale utc;

}
