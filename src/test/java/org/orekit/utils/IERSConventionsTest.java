/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.utils;


import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


public class IERSConventionsTest {

    @Test
    public void testConventionsNumber() {
        Assert.assertEquals(3, IERSConventions.values().length);
    }

    @Test
    public void testIERS1996() {
        checkPrecessionSupported(IERSConventions.IERS_1996,        true);
        checkNutationSupported(IERSConventions.IERS_1996,          true);
        checkNonRotatingOriginSupported(IERSConventions.IERS_1996, true);
    }

    @Test
    public void testIERS2003() {
        checkPrecessionSupported(IERSConventions.IERS_2003,        true);
        checkNutationSupported(IERSConventions.IERS_2003,          false);
        checkNonRotatingOriginSupported(IERSConventions.IERS_2003, true);
    }

    @Test
    public void testIERS2010() {
        checkPrecessionSupported(IERSConventions.IERS_2010,        false);
        checkNutationSupported(IERSConventions.IERS_2010,          false);
        checkNonRotatingOriginSupported(IERSConventions.IERS_2010, true);
    }

    private void checkPrecessionSupported(IERSConventions conventions, boolean expected) {
        Assert.assertEquals(expected, conventions.precessionSupported());
        Assert.assertEquals(expected, checkMethod(conventions, Method.PRECESSION_ZETA));
        Assert.assertEquals(expected, checkMethod(conventions, Method.PRECESSION_THETA));
        Assert.assertEquals(expected, checkMethod(conventions, Method.PRECESSION_Z));
    }

    private void checkNutationSupported(IERSConventions conventions, boolean expected) {
        Assert.assertEquals(expected, conventions.nutationSupported());
        Assert.assertEquals(expected, checkMethod(conventions, Method.NUTATION_LONGITUDE));
        Assert.assertEquals(expected, checkMethod(conventions, Method.NUTATION_OBLIQUITY));
        Assert.assertEquals(expected, checkMethod(conventions, Method.EQUINOXE_CORRECTION));
        Assert.assertEquals(expected, checkMethod(conventions, Method.MEAN_OBLIQUITY));
    }

    private void checkNonRotatingOriginSupported(IERSConventions conventions, boolean expected) {
        Assert.assertEquals(expected, conventions.nonRotatingOriginSupported());
        Assert.assertEquals(expected, checkMethod(conventions, Method.CIP_X));
        Assert.assertEquals(expected, checkMethod(conventions, Method.CIP_Y));
        Assert.assertEquals(expected, checkMethod(conventions, Method.CIO_S));
    }

    public boolean checkMethod(IERSConventions conventions, Method method) {
        try {
            return method.invoke(conventions) != null;
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_PARAMETER, oe.getSpecifier());
        }
        return false;
    }

    private static enum Method {
        NUTATION_LONGITUDE() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getNutationInLongitudeFunction();
            }
        },
        NUTATION_OBLIQUITY() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getNutationInObliquityFunction();
            }
        },
        PRECESSION_ZETA() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getPrecessionZetaFunction();
            }
        },
        PRECESSION_THETA() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getPrecessionThetaFunction();
            }
        },
        PRECESSION_Z() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getPrecessionZFunction();
            }
        },
        EQUINOXE_CORRECTION() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getEquationOfEquinoxesCorrectionFunction();
            }
        },
        MEAN_OBLIQUITY() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getMeanObliquityOfEclipticFunction();
            }
        },
        CIP_X() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getXFunction();
            }
        },
        CIP_Y() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getYFunction();
            }
        },
        CIO_S() {
            public Object invoke(IERSConventions conventions) throws OrekitException {
                return conventions.getSXY2XFunction();
            }
        };

        public abstract Object invoke(IERSConventions conventions) throws OrekitException;

    }

}
