package mosaic.ui.panels;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;
import mosaic.MosaicOverlay;
import mosaic.MosaicOverlayManager;
import colors.LEGOColorGrid;
import bricks.ToBricksType;

/**
 * Panel de control para gestionar múltiples overlays de mosaico.
 * Permite añadir, eliminar, configurar y reordenar overlays con configuraciones independientes.
 * 
 * @author BrickGraphics
 */
public class OverlayControlPanel extends JPanel {
    
    private MosaicOverlayManager overlayManager;
    private JList<MosaicOverlay> overlayList;
    private DefaultListModel<MosaicOverlay> listModel;
    
    // Controles principales
    private JButton addOverlayButton;
    private JButton removeOverlayButton;
    private JButton clearAllButton;
    
    // Controles de orden
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton toFrontButton;
    private JButton toBackButton;
    
    // Controles de configuración
    private JSlider opacitySlider;
    private JCheckBox visibilityCheckBox;
    private JSpinner positionXSpinner;
    private JSpinner positionYSpinner;
    private JComboBox<ToBricksType> brickTypeCombo;
    
    // Información y estado
    private JLabel memoryStatusLabel;
    private JLabel overlayCountLabel;
    
    // Listener para cambios
    private OverlayChangeListener changeListener;
    
    public interface OverlayChangeListener {
        void onOverlaysChanged();
        void onOverlaySelected(MosaicOverlay overlay);
    }
    
    /**
     * Constructor
     */
    public OverlayControlPanel(MosaicOverlayManager manager) {
        this.overlayManager = manager;
        this.listModel = new DefaultListModel<>();
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateControls();
    }
    
    /**
     * Inicializa todos los componentes
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Control de Overlays Múltiples"));
        
        // Lista de overlays
        overlayList = new JList<>(listModel);
        overlayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overlayList.setCellRenderer(new OverlayCellRenderer());
        
        // Botones principales
        addOverlayButton = new JButton("Añadir Overlay");
        removeOverlayButton = new JButton("Eliminar");
        clearAllButton = new JButton("Limpiar Todo");
        
        // Botones de orden
        moveUpButton = new JButton("↑ Subir");
        moveDownButton = new JButton("↓ Bajar");
        toFrontButton = new JButton("Al Frente");
        toBackButton = new JButton("Al Fondo");
        
        // Controles de configuración
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        visibilityCheckBox = new JCheckBox("Visible", true);
        
        positionXSpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 1));
        positionYSpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 1));
        
        brickTypeCombo = new JComboBox<>(ToBricksType.values());
        
        // Labels de información
        memoryStatusLabel = new JLabel("Memoria: 0MB");
        overlayCountLabel = new JLabel("Overlays: 0");
    }
    
    /**
     * Organiza los componentes en el layout
     */
    private void layoutComponents() {
        // Panel principal dividido verticalmente
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        // Panel superior: lista de overlays
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new TitledBorder("Lista de Overlays"));
        
        JScrollPane scrollPane = new JScrollPane(overlayList);
        scrollPane.setPreferredSize(new Dimension(250, 150));
        topPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones principales
        JPanel mainButtonPanel = new JPanel(new FlowLayout());
        mainButtonPanel.add(addOverlayButton);
        mainButtonPanel.add(removeOverlayButton);
        mainButtonPanel.add(clearAllButton);
        topPanel.add(mainButtonPanel, BorderLayout.SOUTH);
        
        // Panel inferior: controles de configuración
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new TitledBorder("Configuración del Overlay Seleccionado"));
        
        // Panel de orden
        JPanel orderPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        orderPanel.setBorder(new TitledBorder("Orden de Capas"));
        orderPanel.add(moveUpButton);
        orderPanel.add(moveDownButton);
        orderPanel.add(toFrontButton);
        orderPanel.add(toBackButton);
        
        // Panel de propiedades
        JPanel propsPanel = new JPanel(new GridBagLayout());
        propsPanel.setBorder(new TitledBorder("Propiedades"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        propsPanel.add(new JLabel("Opacidad:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        propsPanel.add(opacitySlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        propsPanel.add(visibilityCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        propsPanel.add(new JLabel("Posición X:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        propsPanel.add(positionXSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        propsPanel.add(new JLabel("Posición Y:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        propsPanel.add(positionYSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        propsPanel.add(new JLabel("Tipo Ladrillo:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        propsPanel.add(brickTypeCombo, gbc);
        
        // Panel de información
        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.setBorder(new TitledBorder("Estado del Sistema"));
        infoPanel.add(overlayCountLabel);
        infoPanel.add(new JSeparator(JSeparator.VERTICAL));
        infoPanel.add(memoryStatusLabel);
        
        // Ensamblar panel inferior
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.add(orderPanel, BorderLayout.WEST);
        configPanel.add(propsPanel, BorderLayout.CENTER);
        
        bottomPanel.add(configPanel, BorderLayout.CENTER);
        bottomPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Ensamblar todo
        mainSplit.setTopComponent(topPanel);
        mainSplit.setBottomComponent(bottomPanel);
        mainSplit.setDividerLocation(200);
        
        add(mainSplit, BorderLayout.CENTER);
    }
    
    /**
     * Configura los event handlers
     */
    private void setupEventHandlers() {
        // Selección de overlay
        overlayList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateConfigControls();
                MosaicOverlay selected = overlayList.getSelectedValue();
                if (changeListener != null && selected != null) {
                    changeListener.onOverlaySelected(selected);
                }
            }
        });
        
        // Botones principales
        addOverlayButton.addActionListener(e -> showAddOverlayDialog());
        removeOverlayButton.addActionListener(e -> removeSelectedOverlay());
        clearAllButton.addActionListener(e -> clearAllOverlays());
        
        // Botones de orden
        moveUpButton.addActionListener(e -> moveSelectedOverlay(-1));
        moveDownButton.addActionListener(e -> moveSelectedOverlay(1));
        toFrontButton.addActionListener(e -> moveSelectedOverlayToFront());
        toBackButton.addActionListener(e -> moveSelectedOverlayToBack());
        
        // Controles de configuración
        opacitySlider.addChangeListener(e -> updateSelectedOverlayOpacity());
        visibilityCheckBox.addActionListener(e -> updateSelectedOverlayVisibility());
        
        positionXSpinner.addChangeListener(e -> updateSelectedOverlayPosition());
        positionYSpinner.addChangeListener(e -> updateSelectedOverlayPosition());
        
        brickTypeCombo.addActionListener(e -> updateSelectedOverlayBrickType());
    }
    
    /**
     * Actualiza los controles según el estado actual
     */
    private void updateControls() {
        // Actualizar lista de overlays
        listModel.clear();
        List<MosaicOverlay> overlays = overlayManager.getAllOverlays();
        for (MosaicOverlay overlay : overlays) {
            listModel.addElement(overlay);
        }
        
        // Actualizar información de estado
        overlayCountLabel.setText("Overlays: " + overlayManager.getOverlayCount());
        memoryStatusLabel.setText("Memoria: " + (overlayManager.getTotalMemoryUsed() / 1024 / 1024) + "MB");
        
        // Actualizar controles de configuración
        updateConfigControls();
        
        // Repintar
        repaint();
        
        // Notificar cambios
        if (changeListener != null) {
            changeListener.onOverlaysChanged();
        }
    }
    
    /**
     * Actualiza los controles de configuración según el overlay seleccionado
     */
    private void updateConfigControls() {
        MosaicOverlay selected = overlayList.getSelectedValue();
        boolean hasSelection = selected != null;
        
        // Habilitar/deshabilitar controles
        removeOverlayButton.setEnabled(hasSelection);
        moveUpButton.setEnabled(hasSelection && overlayList.getSelectedIndex() > 0);
        moveDownButton.setEnabled(hasSelection && overlayList.getSelectedIndex() < listModel.size() - 1);
        toFrontButton.setEnabled(hasSelection);
        toBackButton.setEnabled(hasSelection);
        
        opacitySlider.setEnabled(hasSelection);
        visibilityCheckBox.setEnabled(hasSelection);
        positionXSpinner.setEnabled(hasSelection);
        positionYSpinner.setEnabled(hasSelection);
        brickTypeCombo.setEnabled(hasSelection);
        
        // Actualizar valores
        if (hasSelection) {
            opacitySlider.setValue((int)(selected.getOpacity() * 100));
            visibilityCheckBox.setSelected(selected.isVisible());
            positionXSpinner.setValue(selected.getPosition().x);
            positionYSpinner.setValue(selected.getPosition().y);
            brickTypeCombo.setSelectedItem(selected.getBrickType());
        }
    }
    
    /**
     * Muestra diálogo para añadir nuevo overlay
     */
    private void showAddOverlayDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog;
        if (parentWindow instanceof Frame) {
            dialog = new JDialog((Frame)parentWindow, "Añadir Nuevo Overlay", true);
        } else if (parentWindow instanceof Dialog) {
            dialog = new JDialog((Dialog)parentWindow, "Añadir Nuevo Overlay", true);
        } else {
            dialog = new JDialog((Frame)null, "Añadir Nuevo Overlay", true);
        }
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JTextField nameField = new JTextField("Overlay " + (overlayManager.getOverlayCount() + 1), 15);
        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 10));
        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 10));
        JSlider newOpacitySlider = new JSlider(0, 100, 100);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Posición X:"), gbc);
        gbc.gridx = 1;
        panel.add(xSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Posición Y:"), gbc);
        gbc.gridx = 1;
        panel.add(ySpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Opacidad:"), gbc);
        gbc.gridx = 1;
        panel.add(newOpacitySlider, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("Crear");
        JButton cancelButton = new JButton("Cancelar");
        
        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Overlay " + (overlayManager.getOverlayCount() + 1);
            
            Point position = new Point((Integer)xSpinner.getValue(), (Integer)ySpinner.getValue());
            
            // Crear imagen de ejemplo (normalmente se cargaría desde archivo)
            BufferedImage exampleImage = createExampleOverlayImage();
            
            MosaicOverlay newOverlay = new MosaicOverlay(name, exampleImage, position);
            newOverlay.setOpacity(newOpacitySlider.getValue() / 100.0f);
            
            overlayManager.addOverlay(newOverlay);
            updateControls();
            
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    /**
     * Crea una imagen de ejemplo para un nuevo overlay
     */
    private BufferedImage createExampleOverlayImage() {
        int size = 100;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Crear patrón colorido
        for (int x = 0; x < size; x += 10) {
            for (int y = 0; y < size; y += 10) {
                g2d.setColor(new Color(
                    (x * 2) % 256,
                    (y * 2) % 256,
                    ((x + y) * 2) % 256
                ));
                g2d.fillRect(x, y, 10, 10);
            }
        }
        
        g2d.dispose();
        return image;
    }
    
    // Métodos de acción
    private void removeSelectedOverlay() {
        int index = overlayList.getSelectedIndex();
        if (index >= 0) {
            overlayManager.removeOverlay(index);
            updateControls();
        }
    }
    
    private void clearAllOverlays() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea eliminar todos los overlays?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            overlayManager.clearAllOverlays();
            updateControls();
        }
    }
    
    private void moveSelectedOverlay(int direction) {
        int index = overlayList.getSelectedIndex();
        if (index >= 0) {
            if (direction < 0) {
                overlayManager.moveOverlayUp(index);
                overlayList.setSelectedIndex(Math.max(0, index - 1));
            } else {
                overlayManager.moveOverlayDown(index);
                overlayList.setSelectedIndex(Math.min(listModel.size() - 1, index + 1));
            }
            updateControls();
        }
    }
    
    private void moveSelectedOverlayToFront() {
        int index = overlayList.getSelectedIndex();
        if (index >= 0) {
            overlayManager.moveOverlayToFront(index);
            overlayList.setSelectedIndex(listModel.size() - 1);
            updateControls();
        }
    }
    
    private void moveSelectedOverlayToBack() {
        int index = overlayList.getSelectedIndex();
        if (index >= 0) {
            overlayManager.moveOverlayToBack(index);
            overlayList.setSelectedIndex(0);
            updateControls();
        }
    }
    
    private void updateSelectedOverlayOpacity() {
        MosaicOverlay selected = overlayList.getSelectedValue();
        if (selected != null && opacitySlider.isEnabled()) {
            selected.setOpacity(opacitySlider.getValue() / 100.0f);
            if (changeListener != null) {
                changeListener.onOverlaysChanged();
            }
        }
    }
    
    private void updateSelectedOverlayVisibility() {
        MosaicOverlay selected = overlayList.getSelectedValue();
        if (selected != null) {
            selected.setVisible(visibilityCheckBox.isSelected());
            if (changeListener != null) {
                changeListener.onOverlaysChanged();
            }
        }
    }
    
    private void updateSelectedOverlayPosition() {
        MosaicOverlay selected = overlayList.getSelectedValue();
        if (selected != null && positionXSpinner.isEnabled() && positionYSpinner.isEnabled()) {
            int x = (Integer)positionXSpinner.getValue();
            int y = (Integer)positionYSpinner.getValue();
            selected.setPosition(x, y);
            if (changeListener != null) {
                changeListener.onOverlaysChanged();
            }
        }
    }
    
    private void updateSelectedOverlayBrickType() {
        MosaicOverlay selected = overlayList.getSelectedValue();
        if (selected != null && brickTypeCombo.isEnabled()) {
            selected.setBrickType((ToBricksType)brickTypeCombo.getSelectedItem());
            if (changeListener != null) {
                changeListener.onOverlaysChanged();
            }
        }
    }
    
    // Getters y setters
    public void setChangeListener(OverlayChangeListener listener) {
        this.changeListener = listener;
    }
    
    public MosaicOverlayManager getOverlayManager() {
        return overlayManager;
    }
    
    /**
     * Renderer personalizado para la lista de overlays
     */
    private static class OverlayCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof MosaicOverlay) {
                MosaicOverlay overlay = (MosaicOverlay) value;
                String text = String.format("%s %s(%.0f%%)",
                    overlay.getName(),
                    overlay.isVisible() ? "👁 " : "🚫 ",
                    overlay.getOpacity() * 100
                );
                setText(text);
                
                if (!overlay.isVisible()) {
                    setForeground(Color.GRAY);
                }
            }
            
            return this;
        }
    }
}