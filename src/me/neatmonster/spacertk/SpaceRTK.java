/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 *
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Handler;
import java.util.logging.Logger;

import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacemodule.api.ActionsManager;
import me.neatmonster.spacertk.actions.FileActions;
import me.neatmonster.spacertk.actions.PluginActions;
import me.neatmonster.spacertk.actions.SchedulerActions;
import me.neatmonster.spacertk.actions.ServerActions;
import me.neatmonster.spacertk.event.BackupEvent;
import me.neatmonster.spacertk.plugins.PluginsManager;
import me.neatmonster.spacertk.scheduler.Scheduler;
import me.neatmonster.spacertk.utilities.backup.BackupManager;
import me.neatmonster.spacertk.utilities.Format;

import org.bukkit.configuration.file.YamlConfiguration;

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventPriority;

/**
 * Main class of SpaceRTK
 */
public class SpaceRTK {
    private static SpaceRTK spaceRTK;

    /**
     * Gets the RTK Instance
     * @return RTK Instance
     */
    public static SpaceRTK getInstance() {
        return spaceRTK;
    }

    public ActionsManager actionsManager;
    public PanelListener  panelListener;
    public PluginsManager pluginsManager;

    public String         type = null;
    public int            port;
    public int            rPort;
    public String         salt;
    public InetAddress    bindAddress;
    public File           worldContainer;
    public String         backupDirName;
    public boolean        backupLogs;
    public SpaceModule    spaceModule;

    private BackupManager backupManager;
    private PingListener pingListener;

    public static final File baseDir = new File(System.getProperty("user.dir"));

    /**
     * Creates a new RTK
     */
    public SpaceRTK() {
        try {
            final Logger rootlog = Logger.getLogger("");
            for (final Handler h : rootlog.getHandlers())
                h.setFormatter(new Format());
            EventDispatcher edt = SpaceModule.getInstance().getEdt();
            edt.registerListener(new BackupListener(), SpaceModule.getInstance().getEventHandler(), ToolkitEventPriority.SYSTEM, BackupEvent.class);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the RTK is disabled
     */
    public void onDisable() {
        try {
            panelListener.stopServer();
            pingListener.shutdown();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the RTK is enabled
     */
    public void onEnable() {
        spaceRTK = this;
        spaceModule = SpaceModule.getInstance();
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(SpaceModule.CONFIGURATION);
        worldContainer = new File(config.getString("General.worldContainer", "."));
        backupDirName = config.getString("General.backupDirectory", "Backups");
        backupLogs = config.getBoolean("General.backupLogs", true);

        bindAddress = spaceModule.bindAddress;
        salt = spaceModule.salt;
        type = spaceModule.type;
        port = spaceModule.port;
        rPort = spaceModule.rPort;

        try {
            config.save(SpaceModule.CONFIGURATION);
        } catch (IOException e) {
            e.printStackTrace();
        }

        pingListener = new PingListener();

        if(backupManager == null)
            backupManager = BackupManager.getInstance();

        File backupDir = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + SpaceRTK.getInstance().backupDirName);
        for(File f : baseDir.listFiles()) {
            if(f.isDirectory()) {
                if(f.getName().equalsIgnoreCase(backupDirName) && !f.getName().equals(backupDirName)) {
                    f.renameTo(backupDir);
                }
            }
        }

        pluginsManager = new PluginsManager();
        actionsManager = new ActionsManager();
        actionsManager.register(FileActions.class);
        actionsManager.register(PluginActions.class);
        actionsManager.register(SchedulerActions.class);
        actionsManager.register(ServerActions.class);
        panelListener = new PanelListener();
        Scheduler.loadJobs();

        pingListener.start();
    }

    /**
     * Gets the Backup Manager
     * @return Backup Manager
     */
    public BackupManager getBackupManager() {
        return backupManager;
    }
}
