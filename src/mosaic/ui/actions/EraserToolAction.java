package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Acción para activar la herramienta borrador.
 */
public class EraserToolAction extends AbstractAction {
    private StudEditController studEditController;
    
    public EraserToolAction(StudEditController studEditController) {
        super("Eraser");
        this.studEditController = studEditController;
        putValue(SHORT_DESCRIPTION, "Eraser - Restore individual studs to original");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        studEditController.setActiveTool(EditTool.ERASER);
    }
}