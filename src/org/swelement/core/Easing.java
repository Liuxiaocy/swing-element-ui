package org.swelement.core;

public interface Easing {
    float apply(float t);

    static float linear(float t) { return t; }
    static float easeIn(float t) { return t * t * t; }
    static float easeOut(float t) { return 1f - (float) Math.pow(1 - t, 3); }
    static float easeInOut(float t) { return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f; }

    static void selfCheck() {
        for (Easing e : new Easing[]{Easing::linear, Easing::easeIn, Easing::easeOut, Easing::easeInOut}) {
            float prev = -1f;
            for (int i = 0; i <= 100; i++) {
                float v = e.apply(i / 100f);
                assert v >= 0f && v <= 1f : e + " out of range at " + i;
                assert v >= prev - 1e-6f : e + " not monotonic at " + i;
                prev = v;
            }
            assert Math.abs(e.apply(0f)) < 1e-4f : e + " apply(0) != 0";
            assert Math.abs(e.apply(1f) - 1f) < 1e-4f : e + " apply(1) != 1";
        }
        System.out.println("Easing self-check OK");
    }

    static void main(String[] args) { selfCheck(); }
}
