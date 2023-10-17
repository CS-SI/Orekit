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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

public class FundamentalNutationArgumentsTest {

    @Test
    public void testNoStream() {
        try {
            new FundamentalNutationArguments(IERSConventions.IERS_2010, TimeScalesFactory.getTT(), null, "dummy");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals("dummy", oe.getParts()[0]);
        }
    }

    @Test
    public void testModifiedData() throws IOException {

        String directory = "/assets/org/orekit/IERS-conventions/";
        InputStream is = getClass().getResourceAsStream(directory + "2010/nutation-arguments.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
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
            Assertions.assertEquals(n, m.invoke(null, n).toString());
        }
        try {
            m.invoke(null, "inexistent");
            Assertions.fail("an exception should have been thrown");
        } catch (InvocationTargetException ite) {
            Assertions.assertTrue(ite.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testDotDouble() {
        final IERSConventions conventions = IERSConventions.IERS_2010;
        final TimeScale ut1 = TimeScalesFactory.getUT1(conventions, false);
        final FundamentalNutationArguments fna = conventions.getNutationArguments(ut1);
        final AbsoluteDate t0 = new AbsoluteDate(2002, 4, 7, 12, 34, 22.5, TimeScalesFactory.getUTC());
        final UnivariateDifferentiableFunction gamma  = differentiate(fna, t0, b -> b.getGamma());
        final UnivariateDifferentiableFunction l      = differentiate(fna, t0, b -> b.getL());
        final UnivariateDifferentiableFunction lPrime = differentiate(fna, t0, b -> b.getLPrime());
        final UnivariateDifferentiableFunction f      = differentiate(fna, t0, b -> b.getF());
        final UnivariateDifferentiableFunction d      = differentiate(fna, t0, b -> b.getD());
        final UnivariateDifferentiableFunction lMe    = differentiate(fna, t0, b -> b.getLMe());
        final UnivariateDifferentiableFunction lVe    = differentiate(fna, t0, b -> b.getLVe());
        final UnivariateDifferentiableFunction lE     = differentiate(fna, t0, b -> b.getLE());
        final UnivariateDifferentiableFunction lMa    = differentiate(fna, t0, b -> b.getLMa());
        final UnivariateDifferentiableFunction lJu    = differentiate(fna, t0, b -> b.getLJu());
        final UnivariateDifferentiableFunction lSa    = differentiate(fna, t0, b -> b.getLSa());
        final UnivariateDifferentiableFunction lUr    = differentiate(fna, t0, b -> b.getLUr());
        final UnivariateDifferentiableFunction lNe    = differentiate(fna, t0, b -> b.getLNe());
        final UnivariateDifferentiableFunction  pa    = differentiate(fna, t0, b -> b.getPa());
        final DSFactory factory = new DSFactory(1, 1);
        double maxErrorGamma  = 0;
        double maxErrorL      = 0;
        double maxErrorLPrime = 0;
        double maxErrorF      = 0;
        double maxErrorD      = 0;
        double maxErrorLMe    = 0;
        double maxErrorLVe    = 0;
        double maxErrorLE     = 0;
        double maxErrorLMa    = 0;
        double maxErrorLJu    = 0;
        double maxErrorLSa    = 0;
        double maxErrorLUr    = 0;
        double maxErrorLNe    = 0;
        double maxErrorPa     = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            BodiesElements be = fna.evaluateAll(t0.shiftedBy(dt));
            DerivativeStructure dtDS = factory.variable(0, dt);
            maxErrorGamma  = FastMath.max(maxErrorGamma,  FastMath.abs(gamma .value(dtDS).getPartialDerivative(1) - be.getGammaDot()));
            maxErrorL      = FastMath.max(maxErrorL,      FastMath.abs(l     .value(dtDS).getPartialDerivative(1) - be.getLDot()));
            maxErrorLPrime = FastMath.max(maxErrorLPrime, FastMath.abs(lPrime.value(dtDS).getPartialDerivative(1) - be.getLPrimeDot()));
            maxErrorF      = FastMath.max(maxErrorF,      FastMath.abs(f     .value(dtDS).getPartialDerivative(1) - be.getFDot()));
            maxErrorD      = FastMath.max(maxErrorD,      FastMath.abs(d     .value(dtDS).getPartialDerivative(1) - be.getDDot()));
            maxErrorLMe    = FastMath.max(maxErrorLMe,    FastMath.abs(lMe   .value(dtDS).getPartialDerivative(1) - be.getLMeDot()));
            maxErrorLVe    = FastMath.max(maxErrorLVe,    FastMath.abs(lVe   .value(dtDS).getPartialDerivative(1) - be.getLVeDot()));
            maxErrorLE     = FastMath.max(maxErrorLE,     FastMath.abs(lE    .value(dtDS).getPartialDerivative(1) - be.getLEDot()));
            maxErrorLMa    = FastMath.max(maxErrorLMa,    FastMath.abs(lMa   .value(dtDS).getPartialDerivative(1) - be.getLMaDot()));
            maxErrorLJu    = FastMath.max(maxErrorLJu,    FastMath.abs(lJu   .value(dtDS).getPartialDerivative(1) - be.getLJuDot()));
            maxErrorLSa    = FastMath.max(maxErrorLSa,    FastMath.abs(lSa   .value(dtDS).getPartialDerivative(1) - be.getLSaDot()));
            maxErrorLUr    = FastMath.max(maxErrorLUr,    FastMath.abs(lUr   .value(dtDS).getPartialDerivative(1) - be.getLUrDot()));
            maxErrorLNe    = FastMath.max(maxErrorLNe,    FastMath.abs(lNe   .value(dtDS).getPartialDerivative(1) - be.getLNeDot()));
            maxErrorPa     = FastMath.max(maxErrorPa,     FastMath.abs(pa    .value(dtDS).getPartialDerivative(1) - be.getPaDot()));
        }
        Assertions.assertEquals(0, maxErrorGamma,  8.0e-13);
        Assertions.assertEquals(0, maxErrorL,      1.0e-14);
        Assertions.assertEquals(0, maxErrorLPrime, 6.0e-16);
        Assertions.assertEquals(0, maxErrorF,      6.0e-15);
        Assertions.assertEquals(0, maxErrorD,      6.0e-15);
        Assertions.assertEquals(0, maxErrorLMe,    2.0e-15);
        Assertions.assertEquals(0, maxErrorLVe,    5.0e-16);
        Assertions.assertEquals(0, maxErrorLE,     3.0e-16);
        Assertions.assertEquals(0, maxErrorLMa,    4.0e-16);
        Assertions.assertEquals(0, maxErrorLJu,    3.0e-17);
        Assertions.assertEquals(0, maxErrorLSa,    4.0e-17);
        Assertions.assertEquals(0, maxErrorLUr,    1.0e-16);
        Assertions.assertEquals(0, maxErrorLNe,    8.0e-17);
        Assertions.assertEquals(0, maxErrorPa,     3.0e-20);
    }

    private UnivariateDifferentiableFunction differentiate(final FundamentalNutationArguments fna, final AbsoluteDate t0,
                                                           final Function<BodiesElements, Double> f) {
        return new FiniteDifferencesDifferentiator(8, 10.0).differentiate(new UnivariateFunction() {
            double angle = 0;
            @Override
            public double value(double t) {
                double raw = f.apply(fna.evaluateAll(t0.shiftedBy(t)));
                angle = MathUtils.normalizeAngle(raw, angle);
                return angle;
            }
        });
    }

    @Test
    public void testDotField() {
        final IERSConventions conventions = IERSConventions.IERS_2010;
        final TimeScale ut1 = TimeScalesFactory.getUT1(conventions, false);
        final FundamentalNutationArguments fna = conventions.getNutationArguments(ut1);
        final FieldAbsoluteDate<Binary64> t0 = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                        2002, 4, 7, 12, 34, 22.5, TimeScalesFactory.getUTC());
        final UnivariateDifferentiableFunction gamma  = differentiate(fna, t0, b -> b.getGamma());
        final UnivariateDifferentiableFunction l      = differentiate(fna, t0, b -> b.getL());
        final UnivariateDifferentiableFunction lPrime = differentiate(fna, t0, b -> b.getLPrime());
        final UnivariateDifferentiableFunction f      = differentiate(fna, t0, b -> b.getF());
        final UnivariateDifferentiableFunction d      = differentiate(fna, t0, b -> b.getD());
        final UnivariateDifferentiableFunction lMe    = differentiate(fna, t0, b -> b.getLMe());
        final UnivariateDifferentiableFunction lVe    = differentiate(fna, t0, b -> b.getLVe());
        final UnivariateDifferentiableFunction lE     = differentiate(fna, t0, b -> b.getLE());
        final UnivariateDifferentiableFunction lMa    = differentiate(fna, t0, b -> b.getLMa());
        final UnivariateDifferentiableFunction lJu    = differentiate(fna, t0, b -> b.getLJu());
        final UnivariateDifferentiableFunction lSa    = differentiate(fna, t0, b -> b.getLSa());
        final UnivariateDifferentiableFunction lUr    = differentiate(fna, t0, b -> b.getLUr());
        final UnivariateDifferentiableFunction lNe    = differentiate(fna, t0, b -> b.getLNe());
        final UnivariateDifferentiableFunction  pa    = differentiate(fna, t0, b -> b.getPa());
        final DSFactory factory = new DSFactory(1, 1);
        double maxErrorGamma  = 0;
        double maxErrorL      = 0;
        double maxErrorLPrime = 0;
        double maxErrorF      = 0;
        double maxErrorD      = 0;
        double maxErrorLMe    = 0;
        double maxErrorLVe    = 0;
        double maxErrorLE     = 0;
        double maxErrorLMa    = 0;
        double maxErrorLJu    = 0;
        double maxErrorLSa    = 0;
        double maxErrorLUr    = 0;
        double maxErrorLNe    = 0;
        double maxErrorPa     = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            FieldBodiesElements<Binary64> be = fna.evaluateAll(t0.shiftedBy(dt));
            DerivativeStructure dtDS = factory.variable(0, dt);
            maxErrorGamma  = FastMath.max(maxErrorGamma,  FastMath.abs(gamma .value(dtDS).getPartialDerivative(1) - be.getGammaDot().getReal()));
            maxErrorL      = FastMath.max(maxErrorL,      FastMath.abs(l     .value(dtDS).getPartialDerivative(1) - be.getLDot().getReal()));
            maxErrorLPrime = FastMath.max(maxErrorLPrime, FastMath.abs(lPrime.value(dtDS).getPartialDerivative(1) - be.getLPrimeDot().getReal()));
            maxErrorF      = FastMath.max(maxErrorF,      FastMath.abs(f     .value(dtDS).getPartialDerivative(1) - be.getFDot().getReal()));
            maxErrorD      = FastMath.max(maxErrorD,      FastMath.abs(d     .value(dtDS).getPartialDerivative(1) - be.getDDot().getReal()));
            maxErrorLMe    = FastMath.max(maxErrorLMe,    FastMath.abs(lMe   .value(dtDS).getPartialDerivative(1) - be.getLMeDot().getReal()));
            maxErrorLVe    = FastMath.max(maxErrorLVe,    FastMath.abs(lVe   .value(dtDS).getPartialDerivative(1) - be.getLVeDot().getReal()));
            maxErrorLE     = FastMath.max(maxErrorLE,     FastMath.abs(lE    .value(dtDS).getPartialDerivative(1) - be.getLEDot().getReal()));
            maxErrorLMa    = FastMath.max(maxErrorLMa,    FastMath.abs(lMa   .value(dtDS).getPartialDerivative(1) - be.getLMaDot().getReal()));
            maxErrorLJu    = FastMath.max(maxErrorLJu,    FastMath.abs(lJu   .value(dtDS).getPartialDerivative(1) - be.getLJuDot().getReal()));
            maxErrorLSa    = FastMath.max(maxErrorLSa,    FastMath.abs(lSa   .value(dtDS).getPartialDerivative(1) - be.getLSaDot().getReal()));
            maxErrorLUr    = FastMath.max(maxErrorLUr,    FastMath.abs(lUr   .value(dtDS).getPartialDerivative(1) - be.getLUrDot().getReal()));
            maxErrorLNe    = FastMath.max(maxErrorLNe,    FastMath.abs(lNe   .value(dtDS).getPartialDerivative(1) - be.getLNeDot().getReal()));
            maxErrorPa     = FastMath.max(maxErrorPa,     FastMath.abs(pa    .value(dtDS).getPartialDerivative(1) - be.getPaDot().getReal()));
        }
        Assertions.assertEquals(0, maxErrorGamma,  8.0e-13);
        Assertions.assertEquals(0, maxErrorL,      1.0e-14);
        Assertions.assertEquals(0, maxErrorLPrime, 6.0e-16);
        Assertions.assertEquals(0, maxErrorF,      6.0e-15);
        Assertions.assertEquals(0, maxErrorD,      6.0e-15);
        Assertions.assertEquals(0, maxErrorLMe,    2.0e-15);
        Assertions.assertEquals(0, maxErrorLVe,    5.0e-16);
        Assertions.assertEquals(0, maxErrorLE,     3.0e-16);
        Assertions.assertEquals(0, maxErrorLMa,    4.0e-16);
        Assertions.assertEquals(0, maxErrorLJu,    3.0e-17);
        Assertions.assertEquals(0, maxErrorLSa,    4.0e-17);
        Assertions.assertEquals(0, maxErrorLUr,    1.0e-16);
        Assertions.assertEquals(0, maxErrorLNe,    8.0e-17);
        Assertions.assertEquals(0, maxErrorPa,     3.0e-20);
    }

    private <T extends CalculusFieldElement<T>> UnivariateDifferentiableFunction differentiate(final FundamentalNutationArguments fna, final FieldAbsoluteDate<T> t0,
                                                                                           final Function<FieldBodiesElements<T>, T> f) {
        return new FiniteDifferencesDifferentiator(8, 10.0).differentiate(new UnivariateFunction() {
            double angle = 0;
            @Override
            public double value(double t) {
                double raw = f.apply(fna.evaluateAll(t0.shiftedBy(t))).getReal();
                angle = MathUtils.normalizeAngle(raw, angle);
                return angle;
            }
        });
    }

    @Test
    public void testSerializationNoTidalCorrection() throws IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        TimeScale ut1 = TimeScalesFactory.getUT1(conventions, true);
        checkSerialization(340000, 350000, conventions.getNutationArguments(ut1));
    }

    @Test
    public void testSerializationTidalCorrection() throws IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        TimeScale ut1 = TimeScalesFactory.getUT1(conventions, false);
        checkSerialization(340000, 350000, conventions.getNutationArguments(ut1));
    }

    @Test
    public void testSerializationNoUT1Correction() throws IOException, ClassNotFoundException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        checkSerialization(850, 950, conventions.getNutationArguments(null));
    }

    private void checkSerialization(int low, int high, FundamentalNutationArguments nutation)
        throws IOException, ClassNotFoundException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(nutation);

        Assertions.assertTrue(bos.size() > low);
        Assertions.assertTrue(bos.size() < high);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        FundamentalNutationArguments deserialized  = (FundamentalNutationArguments) ois.readObject();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            BodiesElements be1 = nutation.evaluateAll(date);
            BodiesElements be2 = deserialized.evaluateAll(date);
            Assertions.assertEquals(be1.getGamma(),  be2.getGamma(),  1.0e-15);
            Assertions.assertEquals(be1.getL(),      be2.getL(),      1.0e-15);
            Assertions.assertEquals(be1.getLPrime(), be2.getLPrime(), 1.0e-15);
            Assertions.assertEquals(be1.getF(),      be2.getF(),      1.0e-15);
            Assertions.assertEquals(be1.getD(),      be2.getD(),      1.0e-15);
            Assertions.assertEquals(be1.getOmega(),  be2.getOmega(),  1.0e-15);
            Assertions.assertEquals(be1.getLMe(),    be2.getLMe(),    1.0e-15);
            Assertions.assertEquals(be1.getLVe(),    be2.getLVe(),    1.0e-15);
            Assertions.assertEquals(be1.getLE(),     be2.getLE(),     1.0e-15);
            Assertions.assertEquals(be1.getLMa(),    be2.getLMa(),    1.0e-15);
            Assertions.assertEquals(be1.getLJu(),    be2.getLJu(),    1.0e-15);
            Assertions.assertEquals(be1.getLSa(),    be2.getLSa(),    1.0e-15);
            Assertions.assertEquals(be1.getLUr(),    be2.getLUr(),    1.0e-15);
            Assertions.assertEquals(be1.getLNe(),    be2.getLNe(),    1.0e-15);
            Assertions.assertEquals(be1.getPa(),     be2.getPa(),     1.0e-15);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
