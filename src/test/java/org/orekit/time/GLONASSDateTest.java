/* Copyright 2002-2024 CS GROUP
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
package org.orekit.time;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class GLONASSDateTest {

    private TimeScale glo;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
        glo = TimeScalesFactory.getGLONASS();
    }

    @Test
    void testFromNaAndN4() {
        GLONASSDate date = new GLONASSDate(251, 5, 7200.0);
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                             new TimeComponents(2, 0, 0.0),
                                             glo);
        assertEquals(251, date.getDayNumber());
        assertEquals(5,   date.getIntervalNumber());
        assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        assertEquals(0,   date.getDate().durationFrom(ref), 1.0e-15);
    }

    @Test
    void testFromAbsoluteDate() {
        GLONASSDate date = new GLONASSDate(new AbsoluteDate(new DateComponents(2012, 9, 7),
                                                            new TimeComponents(2, 0, 0.0),
                                                            glo));
        assertEquals(251,    date.getDayNumber());
        assertEquals(5,      date.getIntervalNumber());
        assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        assertEquals(7200.0, date.getSecInDay(), 1.0e-15);
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        GLONASSDate date = new GLONASSDate(251, 5, 7200.0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(date);

        assertTrue(bos.size() > 95);
        assertTrue(bos.size() < 105);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        GLONASSDate deserialized  = (GLONASSDate) ois.readObject();
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2012, 9, 7),
                                             new TimeComponents(2, 0, 0),
                                             glo);
        assertEquals(251, deserialized.getDayNumber());
        assertEquals(5, deserialized.getIntervalNumber());
        assertEquals(2456177.5, date.getJD0(), 1.0e-16);
        assertEquals(29191.442830, date.getGMST(), 3.0e-3);
        assertEquals(0, deserialized.getDate().durationFrom(ref), 1.0e-15);

    }

}
