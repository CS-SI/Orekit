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


import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;


public class SlerpRallyingLaw implements AttitudeLaw {

    
    /** Serializable UID. */
    private static final long serialVersionUID = 2909376386948681570L;

    /** Evolution spin axis. */
    final Vector3D evolAxis;
    
    /** Evolution spin rate. */
    final double evolRate;

    /** Evolution spin. */
    final Vector3D evolSpin;

    /** Start attitude. */
    final Attitude attitude1;
    
    /** Creates a new instance from a start attitude and an end attitude.
     * @param att1 start attitude
     * @param att2 end attitude
     */
    public SlerpRallyingLaw(Attitude att1, Attitude att2)
        throws OrekitException {
        
        if (att1.getReferenceFrame() != att2.getReferenceFrame()) {
            throw OrekitException.createIllegalArgumentException(
                  OrekitMessages.FRAMES_MISMATCH,
                  att1.getReferenceFrame().getName(), att2.getReferenceFrame().getName());
        }
 
        attitude1 = att1;
        
        /** Get evolution rotation */ 
        final Rotation evolution = att1.getRotation().applyTo((att2.getRotation().revert()));
        
        /** Get evolution axis and angle */
        evolAxis =   evolution.getAxis();
        evolRate = -(evolution.getAngle ()) / (att2.getDate().durationFrom(att1.getDate()));
        evolSpin = new Vector3D(evolRate, evolAxis);
    }

    /*@inherit */
    public Attitude getAttitude(AbsoluteDate date)
        throws OrekitException {
        
        /** Compute intermediary rotation at required date */
        Rotation intermEvolRot = new Rotation(evolAxis, evolRate * date.durationFrom(attitude1.getDate()));

        return new Attitude(date, attitude1.getReferenceFrame(), 
                            intermEvolRot.applyTo(attitude1.getRotation()), evolSpin);
    }

}
