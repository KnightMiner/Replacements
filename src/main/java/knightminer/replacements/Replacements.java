package knightminer.replacements;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.ModIdArgument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Mod(Replacements.MOD_ID)
public class Replacements {
	public static final String MOD_ID = "replacements";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private final List<ReplacementRegistry<?>> replacements;
	public Replacements() {
		ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
		replacements = Arrays.asList(
				new ReplacementRegistry<>(configBuilder, "blocks",         Block.class,          () -> ForgeRegistries.BLOCKS),
				new ReplacementRegistry<>(configBuilder, "fluids",         Fluid.class,          () -> ForgeRegistries.FLUIDS),
				new ReplacementRegistry<>(configBuilder, "items",          Item.class,           () -> ForgeRegistries.ITEMS),
				new ReplacementRegistry<>(configBuilder, "effects",        Effect.class,         () -> ForgeRegistries.POTIONS),
				new ReplacementRegistry<>(configBuilder, "potions",        Potion.class,         () -> ForgeRegistries.POTION_TYPES),
				new ReplacementRegistry<>(configBuilder, "enchantments",   Enchantment.class,    () -> ForgeRegistries.ENCHANTMENTS),
				new ReplacementRegistry<>(configBuilder, "entities",       EntityType.class,     () -> ForgeRegistries.ENTITIES),
				new ReplacementRegistry<>(configBuilder, "block_entites",  TileEntityType.class, () -> ForgeRegistries.TILE_ENTITIES));
		ModLoadingContext.get().registerConfig(Type.COMMON, configBuilder.build());
		MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
	}

	/** Registers commands with Minecraft */
	private void registerCommand(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSource> builder = Commands.literal(MOD_ID);
		LiteralArgumentBuilder<CommandSource> dumpCommand = Commands.literal("dump").requires(source -> source.hasPermission(3));
		// dump all
		dumpCommand.then(Commands.literal("all")
														 .then(Commands.argument("mod", ModIdArgument.modIdArgument())
																					 .executes(this::dumpAll)));
		// individual dumps
		for (ReplacementRegistry<?> replacement : replacements) {
			dumpCommand.then(Commands.literal(replacement.getName())
															 .then(Commands.argument("mod", ModIdArgument.modIdArgument())
																						 .executes(context -> replacement.executeDump(context, true))));
		}
		builder.then(dumpCommand);
		// register the command
		event.getDispatcher().register(builder);
	}

	/** Dumps every single supported registry */
	private int dumpAll(CommandContext<CommandSource> context) {
		int count = 0;
		for (ReplacementRegistry<?> replacement : replacements) {
			count += replacement.executeDump(context, false);
		}
		String mod = context.getArgument("mod", String.class);
		context.getSource().sendSuccess(new TranslationTextComponent("command.replacements.dump_all.success", mod), true);
		return count;
	}
}
