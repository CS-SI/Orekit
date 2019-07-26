The data files for attitude reference were created as follows:

1) retrieved SP3 files for several different constellations from IGS MGEX
   data analysis at <ftp://cddis.gsfc.nasa.gov/pub/gps/products/mgex/>.
   The files selected came from GFZ (GeoForschungsZentrum Potsdam)
   and correspond to data from week 1800 to week 2007, except for a few
   files missing in the archive at time of retrieval

   baseurl=ftp://cddis.gsfc.nasa.gov/pub/gps/products/mgex
   for week in $(seq 1800 2007) ; do
     for day in $(seq 0 6) ; do
       curl $baseurl/$week/gbm$week$day.sp3.Z > gbm$week$day.sp3.Z
     done
   done

   in a later batch of tests, an additional case was also extracted from
   a much earlier file (week 1218 in May 2003):
   ftp://cddis.gsfc.nasa.gov/pub/gps/products/1218/ngs12183.sp3.Z
   
2) ran the FindBaseSamples java program to pick up 5 subsets of
   alignment events:
     - one subset where the beta angle is around -19°
     - one subset where the beta angle is around -1.5°
     - one subset where the beta angle changes sign nears the noon/midnight crossing
     - one subset where the beta angle is around +1.5°
     - one subset where the beta angle is around +30°
   the beta angle have been selected so we get sample in each category for
   all spacecraft types so we can exercise various cases of the attitude models
   for each detected event in the subsets, we sampled input data that will be needed
   for Kouba reference eclips routine and for Orekit models, from 45 minutes
   before the event to 45 minutes after the event with a 6 minutes step size

   for Beidou MEO and IGSO, the attitude model calls for only four subsets:
     - one subset where the beta angle is around -19°
     - one subset where the beta angle is around -4°
     - one subset where the beta angle is around +4°
     - one subset where the beta angle is around +30°
   and the sample was generated from 72 hours before the event to 72 hours
   after the event with a one hour step size

   the base sample generator was configured to use ITRF using IERS 2010 conventions
   and complete EOP including tides, parse an ANTEX file to retrieve the time-dependent
   mapping between PRN numbers and sat code, parse and propagate through all
   available SP3 files

   the base samples finder generates only the meta-data for each event (satellite,
   event type, start and end of sampling time range, SP3 files covering the range).

3) edited the samples-meta-data.txt file to select only some events, in order to
   get tests for several interesting cases. Some start and end date have also been
   manually edited to match the SP3 files coverage and avoid getting points in the
   15 minutes period after last point of a one day file (at 23:45) and first point
   on the next day file (at 00:00). The samples-meta-data.txt file in the same folder
   as this README.txt file is the result from this manual edition

4) ran the GenerateBaseSamples java program on the edited samples-meta-data.txt.
   the base samples generator generates only first few columns of the reference
   data (date, spacecraft position and velocity, sun position, β and δ  angles. The
   other columns will be completed in the next steps.

5) split the big files into smaller files for various constellations:

   for f in beta-crossing.txt \
            beta-small-negative.txt beta-small-positive.txt \
            beta-large-negative.txt beta-large-positive.txt ; do
     for s in "BEIDOU-2G" "BEIDOU-2I" "BEIDOU-2M" \
              "BLOCK-IIA" "BLOCK-IIF" "BLOCK-IIR" \
              "GALILEO" "GLONASS" ; do
       g=`echo $f | sed "s,.txt,-$s.txt,"`
       head -1 < $f > $g
       grep "$s" $f >> $g
     done
     rm $f
   done

6) applied twelve patches to Kouba reference eclips routine from December 2017 to fix issues identified
   during validation (these patches have also been sent to the author if he wants to include them
   in a next eclips version):
     01) prevent NaNs appearing due to numerical noise at exact alignment
     02) missing declaration for math functions leading to them being used as simple precision instead of double precision
     03) fix literal constants leading to them being used as simple precision instead of double precision
     04) fix abrupt switch back to nominal yaw in some beta crossing cases
     05) fix Beidou attitude in non-perfectly circular orbits
     06) replaced projected planar geometry with spherical geometry in triangles solving
     07) improve local orbital rate computation by taking eccentricity into account
     08) fix rotated X vector normalization problem when orbit is not perfectly circular
         fix rotated X vector division by zero at orbit node
         fix rotated X vector ill-conditioning by not basing it on the singular nominal yaw steering
     09) ensure Glonass model fixed-point algorithm reaches convergence
     10) improve turn start/end dates by recomputing them at each step and keeping the best estimates
     11) removed dependency on caller sampling rate by using interpolated beta model
     12) directly recompute orbit angle in Galileo model rather than assuming linear evolution
   The Kouba reference routine can be found at the IGS Analysis Center Coordinator site
   (http://acc.igs.org/orbits).

   echo eclips_Dec2017.f | tar xvf /the/path/to/eclips_Dec_2017.tar --files-from -
   for f in eclips-*.patch ; do
     patch -p0 < $f
   done
   gfortran -g -o driverEclips driverEclips.f eclips_Dec2017.f

7) compiled the eclips driver program and linked it with the patched Kouba reference
   eclipse routine, and ran it on the small files to add new columns at the end of each
   line with the reference values of the yaw and the rotated X axis direction.

   for f in beta-*.txt ; do
     mv $f $f.old
     ../reference-attitude-generator/driverEclips $f.old $f
     rm $f.old 
   done

8) edited the files to select just a few test cases in all configurations at noon and midnight,
   and to remove first point in Beidou case when wrong (this is due a known limitation of
   eclips when starting already in Orbit Normal mode)

9) repeat steps 4, 5 and 6 but WITHOUT applying the patches, in order to get reference data
   from the original Kouba reference (which of course will lead to slightly different results)
