/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

public class GLONASSDateTest {

    private TimeScale glo;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        glo = TimeScalesFactory.getGLONASS();
    }

    @Test
    public void testFromNaAndN4() {
        GLONASSDate date = new GLONASSDate(251, 5, 7200.0);
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                             new TimeComponents(2, 0, 0.0),
                                             glo);
        Assert.assertEquals(251, date.getDayNumber());
        Assert.assertEquals(5,   date.getIntervalNumber());
        Assert.assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        Assert.assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        Assert.assertEquals(0,   date.getDate().durationFrom(ref), 1.0e-15);
    }

    @Test
    public void testFromAbsoluteDate() {
        GLONASSDate date = new GLONASSDate(new AbsoluteDate(new DateComponents(2012, 9, 7),
                                                            new TimeComponents(2, 0, 0.0),
                                                            glo));
        Assert.assertEquals(251,    date.getDayNumber());
        Assert.assertEquals(5,      date.getIntervalNumber());
        Assert.assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        Assert.assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        Assert.assertEquals(7200.0, date.getSecInDay(), 1.0e-15);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        GLONASSDate date = new GLONASSDate(251, 5, 7200.0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(date);

        Assert.assertTrue(bos.size() > 95);
        Assert.assertTrue(bos.size() < 105);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        GLONASSDate deserialized  = (GLONASSDate) ois.readObject();
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                             new TimeComponents(2, 0, 0),
                                             glo);
        Assert.assertEquals(251, deserialized.getDayNumber());
        Assert.assertEquals(5, deserialized.getIntervalNumber());
        Assert.assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        Assert.assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        Assert.assertEquals(0, deserialized.getDate().durationFrom(ref), 1.0e-15);

    }

}
