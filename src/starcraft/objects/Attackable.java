package starcraft.objects;

import java.awt.*;

public interface Attackable {
    double getTargetX();
    double getTargetY();
    int getHp();
    int getTeam();

    default boolean isAlive() {
        return getHp() > 0;
    }

    void receiveDamage(int amount);

    default void triggerHitEffect(Color color, int style, int duration) {
    }
}
