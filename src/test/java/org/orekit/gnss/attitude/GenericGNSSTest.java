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
package org.orekit.gnss.attitude;

import org.junit.jupiter.api.Test;


public class GenericGNSSTest extends AbstractGNSSAttitudeProviderTest {

    @Test
    public void testPatchedLargeNegativeBetaGalileo() {
        doTestAxes("patched-eclips/beta-large-negative-GALILEO.txt", 7.3e-15, 5.8e-16, true);
    }

    @Test
    public void testPatchedLargePositiveBetaGalileo() {
        doTestAxes("patched-eclips/beta-large-positive-GALILEO.txt", 7.3e-15, 7.9e-16, true);
    }

    @Test
    public void testPatchedLargeNegativeBetaGlonass() {
        doTestAxes("patched-eclips/beta-large-negative-GLONASS.txt", 7.2e-15, 1.1e-15, true);
    }

    @Test
    public void testPatchedLargePositiveBetaGLONASS() {
        doTestAxes("patched-eclips/beta-large-positive-GLONASS.txt", 6.8e-15, 9.2e-16, true);
    }

    @Test
    public void testPatchedLargeNegativeBetaBlockIIA() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIA.txt", 6.1e-15, 8.7e-16, true);
    }

    @Test
    public void testPatchedLargePositiveBetaBlockIIA() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIA.txt", 7.2e-15, 8.8e-16, true);
    }

    @Test
    public void testPatchedLargeNegativeBetaBlockIIF() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIF.txt", 6.8e-15, 7.6e-16, true);
    }

    @Test
    public void testPatchedLargePositiveBetaBlockIIF() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIF.txt", 7.4e-15, 6.7e-16, true);
    }

    @Test
    public void testPatchedLargeNegativeBetaBlockIIR() {
        doTestAxes("patched-eclips/beta-large-negative-BLOCK-IIR.txt", 8.0e-15, 8.7e-16, true);
    }

    @Test
    public void testPatchedLargePositiveBetaBlockIIR() {
        doTestAxes("patched-eclips/beta-large-positive-BLOCK-IIR.txt",  6.7e-15, 9.1e-16, true);
    }

    @Test
    public void testOriginalLargeNegativeBetaGalileo() {
        doTestAxes("original-eclips/beta-large-negative-GALILEO.txt", 7.3e-15, 5.8e-16, true);
    }

    @Test
    public void testOriginalLargePositiveBetaGalileo() {
        doTestAxes("original-eclips/beta-large-positive-GALILEO.txt", 7.3e-15, 7.9e-16, true);
    }

    @Test
    public void testOriginalLargeNegativeBetaGlonass() {
        doTestAxes("original-eclips/beta-large-negative-GLONASS.txt", 7.2e-15, 1.1e-15, true);
    }

    @Test
    public void testOriginalLargePositiveBetaGLONASS() {
        doTestAxes("original-eclips/beta-large-positive-GLONASS.txt", 6.8e-15, 9.2e-16, true);
    }

    @Test
    public void testOriginalLargeNegativeBetaBlockIIA() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIA.txt", 6.1e-15, 8.7e-16, true);
    }

    @Test
    public void testOriginalLargePositiveBetaBlockIIA() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIA.txt", 7.2e-15, 8.8e-16, true);
    }

    @Test
    public void testOriginalLargeNegativeBetaBlockIIF() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIF.txt", 6.8e-15, 7.6e-16, true);
    }

    @Test
    public void testOriginalLargePositiveBetaBlockIIF() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIF.txt", 7.4e-15, 6.7e-16, true);
    }

    @Test
    public void testOriginalLargeNegativeBetaBlockIIR() {
        doTestAxes("original-eclips/beta-large-negative-BLOCK-IIR.txt", 8.0e-15, 8.8e-16, true);
    }

    @Test
    public void testOriginalLargePositiveBetaBlockIIR() {
        doTestAxes("original-eclips/beta-large-positive-BLOCK-IIR.txt",  6.7e-15, 9.1e-16, true);
    }

}
