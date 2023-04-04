package committee.nova.mwdmgsrcfix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

public class DamageSourceModular extends EntityDamageSource {
    private final boolean headShot;
    private final String gunName;

    public DamageSourceModular(Entity source, String gunName, boolean headShot) {
        super("modular", source);
        this.gunName = gunName;
        this.headShot = headShot;
    }

    @Override
    @Nonnull
    public ITextComponent getDeathMessage(@Nonnull EntityLivingBase entityLivingBaseIn) {
        String label = "death.attack." + getDamageType();
        if (damageSourceEntity == null) {
            return new TextComponentTranslation(label + ".generic", entityLivingBaseIn.getName());
        }
        if (headShot) {
            return new TextComponentTranslation(label + ".headshot", damageSourceEntity.getName(), entityLivingBaseIn.getName(), gunName);
        }
        return new TextComponentTranslation(label, damageSourceEntity.getName(), entityLivingBaseIn.getName(), gunName);
    }
}