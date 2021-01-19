package org.filkarchive;

import com.google.api.client.googleapis.json.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class ExponentialBackoff
{
    private static final int MAX_ATTEMPTS = 8;

    public static <T> T execute(ExponentialBackoffFunction<T> fn)
        throws IOException, InterruptedException
    {
        Duration t = Duration.ofSeconds(1);

        GoogleJsonResponseException exc = null;
        String desc = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++)
        {
            try {
                T ret = fn.execute();
                if (attempt > 0)
                {
                    System.out.println("ExponentialBackoff: ok");
                }
                return ret;
            }
            catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() != 429) {
                    throw e;
                }
                String newDesc = e.getMessage();
                if (!Objects.equals(desc, newDesc))
                {
                    System.out.println(
                        "Got " + e.getClass().getSimpleName() + ": " + e
                            .getMessage());
                }
                desc = newDesc;
                System.out.println("ExponentialBackoff: sleeping " + t.toString());
                TimeUnit.NANOSECONDS.sleep(t.toNanos());
                t = t.multipliedBy(2);
                exc = e;
            }
        }
        throw exc;
    }

    public interface ExponentialBackoffFunction<T>
    {
        T execute()
            throws IOException;
    }
}
