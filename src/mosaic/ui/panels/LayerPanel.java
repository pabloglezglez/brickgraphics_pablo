package mosaic.ui.panels;

import mosaic.layers.Layer;
import mosaic.layers.LayerManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * Panel de interfaz para gestionar el sistema de capas.
 * Permite cargar, posicionar, modificar y eliminar capas.
 */
public class LayerPanel extends JPanel {
    private LayerManager layerManager;
    private JList<Layer> layerList;
    private DefaultListModel<Layer> listModel;
    private JSlider opacitySlider;
    private JCheckBox visibilityCheckBox;
    private JComboBox<Layer.BlendMode> blendModeCombo;
    private JSpinner xSpinner;
    private JSpinner ySpinner;
    private JLabel layerInfoLabel;
    private JButton addLayerButton;
    private JButton removeLayerButton;
    private JButton duplicateButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JCheckBox enableLayersCheckBox;
    
    // Callback para notificar cambios
    private Runnable onLayersChangedCallback;
    private mosaic.controllers.MainController mainController; // NUEVO: Referencia para acceder al directorio de imagen principal
    
    /**
     * Constructor del panel de capas
     */
    public LayerPanel(LayerManager layerManager, mosaic.controllers.MainController mainController) {
        this.layerManager = layerManager;
        this.mainController = mainController; // NUEVO: Guardar referencia al MainController
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        // updateUI() should be called after everything is initialized
        updateUI();
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initializeComponents() {
        // Lista de capas
        listModel = new DefaultListModel<>();
        layerList = new JList<>(listModel);
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerList.setCellRenderer(new LayerListCellRenderer());
        
        // Controles de capa
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setMinorTickSpacing(5);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        visibilityCheckBox = new JCheckBox("Visible", true);
        
        blendModeCombo = new JComboBox<>(Layer.BlendMode.values());
        
        // Spinners para posición (rango ampliado para permitir movimientos grandes)
        xSpinner = new JSpinner(new SpinnerNumberModel(0, -99999, 99999, 1));
        ySpinner = new JSpinner(new SpinnerNumberModel(0, -99999, 99999, 1));

        // Añadir wheel listeners para permitir cambios rápidos con Shift/Ctrl
        addWheelListenerToSpinner(xSpinner);
        addWheelListenerToSpinner(ySpinner);
    }

    // Helper para añadir listener de rueda a un spinner (mueve más rápido con Shift/Ctrl)
    private void addWheelListenerToSpinner(JSpinner spinner) {
        spinner.addMouseWheelListener(e -> {
            if (!spinner.isEnabled()) return;
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            int currentValue = (Integer) model.getValue();
            int stepSize = model.getStepSize().intValue();
            int multiplier = 1;
            if (e.isShiftDown()) multiplier = 10;
            if (e.isControlDown()) multiplier = 100;
            int delta = e.getWheelRotation() * stepSize * -1 * multiplier;
            int newValue = currentValue + delta;
            Integer min = (Integer) model.getMinimum();
            Integer max = (Integer) model.getMaximum();
            if (min != null && newValue < min) newValue = min;
            if (max != null && newValue > max) newValue = max;
            spinner.setValue(newValue);
            e.consume();
        });
        
        // Información de la capa
        layerInfoLabel = new JLabel("No layers");
        layerInfoLabel.setFont(layerInfoLabel.getFont().deriveFont(Font.ITALIC));
        
        // Botones
        addLayerButton = new JButton("Add");
        addLayerButton.setToolTipText("Load image as new layer");
        
        removeLayerButton = new JButton("Delete");
        removeLayerButton.setToolTipText("Delete selected layer");
        removeLayerButton.setEnabled(false);
        
        duplicateButton = new JButton("Duplicate");
        duplicateButton.setToolTipText("Duplicate selected layer");
        duplicateButton.setEnabled(false);
        
        moveUpButton = new JButton("↑");
        moveUpButton.setToolTipText("Move layer up");
        moveUpButton.setEnabled(false);
        
        moveDownButton = new JButton("↓");
        moveDownButton.setToolTipText("Move layer down");
        moveDownButton.setEnabled(false);
        
        enableLayersCheckBox = new JCheckBox("Enable layers", true);
        enableLayersCheckBox.setToolTipText("Enable or disable all layers");
    }
    
    /**
     * Configura el diseño del panel
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Layers"));
        
        // Panel superior - Control general
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(enableLayersCheckBox);
        add(topPanel, BorderLayout.NORTH);
        
        // Panel central - Lista de capas
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        JScrollPane listScrollPane = new JScrollPane(layerList);
        listScrollPane.setPreferredSize(new Dimension(200, 150));
        centerPanel.add(listScrollPane, BorderLayout.CENTER);
        
        // Panel de botones de la lista
        JPanel listButtonsPanel = new JPanel(new FlowLayout());
        listButtonsPanel.add(addLayerButton);
        listButtonsPanel.add(removeLayerButton);
        listButtonsPanel.add(duplicateButton);
        listButtonsPanel.add(moveUpButton);
        listButtonsPanel.add(moveDownButton);
        centerPanel.add(listButtonsPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Panel inferior - Propiedades de capa
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Información
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        bottomPanel.add(layerInfoLabel, gbc);
        
        // Opacidad
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        bottomPanel.add(new JLabel("Opacity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(opacitySlider, gbc);
        
        // Visibilidad
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        bottomPanel.add(visibilityCheckBox, gbc);
        
        // Modo de mezcla
        gbc.gridx = 0; gbc.gridy = 3;
        bottomPanel.add(new JLabel("Mode:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(blendModeCombo, gbc);
        
        // Posición
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        bottomPanel.add(new JLabel("X:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(xSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE;
        bottomPanel.add(new JLabel("Y:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        bottomPanel.add(ySpinner, gbc);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Selección de capa
        layerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Layer selectedLayer = layerList.getSelectedValue();
                if (selectedLayer != null) {
                    layerManager.setSelectedLayer(selectedLayer);
                    updateSelectedLayerControls();
                }
            }
        });
        
        // Doble clic para editar nombre
        layerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editLayerName();
                }
            }
        });
        
        // Botones
        addLayerButton.addActionListener(e -> addLayer());
        removeLayerButton.addActionListener(e -> removeSelectedLayer());
        duplicateButton.addActionListener(e -> duplicateSelectedLayer());
        moveUpButton.addActionListener(e -> moveSelectedLayerUp());
        moveDownButton.addActionListener(e -> moveSelectedLayerDown());
        
        // Controles de capa
        opacitySlider.addChangeListener(e -> updateLayerOpacity());
        visibilityCheckBox.addActionListener(e -> updateLayerVisibility());
        blendModeCombo.addActionListener(e -> updateLayerBlendMode());
        
        // Posición
        xSpinner.addChangeListener(e -> updateLayerPosition());
        ySpinner.addChangeListener(e -> updateLayerPosition());
        
        // Control general
        enableLayersCheckBox.addActionListener(e -> {
            layerManager.setLayersEnabled(enableLayersCheckBox.isSelected());
            notifyLayersChanged();
        });
    }
    
    /**
     * Añade una nueva capa desde archivo
     */
    private void addLayer() {
        JFileChooser fileChooser = new JFileChooser();
        
        // NUEVO: Establecer directorio inicial basado en la imagen principal
        if (mainController != null) {
            File mainImageDir = mainController.getMainImageDirectory();
            if (mainImageDir != null && mainImageDir.exists()) {
                fileChooser.setCurrentDirectory(mainImageDir);
            }
        }
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        fileChooser.setDialogTitle("Select image for layer");
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                Layer layer = layerManager.addLayerFromFile(file.getAbsolutePath(), null);
                updateLayerList();
                layerList.setSelectedValue(layer, true);
                notifyLayersChanged();
                
                JOptionPane.showMessageDialog(this, 
                    "Layer added: " + layer.getName(),
                    "Layer added", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading image:\n" + ex.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Elimina la capa seleccionada
     */
    private void removeSelectedLayer() {
        Layer selected = layerList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete layer '" + selected.getName() + "'?",
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                layerManager.removeLayer(selected);
                updateLayerList();
                notifyLayersChanged();
            }
        }
    }
    
    /**
     * Duplica la capa seleccionada
     */
    private void duplicateSelectedLayer() {
        Layer selected = layerList.getSelectedValue();
        if (selected != null) {
            Layer duplicate = layerManager.duplicateLayer(selected);
            updateLayerList();
            layerList.setSelectedValue(duplicate, true);
            notifyLayersChanged();
        }
    }
    
    /**
     * Mueve la capa seleccionada hacia arriba
     */
    private void moveSelectedLayerUp() {
        Layer selected = layerList.getSelectedValue();
        if (selected != null && layerManager.moveLayerUp(selected)) {
            updateLayerList();
            layerList.setSelectedValue(selected, true);
            notifyLayersChanged();
        }
    }
    
    /**
     * Mueve la capa seleccionada hacia abajo
     */
    private void moveSelectedLayerDown() {
        Layer selected = layerList.getSelectedValue();
        if (selected != null && layerManager.moveLayerDown(selected)) {
            updateLayerList();
            layerList.setSelectedValue(selected, true);
            notifyLayersChanged();
        }
    }
    
    /**
     * Edita el nombre de la capa seleccionada
     */
    private void editLayerName() {
        Layer selected = layerList.getSelectedValue();
        if (selected != null) {
            String newName = JOptionPane.showInputDialog(this,
                "New name for the layer:",
                selected.getName());
            
            if (newName != null && !newName.trim().isEmpty()) {
                selected.setName(newName.trim());
                updateLayerList();
                notifyLayersChanged();
            }
        }
    }
    
    /**
     * Actualiza la opacidad de la capa seleccionada
     */
    private void updateLayerOpacity() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            float opacity = opacitySlider.getValue() / 100.0f;
            selected.setOpacity(opacity);
            updateLayerInfo();
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la visibilidad de la capa seleccionada
     */
    private void updateLayerVisibility() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            selected.setVisible(visibilityCheckBox.isSelected());
            updateLayerList();
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza el modo de mezcla de la capa seleccionada
     */
    private void updateLayerBlendMode() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            Layer.BlendMode mode = (Layer.BlendMode) blendModeCombo.getSelectedItem();
            selected.setBlendMode(mode);
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la posición de la capa seleccionada
     */
    private void updateLayerPosition() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            int x = (Integer) xSpinner.getValue();
            int y = (Integer) ySpinner.getValue();
            selected.setPosition(x, y);
            updateLayerInfo();
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la lista de capas
     */
    private void updateLayerList() {
        // Verificación defensiva
        if (listModel == null) {
            return; // Skip update if not initialized yet
        }
        
        listModel.clear();
        for (Layer layer : layerManager.getLayers()) {
            listModel.addElement(layer);
        }
        
        // Mantener selección
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            layerList.setSelectedValue(selected, true);
        }
        
        updateButtonStates();
    }
    
    /**
     * Actualiza los controles de la capa seleccionada
     */
    private void updateSelectedLayerControls() {
        // Verificación defensiva
        if (layerManager == null || opacitySlider == null) {
            return; // Skip update if not initialized yet
        }
        
        Layer selected = layerManager.getSelectedLayer();
        
        if (selected != null) {
            // Temporalmente desactivar eventos para evitar loops
            opacitySlider.removeChangeListener(opacitySlider.getChangeListeners()[0]);
            
            opacitySlider.setValue((int) (selected.getOpacity() * 100));
            visibilityCheckBox.setSelected(selected.isVisible());
            blendModeCombo.setSelectedItem(selected.getBlendMode());
            xSpinner.setValue(selected.getPosition().x);
            ySpinner.setValue(selected.getPosition().y);
            
            // Reactivar eventos
            opacitySlider.addChangeListener(e -> updateLayerOpacity());
            
            updateLayerInfo();
        }
        
        updateButtonStates();
    }
    
    /**
     * Actualiza la información de la capa
     */
    private void updateLayerInfo() {
        Layer selected = layerManager.getSelectedLayer();
        
        if (selected != null) {
            layerInfoLabel.setText(String.format("%dx%d px", 
                selected.getWidth(), selected.getHeight()));
        } else {
            layerInfoLabel.setText("No layer selected");
        }
    }
    
    /**
     * Actualiza el estado de los botones
     */
    private void updateButtonStates() {
        // Verificación defensiva
        if (layerManager == null || removeLayerButton == null) {
            return; // Skip update if not initialized yet
        }
        
        boolean hasSelection = layerManager.getSelectedLayer() != null;
        boolean hasLayers = !layerManager.isEmpty();
        
        removeLayerButton.setEnabled(hasSelection);
        duplicateButton.setEnabled(hasSelection);
        moveUpButton.setEnabled(hasSelection);
        moveDownButton.setEnabled(hasSelection);
        
        // Habilitar/deshabilitar controles
        opacitySlider.setEnabled(hasSelection);
        visibilityCheckBox.setEnabled(hasSelection);
        blendModeCombo.setEnabled(hasSelection);
        xSpinner.setEnabled(hasSelection);
        ySpinner.setEnabled(hasSelection);
    }
    
    /**
     * Actualiza toda la interfaz
     */
    public void updateUI() {
        // Verificación defensiva
        if (layerManager == null || enableLayersCheckBox == null) {
            return; // Skip update if not initialized yet
        }
        
        updateLayerList();
        updateSelectedLayerControls();
        enableLayersCheckBox.setSelected(layerManager.isLayersEnabled());
    }
    
    /**
     * Establece el callback para notificar cambios
     */
    public void setOnLayersChangedCallback(Runnable callback) {
        this.onLayersChangedCallback = callback;
    }
    
    /**
     * Notifica que las capas han cambiado
     */
    private void notifyLayersChanged() {
        if (onLayersChangedCallback != null) {
            onLayersChangedCallback.run();
        }
    }
    
    /**
     * Renderer personalizado para la lista de capas
     */
    private static class LayerListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Layer) {
                Layer layer = (Layer) value;
                setText(layer.getName());
                
                // Indicar visibilidad con icono o estilo
                if (!layer.isVisible()) {
                    setForeground(Color.GRAY);
                    setText("(oculta) " + layer.getName());
                } else {
                    setForeground(isSelected ? Color.WHITE : Color.BLACK);
                }
            }
            
            return this;
        }
    }
}