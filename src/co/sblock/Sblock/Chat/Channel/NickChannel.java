package co.sblock.Sblock.Chat.Channel;

import java.util.HashMap;
import java.util.Map;

import co.sblock.Sblock.Chat.ChatMsgs;
import co.sblock.Sblock.Chat.ChatUser;
import co.sblock.Sblock.Chat.ChatUserManager;

public class NickChannel extends NormalChannel {

	private Map<ChatUser, String> nickList = new HashMap<ChatUser, String>();
	
	public NickChannel(String name, AccessLevel a, String creator) {
		super(name, a, creator);
	}
	
	@Override
	public ChannelType getType()	{
		return ChannelType.NICK;
	}
	
	@Override
	public void setNick(ChatUser sender, String nick) {
		nickList.put(sender, nick);
		for(String user : this.getListening()){
			ChatUserManager.getUserManager().getUser(user).sendMessage(ChatMsgs.onUserSetNick(sender.getPlayerName(), nick, this));
		}
	}

	@Override
	public void removeNick(ChatUser sender) {
		for(String user : this.getListening()){
			ChatUserManager.getUserManager().getUser(user).sendMessage(
					ChatMsgs.onUserRmNick(sender.getPlayerName(), nickList.get(sender), this));
		}
		nickList.remove(sender);
	}
	
	@Override
	public String getNick(ChatUser sender) {
		return nickList.get(sender);
	}
	
	@Override
	public boolean hasNick(ChatUser sender)	{
		return nickList.containsKey(sender);
	}
/*	protected HashBiMap<String, String> nickList;

	public NickChannel(String name, AccessLevel a, String creator) {
		super(name, a, creator);
		nickList = HashBiMap.create();
	}

	public ChannelType getType() {
		return ChannelType.NICK;
	}

	public void setNick(ChatUser sender, String nick) {
		if (getUserFromNick(nick) != null) {
			sender.sendMessage(ChatColor.RED + "The nick " + ChatColor.BLUE
					+ nick + ChatColor.RED + " is already in use!");
		}
		this.nickList.put(sender.getPlayerName(), nick);
		sender.sendMessage(ChatColor.YELLOW + "Your nick has been set to \""
				+ ChatColor.BLUE + nick + "\" in "
				+ ChatColor.GOLD + this.getName());
	}

	public void removeNick(ChatUser sender) {
		this.nickList.remove(sender.getPlayerName());
		sender.sendMessage(ChatColor.YELLOW + "Your nick has been reset to \""
				+ ChatColor.BLUE + sender.getNick() + "\" in " +
				ChatColor.GOLD + this.getName());
	}

	@Override
	public Nick getNick(ChatUser user) {
		return this.nickList.get(user.getPlayerName()) != null ?
				new Nick(this.nickList.get(user.getPlayerName())) : new Nick(user.getNick());
	}

	public String getUserFromNick(String n)	{
		return nickList.inverse().get(n);
	}*/
}
