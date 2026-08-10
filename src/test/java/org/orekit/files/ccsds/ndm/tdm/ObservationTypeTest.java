/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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

package org.orekit.files.ccsds.ndm.tdm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.OrekitCcsdsFrameMapper;


class ObservationTypeTest {

    @Test
    public void testMissingCcsdsValue() {
        Assertions.assertThrows(OrekitException.class,
                                () -> ObservationType.RANGE.rawToSI(null, new TdmMetadata(new OrekitCcsdsFrameMapper()), null, 1.0),
                                OrekitMessages.CCSDS_MISSING_OPTIONAL_VALUE.getSourceString());
        Assertions.assertThrows(OrekitException.class,
                () -> ObservationType.RANGE.siToRaw(null, new TdmMetadata(new OrekitCcsdsFrameMapper()), null, 1.0),
                OrekitMessages.CCSDS_MISSING_OPTIONAL_VALUE.getSourceString());
    }
}