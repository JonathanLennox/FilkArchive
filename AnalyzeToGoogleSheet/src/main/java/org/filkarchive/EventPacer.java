package org.filkarchive;

import java.time.*;
import java.util.concurrent.*;

public class EventPacer
{
    private final String label;
    private final Duration interval;
    private final Clock clock;
    private Instant lastEvent = Instant.MIN;
    private int count = 0;

    public EventPacer(String label, Duration interval)
    {
        this.label = label;
        this.interval = interval;
        this.clock = Clock.systemUTC();
    }

    public void pace() throws java.lang.InterruptedException
    {
        Instant now = clock.instant();
        Duration timeSinceLastEvent = Duration.between(lastEvent, now);

        if (timeSinceLastEvent.compareTo(interval) < 0)
        {
            Duration remaining = interval.minus(timeSinceLastEvent);
            TimeUnit.NANOSECONDS.sleep(remaining.toNanos());
            now = clock.instant();
        }

        lastEvent = now;
        count++;
        System.out.printf("\tpacer %s: %s: %d\n", label, now, count);
    }
}
