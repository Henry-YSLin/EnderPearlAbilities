package io.github.henryyslin.enderpearlabilities.abilities.frag;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityCouple;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.AbilityRunnable;
import io.github.henryyslin.enderpearlabilities.utils.AbilityUtils;
import io.github.henryyslin.enderpearlabilities.utils.FunctionChain;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FragAbility extends Ability {

    static final double PROJECTILE_SPEED = 2;

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 60);
        config.addDefault("cooldown", 400);
    }

    public FragAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("frag")
                .name("Frag Grenade")
                .origin("Apex")
                .description("Throw frag grenades that bounce around and explode.")
                .usage("Right click to throw.")
                .activation(ActivationHand.MainHand);

        if (config != null)
            builder
                    .chargeUp(config.getInt("charge-up"))
                    .duration(config.getInt("duration"))
                    .cooldown(config.getInt("cooldown"));

        info = builder.build();
    }

    @Override
    public AbilityInfo getInfo() {
        return info;
    }

    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final AtomicInteger enderPearlHitTime = new AtomicInteger();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            abilityActive.set(false);
            cooldown.startCooldown(info.cooldown);
        }
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        abilityActive.set(true);

        AtomicReference<Projectile> enderPearl = new AtomicReference<>();

        new FunctionChain(
                next -> AbilityUtils.chargeUpSequence(this, player, info.chargeUp, next),
                next -> {
                    Projectile projectile = AbilityUtils.fireEnderPearl(this, player, null, info.duration, PROJECTILE_SPEED, true);
                    if (projectile != null) {
                        enderPearl.set(projectile);
                        projectile.setMetadata("pearl", new FixedMetadataValue(plugin, enderPearl));
                    }
                    next.run();
                },
                next -> new AbilityRunnable() {
                    @Override
                    protected void tick() {
                        Projectile pearl = enderPearl.get();
                        if (pearl == null) {
                            cancel();
                            return;
                        }
                        pearl.setVelocity(pearl.getVelocity().add(new Vector(0, -0.05, 0)));
                        pearl.getWorld().spawnParticle(Particle.END_ROD, pearl.getLocation(), 1, 0, 0, 0, 0);
                    }

                    @Override
                    protected void end() {
                        Projectile pearl = enderPearl.get();
                        if (pearl != null && pearl.isValid()) {
                            World world = pearl.getWorld();
                            Location pearlLocation = pearl.getLocation();
//                            // Flash bang
//                            world.spawnParticle(Particle.FLASH, pearlLocation, 3, 1, 1, 1, 1);
//                            world.spawnParticle(Particle.FIREWORKS_SPARK, pearlLocation, 10, 0.05, 0.05, 0.05, 0.3);
//                            world.getNearbyEntities(pearlLocation, 20, 20, 20, entity -> entity instanceof LivingEntity).forEach(entity -> {
//                                LivingEntity livingEntity = (LivingEntity) entity;
//                                double distance = pearlLocation.distance(livingEntity.getEyeLocation());
//                                RayTraceResult result = world.rayTraceBlocks(pearlLocation, livingEntity.getEyeLocation().toVector().subtract(pearlLocation.toVector()), distance, FluidCollisionMode.NEVER, true);
//                                if (result != null) {
//                                    if (result.getHitBlock() != null) return;
//                                }
//                                int duration = (int) Math.min(120, 1000 / distance);
//                                livingEntity.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(duration, 1));
//                                livingEntity.addPotionEffect(PotionEffectType.CONFUSION.createEffect(duration * 2, 1));
//                            });

                            world.createExplosion(pearl.getLocation().add(0, 0.1, 0), 6, false, false);
                            pearl.remove();
                            abilityActive.set(false);
                            cooldown.startCooldown(info.cooldown);
                        }
                    }
                }.runTaskRepeated(this, 0, 1, info.duration)
        ).execute();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player player)) return;
        if (!AbilityUtils.verifyAbilityCouple(this, projectile)) return;
        if (!player.getName().equals(ownerName)) return;

        if (!(projectile instanceof EnderPearl)) return;

        event.setCancelled(true);
        enderPearlHitTime.set(player.getTicksLived());

        projectile.getWorld().spawnParticle(Particle.SMOKE_NORMAL, projectile.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);

        Location hitPosition = AbilityUtils.correctProjectileHitLocation(projectile);
        Vector newVelocity;
        if (event.getHitBlockFace() != null) {
            double hitMagnitude = Math.abs(projectile.getVelocity().dot(event.getHitBlockFace().getDirection()));
            newVelocity = projectile.getVelocity().add(event.getHitBlockFace().getDirection().multiply(hitMagnitude)).multiply(0.7).add(event.getHitBlockFace().getDirection().multiply(hitMagnitude * 0.5));

        } else {
            newVelocity = projectile.getVelocity().setX(0).setZ(0);
        }
        EnderPearl pearl = projectile.getWorld().spawn(hitPosition, EnderPearl.class);
        pearl.setShooter(player);
        pearl.setMetadata("ability", new FixedMetadataValue(plugin, new AbilityCouple(info.codeName, ownerName)));
        pearl.setVelocity(newVelocity);
        Optional<Object> ref = AbilityUtils.getMetadata(projectile, "pearl");
        if (ref.isPresent()) {
            AtomicReference<Projectile> enderPearl = (AtomicReference<Projectile>) ref.get();
            enderPearl.set(pearl);
            pearl.setMetadata("pearl", new FixedMetadataValue(plugin, enderPearl));
        }
        projectile.remove();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.getPlayer().getName().equals(ownerName)) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (Math.abs(event.getPlayer().getTicksLived() - enderPearlHitTime.get()) > 1) return;
        event.setCancelled(true);
    }
}
