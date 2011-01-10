/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * This class handles body center pointing attitude provider.

 * <p>
 * This class represents the attitude provider where the satellite z axis is
 * pointing to the body frame center.</p>
 * <p>
 * The object <code>BodyCenterPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class BodyCenterPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -6528493179834801802L;

    /** Creates new instance.
     * @param bodyFrame Body frame
     */
    public BodyCenterPointing(final Frame bodyFrame) {
        super(bodyFrame);
    }

    /** {@inheritDoc} */
    protected Vector3D getTargetPoint(final PVCoordinatesProvider pvProv,
                                      final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return getBodyFrame().getTransformTo(frame, date).transformPosition(Vector3D.ZERO);
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates getTargetPV(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return getBodyFrame().getTransformTo(frame, date).transformPVCoordinates(PVCoordinates.ZERO);
    }

}
