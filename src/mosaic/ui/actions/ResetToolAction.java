package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * Acción para activar la herramienta reset.
 */
public class ResetToolAction extends AbstractAction {
    private StudEditController studEditController;
    
    public ResetToolAction(StudEditController studEditController) {
        super("Reset Single");
        this.studEditController = studEditController;
        putValue(SHORT_DESCRIPTION, "Reset Single - Clear individual stud");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        studEditController.setActiveTool(EditTool.RESET);
    }
}