/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.ncwms.utils;

import java.text.DateFormat;
import java.util.Date;
import ucar.nc2.units.DateFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

/**
 * <p>Collection of static utility methods that are useful in the WMS application.</p>
 *
 * <p>Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, these functions
 * are also available as JSP2.0 functions. For example:</p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * The epoch: ${utils:secondsToISO8601(0)}
 * </code>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class WmsUtils
{
    /**
     * The version of the WMS standard that this server supports
     * @todo Support more versions (e.g. 1.1.1)?
     */
    public static final String VERSION = "1.3.0";
    
    /**
     * Time zone representing Greenwich Mean Time
     */
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT+0");

    /**
     * Converts a number of milliseconds since the epoch into an ISO8601-formatted
     * String.
     */
    public static String millisecondsToISO8601(long millisecondsSinceEpoch)
    {
        return dateToISO8601(new Date(millisecondsSinceEpoch));
    }

    /**
     * Converts a Date object into an ISO8601-formatted String.
     */
    public static String dateToISO8601(Date date)
    {
        return new DateFormatter().toDateTimeStringISO(date);
    }

    /**
     * Converts an ISO8601-formatted String into a Date
     */
    public static Date iso8601ToDate(String isoDateTime)
    {
        return new DateFormatter().getISODate(isoDateTime);
    }
    
    /**
     * Converts an ISO8601-formatted time into a number of milliseconds since the
     * epoch
     * @todo: shouldn't this throw a parse error?
     */
    public static long iso8601ToMilliseconds(String isoDateTime)
    {
        return iso8601ToDate(isoDateTime).getTime();
    }
        
    /**
     * @return the version of WMS that this server supports (equal to the VERSION
     * field but wrapped as a function to support the creation of JSP tags.
     */
    public static final String getVersion()
    {
        return VERSION;
    }
    
    /**
     * @return a heading (e.g. "Oct 2006") for the given date, which is 
     * expressed in milliseconds since the epoch.  Used by showCalendar.jsp.
     */
    public static String getCalendarHeading(long millisecondsSinceEpoch)
    {
        DateFormat df = new SimpleDateFormat("MMM yyyy");
        // Must set the time zone to avoid problems with daylight saving
        df.setTimeZone(GMT);
        return df.format(new Date(millisecondsSinceEpoch));
    }
    
    /**
     * @return an ISO8601-formatted date that is exactly one year earlier than
     * the given date, in milliseconds since the epoch.
     */
    public static String getYearBefore(long millisecondsSinceEpoch)
    {
        Calendar cal = getCalendar(millisecondsSinceEpoch);
        cal.add(Calendar.YEAR, -1);
        return dateToISO8601(cal.getTime());
    }
    
    /**
     * @return a new Calendar object, set to the given time (in milliseconds
     * since the epoch).
     */
    public static Calendar getCalendar(long millisecondsSinceEpoch)
    {
        Date date = new Date(millisecondsSinceEpoch);
        Calendar cal = Calendar.getInstance();
        // Must set the time zone to avoid problems with daylight saving
        cal.setTimeZone(GMT);
        cal.setTime(date);
        return cal;
    }
    
    /**
     * @return an ISO8601-formatted date that is exactly one year later than
     * the given date, in milliseconds since the epoch.
     */
    public static String getYearAfter(long millisecondsSinceEpoch)
    {
        Calendar cal = getCalendar(millisecondsSinceEpoch);
        cal.add(Calendar.YEAR, 1);
        return dateToISO8601(cal.getTime());
    }
    
    /**
     * @return an ISO8601-formatted date that is exactly one month earlier than
     * the given date, in milliseconds since the epoch.
     */
    public static String getMonthBefore(long millisecondsSinceEpoch)
    {
        Calendar cal = getCalendar(millisecondsSinceEpoch);
        cal.add(Calendar.MONTH, -1);
        return dateToISO8601(cal.getTime());
    }
    
    /**
     * @return an ISO8601-formatted date that is exactly one month later than
     * the given date, in milliseconds since the epoch.
     */
    public static String getMonthAfter(long millisecondsSinceEpoch)
    {
        Calendar cal = getCalendar(millisecondsSinceEpoch);
        cal.add(Calendar.MONTH, 1);
        return dateToISO8601(cal.getTime());
    }
    
    /**
     * Formats a date (in milliseconds since the epoch) as human-readable "dd MMM yyyy",
     * e.g. "02 Jul 2007".
     */
    public static String formatPrettyDate(long millisecondsSinceEpoch)
    {
        DateFormat df = new SimpleDateFormat("dd MMM yyyy");
        // Must set the time zone to avoid problems with daylight saving
        df.setTimeZone(GMT);
        return df.format(new Date(millisecondsSinceEpoch));
    }
    
    /**
     * Formats a date (in milliseconds since the epoch) as human-readable "HH:mm:ss",
     * e.g. "14:53:03".
     */
    public static String formatPrettyTime(long millisecondsSinceEpoch)
    {
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        // Must set the time zone to avoid problems with daylight saving
        df.setTimeZone(GMT);
        return df.format(new Date(millisecondsSinceEpoch));
    }
    
    /**
     * <p>@return a calendar representation of the month that contains the date
     * represented by the given number of milliseconds since the epoch.  Each item
     * in the returned List represents a week in the calendar (starting on a
     * Monday).  Each week is represented by an array of 7 DayInfo objects, giving the
     * day number on each day of that week and the index of the corresponding point
     * along the time axis.  If a day does not belong in the
     * calendar for that month, its value will be null.  If there is no corresponding
     * time axis value for a given day, the t index will be set to -1.</p>
     *
     * <p>For example, for a date in March 2007, this will return a calendar
     * of the form:<p>
     * <table border="1">
     * <tr><td>null</td><td>null</td><td>null</td><td>1</td><td>2</td><td>3</td><td>4</td></tr>
     * <tr><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td><td>11</td></tr>
     * <tr><td>12</td><td>13</td><td>14</td><td>15</td><td>16</td><td>17</td><td>18</td></tr>
     * <tr><td>19</td><td>20</td><td>21</td><td>22</td><td>23</td><td>24</td><td>25</td></tr>
     * <tr><td>26</td><td>27</td><td>28</td><td>29</td><td>30</td><td>31</td><td>null</td></tr>
     * </table>
     */
    public static List<DayInfo[]> getMonthCalendar(long targetTime,
        long[] axisValues)
    {
        final int DAYS_IN_WEEK = 7;
        List<DayInfo[]> weeks = new ArrayList<DayInfo[]>();
        Calendar cal = getCalendar(targetTime);
        
        // Find the first Monday of the month
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        // If this isn't the first day of the month then we have a partial first week
        if (day != 1) day -= DAYS_IN_WEEK; // Start with the week before the first Monday
        
        // Construct the weeks
        int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int tIndex = 0; // Index in the array of t values
        DayComparator dayComparator = new DayComparator();
        while (day < lastDayOfMonth)
        {
            DayInfo[] week = new DayInfo[DAYS_IN_WEEK];
            int firstDayNextWeek = day + DAYS_IN_WEEK;
            for (int i = 0; i < DAYS_IN_WEEK; i++)
            {
                if (day >= 1 && day <= lastDayOfMonth)
                {
                    // Get the current day (in milliseconds since the epoch)
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    long currentDay = cal.getTimeInMillis();
                    // Look to see if we have any data on this day
                    boolean found = false;
                    while (!found && tIndex < axisValues.length)
                    {
                        int result = dayComparator.compare(axisValues[tIndex],
                            currentDay);
                        if (result == 0) found = true; // Found data on the target day
                        else if (result < 0) tIndex++; // Move to the next day
                        else break; // Date on axis is after target day: no point searching further
                    }
                    if (found) week[i] = new DayInfo(day, tIndex);
                    else week[i] = new DayInfo(day, -1);
                }
                else
                {
                    // This day is not present in the current month
                    week[i] = null;
                }
                day++;
            }
            weeks.add(week);
        }
        
        return weeks;
    }
    
    /**
     * Simple class containing information about a particular day on the calendar
     * that is returned by getMonthCalendar()
     */
    public static class DayInfo
    {
        private int dayNumber; // Number of day in the given month
        private int tIndex; // time index of a data point on this day
        
        public DayInfo(int dayNumber, int tIndex)
        {
            this.dayNumber = dayNumber;
            this.tIndex = tIndex;
        }

        public int getDayNumber()
        {
            return dayNumber;
        }

        public int getTindex()
        {
            return tIndex;
        }
    }
    
    /**
     * Compares two dates (expressed in milliseconds since the epoch) to see if they
     * fall on the same day or different days (ignoring hours, minutes and seconds)
     */
    public static class DayComparator implements Comparator<Long>
    {
        /**
         * Compares two dates (expressed in milliseconds since the epoch), returning
         * 0 if they are on the same day, <0 if the first date is on a day before
         * that of the second date or >0 if the first date is on a day after the
         * second date.
         */
        public int compare(Long d1, Long d2)
        {
            Calendar cal1 = getCalendar(d1);
            Calendar cal2 = getCalendar(d2);
            // Set hours, minutes, seconds and milliseconds to zero for both
            // calendars
            resetHoursEtc(cal1);
            resetHoursEtc(cal2);
            // Now we know that any differences are due to the day, month or year
            return cal1.compareTo(cal2);
        }
        
        /**
         * zeros out the hours, minutes, seconds and milliseconds fields of
         * the given Calendar
         */
        private static void resetHoursEtc(Calendar cal)
        {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
    }
    
}
