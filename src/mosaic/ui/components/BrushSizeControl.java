package mosaic.ui.components;

import mosaic.controllers.StudEditController;
import javax.swing.*;
import java.awt.*;

/**
 * Componente para controlar el tamaño del pincel.
 */
public class BrushSizeControl extends JPanel {
    private StudEditController studEditController;
    private JSpinner brushSizeSpinner;
    private JLabel sizeLabel;
    
    public BrushSizeControl(StudEditController studEditController) {
        this.studEditController = studEditController;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Label
        sizeLabel = new JLabel("Brush Size:");
        add(sizeLabel);
        
        // Spinner para seleccionar tamaño 1-5
        SpinnerNumberModel model = new SpinnerNumberModel(1, 1, 5, 1);
        brushSizeSpinner = new JSpinner(model);
        brushSizeSpinner.setPreferredSize(new Dimension(60, 25));
        
        // Listener para cambios
        brushSizeSpinner.addChangeListener(e -> {
            int newSize = (Integer) brushSizeSpinner.getValue();
            studEditController.setBrushSize(newSize);
        });
        
        add(brushSizeSpinner);
        
        // Sync inicial
        brushSizeSpinner.setValue(studEditController.getBrushSize());
    }
    
    public void updateBrushSize() {
        SwingUtilities.invokeLater(() -> {
            brushSizeSpinner.setValue(studEditController.getBrushSize());
        });
    }
}