package io.github.henryyslin.enderpearlabilities.abilities.ash;

import io.github.henryyslin.enderpearlabilities.abilities.Ability;
import io.github.henryyslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henryyslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henryyslin.enderpearlabilities.utils.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AshAbility extends Ability {

    static final int TARGET_RANGE = 80;
    static final double ANGLE_ALLOWANCE = Math.toRadians(20);

    private final AbilityInfo info;

    @Override
    public void setConfigDefaults(ConfigurationSection config) {
        config.addDefault("charge-up", 0);
        config.addDefault("duration", 15);
        config.addDefault("cooldown", 400);
    }

    public AshAbility(Plugin plugin, String ownerName, ConfigurationSection config) {
        super(plugin, ownerName, config);

        AbilityInfo.Builder builder = new AbilityInfo.Builder()
                .codeName("ash")
                .name("Phase Breach")
                .origin("Apex - Ash")
                .description("Tear open a one-way portal to a targeted location.")
                .usage("Right click.")
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

    final AtomicBoolean chargingUp = new AtomicBoolean(false);
    final AtomicBoolean abilityActive = new AtomicBoolean(false);
    final Random random = new Random();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        super.onPlayerJoin(event);
        Player player = event.getPlayer();
        if (player.getName().equals(ownerName)) {
            setUpPlayer(player);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (player != null) {
            setUpPlayer(player);
        }
    }

    private void setUpPlayer(Player player) {
        chargingUp.set(false);
        abilityActive.set(false);
        cooldown.startCooldown(info.cooldown);
    }

    private Vector rotateVectorDownwards(Vector vector, double angle) {
        double horizontalDist = vector.clone().setY(0).length();
        vector.setY(Math.max(vector.getY() - vector.length(), horizontalDist * Math.tan(Math.max(Math.toRadians(-89.99999), Math.atan2(vector.getY(), horizontalDist) - angle))));
        return vector;
    }

    private Optional<Location> getTargetLocation(Player player) {
        World world = player.getWorld();
        Vector direction = player.getEyeLocation().getDirection();
        Location eyeLocation = player.getEyeLocation();

        if (MathUtils.almostEqual(direction.getX(), 0) && MathUtils.almostEqual(direction.getZ(), 0) && direction.getY() > 0)
            return Optional.empty();

        RayTraceResult eyeRay = world.rayTraceBlocks(eyeLocation, direction, TARGET_RANGE, FluidCollisionMode.NEVER);

        Vector eyeHitPosition;
        if (eyeRay != null) {
            if (eyeRay.getHitBlockFace() == BlockFace.UP)
                return Optional.of(eyeRay.getHitPosition().toLocation(player.getWorld()));
            eyeHitPosition = eyeRay.getHitPosition().add(direction.clone().multiply(-0.1));
        } else {
            if (MathUtils.almostEqual(direction.getX(), 0) && MathUtils.almostEqual(direction.getZ(), 0) && direction.getY() < 0)
                return Optional.empty();
            eyeHitPosition = player.getEyeLocation().add(direction.clone().multiply(TARGET_RANGE)).toVector();
        }

        Vector eyeLocationVector = eyeLocation.toVector();
        for (Vector offset = direction.clone().multiply(-0.5); eyeHitPosition.distanceSquared(eyeLocationVector) > 1; eyeHitPosition.add(offset)) {
            Vector relative = eyeHitPosition.clone().subtract(eyeLocationVector);
            Vector downward = rotateVectorDownwards(relative.clone(), ANGLE_ALLOWANCE);
            RayTraceResult result = world.rayTraceBlocks(eyeHitPosition.toLocation(world), new Vector(0, -1, 0), relative.getY() - downward.getY(), FluidCollisionMode.NEVER, true);
            if (result == null) continue;
            Location targetEye = result.getHitPosition().toLocation(world).add(0, player.getEyeHeight(), 0);
            Vector targetDirection = eyeLocation.toVector().subtract(targetEye.toVector());
            RayTraceResult check = world.rayTraceBlocks(targetEye, targetDirection, targetDirection.length(), FluidCollisionMode.NEVER, true);
            if (check == null) return Optional.of(result.getHitPosition().toLocation(world));
        }
        return Optional.empty();
    }

    @EventHandler
    public synchronized void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!AbilityUtils.abilityShouldActivate(event, ownerName, info.activation)) return;

        event.setCancelled(true);

        if (cooldown.getCoolingDown()) return;
        if (abilityActive.get()) return;

        if (chargingUp.get()) {
            PlayerUtils.consumeEnderPearl(player);
            chargingUp.set(false);
            abilityActive.set(true);
        } else {
            new FunctionChain(
                    next -> {
                        chargingUp.set(true);
                        next.run();
                    },
                    next -> new AbilityRunnable() {
                        LivingEntity target;
                        Location lastLocation;

                        @Override
                        protected void start() {
                            target = player.getWorld().spawn(player.getLocation().add(0, 300, 0), Villager.class, entity -> {
                                entity.setGlowing(false);
                                entity.setSilent(true);
                                entity.setAI(false);
                                entity.setGravity(false);
                                entity.setInvisible(true);
                                entity.setInvulnerable(true);
                                entity.setCollidable(false);
                            });
                        }

                        @Override
                        protected void tick() {
                            if (!chargingUp.get()) {
                                cancel();
                                return;
                            }
                            boolean mainHandPearl = player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL;
                            boolean offHandPearl = player.getInventory().getItemInOffHand().getType() == Material.ENDER_PEARL;
                            if (ability.getInfo().activation == ActivationHand.MainHand && mainHandPearl ||
                                    ability.getInfo().activation == ActivationHand.OffHand && offHandPearl) {
                                Optional<Location> targetLocation = getTargetLocation(player);
                                lastLocation = targetLocation.orElse(null);
                                targetLocation.ifPresentOrElse(location -> {
                                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 0.6, 0), 20, 0.05, 0.6, 0.05, 0, null, true);
                                    target.teleport(location.setDirection(player.getLocation().getDirection()));
                                    target.setGlowing(true);
                                }, () -> {
                                    target.setGlowing(false);
                                    target.teleport(player.getLocation().add(0, 300, 0));
                                });
                            } else {
                                chargingUp.set(false);
                                abilityActive.set(false);
                                cancel();
                            }
                        }

                        @Override
                        protected void end() {
                            target.remove();
                            if (abilityActive.get()) {
                                if (lastLocation != null)
                                    player.teleport(lastLocation);
                                abilityActive.set(false);
                            }
                        }
                    }.runTaskRepeated(this, 0, 1, 600)
            ).execute();
        }
    }
}
