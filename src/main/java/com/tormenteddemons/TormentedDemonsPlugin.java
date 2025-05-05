package com.tormenteddemons;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Tormented Demons"
)
public class TormentedDemonsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private TormentedDemonsConfig config;

	@Getter(AccessLevel.PACKAGE)
	private Actor lastOpponent;

	@Inject
	private NPCManager npcManager;

	@Inject
	private Notifier notifier;

	private Integer lastMaxHealth;
	private int lastRatio = 0;
	private int lastHealthScale = 0;

	private boolean threeQuarters = false;
	private boolean twoQuarters = false;
	private boolean oneQuarter = false;

	private Integer ticks;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says aaaaa" + config.greeting(), null);
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		Actor opponent = event.getTarget();

		if (opponent != null && lastOpponent != opponent)
		{
			lastOpponent = opponent;
			threeQuarters = false;
			twoQuarters = false;
			oneQuarter = false;
			ticks = 0;
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		ticks++;
		if (Objects.equals(lastOpponent.getName(), "Tormented Demon"))
		{
			lastMaxHealth = npcManager.getHealth(((NPC) lastOpponent).getId());
			lastRatio = lastOpponent.getHealthRatio();
			lastHealthScale = lastOpponent.getHealthScale();

			int health = 0;
			if (lastRatio > 0)
			{
				int minHealth = 1;
				int maxHealth;
				if (lastHealthScale > 1)
				{
					if (lastRatio > 1)
					{
						// This doesn't apply if healthRatio = 1, because of the special case in the server calculation that
						// health = 0 forces healthRatio = 0 instead of the expected healthRatio = 1
						minHealth = (lastMaxHealth * (lastRatio - 1) + lastHealthScale - 2) / (lastHealthScale - 1);
					}
					maxHealth = (lastMaxHealth * lastRatio - 1) / (lastHealthScale - 1);
					if (maxHealth > lastMaxHealth)
					{
						maxHealth = lastMaxHealth;
					}
				}
				else
				{
					// If healthScale is 1, healthRatio will always be 1 unless health = 0
					// so we know nothing about the upper limit except that it can't be higher than maxHealth
					maxHealth = lastMaxHealth;
				}
				// Take the average of min and max possible healths
				health = (minHealth + maxHealth + 1) / 2;
			}

			if (health <= 455 && !threeQuarters && health != 0)
			{
				notifier.notify("Keisk attacku");
				threeQuarters = true;
			}
			if (health <= 305 && !twoQuarters && health != 0)
			{
				notifier.notify("Keisk attacku");
				twoQuarters = true;
			}
			if (health <= 155 && !oneQuarter && health != 0)
			{
				notifier.notify("Keisk attacku");
				oneQuarter = true;
			}
			if (ticks >= 62)
			{
				notifier.notify("BEK BEK BEK");
				ticks = 0;
			}


			log.info(lastOpponent.getName());
			log.info(String.valueOf(health));
			log.info(String.valueOf(ticks));
		}
	}

	@Provides
	TormentedDemonsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TormentedDemonsConfig.class);
	}
}
