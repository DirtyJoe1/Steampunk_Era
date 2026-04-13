package com.steampunkera;

import com.steampunkera.block.ItemPipeBlock;
import com.steampunkera.block.entity.ItemPipeBlockEntity;
import com.steampunkera.item.ServoItem;
import com.steampunkera.item.WrenchItem;
import com.steampunkera.network.ServoPayload;
import com.steampunkera.util.ServoMenuData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.component.ComponentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SteampunkEra implements ModInitializer {
	public static final String MOD_ID = "steampunk-era";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Set<ItemPipeBlockEntity> TICKING_PIPES = ConcurrentHashMap.newKeySet();

	public static final ComponentType<NbtCompound> PIPE_DATA_COMPONENT = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			id("pipe_data"),
			ComponentType.<NbtCompound>builder()
					.codec(NbtCompound.CODEC)
					.packetCodec(net.minecraft.network.codec.PacketCodec.of(
							(nbt, buf) -> buf.writeNbt(nbt),
							buf -> buf.readNbt()
					))
					.build()
	);

	public static final Item WRENCH = Registry.register(
			Registries.ITEM,
			id("wrench"),
			new WrenchItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, id("wrench"))))
	);

	public static final Item SERVO = Registry.register(
			Registries.ITEM,
			id("servo"),
			new ServoItem(new Item.Settings()
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, id("servo"))))
	);

	public static final Item SERVOS_ITEM = SERVO;

	public static final Block ITEM_PIPE = registerBlock(
			"item_pipe",
			new ItemPipeBlock(AbstractBlock.Settings.create()
					.mapColor(net.minecraft.block.MapColor.IRON_GRAY)
					.registryKey(RegistryKey.of(RegistryKeys.BLOCK, id("item_pipe"))))
	);

	public static final BlockEntityType<ItemPipeBlockEntity> ITEM_PIPE_BLOCK_ENTITY_TYPE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			id("item_pipe"),
			FabricBlockEntityTypeBuilder.create(ItemPipeBlockEntity::new, ITEM_PIPE).build(null)
	);

	public static final RegistryKey<ItemGroup> STEAMPUNK_TAB_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, id("steampunk_tab"));
	public static final ItemGroup STEAMPUNK_TAB = Registry.register(
			Registries.ITEM_GROUP,
			id("steampunk_tab"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(WRENCH))
					.displayName(Text.translatable("itemGroup.steampunk-era.steampunk_tab"))
					.entries((displayContext, entries) -> {
						entries.add(ITEM_PIPE);
						entries.add(WRENCH);
						entries.add(SERVO);
					})
					.build()
	);

	private static Block registerBlock(String name, Block block) {
		var blockKey = RegistryKey.of(RegistryKeys.BLOCK, id(name));
		Registry.register(Registries.BLOCK, blockKey, block);

		var itemKey = RegistryKey.of(RegistryKeys.ITEM, id(name));
		var blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
		Registry.register(Registries.ITEM, itemKey, blockItem);

		return block;
	}

	private static net.minecraft.util.Identifier id(String path) {
		return net.minecraft.util.Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		ServoMenuData.init();
		ServoPayload.register();

		ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
			TICKING_PIPES.clear();
		});

		ServerTickEvents.START_SERVER_TICK.register((MinecraftServer server) -> {
			for (ItemPipeBlockEntity pipeBE : TICKING_PIPES) {
				pipeBE.tick(server.getOverworld());
			}
		});
	}
}
