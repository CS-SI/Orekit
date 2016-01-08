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

# Geomagnetic Field

This tutorial shows how to use the Geomagnetic Field Model.

## Introduction

Orekit provides an easy to use class for calculating the geomagnetic field of the earth.
Currently there is support for two different models:

* WMM: [http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml](http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml)
* IGRF: [http://www.ngdc.noaa.gov/IAGA/vmod/igrf.html](http://www.ngdc.noaa.gov/IAGA/vmod/igrf.html)

## Prerequisites

A geomagnetic field model relies on published coefficients files that are loaded on demand.
There exist different formats for these files, but orekit uses a modified version, which may
contain coefficients from multiple epochs inside one file.

The format of the expected model file is as follows:

      {model name} {epoch} {nMax} {nMaxSec} {nMax3} {validity start} {validity end} {minAlt} {maxAlt} {model name} {line number}
    {n} {m} {gnm} {hnm} {dgnm} {dhnm} {model name} {line number}

Example:

       WMM2010  2010.00 12 12  0 2010.00 2015.00   -1.0  600.0          WMM2010   0
    1  0  -29496.6       0.0      11.6       0.0                        WMM2010   1
    1  1   -1586.3    4944.4      16.5     -25.9                        WMM2010   2

The latest coefficient files for IGRF and WMM can be found in attachment.

## Basic Use

To calculate the geomagnetic field of a specified geodetic location and altitude is
pretty much straight-forward.

First retrieve the geomagnetic field model for a desired year (e.g. WMM):

    GeoMagneticField model = GeoMagneticFieldFactory.getWMM(2012.0);

The year has to be specified as a decimal year, which can be converted from a julian
date like this:

    double year = GeoMagneticField.getDecimalYear(2012, 6, 1);

Finally we can calculate the magnetic field at a geodetic location (lat/lon in deg) and
altitude (in km):

    GeoMagneticElements result = model.calculateField(80.0, 0.0, 100.0);

The result contains the seven magnetic components:

* F - Total Intensity of the geomagnetic field
* H - Horizontal Intensity of the geomagnetic field
* X - North Component of the geomagnetic field
* Y - East Component of the geomagnetic field
* Z - Vertical Component of the geomagnetic field
* I (DIP) - Geomagnetic Inclination
* D (DEC) - Geomagnetic Declination (Magnetic Variation)
  