package au.org.ala.util;

import java.util.Date;

/**
 * Management bean for counters to allow JMX access
 */
public interface CounterMXBean {
    /**
     * Get the start time for the counter.
     *
     * @retyrn The time the counter started
     */
    public Date getStartTime();
    /**
     * Get the stop time for the counter.
     *
     * @retyrn The time the counter stapped
     */
    public Date getStopTime();
    /**
     * Get the current rate for the counter.
     * <p>
     * If the counter is stopped, return the total rate.
     * </p>
     *
     * @return The number of counts per second in the current segment
     */
    public double getCurrentRate();
    /**
     * Get the total count.
     *
     * @return The count
     */
    public int getCount();

}
