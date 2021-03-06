package com.easterlyn.machines.type.computer;

import com.easterlyn.effects.Effects;
import com.easterlyn.effects.effect.BehaviorGodtier;
import com.easterlyn.effects.effect.Effect;
import com.easterlyn.machines.Machines;
import com.easterlyn.users.User;
import com.easterlyn.users.UserAffinity;
import com.easterlyn.users.Users;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Jikoo
 */
public class GodtierToggle extends Program {

	private final Effects effects;
	private final Users users;
	private final ItemStack icon, icoff;

	public GodtierToggle(Machines machines) {
		super(machines);
		this.effects = machines.getPlugin().getModule(Effects.class);
		this.users = machines.getPlugin().getModule(Users.class);

		icon = new ItemStack(Material.LIME_WOOL);
		icoff = new ItemStack(Material.RED_WOOL);

		ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.WHITE_WOOL);
		meta.setDisplayName(ChatColor.YELLOW + "Toggle " + ChatColor.GREEN + "ON"
				+ ChatColor.YELLOW + " or " + ChatColor.RED + "OFF");

		icon.setItemMeta(meta);
		icoff.setItemMeta(meta);
	}

	@Override
	protected void execute(Player player, ItemStack clicked, boolean verified) {
		if (!clicked.hasItemMeta()) {
			return;
		}
		ItemMeta meta = clicked.getItemMeta();
		if (!meta.hasLore()) {
			return;
		}
		List<String> lore = meta.getLore();
		if (lore.isEmpty()) {
			return;
		}

		User user = users.getUser(player.getUniqueId());
		String effectName = lore.get(0).replaceFirst("..toggle ", "");
		Effect effect = effects.getEffect(effectName);
		if (effect == null) {
			return;
		}
		ItemStack newClicked;
		if (clicked.getData().equals(icon.getData())) {
			user.removeGodtierEffect(effect);
			newClicked = getIcon(effect, user.getUserAffinity(), false);
		} else if (clicked.getData().equals(icoff.getData())) {
			newClicked = getIcon(effect, user.getUserAffinity(), user.addGodtierEffect(effect));
		} else {
			return;
		}
		Inventory top = player.getOpenInventory().getTopInventory();
		for (int i = 0; i < top.getSize(); i++) {
			if (clicked.isSimilar(top.getItem(i))) {
				top.setItem(i, newClicked);
				break;
			}
		}
		player.updateInventory();
	}

	@Override
	public ItemStack getIcon() {
		return icon;
	}

	public ItemStack getIcon(Effect effect, UserAffinity aspect, boolean enabled) {
		ItemStack stack = enabled ? icon.clone() : icoff.clone();
		ItemMeta meta = stack.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		lore.add(ChatColor.WHITE + "toggle " + effect.getName());
		lore.addAll(((BehaviorGodtier) effect).getDescription(aspect));
		meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	@Override
	public ItemStack getInstaller() {
		return null;
	}

}
