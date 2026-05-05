package io.github.evelynnlovesyou.evesstaffchat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.evelynnlovesyou.evesstaffchat.commands.StaffChatCommand;
import io.github.evelynnlovesyou.evesstaffchat.config.ConfigRepository;
import io.github.evelynnlovesyou.evesstaffchat.events.PlayerConnectionHandler;
import io.github.evelynnlovesyou.evesstaffchat.events.StaffChatMessageHandler;
import io.github.evelynnlovesyou.evesstaffchat.manager.PermissionManager;

public class EvesStaffChat implements ModInitializer {
	public static final String MOD_ID = "eves-staff-chat";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		String version = FabricLoader.getInstance()
			.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
		LOGGER.info("eves-staff-chat Initialising {} v{}", MOD_ID, version);

		try {
			ConfigRepository.init();
		} catch (Exception e) {
			LOGGER.error("Failed to load configuration: {}", e.getMessage());
			return;
		}

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StaffChatCommand.register(dispatcher);
		});

		// Register event handlers
		PlayerConnectionHandler.register();
		StaffChatMessageHandler.register();

		// Initialize LuckPerms after server has started
		ServerLifecycleEvents.SERVER_STARTED.register(server -> PermissionManager.init());
	}
}