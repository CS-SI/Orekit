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

import java.io.IOException;

import org.orekit.files.ccsds.definitions.Units;
import org.orekit.files.ccsds.section.AbstractWriter;
import org.orekit.files.ccsds.utils.generation.Generator;

/**
 * Writer for RTN covariance matrix data block for CCSDS Conjunction Data Messages.
 *
 * @author Melina Vanel
 * @since 11.2
 */
public class RTNCovarianceWriter extends AbstractWriter {

    /** RTN covariance block. */
    private final RTNCovariance rtnCovariance;

    /** Create a writer.
     * @param xmlTag name of the XML tag surrounding the section
     * @param kvnTag name of the KVN tag surrounding the section (may be null)
     * @param RTNCovariance RTN covariance data to write
     */
    RTNCovarianceWriter(final String xmlTag, final String kvnTag,
                        final RTNCovariance RTNCovariance) {
        super(xmlTag, kvnTag);
        this.rtnCovariance = RTNCovariance;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final Generator generator) throws IOException {

        generator.writeComments(rtnCovariance.getComments());

        // RTN covariance matrix
        generator.writeEntry(RTNCovarianceKey.CR_R.name(),         rtnCovariance.getCrr(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CT_R.name(),         rtnCovariance.getCtr(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CT_T.name(),         rtnCovariance.getCtt(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CN_R.name(),         rtnCovariance.getCnr(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CN_T.name(),         rtnCovariance.getCnt(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CN_N.name(),         rtnCovariance.getCnn(),       Units.M2,          true);
        generator.writeEntry(RTNCovarianceKey.CRDOT_R.name(),      rtnCovariance.getCrdotr(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CRDOT_T.name(),      rtnCovariance.getCrdott(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CRDOT_N.name(),      rtnCovariance.getCrdotn(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CRDOT_RDOT.name(),   rtnCovariance.getCrdotrdot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CTDOT_R.name(),      rtnCovariance.getCtdotr(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CTDOT_T.name(),      rtnCovariance.getCtdott(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CTDOT_N.name(),      rtnCovariance.getCtdotn(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CTDOT_RDOT.name(),   rtnCovariance.getCtdotrdot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CTDOT_TDOT.name(),   rtnCovariance.getCtdottdot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_R.name(),      rtnCovariance.getCndotr(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_T.name(),      rtnCovariance.getCndott(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_N.name(),      rtnCovariance.getCndotn(),    Units.M2_PER_S,    true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_RDOT.name(),   rtnCovariance.getCndotrdot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_TDOT.name(),   rtnCovariance.getCndottdot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CNDOT_NDOT.name(),   rtnCovariance.getCndotndot(), Units.M2_PER_S2,   true);
        generator.writeEntry(RTNCovarianceKey.CDRG_R.name(),       rtnCovariance.getCdrgr(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CDRG_T.name(),       rtnCovariance.getCdrgt(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CDRG_N.name(),       rtnCovariance.getCdrgn(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CDRG_RDOT.name(),    rtnCovariance.getCdrgrdot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CDRG_TDOT.name(),    rtnCovariance.getCdrgtdot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CDRG_NDOT.name(),    rtnCovariance.getCdrgndot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CDRG_DRG.name(),     rtnCovariance.getCdrgdrg(),   Units.M4_PER_KG2,  false);
        generator.writeEntry(RTNCovarianceKey.CSRP_R.name(),       rtnCovariance.getCsrpr(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CSRP_T.name(),       rtnCovariance.getCsrpt(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CSRP_N.name(),       rtnCovariance.getCsrpn(),     Units.M3_PER_KG,   false);
        generator.writeEntry(RTNCovarianceKey.CSRP_RDOT.name(),    rtnCovariance.getCsrprdot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CSRP_TDOT.name(),    rtnCovariance.getCsrptdot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CSRP_NDOT.name(),    rtnCovariance.getCsrpndot(),  Units.M3_PER_KGS,  false);
        generator.writeEntry(RTNCovarianceKey.CSRP_DRG.name(),     rtnCovariance.getCsrpdrg(),   Units.M4_PER_KG2,  false);
        generator.writeEntry(RTNCovarianceKey.CSRP_SRP.name(),     rtnCovariance.getCsrpsrp(),   Units.M4_PER_KG2,  false);
        generator.writeEntry(RTNCovarianceKey.CTHR_R.name(),       rtnCovariance.getCthrr(),     Units.M2_PER_S2,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_T.name(),       rtnCovariance.getCthrt(),     Units.M2_PER_S2,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_N.name(),       rtnCovariance.getCthrn(),     Units.M2_PER_S2,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_RDOT.name(),    rtnCovariance.getCthrrdot(),  Units.M2_PER_S3,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_TDOT.name(),    rtnCovariance.getCthrtdot(),  Units.M2_PER_S3,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_NDOT.name(),    rtnCovariance.getCthrndot(),  Units.M2_PER_S3,   false);
        generator.writeEntry(RTNCovarianceKey.CTHR_DRG.name(),     rtnCovariance.getCthrdrg(),   Units.M3_PER_KGS2, false);
        generator.writeEntry(RTNCovarianceKey.CTHR_SRP.name(),     rtnCovariance.getCthrsrp(),   Units.M3_PER_KGS2, false);
        generator.writeEntry(RTNCovarianceKey.CTHR_THR.name(),     rtnCovariance.getCthrthr(),   Units.M2_PER_S4,   false);

    }
}
