package org.swelement.core;

import javax.swing.Timer;

public final class Animator {
    public interface Listener { void update(float v); }

    private final Timer timer;
    private final long duration;
    private final Easing easing;
    private final Listener listener;
    private float from, to;
    private long start;
    private Runnable onComplete;

    public Animator(int durationMs, Easing easing, Listener listener) {
        this.duration = durationMs;
        this.easing = easing;
        this.listener = listener;
        this.timer = new Timer(15, e -> tick());
    }

    public void go(float from, float to) {
        go(from, to, null);
    }

    public void go(float from, float to, Runnable onComplete) {
        this.from = from;
        this.to = to;
        this.start = System.currentTimeMillis();
        this.onComplete = onComplete;
        this.timer.start();
    }

    public void stop() { timer.stop(); }

    public boolean running() { return timer.isRunning(); }

    private void tick() {
        float p = (System.currentTimeMillis() - start) / (float) duration;
        boolean done = p >= 1f;
        if (done) { p = 1f; timer.stop(); }
        listener.update(from + (to - from) * easing.apply(p));
        if (done && onComplete != null) {
            Runnable cb = onComplete;
            onComplete = null;
            cb.run();
        }
    }

    public static void main(String[] args) throws Exception {
        final float[] last = {-1f};
        Animator a = new Animator(40, Easing::linear, v -> last[0] = v);
        a.go(0f, 1f);
        Thread.sleep(200);
        assert a.running() == false : "animator should have stopped";
        assert Math.abs(last[0] - 1f) < 0.001f : "did not reach target: " + last[0];

        a.go(5f, 0f);
        Thread.sleep(200);
        assert Math.abs(last[0] - 0f) < 0.001f : "did not animate to 0: " + last[0];

        float mid = last[0];
        a.go(0f, 1f);
        Thread.sleep(30);
        a.go(last[0], 0f);   // 中断重定向：从当前值反向
        Thread.sleep(200);
        assert Math.abs(last[0] - 0f) < 0.001f : "interrupt re-target failed: " + last[0];

        System.out.println("Animator self-check OK");
    }
}