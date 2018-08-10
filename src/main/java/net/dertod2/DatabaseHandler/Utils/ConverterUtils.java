package net.dertod2.DatabaseHandler.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConverterUtils {

	public static String toString(Location location) {
		if (location == null) return "";
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("world", location.getWorld().getName());
		
		jsonObject.addProperty("x", location.getX());
		jsonObject.addProperty("y", location.getY());
		jsonObject.addProperty("z", location.getZ());
		
		jsonObject.addProperty("yaw", location.getYaw());
		jsonObject.addProperty("pitch", location.getPitch());
		
		return new Gson().toJson(jsonObject);
	}
	
	public static Location toLocation(String string) {
		if (string == null || string.length() <= 0) return null;
		
		JsonObject jsonObject = new JsonParser().parse(string).getAsJsonObject();	
		World world = Bukkit.getWorld(jsonObject.get("world").getAsString());
		
		double x = jsonObject.get("x").getAsDouble();
		double y = jsonObject.get("y").getAsDouble();
		double z = jsonObject.get("z").getAsDouble();
		
		float yaw = jsonObject.get("yaw").getAsFloat();
		float pitch = jsonObject.get("pitch").getAsFloat();
		
		return new Location(world, x, y, z, yaw, pitch);
	}
}
