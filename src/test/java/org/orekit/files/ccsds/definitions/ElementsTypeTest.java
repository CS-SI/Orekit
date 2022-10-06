/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.units.Unit;

import java.util.Arrays;
import java.util.Collections;


public class ElementsTypeTest {

    @Test
    public void testNotSupported() {
        for (final ElementsType et : Arrays.asList(ElementsType.ADBARV,
                                                   ElementsType.DELAUNAY, ElementsType.DELAUNAYMOD,
                                                   ElementsType.EIGVAL3EIGVEC3, ElementsType.GEODETIC,
                                                   ElementsType.LDBARV, ElementsType.ONSTATION,
                                                   ElementsType.POINCARE)) {
            try {
                et.toCartesian(AbsoluteDate.J2000_EPOCH, new double[et.getUnits().size()],
                               Constants.EIGEN5C_EARTH_MU);
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE,
                                    oe.getSpecifier());
            }
        }
    }

    @Test
    public void testUnsupportedRetrograde() {
        for (final ElementsType et : Arrays.asList(ElementsType.EQUINOCTIAL, ElementsType.EQUINOCTIALMOD)) {
            try {
                et.toCartesian(AbsoluteDate.J2000_EPOCH,
                               new double[] { 7e6, 1.0e-3, 2.0e-3, 1.2, 0.4, 0.5, -1 },
                               Constants.EIGEN5C_EARTH_MU);
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_UNSUPPORTED_RETROGRADE_EQUINOCTIAL,
                                    oe.getSpecifier());
            }
        }
    }

    @Test
    public void testCartesian() {

        final double[] elements = { 1e6, 2e6, 3e6, 4e3, 5e3, 6e3, 7, 8, 9 };

        final TimeStampedPVCoordinates p = ElementsType.CARTP.toCartesian(AbsoluteDate.J2000_EPOCH,
                                                                          elements, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertEquals(14.0e12, p.getPosition().getNormSq(),     1.0);
        Assertions.assertEquals(0.0,     p.getVelocity().getNormSq(),     1.0e-12);
        Assertions.assertEquals(0.0,     p.getAcceleration().getNormSq(), 1.0e-12);

        final TimeStampedPVCoordinates pv = ElementsType.CARTPV.toCartesian(AbsoluteDate.J2000_EPOCH,
                                                                            elements, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertEquals(14.0e12, pv.getPosition().getNormSq(),     1.0);
        Assertions.assertEquals(77.0e6,  pv.getVelocity().getNormSq(),     1.0);
        Assertions.assertEquals(0.0,     pv.getAcceleration().getNormSq(), 1.0e-12);

        final TimeStampedPVCoordinates pva = ElementsType.CARTPVA.toCartesian(AbsoluteDate.J2000_EPOCH,
                                                                              elements, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertEquals(14.0e12, pva.getPosition().getNormSq(),     1.0);
        Assertions.assertEquals(77.0e6,  pva.getVelocity().getNormSq(),     1.0);
        Assertions.assertEquals(194.0,   pva.getAcceleration().getNormSq(), 1.0);

    }

    @Test
    public void testKeplerian() {
        TimeStampedPVCoordinates cart =
                        ElementsType.KEPLERIAN.
                        toCartesian(AbsoluteDate.ARBITRARY_EPOCH,
                                    new double[] { 24464560.0, 0.7311, 0.122138, 1.00681, 3.10686, 0.048363 },
                                    3.9860047e14);
        Vector3D pos = cart.getPosition();
        Vector3D vel = cart.getVelocity();
        Assertions.assertEquals(-3442769.3470219444, pos.getX(), Utils.epsilonTest * FastMath.abs(pos.getX()));
        Assertions.assertEquals(-5609538.400204982,  pos.getY(), Utils.epsilonTest * FastMath.abs(pos.getY()));
        Assertions.assertEquals(-10929.660213580295, pos.getZ(), Utils.epsilonTest * FastMath.abs(pos.getZ()));

        Assertions.assertEquals(8551.139870105022,   vel.getX(), Utils.epsilonTest * FastMath.abs(vel.getX()));
        Assertions.assertEquals(-5491.048921200239,  vel.getY(), Utils.epsilonTest * FastMath.abs(vel.getY()));
        Assertions.assertEquals(-1247.3904560056558, vel.getZ(), Utils.epsilonTest * FastMath.abs(vel.getZ()));
    }

    @Test
    public void testKeplerianMean() {
        TimeStampedPVCoordinates cart =
                        ElementsType.KEPLERIANMEAN.
                        toCartesian(AbsoluteDate.ARBITRARY_EPOCH,
                                    new double[] { 24464560.0, 0.7311, 0.122138, 1.00681, 3.10686, 0.048363 },
                                    3.9860047e14);
        Vector3D pos = cart.getPosition();
        Vector3D vel = cart.getVelocity();
        Assertions.assertEquals(-0.107622532467967e+07, pos.getX(), Utils.epsilonTest * FastMath.abs(pos.getX()));
        Assertions.assertEquals(-0.676589636432773e+07, pos.getY(), Utils.epsilonTest * FastMath.abs(pos.getY()));
        Assertions.assertEquals(-0.332308783350379e+06, pos.getZ(), Utils.epsilonTest * FastMath.abs(pos.getZ()));

        Assertions.assertEquals( 0.935685775154103e+04, vel.getX(), Utils.epsilonTest * FastMath.abs(vel.getX()));
        Assertions.assertEquals(-0.331234775037644e+04, vel.getY(), Utils.epsilonTest * FastMath.abs(vel.getY()));
        Assertions.assertEquals(-0.118801577532701e+04, vel.getZ(), Utils.epsilonTest * FastMath.abs(vel.getZ()));
    }

    @Test
    public void testEquinoctial() {
        TimeStampedPVCoordinates cart =
                        ElementsType.EQUINOCTIAL.
                        toCartesian(AbsoluteDate.ARBITRARY_EPOCH,
                                    new double[] { 7000000.0, 0.01, -0.02, FastMath.toRadians(40.), 2.1, 1.2, +1 },
                                    3.9860047e14);
        Vector3D pRef = new Vector3D(2004367.298657, 6575317.978060, -1518024.843914);
        Vector3D vRef = new Vector3D(5574.049, -368.839, 5009.529);
        Assertions.assertEquals(0, cart.getPosition().subtract(pRef).getNorm(), 1.0e-6);
        Assertions.assertEquals(0, cart.getVelocity().subtract(vRef).getNorm(), 1.0e-3);
    }

    @Test
    public void testEquinoctialMod() {
        TimeStampedPVCoordinates cart =
                        ElementsType.EQUINOCTIALMOD.
                        toCartesian(AbsoluteDate.ARBITRARY_EPOCH,
                                    new double[] { 7000000.0, 0.01, -0.02, FastMath.toRadians(40.), 2.1, 1.2, +1 },
                                    3.9860047e14);
        Vector3D pRef = new Vector3D(1777672.636613, 6587379.027297, -1720306.101389);
        Vector3D vRef = new Vector3D(5660.262, -63.842, 4933.262);
        Assertions.assertEquals(0, cart.getPosition().subtract(pRef).getNorm(), 1.0e-6);
        Assertions.assertEquals(0, cart.getVelocity().subtract(vRef).getNorm(), 1.0e-3);
    }

    @Test
    public void checkWrongNumber() {
        try {
            ElementsType.CARTP.checkUnits(Collections.singletonList(Unit.KILOMETRE));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_ELEMENT_SET_WRONG_NB_COMPONENTS,
                                oe.getSpecifier());
        }
    }

    @Test
    public void checkWrongUnitsDimension() {
        try {
            ElementsType.CARTPV.checkUnits(Arrays.asList(Unit.KILOMETRE, Unit.KILOMETRE, Unit.KILOMETRE,
                                                         Units.KM_PER_S, Units.KM_PER_S, Units.KM_PER_S2));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("km/s",  oe.getParts()[0]);
            Assertions.assertEquals("km/s²", oe.getParts()[1]);
        }
    }

    @Test
    public void checkWrongUnitsScale() {
        try {
            ElementsType.CARTPV.checkUnits(Arrays.asList(Unit.KILOMETRE, Unit.KILOMETRE, Unit.KILOMETRE,
                                                         Units.KM_PER_S, Units.KM_PER_S, Unit.parse("m/s")));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("km/s", oe.getParts()[0]);
            Assertions.assertEquals("m/s",  oe.getParts()[1]);
        }
    }

    @Test
    public void checkCorreectUnits() {
        ElementsType.CARTPV.checkUnits(Arrays.asList(Unit.KILOMETRE, Unit.KILOMETRE, Unit.KILOMETRE,
                                                     Units.KM_PER_S, Units.KM_PER_S, Units.KM_PER_S));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
