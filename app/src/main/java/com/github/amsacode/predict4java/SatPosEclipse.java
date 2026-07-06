package com.github.amsacode.predict4java;

/**
 * predict4java'nın SatPos sınıfındaki eclipse (gölge) bilgisi protected
 * olduğu için aynı paketten erişim sağlayan yardımcı sınıf.
 * Uydu Dünya'nın gölgesindeyse güneş ışığı yansıtamaz ve görünmez olur.
 */
public final class SatPosEclipse {

    private SatPosEclipse() {
    }

    public static boolean isEclipsed(SatPos pos) {
        return pos.isEclipsed();
    }
}
