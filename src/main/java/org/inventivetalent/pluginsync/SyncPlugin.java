package org.inventivetalent.pluginsync;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class SyncPlugin extends JavaPlugin {

	String source      = "https://my.download.site/downloads/";
	String destination = "plugins/update";
	File destinationFile;

	List<String> fileList = new ArrayList<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();

		FileConfiguration config = getConfig();
		source = config.getString("source");
		destination = config.getString("destination","plugins/update");
		destinationFile = new File(destination);
		if (!destinationFile.exists()) {
			destinationFile.mkdirs();
		}
		fileList.addAll(config.getStringList("files"));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if ("download-latest-versions".equalsIgnoreCase(command.getName())) {
			if (sender.hasPermission("pluginsync.admin")) {
				sender.sendMessage("Downloading all updates...");
				downloadAllUpdates(sender);
				return true;
			}
		}

		return false;
	}

	public void downloadAllUpdates(CommandSender sender) {
		AtomicInteger integer = new AtomicInteger(0);
		for (String string : fileList) {
			Bukkit.getScheduler().runTaskAsynchronously(this, () ->{
				downloadUpdate(string);
				if (sender != null) {
					sender.sendMessage("Downloaded " + integer.incrementAndGet() + "/" + fileList.size());
				}
			});
		}
	}

	public boolean downloadUpdate(String name) {
		getLogger().info("Downloading " + source + "/" + name + " to " + destination + "/" + name + "...");
		File outFile = new File(destination, name);
		try {
			URL url = new URL(source + name);
			URLConnection urlConnection =url.openConnection();
			urlConnection.setRequestProperty("User-Agent", "Plugin-Sync");
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			ReadableByteChannel readableByteChannel = Channels.newChannel(urlConnection.getInputStream());
			FileOutputStream fileOutputStream = new FileOutputStream(outFile);
			FileChannel fileChannel = fileOutputStream.getChannel();
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Failed to download " + name, e);
		}
		return false;
	}

}
