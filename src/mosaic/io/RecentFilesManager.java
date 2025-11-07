package mosaic.io;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import icon.Icons;
import mosaic.controllers.MainController;
import mosaic.ui.MainWindow;

/**
 * Manages the list of recently opened files and provides a menu for quick access.
 * @author GitHub Copilot
 */
public class RecentFilesManager {
    private static final String PREF_KEY = "recent_files";
    private static final String PREF_SEPARATOR = "|";
    private static final int MAX_RECENT_FILES = 10;
    
    private LinkedList<String> recentFiles;
    private Preferences preferences;
    private MainController mc;
    private MainWindow mw;
    
    public RecentFilesManager(MainController mc, MainWindow mw) {
        this.mc = mc;
        this.mw = mw;
        this.preferences = Preferences.userNodeForPackage(RecentFilesManager.class);
        this.recentFiles = new LinkedList<>();
        loadRecentFiles();
    }
    
    /**
     * Adds a file to the recent files list.
     * @param file The file to add
     */
    public void addRecentFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        String filePath = file.getAbsolutePath();
        
        // Remove if already exists to avoid duplicates
        recentFiles.remove(filePath);
        
        // Add to the beginning
        recentFiles.addFirst(filePath);
        
        // Limit the number of recent files
        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.removeLast();
        }
        
        saveRecentFiles();
    }
    
    /**
     * Creates a menu for recent files.
     * @return JMenu for recent files
     */
    public JMenu createRecentFilesMenu() {
        final JMenu recentMenu = new JMenu("Open Recent");
        recentMenu.setMnemonic('R');
        recentMenu.setIcon(Icons.get(16, "document-open-recent", "RECENT"));
        
        // Add menu listener to populate menu when opened
        recentMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                populateRecentFilesMenu(recentMenu);
            }
            
            @Override
            public void menuDeselected(MenuEvent e) {
                // No action needed
            }
            
            @Override
            public void menuCanceled(MenuEvent e) {
                // No action needed
            }
        });
        
        // Initial population
        populateRecentFilesMenu(recentMenu);
        
        return recentMenu;
    }
    
    /**
     * Populates the recent files menu with current recent files.
     */
    private void populateRecentFilesMenu(JMenu recentMenu) {
        recentMenu.removeAll();
        
        if (recentFiles.isEmpty()) {
            JMenuItem noRecentFiles = new JMenuItem("No recent files");
            noRecentFiles.setEnabled(false);
            recentMenu.add(noRecentFiles);
            return;
        }
        
        // Add recent files
        for (int i = 0; i < recentFiles.size(); i++) {
            final String filePath = recentFiles.get(i);
            File file = new File(filePath);
            
            // Skip if file no longer exists
            if (!file.exists()) {
                continue;
            }
            
            // Create menu item
            String displayName = file.getName();
            if (displayName.length() > 30) {
                displayName = displayName.substring(0, 27) + "...";
            }
            
            JMenuItem menuItem = new JMenuItem((i + 1) + " " + displayName);
            menuItem.setToolTipText(filePath);
            
            // Set icon based on file extension
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".kvm")) {
                menuItem.setIcon(Icons.get(16, "mosaic", "MOSAIC"));
            } else {
                menuItem.setIcon(Icons.get(16, "image", "IMAGE"));
            }
            
            // Add action listener
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openRecentFile(filePath);
                }
            });
            
            recentMenu.add(menuItem);
        }
        
        // Add separator and clear menu item
        if (!recentFiles.isEmpty()) {
            recentMenu.addSeparator();
            JMenuItem clearItem = new JMenuItem("Clear Recent Files");
            clearItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearRecentFiles();
                }
            });
            recentMenu.add(clearItem);
        }
    }
    
    /**
     * Opens a recent file.
     */
    private void openRecentFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(mw, 
                "File no longer exists: " + filePath, 
                "File Not Found", 
                JOptionPane.WARNING_MESSAGE);
            removeRecentFile(filePath);
            return;
        }
        
        try {
            MosaicIO.load(mc, file);
        } catch (Exception ex) {
            String message = "An error occurred while opening file " + file.getName() + "\n" + ex.getMessage();
            JOptionPane.showMessageDialog(mw, message, "Error when opening file", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Removes a file from the recent files list.
     */
    private void removeRecentFile(String filePath) {
        recentFiles.remove(filePath);
        saveRecentFiles();
    }
    
    /**
     * Clears all recent files.
     */
    private void clearRecentFiles() {
        recentFiles.clear();
        saveRecentFiles();
    }
    
    /**
     * Loads recent files from preferences.
     */
    private void loadRecentFiles() {
        String recentFilesStr = preferences.get(PREF_KEY, "");
        if (!recentFilesStr.isEmpty()) {
            String[] files = recentFilesStr.split("\\" + PREF_SEPARATOR);
            for (String file : files) {
                if (!file.trim().isEmpty()) {
                    recentFiles.add(file.trim());
                }
            }
        }
    }
    
    /**
     * Saves recent files to preferences.
     */
    private void saveRecentFiles() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recentFiles.size(); i++) {
            if (i > 0) {
                sb.append(PREF_SEPARATOR);
            }
            sb.append(recentFiles.get(i));
        }
        preferences.put(PREF_KEY, sb.toString());
        
        try {
            preferences.flush();
        } catch (Exception e) {
            // Ignore errors when saving preferences
        }
    }
}