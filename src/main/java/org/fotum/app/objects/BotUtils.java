package org.fotum.app.objects;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;

import net.dv8tion.jda.api.JDA;

public class BotUtils
{
	public static void saveConfigs()
	{
		BotUtils.serializeObject("listening_channels.dat", SiegeManager.getInstance().getListeningChannels());
		BotUtils.serializeObject("managing_roles.dat", SiegeManager.getInstance().getManagingRoles());
		BotUtils.serializeObject("prefix_roles.dat", SiegeManager.getInstance().getPrefixRoles());
	}

	public static void loadConfigs(JDA jda)
	{
		Map<Long, Long> channels = BotUtils.deserializeObject("listening_channels.dat");
		if (channels != null)
			SiegeManager.getInstance().setListeningChannels(channels);

		Map<Long, Long> roles = BotUtils.deserializeObject("managing_roles.dat");
		if (roles != null)
			SiegeManager.getInstance().setManagingRoles(roles);

		Map<Long, Set<Long>> prefixes = BotUtils.deserializeObject("prefix_roles.dat");
		if (prefixes != null)
			SiegeManager.getInstance().setPrefixRoles(prefixes);
	}

	public static <T> void serializeObject(String fileNm, T obj)
	{
		if (obj == null)
			return;
		
		File outFile = new File(Constants.SETTINGS_LOC + File.separator + fileNm);
		try (FileOutputStream fos = new FileOutputStream(outFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
			)
		{
			oos.writeObject(obj);
			oos.flush();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserializeObject(String fileNm)
	{
		File inFile = new File(Constants.SETTINGS_LOC + File.separator + fileNm);
		if (!inFile.exists())
		{
			return null;
		}
		
		T obj = null;
		try (FileInputStream fis = new FileInputStream(inFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
			)
		{
			obj = (T) ois.readObject();
		}
		catch (IOException | ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		
		return obj;
	}
	
	public static void shutdownInstances()
	{
		Map<Long, SiegeInstance> instances = SiegeManager.getInstance().getSiegeInstances();
		for (Long guildId : instances.keySet())
		{
			SiegeManager.getInstance().removeSiegeInstance(guildId);
		}
	}
}
