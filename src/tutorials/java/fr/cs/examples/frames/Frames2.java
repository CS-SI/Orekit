/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3DFormat;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for advanced frames support.
 * <p>This tutorial shows a smart usage of frames and transforms.</p>
 * @author Pascal Parraud
 */
public class Frames2 {

    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            Vector3DFormat v3 = new Vector3DFormat(new DecimalFormat("0.000",     symbols));
            Vector3DFormat v7 = new Vector3DFormat(new DecimalFormat("0.0000000", symbols));

            // The Center of Gravity frame has its origin at the satellite center of gravity (CoG)
            // and its axes parallel to EME2000. It is derived from EME2000 frame at any moment
            // by an unknown transform which depends on the current position and the velocity.
            // Let's initialize this transform by the identity transform.
            Frame cogFrame = new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "LOF", false);

            // The satellite frame, with origin also at the CoG, depends on attitude.
            // For the sake of this tutorial, we consider a simple inertial attitude here
            Transform cogToSat = new Transform(new Rotation(0.6, 0.48, 0, 0.64, false));
            Frame satFrame = new Frame(cogFrame, cogToSat, "sat", false);

            // Finally, the GPS antenna frame can be defined from the satellite frame by 2 transforms:
            // a translation and a rotation
            Transform translateGPS = new Transform(new Vector3D(0, 0, 1));
            Transform rotateGPS    = new Transform(new Rotation(new Vector3D(0, 1, 3), FastMath.toRadians(10)));
            Frame gpsFrame         = new Frame(satFrame, new Transform(translateGPS, rotateGPS), "GPS", false);

            // Considering the following Computing/Measurement date in UTC time scale
            TimeScale utc = TimeScalesFactory.getUTC();
            AbsoluteDate date = new AbsoluteDate(2008, 10, 01, 12, 00, 00.000, utc);

            // Let's get the satellite position and velocity in ITRF2005 as measured by GPS antenna at this moment:
            final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            System.out.println("GPS antenna position in ITRF2005: " + v3.format(position));
            System.out.println("GPS antenna velocity in ITRF2005: " + v7.format(velocity));

            // The transform from GPS frame to ITRF2005 frame at this moment is defined by
            // a translation and a rotation. The translation is directly provided by the
            // GPS measurement above. The rotation is extracted from the existing tree, where
            // we know all rotations are already up to date, even if one translation is still
            // unknown. We combine the extracted rotation and the measured translation by
            // applying the rotation first because the position/velocity vector are given in
            // ITRF frame not in GPS antenna frame:
            Transform measuredTranslation = new Transform(position, velocity);
            Transform formerTransform     = gpsFrame.getTransformTo(FramesFactory.getITRF2005(true), date);
            Transform preservedRotation   = new Transform(formerTransform.getRotation(),
                                                          formerTransform.getRotationRate());
            Transform gpsToItrf           = new Transform(preservedRotation, measuredTranslation);

            // So we can update the transform from EME2000 to CoG frame
            cogFrame.updateTransform(gpsFrame, FramesFactory.getITRF2005(true), gpsToItrf, date);

            // And we can get the position and velocity of satellite CoG in EME2000 frame
            PVCoordinates origin  = PVCoordinates.ZERO;
            Transform cogToItrf   = cogFrame.getTransformTo(FramesFactory.getITRF2005(true), date);
            PVCoordinates satItrf = cogToItrf.transformPVCoordinates(origin);
            System.out.println("Satellite position in ITRF2005: " + v3.format(satItrf.getPosition()));
            System.out.println("Satellite velocity in ITRF2005: " + v7.format(satItrf.getVelocity()));

            Transform cogToEme2000   = cogFrame.getTransformTo(FramesFactory.getEME2000(), date);
            PVCoordinates satEME2000 = cogToEme2000.transformPVCoordinates(origin);
            System.out.println("Satellite position in EME2000: " +
                               v3.format(satEME2000.getPosition()));
            System.out.println("Satellite velocity in EME2000: " +
                               v7.format(satEME2000.getVelocity()));

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }

}
