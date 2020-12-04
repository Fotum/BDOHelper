package org.fotum.app.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.json.JSONArray;
import org.json.JSONObject;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class BotUtils
{
	public static void saveConfigs()
	{
		BotUtils.serializeObject("listening_channels.dat", SiegeManager.getInstance().getListeningChannels());
		BotUtils.serializeObject("managing_roles.dat", SiegeManager.getInstance().getManagingRoles());
		BotUtils.saveInstancesToJSON("scheduler_settings.json");
	}

	public static void loadConfigs(JDA jda)
	{
		Map<Long, Long> channels = BotUtils.deserializeObject("listening_channels.dat");
		if (channels != null)
			SiegeManager.getInstance().setListeningChannels(channels);

		Map<Long, Long> roles = BotUtils.deserializeObject("managing_roles.dat");
		if (roles != null)
			SiegeManager.getInstance().setManagingRoles(roles);

		Map<Long, SiegeInstance> instances = BotUtils.loadInstancesFromJSON("scheduler_settings.json", jda);
		SiegeManager.getInstance().setSiegeInstances(instances);
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
	
	private static void saveInstancesToJSON(String fileNm)
	{
		JSONArray guildObjs = new JSONArray();
		Map<Long, SiegeInstance> instances = SiegeManager.getInstance().getSiegeInstances();
		for (Entry<Long, SiegeInstance> entry : instances.entrySet())
		{
			Long guildId = entry.getKey();
			SiegeInstance inst = entry.getValue();
			inst.unschedule();
			inst.setMessageToEdit(null);

			JSONObject guildObj = new JSONObject();
			guildObj.put("guild_id", guildId);
			guildObj.put("start_dt", inst.getStartDt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
			guildObj.put("players_max", inst.getPlayersMax());
			guildObj.put("zone", inst.getZone());

			guildObj.put("title_message", inst.getTitleMessage());
			guildObj.put("description_message", inst.getDescriptionMessage());
			guildObj.put("announcer_delay", inst.getAnnouncerDelay());
			guildObj.put("announcer_offset", inst.getAnnouncerOffset());
			guildObj.put("channel", inst.getChannel().getIdLong());

			guildObj.put("registred_players", new JSONArray(inst.getRegistredPlayers()));
			guildObj.put("late_players", new JSONArray(inst.getLatePlayers()));
			guildObj.put("prefix_roles", new JSONArray(inst.getPrefixRoles()));

			guildObjs.put(guildObj);
		}
		
		File outFile = new File(Constants.SETTINGS_LOC + File.separator + fileNm);
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))
		{
			writer.write(guildObjs.toString(4));
			writer.flush();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private static Map<Long, SiegeInstance> loadInstancesFromJSON(String fileNm, JDA jda)
	{
		HashMap<Long, SiegeInstance> instances = new HashMap<Long, SiegeInstance>();
		File inFile = new File(Constants.SETTINGS_LOC + File.separator + fileNm);
		if (!inFile.exists())
			return instances;

		String content = "";
		try (FileReader fr = new FileReader(inFile, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(fr))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				content += line;
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			return instances;
		}


		JSONArray root = new JSONArray(content);
		for (int i = 0; i < root.length(); i++)
		{
			JSONObject guildObj = root.getJSONObject(i);
			Long guildId = guildObj.optLong("guild_id");
			SiegeInstance inst = new SiegeInstance();

			inst.setStartDt(LocalDate.parse(guildObj.optString("start_dt"), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
			inst.setPlayersMax(guildObj.optInt("players_max"));
			inst.setZone(guildObj.optString("zone"));
			inst.setTitleMessage(guildObj.optString("title_message"));
			inst.setDescriptionMessage(guildObj.optString("description_message"));
			inst.setAnnouncerDelay(guildObj.optInt("announcer_delay"));
			inst.setAnnouncerOffset(guildObj.optInt("announcer_offset"));
			
			JSONArray registred = guildObj.optJSONArray("registred_players");
			Set<Long> registredSet = new LinkedHashSet<Long>();
			for (int j = 0; j < registred.length(); j++)
			{
				registredSet.add(registred.optLong(j));
			}
			inst.setRegistredPlayers(registredSet);

			JSONArray late = guildObj.optJSONArray("late_players");
			Set<Long> lateSet = new LinkedHashSet<Long>();
			for (int j = 0; j < late.length(); j++)
			{
				lateSet.add(late.optLong(j));
			}
			inst.setLatePlayers(lateSet);
			
			JSONArray prefRoles = guildObj.optJSONArray("prefix_roles");
			Set<Long> rolesSet = new LinkedHashSet<Long>();
			for (int j = 0; j < prefRoles.length(); j++)
			{
				rolesSet.add(prefRoles.optLong(j));
			}
			inst.setPrefixRoles(rolesSet);

			Guild guild = jda.getGuildById(guildId);
			TextChannel channel = guild.getTextChannelById(guildObj.optLong("channel"));
			inst.setChannel(channel);

			instances.put(guildId, inst);
			inst.schedule();
		}
		
		return instances;
	}
}
