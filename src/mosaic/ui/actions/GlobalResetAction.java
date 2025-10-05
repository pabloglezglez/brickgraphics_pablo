package mosaic.ui.actions;

import mosaic.controllers.StudEditController;
import mosaic.ui.BrickedView;
import colors.LEGOColorGrid;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Acción para hacer un reset global del mosaico.
 */
public class GlobalResetAction extends AbstractAction {
    private StudEditController studEditController;
    private BrickedView brickedView;
    
    public GlobalResetAction(StudEditController studEditController, BrickedView brickedView) {
        super("Reset All");
        this.studEditController = studEditController;
        this.brickedView = brickedView;
        putValue(SHORT_DESCRIPTION, "Reset All - Clear all modifications");
    }
    
    public void setBrickedView(BrickedView brickedView) {
        this.brickedView = brickedView;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (brickedView == null) return;
        
        int result = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to reset all changes? This cannot be undone.",
            "Confirm Reset All",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            LEGOColorGrid colorGrid = brickedView.getColorGrid();
            if (colorGrid != null) {
                studEditController.globalReset(colorGrid);
                brickedView.repaint();
            }
        }
    }
}