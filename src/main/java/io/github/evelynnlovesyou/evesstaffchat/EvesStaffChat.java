package io.github.evelynnlovesyou.evesstaffchat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.evelynnlovesyou.evesstaffchat.commands.StaffChatCommand;
import io.github.evelynnlovesyou.evesstaffchat.events.PlayerConnectionHandler;
import io.github.evelynnlovesyou.evesstaffchat.events.StaffChatMessageHandler;
import io.github.evelynnlovesyou.evesstaffchat.manager.StaffChatManager;

public class EvesStaffChat implements ModInitializer {
	public static final String MOD_ID = "eves-staff-chat";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initialising " + MOD_ID + " v1.0.0");

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			StaffChatCommand.register(dispatcher);
		});

		// Register event handlers
		PlayerConnectionHandler.register();
		StaffChatMessageHandler.register();

		// Initialize LuckPerms after server has started
		ServerLifecycleEvents.SERVER_STARTED.register(server -> StaffChatManager.init());
	}
}