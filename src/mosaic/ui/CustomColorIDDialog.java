package mosaic.ui;

import mosaic.controllers.*;
import colors.LEGOColor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Ventana de diálogo para gestionar números personalizados de colores.
 * Permite ver, asignar y eliminar números específicos para cada color.
 */
public class CustomColorIDDialog extends JDialog {
    
    private ColorController colorController;
    private CustomColorIDManager manager;
    private JList<ColorEntry> colorList;
    private DefaultListModel<ColorEntry> listModel;
    private JTextField numberField;
    private JButton assignButton, removeButton, clearAllButton;
    
    /**
     * Clase auxiliar para mostrar información de color en la lista
     */
    private static class ColorEntry {
        LEGOColor color;
        String colorName;
        String currentID;
        Integer customID;
        
        ColorEntry(LEGOColor color, String colorName, String currentID, Integer customID) {
            this.color = color;
            this.colorName = colorName;
            this.currentID = currentID;
            this.customID = customID;
        }
        
        @Override
        public String toString() {
            String custom = customID != null ? " [PERSONALIZADO: " + customID + "]" : "";
            return String.format("ID: %s - %s%s", currentID, colorName, custom);
        }
    }
    
    public CustomColorIDDialog(Frame parent, ColorController colorController) {
        super(parent, "Números Personalizados de Colores", true);
        this.colorController = colorController;
        this.manager = new CustomColorIDManager(colorController);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        refreshColorList();
        
        setSize(600, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        listModel = new DefaultListModel<>();
        colorList = new JList<>(listModel);
        colorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        colorList.setCellRenderer(new ColorListCellRenderer());
        
        numberField = new JTextField(10);
        assignButton = new JButton("Asignar Número");
        removeButton = new JButton("Quitar Personalización");
        clearAllButton = new JButton("Limpiar Todo");
        
        // Estado inicial de botones
        updateButtonStates();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel principal con lista de colores
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder("Colores Disponibles"));
        
        JScrollPane scrollPane = new JScrollPane(colorList);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de información
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("💡 Los números personalizados tienen prioridad sobre automáticos"));
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de control
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Asignar Número Personalizado"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Número:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        controlPanel.add(numberField, gbc);
        
        gbc.gridx = 2; gbc.gridy = 0;
        controlPanel.add(assignButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        controlPanel.add(removeButton, gbc);
        
        gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 1;
        controlPanel.add(clearAllButton, gbc);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Actualizar");
        JButton closeButton = new JButton("Cerrar");
        
        refreshButton.addActionListener(e -> refreshColorList());
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.NORTH);
    }
    
    private void setupEventHandlers() {
        colorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
                ColorEntry selected = colorList.getSelectedValue();
                if (selected != null && selected.customID != null) {
                    numberField.setText(selected.customID.toString());
                } else {
                    numberField.setText("");
                }
            }
        });
        
        assignButton.addActionListener(e -> assignCustomNumber());
        removeButton.addActionListener(e -> removeCustomNumber());
        clearAllButton.addActionListener(e -> clearAllCustomNumbers());
        
        numberField.addActionListener(e -> assignCustomNumber());
    }
    
    private void refreshColorList() {
        listModel.clear();
        
        List<LEGOColor> filteredColors = colorController.getFilteredColors();
        if (filteredColors != null) {
            for (LEGOColor color : filteredColors) {
                String currentID = colorController.getShownID(color);
                String colorName = colorController.getShownName(color);
                Integer customID = colorController.getCustomColorID(color);
                
                if (currentID != null && colorName != null) {
                    // Extraer solo el número antes de la coma
                    String displayID = currentID;
                    int commaIndex = currentID.indexOf(",");
                    if (commaIndex != -1) {
                        displayID = currentID.substring(0, commaIndex).trim();
                    }
                    
                    listModel.addElement(new ColorEntry(color, colorName, displayID, customID));
                }
            }
        }
        
        if (listModel.isEmpty()) {
            listModel.addElement(new ColorEntry(null, "No hay colores disponibles", "", null));
        }
    }
    
    private void updateButtonStates() {
        ColorEntry selected = colorList.getSelectedValue();
        boolean hasSelection = selected != null && selected.color != null;
        
        assignButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection && selected.customID != null);
        numberField.setEnabled(hasSelection);
    }
    
    private void assignCustomNumber() {
        ColorEntry selected = colorList.getSelectedValue();
        if (selected == null || selected.color == null) {
            return;
        }
        
        try {
            String text = numberField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Por favor, introduce un número.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int customNumber = Integer.parseInt(text);
            if (customNumber <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "El número debe ser mayor que 0.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            colorController.setCustomColorID(selected.color, customNumber);
            
            JOptionPane.showMessageDialog(this, 
                String.format("✅ Número %d asignado al color '%s'", customNumber, selected.colorName),
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            refreshColorList();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, introduce un número válido.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeCustomNumber() {
        ColorEntry selected = colorList.getSelectedValue();
        if (selected == null || selected.color == null) {
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            String.format("¿Eliminar el número personalizado del color '%s'?", selected.colorName),
            "Confirmar", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            colorController.removeCustomColorID(selected.color);
            JOptionPane.showMessageDialog(this, 
                String.format("✅ Número personalizado eliminado del color '%s'", selected.colorName),
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refreshColorList();
        }
    }
    
    private void clearAllCustomNumbers() {
        int result = JOptionPane.showConfirmDialog(this,
            "¿Eliminar TODOS los números personalizados?\nEsto restaurará la numeración automática.",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            colorController.clearAllCustomColorIDs();
            JOptionPane.showMessageDialog(this, 
                "✅ Todos los números personalizados han sido eliminados",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refreshColorList();
        }
    }
    
    /**
     * Renderer personalizado para mostrar colores en la lista
     */
    private class ColorListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ColorEntry) {
                ColorEntry entry = (ColorEntry) value;
                
                if (entry.color != null) {
                    // Crear un pequeño cuadrado de color
                    setIcon(new ColorIcon(entry.color.getRGB()));
                    
                    // Cambiar color de texto si tiene número personalizado
                    if (entry.customID != null) {
                        setForeground(isSelected ? Color.WHITE : new Color(0, 100, 0)); // Verde oscuro
                    }
                }
            }
            
            return this;
        }
    }
    
    /**
     * Icono simple para mostrar el color
     */
    private static class ColorIcon implements Icon {
        private Color color;
        
        public ColorIcon(Color color) {
            this.color = color;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.BLACK);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        }
        
        @Override
        public int getIconWidth() { return 16; }
        
        @Override
        public int getIconHeight() { return 16; }
    }
}