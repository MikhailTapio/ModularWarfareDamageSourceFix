package committee.nova.mwdmgsrcfix.mixin;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.capability.extraslots.CapabilityExtra;
import com.modularwarfare.common.capability.extraslots.IExtraItemHandler;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.guns.manager.ShotValidation;
import com.modularwarfare.common.network.PacketExpGunFire;
import com.modularwarfare.common.network.PacketPlayHitmarker;
import com.modularwarfare.common.network.PacketPlaySound;
import com.modularwarfare.common.network.PacketPlayerHit;
import com.modularwarfare.utility.RayUtil;
import committee.nova.mwdmgsrcfix.DamageSourceModular;
import committee.nova.mwdmgsrcfix.ModularWarfareDamageSourceFix;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = PacketExpGunFire.class, remap = false)
public class MixinPacketExpGunFire {
    @Shadow
    public String internalname;

    @Shadow
    public int entityId;

    @Shadow
    public String hitboxType;

    @Shadow
    public int fireTickDelay;

    @Shadow
    public float recoilPitch;

    @Shadow
    public float recoilYaw;

    @Shadow
    public float bulletSpread;

    @Shadow
    public float recoilAimReducer;

    @Shadow
    private double posX;

    @Shadow
    private double posY;

    @Shadow
    private double posZ;

    @Shadow
    private EnumFacing facing;

    @Inject(method = "handleServerSide", at = @At("HEAD"), cancellable = true, remap = false)
    private void redirect$handleServerSide(EntityPlayerMP entityPlayer, CallbackInfo ci) {
        ci.cancel();
        IThreadListener mainThread = (WorldServer) entityPlayer.world;
        mainThread.addScheduledTask(() -> {
            entityPlayer.getHeldItemMainhand();
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun && ModularWarfare.gunTypes.get(internalname) != null) {
                ItemGun itemGun = ModularWarfare.gunTypes.get(internalname);
                if (entityPlayer.getHeldItemMainhand().getItem() != itemGun) return;
                if ((float) entityPlayer.ping > ModConfig.INSTANCE.shots.maxShooterPing) {
                    entityPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "[" + TextFormatting.RED + "ModularWarfare" + TextFormatting.GRAY + "] Your ping is too high, shot not registered."));
                    return;
                }

                if (entityId != -1) {
                    Entity target = entityPlayer.world.getEntityByID(entityId);
                    WeaponFireMode fireMode = GunType.getFireMode(entityPlayer.getHeldItemMainhand());
                    if (fireMode == null) {
                        return;
                    }

                    IExtraItemHandler extraSlots = null;
                    ItemStack plate = null;
                    if (ShotValidation.verifShot(entityPlayer, entityPlayer.getHeldItemMainhand(), itemGun, fireMode, fireTickDelay, recoilPitch, recoilYaw, recoilAimReducer, bulletSpread) && target != null) {
                        float damage = itemGun.type.gunDamage;
                        if (target instanceof EntityPlayer && hitboxType != null && hitboxType.contains("body")) {
                            EntityPlayer player = (EntityPlayer) target;
                            if (player.hasCapability(CapabilityExtra.CAPABILITY, (EnumFacing) null)) {
                                extraSlots = player.getCapability(CapabilityExtra.CAPABILITY, (EnumFacing) null);
                                plate = Objects.requireNonNull(extraSlots).getStackInSlot(1);
                                if (plate.getItem() instanceof ItemSpecialArmor) {
                                    ArmorType armorType = ((ItemSpecialArmor) plate.getItem()).type;
                                    damage = (float) (damage - damage * armorType.defense);
                                }
                            }
                        }

                        ItemBullet bulletItem = ItemGun.getUsedBullet(entityPlayer.getHeldItemMainhand(), itemGun.type);
                        if (bulletItem == null) return;
                        if (target instanceof EntityLivingBase) {
                            EntityLivingBase targetELB = (EntityLivingBase) target;
                            if (bulletItem.type != null && bulletItem.type.bulletProperties != null && !bulletItem.type.bulletProperties.isEmpty()) {
                                BulletProperty bulletProperty = bulletItem.type.bulletProperties.get(targetELB.getName()) != null ? (BulletProperty) bulletItem.type.bulletProperties.get(targetELB.getName()) : (BulletProperty) bulletItem.type.bulletProperties.get("All");
                                if (bulletProperty.potionEffects != null) {
                                    PotionEntry[] var10 = bulletProperty.potionEffects;
                                    int var11 = var10.length;

                                    for (PotionEntry potionEntry : var10) {
                                        targetELB.addPotionEffect(new PotionEffect(potionEntry.potionEffect.getPotion(), potionEntry.duration, potionEntry.level));
                                    }
                                }
                            }
                        }

                        if (bulletItem.type == null) return;
                        damage *= bulletItem.type.bulletDamageFactor;
                        boolean flag;
                        boolean headShot = hitboxType.contains("head");
                        DamageSourceModular damageSource = ModularWarfareDamageSourceFix.causeModularDmg(entityPlayer, itemGun.type.displayName, headShot);
                        if (bulletItem.type.isFireDamage) {
                            damageSource.setFireDamage();
                        }

                        if (bulletItem.type.isAbsoluteDamage) {
                            damageSource.setDamageIsAbsolute();
                        }

                        if (bulletItem.type.isBypassesArmorDamage) {
                            damageSource.setDamageBypassesArmor();
                        }

                        if (bulletItem.type.isExplosionDamage) {
                            damageSource.setExplosion();
                        }

                        if (bulletItem.type.isMagicDamage) {
                            damageSource.setMagicDamage();
                        }

                        if (!ModConfig.INSTANCE.shots.knockback_entity_damage) {
                            flag = RayUtil.attackEntityWithoutKnockback(target, damageSource, headShot ? damage + itemGun.type.gunDamageHeadshotBonus : damage);
                        } else {
                            flag = target.attackEntityFrom(damageSource, headShot ? damage + itemGun.type.gunDamageHeadshotBonus : damage);
                        }

                        target.hurtResistantTime = 0;
                        if (flag && plate != null) {
                            plate.attemptDamageItem(1, entityPlayer.getRNG(), entityPlayer);
                            if (plate.getItemDamage() >= plate.getMaxDamage()) {
                                extraSlots.setStackInSlot(1, ItemStack.EMPTY);
                            } else {
                                extraSlots.setStackInSlot(1, plate);
                            }
                        }

                        if (entityPlayer instanceof EntityPlayerMP) {
                            ModularWarfare.NETWORK.sendTo(new PacketPlayHitmarker(headShot), entityPlayer);
                            ModularWarfare.NETWORK.sendTo(new PacketPlaySound(target.getPosition(), "flyby", 1.0F, 1.0F), (EntityPlayerMP) target);
                            if (ModConfig.INSTANCE.hud.snap_fade_hit) {
                                ModularWarfare.NETWORK.sendTo(new PacketPlayerHit(), (EntityPlayerMP) target);
                            }
                        }
                    }
                } else {
                    BlockPos blockPos = new BlockPos(posX, posY, posZ);
                    ItemGun.playImpactSound(entityPlayer.world, blockPos, itemGun.type);
                    itemGun.type.playSoundPos(blockPos, entityPlayer.world, WeaponSoundType.Crack, entityPlayer, 1.0F);
                    ItemGun.doHit(posX, posY, posZ, facing, entityPlayer);
                }
            }

        });
    }
}
