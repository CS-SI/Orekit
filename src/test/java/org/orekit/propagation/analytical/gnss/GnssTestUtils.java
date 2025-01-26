/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GnssTestUtils {

    @SuppressWarnings("unchecked")
    public static <O extends GNSSOrbitalElements<O>>
    void checkFieldConversion(final O message) {
        try {
            // looping over several types to check conversion functions
            FieldGnssOrbitalElements<Binary64, ?> intermediate1 =
                message.toField(Binary64Field.getInstance());
            FieldGnssOrbitalElements<? extends FieldGradient<?>, ?> intermediate2 =
                intermediate1.changeField(t -> FieldGradient.constant(6, t));
            final O rebuilt = (O) intermediate2.toNonField();

            for (final Method getter : getGetters(message, Integer.TYPE)) {
                final Method fieldGetter = intermediate2.getClass().getMethod(getter.getName());
                Assertions.assertEquals(getter.invoke(message), fieldGetter.invoke(intermediate2));
                Assertions.assertEquals(getter.invoke(message), getter.invoke(rebuilt));
            }
            for (final Method getter : getGetters(message, Double.TYPE)) {
                final Method fieldGetter = intermediate2.getClass().getMethod(getter.getName());
                final double f = fieldGetter.getReturnType().equals(Double.TYPE) ?
                                 (Double) fieldGetter.invoke(intermediate2) :
                                 ((CalculusFieldElement<?>) fieldGetter.invoke(intermediate2)).getReal();
                Assertions.assertEquals((Double) getter.invoke(message), f, 1.0e-15);
                Assertions.assertEquals((Double) getter.invoke(message), (Double) getter.invoke(rebuilt), 1.0e-15, message.getClass().getName() + "." +getter.getName());
            }
            for (final Method getter : getGetters(message, AbsoluteDate.class)) {
                final Method               fieldGetter = intermediate2.getClass().getMethod(getter.getName());
                final AbsoluteDate         date        = (AbsoluteDate) getter.invoke(message);
                final FieldAbsoluteDate<?> fieldDate   = (FieldAbsoluteDate<?>) fieldGetter.invoke(intermediate2);
                final AbsoluteDate         rebuiltDate = (AbsoluteDate) getter.invoke(rebuilt);
                Assertions.assertEquals(0.0, date.durationFrom(fieldDate.toAbsoluteDate()), 1.0e-15);
                Assertions.assertEquals(0.0, date.durationFrom(rebuiltDate),                1.0e-15);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException nsme) {
            Assertions.fail(nsme.getLocalizedMessage());
        }
    }

    private static List<Method> getGetters(final Object o, final Class<?> returnType) {
        final List<Method> getters = new ArrayList<>();
        for (Class<?> cls = o.getClass();
             cls.getName().startsWith("org.orekit") ;
             cls = cls.getSuperclass()) {
            for (final Method method : cls.getDeclaredMethods()) {
                if (method.getName().startsWith("get") && returnType.equals(method.getReturnType())) {
                    getters.add(method);
                }
            }
        }
        return getters;
    }

}
