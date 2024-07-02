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
package org.orekit.gnss.metric.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;

public class IgsSsrMessageTypeTest {

    @Test
    public void testUnknownMessageNumber() {
        try {
            IgsSsrMessageType.getMessageType("-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException re) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, re.getSpecifier());
            Assertions.assertEquals("-1", re.getParts()[0]);
        }
    }

    @Test
    public void testIrnssMessage() {
        Assertions.assertEquals(SatelliteSystem.IRNSS, IgsSsrMessageType.messageNumberToSatelliteSystem(141));
    }

}
