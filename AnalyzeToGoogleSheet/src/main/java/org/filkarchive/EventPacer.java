package org.filkarchive;

import java.time.*;
import java.util.concurrent.*;

public class EventPacer
{
    private final Duration interval;
    private final Clock clock;
    private Instant lastEvent = Instant.MIN;

    public EventPacer(Duration interval)
    {
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
    }
}
