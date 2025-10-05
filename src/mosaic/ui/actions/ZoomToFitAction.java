package mosaic.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import mosaic.controllers.MosaicZoomController;

/**
 * Acción para ajustar el mosaico completo en la ventana.
 */
public class ZoomToFitAction extends AbstractAction {
    
    private MosaicZoomController zoomController;
    
    public ZoomToFitAction(MosaicZoomController zoomController) {
        super("Fit to Window");
        this.zoomController = zoomController;
        
        putValue(Action.SHORT_DESCRIPTION, "Fit complete mosaic in window");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        zoomController.zoomToFit();
    }
}