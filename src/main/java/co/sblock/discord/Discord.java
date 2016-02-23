package co.sblock.discord;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import org.reflections.Reflections;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import co.sblock.Sblock;
import co.sblock.chat.message.Message;
import co.sblock.discord.abstraction.DiscordCallable;
import co.sblock.discord.abstraction.DiscordCommand;
import co.sblock.discord.abstraction.DiscordModule;
import co.sblock.discord.listeners.DiscordMessageReceivedListener;
import co.sblock.discord.listeners.DiscordReadyListener;
import co.sblock.module.Module;
import co.sblock.utilities.PlayerLoader;
import co.sblock.utilities.TextUtils;

import net.md_5.bungee.api.ChatColor;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.DiscordException;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.MissingPermissionsException;
import sx.blah.discord.handle.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.HTTP429Exception;

/**
 * A Module for managing messaging to and from Discord.
 * 
 * @author Jikoo
 */
public class Discord extends Module {

	public static final String BOT_NAME = "Sbot";

	private final String chars = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private final Pattern toEscape = Pattern.compile("([\\_~*])"),
			spaceword = Pattern.compile("(\\s*)(\\S*)");
	private final ConcurrentLinkedQueue<DiscordCallable> queue, messageQueue;
	private final Map<Class<? extends DiscordModule>, DiscordModule> modules;
	private final Map<String, DiscordCommand> commands;
	private final LoadingCache<Object, Object> authentications;
	private final YamlConfiguration discordData;
	private final Cache<IMessage, String> pastMainMessages;

	private String channelMain, channelLog, channelReports;
	private Optional<String> login, password;
	private IDiscordClient client;
	private BukkitTask heartbeatTask;
	private Thread drainQueueThread, drainMessageQueueThread;

	private boolean lock = false;

	public Discord(Sblock plugin) {
		super(plugin);

		queue = new ConcurrentLinkedQueue<>();
		messageQueue = new ConcurrentLinkedQueue<>();
		modules = new HashMap<>();
		commands = new HashMap<>();

		authentications = CacheBuilder.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build(
				new CacheLoader<Object, Object>() {
					@Override
					public Object load(Object key) throws Exception {
						if (!(key instanceof UUID)) {
							return null;
						}
						String code = generateUniqueCode();
						authentications.put(code, key);
						return code;
					};
				});

		pastMainMessages = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).maximumSize(10)
				.removalListener(new RemovalListener<IMessage, String>() {
					@Override
					public void onRemoval(RemovalNotification<IMessage, String> notification) {
						messageQueue.add(new DiscordCallable() {
							@Override
							public void call() throws MissingPermissionsException, HTTP429Exception, DiscordException {
								// Editing messages causes them to use the current name.
								resetBotName();
								notification.getKey().edit(notification.getValue());
							}

							@Override
							public boolean retryOnException() {
								return true;
							}
						});
					}
				}).build();

		File file = new File(plugin.getDataFolder(), "DiscordData.yml");
		if (file.exists()) {
			discordData = YamlConfiguration.loadConfiguration(file);
		} else {
			discordData = new YamlConfiguration();
		}
	}

	@Override
	protected void onEnable() {
		login = Optional.of(getConfig().getString("discord.login"));
		password = Optional.of(getConfig().getString("discord.password"));
		channelMain = getConfig().getString("discord.chat.main");
		channelLog = getConfig().getString("discord.chat.log");
		channelReports = getConfig().getString("discord.chat.reports");

		if (login == null || password == null) {
			getLogger().severe("Unable to connect to Discord, no username or password!");
			this.disable();
			return;
		}

		try {
			this.client = new ClientBuilder().withLogin(this.login.get(), this.password.get()).build();
			EventDispatcher dispatcher = this.client.getDispatcher();
			dispatcher.registerListener(new DiscordReadyListener(this));

			this.client.login();

			dispatcher.registerListener(new DiscordMessageReceivedListener(this));
		} catch (DiscordException e) {
			e.printStackTrace();
			this.disable();
			return;
		}

		// Only load modules and whatnot once, no matter how many times we re-enable
		if (lock) {
			return;
		}

		lock = true;

		Reflections reflections = new Reflections("co.sblock.discord.modules");
		Set<Class<? extends DiscordModule>> moduleClazzes = reflections.getSubTypesOf(DiscordModule.class);
		for (Class<? extends DiscordModule> clazz : moduleClazzes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!Sblock.areDependenciesPresent(clazz)) {
				getLogger().warning(clazz.getSimpleName() + " is missing dependencies, skipping.");
				continue;
			}
			try {
				Constructor<? extends DiscordModule> constructor = clazz.getConstructor(getClass());
				DiscordModule module = constructor.newInstance(this);
				modules.put(clazz, module);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		reflections = new Reflections("co.sblock.discord.commands");
		Set<Class<? extends DiscordCommand>> commandClazzes = reflections.getSubTypesOf(DiscordCommand.class);
		for (Class<? extends DiscordCommand> clazz : commandClazzes) {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				continue;
			}
			if (!Sblock.areDependenciesPresent(clazz)) {
				getLogger().warning(clazz.getSimpleName() + " is missing dependencies, skipping.");
				continue;
			}
			try {
				Constructor<? extends DiscordCommand> constructor = clazz.getConstructor(getClass());
				DiscordCommand command = constructor.newInstance(this);
				commands.put('/' + command.getName(), command);
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDisable() {
		pastMainMessages.invalidateAll();
		if (client != null) {
			resetBotName();
			try {
				client.logout();
			} catch (HTTP429Exception | DiscordException e) {
				e.printStackTrace();
			}
		}
		try {
			discordData.save(new File(getPlugin().getDataFolder(), "DiscordData.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public YamlConfiguration loadConfig() {
		super.loadConfig();

		login = Optional.of(getConfig().getString("discord.login"));
		password = Optional.of(getConfig().getString("discord.password"));
		channelMain = getConfig().getString("discord.chat.main");
		channelLog = getConfig().getString("discord.chat.log");
		channelReports = getConfig().getString("discord.chat.reports");

		return getConfig();
	}

	private void resetBotName() {
		if (!client.getOurUser().getName().equals(BOT_NAME)) {
			try {
				client.changeAccountInfo(Optional.of(BOT_NAME), login, password, Optional.empty());
			} catch (HTTP429Exception | DiscordException e) {
				// Nothing we can do about this, really
			}
		}
	}

	private String generateUniqueCode() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(chars.charAt(getPlugin().getRandom().nextInt(chars.length())));
		}
		String code = sb.toString();
		if (authentications.getIfPresent(code) != null) {
			return generateUniqueCode();
		}
		return code;
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && this.client != null;
	}

	public YamlConfiguration getDatastore() {
		return discordData;
	}

	public void startHeartbeat() {
		if (heartbeatTask != null && heartbeatTask.getTaskId() != -1) {
			return;
		}
		heartbeatTask = new BukkitRunnable() {
			@Override
			public void run() {
				if (!isEnabled()) {
					cancel();
					return;
				}
				// In case either queue has encountered an error, attempt to restart them
				startQueueDrain();

				resetBotName();
				// In case no one is on or talking, clean up messages anyway
				pastMainMessages.cleanUp();

				for (DiscordModule module : modules.values()) {
					module.doHeartbeat();
				}
			}
		}.runTaskTimerAsynchronously(getPlugin(), 20L, 6000L); // 5 minutes between checks
	}

	private void startQueueDrain() {
		if (drainQueueThread == null || !drainQueueThread.isAlive()) {
			drainQueueThread = new QueueDrainThread(this, queue, 150, "Sblock-DiscordQueue");
			drainQueueThread.start();
		}
		if (drainMessageQueueThread == null || !drainMessageQueueThread.isAlive()) {
			drainMessageQueueThread = new QueueDrainThread(this, messageQueue, 250, "Sblock-DiscordMessageQueue");
			drainMessageQueueThread.start();
		}
	}

	public void queue(DiscordCallable call) {
		queue.add(call);
	}

	public void queueMessageDeletion(IMessage message) {
		queue.add(new DiscordCallable() {
			@Override
			public void call() throws MissingPermissionsException, HTTP429Exception, DiscordException {
				message.delete();
			}

			@Override
			public boolean retryOnException() {
				return false;
			}
		});
	}

	public IDiscordClient getClient() {
		return this.client;
	}

	public String getMainChannel() {
		return this.channelMain;
	}

	public String getLogChannel() {
		return this.channelLog;
	}

	public String getReportChannel() {
		return this.channelReports;
	}

	@SuppressWarnings("unchecked")
	public <T extends DiscordModule> T getModule(Class<T> clazz) throws IllegalArgumentException {
		Validate.isTrue(Module.class.isAssignableFrom(clazz), clazz.getName() + " is not a DiscordModule.");
		Validate.isTrue(modules.containsKey(clazz), "Module not enabled!");
		Object object = modules.get(clazz);
		Validate.isTrue(clazz.isAssignableFrom(object.getClass()));
		return (T) object;
	}

	public void log(String message) {
		postMessage(BOT_NAME, message, getLogChannel());
	}

	public void postMessage(Message message, boolean global) {
		if (global) {
			postMessage((message.isThirdPerson() ? "* " : "") + message.getSenderName(),
					message.getDiscordMessage(), getMainChannel());
		}
		postMessage(BOT_NAME, message.getConsoleMessage(), getLogChannel());
	}

	public void postMessage(String name, String message, boolean global) {
		if (global) {
			postMessage(name, message, getMainChannel(), getLogChannel());
		} else {
			postMessage(name, message, getLogChannel());
		}
	}

	public void postMessage(String name, String message, String... channels) {
		if (!isEnabled()) {
			return;
		}
		name = ChatColor.stripColor(name);
		// TODO allow formatting codes in any chat? Could support markdown rather than &codes.
		message = ChatColor.stripColor(message);
		if (message.trim().isEmpty()) {
			return;
		}
		// Discord is case-sensitive. This prevents an @everyone alert without altering content.
		message = message.replace("@everyone", "@Everyone");
		StringBuilder builder = new StringBuilder();
		Matcher matcher = spaceword.matcher(message);
		while (matcher.find()) {
			builder.append(matcher.group(1));
			String word = matcher.group(2);
			if (!TextUtils.URL_PATTERN.matcher(word).find()) {
				word = toEscape.matcher(word).replaceAll("\\\\$1");
			}
			builder.append(word);
		}
		for (String channel : channels) {
			addMessageToQueue(channel, name, builder.toString());
		}
	}

	private void addMessageToQueue(final String channel, final String name, final String message) {
		messageQueue.add(new DiscordCallable() {
			@Override
			public void call() throws MissingPermissionsException, HTTP429Exception, DiscordException {
				IChannel group = client.getChannelByID(channel);
				if (group == null) {
					IUser user = client.getUserByID(channel);
					if (user == null) {
						return;
					}
					try {
						group = client.getOrCreatePMChannel(user);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}
				if (!client.getOurUser().getName().equals(name)) {
					try {
						client.changeAccountInfo(Optional.of(name), login, password,
								Optional.empty());
					} catch (HTTP429Exception | DiscordException e) {
						// Trivial issue
					}
				}
				IMessage posted = group.sendMessage(message);
				if (channel.equals(getMainChannel()) && !name.equals(BOT_NAME)) {
					StringBuilder builder = new StringBuilder().append("**")
							.append(toEscape.matcher(name).replaceAll("\\\\$1"));
					if (!name.startsWith("* ")) {
						builder.append(':');
					}
					builder.append("** ").append(message);
					pastMainMessages.put(posted, builder.toString());
				}
				try {
					/*
					 * Sleep .75 seconds after posting a message to avoid being rate limited. This
					 * will still get us hit when continuously posting, but it allows for much more
					 * responsive chat. As we're a small server and chat is often calm, I'm not
					 * going to worry about it.
					 */
					Thread.sleep(750);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean retryOnException() {
				return false;
			}
		});
	}

	public void postReport(String message) {
		postMessage(Discord.BOT_NAME, message, getReportChannel());
	}

	public LoadingCache<Object, Object> getAuthCodes() {
		return authentications;
	}

	public UUID getUUIDOf(IUser user) {
		String uuidString = discordData.getString("users." + user.getID());
		if (uuidString == null) {
			return null;
		}
		return UUID.fromString(uuidString);
	}

	public void addLink(UUID uuid, IUser user) {
		discordData.set("users." + user.getID(), uuid.toString());
	}

	public DiscordPlayer getDiscordPlayerFor(IUser user) {
		UUID uuid = getUUIDOf(user);
		if (uuid == null) {
			return null;
		}
		Player player = PlayerLoader.getPlayer(uuid);
		if (player instanceof DiscordPlayer) {
			return (DiscordPlayer) player;
		}
		// PlayerLoader loads a PermissiblePlayer, wrapping a wrapper would be silly.
		DiscordPlayer dplayer = new DiscordPlayer(this, user, player.getPlayer());
		PlayerLoader.modifyCachedPlayer(dplayer);
		return dplayer;
	}

	public boolean handleDiscordCommand(String command, IUser user, IChannel channel) {
		String[] split = command.split("\\s");
		if (!commands.containsKey(split[0])) {
			return false;
		}
		String[] args = new String[split.length - 1];
		if (args.length > 0) {
			System.arraycopy(split, 1, args, 0, args.length);
		}
		commands.get(split[0]).execute(user, channel, command, args);
		return true;
	}

	@Override
	public String getName() {
		return "Discord";
	}

}
