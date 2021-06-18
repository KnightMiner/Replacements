package knightminer.replacements;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.MissingMappings;
import net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ReplacementRegistry<T extends IForgeRegistryEntry<T>> {
	private final String name;
	private final Supplier<IForgeRegistry<T>> registrySupplier;
	private final ConfigValue<List<? extends String>> configList;
	private final Map<ResourceLocation,T> remap = new HashMap<>();

	public ReplacementRegistry(ForgeConfigSpec.Builder config, String name, Class<? super T> classType, Supplier<IForgeRegistry<T>> registrySupplier) {
		this.name = name;
		this.registrySupplier = registrySupplier;
		this.configList = config.comment("IDs to remap on missing mappings, format is 'old_mod:old_id=new_mod:new_id")
														.defineList(name, Collections.emptyList(), this::isValid);

		// add listeners
		MinecraftForge.EVENT_BUS.addGenericListener(classType, this::onMissingMappings);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::configChanged);
	}

	/** Gets the name of this replacement */
	public String getName() {
		return name;
	}

	/**
	 * Ensures the given array object is valid
	 */
	private boolean isValid(Object value) {
		if (!(value instanceof String)) {
			return false;
		}
		return isValid(((String) value).split("="));
	}

	/**
	 * Ensures the given string is valid
	 */
	private boolean isValid(String[] parts) {
		if (parts.length != 2) {
			return false;
		}
		ResourceLocation oldName = ResourceLocation.tryParse(parts[0]);
		if (oldName == null) {
			return false;
		}
		ResourceLocation newName = ResourceLocation.tryParse(parts[1]);
		if (newName == null) {
			return false;
		}
		return registrySupplier.get().containsKey(newName);
	}

	/**
	 * Called when the config changes
	 */
	private void configChanged(final ModConfigEvent event) {
		if (Replacements.MOD_ID.equals(event.getConfig().getModId())) {
			List<? extends String> strings = configList.get();
			remap.clear();

			// fetch all new values from the config
			IForgeRegistry<T> registry = registrySupplier.get();
			for (String string : strings) {
				String[] parts = string.split("=");
				if (isValid(parts)) {
					remap.put(new ResourceLocation(parts[0]), registry.getValue(new ResourceLocation(parts[1])));
				}
			}
		}
	}

	/**
	 * Called when mappings are missing in the world
	 */
	private void onMissingMappings(MissingMappings<T> event) {
		if (!remap.isEmpty()) {
			event.getAllMappings().forEach(this::handleMapping);
		}
	}

	/**
	 * Handles the given missing mapping
	 */
	private void handleMapping(Mapping<T> mapping) {
		T value = remap.get(mapping.key);
		if (value != null) {
			mapping.remap(value);
		}
	}

	/** Executes the dump command, printing all values */
	public int executeDump(CommandContext<CommandSource> context, boolean printSuccess) {
		String mod = context.getArgument("mod", String.class);
		int count = 0;
		StringBuilder output = new StringBuilder();
		for (ResourceLocation key : registrySupplier.get().getKeys()) {
			if (mod.equals(key.getNamespace())) {
				count++;
				output.append(key.toString());
				output.append(System.lineSeparator());
			}
		}

		// print results
		Replacements.LOGGER.info("Mod dump for registry {}: {}{}", name, System.lineSeparator(), output.toString());
		if (printSuccess) {
			context.getSource().sendSuccess(new TranslationTextComponent("command.replacements.dump.success", name, mod), true);
		}
		return count;
	}
}
