package io.github.hello09x.fakeplayer.core.util;

public class Mth {

    /**
     * <ul>
     *     <li>3.0, 0.5 -> 3.0</li>
     *     <li>3.1, 0.5 -> 3.0</li>
     *     <li>3.6, 0.5 -> 3.5</li>
     * </ul>
     *
     */
    public static double floor(double num, double base) {
        if (num % base == 0) {
            return num;
        }
        return Math.floor(num / base) * base;
    }

    /**
     *
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }


}
