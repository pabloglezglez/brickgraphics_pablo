package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Acción para activar la herramienta pincel.
 */
public class BrushToolAction extends AbstractAction {
    private StudEditController studEditController;
    
    public BrushToolAction(StudEditController studEditController) {
        super("Pincel");
        this.studEditController = studEditController;
        putValue(SHORT_DESCRIPTION, "Pincel - Cambiar color de studs individuales");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        studEditController.setActiveTool(EditTool.BRUSH);
    }
}