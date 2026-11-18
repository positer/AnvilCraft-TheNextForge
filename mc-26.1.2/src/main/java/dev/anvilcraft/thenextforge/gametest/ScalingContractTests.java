package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ModelScaling;
import com.mojang.math.Transformation;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ScalingContractTests {
    public static void displaySingleFactor(GameTestHelper helper) {
        for (float stored : new float[] {1, 1.2F, 1.5F, 3}) {
            var pose = new Transformation(new Vector3f(0, 0.12F, 0), new Quaternionf(),
                new Vector3f(stored), new Quaternionf().rotateX((float)Math.PI / 2));
            float correction = ModelScaling.displayCorrection(pose);
            helper.assertTrue(Math.abs(stored * correction * 1.5F - 1.5F) < 0.001F, "展示只能缩放一次");
            helper.assertTrue(Math.abs(stored * correction - 1) < 0.001F, "关闭后旧展示倍率也必须抵消");
        }
        var foreign = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(2), new Quaternionf());
        helper.assertTrue(ModelScaling.displayCorrection(foreign) == 1, "其他展示物自定义变换不得被归一");
        var nonUniform = new Transformation(new Vector3f(0, 0.12F, 0), new Quaternionf(),
            new Vector3f(1, 2, 3), new Quaternionf().rotateX((float)Math.PI / 2));
        helper.assertTrue(ModelScaling.displayCorrection(nonUniform) == 1, "非统一模型缩放不得被归一");
        helper.succeed();
    }
}
