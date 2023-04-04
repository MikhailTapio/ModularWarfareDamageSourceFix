package committee.nova.mwdmgsrcfix;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = ModularWarfareDamageSourceFix.MOD_ID, useMetadata = true)
public class ModularWarfareDamageSourceFix {
    public static final String MOD_ID = "mwdmgsrcfix";

    public static DamageSourceModular causeModularDmg(Entity shooter, String gunName, boolean headShot) {
        return new DamageSourceModular(shooter, gunName, headShot);
    }
}
