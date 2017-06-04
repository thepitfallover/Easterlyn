package com.easterlyn.effects.effect.godtier.active;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.easterlyn.Easterlyn;
import com.easterlyn.effects.effect.BehaviorActive;
import com.easterlyn.effects.effect.BehaviorGodtier;
import com.easterlyn.effects.effect.Effect;
import com.easterlyn.users.UserAspect;
import com.easterlyn.utilities.BlockDrops;
import com.easterlyn.utilities.InventoryUtils;

import org.bukkit.CropState;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.CocoaPlant.CocoaPlantSize;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;
import org.bukkit.material.NetherWarts;

import net.md_5.bungee.api.ChatColor;

/**
 * Effect for automatically harvesting and planting crops when right clicked.
 * 
 * @author Jikoo
 */
public class EffectGreenThumb extends Effect implements BehaviorActive, BehaviorGodtier {

	public EffectGreenThumb(Easterlyn plugin) {
		super(plugin, Integer.MAX_VALUE, 1, 1, "Green Thumb");
	}

	@Override
	public Collection<UserAspect> getAspects() {
		return Collections.singletonList(UserAspect.KNOWLEDGE);
	}

	@Override
	public List<String> getDescription(UserAspect aspect) {
		ArrayList<String> list = new ArrayList<>();
		if (aspect == UserAspect.KNOWLEDGE) {
			list.add(aspect.getColor() + "Harvest Goddess");
		}
		list.add(ChatColor.WHITE + "Tending plants made easy.");
		list.add(ChatColor.GRAY + "Right click crops to harvest and replant.");
		return list;
	}

	@Override
	public Collection<Class<? extends Event>> getApplicableEvents() {
		return Collections.singletonList(PlayerInteractEvent.class);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(Event event, LivingEntity entity, int level) {
		PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
		if (!interactEvent.hasBlock() || interactEvent.getAction() == Action.LEFT_CLICK_BLOCK) {
			return;
		}

		Block clicked = interactEvent.getClickedBlock();
		BlockState state = clicked.getState();
		MaterialData data = state.getData();
		Material seed = null;
		Short seedData = 0;

		if (data instanceof Crops) {
			Crops crops = (Crops) data;
			if (crops.getState() != CropState.RIPE) {
				return;
			}
			crops.setState(CropState.SEEDED);
			switch (crops.getItemType()) {
			case BEETROOT_BLOCK:
				seed = Material.BEETROOT_SEEDS;
				break;
			case CARROT:
				seed = Material.CARROT_ITEM;
				break;
			case CROPS:
				seed = Material.SEEDS;
				break;
			case POTATO:
				seed = Material.POTATO_ITEM;
				break;
			default:
				break;
			}
		} else if (data instanceof CocoaPlant) {
			CocoaPlant cocoa = (CocoaPlant) data;
			if (cocoa.getSize() != CocoaPlantSize.LARGE) {
				return;
			}
			cocoa.setSize(CocoaPlantSize.SMALL);
			seed = Material.INK_SACK;
			seedData = (short) DyeColor.BROWN.getDyeData();
		} else if (data instanceof NetherWarts) {
			NetherWarts warts = (NetherWarts) data;
			if (warts.getState() != NetherWartsState.RIPE) {
				return;
			}
			warts.setState(NetherWartsState.SEEDED);
			seed = Material.NETHER_STALK;
		} else {
			return;
		}

		Player player = interactEvent.getPlayer();
		boolean reseed = false;
		for (ItemStack drop : BlockDrops.getDrops(getPlugin(), player, interactEvent.getItem(), clicked)) {
			if (seed != null && drop.getType() == seed && !reseed) {
				// Re-seed cost
				drop = InventoryUtils.decrement(drop, 1);
				reseed = true;
			}
			if (drop != null) {
				player.getWorld().dropItem(player.getLocation(), drop).setPickupDelay(0);
			}
		}
		if (!reseed && seed != null) {
			reseed = player.getInventory().removeItem(new ItemStack(seed, 1, seedData)).size() < 1;
		}

		clicked.getWorld().playSound(clicked.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
		clicked.getWorld().spawnParticle(Particle.BLOCK_CRACK, clicked.getLocation(), 1, new MaterialData(data.getItemType(), data.getData()));

		if (reseed) {
			state.setData(data);
			state.update(true);
		} else {
			clicked.setType(Material.AIR);
		}
	}

}