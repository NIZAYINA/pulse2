package pulsefx;

/** 7 custom skies + vanilla. Colours are 0xRRGGBB. */
public final class SkyPresets {
    public static final String[] NAMES = {
        "Vanilla", "Pink Sunset", "Neon Night", "Aurora", "Lavender Dawn", "Deep Ocean", "Blood Moon", "Golden Hour"
    };
    public static final int[] TOP = {
        0x78A7FF, 0x5B2A86, 0x0B0720, 0x041A24, 0x9B8CFF, 0x03213F, 0x1A0003, 0xF28C28
    };
    public static final int[] HORIZON = {
        0xBFD8FF, 0xFF7EB3, 0x6A2BD9, 0x2FE6A7, 0xFFD6E8, 0x2A9DBF, 0xB3121B, 0xFFE8A3
    };
    public static final boolean[] STARS = {false, false, true, true, false, true, true, false};

    private SkyPresets() {}
}
