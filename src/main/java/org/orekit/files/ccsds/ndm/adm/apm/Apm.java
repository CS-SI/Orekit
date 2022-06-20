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
package org.orekit.files.ccsds.ndm.adm.apm;

import java.util.List;

import org.hipparchus.complex.Quaternion;
import org.orekit.attitudes.Attitude;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.NdmConstituent;
import org.orekit.files.ccsds.ndm.adm.AdmMetadata;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.frames.Frame;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * This class stores all the information of the Attitude Parameter Message (APM) File parsed
 * by APMParser. It contains the header and the metadata and a the data lines.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class Apm extends NdmConstituent<Header, Segment<AdmMetadata, ApmData>> {

    /** Root element for XML files. */
    public static final String ROOT = "apm";

    /** Key for format version. */
    public static final String FORMAT_VERSION_KEY = "CCSDS_APM_VERS";

    /** Simple constructor.
     * @param header file header
     * @param segments file segments
     * @param conventions IERS conventions
     * @param dataContext used for creating frames, time scales, etc.
     */
    public Apm(final Header header, final List<Segment<AdmMetadata, ApmData>> segments,
               final IERSConventions conventions, final DataContext dataContext) {
        super(header, segments, conventions, dataContext);
    }

    /** Get the file metadata.
     * @return file metadata
     */
    public AdmMetadata getMetadata() {
        return getSegments().get(0).getMetadata();
    }

    /** Get the file data.
     * @return file data
     */
    public ApmData getData() {
        return getSegments().get(0).getData();
    }

    /** Get the attitude.
     * <p>
     * The orientation part of the attitude is always extracted from the file mandatory
     * {@link ApmQuaternion quaternion logical block}. The rotation rate part of
     * the attitude is extracted from the {@link ApmQuaternion quaternion logical block}
     * if rate is available there, or from the {@link Euler Euler logical block} if rate
     * is missing from quaternion logical block but available in Euler logical block.
     * </p>
     * @param frame reference frame with respect to which attitude must be defined,
     * (may be null if attitude is <em>not</em> orbit-relative and one wants
     * attitude in the same frame as used in the attitude message)
     * @param pvProvider provider for spacecraft position and velocity
     * (may be null if attitude is <em>not</em> orbit-relative)
     * @return attitude
     */
    public Attitude getAttitude(final Frame frame, final PVCoordinatesProvider pvProvider) {

        final ApmData       data     = getSegments().get(0).getData();
        final ApmQuaternion qBlock   = data.getQuaternionBlock();
        final Euler         eBlock   = data.getEulerBlock();

        final TimeStampedAngularCoordinates tac;
        if (qBlock.hasRates()) {
            // quaternion logical block includes everything we need
            final Quaternion q    = qBlock.getQuaternion();
            final Quaternion qDot = qBlock.getQuaternionDot();
            tac = AttitudeType.QUATERNION_DERIVATIVE.build(true, qBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                           null, true, qBlock.getEpoch(),
                                                           q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(),
                                                           qDot.getQ0(), qDot.getQ1(), qDot.getQ2(), qDot.getQ3());
        } else if (eBlock != null && eBlock.hasRates()) {
            // we have to rely on the Euler logical block to take rates into account

            if (!qBlock.getEndpoints().isCompatibleWith(eBlock.getEndpoints())) {
                // nothing really prevents having two logical blocks with different settings
                // but it is a nightmare to process and probably not worse the trouble.
                // For now, we just throw an exception, we may reconsider this if some use case arises
                throw new OrekitException(OrekitMessages.INCOMPATIBLE_FRAMES,
                                          qBlock.getEndpoints().toString(),
                                          eBlock.getEndpoints().toString());
            }

            final Quaternion q     = qBlock.getQuaternion();
            final double[]   rates = eBlock.getRotationRates();
            tac = AttitudeType.QUATERNION_RATE.build(true,
                                                     qBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                     eBlock.getEulerRotSeq(), eBlock.isSpacecraftBodyRate(), qBlock.getEpoch(),
                                                     q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(),
                                                     rates[0], rates[1], rates[2]);

        } else {
            // we rely only on the quaternion logical block, despite it doesn't include rates
            final Quaternion q    = qBlock.getQuaternion();
            tac = AttitudeType.QUATERNION.build(true, qBlock.getEndpoints().isExternal2SpacecraftBody(),
                                                null, true, qBlock.getEpoch(),
                                                q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3());
        }

        // build the attitude
        return qBlock.getEndpoints().build(frame, pvProvider, tac);

    }

}
