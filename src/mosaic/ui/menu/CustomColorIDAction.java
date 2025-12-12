package mosaic.ui.menu;

import mosaic.controllers.ColorController;
import mosaic.ui.CustomColorIDDialog;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Acción para abrir la ventana de gestión de números personalizados de colores
 */
public class CustomColorIDAction extends JMenuItem {
    
    private ColorController colorController;
    
    public CustomColorIDAction(ColorController colorController) {
        super("Custom Color Numbers...");
        this.colorController = colorController;
        
        setMnemonic('P');
        setDisplayedMnemonicIndex(8);
        setToolTipText("Assign specific numbers to colors");
        
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCustomColorIDDialog();
            }
        });
    }
    
    private void showCustomColorIDDialog() {
        // Obtener la ventana padre
        java.awt.Window parentWindow = SwingUtilities.getWindowAncestor(this);
        java.awt.Frame parentFrame = null;
        
        if (parentWindow instanceof java.awt.Frame) {
            parentFrame = (java.awt.Frame) parentWindow;
        }
        
        // Crear y mostrar el diálogo
        CustomColorIDDialog dialog = new CustomColorIDDialog(parentFrame, colorController);
        dialog.setVisible(true);
    }
}