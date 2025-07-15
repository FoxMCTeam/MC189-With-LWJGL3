package lol.nightmaple.utils.objects;

import org.lwjglx.input.Mouse;

public class ScrollUtils {
    public static float yOffset;
    public static float speed = 120.0F;

    public static void run() {
        Mouse.addWheelEvent(scrollSpeed(yOffset, speed));
    }

    public static float scrollSpeed(float yOffset, float speed) {
        return yOffset * speed;
    }
}
