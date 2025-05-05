package com.tormenteddemons;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TormentedDemonsTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TormentedDemonsPlugin.class);
		RuneLite.main(args);
	}
}