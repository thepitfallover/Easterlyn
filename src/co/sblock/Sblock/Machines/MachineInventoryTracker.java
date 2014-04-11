package co.sblock.Sblock.Machines;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_7_R2.Container;
import net.minecraft.server.v1_7_R2.ContainerAnvil;
import net.minecraft.server.v1_7_R2.ContainerMerchant;
import net.minecraft.server.v1_7_R2.EntityHuman;
import net.minecraft.server.v1_7_R2.EntityPlayer;
import net.minecraft.server.v1_7_R2.IMerchant;
import net.minecraft.server.v1_7_R2.ItemStack;
import net.minecraft.server.v1_7_R2.MerchantRecipe;
import net.minecraft.server.v1_7_R2.MerchantRecipeList;
import net.minecraft.server.v1_7_R2.PacketDataSerializer;
import net.minecraft.util.io.netty.buffer.Unpooled;

import org.bukkit.craftbukkit.v1_7_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import co.sblock.Sblock.Events.Packets.WrapperPlayServerCustomPayload;
import co.sblock.Sblock.Events.Packets.WrapperPlayServerOpenWindow;
import co.sblock.Sblock.Machines.Type.Machine;

import com.comphenix.protocol.ProtocolLibrary;

/**
 * brb going insane because of NBT
 * 
 * @author Jikoo
 */
public class MachineInventoryTracker {

	private static MachineInventoryTracker instance;

	private Map<Player, Machine> openMachines;

	public MachineInventoryTracker() {
		openMachines = new HashMap<>();
	}

	public boolean hasMachineOpen(Player p) {
		return openMachines.containsKey(p);
	}

	public Machine getOpenMachine(Player p) {
		return openMachines.get(p);
	}

	public void closeMachine(Player p) {
		openMachines.remove(p);
	}

	public void openMachineInventory(Player player, Machine m, InventoryType it, org.bukkit.inventory.ItemStack... items) {
		// Opens a real anvil window for the Player in question
		WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow();
		packet.setInventoryType(it);
		packet.setWindowTitle(m.getType().getFriendlyName());
		packet.setTitleExact(true);

		EntityPlayer p = ((CraftPlayer) player).getHandle();

		// tick container counter - otherwise server will be confused by slot numbers
		packet.setWindowId((byte) p.nextContainerCounter());

		Container container;
		switch (it) {
		case ANVIL:
			container = new AnvilContainer(p, m.getKey());
			break;
		case MERCHANT:
			container = new MerchantContainer(p);
			break;
		default:
			return;
		}

		p.activeContainer = container;
		p.activeContainer.windowId = packet.getWindowId();
		container.addSlotListener(p);

		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet.getHandle());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return;
		}

		this.openMachines.put(player, m);

		if (!(container instanceof MerchantContainer)) {
			return;
		}
		MerchantRecipeList list = new MerchantRecipeList();
		for (int i = 0; i < items.length; i++) {
			if (i % 3 == 0 && items.length - i > 2) {
				list.a(new MerchantRecipe(org.bukkit.craftbukkit.v1_7_R2.inventory.CraftItemStack.asNMSCopy(items[i]),
						org.bukkit.craftbukkit.v1_7_R2.inventory.CraftItemStack.asNMSCopy(items[i+1]),
						org.bukkit.craftbukkit.v1_7_R2.inventory.CraftItemStack.asNMSCopy(items[i+2])));
			}
		}
		try {

			PacketDataSerializer out = new PacketDataSerializer(Unpooled.buffer());
			out.writeInt(packet.getWindowId());
			list.a(out);

			WrapperPlayServerCustomPayload trades = new WrapperPlayServerCustomPayload();
			trades.setChannel("MC|TrList");
			trades.setData(out.array());
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, trades.getHandle());

			

		} catch (IllegalArgumentException | SecurityException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	
	public class MerchantContainer extends ContainerMerchant {

		public MerchantContainer(EntityPlayer player) {
			super(player.inventory, new FakeMerchant(player), player.world);
			this.checkReachable = false;
		}
	}

	public class FakeMerchant implements IMerchant {
		private EntityHuman customer;

		public FakeMerchant(EntityHuman customer) {
			this.customer = customer;
		}

		@Override
		public void a_(EntityHuman paramEntityHuman) {
			this.customer = paramEntityHuman;
		}

		@Override
		public EntityHuman b() {
			return customer;
		}

		@Override
		public MerchantRecipeList getOffers(EntityHuman paramEntityHuman) {
			return new MerchantRecipeList();
		}

		@Override
		public void a(MerchantRecipe paramMerchantRecipe) {
			// adds a trade
		}

		@Override
		public void a_(ItemStack paramItemStack) {
			// reduces remaining trades and makes yes/no noises
		}
	}

	public class AnvilContainer extends ContainerAnvil {

		public AnvilContainer(net.minecraft.server.v1_7_R2.EntityHuman entity, Location l) {
			super(entity.inventory, ((CraftWorld) l.getWorld()).getHandle(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), entity);
		}

		@Override
		public boolean a(EntityHuman entityhuman) {
			return true;
		}
	}

	public static MachineInventoryTracker getTracker() {
		if (instance == null) {
			instance = new MachineInventoryTracker();
		}
		return instance;
	}
}