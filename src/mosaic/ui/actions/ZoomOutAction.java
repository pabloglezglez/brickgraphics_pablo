package mosaic.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import mosaic.controllers.MosaicZoomController;

/**
 * Acción para hacer zoom out en el mosaico.
 */
public class ZoomOutAction extends AbstractAction {
    
    private MosaicZoomController zoomController;
    
    public ZoomOutAction(MosaicZoomController zoomController) {
        super("Zoom Out");
        this.zoomController = zoomController;
        
        putValue(Action.SHORT_DESCRIPTION, "Reducir zoom del mosaico (Ctrl+-)");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        zoomController.zoomOut();
    }
}