package mosaic.ui.components;

import mosaic.ui.BrushSize;
import mosaic.controllers.StudEditController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Selector para cambiar el tamaño del pincel.
 * Permite seleccionar entre diferentes tamaños predefinidos.
 */
public class BrushSizeSelector extends JPanel {
    private StudEditController studEditController;
    private JComboBox<BrushSize> sizeComboBox;
    
    /**
     * Constructor del selector de tamaño de pincel.
     * @param studEditController el controlador de edición
     */
    public BrushSizeSelector(StudEditController studEditController) {
        this.studEditController = studEditController;
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }
    
    /**
     * Inicializa los componentes del selector.
     */
    private void initializeComponents() {
        sizeComboBox = new JComboBox<>(BrushSize.values());
        sizeComboBox.setSelectedItem(studEditController.getBrushSize());
        sizeComboBox.setToolTipText("Select brush size");
    }
    
    /**
     * Organiza los componentes en el panel.
     */
    private void layoutComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Etiqueta
        JLabel label = new JLabel("Brush:");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        add(label);
        
        // ComboBox de tamaño
        sizeComboBox.setPreferredSize(new Dimension(80, 25));
        add(sizeComboBox);
    }
    
    /**
     * Configura los manejadores de eventos.
     */
    private void setupEventHandlers() {
        sizeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrushSize selectedSize = (BrushSize) sizeComboBox.getSelectedItem();
                if (selectedSize != null) {
                    studEditController.setBrushSize(selectedSize);
                }
            }
        });
        
        // Listener para actualizar el combo cuando cambie el tamaño del pincel
        studEditController.addChangeListener(e -> {
            BrushSize currentSize = studEditController.getBrushSize();
            if (!currentSize.equals(sizeComboBox.getSelectedItem())) {
                sizeComboBox.setSelectedItem(currentSize);
            }
        });
    }
    
    /**
     * Actualiza el selector para reflejar el tamaño actual.
     */
    public void updateSelection() {
        sizeComboBox.setSelectedItem(studEditController.getBrushSize());
    }
}