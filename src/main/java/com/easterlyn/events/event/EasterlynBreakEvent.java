package com.easterlyn.events.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Event wrapper used to prevent certain methods from recursively calling themselves.
 * 
 * @author Jikoo
 */
public class EasterlynBreakEvent extends BlockBreakEvent {

	public EasterlynBreakEvent(Block theBlock, Player player) {
		super(theBlock, player);
	}
}
