package org.swelement.core;

import java.awt.*;

public final class PopupPositioner {
    public static final class Result {
        public final Point location;
        public final AnimatedPopup.Direction actualDirection;
        public Result(Point l, AnimatedPopup.Direction a) { location = l; actualDirection = a; }
    }
    private final Dimension popupSize;
    private final Rectangle screenBounds;
    private static final int MARGIN = 8;

    public PopupPositioner(Dimension popupSize, Rectangle screenBounds) {
        this.popupSize = popupSize;
        this.screenBounds = screenBounds;
    }

    public Result calc(Rectangle invoker, AnimatedPopup.Direction preferred) {
        int px = 0, py = 0;
        AnimatedPopup.Direction actual = preferred;
        switch (preferred) {
            case ABOVE:
                px = invoker.x + invoker.width / 2 - popupSize.width / 2;
                py = invoker.y - popupSize.height - MARGIN;
                break;
            case BELOW:
                px = invoker.x + invoker.width / 2 - popupSize.width / 2;
                py = invoker.y + invoker.height + MARGIN;
                break;
            case LEFT:
                px = invoker.x - popupSize.width - MARGIN;
                py = invoker.y + invoker.height / 2 - popupSize.height / 2;
                break;
            case RIGHT:
                px = invoker.x + invoker.width + MARGIN;
                py = invoker.y + invoker.height / 2 - popupSize.height / 2;
                break;
            case TOP_CENTER:
                px = screenBounds.x + screenBounds.width / 2 - popupSize.width / 2;
                py = screenBounds.y + 20;
                return clampAndReturn(px, py, actual);
            case BOTTOM_RIGHT_CORNER:
                px = screenBounds.x + screenBounds.width - popupSize.width - 40;
                py = screenBounds.y + screenBounds.height - popupSize.height - 80;
                return clampAndReturn(px, py, actual);
        }
        boolean flip = false;
        switch (preferred) {
            case ABOVE: if (py < screenBounds.y) flip = true; break;
            case BELOW: if (py + popupSize.height > screenBounds.y + screenBounds.height) flip = true; break;
            case LEFT:  if (px < screenBounds.x) flip = true; break;
            case RIGHT: if (px + popupSize.width > screenBounds.x + screenBounds.width) flip = true; break;
        }
        if (flip) {
            switch (preferred) {
                case ABOVE: actual = AnimatedPopup.Direction.BELOW;
                    py = invoker.y + invoker.height + MARGIN; break;
                case BELOW: actual = AnimatedPopup.Direction.ABOVE;
                    py = invoker.y - popupSize.height - MARGIN; break;
                case LEFT:  actual = AnimatedPopup.Direction.RIGHT;
                    px = invoker.x + invoker.width + MARGIN; break;
                case RIGHT: actual = AnimatedPopup.Direction.LEFT;
                    px = invoker.x - popupSize.width - MARGIN; break;
            }
        }
        return clampAndReturn(px, py, actual);
    }

    private Result clampAndReturn(int px, int py, AnimatedPopup.Direction actual) {
        px = Math.max(screenBounds.x + MARGIN, Math.min(px, screenBounds.x + screenBounds.width - popupSize.width - MARGIN));
        py = Math.max(screenBounds.y + MARGIN, Math.min(py, screenBounds.y + screenBounds.height - popupSize.height - MARGIN));
        return new Result(new Point(px, py), actual);
    }

    static void selfCheck() {
        Rectangle screen = new Rectangle(0, 0, 1920, 1080);
        PopupPositioner pp = new PopupPositioner(new Dimension(200, 100), screen);
        Result r = pp.calc(new Rectangle(1000, 10, 100, 30), AnimatedPopup.Direction.BELOW);
        assert r.location.y >= 10 + 30 : "below placement";
        r = pp.calc(new Rectangle(1000, 1070, 100, 30), AnimatedPopup.Direction.BELOW);
        assert r.actualDirection == AnimatedPopup.Direction.ABOVE : "overflow flip";
        assert r.location.y + 100 <= 1070 : "above placed above invoker bottom";
        r = pp.calc(new Rectangle(0,0,1,1), AnimatedPopup.Direction.TOP_CENTER);
        assert r.location.x == 1920/2 - 100 : "top center x";
        assert r.location.y == screen.y + 20 : "top center y";
        System.out.println("PopupPositioner self-check OK");
    }
    public static void main(String[] args) { selfCheck(); }
}
