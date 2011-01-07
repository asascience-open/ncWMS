/*
 * Copyright (c) 2011 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test of the {@link WmsUtils} class.
 * @author Jon
 */
public final class WmsUtilsTest
{
    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = ONE_SECOND * 60;
    private static final int ONE_HOUR = ONE_MINUTE * 60;
    private static final int ONE_DAY = ONE_HOUR * 24;
    private static final DateTime EPOCH = new DateTime(0);

    /**
     * Tests the generation of ISO8601 Period strings from millisecond
     * durations
     */
    @Test
    public void testGetPeriodString()
    {
        assertEquals("PT0.001S",  WmsUtils.getPeriodString(1));
        assertEquals("PT0.009S",  WmsUtils.getPeriodString(9));
        assertEquals("PT0.01S",  WmsUtils.getPeriodString(10));
        assertEquals("PT0.011S",  WmsUtils.getPeriodString(11));
        assertEquals("PT0.099S",  WmsUtils.getPeriodString(99));
        assertEquals("PT0.1S",  WmsUtils.getPeriodString(100));
        assertEquals("PT0.101S",  WmsUtils.getPeriodString(101));
        assertEquals("PT0.109S",  WmsUtils.getPeriodString(109));
        assertEquals("PT0.11S",  WmsUtils.getPeriodString(110));
        assertEquals("PT0.999S",  WmsUtils.getPeriodString(999));
        assertEquals("PT1S",      WmsUtils.getPeriodString(ONE_SECOND));
        assertEquals("PT59.999S", WmsUtils.getPeriodString(ONE_MINUTE - 1));
        assertEquals("PT1M",      WmsUtils.getPeriodString(ONE_MINUTE));
        assertEquals("PT2M30.5S", WmsUtils.getPeriodString(2 * ONE_MINUTE + 30 * ONE_SECOND + 500));
        assertEquals("PT59M59.999S", WmsUtils.getPeriodString(ONE_HOUR - 1));
        assertEquals("PT1H",      WmsUtils.getPeriodString(ONE_HOUR));
        assertEquals("PT12H",     WmsUtils.getPeriodString(ONE_HOUR * 12));
        assertEquals("P1D",       WmsUtils.getPeriodString(ONE_DAY));
        assertEquals("P5D",       WmsUtils.getPeriodString(ONE_DAY * 5));
        assertEquals("P2DT0.001S", WmsUtils.getPeriodString(ONE_DAY * 2 + 1));
        assertEquals("P2DT1H",    WmsUtils.getPeriodString(ONE_DAY * 2 + ONE_HOUR));
        assertEquals("P3DT1H3M",  WmsUtils.getPeriodString(ONE_DAY * 3 + ONE_HOUR + ONE_MINUTE * 3));
        assertEquals("P3DT1H3S",  WmsUtils.getPeriodString(ONE_DAY * 3 + ONE_HOUR + ONE_SECOND * 3));
    }

    /**
     * Tests the generation of regularly-spaced time strings
     */
    @Test
    public void testGetRegularlySpacedTimeString()
    {
        DateTime year1971 = new DateTime(0).withYear(1971);

        // Test if we pass a single time
        List<DateTime> oneTime = Arrays.asList(EPOCH);
        assertEquals(
            WmsUtils.dateTimeToISO8601(EPOCH),
            WmsUtils.getRegularlySpacedTimeString(oneTime, 0).toString()
        );

        // Test if we pass two times
        List<DateTime> twoTimes = Arrays.asList(EPOCH, year1971);
        assertEquals(
            WmsUtils.dateTimeToISO8601(EPOCH) + "," + WmsUtils.dateTimeToISO8601(year1971),
            WmsUtils.getRegularlySpacedTimeString(twoTimes, 0).toString()
        );

        // Now for three times, spaced hourly
        List<DateTime> threeTimes = Arrays.asList(
            EPOCH,
            EPOCH.withHourOfDay(1),
            EPOCH.withHourOfDay(2)
        );
        assertEquals(
            WmsUtils.dateTimeToISO8601(EPOCH) + "/" + WmsUtils.dateTimeToISO8601(EPOCH.withHourOfDay(2)) + "/PT1H",
            WmsUtils.getRegularlySpacedTimeString(threeTimes, ONE_HOUR).toString()
        );
        
        // Now for lots of times, spaced daily
        int n = 10;
        List<DateTime> manyTimes = getRegularlySpacedTimes(EPOCH, ONE_DAY, n);
        String expected = WmsUtils.dateTimeToISO8601(EPOCH) + "/" +
                WmsUtils.dateTimeToISO8601(EPOCH.withDayOfMonth(n)) + "/P1D";
        assertEquals(expected, WmsUtils.getTimeStringForCapabilities(manyTimes));
        
        // Now another test, spaced two-hourly
        DateTime start = EPOCH.withYear(2001).withMonthOfYear(3).withDayOfMonth(3).withHourOfDay(5);
        manyTimes = getRegularlySpacedTimes(start, ONE_HOUR * 2, 5);
        expected = WmsUtils.dateTimeToISO8601(start) + "/" +
                WmsUtils.dateTimeToISO8601(start.withHourOfDay(13)) + "/PT2H";
        assertEquals(expected, WmsUtils.getTimeStringForCapabilities(manyTimes));
    }
    
    private static List<DateTime> getRegularlySpacedTimes(DateTime first, int periodMs, int size)
    {
        List<DateTime> times = new ArrayList<DateTime>(size);
        for (int i = 0; i < size; i++)
        {
            times.add(first.plusMillis(i * periodMs));
        }
        return times;
    }

    /**
     * Tests an irregularly-spaced time list
     */
    @Test
    public void testIrregularTimeStringForCapabilities()
    {
        // A totally-irregular list.  We expect this to be rendered as an explicit
        // list of date-time values.
        List<DateTime> times = new ArrayList<DateTime>();
        StringBuilder expected = new StringBuilder();
        int n = 10;
        for (int i = 0; i < n; i++)
        {
            DateTime dt = i == 0 ? EPOCH : times.get(i - 1).plusMinutes(i);
            times.add(dt);
            expected.append(WmsUtils.dateTimeToISO8601(dt));
            if (i < n - 1) expected.append(",");
        }
        String actual = WmsUtils.getTimeStringForCapabilities(times);
        assertEquals(expected.toString(), actual);
    }
    
    /**
     * Tests a list of times comprised of two regularly-spaced intervals appended
     * together
     */
    @Test
    public void testTwoRegularlySpacedTimeLists()
    {
        DateTime start1 = EPOCH.withYear(2000);
        int n1 = 10;
        DateTime start2 = EPOCH.withYear(2000).withMonthOfYear(2);
        int n2 = 5;
        List<DateTime> times = getRegularlySpacedTimes(start1, ONE_DAY, 10);
        times.addAll(getRegularlySpacedTimes(start2, ONE_HOUR, 5));
        String actual = WmsUtils.getTimeStringForCapabilities(times);
        
        StringBuilder expected = new StringBuilder();
        expected.append(WmsUtils.dateTimeToISO8601(start1) + "/");
        expected.append(WmsUtils.dateTimeToISO8601(start1.withDayOfMonth(n1)) + "/P1D");

        expected.append(",");

        expected.append(WmsUtils.dateTimeToISO8601(start2) + "/");
        expected.append(WmsUtils.dateTimeToISO8601(start2.plusHours(n2 - 1)) + "/PT1H");

        System.out.println(expected);
        System.out.println(actual);
        assertEquals(expected.toString(), actual);
    }

    /**
     * Tests a list of times which would be regularly-spaced except one time is
     * missing
     */
    @Test
    public void testRegularTimesWithMissingValue()
    {
        List<DateTime> times = getRegularlySpacedTimes(EPOCH, ONE_DAY, 20);
        times.remove(10);
        String actual = WmsUtils.getTimeStringForCapabilities(times);
        String expected = WmsUtils.dateTimeToISO8601(EPOCH) + "/" +
            WmsUtils.dateTimeToISO8601(EPOCH.plusDays(9)) + "/P1D," +
            WmsUtils.dateTimeToISO8601(EPOCH.plusDays(11)) + "/" +
            WmsUtils.dateTimeToISO8601(EPOCH.plusDays(19)) + "/P1D";
        System.out.println(expected);
        System.out.println(actual);
        assertEquals(expected, actual);
    }

    /**
     * Tests a list of times that has some regular and some irregular elements
     */
    @Test
    public void testCompoundTimeStringForCapabilities()
    {
        StringBuilder expected = new StringBuilder();
        // Start with a single time
        List<DateTime> times = new ArrayList<DateTime>();
        times.add(EPOCH.withYear(1999));
        expected.append(WmsUtils.dateTimeToISO8601(times.get(0)) + ",");

        // Now some regularly-spaced times
        DateTime start = EPOCH.withYear(1999).withMonthOfYear(4);
        times.addAll(getRegularlySpacedTimes(start, ONE_MINUTE * 5, 30));
        expected.append(WmsUtils.dateTimeToISO8601(start) + "/");
        expected.append(WmsUtils.dateTimeToISO8601(start.plusHours(2).plusMinutes(25)) + "/PT5M");
        
        // Now some irregularly-spaced times
        // A totally-irregular list.  We expect this to be rendered as an explicit
        // list of date-time values.
        for (int i = 0; i < 5; i++)
        {
            DateTime dt = times.get(times.size() - 1).plusMinutes(i);
            times.add(dt);
            expected.append("," + WmsUtils.dateTimeToISO8601(dt));
        }
        
        String actual = WmsUtils.getTimeStringForCapabilities(times);
        System.out.println(expected);
        System.out.println(actual);
        assertEquals(expected.toString(), actual);
    }
}
