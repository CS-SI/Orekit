/* Copyright 2002-2008 CS Communication & Systèmes
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

package fr.cs.examples.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for frames support.
 * <p>This tutorial shows a smart usage of frames and transforms.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class Frames1 {

    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // The local orbital frame (LOF) has its origin at the satellite center of gravity (CoG).
            // It is derived from EME2000 frame by an unknown transform which relies at some date on
            // the position and the velocity to be found.
            // Let's initialize this transform by the identity transform.
            Frame lofFrame = new Frame(Frame.getEME2000(), Transform.IDENTITY, "LOF");

            // Considering the following Computing/Measurement date in UTC time scale
            TimeScale utc = UTCScale.getInstance();
            AbsoluteDate date = new AbsoluteDate(2008, 10, 01, 12, 00, 00.000, utc);
            
            // The satellite frame, with origin the CoG, is related to the LOF.
            // The satellite attitude with respect to LOF is known at some date
            // by the (roll, pitch, yaw) angles according to the guidance laws.
            // Let's assume the following values at the date :
            double roll  = Math.toRadians(0);
            double pitch = Math.toRadians(0);
            double yaw   = Math.toRadians(0);

            // The transform between LOF and satellite frames at the date is defined by these rotation.
            Transform loftosat = new Transform(new Rotation(RotationOrder.XYZ, roll, pitch, yaw));

            // The satellite CoG frame is defined with respect to the LOF
            Frame cogFrame = new Frame(lofFrame, loftosat, "CoG");

            // The GPS antenna frame can be defined from the satellite CoG frame by 2 transforms :
            // a translation and a rotation
            Vector3D translateGPS = new Vector3D(0, 0, 0);
            Rotation rotateGPS = new Rotation(new Vector3D(1, 0, 0), Math.toRadians(0));
            Frame gpsFrame = new Frame(cogFrame, new Transform(new Transform(translateGPS), new Transform(rotateGPS)), "GPS");

            // Satellite position and velocity in ITRF2005 are measured by GPS antenna at the date
            Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            PVCoordinates pvgpsitrf2005 = new PVCoordinates(position, velocity);

            // The transform between GPS antenna and ITRF2005 frames at this date is then
            Transform itrftogps = new Transform(position, velocity);

            // Satellite position and velocity in EME2000 as measured by GPS antenna at the date
            PVCoordinates pvgpseme2000 = Frame.getITRF2005().getTransformTo(Frame.getEME2000(), date).transformPVCoordinates(pvgpsitrf2005);

            // And we can get the position and velocity of satellite CoG in EME2000 frame
            System.out.println("Position of satellite from GPS measurement in EME2000 : "
                               + pvgpseme2000.getPosition());
            System.out.println("Velocity of satellite from GPS measurement in EME2000 : "
                               + pvgpseme2000.getVelocity());

            // So we can update the transform from EME2000 to LOF frame
            lofFrame.updateTransform(gpsFrame, Frame.getITRF2005(), itrftogps, date);

            // And we can get the position and velocity of satellite CoG in EME2000 frame
            System.out.println("Position of satellite CoG in EME2000 : "
                               + lofFrame.getTransformTo(Frame.getEME2000(), date).getTranslation());
            System.out.println("Velocity of satellite CoG in EME2000 : "
                               + lofFrame.getTransformTo(Frame.getEME2000(), date).getVelocity());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
