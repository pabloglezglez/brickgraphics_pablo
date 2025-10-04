package mosaic.ui.menu;

import javax.swing.*;
import mosaic.controllers.MosaicZoomController;
import mosaic.ui.actions.*;

/**
 * Menú de zoom para el mosaico.
 */
public class ZoomMenu extends JMenu {
    
    public ZoomMenu(MosaicZoomController zoomController) {
        super("Zoom");
        setMnemonic('Z');
        setDisplayedMnemonicIndex(0);
        
        // Acciones de zoom
        add(new ZoomInAction(zoomController));
        add(new ZoomOutAction(zoomController));
        addSeparator();
        add(new ZoomToFitAction(zoomController));
        add(new ZoomToActualSizeAction(zoomController));
        addSeparator();
        
        // Submenu con niveles específicos
        JMenu levelsMenu = new JMenu("Nivel Específico");
        levelsMenu.setMnemonic('N');
        
        for (int i = 0; i < MosaicZoomController.ZOOM_LEVELS.length; i++) {
            final int index = i;
            double level = MosaicZoomController.ZOOM_LEVELS[i];
            String percentage = Math.round(level * 100) + "%";
            
            JMenuItem levelItem = new JMenuItem(percentage);
            levelItem.addActionListener(e -> zoomController.setZoomIndex(index));
            
            levelsMenu.add(levelItem);
        }
        
        add(levelsMenu);
    }
}