package mosaic.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import mosaic.controllers.MosaicZoomController;

/**
 * Acción para zoom al tamaño actual (100%).
 */
public class ZoomToActualSizeAction extends AbstractAction {
    
    private MosaicZoomController zoomController;
    
    public ZoomToActualSizeAction(MosaicZoomController zoomController) {
        super("Tamaño Real (100%)");
        this.zoomController = zoomController;
        
        putValue(Action.SHORT_DESCRIPTION, "Zoom al tamaño real del mosaico");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK));
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_A);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        zoomController.zoomToActualSize();
    }
}