package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Acción para activar la herramienta cuentagotas.
 */
public class EyedropperToolAction extends AbstractAction {
    private StudEditController studEditController;
    
    public EyedropperToolAction(StudEditController studEditController) {
        super("Cuentagotas");
        this.studEditController = studEditController;
        putValue(SHORT_DESCRIPTION, "Cuentagotas - Seleccionar color del mosaico");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        studEditController.setActiveTool(EditTool.EYEDROPPER);
    }
}