package com.zhongfei.scheduler.utils;

/**
 * @Auther: yuli
 * @Date: 2019/7/18 17:10
 * @Description:
 */
public class IDGenerator {
    private static final long EPOCH = 1514736000000L;
    private static final long SEQ_BITS = 12L;
    private static final long INSTANCE_BITS = 10L;
    private static final long TIMESTAMP_BITS = 41L;
    private static final long INSTANCE_SHIFT = SEQ_BITS;
    private static final long TIMESTAMP_SHIFT = SEQ_BITS + INSTANCE_BITS;
    private static final long SEQ_MASK = ~(-1L << SEQ_BITS);
    private static final long INSTANCE_MASK = ~(-1L << INSTANCE_BITS);
    private static final long TIMESTAMP_MASK = ~(-1L << TIMESTAMP_BITS);
    private static final long INSTANCE = maskValue(System.currentTimeMillis(), INSTANCE_MASK, INSTANCE_SHIFT);

    private static long seq = 0L;
    private static long lastTimestamp = 0L;

    public static long next() {
        long timestamp = getTimestamp();
        if (lastTimestamp == timestamp) {
            seq = maskValue(seq + 1, SEQ_MASK, 0L);
            if (seq == 0L) {
                timestamp = tillNext(lastTimestamp);
            }
        } else {
            seq = 0L;
        }
        lastTimestamp = timestamp;
        long timePart = maskValue(timestamp, TIMESTAMP_MASK, TIMESTAMP_SHIFT);
        return timePart | INSTANCE | seq;
    }

    private static long tillNext(long lastTimestamp) {
        long timestamp = getTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getTimestamp();
        }
        return timestamp;
    }

    private static long getTimestamp() {
        return System.currentTimeMillis() - EPOCH;
    }

    private static long maskValue(long value, long mask, long shift) {
        return (value & mask) << shift;
    }
}
