<!--- Copyright 2002-2016 CS Systèmes d'Information
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Integration in Jython

Jython is a Java-based implementation of the Python language, and interfaces well to
Java libraries. The number of Jython modules available for plotting is limited, but
Java libraries can be used.

It is important that the orekit jar file and Hipparchus jars are in the Java CLASSPATH.
This can be set manually at the command prompt, or by using an IDE such as Eclipse
with PyDev. The file `orekit-data.zip` should be in the current directory, the same
as your Jython files.

## Example SlaveMode

This example is a translation of the SlaveMode.java example to Jython, showing a stepped
Keplerian propagation.

    /* Copyright 2002-2016 CS Systèmes d'Information
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

    # orekit.jar,orekit-data.zip and Hipparchus jars in CLASSPATH through eclipse project
    import java, os

    from org.orekit.errors import OrekitException
    from org.orekit.frames import Frame
    from org.orekit.frames import FramesFactory
    from org.orekit.orbits import KeplerianOrbit
    from org.orekit.orbits import Orbit
    from org.orekit.orbits import PositionAngle
    from org.orekit.propagation import SpacecraftState
    from org.orekit.propagation.analytical import KeplerianPropagator
    from org.orekit.data import DataProvidersManager
    from org.orekit.data import ZipJarCrawler
    from org.orekit.time import AbsoluteDate
    from org.orekit.time import TimeScalesFactory

    from math import radians

    # Configure Orekit. The file orekit-data.zip must be in current dir
    DM = DataProvidersManager.getInstance()
    crawler=ZipJarCrawler("orekit-data.zip")
    DM.clearProviders()
    DM.addProvider(crawler)

    #Initial orbit parameters
    a = 24396159    # semi major axis in meters
    e = 0.72831215  # eccentricity
    i = radians(7.0)# inclination
    omega = radians(180) # perigee argument
    raan = radians(261) #right ascension of ascending node
    lM = 0.0 # mean anomaly

    #Inertial frame
    inertialFrame = FramesFactory.getEME2000()

    #Initial date in UTC time scale
    utc = TimeScalesFactory.getUTC();
    initialDate = AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc)

    #gravitation coefficient
    mu =  3.986004415e+14

    #Orbit construction as Keplerian
    initialOrbit = KeplerianOrbit(a, e, i, omega, raan, lM,
                                  PositionAngle.MEAN,
                                  inertialFrame, initialDate, mu)

    #Simple extrapolation with Keplerian motion
    kepler = KeplerianPropagator(initialOrbit)

    #Set the propagator to slave mode (could be omitted as it is the default mode)
    kepler.setSlaveMode()

    #Overall duration in seconds for extrapolation
    duration = 90*60.0

    #Stop date
    finalDate =  AbsoluteDate(initialDate, duration, utc)

    #Step duration in seconds
    stepT = 30.0

    #Extrapolation loop
    cpt = 1
    extrapDate = initialDate
    while extrapDate.compareTo(finalDate) <= 0:
        currentState = kepler.propagate(extrapDate)
        print  "step %d: time %s %s" % (cpt, currentState.getDate(), currentState.getOrbit())
        extrapDate = AbsoluteDate(extrapDate, stepT, utc)
        cpt=cpt+1


## Example: VisibilityCheck

This example is based on the VisiblityCheck.java, translated into Jython. It includes an
example of subclassing of a Java object into a jython object.

    /* Copyright 2002-2016 CS Systèmes d'Information
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

    # Orekit and Hipparchus in CLASSPATH through eclipse project
    import java, os

    from org.orekit.data import DataProvidersManager
    from org.orekit.data import ZipJarCrawler
    from org.hipparchus.geometry import Vector3D
    from org.orekit.bodies import BodyShape
    from org.orekit.bodies import GeodeticPoint
    from org.orekit.bodies import OneAxisEllipsoid
    from org.orekit.errors import OrekitException;
    from org.orekit.frames import Frame
    from org.orekit.frames import FramesFactory
    from org.orekit.frames import TopocentricFrame
    from org.orekit.orbits import KeplerianOrbit
    from org.orekit.orbits import Orbit
    from org.orekit.propagation import Propagator
    from org.orekit.propagation import SpacecraftState
    from org.orekit.propagation.analytical import KeplerianPropagator
    from org.orekit.propagation.events import ElevationDetector
    from org.orekit.propagation.events import EventDetector
    from org.orekit.time import AbsoluteDate
    from org.orekit.time import TimeScalesFactory
    from org.orekit.utils import PVCoordinates
    from org.orekit.utils import IERSConventions

    from math import degrees, radians, pi

    # Configure Orekit
    DM = DataProvidersManager.getInstance()
    crawler=ZipJarCrawler("orekit-data.zip")
    DM.clearProviders()
    DM.addProvider(crawler)

    # Initial state definition: date, orbit
    initialDate = AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC())
    mu =  3.986004415e+14
    inertialFrame = FramesFactory.getEME2000() # inertial frame for orbit definition
    position  = Vector3D(-6142438.668, 3492467.560, -25767.25680)
    velocity  = Vector3D(505.8479685, 942.7809215, 7435.922231)
    pvCoordinates = PVCoordinates(position, velocity)
    initialOrbit = KeplerianOrbit(pvCoordinates, inertialFrame, initialDate, mu)

    # Propagator : consider a simple keplerian motion (could be more elaborate)
    kepler = KeplerianPropagator(initialOrbit)

    #Earth and frame
    ae =  6378137.0 # // equatorial radius in meter
    f  =  1.0 / 298.257223563 #; // flattening
    itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, True) #; // terrestrial frame at an arbitrary date
    earth = OneAxisEllipsoid(ae, f, itrf)

    # Station
    longitude = radians(45.0)
    latitude  = radians(25.0)
    altitude  = 0.0
    station1 = GeodeticPoint(latitude, longitude, altitude)
    sta1Frame = TopocentricFrame(earth, station1, "station1")

    # Event definition
    maxcheck  = 1.0
    elevation = radians(5.0)


    class VisibilityDetector(ElevationDetector):
    # Class for handling the eventOccured java. Example of subclassing
    # a java class in jython
        def __init__(self,  maxCheck,  elevation, topo):
            ElevationDetector.__init__(self,maxCheck, elevation, topo)

        def eventOccurred(self, s, increasing):
            if (increasing):
                print "Visibility on", self.topocentricFrame.getName(),"begins at" , s.getDate()
            else:
                print "Visibility on",  self.topocentricFrame.getName(), "ends at" , s.getDate()
            return self.CONTINUE

    sta1Visi = VisibilityDetector(maxcheck, elevation, sta1Frame)

    #Add event to be detected
    kepler.addEventDetector(sta1Visi)

    #Propagate from the initial date to the first raising or for the fixed duration
    finalState = kepler.propagate(initialDate.shiftedBy(1500.0))

    print "Final state : " , finalState.getDate().durationFrom(initialDate)
