/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.stk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.files.stk.STKEphemerisFile.STKCoordinateSystem;

/**
 * Unit tests for {@link STKEphemerisFile}.
 */
public final class STKEphemerisFileTest {

  /**
   * Tests parsing of {@link STKCoordinateSystem}.
   */
  @Test
  public void testParseSTKCoordinateSystem() {
      assertEquals(STKCoordinateSystem.ICRF, STKCoordinateSystem.parse("ICRF"));
      assertEquals(STKCoordinateSystem.J2000, STKCoordinateSystem.parse("J2000"));
      assertEquals(STKCoordinateSystem.INERTIAL, STKCoordinateSystem.parse("Inertial"));
      assertEquals(STKCoordinateSystem.FIXED, STKCoordinateSystem.parse("Fixed"));
      assertEquals(STKCoordinateSystem.TRUE_OF_DATE, STKCoordinateSystem.parse("TrueOfDate"));
      assertEquals(STKCoordinateSystem.MEAN_OF_DATE, STKCoordinateSystem.parse("MeanOfDate"));
      assertEquals(STKCoordinateSystem.TEME_OF_DATE, STKCoordinateSystem.parse("TemeOfDate"));
      final OrekitException exception = Assertions.assertThrows(OrekitException.class, () -> STKCoordinateSystem.parse("asdf"));
      assertEquals("STK coordinate system \"asdf\" is invalid or not yet supported", exception.getMessage());
  }

}
