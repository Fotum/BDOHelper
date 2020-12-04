package org.fotum.app.objects;

import java.time.Instant;

import org.fotum.app.MainApp;

import net.dv8tion.jda.api.EmbedBuilder;

public class EmbedCreator
{	
	public static EmbedBuilder getDefault()
	{
		return new EmbedBuilder()
				.setColor(MainApp.getRandomColor())
				.setFooter("{BDOHelper}", null)
				.setTimestamp(Instant.now());
	}
}
