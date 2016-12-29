/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.data;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class FundamentalNutationArgumentsTest {

    @Test
    public void testNoStream() {
        try {
            new FundamentalNutationArguments(IERSConventions.IERS_2010, TimeScalesFactory.getTT(), null, "dummy");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @Test
    public void testModifiedData() throws OrekitException, IOException {

        String directory = "/assets/org/orekit/IERS-conventions/";
        InputStream is = getClass().getResourceAsStream(directory + "2010/nutation-arguments.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            builder.append(line);
            builder.append('\n');
        }
        reader.close();
        try {
            // remove the line:
            // F5 ≡ Ω = 125.04455501◦ − 6962890.5431″t + 7.4722″t² + 0.007702″t³ − 0.00005939″t⁴
            String modified = builder.toString().replaceAll("F5[^\\n]+", "");
            new FundamentalNutationArguments(IERSConventions.IERS_2010, null,
                                             new ByteArrayInputStream(modified.getBytes()),
                                             "modified-data");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testEnum() throws NoSuchMethodException, SecurityException,
                                  IllegalAccessException, IllegalArgumentException,
                                  InvocationTargetException {
        Class<?> e = null;
        for (final Class<?> c : FundamentalNutationArguments.class.getDeclaredClasses()) {
            if (c.getName().endsWith("FundamentalName")) {
                e = c;
            }
        }
        Method m = e.getDeclaredMethod("valueOf", String.class);
        m.setAccessible(true);
        for (String n : Arrays.asList("L", "L_PRIME", "F", "D", "OMEGA",
                                      "L_ME", "L_VE", "L_E", "L_MA", "L_J", "L_SA", "L_U", "L_NE", "PA")) {
            Assert.assertEquals(n, m.invoke(null, n).toString());
        }
        try {
            m.invoke(null, "inexistent");
            Assert.fail("an exception should have been thrown");
        } catch (InvocationTargetException ite) {
            Assert.assertTrue(ite.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testSerializationNoTidalCorrection() throws OrekitException, IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        TimeScale ut1 = TimeScalesFactory.getUT1(conventions, true);
        checkSerialization(280000, 285000, conventions.getNutationArguments(ut1));
    }

    @Test
    public void testSerializationTidalCorrection() throws OrekitException, IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        TimeScale ut1 = TimeScalesFactory.getUT1(conventions, false);
        checkSerialization(280000, 285000, conventions.getNutationArguments(ut1));
    }

    @Test
    public void testSerializationNoUT1Correction() throws OrekitException, IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        checkSerialization(850, 950, conventions.getNutationArguments(null));
    }

    private void checkSerialization(int low, int high, FundamentalNutationArguments nutation)
        throws OrekitException, IOException, ClassNotFoundException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(nutation);

        Assert.assertTrue(bos.size() > low);
        Assert.assertTrue(bos.size() < high);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        FundamentalNutationArguments deserialized  = (FundamentalNutationArguments) ois.readObject();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            BodiesElements be1 = nutation.evaluateAll(date);
            BodiesElements be2 = deserialized.evaluateAll(date);
            Assert.assertEquals(be1.getGamma(),  be2.getGamma(),  1.0e-15);
            Assert.assertEquals(be1.getL(),      be2.getL(),      1.0e-15);
            Assert.assertEquals(be1.getLPrime(), be2.getLPrime(), 1.0e-15);
            Assert.assertEquals(be1.getF(),      be2.getF(),      1.0e-15);
            Assert.assertEquals(be1.getD(),      be2.getD(),      1.0e-15);
            Assert.assertEquals(be1.getOmega(),  be2.getOmega(),  1.0e-15);
            Assert.assertEquals(be1.getLMe(),    be2.getLMe(),    1.0e-15);
            Assert.assertEquals(be1.getLVe(),    be2.getLVe(),    1.0e-15);
            Assert.assertEquals(be1.getLE(),     be2.getLE(),     1.0e-15);
            Assert.assertEquals(be1.getLMa(),    be2.getLMa(),    1.0e-15);
            Assert.assertEquals(be1.getLJu(),    be2.getLJu(),    1.0e-15);
            Assert.assertEquals(be1.getLSa(),    be2.getLSa(),    1.0e-15);
            Assert.assertEquals(be1.getLUr(),    be2.getLUr(),    1.0e-15);
            Assert.assertEquals(be1.getLNe(),    be2.getLNe(),    1.0e-15);
            Assert.assertEquals(be1.getPa(),     be2.getPa(),     1.0e-15);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
