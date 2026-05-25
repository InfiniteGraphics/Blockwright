package top.huliawsl.blockwright.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class PcgEditorViewportNavigator {
    private PcgEditorViewportNavigator() {
    }

    public static void tick(Minecraft minecraft, PcgEditorSession session) {
        if (!session.isOpen() || !session.isNavigating() || minecraft.player == null || minecraft.screen == null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        double speed = minecraft.options.keySprint.isDown() ? 1.2D : 0.45D;
        Vec3 forward = player.getLookAngle();
        Vec3 flatForward = new Vec3(forward.x, 0.0D, forward.z);
        if (flatForward.lengthSqr() < 1.0E-6D) {
            flatForward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flatForward = flatForward.normalize();
        }
        Vec3 right = new Vec3(-flatForward.z, 0.0D, flatForward.x);
        Vec3 movement = Vec3.ZERO;

        if (minecraft.options.keyUp.isDown()) {
            movement = movement.add(flatForward.scale(speed));
        }
        if (minecraft.options.keyDown.isDown()) {
            movement = movement.subtract(flatForward.scale(speed));
        }
        if (minecraft.options.keyLeft.isDown()) {
            movement = movement.subtract(right.scale(speed));
        }
        if (minecraft.options.keyRight.isDown()) {
            movement = movement.add(right.scale(speed));
        }
        if (minecraft.options.keyJump.isDown()) {
            movement = movement.add(0.0D, speed, 0.0D);
        }
        if (minecraft.options.keyShift.isDown()) {
            movement = movement.add(0.0D, -speed, 0.0D);
        }
        if (movement.lengthSqr() == 0.0D) {
            return;
        }

        player.move(MoverType.SELF, movement);
        player.setDeltaMovement(Vec3.ZERO);
    }
}
