package org.fotum.app.objects;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BotUtils
{
	public static void saveConfigs()
	{
		BotUtils.serializeObject("listening_channels.dat", SiegeManager.getInstance().getListeningChannels());
		BotUtils.serializeObject("managing_roles.dat", SiegeManager.getInstance().getManagingRoles());
		BotUtils.serializeObject("prefix_roles.dat", SiegeManager.getInstance().getPrefixRoles());
	}

	public static void loadConfigs()
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
				ObjectOutputStream oos = new ObjectOutputStream(fos)
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
				ObjectInputStream ois = new ObjectInputStream(fis)
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
		Iterator<Long> instancesIter = instances.keySet().iterator();

		while (instancesIter.hasNext())
		{
			Long guildId = instancesIter.next();
			SiegeManager.getInstance().removeSiegeInstance(guildId);
		}
	}
}
