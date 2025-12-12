package mosaic.ui;

import mosaic.controllers.*;
import colors.LEGOColor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

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
        super(parent, "Custom Color Numbers", true);
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
        assignButton = new JButton("Assign Number");
        removeButton = new JButton("Remove Customization");
        clearAllButton = new JButton("Clear All");
        
        // Estado inicial de botones
        updateButtonStates();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel principal con lista de colores
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder("Available Colors"));
        
        JScrollPane scrollPane = new JScrollPane(colorList);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de información
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("💡 Custom numbers have priority over automatic ones"));
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Panel de control
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Assign Custom Number"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Number:"), gbc);
        
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
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");
        
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
            // Crear lista temporal para ordenar
            List<ColorEntry> entries = new ArrayList<>();
            
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
                    
                    entries.add(new ColorEntry(color, colorName, displayID, customID));
                }
            }
            
            // Ordenar por número efectivo (personalizado o automático)
            entries.sort((a, b) -> {
                int numA = getEffectiveNumber(a);
                int numB = getEffectiveNumber(b);
                return Integer.compare(numA, numB);
            });
            
            // Añadir entradas ordenadas al modelo
            for (ColorEntry entry : entries) {
                listModel.addElement(entry);
            }
        }
        
        if (listModel.isEmpty()) {
            listModel.addElement(new ColorEntry(null, "No colors available", "", null));
        }
    }
    
    // Obtiene el número efectivo de una entrada (personalizado tiene prioridad)
    private int getEffectiveNumber(ColorEntry entry) {
        if (entry.customID != null) {
            return entry.customID;
        }
        
        // Intentar parsear el displayID como número
        try {
            return Integer.parseInt(entry.currentID);
        } catch (NumberFormatException e) {
            // Si no es número, usar un valor alto para ponerlo al final
            return Integer.MAX_VALUE;
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
                    "Please enter a number.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int customNumber = Integer.parseInt(text);
            if (customNumber <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "The number must be greater than 0.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            colorController.setCustomColorID(selected.color, customNumber);
            
            JOptionPane.showMessageDialog(this, 
                String.format("✅ Number %d assigned to color '%s'", customNumber, selected.colorName),
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            refreshColorList();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid number.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeCustomNumber() {
        ColorEntry selected = colorList.getSelectedValue();
        if (selected == null || selected.color == null) {
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            String.format("Remove the custom number from color '%s'?", selected.colorName),
            "Confirm", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            colorController.removeCustomColorID(selected.color);
            JOptionPane.showMessageDialog(this, 
                String.format("✅ Custom number removed from color '%s'", selected.colorName),
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshColorList();
        }
    }
    
    private void clearAllCustomNumbers() {
        int result = JOptionPane.showConfirmDialog(this,
            "Remove ALL custom numbers?\nThis will restore automatic numbering.",
            "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            colorController.clearAllCustomColorIDs();
            JOptionPane.showMessageDialog(this, 
                "✅ All custom numbers have been removed",
                "Success", JOptionPane.INFORMATION_MESSAGE);
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