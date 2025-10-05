package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Acción para activar la herramienta restaurar.
 */
public class RestoreToolAction extends AbstractAction {
    private StudEditController studEditController;
    
    public RestoreToolAction(StudEditController studEditController) {
        super("Restore");
        this.studEditController = studEditController;
        putValue(SHORT_DESCRIPTION, "Restore - Restore area to original state");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        studEditController.setActiveTool(EditTool.RESTORE);
    }
}