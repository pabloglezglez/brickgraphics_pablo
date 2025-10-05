package mosaic.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import mosaic.controllers.MosaicZoomController;
import mosaic.ui.BrickedView;

/**
 * Acción para hacer zoom in en el mosaico.
 */
public class ZoomInAction extends AbstractAction {
    
    private MosaicZoomController zoomController;
    
    public ZoomInAction(MosaicZoomController zoomController) {
        super("Zoom In");
        this.zoomController = zoomController;
        
        putValue(Action.SHORT_DESCRIPTION, "Aumentar zoom del mosaico (Ctrl++)");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK));
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        zoomController.zoomIn();
    }
}