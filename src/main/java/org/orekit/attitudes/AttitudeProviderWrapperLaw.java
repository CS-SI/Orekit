/* Copyright 2010 Centre National d'Études Spatiales
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.attitudes;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/** This class is a bridge between an {@link AttitudeProvider AttitudeProvider} and an {@link AttitudeLaw AttitudeLaw} .
 * <p>An attitude provider wrapper law provides a way to implement an {@link AttitudeLaw AttitudeLaw}
 * from an {@link AttitudeProvider AttitudeProvider} and a separate {@link PVCoordinatesProvider PVCoordinatesProvider}.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class AttitudeProviderWrapperLaw
    implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -1690571179199811195L;

    /** Position-velocity provider. */
    PVCoordinatesProvider pvProvider;
    
    /** Attitude provider. */
    AttitudeProvider attProvider;
    
    /** Reference frame from which attitude is computed. */
    Frame referenceFrame;
    
    /** Create new instance.
     * @param referenceFrame reference frame from which attitude is defined
     * @param attProvider attitude provider
     * @param pvProvider position-velocity provider
     */
    public AttitudeProviderWrapperLaw(final Frame referenceFrame,
                                      final PVCoordinatesProvider pvProvider,
                                      final AttitudeProvider attProvider) {
        this.referenceFrame = referenceFrame;
        this.attProvider    = attProvider;
        this.pvProvider     = pvProvider;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(AbsoluteDate date)
        throws OrekitException {
        
        return attProvider.getAttitude(pvProvider, date, referenceFrame);
    }

}
