package org.swelement.core;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Core animation engine: linear + easing interpolation with optional completion callback.
 * Uses javax.swing.Timer — updates fire on EDT.
 */
public class Animator {
    private final int durationMs;
    private final Easing easing;
    private final Listener listener;
    private final Timer timer;
    private float from;
    private float to;
    private long startTime;
    private Runnable onComplete; // may be null

    public Animator(int durationMs, Easing easing, Listener listener) {
        this.durationMs = durationMs;
        this.easing = easing;
        this.listener = listener;
        this.timer = new Timer(16, null);
        this.timer.setCoalesce(true);
    }

    /** Start a new animation from value → value. Cancels any in-flight animation, clears any prior onComplete. */
    public void go(float from, float to) {
        stop();            // cancels prior + clears pending onComplete (via stop clearing)
        this.onComplete = null; // explicitly clear completion callback registered by a previous 3-arg call
        this.from = from;
        this.to = to;
        startTime = System.currentTimeMillis();
        ActionListener tick = new ActionListener() {
            public void actionPerformed(ActionEvent e) { tick(); }
        };
        for (ActionListener al : timer.getActionListeners()) timer.removeActionListener(al);
        timer.addActionListener(tick);
        timer.start();
        listener.update(from);
    }

    /** Start a new animation (delegates to go(from,to)) then registers completion callback.
     *  onComplete runs once on EDT when animation reaches end. Cancelled animations never trigger onComplete. */
    public void go(float from, float to, Runnable onComplete) {
        go(from, to);
        this.onComplete = onComplete; // 2-arg cleared any prior, set fresh after
    }

    /** Cancel current animation if running; onComplete not invoked. Safe to call multiple times. */
    public void stop() {
        timer.stop();
        // Clear action listeners — no stray tick firing after stop
        for (ActionListener al : timer.getActionListeners()) timer.removeActionListener(al);
        // Clear pending onComplete so a subsequent start doesn't inherit stale callback
        this.onComplete = null;
    }

    private void tick() {
        long elapsed = System.currentTimeMillis() - startTime;
        float t = Math.min(1f, (float) elapsed / (float) Math.max(1, durationMs));
        float eased = easing.apply(t);
        float v = from + (to - from) * eased;
        listener.update(v);
        if (t >= 1f) {
            timer.stop();
            Runnable cb = this.onComplete;
            this.onComplete = null;
            if (cb != null) cb.run();
        }
    }

    public interface Listener { void update(float value); }
}
