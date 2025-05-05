package com.tormenteddemons;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.HitsplatApplied;

import java.time.Instant;
import java.util.Objects;
import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
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
	private Integer totalDamage;

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
			totalDamage = 0;
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied)
	{
		Player player = client.getLocalPlayer();
		Actor actor = hitsplatApplied.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}
		Hitsplat hitsplat = hitsplatApplied.getHitsplat();
		if (hitsplat.isMine()) {
			totalDamage += hitsplat.getAmount();
			log.info(String.valueOf(totalDamage));
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

			//if (health <= 455 && !threeQuarters && health != 0)
			if (totalDamage >= 150 && !threeQuarters)
			{
				playSound("switch.wav");
				threeQuarters = true;
			}
			//if (health <= 305 && !twoQuarters && health != 0)
			if (totalDamage >= 300 && !twoQuarters)
			{
				playSound("switch.wav");
				twoQuarters = true;
			}
			//if (health <= 155 && !oneQuarter && health != 0)
			if (totalDamage >= 450 && !oneQuarter)
			{
				playSound("switch.wav");
				oneQuarter = true;
			}
			if (ticks >= 60)
			{
				playSound("run.wav");
				ticks = 0;
			}
		}
	}

	private void playSound(String fileName)
	{
		try (InputStream audioSrc = getClass().getResourceAsStream("/audio/" + fileName))
		{
			if (audioSrc == null)
			{
				log.warn("Sound file not found: " + fileName);
				return;
			}
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioSrc);
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);
			clip.start();
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
		{
			log.error("Error playing sound: " + fileName, e);
		}
	}

	@Provides
	TormentedDemonsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TormentedDemonsConfig.class);
	}
}
