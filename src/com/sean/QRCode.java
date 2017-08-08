package com.sean;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class QRCode {

    /* ------ Instance Fields ------ */

    // This QR Codes version number [1, 40]
    public final int version;

    // Width and height of QR Code, measured in modules
    public final int size;

    // The Error Correction Level. Never null
    public final Ecc errorCorrectionLevel;

    // The mask pattern used in this QR Code [0, 7]
    public final int mask;

    // Grids of modules/pixels
    private boolean[][] modules;
    private boolean[][] isFunction;

    /* ------ Constructors ------ */
    // TODO: Add constructors


    public QRCode(int version, int size, Ecc errorCorrectionLevel, int mask) {
        this.version = version;
        this.size = size;
        this.errorCorrectionLevel = errorCorrectionLevel;
        this.mask = mask;
    }

    @NotNull
    public static QRCode encodeText(String text, Ecc ecl) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(ecl);
        List<QRSegment> segments = QRSegment.makeSegments(text);
        return encodeSegments(segments, ecl);
    }

    @NotNull
    public static QRCode encodeBinary(byte[] data, Ecc ecl) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(ecl);
        QRSegment segment = QRSegment.makeBytes(data);
        return encodeSegments(Arrays.asList(segment), ecl);
    }

    @NotNull
    private static QRCode encodeSegments(List<QRSegment> segments, Ecc ecl) {
        return encodeSegments(segments, ecl, 1, 40, -1, true);
    }

    @NotNull
    private static QRCode encodeSegments(List<QRSegment> segments, Ecc ecl, int minVersion, int maxVersion, int mask, boolean boostEcl) {
        Objects.requireNonNull(segments);
        Objects.requireNonNull(ecl);
        if (!(1 <= minVersion && minVersion <= maxVersion && maxVersion <= 40) || mask < -1 || mask > 7) {
            throw new IllegalArgumentException("Invalid Value");
        }

        // Find minimal version number (more data to encode --> higher version
        int version, dataUsedBits;
        for (version = minVersion; ; version++) {
            int dataCapacityBits = getNumDataCodewords(version, ecl) * 8; // Available data bits
            dataUsedBits = QRSegment.getTotalBits(segments, version);
            if (dataUsedBits != -1 && dataUsedBits <= dataCapacityBits){
                break; // This version is suitable for the amount of data
            }
            if (version >= maxVersion) { // Data does not fit in any version
                throw new IllegalArgumentException("Data too long!");
            }
        }
        if (dataUsedBits == -1) {
            throw new AssertionError();
        }

        // Increase error correction level up until the data does not fit in current version anymore.
        for (Ecc newEcl : Ecc.values()) {
            if (boostEcl && dataUsedBits <= getNumDataCodewords(version, newEcl) * 8){
                ecl = newEcl;
            }
        }

        // Create data bit string by concatinating all segments
        int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
        BitBuffer bb = new BitBuffer();
        for (QRSegment segment : segments) {
            bb.appendBits(segment.mode.modeBits, 4);
            bb.appendBits(segment.numChars, segment.mode.numCharCountBits(version));
            bb.appendData(segment);
        }

        // Add terminator and pad up to a byte
        bb.appendBits(0, Math.min(4, dataCapacityBits - bb.bitLength()));
        bb.appendBits(0, (8 - bb.bitLength() % 8) % 8);

        // Pad with alternate bytes until data capacity is reached
        for (int padByte = 0xEC; bb.bitLength() < dataCapacityBits; padByte ^= 0xEC ^ 0x11)
            bb.appendBits(padByte, 8);
        if (bb.bitLength() % 8 != 0)
            throw new AssertionError();

        // Create the QR Code symbol
        return new QRCode(version, ecl, bb.getBytes(), mask);
    }

    private static int getNumDataCodewords(int version, Ecc ecl) {
        return 0;
    }


    /**
     * Enum to represent the error correction level used in the QR Code (0-3)
     */
    public enum Ecc {
        // Have to be declared in ascending order of error protection.
        LOW(1), MEDIUM(0), QUARTILE(3), HIGH(2);

        // Range 0-3
        final int formatBits;

        //Constructor
        private Ecc(int fb) {
            formatBits = fb;
        }

    }
}
