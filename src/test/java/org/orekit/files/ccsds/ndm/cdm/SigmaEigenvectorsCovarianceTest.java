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
package org.orekit.files.ccsds.ndm.cdm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Tests for SigmaEigenvectorsCovariance class.
 * These tests are used to increase condition coverage, other global tests are in CdmParser/WriterTest classes.
 */
public class SigmaEigenvectorsCovarianceTest {

    /** Condition coverage on the checkScreenVolumeConditions method. */
    @Test
    public void testConditions() {

        SigmaEigenvectorsCovariance cov = new SigmaEigenvectorsCovariance(false);
        
        // !isAltCovFlagSet() in validate method
        try {
            cov.validate(1.0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, oe.getSpecifier());
            Assertions.assertEquals(CdmMetadataKey.ALT_COV_TYPE, oe.getParts()[0]);
        }
        
        // !isAltCovFlagSet() in setCsig3eigvec3 method
        try {
            cov.setCsig3eigvec3(null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_DATE_INVALID_PREAMBLE_FIELD, oe.getSpecifier());
            Assertions.assertEquals(CdmMetadataKey.ALT_COV_TYPE, oe.getParts()[0]);
        }
        
        // Check get/setCsig3eigvec3 when null
        cov = new SigmaEigenvectorsCovariance(true);
        cov.setCsig3eigvec3(null);
        Assertions.assertEquals(null, cov.getCsig3eigvec3());
    }
}
