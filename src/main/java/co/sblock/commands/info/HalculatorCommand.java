package co.sblock.commands.info;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.bukkit.command.CommandSender;

import com.google.common.collect.ImmutableList;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.commands.SblockCommand;

import net.md_5.bungee.api.ChatColor;

/**
 * SblockCommand for using the halculate function just for yourself.
 * 
 * @author Jikoo
 */
public class HalculatorCommand extends SblockCommand {

	private final Chat chat;

	public HalculatorCommand(Sblock plugin) {
		super(plugin, "halculate");
		this.setAliases("halc", "evhal", "evhaluate");
		this.setDescription("Halculate an equation privately.");
		this.setUsage("/halc 1+1");
		this.chat = plugin.getModule(Chat.class);
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("Please enter an equation! Ex. /halc (1+1)^(2/3) + 10");
		} else {
			sender.sendMessage(ChatColor.RED + "Evhaluation: " + ChatColor.GRAY
					+ chat.getHalculator().evhaluate(StringUtils.join(args, ' ')));
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return ImmutableList.of();
	}
}