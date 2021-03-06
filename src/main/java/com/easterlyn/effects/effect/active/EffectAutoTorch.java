package com.easterlyn.effects.effect.active;

import com.easterlyn.Easterlyn;
import com.easterlyn.effects.effect.BehaviorActive;
import com.easterlyn.effects.effect.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;

/**
 * Automatically consumes and places torches if light level is below 8 and the block below is solid.
 *
 * @author Jikoo
 */
public class EffectAutoTorch extends Effect implements BehaviorActive { // TODO godtier: light

	public EffectAutoTorch(Easterlyn plugin) {
		super(plugin, 200, 1, 1, "Darkness Despising");
	}

	@Override
	public Collection<Class<? extends Event>> getApplicableEvents() {
		return Collections.singletonList(BlockBreakEvent.class);
	}

	@Override
	public void handleEvent(Event event, LivingEntity entity, int level) {
		Player player = (Player) entity;
		Block block = player.getLocation().getBlock();
		if (!block.isEmpty() || block.getLightFromBlocks() > 7
				|| !block.getRelative(BlockFace.DOWN).getType().isSolid()
				|| player.getGameMode() != GameMode.CREATIVE
				&& !player.getInventory().removeItem(new ItemStack(Material.TORCH)).isEmpty()) {
			return;
		}
		block.setType(Material.TORCH);
	}

}
