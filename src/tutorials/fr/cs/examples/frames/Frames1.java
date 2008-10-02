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
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for transform and frames support.
 * <p>The aim of this tutorial is to manipulate transforms, frames and position and velocity coordinates.<p>
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class Frames1 {

    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // initial point in frame 1 :
            Frame frame1 = new Frame(Frame.getEME2000(), Transform.IDENTITY, "frame 1");
            PVCoordinates pointP1 = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_I);
            System.out.println(" point P1 in frame 1 : " + pointP1);

            // translation transform
            // We want to translate frame1 to the right at the speed of 1 so that P1 is fixed in it.
            Transform frame1toframe2 = new Transform(Vector3D.MINUS_I, Vector3D.MINUS_I);
            // in vectorial transform convention, the translation is actually minusI !
            Frame frame2 = new Frame(frame1, frame1toframe2, "frame 2");
            PVCoordinates pointP2 = frame1.getTransformTo(frame2, new AbsoluteDate())
                                          .transformPVCoordinates(pointP1);
            System.out.println(" point P1 in frame 2 : " + pointP2);

            // rotation transform
            // We want to rotate frame1 of minus PI/2
            // so that P1 has now for coordinates :
            // position : (0,1,0) and velocity : (-2, 1, 0)
            Rotation R = new Rotation(Vector3D.PLUS_K, Math.PI/2);
            // in vectorial transform convention, the rotation is actually PLUS pi/2 !
            Transform frame1toframe3 = new Transform(R, new Vector3D(0, 0, -2));
            Frame frame3 = new Frame(frame1, frame1toframe3, "frame 3");
            PVCoordinates pointP3 = frame1.getTransformTo(frame3, new AbsoluteDate())
                                          .transformPVCoordinates(pointP1);
            System.out.println(" point P1 in frame 3 : " + pointP3);

            // combine translation and rotation
            // The origin of the frame 2 should become the point P3 in frame 3.
            // Let's check this result by combining two transforms in the frame tree :
            PVCoordinates initialPoint = new PVCoordinates(new Vector3D(0,0,0), new Vector3D(0,0,0));
            PVCoordinates finalPoint = frame2.getTransformTo(frame3, new AbsoluteDate())
                                             .transformPVCoordinates(initialPoint);
            System.out.println(" origin of frame 2 expressed in frame 3 : " + finalPoint);

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
