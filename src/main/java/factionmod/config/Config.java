package factionmod.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import factionmod.FactionMod;
import factionmod.handler.EventHandlerChunk;
import factionmod.manager.instanciation.Zone;
import factionmod.utils.ServerUtils;

/**
 * The configuration of the mod, it loads and saves the state of the mod.
 * 
 * @author BrokenSwing
 *
 */
public class Config {

    public static int immunityLevel;
    public static int factionDescriptionMaxLength;
    public static int factionNameMaxLength;
    public static int maxFactionDamages;
    public static int damagesNeededToCounterClaim;
    public static int teleportationDelay;

    /**
     * Loads a list of {@link Zone} from a file.
     * 
     * @param fileName
     *            The name of the file
     */
    public static final void loadZones(String fileName) {
        ServerUtils.getProfiler().startSection("loadZones");

        Zone z;
        JsonElement file = getFile(fileName);
        if (file != null) {
            JsonArray zones = file.getAsJsonArray();
            for(int i = 0; i < zones.size(); i++) {
                JsonObject zone = zones.get(i).getAsJsonObject();
                String name = zone.get("name").getAsString();
                String clazz = zone.get("class").getAsString();
                JsonElement element = zone.get("instance");
                try {
                    if (element != null) {
                        String instance = element.getAsString();
                        z = new Zone(name, clazz, instance);
                    } else {
                        z = new Zone(name, clazz);
                    }
                    EventHandlerChunk.registerZone(z);
                } catch (Exception e) {
                    FactionMod.getLogger().error("Couldn't load zone " + name);
                    e.printStackTrace();
                }
            }
        }

        ServerUtils.getProfiler().endSection();
    }

    /**
     * Loads all the parameters from the configuration file.
     */
    public static void loadConfigFile() {
        ServerUtils.getProfiler().startSection("loadConfiguration");

        JsonElement element = getFile("configuration.json");
        if(element == null)
            element = new JsonObject();
        
        JsonObject expObj = new JsonObject();
        JsonObject languageObj = new JsonObject();
        JsonArray chestArray = new JsonArray();
        
        if (element.isJsonObject()) {
            JsonElement el;
            JsonObject root = element.getAsJsonObject();
            immunityLevel = ConfigExperience.getInt("immunity_level", root, 5);
            factionDescriptionMaxLength = ConfigExperience.getInt("factionDescriptionMaxLength", root, 50);
            factionNameMaxLength = ConfigExperience.getInt("factionNameMaxLength", root, 15);
            maxFactionDamages = ConfigExperience.getInt("maxFactionDamages", root, 15);
            damagesNeededToCounterClaim = ConfigExperience.getInt("damagesNeededToCounterClaim", root, 5);
            teleportationDelay = ConfigExperience.getInt("teleportationDelay", root, 10);
            if (root.has("exp")) {
                el = root.get("exp");
                if (el.isJsonObject()) {
                    expObj = el.getAsJsonObject();
                }
            }
            if (root.has("language")) {
                el = root.get("language");
                if (el.isJsonObject()) {
                    languageObj = el.getAsJsonObject();
                }
            }
            if (root.has("unvalidItems")) {
                el = root.get("unvalidItems");
                if (el.isJsonArray()) {
                    chestArray = el.getAsJsonArray();
                }
            }
        }
        ConfigExperience.loadFromJson(expObj);
        ConfigLang.loadFromJson(languageObj);
        ConfigFactionInventory.load(chestArray);

        ServerUtils.getProfiler().endSection();
    }

    /**
     * Loads a file from the config directory. Will return null if the file
     * doesn't exist.
     * 
     * @param fileName
     *            The name of the file
     * @return a {@link JsonElement} or null if the file doesn't exist
     */
    private static JsonElement getFile(String fileName) {
        File file = new File(FactionMod.getConfigDir(), fileName);
        if (file.exists()) {
            try {
                return new JsonParser().parse(new FileReader(file));
            } catch (Exception e) {
                FactionMod.getLogger().error("Error on loading file " + fileName);
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Writes text in a file.
     * 
     * @param fileName
     *            The name of the file
     * @param contents
     *            The text to write
     */
    private static void writeFile(String fileName, String contents) {
        File file = new File(FactionMod.getConfigDir() + "/" + fileName);
        try {
            file.createNewFile();

            FileWriter writer = new FileWriter(file);
            writer.write(contents);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            FactionMod.getLogger().error("Error on writing file " + fileName);
            e.printStackTrace();
        }
    }

    /**
     * Creates the directory containing the configuration files of the mod.
     * Creates the files zones.json if it doesn't exist.
     */
    public static void initDirectory() {
        File dir = new File(FactionMod.getConfigDir());
        if (!dir.exists()) {
            dir.mkdirs();
            File file = new File(FactionMod.getConfigDir() + "/zones.json");
            if (!file.exists()) {
                ServerUtils.getProfiler().startSection("generateDefaultZonesFile");
                JsonArray root = new JsonArray();
                JsonObject safeZone = new JsonObject();
                safeZone.add("name", new JsonPrimitive("safe"));
                safeZone.add("class", new JsonPrimitive("factionmod.manager.ManagerSafeZone"));
                safeZone.add("instance", new JsonPrimitive("DEFAULT"));

                JsonObject factionZone = new JsonObject();
                factionZone.add("name", new JsonPrimitive("faction"));
                factionZone.add("class", new JsonPrimitive("factionmod.manager.ManagerFaction"));

                JsonObject warZone = new JsonObject();
                warZone.add("name", new JsonPrimitive("war"));
                warZone.add("class", new JsonPrimitive("factionmod.manager.ManagerWarZone"));
                warZone.add("instance", new JsonPrimitive("DEFAULT"));

                root.add(safeZone);
                root.add(warZone);
                root.add(factionZone);

                writeFile("zones.json", root.toString());
                ServerUtils.getProfiler().endSection();
            }
            file = new File(FactionMod.getConfigDir(), "configuration.json");
            if (!file.exists()) {
                writeFile("configuration.json", new JsonObject().toString());
            }
        }
    }

}
