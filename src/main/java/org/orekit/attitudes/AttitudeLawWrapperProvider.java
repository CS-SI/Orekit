/* Copyright 2010-2011 Centre National d'Études Spatiales
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

/** This class is a bridge between an {@link AttitudeLaw AttitudeLaw} and an {@link AttitudeProvider AttitudeProvider} .
 * <p>An attitude law wrapper provider provides a way to implement an {@link AttitudeProvider AttitudeProvider}
 * from an {@link AttitudeLaw AttitudeLaw}.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class AttitudeLawWrapperProvider implements AttitudeProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 6258333274105840419L;

    /** Attitude provider. */
    private final AttitudeLaw attLaw;

    /** Create new instance.
     * @param attLaw wrapped attitude law
     */
    public AttitudeLawWrapperProvider(final AttitudeLaw attLaw) {
        this.attLaw = attLaw;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return attLaw.getAttitude(date);
    }

}
