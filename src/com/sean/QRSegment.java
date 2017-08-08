package com.sean;

import java.util.List;

public class QRSegment {
    public Object numChars;
    public Object mode;

    public static List<QRSegment> makeSegments(String text) {
    }

    public static QRSegment makeBytes(byte[] data) {
    }

    public static int getTotalBits(List<QRSegment> segments, int version) {
        return 0;
    }
}
