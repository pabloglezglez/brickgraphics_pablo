package mosaic.ui.panels;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import mosaic.layers.Layer;
import mosaic.layers.LayerManager;
import mosaic.ui.menu.ImagePreparingToolBar;
import mosaic.ui.ImagePreparingView;
import io.Model;
import mosaic.io.BrickGraphicsState;

/**
 * Panel integrado que combina el sistema de capas con los controles de imagen.
 * Permite gestionar capas y aplicar transformaciones individuales a cada capa.
 */
public class IntegratedImageLayerPanel extends JPanel {
    private LayerManager layerManager;
    private ImagePreparingView imagePreparingView;
    private Model<BrickGraphicsState> model;
    private mosaic.ui.BrickedView brickedView; // Referencia para controles de PaintOverlay
    
    // Componentes de UI
    private JList<Layer> layerList;
    private DefaultListModel<Layer> listModel;
    private LayerListCellRenderer cellRenderer;
    
    // Controles de capas
    private JPanel topPanel;
    private JPanel layerButtonsPanel;
    private JButton addLayerButton;
    private JButton removeLayerButton;
    private JButton duplicateButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JCheckBox enableLayersCheckBox;
    private JLabel layerInfoLabel;
    
    // NUEVOS: Controles de Paint Overlay
    private JCheckBox paintOverlayCheckBox;
    private JButton clearPaintButton;
    
    // Panel de propiedades de capa seleccionada
    private JPanel layerPropertiesPanel;
    private JSlider opacitySlider;
    private JCheckBox visibilityCheckBox;
    private JComboBox<Layer.BlendMode> blendModeCombo;
    
    // Controles de transformaciones de imagen individuales
    private JSlider brightnessSlider;
    private JSlider contrastSlider;
    private JSlider saturationSlider;
    
    // Variables para transformaciones de imagen de fondo independientes
    // Se marcan como volatile para asegurar visibilidad entre hilos (EDT vs pipeline)
    private volatile float backgroundBrightness = 1.0f;
    private volatile float backgroundContrast = 1.0f;
    private volatile float backgroundSaturation = 1.0f;
    private volatile float backgroundGamma = 1.0f;
    private volatile float backgroundSharpness = 1.0f;
    private JSlider gammaSlider;
    private JSlider sharpnessSlider;
    private JSlider scaleSlider;
    private JButton resetAdjustmentsButton;
    
    // Etiquetas para mostrar valores numéricos
    private JLabel brightnessLabel;
    private JLabel contrastLabel;
    private JLabel saturationLabel;
    private JLabel gammaLabel;
    private JLabel sharpnessLabel;
    private JLabel scaleLabel;
    private JLabel opacityLabel;
    
    // Controles de posición
    private JSpinner positionXSpinner;
    private JSpinner positionYSpinner;
    
    // Callbacks
    private Runnable onLayersChangedCallback;
    private boolean updatingControls = false;
    
    /**
     * Constructor del panel integrado
     */
    public IntegratedImageLayerPanel(LayerManager layerManager, ImagePreparingView imagePreparingView, Model<BrickGraphicsState> model, mosaic.ui.BrickedView brickedView) {
        this.layerManager = layerManager;
        this.imagePreparingView = imagePreparingView;
        this.model = model;
        this.brickedView = brickedView;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupKeyBindings();
        updateUI();
    // construction complete
    }

    /**
     * Fuerza a refrescar la lista de capas desde el estado actual del LayerManager.
     * Útil tras cargar un KMV para que la UI refleje inmediatamente las capas restauradas.
     */
    public void refreshLayerList() {
        SwingUtilities.invokeLater(() -> {
            updateLayerList();
            revalidate();
            repaint();
        });
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initializeComponents() {
        // Lista de capas
        listModel = new DefaultListModel<>();
        layerList = new JList<>(listModel);
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cellRenderer = new LayerListCellRenderer();
        layerList.setCellRenderer(cellRenderer);
        
        // Hacer la lista más visible para debug
        layerList.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        layerList.setBackground(Color.WHITE);
        
        // Botones de gestión de capas con texto más corto
        addLayerButton = new JButton("+");
        addLayerButton.setToolTipText("Añadir nueva capa desde una imagen (Ctrl+N)");
        removeLayerButton = new JButton("✕");
        removeLayerButton.setToolTipText("Eliminar capa seleccionada (Supr/Backspace)");
        duplicateButton = new JButton("⧉");
        duplicateButton.setToolTipText("Duplicar capa seleccionada (Ctrl+D)");
        moveUpButton = new JButton("↑");
        moveUpButton.setToolTipText("Mover capa seleccionada hacia arriba (Alt+↑)");
        moveDownButton = new JButton("↓");
        moveDownButton.setToolTipText("Mover capa seleccionada hacia abajo (Alt+↓)");
        
        // Configurar tamaño uniforme para los botones
        Dimension buttonSize = new Dimension(32, 28);
        addLayerButton.setPreferredSize(buttonSize);
        removeLayerButton.setPreferredSize(buttonSize);
        duplicateButton.setPreferredSize(buttonSize);
        moveUpButton.setPreferredSize(buttonSize);
        moveDownButton.setPreferredSize(buttonSize);
        
        // Configurar font para mejor visibilidad
        Font buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        addLayerButton.setFont(buttonFont);
        removeLayerButton.setFont(buttonFont);
        duplicateButton.setFont(buttonFont);
        moveUpButton.setFont(buttonFont);
        moveDownButton.setFont(buttonFont);
        
        enableLayersCheckBox = new JCheckBox("Capas", true);
        enableLayersCheckBox.setToolTipText("Activar / Desactivar todas las capas");
        
        // NUEVOS: Controles de Paint Overlay mejorados
        paintOverlayCheckBox = new JCheckBox("Pintura", false);
        paintOverlayCheckBox.setToolTipText("Activar/desactivar pintura sobre el mosaico final");
        clearPaintButton = new JButton("Limpiar");
        clearPaintButton.setToolTipText("Borrar toda la pintura aplicada");
        clearPaintButton.setPreferredSize(new Dimension(60, 28));
        
        // Información de capas
        layerInfoLabel = new JLabel("No hay capas");
        layerInfoLabel.setFont(layerInfoLabel.getFont().deriveFont(Font.ITALIC));
        
        // Controles de propiedades de capa
        opacitySlider = createImageSlider("Opacidad", 0, 100, 100);
        
        visibilityCheckBox = new JCheckBox("Visible", true);
        blendModeCombo = new JComboBox<>(Layer.BlendMode.values());
        
        // Controles de transformaciones de imagen
        brightnessSlider = createImageSlider("Brillo", 0, 200, 100);
        contrastSlider = createImageSlider("Contraste", 0, 200, 100);
        saturationSlider = createImageSlider("Saturación", 0, 200, 100);
        gammaSlider = createImageSlider("Gamma", 50, 200, 100);
        sharpnessSlider = createImageSlider("Nitidez", 50, 150, 100);
        scaleSlider = createImageSlider("Escala", 10, 300, 100); // 10% a 300%, defecto 100%
        
        // Etiquetas para mostrar valores numéricos
        opacityLabel = new JLabel("100%");
        brightnessLabel = new JLabel("100%");
        contrastLabel = new JLabel("100%");
        saturationLabel = new JLabel("100%");
        gammaLabel = new JLabel("1.00");
        sharpnessLabel = new JLabel("100%");
        scaleLabel = new JLabel("100%");
        
        // Configurar ancho mínimo para las etiquetas para alineación
        int labelWidth = 50;
        opacityLabel.setPreferredSize(new Dimension(labelWidth, opacityLabel.getPreferredSize().height));
        brightnessLabel.setPreferredSize(new Dimension(labelWidth, brightnessLabel.getPreferredSize().height));
        contrastLabel.setPreferredSize(new Dimension(labelWidth, contrastLabel.getPreferredSize().height));
        saturationLabel.setPreferredSize(new Dimension(labelWidth, saturationLabel.getPreferredSize().height));
        gammaLabel.setPreferredSize(new Dimension(labelWidth, gammaLabel.getPreferredSize().height));
        sharpnessLabel.setPreferredSize(new Dimension(labelWidth, sharpnessLabel.getPreferredSize().height));
        scaleLabel.setPreferredSize(new Dimension(labelWidth, scaleLabel.getPreferredSize().height));
        
        // Alinear texto a la derecha para mejor apariencia
        opacityLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        brightnessLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contrastLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        saturationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gammaLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sharpnessLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        scaleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Botón reset para ajustes
        resetAdjustmentsButton = new JButton("Reset Ajustes");
        resetAdjustmentsButton.setToolTipText("Restablecer todos los ajustes de capa a valores por defecto");
        
        // Spinners de posición
        positionXSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 1));
        positionYSpinner = new JSpinner(new SpinnerNumberModel(0, -2000, 2000, 1));
        positionXSpinner.setToolTipText("Posición X de la capa");
        positionYSpinner.setToolTipText("Posición Y de la capa");
    }
    
    /**
     * Crea un slider para transformaciones de imagen
     */
    private JSlider createImageSlider(String name, int min, int max, int defaultValue) {
        JSlider slider = new JSlider(min, max, defaultValue);
        slider.setMajorTickSpacing((max - min) / 4);
        slider.setMinorTickSpacing((max - min) / 8);
        slider.setPaintTicks(true);
        slider.setName(name);
        return slider;
    }
    
    /**
     * Configura la disposición de los componentes
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Capas e Imagen"));
        
    // Panel superior como JToolBar (dos barras apiladas)
    topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

    // Barra 1: controles principales
    JToolBar mainBar = new JToolBar();
    mainBar.setFloatable(false);
    mainBar.setRollover(true);
    mainBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    
    // Configurar todos los componentes como no focusables
    enableLayersCheckBox.setFocusable(false);
    paintOverlayCheckBox.setFocusable(false);
    clearPaintButton.setFocusable(false);
    addLayerButton.setFocusable(false);
    removeLayerButton.setFocusable(false);
    duplicateButton.setFocusable(false);
    moveUpButton.setFocusable(false);
    moveDownButton.setFocusable(false);
    
    // Añadir componentes con espaciado adecuado
    mainBar.add(enableLayersCheckBox);
    mainBar.addSeparator(new Dimension(8, 0));
    mainBar.add(paintOverlayCheckBox);
    mainBar.add(Box.createHorizontalStrut(4));
    mainBar.add(clearPaintButton);
    mainBar.addSeparator(new Dimension(12, 0));
    
    // Grupo de botones de capa
    mainBar.add(addLayerButton);
    mainBar.add(Box.createHorizontalStrut(2));
    mainBar.add(removeLayerButton);
    mainBar.add(Box.createHorizontalStrut(2));
    mainBar.add(duplicateButton);

    // Barra 2: controles de orden
    JToolBar orderBar = new JToolBar();
    orderBar.setFloatable(false);
    orderBar.setRollover(true);
    orderBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
    
    orderBar.add(new JLabel("Orden: "));
    orderBar.add(moveUpButton);
    orderBar.add(Box.createHorizontalStrut(2));
    orderBar.add(moveDownButton);
    orderBar.add(Box.createHorizontalGlue()); // Empujar a la izquierda

    topPanel.add(mainBar);
    topPanel.add(orderBar);
        
    // Dejar que el layout calcule la altura necesaria (no fijar 60px)
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Panel central: lista de capas con scroll adecuado
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new TitledBorder("Capas"));
        
        JScrollPane layerScrollPane = new JScrollPane(layerList);
        layerScrollPane.setPreferredSize(new Dimension(200, 150));
        layerScrollPane.setMinimumSize(new Dimension(180, 120));
        layerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        layerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        centerPanel.add(layerScrollPane, BorderLayout.CENTER);
        
        // Ya no añadimos barra de botones abajo; todos van en el topPanel
        
        // Panel inferior: propiedades de capa seleccionada
        layerPropertiesPanel = createLayerPropertiesPanel();
        
        // Crear panel principal con controles de imagen reorganizados
        JPanel imageControlsPanel = createImageControlsPanel();
        
        // Organizar contenido en pestañas con márgenes reducidos
        JPanel layersContent = new JPanel(new BorderLayout());
        layersContent.add(centerPanel, BorderLayout.CENTER);
        layersContent.add(layerPropertiesPanel, BorderLayout.SOUTH);
        layersContent.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Capas", layersContent);
        tabbedPane.addTab("Controles de Imagen", imageControlsPanel);
        
        // Layout principal - botón CLARAMENTE SEPARADO de las pestañas
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(layerInfoLabel, BorderLayout.SOUTH);
        
        // Asegurar focus y habilitación para eventos de ratón
        setFocusable(true);
        addLayerButton.setFocusable(true);
        
        // Forzar actualización del layout y repintado
        SwingUtilities.invokeLater(() -> {
            validate();
            doLayout();
            revalidate();
            repaint();
            
            // Debug: imprimir dimensiones
            io.Log.log("TopPanel size: " + topPanel.getSize());
            io.Log.log("AddLayerButton size: " + addLayerButton.getSize());
            if (layerButtonsPanel != null) {
                io.Log.log("LayerButtonsPanel size: " + layerButtonsPanel.getSize());
                io.Log.log("Btn Subir size: " + moveUpButton.getSize());
                io.Log.log("Btn Bajar size: " + moveDownButton.getSize());
                io.Log.log("Btn Eliminar size: " + removeLayerButton.getSize());
                io.Log.log("Btn Duplicar size: " + duplicateButton.getSize());
                // Estados de visibilidad/habilitación
                io.Log.log("Btn Subir visible: " + moveUpButton.isVisible() + ", showing: " + moveUpButton.isShowing() + ", enabled: " + moveUpButton.isEnabled());
                io.Log.log("Btn Bajar visible: " + moveDownButton.isVisible() + ", showing: " + moveDownButton.isShowing() + ", enabled: " + moveDownButton.isEnabled());
                io.Log.log("Btn Eliminar visible: " + removeLayerButton.isVisible() + ", showing: " + removeLayerButton.isShowing() + ", enabled: " + removeLayerButton.isEnabled());
                io.Log.log("Btn Duplicar visible: " + duplicateButton.isVisible() + ", showing: " + duplicateButton.isShowing() + ", enabled: " + duplicateButton.isEnabled());
            }
        });
    }
    
    /**
     * Crea el panel principal con controles de imagen reorganizados
     */
    private JPanel createImageControlsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Reducir espaciado
        
        // === SECCIÓN SUPERIOR: CONTROLES GLOBALES (TOOLBAR ORIGINAL CON ICONOS) ===
        JPanel globalControlsPanel = new JPanel(new BorderLayout());
        globalControlsPanel.setBorder(new TitledBorder("Controles Globales (Imagen Principal + Capas)"));
        
        // Toolbar existente de ImagePreparingView - ¡CON LOS ICONOS ÚTILES!
        if (imagePreparingView != null && imagePreparingView.getToolBar() != null) {
            globalControlsPanel.add(imagePreparingView.getToolBar(), BorderLayout.CENTER);
        } else {
            JLabel noToolbarLabel = new JLabel("Toolbar no disponible", JLabel.CENTER);
            globalControlsPanel.add(noToolbarLabel, BorderLayout.CENTER);
        }
        
        // === SECCIÓN INFERIOR: CONTROLES DE IMAGEN DE FONDO ===
        JPanel backgroundControlsPanel = new JPanel(new BorderLayout());
        backgroundControlsPanel.setBorder(new TitledBorder("Controles de Imagen de Fondo (sin capas)"));
        
        // Crear controles reales para imagen de fondo usando ImagePreparingView
        JPanel backgroundSlidersPanel = createBackgroundImageControls();
        backgroundControlsPanel.add(backgroundSlidersPanel, BorderLayout.CENTER);
        
        // Organizar en el panel principal con espaciado reducido
        mainPanel.add(globalControlsPanel, BorderLayout.NORTH);
        mainPanel.add(backgroundControlsPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * Crea controles funcionales para modificar la imagen de fondo
     */
    private JPanel createBackgroundImageControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Crear sliders para ajustes específicos de imagen de fondo
        JPanel slidersGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 2, 1, 2); // Espaciado compacto
        gbc.anchor = GridBagConstraints.WEST;
        
        // Sliders para imagen de fondo usando los métodos de ImagePreparingView
        
        // Fila 0: Brillo de fondo
        JSlider bgBrightness = new JSlider(0, 400, 100); // 0.0 a 4.0
        JLabel bgBrightnessLabel = new JLabel("1.0");
        addCompactImageControl(slidersGrid, "Brillo:", bgBrightness, bgBrightnessLabel, gbc, 0);
        
        // Fila 1: Contraste de fondo  
        JSlider bgContrast = new JSlider(-100, 400, 100); // -1.0 a 4.0
        JLabel bgContrastLabel = new JLabel("1.0");
        addCompactImageControl(slidersGrid, "Contraste:", bgContrast, bgContrastLabel, gbc, 1);
        
        // Fila 2: Saturación de fondo
        JSlider bgSaturation = new JSlider(0, 400, 100); // 0.0 a 4.0
        JLabel bgSaturationLabel = new JLabel("1.0");
        addCompactImageControl(slidersGrid, "Saturación:", bgSaturation, bgSaturationLabel, gbc, 2);
        
        // Fila 3: Gamma de fondo
        JSlider bgGamma = new JSlider(5, 600, 100); // 0.05 a 6.0
        JLabel bgGammaLabel = new JLabel("1.0");
        addCompactImageControl(slidersGrid, "Gamma:", bgGamma, bgGammaLabel, gbc, 3);
        
        // Fila 4: Nitidez de fondo
        JSlider bgSharpness = new JSlider(50, 150, 100); // 0.5 a 1.5
        JLabel bgSharpnessLabel = new JLabel("1.0");
        addCompactImageControl(slidersGrid, "Nitidez:", bgSharpness, bgSharpnessLabel, gbc, 4);
        
        // Botón reset compacto
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; 
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(3, 2, 1, 2);
        JButton resetBgButton = new JButton("Reset Imagen de Fondo");
        resetBgButton.setPreferredSize(new Dimension(150, 22));
        slidersGrid.add(resetBgButton, gbc);
        
    // Configurar listeners funcionales para imagen de fondo
        setupBackgroundSliderListeners(bgBrightness, bgBrightnessLabel, 
                                     bgContrast, bgContrastLabel,
                                     bgSaturation, bgSaturationLabel,
                                     bgGamma, bgGammaLabel,
                                     bgSharpness, bgSharpnessLabel,
                                     resetBgButton);
        
        // Sincronizar con valores actuales del modelo
        syncBackgroundControlsWithModel(bgBrightness, bgBrightnessLabel, 
                                      bgContrast, bgContrastLabel,
                                      bgSaturation, bgSaturationLabel,
                                      bgGamma, bgGammaLabel,
                                      bgSharpness, bgSharpnessLabel);
        
        panel.add(slidersGrid);
        return panel;
    }
    
    /**
     * Sincroniza los controles con los valores actuales del modelo
     */
    private void syncBackgroundControlsWithModel(JSlider brightness, JLabel brightnessLabel,
                                                JSlider contrast, JLabel contrastLabel,
                                                JSlider saturation, JLabel saturationLabel,
                                                JSlider gamma, JLabel gammaLabel,
                                                JSlider sharpness, JLabel sharpnessLabel) {
        if (model == null) return;
        
        updatingControls = true;
        
        // Leer valores del modelo y actualizar controles
        Object brightnessObj = model.get(BrickGraphicsState.PrepareBrightness);
        if (brightnessObj instanceof Float) {
            Float brightnessValue = (Float) brightnessObj;
            brightness.setValue((int)(brightnessValue * 100));
            brightnessLabel.setText(String.format("%.2f", brightnessValue));
        }
        
        Object contrastObj = model.get(BrickGraphicsState.PrepareContrast);
        if (contrastObj instanceof Float) {
            Float contrastValue = (Float) contrastObj;
            contrast.setValue((int)(contrastValue * 100));
            contrastLabel.setText(String.format("%.2f", contrastValue));
        }
        
        Object saturationObj = model.get(BrickGraphicsState.PrepareSaturation);
        if (saturationObj instanceof Float) {
            Float saturationValue = (Float) saturationObj;
            saturation.setValue((int)(saturationValue * 100));
            saturationLabel.setText(String.format("%.2f", saturationValue));
        }
        
        Object gammaObj = model.get(BrickGraphicsState.PrepareGamma);
        if (gammaObj instanceof Float) {
            Float gammaValue = (Float) gammaObj;
            gamma.setValue((int)(gammaValue * 100));
            gammaLabel.setText(String.format("%.2f", gammaValue));
        }
        
        Object sharpnessObj = model.get(BrickGraphicsState.PrepareSharpness);
        if (sharpnessObj instanceof Float) {
            Float sharpnessValue = (Float) sharpnessObj;
            sharpness.setValue((int)(sharpnessValue * 100));
            sharpnessLabel.setText(String.format("%.2f", sharpnessValue));
        }
        
        updatingControls = false;
    }
    
    /**
     * Configura los listeners funcionales para modificar solo la imagen de fondo
     */
    private void setupBackgroundSliderListeners(JSlider brightness, JLabel brightnessLabel,
                                               JSlider contrast, JLabel contrastLabel,
                                               JSlider saturation, JLabel saturationLabel,
                                               JSlider gamma, JLabel gammaLabel,
                                               JSlider sharpness, JLabel sharpnessLabel,
                                               JButton resetButton) {
        
        // Brightness listener - modifica SOLO la imagen de fondo
        brightness.addChangeListener(e -> {
            if (updatingControls) return;
            io.Log.log("DEBUG: Listener - brightness.getValue() = " + brightness.getValue());
            backgroundBrightness = brightness.getValue() / 100.0f;
            io.Log.log("DEBUG: Listener - backgroundBrightness ANTES = " + backgroundBrightness);
            brightnessLabel.setText(String.format("%.2f", backgroundBrightness));
            io.Log.log("DEBUG: Listener - backgroundBrightness DESPUÉS = " + backgroundBrightness);
            io.Log.log("DEBUG: Slider de fondo - Brillo cambiado a: " + backgroundBrightness);
            notifyLayersChanged(); // Aplicar transformación a imagen de fondo
        });
        
        // Contrast listener - modifica SOLO la imagen de fondo
        contrast.addChangeListener(e -> {
            if (updatingControls) return;
            backgroundContrast = contrast.getValue() / 100.0f;
            contrastLabel.setText(String.format("%.2f", backgroundContrast));
            io.Log.log("DEBUG: Slider de fondo - Contraste cambiado a: " + backgroundContrast);
            notifyLayersChanged(); // Aplicar transformación a imagen de fondo
        });
        
        // Saturation listener - modifica SOLO la imagen de fondo
        saturation.addChangeListener(e -> {
            if (updatingControls) return;
            backgroundSaturation = saturation.getValue() / 100.0f;
            saturationLabel.setText(String.format("%.2f", backgroundSaturation));
            io.Log.log("DEBUG: Slider de fondo - Saturación cambiada a: " + backgroundSaturation);
            notifyLayersChanged(); // Aplicar transformación a imagen de fondo
        });
        
        // Gamma listener - modifica SOLO la imagen de fondo
        gamma.addChangeListener(e -> {
            if (updatingControls) return;
            backgroundGamma = gamma.getValue() / 100.0f;
            gammaLabel.setText(String.format("%.2f", backgroundGamma));
            notifyLayersChanged(); // Aplicar transformación a imagen de fondo
        });
        
        // Sharpness listener - modifica SOLO la imagen de fondo
        sharpness.addChangeListener(e -> {
            if (updatingControls) return;
            backgroundSharpness = sharpness.getValue() / 100.0f;
            sharpnessLabel.setText(String.format("%.2f", backgroundSharpness));
            notifyLayersChanged(); // Aplicar transformación a imagen de fondo
        });
        
        // Reset button - resetea SOLO la imagen de fondo
        resetButton.addActionListener(e -> {
            updatingControls = true;
            brightness.setValue(100);
            contrast.setValue(100);
            saturation.setValue(100);
            gamma.setValue(100);
            sharpness.setValue(100);
            brightnessLabel.setText("1.0");
            contrastLabel.setText("1.0");
            saturationLabel.setText("1.0");
            gammaLabel.setText("1.0");
            sharpnessLabel.setText("1.0");
            updatingControls = false;
            
            // Reset valores de imagen de fondo
            backgroundBrightness = 1.0f;
            backgroundContrast = 1.0f;
            backgroundSaturation = 1.0f;
            backgroundGamma = 1.0f;
            backgroundSharpness = 1.0f;
            notifyLayersChanged(); // Aplicar reset a imagen de fondo
        });
    }
    
    /**
     * Añade un control compacto al panel  
     */
    private void addCompactImageControl(JPanel panel, String label, JSlider slider, JLabel valueLabel, GridBagConstraints gbc, int row) {
        // Label
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.35;
        JLabel labelComp = new JLabel(label);
        labelComp.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(labelComp, gbc);
        
        // Slider
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.40;
        panel.add(slider, gbc);
        
        // Value label
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.25;
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        panel.add(valueLabel, gbc);
    }
    
    /**
     * Crea el panel de propiedades de capa seleccionada
     */
    private JPanel createLayerPropertiesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Propiedades de Capa"));
        
        // Panel de propiedades básicas
        JPanel basicProps = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Fila 0: Visibilidad y modo de fusión
        gbc.gridx = 0; gbc.gridy = 0;
        basicProps.add(visibilityCheckBox, gbc);
        gbc.gridx = 1;
        basicProps.add(new JLabel("Modo:"), gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        basicProps.add(blendModeCombo, gbc);
        
        // Fila 1: Opacidad (movido a ajustes de imagen)
        // addImageTransformControl(basicProps, "Opacidad:", opacitySlider, opacityLabel, gbc, 1);
        
        // Fila 2: Posición X
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; // Cambiado gridy a 1
        basicProps.add(new JLabel("Pos X:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        basicProps.add(positionXSpinner, gbc);

        // Fila 3: Posición Y
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; // Cambiado gridy a 2
        basicProps.add(new JLabel("Pos Y:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        basicProps.add(positionYSpinner, gbc);
        
        // Panel de transformaciones de imagen
        JPanel imageTransformsPanel = new JPanel(new GridBagLayout());
        imageTransformsPanel.setBorder(new TitledBorder("Ajustes de Imagen"));
        
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.insets = new Insets(2, 5, 2, 5);
        igbc.anchor = GridBagConstraints.WEST;
        
        // Añadir controles de transformación
        addImageTransformControl(imageTransformsPanel, "Opacidad:", opacitySlider, opacityLabel, igbc, 0);
        addImageTransformControl(imageTransformsPanel, "Brillo:", brightnessSlider, brightnessLabel, igbc, 1);
        addImageTransformControl(imageTransformsPanel, "Contraste:", contrastSlider, contrastLabel, igbc, 2);
        addImageTransformControl(imageTransformsPanel, "Saturación:", saturationSlider, saturationLabel, igbc, 3);
        addImageTransformControl(imageTransformsPanel, "Gamma:", gammaSlider, gammaLabel, igbc, 4);
        addImageTransformControl(imageTransformsPanel, "Nitidez:", sharpnessSlider, sharpnessLabel, igbc, 5);
        addImageTransformControl(imageTransformsPanel, "Escala:", scaleSlider, scaleLabel, igbc, 6);
        
        // Fila para botón reset
        igbc.gridx = 0; igbc.gridy = 7; igbc.gridwidth = 2; igbc.fill = GridBagConstraints.HORIZONTAL;
        imageTransformsPanel.add(resetAdjustmentsButton, igbc);
        
        panel.add(basicProps, BorderLayout.NORTH);
        panel.add(imageTransformsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Añade un control de transformación de imagen al panel
     */
    private void addImageTransformControl(JPanel panel, String label, JSlider slider, JLabel valueLabel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(slider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(valueLabel, gbc);
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
                }
                updateLayerControls();
            }
        });
        
        // Botones
        addLayerButton.addActionListener(e -> addLayer());
        removeLayerButton.addActionListener(e -> removeSelectedLayer());
        duplicateButton.addActionListener(e -> duplicateSelectedLayer());
        
        // Mover capa hacia arriba
        moveUpButton.addActionListener(e -> {
            Layer selectedLayer = layerList.getSelectedValue();
            if (selectedLayer != null) {
                int oldIndex = layerManager.getLayers().indexOf(selectedLayer);
                if (layerManager.moveLayerUp(selectedLayer)) {
                    updateUI();
                    layerList.setSelectedIndex(oldIndex - 1);
                    notifyLayersChanged();
                    // Persist only external artifacts (layers.json + overlays) after move
                    try {
                        layerManager.autosaveArtifacts();
                        io.Log.log("DEBUG: IntegratedImageLayerPanel - Artifacts autosaved after move up");
                    } catch (Exception ex) {
                        io.Log.log("WARN: IntegratedImageLayerPanel - Artifacts autosave after move up failed: " + ex.getMessage());
                    }
                }
            }
        });

        // Mover capa hacia abajo
        moveDownButton.addActionListener(e -> {
            Layer selectedLayer = layerList.getSelectedValue();
            if (selectedLayer != null) {
                int oldIndex = layerManager.getLayers().indexOf(selectedLayer);
                if (layerManager.moveLayerDown(selectedLayer)) {
                    updateUI();
                    layerList.setSelectedIndex(oldIndex + 1);
                    notifyLayersChanged();
                    // Persist only external artifacts (layers.json + overlays) after move
                    try {
                        layerManager.autosaveArtifacts();
                        io.Log.log("DEBUG: IntegratedImageLayerPanel - Artifacts autosaved after move down");
                    } catch (Exception ex) {
                        io.Log.log("WARN: IntegratedImageLayerPanel - Artifacts autosave after move down failed: " + ex.getMessage());
                    }
                }
            }
        });
        
        // Checkbox global
        enableLayersCheckBox.addActionListener(e -> {
            layerManager.setLayersEnabled(enableLayersCheckBox.isSelected());
            notifyLayersChanged();
        });
        
        // NUEVOS: Controles de Paint Overlay
        paintOverlayCheckBox.addActionListener(e -> {
            if (brickedView != null) {
                brickedView.setPaintOverlayEnabled(paintOverlayCheckBox.isSelected());
                brickedView.repaint();
            }
        });
        
        clearPaintButton.addActionListener(e -> {
            if (brickedView != null) {
                brickedView.clearPaintOverlay();
                brickedView.repaint();
            }
        });
        
        // Controles de capa
        opacitySlider.addChangeListener(e -> updateLayerOpacity());
        visibilityCheckBox.addActionListener(e -> updateLayerVisibility());
        blendModeCombo.addActionListener(e -> updateLayerBlendMode());
        
        // Controles de posición
        positionXSpinner.addChangeListener(e -> updateLayerPosition());
        positionYSpinner.addChangeListener(e -> updateLayerPosition());
        
        // Añadir wheel listeners para los spinners de posición
        addWheelListenerToSpinner(positionXSpinner);
        addWheelListenerToSpinner(positionYSpinner);
        
        // Controles de transformaciones de imagen
        ChangeListener imageTransformListener = e -> updateLayerImageTransforms();
        brightnessSlider.addChangeListener(imageTransformListener);
        contrastSlider.addChangeListener(imageTransformListener);
        saturationSlider.addChangeListener(imageTransformListener);
        gammaSlider.addChangeListener(imageTransformListener);
        sharpnessSlider.addChangeListener(imageTransformListener);
        scaleSlider.addChangeListener(imageTransformListener);
        
        // Botón reset
        resetAdjustmentsButton.addActionListener(e -> resetLayerAdjustments());
    }

    /**
     * Configura atajos de teclado globales para acciones frecuentes.
     * Se añaden al InputMap/ActionMap del panel principal para que funcionen
     * independientemente del foco (mientras el panel sea foco descendiente).
     */
    private void setupKeyBindings() {
        // Usar WHEN_ANCESTOR_OF_FOCUSED_COMPONENT para mayor alcance.
        InputMap inputMap = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = this.getActionMap();

        // Añadir capa (Ctrl+N)
        KeyStroke ksAdd = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        inputMap.put(ksAdd, "add-layer");
        actionMap.put("add-layer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (addLayerButton.isEnabled()) addLayerButton.doClick(); }
        });

        // Eliminar capa (Delete / Backspace)
        KeyStroke ksDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        KeyStroke ksBack = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
        inputMap.put(ksDel, "remove-layer");
        inputMap.put(ksBack, "remove-layer");
        actionMap.put("remove-layer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (removeLayerButton.isEnabled()) removeLayerButton.doClick(); }
        });

        // Duplicar capa (Ctrl+D)
        KeyStroke ksDup = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK);
        inputMap.put(ksDup, "duplicate-layer");
        actionMap.put("duplicate-layer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (duplicateButton.isEnabled()) duplicateButton.doClick(); }
        });

        // Subir capa (Alt+Up)
        KeyStroke ksUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);
        inputMap.put(ksUp, "move-up-layer");
        actionMap.put("move-up-layer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (moveUpButton.isEnabled()) moveUpButton.doClick(); }
        });

        // Bajar capa (Alt+Down)
        KeyStroke ksDown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
        inputMap.put(ksDown, "move-down-layer");
        actionMap.put("move-down-layer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (moveDownButton.isEnabled()) moveDownButton.doClick(); }
        });

        // Toggle visibilidad de capa seleccionada (Space)
        KeyStroke ksSpace = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        inputMap.put(ksSpace, "toggle-layer-visibility");
        actionMap.put("toggle-layer-visibility", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                Layer selected = layerManager.getSelectedLayer();
                if (selected != null && visibilityCheckBox.isEnabled()) {
                    visibilityCheckBox.setSelected(!visibilityCheckBox.isSelected());
                    updateLayerVisibility();
                }
            }
        });

        // Toggle habilitación global de capas (Ctrl+Space)
        KeyStroke ksCtrlSpace = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK);
        inputMap.put(ksCtrlSpace, "toggle-global-layers");
        actionMap.put("toggle-global-layers", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (enableLayersCheckBox.isEnabled()) {
                    enableLayersCheckBox.setSelected(!enableLayersCheckBox.isSelected());
                    layerManager.setLayersEnabled(enableLayersCheckBox.isSelected());
                    notifyLayersChanged();
                }
            }
        });
    }
    
    /**
     * Añade una nueva capa desde un archivo
     */
    private void addLayer() {
    io.Log.log("DEBUG: IntegratedImageLayerPanel - Iniciando addLayer()");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            io.Log.log("DEBUG: IntegratedImageLayerPanel - Archivo seleccionado: " + selectedFile.getAbsolutePath());
            try {
                Layer layer = layerManager.addLayerFromFile(
                    selectedFile.getAbsolutePath(), 
                    new Point(0, 0)
                );
                io.Log.log("DEBUG: IntegratedImageLayerPanel - Capa creada: " + layer.getName());
                layerManager.setSelectedLayer(layer);
                io.Log.log("DEBUG: IntegratedImageLayerPanel - Capa seleccionada");
                updateUI();
                io.Log.log("DEBUG: IntegratedImageLayerPanel - UI actualizada");
                notifyLayersChanged();
                io.Log.log("DEBUG: IntegratedImageLayerPanel - Notificación enviada");
            } catch (IOException ex) {
                io.Log.log("DEBUG: IntegratedImageLayerPanel - Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, 
                    "Error al cargar la imagen: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Elimina la capa seleccionada
     */
    private void removeSelectedLayer() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            layerManager.removeLayer(selected);
            updateUI();
            notifyLayersChanged();
        }
    }
    
    /**
     * Duplica la capa seleccionada
     */
    private void duplicateSelectedLayer() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            Layer duplicate = layerManager.duplicateLayer(selected);
            layerManager.setSelectedLayer(duplicate);
            updateUI();
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la opacidad de la capa seleccionada
     */
    private void updateLayerOpacity() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            float opacity = opacitySlider.getValue() / 100.0f;
            opacityLabel.setText(String.format("%.0f%%", opacity * 100));
            selected.setOpacity(opacity);
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la visibilidad de la capa seleccionada
     */
    private void updateLayerVisibility() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            selected.setVisible(visibilityCheckBox.isSelected());
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza el modo de fusión de la capa seleccionada
     */
    private void updateLayerBlendMode() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null && blendModeCombo.getSelectedItem() != null) {
            selected.setBlendMode((Layer.BlendMode) blendModeCombo.getSelectedItem());
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza la posición de la capa seleccionada
     */
    private void updateLayerPosition() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            int x = (Integer) positionXSpinner.getValue();
            int y = (Integer) positionYSpinner.getValue();
            selected.setPosition(new Point(x, y));
            notifyLayersChanged();
        }
    }
    
    /**
     * Actualiza las transformaciones de imagen de la capa seleccionada
     */
    private void updateLayerImageTransforms() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            // Convertir valores de slider (0-200) a factores (0.0-2.0)
            float brightness = brightnessSlider.getValue() / 100.0f;
            float contrast = contrastSlider.getValue() / 100.0f;
            float saturation = saturationSlider.getValue() / 100.0f;
            float gamma = gammaSlider.getValue() / 100.0f;
            float sharpness = sharpnessSlider.getValue() / 100.0f;
            float scale = scaleSlider.getValue() / 100.0f;
            
            // Actualizar etiquetas de valores
            brightnessLabel.setText(String.format("%.0f%%", brightness * 100));
            contrastLabel.setText(String.format("%.0f%%", contrast * 100));
            saturationLabel.setText(String.format("%.0f%%", saturation * 100));
            gammaLabel.setText(String.format("%.2f", gamma));
            sharpnessLabel.setText(String.format("%.0f%%", sharpness * 100));
            scaleLabel.setText(String.format("%.0f%%", scale * 100));
            
            io.Log.log("DEBUG: Aplicando transformaciones - Brillo: " + brightness + 
                             ", Contraste: " + contrast + ", Saturación: " + saturation + 
                             ", Gamma: " + gamma + ", Nitidez: " + sharpness + 
                             ", Escala: " + scale);
            
            selected.setBrightness(brightness);
            selected.setContrast(contrast);
            selected.setSaturation(saturation);
            selected.setGamma(gamma);
            selected.setSharpness(sharpness);
            selected.setScale(scale);
            
            notifyLayersChanged();
        }
    }
    
    /**
     * Restablece todos los ajustes de la capa seleccionada a valores por defecto (excepto posición y escala)
     */
    private void resetLayerAdjustments() {
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            // Guardar posición y escala actuales antes del reset
            Point currentPosition = new Point(selected.getPosition());
            float currentScale = selected.getScale();
            
            updatingControls = true;
            try {
                // Restablecer valores por defecto (NO incluir posición ni escala)
                opacitySlider.setValue(100);
                // NO resetear: positionXSpinner.setValue(0);
                // NO resetear: positionYSpinner.setValue(0);
                brightnessSlider.setValue(100);
                contrastSlider.setValue(100);
                saturationSlider.setValue(100);
                gammaSlider.setValue(100);
                sharpnessSlider.setValue(100);
                // NO resetear: scaleSlider.setValue(100);
                
                // Restablecer etiquetas a valores por defecto (mantener escala actual)
                opacityLabel.setText("100%");
                brightnessLabel.setText("100%");
                contrastLabel.setText("100%");
                saturationLabel.setText("100%");
                gammaLabel.setText("1.00");
                sharpnessLabel.setText("100%");
                scaleLabel.setText(String.format("%.0f%%", currentScale * 100)); // Mantener escala actual
                
                // Aplicar valores por defecto a la capa (mantener posición y escala actuales)
                selected.setOpacity(1.0f);
                // NO resetear: selected.setPosition(new Point(0, 0));
                selected.setPosition(currentPosition); // Mantener posición actual
                selected.setBrightness(1.0f);
                selected.setContrast(1.0f);
                selected.setSaturation(1.0f);
                selected.setGamma(1.0f);
                selected.setSharpness(1.0f);
                // NO resetear: selected.setScale(1.0f);
                selected.setScale(currentScale); // Mantener escala actual
                
                notifyLayersChanged();
            } finally {
                updatingControls = false;
            }
        }
    }
    
    /**
     * Actualiza los controles basados en la capa seleccionada
     */
    private void updateLayerControls() {
        updatingControls = true;
        try {
            Layer selected = layerManager.getSelectedLayer();
            boolean hasSelection = selected != null;
            
            // Habilitar/deshabilitar controles
            opacitySlider.setEnabled(hasSelection);
            visibilityCheckBox.setEnabled(hasSelection);
            blendModeCombo.setEnabled(hasSelection);
            positionXSpinner.setEnabled(hasSelection);
            positionYSpinner.setEnabled(hasSelection);
            brightnessSlider.setEnabled(hasSelection);
            contrastSlider.setEnabled(hasSelection);
            saturationSlider.setEnabled(hasSelection);
            gammaSlider.setEnabled(hasSelection);
            sharpnessSlider.setEnabled(hasSelection);
            scaleSlider.setEnabled(hasSelection);
            resetAdjustmentsButton.setEnabled(hasSelection);
            removeLayerButton.setEnabled(hasSelection);
            duplicateButton.setEnabled(hasSelection);
            
            if (hasSelection) {
                // Actualizar valores de controles
                opacitySlider.setValue((int) (selected.getOpacity() * 100));
                visibilityCheckBox.setSelected(selected.isVisible());
                blendModeCombo.setSelectedItem(selected.getBlendMode());
                
                // Actualizar valores de posición
                positionXSpinner.setValue(selected.getPosition().x);
                positionYSpinner.setValue(selected.getPosition().y);
                
                // Actualizar controles de transformación de imagen
                brightnessSlider.setValue((int) (selected.getBrightness() * 100));
                contrastSlider.setValue((int) (selected.getContrast() * 100));
                saturationSlider.setValue((int) (selected.getSaturation() * 100));
                gammaSlider.setValue((int) (selected.getGamma() * 100));
                sharpnessSlider.setValue((int) (selected.getSharpness() * 100));
                scaleSlider.setValue((int) (selected.getScale() * 100));
                
                // Actualizar etiquetas con valores actuales
                opacityLabel.setText(String.format("%.0f%%", selected.getOpacity() * 100));
                brightnessLabel.setText(String.format("%.0f%%", selected.getBrightness() * 100));
                contrastLabel.setText(String.format("%.0f%%", selected.getContrast() * 100));
                saturationLabel.setText(String.format("%.0f%%", selected.getSaturation() * 100));
                gammaLabel.setText(String.format("%.2f", selected.getGamma()));
                sharpnessLabel.setText(String.format("%.0f%%", selected.getSharpness() * 100));
                scaleLabel.setText(String.format("%.0f%%", selected.getScale() * 100));
            }
        } finally {
            updatingControls = false;
        }
    }
    
    /**
     * Actualiza la lista de capas
     */
    private void updateLayerList() {
        io.Log.log("DEBUG: updateLayerList() - Iniciando actualización de lista");
        if (listModel == null) {
            io.Log.log("DEBUG: updateLayerList() - listModel es null");
            return;
        }
        
        io.Log.log("DEBUG: updateLayerList() - Limpiando lista existente");
        listModel.clear();
        
        List<Layer> layers = layerManager.getLayers();
        io.Log.log("DEBUG: updateLayerList() - Número de capas: " + layers.size());
        
        for (Layer layer : layers) {
            io.Log.log("DEBUG: updateLayerList() - Añadiendo capa a lista: " + layer.getName());
            listModel.addElement(layer);
        }
        
        // Mantener selección
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            io.Log.log("DEBUG: updateLayerList() - Seleccionando capa: " + selected.getName());
            layerList.setSelectedValue(selected, true);
        }
        
        io.Log.log("DEBUG: updateLayerList() - Lista actualizada, tamaño del modelo: " + listModel.getSize());
        
        // Forzar actualización visual del JList
        SwingUtilities.invokeLater(() -> {
            layerList.revalidate();
            layerList.repaint();
            // También actualizar el scroll pane si está disponible
            if (layerList.getParent() != null && layerList.getParent().getParent() instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) layerList.getParent().getParent();
                scrollPane.revalidate();
                scrollPane.repaint();
            }
            io.Log.log("DEBUG: updateLayerList() - Forzada actualización visual del JList");
        });
    }
    
    /**
     * Actualiza toda la interfaz
     */
    public void updateUI() {
        if (layerManager == null || enableLayersCheckBox == null) {
            io.Log.log("DEBUG: updateUI() - Componentes no inicializados");
            return; // Skip si no están inicializados
        }
        
		io.Log.log("DEBUG: updateUI() - Iniciando actualización completa");
        updateLayerList();
        updateLayerControls();
        enableLayersCheckBox.setSelected(layerManager.isLayersEnabled());
        
        // Actualizar información
        int layerCount = layerManager.getLayers().size();
        layerInfoLabel.setText("Capas: " + layerCount);
		io.Log.log("DEBUG: updateUI() - Actualización completa finalizada");
    }
    
    /**
     * Clase de prueba para testing
     */
    private static class TestLayer extends Layer {
        public TestLayer(String name) {
            super(name, null, new Point(0, 0), "test");
        }
    }
    
    /**
     * Notifica que las capas han cambiado
     */
    private void notifyLayersChanged() {
    // notifyLayersChanged called
        if (onLayersChangedCallback != null) {
            io.Log.log("DEBUG: IntegratedImageLayerPanel - Ejecutando callback");
            onLayersChangedCallback.run();
            io.Log.log("DEBUG: IntegratedImageLayerPanel - Callback ejecutado");
        } else {
            io.Log.log("DEBUG: IntegratedImageLayerPanel - WARNING: Callback es null!");
        }
    }
    
    /**
     * Obtiene los valores actuales de transformación de imagen de fondo
     */
    public float getBackgroundBrightness() {
        io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundBrightness() devuelve: " + backgroundBrightness);
        return backgroundBrightness;
    }
    
    public float getBackgroundContrast() {
        io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundContrast() devuelve: " + backgroundContrast);
        return backgroundContrast;
    }
    
    public float getBackgroundSaturation() {
        io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundSaturation() devuelve: " + backgroundSaturation);
        return backgroundSaturation;
    }
    
    public float getBackgroundGamma() {
        io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundGamma() devuelve: " + backgroundGamma);
        return backgroundGamma;
    }
    
    public float getBackgroundSharpness() {
        io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundSharpness() devuelve: " + backgroundSharpness);
        return backgroundSharpness;
    }

    /**
     * Establece el callback para notificar cambios en las capas
     */
    public void setOnLayersChangedCallback(Runnable callback) {
        this.onLayersChangedCallback = callback;
    }
    
    /**
     * Añade un listener de rueda del ratón a un spinner para cambiar valores con scroll
     */
    private void addWheelListenerToSpinner(JSpinner spinner) {
        spinner.addMouseWheelListener(e -> {
            if (!spinner.isEnabled()) return;
            
            // Obtener el modelo del spinner
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            int currentValue = (Integer) model.getValue();
            int stepSize = model.getStepSize().intValue();
            
            // Calcular nuevo valor basado en la dirección de la rueda
            int delta = e.getWheelRotation() * stepSize * -1; // Invertir para que sea más intuitivo
            int newValue = currentValue + delta;
            
            // Verificar que esté dentro de los límites
            Integer min = (Integer) model.getMinimum();
            Integer max = (Integer) model.getMaximum();
            
            if (min != null && newValue < min) {
                newValue = min;
            }
            if (max != null && newValue > max) {
                newValue = max;
            }
            
            // Actualizar el valor
            spinner.setValue(newValue);
            
            // Consumir el evento para evitar que se propague
            e.consume();
        });
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
                String text = "📄 " + layer.getName(); // Emoji para hacer más visible
                
                if (!layer.isVisible()) {
                    text += " (oculta)";
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else {
                    setFont(getFont().deriveFont(Font.BOLD)); // Negrita para mayor visibilidad
                }
                
                if (layer.getOpacity() < 1.0f) {
                    text += String.format(" (%.0f%%)", layer.getOpacity() * 100);
                }
                
                setText(text);
                
                // Colores más contrastados para mejor visibilidad
                if (isSelected) {
                    setBackground(Color.BLUE);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
            }
            
            return this;
        }
    }
}