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

# Time

The `org.orekit.time` package is an independent package providing classes to handle epochs and
time scales, and to compare instants.
	
## Time Presentation

The principal class is `AbsoluteDate` which represents a unique instant in time, 
so as to be able to locate it with respect to the many different times scales in
use in the space dynamics and astronomy fields.

This greatly simplifies development as it hides some models internals. For example
when using JPL-based ephemerides, time must be in Terrestrial Time (formerly
known as Ephemeris Time). However, this is an implementation detail and someone
calling Orekit from a high level application should not have to deal with it. The `AbsoluteDate`
class allows users to pass a date regardless of the time scale it was defined in,
conversions will be done as required transparently.

## Time Scales

Dates are commonly defined by specifying a point in a specific _time scale_.
For example, the J2000.0 epoch is defined from its calendar components as
2000-01-01T12:00:00 in Terrestrial Time. It is of prime importance to understand 
the various available time scales definitions to avoid mistakes. The
`TimeScalesFactory` class provides several predefined time scales:

* _International Atomic Time_

  this is the most accurate and regular time scale that can be used at
  the surface of the Earth.

* _Terrestrial Time_ 

  defined by IAU(1991)	recommendation IV _Coordinate time at the surface of the Earth_,
  it is the successor of Ephemeris Time TE. By convention, TT = TAI + 32.184 s.

* _Universal Time Coordinate_

  UTC is mainly related to TAI, but some step adjustments are introduced from time to time to keep take
  into account Earth rotation irregularities and to prevent the _legal_ time from
  drifting with respect to day and night. The International Earth Rotation Service
  (IERS) is in charge of this time-keeping. These adjustments require introduction
  of leap seconds, which means some days are not 86400 seconds long.

* _Universal Time 1_

  UT1 is a time scale directly linked to the actual
  rotation of the Earth. It is an irregular scale, reflecting Earth's irregular
  rotation rate. The offset between UT1 and UTCScale is found in the Earth
  Orientation Parameters published by IERS.

* _Geocentric Coordinate Time_

  Coordinate time at the center of mass of the Earth. This time scale depends
  linearly on TTScale (and hence on TAI).

* _Barycentric Dynamic Time_

  time used to compute ephemerides in the solar system. This time is offset with
  respect to TT by small relativistic corrections due to Earth motion.

* _Barycentric Coordinate Time>_

  coordinate time used for computations in the solar system. This time scale depends
  linearly on TDBScale.

* _Global Positioning System reference scale_

  this scale was equal to UTC at start of the GPS Epoch when UTC was 19 seconds behind TAI,
  and has stayed parallel to TAI since then (i.e. UTC is now offset from GPS due to
  leap seconds). TGPS = TAI - 19 s.

* _Galileo System reference scale_

  this scale is equal to UTC + 13s at Galileo epoch (1999-08-22T00:00:00Z). Galileo System
  Time and GPS time are very close scales. Without any errors, they should be identical.
  The offset between these two scales is the GGTO, it depends on the clocks used to
  realize the time scales. It is of the order of a few tens nanoseconds. This class
  does not implement this offset,

* _GLONASS System reference scale_

  this scale is equal to UTC + 3h at any time. GLONASS System Time does include leap
  seconds just as UTC scale (and they occur at the same instant, which is 3h00 at
  GLONASS clock time since it is 3h ahead of UTC),

* _Quasi-Zenith reference scale_

  Quasi Zenith System Time and GPS time are very close scales. Without any errors,
  they should be identical. The offset between these two scales is the GGTO, it depends
  on the clocks used to realize the time scales. This class does not implement this offset,

* _Greenwich Mean Sidereal Time scale_

  the Greenwich Mean Sidereal Time is the hour angle between the meridian of Greenwich
  and mean equinox of date at 0h UT1.

Orekit supports both the linear models of UTC-TAI offsets used between 1961 and 1972 and
the constant models with only whole seconds offsets (and leap seconds) used since 1972.
The following figure shows the offset history up to 2010.

![plot showing offset between UTC and TAI over the last half century](../images/utc-tai.png)

## Date Definition

There are three main ways to define a date:

* using a location in a time scale as a set of calendar and hourly components
* using a location in a time scale as an apparent seconds offset since an origin
* using an elapsed physical duration since a reference date

The first option is the more straightforward one, but is not sufficient for some needs.
The two last options are confusingly similar, because of the complexity of time scales.
Understanding the differences between the two is key to avoiding large errors.

An _apparent seconds offset_ is the difference between two readings on a clock synchronized
with a time scale. If for example the first reading is 23:59:59 and the second reading is
00:00:00, the the apparent seconds offset is 1 second. An elapsed duration is the count
of seconds that could be measured by a stop watch started at the first instant and stopped
at the second instant. Most of the time, both times are identical. However, if the time scale
is UTC and if the readings are made when a leap second is introduced, then the
elapsed time between the two events is 2 seconds and not 1 second!

The method `offsetFrom` which takes both a date and a time scale as parameters,
computes the apparent offset. The `durationFrom` method which takes only a date as
parameter computes the elapsed duration. In the example above, the first method would return
1 second while the second method would return 2 seconds:
 
    TimeScale    utc   = TimeScalesFactory.getUTC();
    AbsoluteDate start = new AbsoluteDate(2005, 12, 31, 23, 59, 59, utc);
    AbsoluteDate stop  = new AbsoluteDate(2006,  1,  1,  0,  0,  0, utc);
    System.out.println(stop.offsetFrom(start, utc));  // prints 1.0
    System.out.println(stop.durationFrom(start));     // prints 2.0

This property is used in reverse to define dates. We can define the second instant
as the instant which will occur when the reading of the clock is one second away for the
reading of the first date. We can also define it as the instant occurring when two seconds
have elapsed since the first instant, without any reference to an external clock. Both
approaches are possible, it depends on the available data and its meaning. The following
code shows both approaches:

    TimeScale    utc           = TimeScalesFactory.getUTC();
    AbsoluteDate referenceDate = new AbsoluteDate(2005, 12, 31, 23, 59, 59, utc);
    AbsoluteDate date1         = new AbsoluteDate(referenceDate, 1.0, utc);
    AbsoluteDate date2         = new AbsoluteDate(referenceDate, 2.0);

The two variables `date1` and `date2` represent the same instant. The first one has been
defined relative to a time scale, the second one has been defined independently of any time
scale.

## Reference Epochs

Orekit defines 10 reference epochs. The first 7 are commonly used in the space
community, the seventh one is commonly used in the computer science field and the
last two are convenient for initialization in min/max research loops:

* _Julian Epoch_: -4712-01-01 at 12:00:00, TTScale
* _Modified Julian Epoch_: 1858-11-17 at 00:00:00, TTScale
* _Fifties Epoch_: 1950-01-01 at 00:00:00,  TTScale
* _CCSDS Epoch_: 1958-01-01 at 00:00:00,  TAIScale
* _Galileo Epoch_: 1999-08-22 at 00:00:00,  UTCScale
* _GPS Epoch_: 1980-01-06 at 00:00:00,  UTCScale
* _J2000 Epoch_: 2000-01-01 at 12:00:00, TTScale
* _Java Epoch_: 1970-01-01 at 00:00:00, UTCScale
* _Past infinity Epoch_: at infinity in the past
* _Future Epoch_: at infinity in the future

## Time Use

Once it is constructed, an `AbsoluteDate` can be compared to others, and converted to 
and expressed in other time scales. It is used to define states, orbits, frames ...

Classes that include a date implement the `TimeStamped` interface.
The `ChronologicalComparator` singleton can sort objects implementing this
interface chronologically. This is particularly interesting for ephemerides. One trick
that can be used for such collections is to actually define them using generic
`TimeStamped` instances as in the following example. The trick is that we want to
be allowed to use `AbsoluteDate` instances in methods like `headSet`, `tailSet`
or `subSet`.

    public class MyClass implements TimeStamped {
        ...
    }

    public class Ephemeris {

        // we declare the ephemeris as containing general TimeStamped instances
        private final TreeSet<TimeStamped> ephemeris;

        public Ephemeris(MyClass[] array) {
            ephemeris = new TreeSet<TimeStamped>(new ChronologicalComparator());
            for (int i = 0; i < n; ++i) {
                ephemeris.add(new MyClass(...));
            }
        }

        public MyClass getBefore(AbsoluteDate date) {
            // since AbsoluteDate implements TimeStamped, we can do the following
            return (MyClass) ephemeris.headSet(date).last();
        }

        public MyClass getAfter(AbsoluteDate date) {
            // since AbsoluteDate implements TimeStamped, we can do the following
            return (MyClass) ephemeris.tailSet(date).first();
        }

    }


## Time shift

Lots of space flight dynamics objects are date related (dates themselves, of course,
but also attitudes, orbits, position-velocity coordinates or spacecraft states). In some
cases, it is useful to be able to slightly shift these objects by very small time offsets,
for example when computing finite differences. In these cases, it would be cumbersome
to be forced to set up complete propagation models. The time package provide a simple
parameterized interface: `TimeShiftable` which defines a single `shiftedBy` method
that should be implemented by classes allowing such small shifts. The method returns
a new instance shifted in time without changing the original object.

This feature should not be used for large shifts where complex propagation models are
needed.

## Package organization

![time class diagram](../images/design/time-class-diagram.png)
