The data files for attitude reference were created as follows:

1) retrieved SP3 files for several different constellations from IGS MGEX
   data analysis at <ftp://cddis.gsfc.nasa.gov/pub/gps/products/mgex/>.
   The files selected came from GFZ (GeoForschungsZentrum Potsdam)
   and correspond to data from week 1800 to week 1988, except for about
   30 files missing in the archive
   
2) ran the GenerateBaseSample java program to pick up 5 subsets of
   alignment events:
     - one subset where the beta angle is around -19°
     - one subset where the beta angle is around -1.5°
     - one subset where the beta angle changes sign nears the noon/midnight crossing
     - one subset where the beta angle is around +1.5°
     - one subset where the beta angle is around +30°
   the beta angle have been selected so we get sample in each category for
   all spacecraft types so we can exercise various cases of the attitude models
   for each detected event in the subsets, we sampled input data that will be needed
   for Kouba reference eclips routine and for Orekit models, from 41 minutes
   before the event to 41 minutes after the event.

   the base sample generator is configured to use ITRF using IERS 2010 conventions
   and complete EOP including tides, parse an ANTEX file to retrieve the time-dependent
   mapping between PRN numbers and sat code, parse and propagate through all
   available SP3 files

3) split the big files into smaller files for various constellations:

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

4) applied two patches to Kouba reference eclips routine from May 2017 to fix issues detected
   during validation:
     - prevent NaNs appearing due to numerical noise at exact alignment
     - fix rotated X vector normalization problem when orbit is not perfectly circular
     - fix rotated X vector division by zero at orbit node
   The Kouba reference routine can be found at the IGS Analysis Center Coordinator site
   (http://acc.igs.org/orbits). The patch eclips-01-max.patch addresses the first issue
   whereas the patch eclips-02-normalization.patch addresses the second and third issues.
   In fact, as an additional precaution, we took care in the base sample generator program
   to avoid extracting sample points exactly at alignment (we extracted points at -41min,
   -39min, ..., -1min, +1min, ..., +39min, +41min), so the first patch could have been safely
   ignored with these specific samples (but it was mandatory in our first tests as we used
   the perfectly aligned points provided by Orekit alignment detector)

5) compiled the eclips driver program and linked it with the patched Kouba reference
   eclipse routine, and ran it on the small files to add new columns at the end of each
   line with the reference values of the yaw and the rotated X axis direction.

   for f in beta-*.txt ; do
     mv $f $f.old
     ./driverEclips $f.old $f
     rm $f.old 
   done

6) edited the files to select just a few test cases in all configurations at noon and midnight
