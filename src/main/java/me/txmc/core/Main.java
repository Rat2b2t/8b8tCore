package me.txmc.core;

import lombok.Getter;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.chat.listeners.OpWhiteListListener;
import me.txmc.core.chat.tasks.AnnouncementTask;
import me.txmc.core.command.CommandSection;
import me.txmc.core.customexperience.PlayerPrefix;
import me.txmc.core.customexperience.PlayerSimulationDistance;
import me.txmc.core.customexperience.PlayerViewDistance;
import me.txmc.core.deathmessages.DeathMessageListener;
import me.txmc.core.dupe.DupeSection;
import me.txmc.core.dupe.SalC1Dupe;
import me.txmc.core.home.HomeManager;
import me.txmc.core.patch.PatchSection;
import me.txmc.core.tablist.TabSection;
import me.txmc.core.util.MapCreationLogger;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.vote.VoteSection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

public class Main extends JavaPlugin {
    @Getter private static Main instance;
    @Getter public static String prefix;
    @Getter private ScheduledExecutorService executorService;
    private List<Section> sections;
    private List<Reloadable> reloadables;
    private List<ViolationManager> violationManagers;
    @Getter private long startTime;

    @Override
    public void onEnable() {
        sections = new ArrayList<>();
        reloadables = new ArrayList<>();
        violationManagers = new ArrayList<>();
        instance = this;
        executorService = Executors.newScheduledThreadPool(4);
        startTime = System.currentTimeMillis();
        prefix = getConfig().getString("PluginMessagePrefix", "&6[&18b&98t&cCore&6]");
        saveDefaultConfig();
        getLogger().addHandler(new LoggerHandler());
        Localization.loadLocalizations(getDataFolder());

        executorService.scheduleAtFixedRate(() -> violationManagers.forEach(ViolationManager::decrementAll), 0, 1, TimeUnit.SECONDS);
        getExecutorService().scheduleAtFixedRate(new AnnouncementTask(), 10L, getConfig().getInt("AnnouncementInterval"), TimeUnit.SECONDS);

        register(new TabSection(this));
        register(new ChatSection(this));
        register(new TPASection(this));
        register(new HomeManager(this));
        register(new CommandSection(this));
        register(new PatchSection(this));
        register(new DupeSection(this));
        register(new DeathMessageListener());
        register(new PlayerPrefix(this));
        register(new PlayerViewDistance(this));
        register(new PlayerSimulationDistance(this));
        register(new OpWhiteListListener(this));
        register(new SalC1Dupe(this));

        if(getConfig().getBoolean("AntiIllegal.Enabled", true)) register(new AntiIllegalMain(this));
        if (getServer().getPluginManager().getPlugin("VotifierPlus") != null) register(new VoteSection(this));

        for (Section section : sections) {
            try {
                section.enable();
            } catch (Exception e) {
                log(Level.SEVERE, "Failed to enable section %s. Please see stacktrace below for more info", section.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        violationManagers.clear();
        sections.forEach(Section::disable);
        sections.clear();

        for (Handler handler : MapCreationLogger.getLogger().getHandlers()) {
            handler.close();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        sections.forEach(s -> {
            ConfigurationSection section = getConfig().getConfigurationSection(s.getName());
            if (section != null) s.reloadConfig();
        });
        reloadables.forEach(Reloadable::reloadConfig);
    }

    private void register(Section section) {
        if (getSectionByName(section.getName()) != null)
            throw new IllegalArgumentException("Section has already been registered " + section.getName());
        sections.add(section);
    }

    public void register(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
            if (listener instanceof Reloadable reloadable) reloadables.add(reloadable);
        }
    }

    public void register(ViolationManager manager) {
//        if (violationManagers.contains(manager)) throw new IllegalArgumentException("Attempted to register violation manager twice");
        if (violationManagers.contains(manager)) return;
        violationManagers.add(manager);
        if (manager instanceof Listener) register((Listener) manager);
    }

    public Reloadable registerReloadable(Reloadable reloadable) {
        reloadables.add(reloadable);
        return reloadable;
    }

    public ConfigurationSection getSectionConfig(Section section) {
        return getConfig().getConfigurationSection(section.getName());
    }

    public File getSectionDataFolder(Section section) {
        File dataFolder = new File(getDataFolder(), section.getName());
        if (!dataFolder.exists()) dataFolder.mkdirs();
        return dataFolder;
    }

    public Section getSectionByName(String name) {
        return sections.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
    }
}
