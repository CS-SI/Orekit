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

package org.orekit.files.ccsds.ndm.odm;

import java.io.IOException;

import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/** Writer for covariance matrix data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CartesianCovarianceWriter extends AbstractWriter {

    /** Covariance matrix block. */
    private final CartesianCovariance covariance;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param covariance covariance matrix to write
     */
    public CartesianCovarianceWriter(final String xmlTag, final String kvnTag,
                                      final CartesianCovariance covariance) {
        super(xmlTag, kvnTag);
        this.covariance = covariance;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        final RealMatrix matrix = covariance.getCovarianceMatrix();

        // covariance block
        generator.writeComments(covariance.getComments());
        // note that there are no epochs in the OPM/OMM covariance matrices (but there are epochs in OEM covariance matrices)
        generator.writeEntry(CartesianCovarianceKey.COV_REF_FRAME.name(), covariance.getReferenceFrame().getName(), null,             false);
        generator.writeEntry(CartesianCovarianceKey.CX_X.name(),          matrix.getEntry(0, 0),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CY_X.name(),          matrix.getEntry(1, 0),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CY_Y.name(),          matrix.getEntry(1, 1),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CZ_X.name(),          matrix.getEntry(2, 0),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CZ_Y.name(),          matrix.getEntry(2, 1),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CZ_Z.name(),          matrix.getEntry(2, 2),                    Units.KM2,        true);
        generator.writeEntry(CartesianCovarianceKey.CX_DOT_X.name(),      matrix.getEntry(3, 0),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CX_DOT_Y.name(),      matrix.getEntry(3, 1),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CX_DOT_Z.name(),      matrix.getEntry(3, 2),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CX_DOT_X_DOT.name(),  matrix.getEntry(3, 3),                    Units.KM2_PER_S2, true);
        generator.writeEntry(CartesianCovarianceKey.CY_DOT_X.name(),      matrix.getEntry(4, 0),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CY_DOT_Y.name(),      matrix.getEntry(4, 1),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CY_DOT_Z.name(),      matrix.getEntry(4, 2),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CY_DOT_X_DOT.name(),  matrix.getEntry(4, 3),                    Units.KM2_PER_S2, true);
        generator.writeEntry(CartesianCovarianceKey.CY_DOT_Y_DOT.name(),  matrix.getEntry(4, 4),                    Units.KM2_PER_S2, true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_X.name(),      matrix.getEntry(5, 0),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Y.name(),      matrix.getEntry(5, 1),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Z.name(),      matrix.getEntry(5, 2),                    Units.KM2_PER_S,  true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_X_DOT.name(),  matrix.getEntry(5, 3),                    Units.KM2_PER_S2, true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Y_DOT.name(),  matrix.getEntry(5, 4),                    Units.KM2_PER_S2, true);
        generator.writeEntry(CartesianCovarianceKey.CZ_DOT_Z_DOT.name(),  matrix.getEntry(5, 5),                    Units.KM2_PER_S2, true);

    }

}
