      program driver
      implicit none
      integer satmax, eclmax
      parameter (satmax = 136, eclmax = 1)
C
      integer idir, iprn, preprn, inprn
      integer neclips(satmax), ieclips(satmax), iblk(satmax)
      integer year, month, day, week, oweek, pweek
      integer isat, code, uin, uout
      double precision mill, omill, pmill, ttag, svbcos, anoon, anight
      double precision rawbet, beta, betaini(satmax), delta, pi, nan
      double precision sp, p2, v2, a, phi1, phi2, prephi, yrtin, ybsin
      double precision eclstm(satmax, eclmax), ecletm(satmax, eclmax)
      double precision xsv(3), santxyz(3), vsvc(3), sun(3), out(3)
      character line*512, type*11, id1*3, id2*4, system*1
      character input*512, output*512
C
      double precision min, sqrt, cos, acos, phi, conti
C
      if (iargc() .ne.2) then
         write (0, *) 'usage: driverEclips input-file output-file'
         stop
      endif
      uin  = 7
      uout = 8
      call getarg(1, input)
      call getarg(2, output)
      open (unit = uin,  file = input)
      open (unit = uout, file = output)
      pi     = 3.1415926535897932385d0
      nan    = -1.0d0
      nan    = sqrt(nan)
      prephi = 0.0d0
      preprn = -1
      pweek  = -1
      pmill = 0.0
 9000 format (i4, x, i2, x, i2, x, i4, x, f16.6, x, a3, x, a11, 2x,
     &        a4, x, f16.6, 2x, f16.6, 2x, f16.6, x,
     &        f16.9, 2x, f16.9, 2x, f16.9, 3(2x, f16.2), 2(x, f15.11))
 10   continue
      read  (uin, '(a)', end = 20) line
      if (line(1:1) .eq. '#') then
C        header line
         write (uout, '(a)') trim(line)
      else
C        data line
         read (line, 9000) year, month, day, oweek, omill,
     &        id1, type, id2, xsv(1), xsv(2), xsv(3),
     &        vsvc(1), vsvc(2), vsvc(3), sun(1), sun(2), sun(3),
     &        rawbet, delta
        read (id1, '(a, i2)') system, isat 
        read (id2, '(a, i3)') system, code
        if (type .eq. 'BEIDOU-2G  ') then
           iprn       = 100 + isat
           iblk(iprn) = 23
           anoon      = nan
           anight     = 180.0d0 + 2.0d0
        else if (type .eq. 'BEIDOU-2I  ') then
           iprn       = 100 + isat
           iblk(iprn) = 22
           anoon      = nan
           anight     = 180.0d0 + 2.0d0
        else if (type .eq. 'BEIDOU-2M  ') then
           iprn       = 100 + isat
           iblk(iprn) = 21
           anoon      = nan
           anight     = 180.0d0 + 2.0d0
        else if (type .eq. 'BLOCK-IIA  ') then
           iprn       = isat
           iblk(iprn) = 3
           anoon      = nan
           anight     = 180.0d0 + 13.25d0
        else if (type .eq. 'BLOCK-IIF  ') then
           iprn       = isat
           iblk(iprn) = 6
           anoon      = nan
           anight     = 180.0d0 + 13.25d0
        else if (type .eq. 'BLOCK-IIR-A') then
           iprn       = isat
           iblk(iprn) = 4
           anoon      = nan
           anight     = 180.0d0 + 13.25d0
        else if (type .eq. 'BLOCK-IIR-B') then
           iprn       = isat
           iblk(iprn) = 4
           anoon      = nan
           anight     = 180.0d0 + 13.25d0
        else if (type .eq. 'BLOCK-IIR-M') then
           iprn       = isat
           iblk(iprn) = 5
           anoon      = nan
           anight     = 180.0d0 + 13.25d0
        else if (type .eq. 'GALILEO-1  ') then
           iprn       = isat + 64
           iblk(iprn) = -1
           anoon      = 15.0
           anight     = nan
        else if (type .eq. 'GALILEO-2  ') then
           iprn       = isat + 64
           iblk(iprn) = -1
           anoon      = 15.0d0 * pi / 180.0d0
           anight     = nan
        else if (type .eq. 'GLONASS-M  ') then
           iprn       = isat + 32
           iblk(iprn) = -1
           anoon      = nan
           anight     = 180.0d0 + 14.2d0
        endif
        idir = 1
        if (iprn .eq. preprn .and.
     &      omill .lt. pmill .and. oweek .eq. (pweek + 1)) then
C           fix week to avoid decreasing ttag
            week = oweek - 1
            mill = omill + 7.0D0 * 86400.0D0 * 1000.0D0
        else
C           keep original week
            week = oweek
            mill = omill
        endif
        ttag  = mill / 1000.0d0
        pweek = week
        pmill = mill
        svbcos = cos(delta * pi / 180.0d0)
        beta   = (90.0d0 + rawbet) * pi / 180.0d0
        sp = xsv(1) * sun(1) + xsv(2) * sun(2) + xsv(3) * sun(3)
        p2 = xsv(1) * xsv(1) + xsv(2) * xsv(2) + xsv(3) * xsv(3)
        v2 = vsvc(1) * vsvc(1) + vsvc(2) * vsvc(2) + vsvc(3) * vsvc(3)
        santxyz(1) = p2 * sun(1) - sp * xsv(1)
        santxyz(2) = p2 * sun(2) - sp * xsv(2)
        santxyz(3) = p2 * sun(3) - sp * xsv(3)
        a = sqrt(santxyz(1) * santxyz(1) +
     &           santxyz(2) * santxyz(2) +
     &           santxyz(3) * santxyz(3))
        santxyz(1) = santxyz(1) / a
        santxyz(2) = santxyz(2) / a
        santxyz(3) = santxyz(3) / a
        out(1) = santxyz(1)
        out(2) = santxyz(2)
        out(3) = santxyz(3)
        if (iprn .ne. preprn) then
            prephi          = 0.0d0
            betaini(iprn)   = 0.0d0
            neclips(iprn)   = 0
            eclstm(iprn, 1) = 0
            ecletm(iprn, 1) = 0
        endif
        preprn = iprn
        yrtin  = 0.0d0
        ybsin  = 0.0d0
        inprn  = 0
        call eclips(idir, iprn, ttag, svbcos, anoon, anight,
     &              neclips, eclstm, ecletm, ieclips, pi,
     &              xsv, out, vsvc, beta, iblk, betaini,
     &              yrtin, ybsin, inprn)
        phi1   = conti(phi(xsv, vsvc, santxyz) * 180d0 / pi, prephi)
        phi2   = conti(phi(xsv, vsvc, out)     * 180d0 / pi, phi1)
        prephi = phi1
        write (uout, 9010) year, month, day, oweek, omill,
     &     id1, type, id2, xsv(1), xsv(2), xsv(3),
     &     vsvc(1), vsvc(2), vsvc(3), sun(1), sun(2), sun(3),
     &       rawbet, delta,
     &       santxyz(1), santxyz(2), santxyz(3), phi1,
     &       out(1), out(2), out(3), phi2
 9010   format (i4, '-', i2.2, '-', i2.2, x, i4, x, f16.6, x, a3, x,
     &          a11, 2x, a4, x, f16.6, 2x, f16.6, 2x, f16.6, x,
     &          f16.9, 2x, f16.9, 2x, f16.9, 3(2x, f16.2), 2(x, f15.11),
     &          2(3(2x, f21.14), 2x, f15.10))
      endif
      goto 10
 20   continue
      close(uin)
      close(uout)
      stop
      end
      double precision function phi(p, v, x)
      implicit none
      double precision p(3), v(3), x(3)
      double precision c(3), vx, vnorm, xnorm, cx
      double precision sqrt, acos, min, sign
C
C     compute orbital momentum
      c(1) = p(2) * v(3) - p(3) * v(2)
      c(2) = p(3) * v(1) - p(1) * v(3)
      c(3) = p(1) * v(2) - p(2) * v(1)
C
      vx    = v(1) * x(1) + v(2) * x(2) + v(3) * x(3)
      xnorm = sqrt(x(1) * x(1) + x(2) * x(2) + x(3) * x(3))
      vnorm = sqrt(v(1) * v(1) + v(2) * v(2) + v(3) * v(3))
      cx    = c(1) * x(1) + c(2) * x(2) + c(3) * x(3)
      phi   = sign(acos(min(1.0, vx / (vnorm * xnorm))), -cx)
      return
      end
      double precision function conti(angle, prev)
      implicit none
      double precision angle, prev
      conti = angle
 10   continue
      if (conti .lt. prev - 180.0d0) then
         conti = conti + 360.0d0
         goto 10
      endif
 20   continue
      if (conti .gt. prev + 180.0d0) then
         conti = conti - 360.0d0
         goto 20
      endif
      return
      end
