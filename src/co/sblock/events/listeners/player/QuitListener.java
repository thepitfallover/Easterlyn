package co.sblock.events.listeners.player;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import co.sblock.Sblock;
import co.sblock.chat.Color;
import co.sblock.effects.Effects;
import co.sblock.events.Events;
import co.sblock.micromodules.FreeCart;
import co.sblock.micromodules.Godule;
import co.sblock.micromodules.Slack;
import co.sblock.micromodules.SleepVote;
import co.sblock.micromodules.Spectators;
import co.sblock.progression.Entry;
import co.sblock.users.OfflineUser;
import co.sblock.users.OnlineUser;
import co.sblock.users.ProgressionState;
import co.sblock.users.Users;
import co.sblock.utilities.InventoryManager;

/**
 * Listener for PlayerQuitEvents.
 * 
 * @author Jikoo
 */
public class QuitListener implements Listener {

	/**
	 * The event handler for PlayerQuitEvents.
	 * 
	 * @param event the PlayerQuitEvent
	 */
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		// Our very own custom quits!
		if (event.getQuitMessage() != null) {
			event.setQuitMessage(Color.BAD_PLAYER + event.getPlayer().getDisplayName() + Color.BAD + " ollies outie");
		}

		// Handle reactive Effects that use quits
		Effects.getInstance().handleEvent(event, event.getPlayer(), true);

		// Slack integration
		Slack.getInstance().postMessage(event.getPlayer().getName(), event.getPlayer().getUniqueId(),
				event.getPlayer().getName() + " logs out.", true);

		// Update vote
		SleepVote.getInstance().updateVoteCount(event.getPlayer().getWorld().getName(), event.getPlayer().getName());

		// Remove free minecart if riding one
		FreeCart.getInstance().remove(event.getPlayer());

		// Remove Spectator status
		if (Spectators.getInstance().isSpectator(event.getPlayer().getUniqueId())) {
			Spectators.getInstance().removeSpectator(event.getPlayer());
		}

		// Stop scheduled sleep teleport
		if (Events.getInstance().getSleepTasks().containsKey(event.getPlayer().getName())) {
			Events.getInstance().getSleepTasks().remove(event.getPlayer().getName()).cancel();
		}

		// Restore inventory if still preserved
		InventoryManager.restoreInventory(event.getPlayer());

		// Delete team for exiting player to avoid clutter
		Users.unteam(event.getPlayer());

		final UUID uuid = event.getPlayer().getUniqueId();
		OfflineUser user = Users.getGuaranteedUser(uuid);
		user.save();

		// Disable "god" effect, if any
		if (event.getPlayer().hasPermission("sblock.god")) {
			Godule.getInstance().disable(user.getUserAspect());
		}

		// Remove Server status
		if (user instanceof OnlineUser && ((OnlineUser) user).isServer()) {
			((OnlineUser) user).stopServerMode();
		}

		// Complete success sans animation if player logs out
		if (user.getProgression() == ProgressionState.ENTRY_COMPLETING) {
			Entry.getEntry().finalizeSuccess(event.getPlayer(), user);
		}

		// Fail Entry if in progress
		if (user.getProgression() == ProgressionState.ENTRY_UNDERWAY) {
			Entry.getEntry().fail(user);
		}

		// Inform channels that the player is no longer listening to them
		for (Iterator<String> iterator = user.getListening().iterator(); iterator.hasNext();) {
			if (!user.removeListeningQuit(iterator.next())) {
				iterator.remove();
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				// A player chatting during an unload will cause the recipient's OnlineUser to be re-created.
				// To combat this, we attempt to unload the user after they should no longer be online.
				if (!Bukkit.getOfflinePlayer(uuid).isOnline()) {
					Users.unloadUser(uuid);
				}
			}
		}.runTask(Sblock.getInstance());
	}
}
