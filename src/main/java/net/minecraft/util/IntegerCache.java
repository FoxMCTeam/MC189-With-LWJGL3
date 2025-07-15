package net.minecraft.util;

public class IntegerCache
{
    private static final Integer[] CACHE = new Integer[65535];

    public static Integer getInteger(int p_181756_0_)
    {
        return p_181756_0_ >= 0 && p_181756_0_ < CACHE.length ? CACHE[p_181756_0_] : new Integer(p_181756_0_);
    }

    static
    {
        int i = 0;

        for (int j = CACHE.length; i < j; ++i)
        {
            CACHE[i] = Integer.valueOf(i);
        }
    }
}
