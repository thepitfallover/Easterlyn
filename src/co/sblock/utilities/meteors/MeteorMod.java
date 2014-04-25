package co.sblock.utilities.meteors;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import co.sblock.Module;
import co.sblock.Sblock;

/**
 * @author Dublek, Jikoo
 */
public class MeteorMod extends Module implements Listener {
	/** The MeteorMod instance. */
	private static MeteorMod instance;
	/** The Map of Meteorite FallingBlock UUIDs */
	private HashMap<Entity, Boolean> entities;
	/** exploding entitiy id's, probably. */
	private int task;

	/**
	 * @see Module#onEnable()
	 */
	@Override
	public void onEnable() {
		instance = this;
		entities = new HashMap<>();
		this.registerCommands(new MeteorCommandListener());
		Sblock.getInstance().getServer().getPluginManager()
				.registerEvents(this, Sblock.getInstance());
		task = -1;
		// startReckoning(20*20);
	}

	/**
	 * @see Module#onDisable()
	 */
	@Override
	public void onDisable() {
		// stopReckoning();
	}

	/**
	 * Gets the MeteorMod instance.
	 * 
	 * @return the MeteorMod instance
	 */
	public static MeteorMod getInstance() {
		return instance;
	}

	/**
	 * Add an Entity. If FallingBlock, specify if the entity is to explode on contact.
	 * 
	 * @param entity the Entity
	 * @param damage true if the explosion is to do terrain damage
	 */
	public void addEntity(Entity entity, boolean damage) {
		entities.put(entity, damage);
		if (task == -1) {
			task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Sblock.getInstance(), new Runnable() {
				@Override
				public void run() {
					if (entities.size() == 0) {
						Bukkit.getScheduler().cancelTask(task);
						task = -1;
						return;
					}
					for (Entity entity : entities.keySet().toArray(new Entity[0])) {
						if (entity.isDead()) {
							entities.remove(entity);
						}
						if (entity.getType() == EntityType.FALLING_BLOCK) {
							entity.getWorld().playEffect(entity.getLocation(), Effect.MOBSPAWNER_FLAMES, 48);
						}
					}
				}
			}, 0, 4);
		}
	}

	/**
	 * The EventHandler for EntityChangeBlockEvents to handle Meteorite FallingBlock landings.
	 * 
	 * @param event the EntityChangeBlockEvent
	 */
	@EventHandler
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
		if (event.getEntityType() != EntityType.FALLING_BLOCK) {
			return;
		}
		if (entities.containsKey(event.getEntity())) {
			
			event.setCancelled(true);
			event.getEntity().remove();
			explode(event.getBlock().getLocation(), entities.remove(event.getEntity()));
			event.getBlock().setType(Material.AIR);
			return;
		}
	}

	/**
	 * Cause an explosion at a Location.
	 * 
	 * @param loc the Location to explode at
	 */
	public void explode(Location loc, boolean explosionBlockDamage) {
		loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 4F, false,
				explosionBlockDamage);
	}

	/**
	 * Starts Meteorites being created in the area of online Players.
	 * 
	 * @param rLong the time to delay the reckoning start by in ticks
	 */
	/*
	 * public void startReckoning(long rLong) { task = new
	 * scheduledReckoning().runTaskTimer(this, 20*300, rLong); }
	 * 
	 * public void stopReckoning() { task.cancel(); }
	 */

	/*
	 * public class scheduledReckoning extends BukkitRunnable {
	 * 
	 * @Override public void run() { if (getServer().getOnlinePlayers().length
	 * >= 1) { Player pTarget = getServer().getOnlinePlayers()[(int)
	 * (getServer() .getOnlinePlayers().length * Math.random())]; Location
	 * target = pTarget.getLocation(); target.setX((int) ((160 * Math.random())
	 * - 80)); target.setZ((int) ((160 * Math.random()) - 80)); int radius = -1;
	 * int countdown = -1; String material = ""; boolean blockDamage = false;
	 * getLogger().info( pTarget.getName() +
	 * "has been randomly selected for termination"); meteorites.add(new
	 * Meteorite(plugin, pTarget, target, material, radius, countdown,
	 * blockDamage)); } } }
	 */
}
