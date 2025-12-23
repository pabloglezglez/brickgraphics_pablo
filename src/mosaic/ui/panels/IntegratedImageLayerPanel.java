package mosaic.ui.panels;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import colors.LEGOColorGrid;
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
    private mosaic.controllers.MainController mainController; // NUEVO: Referencia para acceder al directorio de imagen principal
    
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
    
    // Spinners para controles de imagen de fondo
    private JSpinner backgroundBrightnessSpinner;
    private JSpinner backgroundContrastSpinner;
    private JSpinner backgroundSaturationSpinner;
    private JSpinner backgroundGammaSpinner;
    private JSpinner backgroundSharpnessSpinner;
    private JSlider backgroundBrightnessSlider;
    private JSlider backgroundContrastSlider;
    private JSlider backgroundSaturationSlider;
    private JSlider backgroundGammaSlider;
    private JSlider backgroundSharpnessSlider;
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
    private JSpinner scaleSpinner; // CAMBIADO: de JTextField a JSpinner como posición X/Y
    private JLabel opacityLabel;
    
    // Spinners para entrada directa de valores
    private JSpinner brightnessSpinner;
    private JSpinner contrastSpinner;
    private JSpinner saturationSpinner;
    private JSpinner gammaSpinner;
    private JSpinner sharpnessSpinner;
    
    // Controles de posición
    private JSpinner positionXSpinner;
    private JSpinner positionYSpinner;
    
    // Callbacks
    private Runnable onLayersChangedCallback;
    
    // Sistema de debounce para optimizar rendimiento
    private Timer debounceTimer;
    private Timer backgroundDebounceTimer;
    private boolean pendingLayerUpdate = false;
    private boolean pendingBackgroundUpdate = false;
    private static final int DEBOUNCE_DELAY_MS = 10; // Reducido a 10ms para máxima respuesta
    private boolean updatingControls = false;
    private final Color defaultPanelBackground = resolveDefaultPanelBackground();
    
    /**
     * Constructor del panel integrado
     */
    public IntegratedImageLayerPanel(LayerManager layerManager, ImagePreparingView imagePreparingView, Model<BrickGraphicsState> model, mosaic.ui.BrickedView brickedView, mosaic.controllers.MainController mainController) {
        this.layerManager = layerManager;
        this.imagePreparingView = imagePreparingView;
        this.model = model;
        this.brickedView = brickedView;
        this.mainController = mainController; // NUEVO: Guardar referencia al MainController
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupKeyBindings();
        setupZoomListener(); // NUEVO: Configurar listener para cambios de zoom
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
        
        // Habilitar drag and drop para reordenar capas
        layerList.setDragEnabled(true);
        layerList.setDropMode(DropMode.INSERT);
        layerList.setTransferHandler(new LayerTransferHandler());
        
        // Hacer la lista más visible para debug
        layerList.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        layerList.setBackground(Color.WHITE);
        
        // Botones de gestión de capas con texto más corto
        addLayerButton = new JButton("+");
        addLayerButton.setToolTipText("Add new layer from image (Ctrl+N)");
        removeLayerButton = new JButton("✕");
        removeLayerButton.setToolTipText("Delete selected layer (Del/Backspace)");
        duplicateButton = new JButton("⧉");
        duplicateButton.setToolTipText("Duplicate selected layer (Ctrl+D)");
        moveUpButton = new JButton("↑");
        moveUpButton.setToolTipText("Move selected layer up (Alt+↑)");
        moveDownButton = new JButton("↓");
        moveDownButton.setToolTipText("Move selected layer down (Alt+↓)");
        
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
        
        enableLayersCheckBox = new JCheckBox("Layers", true);
        enableLayersCheckBox.setToolTipText("Enable or disable all layers");
        
        // Información de capas
        layerInfoLabel = new JLabel("No layers");
        layerInfoLabel.setFont(layerInfoLabel.getFont().deriveFont(Font.ITALIC));
        
        // Controles de propiedades de capa
        opacitySlider = createImageSlider("Opacity", 0, 100, 100);
        
        visibilityCheckBox = new JCheckBox("Visible", true);
        blendModeCombo = new JComboBox<>(Layer.BlendMode.values());
        blendModeCombo.setPrototypeDisplayValue(Layer.BlendMode.SOFT_LIGHT);
        blendModeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Layer.BlendMode && comp instanceof JLabel label) {
                    label.setText(getBlendModeDisplayName((Layer.BlendMode) value));
                }
                return comp;
            }
        });
        
        // Controles de transformaciones de imagen (rango -100 a 100, neutro en 0)
        brightnessSlider = createImageSlider("Brightness", -100, 100, 0);
        contrastSlider = createImageSlider("Contrast", -100, 100, 0);
        saturationSlider = createImageSlider("Saturation", -100, 100, 0);
        gammaSlider = createImageSlider("Gamma", 50, 200, 100); // 0.5 to 2.0 (x100), neutral at 100
        sharpnessSlider = createImageSlider("Sharpness", -100, 100, 0);
        // Permitir escalados hasta 300% (3x)
        scaleSlider = createImageSlider("Scale", 1, 300, 100); // 1% a 300%, defecto 100%
        
        // Etiquetas para mostrar valores numéricos
        opacityLabel = new JLabel("100%");
        brightnessLabel = new JLabel("0%");
        contrastLabel = new JLabel("0%");
        saturationLabel = new JLabel("0%");
        gammaLabel = new JLabel("1.00");
        sharpnessLabel = new JLabel("0%");
        scaleSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 300, 5)); // Spinner para escala: 1%-300%, step 5%
        scaleSpinner.setToolTipText("Layer scale (1%-300%, 5% per step - use Shift x10 or Ctrl x100)");
        
        // Spinners para los demás controles de transformación
        brightnessSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // -100% a +100%, neutro en 0
        brightnessSpinner.setToolTipText("Layer brightness (-100% to +100%, 5% per step)");
        
        contrastSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // -100% a +100%, neutro en 0
        contrastSpinner.setToolTipText("Layer contrast (-100% to +100%, 5% per step)");
        
        saturationSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // -100% a +100%, neutro en 0
        saturationSpinner.setToolTipText("Layer saturation (-100% to +100%, 5% per step)");
        
        gammaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.5, 2.0, 0.1)); // 0.5-2.0, neutro en 1.0
        gammaSpinner.setToolTipText("Layer gamma (0.5-2.0, 0.1 per step)");
        
        sharpnessSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // -100% a +100%, neutro en 0
        sharpnessSpinner.setToolTipText("Layer sharpness (-100% to +100%, 5% per step)");
        
        // Configurar ancho mínimo para las etiquetas para alineación
        int labelWidth = 50;
        opacityLabel.setPreferredSize(new Dimension(labelWidth, opacityLabel.getPreferredSize().height));
        brightnessLabel.setPreferredSize(new Dimension(labelWidth, brightnessLabel.getPreferredSize().height));
        contrastLabel.setPreferredSize(new Dimension(labelWidth, contrastLabel.getPreferredSize().height));
        saturationLabel.setPreferredSize(new Dimension(labelWidth, saturationLabel.getPreferredSize().height));
        gammaLabel.setPreferredSize(new Dimension(labelWidth, gammaLabel.getPreferredSize().height));
        sharpnessLabel.setPreferredSize(new Dimension(labelWidth, sharpnessLabel.getPreferredSize().height));
        scaleSpinner.setPreferredSize(new Dimension(80, scaleSpinner.getPreferredSize().height)); // Spinner más ancho
        
        // Configurar ancho de los nuevos spinners
        brightnessSpinner.setPreferredSize(new Dimension(80, brightnessSpinner.getPreferredSize().height));
        contrastSpinner.setPreferredSize(new Dimension(80, contrastSpinner.getPreferredSize().height));
        saturationSpinner.setPreferredSize(new Dimension(80, saturationSpinner.getPreferredSize().height));
        gammaSpinner.setPreferredSize(new Dimension(80, gammaSpinner.getPreferredSize().height));
        sharpnessSpinner.setPreferredSize(new Dimension(80, sharpnessSpinner.getPreferredSize().height));
        
        // Alinear texto a la derecha para mejor apariencia
        opacityLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        brightnessLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        contrastLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        saturationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gammaLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sharpnessLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        // scaleSpinner ya está configurado en su inicialización
        
        // Botón reset para ajustes
        resetAdjustmentsButton = new JButton("Reset adjustments");
        resetAdjustmentsButton.setToolTipText("Reset all layer adjustments to their default values");
        
        // Spinners de posición (rango amplio para permitir desplazamientos grandes)
        positionXSpinner = new JSpinner(new SpinnerNumberModel(0, -99999, 99999, 1)); // Paso constante: 1 mosaico
        positionXSpinner.setToolTipText("Layer X position (1 mosaic unit per step - use Shift x10 or Ctrl x100)");
        positionYSpinner = new JSpinner(new SpinnerNumberModel(0, -99999, 99999, 1)); // Paso constante: 1 mosaico
        positionYSpinner.setToolTipText("Layer Y position (1 mosaic unit per step - use Shift x10 or Ctrl x100)");
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
        setBorder(new TitledBorder("Layers & Image"));
        setPreferredSize(new Dimension(320, 720));
        setMinimumSize(new Dimension(300, 520));
        setBackground(defaultPanelBackground);
        
    // Panel superior como JToolBar (dos barras apiladas)
    topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
    topPanel.setOpaque(true);
    topPanel.setBackground(defaultPanelBackground);

    // Barra 1: controles principales
    JToolBar mainBar = new JToolBar();
    mainBar.setFloatable(false);
    mainBar.setRollover(true);
    mainBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    mainBar.setOpaque(true);
    mainBar.setBackground(defaultPanelBackground);
    
    // Configurar todos los componentes como no focusables
    enableLayersCheckBox.setFocusable(false);
    addLayerButton.setFocusable(false);
    removeLayerButton.setFocusable(false);
    duplicateButton.setFocusable(false);
    moveUpButton.setFocusable(false);
    moveDownButton.setFocusable(false);
    
    // Añadir componentes con espaciado adecuado
    mainBar.add(enableLayersCheckBox);
    mainBar.addSeparator(new Dimension(8, 0));
    
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
    orderBar.setOpaque(true);
    orderBar.setBackground(defaultPanelBackground);
    
    orderBar.add(new JLabel("Order: "));
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
        centerPanel.setOpaque(true);
        centerPanel.setBackground(defaultPanelBackground);
        centerPanel.setBorder(new TitledBorder("Layers"));
        
        JScrollPane layerScrollPane = new JScrollPane(layerList);
        layerScrollPane.setPreferredSize(new Dimension(200, 150));
        layerScrollPane.setMinimumSize(new Dimension(180, 120));
        layerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        layerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        layerScrollPane.getViewport().setBackground(Color.WHITE);
        layerScrollPane.getViewport().setOpaque(true);
        centerPanel.add(layerScrollPane, BorderLayout.CENTER);
        
        // Ya no añadimos barra de botones abajo; todos van en el topPanel
        
        // Panel inferior: propiedades de capa seleccionada
        layerPropertiesPanel = createLayerPropertiesPanel();
        
        // Crear panel principal con controles de imagen reorganizados
        JPanel imageControlsPanel = createImageControlsPanel();
        
        // Organizar contenido en pestañas con márgenes reducidos
        JPanel layersContent = new JPanel(new BorderLayout());
        layersContent.setOpaque(true);
        layersContent.setBackground(defaultPanelBackground);
        layersContent.add(centerPanel, BorderLayout.CENTER);
        layersContent.add(layerPropertiesPanel, BorderLayout.SOUTH);
        layersContent.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tabbedPane.setOpaque(true);
        tabbedPane.setBackground(defaultPanelBackground);
        tabbedPane.addTab("Layers", layersContent);
        tabbedPane.setBackgroundAt(0, defaultPanelBackground);

        JScrollPane imageControlsScroll = new JScrollPane(imageControlsPanel);
        imageControlsScroll.setBorder(BorderFactory.createEmptyBorder());
        imageControlsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        imageControlsScroll.getVerticalScrollBar().setUnitIncrement(12);
        imageControlsScroll.getViewport().setOpaque(true);
        imageControlsScroll.getViewport().setBackground(defaultPanelBackground);
        imageControlsScroll.setOpaque(true);
        imageControlsScroll.setBackground(defaultPanelBackground);
        tabbedPane.addTab("Image Controls", imageControlsScroll);
        tabbedPane.setBackgroundAt(1, defaultPanelBackground);
        
        // Layout principal - botón CLARAMENTE SEPARADO de las pestañas
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(topPanel, BorderLayout.CENTER);
        topWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        topWrapper.setOpaque(true);
        topWrapper.setBackground(defaultPanelBackground);
        add(topWrapper, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        layerInfoLabel.setOpaque(true);
        layerInfoLabel.setBackground(defaultPanelBackground);
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
        mainPanel.setOpaque(true);
        mainPanel.setBackground(defaultPanelBackground);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Reducir espaciado
        
        // === SECCIÓN SUPERIOR: CONTROLES GLOBALES (TOOLBAR ORIGINAL CON ICONOS) ===
        JPanel globalControlsPanel = new JPanel(new BorderLayout());
        globalControlsPanel.setOpaque(true);
        globalControlsPanel.setBackground(defaultPanelBackground);
        globalControlsPanel.setBorder(new TitledBorder("Global controls (Main image + layers)"));
        
        // Toolbar existente de ImagePreparingView - ¡CON LOS ICONOS ÚTILES!
        if (imagePreparingView != null && imagePreparingView.getToolBar() != null) {
            ImagePreparingToolBar toolBar = imagePreparingView.getToolBar();
            toolBar.setOpaque(true);
            toolBar.setBackground(defaultPanelBackground);
            globalControlsPanel.add(toolBar, BorderLayout.CENTER);
        } else {
            JLabel noToolbarLabel = new JLabel("Toolbar not available", JLabel.CENTER);
            globalControlsPanel.add(noToolbarLabel, BorderLayout.CENTER);
        }
        
        // === SECCIÓN INFERIOR: CONTROLES DE IMAGEN DE FONDO ===
        JPanel backgroundControlsPanel = new JPanel(new BorderLayout());
        backgroundControlsPanel.setOpaque(true);
        backgroundControlsPanel.setBackground(defaultPanelBackground);
        backgroundControlsPanel.setBorder(new TitledBorder("Background image controls (no layers)"));
        
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
        panel.setOpaque(true);
        panel.setBackground(defaultPanelBackground);
        
        // Crear controles para imagen de fondo con spinners
        JPanel slidersGrid = new JPanel(new GridBagLayout());
        slidersGrid.setOpaque(true);
        slidersGrid.setBackground(defaultPanelBackground);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 2, 1, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Crear spinners para controles de imagen de fondo (todos empiezan en valor neutro)
        backgroundBrightnessSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // Neutro en 0
        backgroundContrastSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // Neutro en 0  
        backgroundSaturationSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // Neutro en 0
        backgroundGammaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 3.0, 0.1)); // Neutro en 1.0
        backgroundSharpnessSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 5)); // Neutro en 0
        
        // Configurar formato de los spinners
        JSpinner.NumberEditor bgGammaEditor = new JSpinner.NumberEditor(backgroundGammaSpinner, "0.0");
        backgroundGammaSpinner.setEditor(bgGammaEditor);
        
        // Sliders para imagen de fondo usando addImageTransformControl con spinners
        
        // Fila 0: Brillo de fondo
        backgroundBrightnessSlider = new JSlider(-100, 100, 0);
        addImageTransformControl(slidersGrid, "Brightness:", backgroundBrightnessSlider, backgroundBrightnessSpinner, gbc, 0);
        
        // Fila 1: Contraste de fondo  
        backgroundContrastSlider = new JSlider(-100, 100, 0);
        addImageTransformControl(slidersGrid, "Contrast:", backgroundContrastSlider, backgroundContrastSpinner, gbc, 1);
        
        // Fila 2: Saturación de fondo
        backgroundSaturationSlider = new JSlider(-100, 100, 0);
        addImageTransformControl(slidersGrid, "Saturation:", backgroundSaturationSlider, backgroundSaturationSpinner, gbc, 2);
        
        // Fila 3: Gamma de fondo
        backgroundGammaSlider = new JSlider(10, 300, 100); // 0.1 a 3.0
        addImageTransformControl(slidersGrid, "Gamma:", backgroundGammaSlider, backgroundGammaSpinner, gbc, 3);
        
        // Fila 4: Nitidez de fondo
        backgroundSharpnessSlider = new JSlider(-100, 100, 0);
        addImageTransformControl(slidersGrid, "Sharpness:", backgroundSharpnessSlider, backgroundSharpnessSpinner, gbc, 4);
        
        // Botón reset compacto
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; 
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(3, 2, 1, 2);
        JButton resetBgButton = new JButton("Reset background image");
        resetBgButton.setPreferredSize(new Dimension(150, 22));
        slidersGrid.add(resetBgButton, gbc);
        
        // Configurar listeners funcionales para imagen de fondo
        setupBackgroundSliderListeners(backgroundBrightnessSlider, backgroundBrightnessSpinner, 
                         backgroundContrastSlider, backgroundContrastSpinner,
                         backgroundSaturationSlider, backgroundSaturationSpinner,
                         backgroundGammaSlider, backgroundGammaSpinner,
                         backgroundSharpnessSlider, backgroundSharpnessSpinner,
                                     resetBgButton);
        
        // Sincronizar con valores actuales del modelo
        syncBackgroundControlsWithModel();
        
        panel.add(slidersGrid);
        return panel;
    }
    
    /**
     * Sincroniza los controles con los valores actuales del modelo
     */
    private void syncBackgroundControlsWithModel() {
        if (model == null) {
            return;
        }

        float brightnessValue = getFloatState(BrickGraphicsState.BackgroundBrightness, 1.0f);
        float contrastValue = getFloatState(BrickGraphicsState.BackgroundContrast, 1.0f);
        float saturationValue = getFloatState(BrickGraphicsState.BackgroundSaturation, 1.0f);
        float gammaValue = getFloatState(BrickGraphicsState.BackgroundGamma, 1.0f);
        float sharpnessValue = getFloatState(BrickGraphicsState.BackgroundSharpness, 1.0f);

        backgroundBrightness = brightnessValue;
        backgroundContrast = contrastValue;
        backgroundSaturation = saturationValue;
        backgroundGamma = gammaValue;
        backgroundSharpness = sharpnessValue;

        applyBackgroundValues(brightnessValue, contrastValue, saturationValue, gammaValue, sharpnessValue);
    }
    
    /**
     * Configura los listeners funcionales para modificar solo la imagen de fondo
     */
    private void setupBackgroundSliderListeners(JSlider brightness, JSpinner brightnessSpinner,
                                               JSlider contrast, JSpinner contrastSpinner,
                                               JSlider saturation, JSpinner saturationSpinner,
                                               JSlider gamma, JSpinner gammaSpinner,
                                               JSlider sharpness, JSpinner sharpnessSpinner,
                                               JButton resetButton) {
        
        // Brightness listeners - bidireccional entre slider y spinner (con debounce)
        brightness.addChangeListener(e -> {
            if (updatingControls) return;
            int value = brightness.getValue();
            backgroundBrightness = 1.0f + (value / 100.0f);
            updatingControls = true;
            brightnessSpinner.setValue(value);
            updatingControls = false;
            
            // Usar debounce para mejor rendimiento
            pendingBackgroundUpdate = true;
            backgroundDebounceTimer.restart();
        });
        
        brightnessSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = (Integer) brightnessSpinner.getValue();
            backgroundBrightness = 1.0f + (value / 100.0f);
            updatingControls = true;
            brightness.setValue(value);
            updatingControls = false;
            
            // Usar debounce para mejor rendimiento
            pendingBackgroundUpdate = true;
            backgroundDebounceTimer.restart();
        });
        
        // Contrast listeners - bidireccional entre slider y spinner (con debounce)
        contrast.addChangeListener(e -> {
            if (updatingControls) return;
            int value = contrast.getValue();
            backgroundContrast = 1.0f + (value / 100.0f);
            updatingControls = true;
            contrastSpinner.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        contrastSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = (Integer) contrastSpinner.getValue();
            backgroundContrast = 1.0f + (value / 100.0f);
            updatingControls = true;
            contrast.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        // Saturation listeners - bidireccional entre slider y spinner (con debounce)
        saturation.addChangeListener(e -> {
            if (updatingControls) return;
            int value = saturation.getValue();
            backgroundSaturation = 1.0f + (value / 100.0f);
            updatingControls = true;
            saturationSpinner.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        saturationSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = (Integer) saturationSpinner.getValue();
            backgroundSaturation = 1.0f + (value / 100.0f);
            updatingControls = true;
            saturation.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        // Gamma listeners - bidireccional entre slider y spinner (con debounce)
        gamma.addChangeListener(e -> {
            if (updatingControls) return;
            float value = gamma.getValue() / 100.0f;
            backgroundGamma = value;
            updatingControls = true;
            gammaSpinner.setValue((double)value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        gammaSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            double value = (Double) gammaSpinner.getValue();
            backgroundGamma = (float) value;
            updatingControls = true;
            gamma.setValue((int)(value * 100));
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        // Sharpness listeners - bidireccional entre slider y spinner (con debounce)
        sharpness.addChangeListener(e -> {
            if (updatingControls) return;
            int value = sharpness.getValue();
            backgroundSharpness = 1.0f + (value / 100.0f);
            updatingControls = true;
            sharpnessSpinner.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        sharpnessSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = (Integer) sharpnessSpinner.getValue();
            backgroundSharpness = 1.0f + (value / 100.0f);
            updatingControls = true;
            sharpness.setValue(value);
            updatingControls = false;
            
            scheduleBackgroundUpdate();
        });
        
        // Reset button - resetea SOLO la imagen de fondo
        resetButton.addActionListener(e -> {
            updatingControls = true;
            brightness.setValue(0);
            contrast.setValue(0);
            saturation.setValue(0);
            gamma.setValue(100);
            sharpness.setValue(0);
            brightnessSpinner.setValue(0);
            contrastSpinner.setValue(0);
            saturationSpinner.setValue(0);
            gammaSpinner.setValue(1.0);
            sharpnessSpinner.setValue(0);
            updatingControls = false;
            
            // Reset valores de imagen de fondo
            backgroundBrightness = 1.0f;
            backgroundContrast = 1.0f;
            backgroundSaturation = 1.0f;
            backgroundGamma = 1.0f;
            backgroundSharpness = 1.0f;
            
            scheduleBackgroundUpdate();
        });
    }

    private float getFloatState(BrickGraphicsState state, float defaultValue) {
        if (model == null) {
            return defaultValue;
        }
        Object value = model.get(state);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    private void applyBackgroundValues(float brightness, float contrast, float saturation, float gamma, float sharpness) {
        if (backgroundBrightnessSlider == null || backgroundContrastSlider == null ||
            backgroundSaturationSlider == null || backgroundGammaSlider == null ||
            backgroundSharpnessSlider == null || backgroundBrightnessSpinner == null ||
            backgroundContrastSpinner == null || backgroundSaturationSpinner == null ||
            backgroundGammaSpinner == null || backgroundSharpnessSpinner == null) {
            return;
        }

        updatingControls = true;

        int brightnessValue = clampInt(Math.round((brightness - 1.0f) * 100.0f),
                                       backgroundBrightnessSlider.getMinimum(),
                                       backgroundBrightnessSlider.getMaximum());
        backgroundBrightnessSlider.setValue(brightnessValue);
        backgroundBrightnessSpinner.setValue(Integer.valueOf(brightnessValue));

        int contrastValue = clampInt(Math.round((contrast - 1.0f) * 100.0f),
                                     backgroundContrastSlider.getMinimum(),
                                     backgroundContrastSlider.getMaximum());
        backgroundContrastSlider.setValue(contrastValue);
        backgroundContrastSpinner.setValue(Integer.valueOf(contrastValue));

        int saturationValue = clampInt(Math.round((saturation - 1.0f) * 100.0f),
                                       backgroundSaturationSlider.getMinimum(),
                                       backgroundSaturationSlider.getMaximum());
        backgroundSaturationSlider.setValue(saturationValue);
        backgroundSaturationSpinner.setValue(Integer.valueOf(saturationValue));

        SpinnerNumberModel gammaModel = (SpinnerNumberModel) backgroundGammaSpinner.getModel();
        double gammaMin = ((Number) gammaModel.getMinimum()).doubleValue();
        double gammaMax = ((Number) gammaModel.getMaximum()).doubleValue();
        double clampedGamma = clampDouble(gamma, gammaMin, gammaMax);
        int gammaSliderValue = clampInt((int)Math.round(clampedGamma * 100.0d),
                                        backgroundGammaSlider.getMinimum(),
                                        backgroundGammaSlider.getMaximum());
        backgroundGammaSlider.setValue(gammaSliderValue);
        gammaModel.setValue(Double.valueOf(clampedGamma));

        int sharpnessValue = clampInt(Math.round((sharpness - 1.0f) * 100.0f),
                                      backgroundSharpnessSlider.getMinimum(),
                                      backgroundSharpnessSlider.getMaximum());
        backgroundSharpnessSlider.setValue(sharpnessValue);
        backgroundSharpnessSpinner.setValue(Integer.valueOf(sharpnessValue));

        updatingControls = false;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void persistBackgroundAdjustments() {
        if (layerManager != null) {
            layerManager.updateBackgroundAdjustmentCache(
                backgroundBrightness,
                backgroundContrast,
                backgroundSaturation,
                backgroundGamma,
                backgroundSharpness
            );
        }
    }

    private void scheduleBackgroundUpdate() {
        pendingBackgroundUpdate = true;
        if (backgroundDebounceTimer != null) {
            backgroundDebounceTimer.restart();
        } else {
            persistBackgroundAdjustments();
            notifyLayersChanged();
            pendingBackgroundUpdate = false;
        }
    }

    public void reloadBackgroundAdjustmentsFromModel() {
        SwingUtilities.invokeLater(this::syncBackgroundControlsWithModel);
    }

    public void applyBackgroundAdjustments(float brightness, float contrast, float saturation, float gamma, float sharpness) {
        backgroundBrightness = brightness;
        backgroundContrast = contrast;
        backgroundSaturation = saturation;
        backgroundGamma = gamma;
        backgroundSharpness = sharpness;
        SwingUtilities.invokeLater(() -> applyBackgroundValues(brightness, contrast, saturation, gamma, sharpness));
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

    private Color resolveDefaultPanelBackground() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) {
            bg = new Color(0xF0F0F0);
        }
        return bg;
    }
    
    /**
     * Crea el panel de propiedades de capa seleccionada
     */
    private JPanel createLayerPropertiesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Layer Properties"));
        panel.setOpaque(true);
        panel.setBackground(defaultPanelBackground);
        
        // Panel de propiedades básicas
        JPanel basicProps = new JPanel(new GridBagLayout());
        basicProps.setOpaque(true);
        basicProps.setBackground(defaultPanelBackground);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Fila 0: Visibilidad y modo de fusión
        gbc.gridx = 0; gbc.gridy = 0;
        basicProps.add(visibilityCheckBox, gbc);
        gbc.gridx = 1;
        basicProps.add(new JLabel("Mode:"), gbc);
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
        imageTransformsPanel.setBorder(new TitledBorder("Image adjustments"));
        imageTransformsPanel.setOpaque(true);
        imageTransformsPanel.setBackground(defaultPanelBackground);
        
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.insets = new Insets(2, 5, 2, 5);
        igbc.anchor = GridBagConstraints.WEST;
        
        // Añadir controles de transformación
        addImageTransformControl(imageTransformsPanel, "Opacity:", opacitySlider, opacityLabel, igbc, 0);
        addImageTransformControl(imageTransformsPanel, "Brightness:", brightnessSlider, brightnessSpinner, igbc, 1); // CAMBIADO: brightnessLabel -> brightnessSpinner
        addImageTransformControl(imageTransformsPanel, "Contrast:", contrastSlider, contrastSpinner, igbc, 2); // CAMBIADO: contrastLabel -> contrastSpinner
        addImageTransformControl(imageTransformsPanel, "Saturation:", saturationSlider, saturationSpinner, igbc, 3); // CAMBIADO: saturationLabel -> saturationSpinner
        addImageTransformControl(imageTransformsPanel, "Gamma:", gammaSlider, gammaSpinner, igbc, 4); // CAMBIADO: gammaLabel -> gammaSpinner
        addImageTransformControl(imageTransformsPanel, "Sharpness:", sharpnessSlider, sharpnessSpinner, igbc, 5); // CAMBIADO: sharpnessLabel -> sharpnessSpinner
        addImageTransformControl(imageTransformsPanel, "Scale:", scaleSlider, scaleSpinner, igbc, 6); // CAMBIADO: scaleField -> scaleSpinner
        
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
    private void addImageTransformControl(JPanel panel, String label, JSlider slider, Component valueComponent, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(slider, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        panel.add(valueComponent, gbc); // CAMBIADO: de valueLabel a valueComponent para soportar JTextField
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
                if (layerManager.moveLayerUp(selectedLayer)) {
                    updateUI();
                    // Lista invertida: subir en renderizado = subir en lista visual (menor índice visual)
                    int newVisualIndex = getVisualIndexForLayer(selectedLayer);
                    if (newVisualIndex >= 0) {
                        layerList.setSelectedIndex(newVisualIndex);
                    }
                    notifyLayersChanged();
                    // Forzar repintado de la vista principal
                    if (getParent() != null) {
                        getParent().repaint();
                    }
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
                if (layerManager.moveLayerDown(selectedLayer)) {
                    updateUI();
                    // Lista invertida: bajar en renderizado = bajar en lista visual (mayor índice visual)
                    int newVisualIndex = getVisualIndexForLayer(selectedLayer);
                    if (newVisualIndex >= 0) {
                        layerList.setSelectedIndex(newVisualIndex);
                    }
                    notifyLayersChanged();
                    // Forzar repintado de la vista principal
                    if (getParent() != null) {
                        getParent().repaint();
                    }
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
        
        // Inicializar timers de debounce para optimizar rendimiento
        debounceTimer = new Timer(DEBOUNCE_DELAY_MS, e -> {
            if (pendingLayerUpdate) {
                updateLayerImageTransforms();
                pendingLayerUpdate = false;
            }
        });
        debounceTimer.setRepeats(false);
        
        backgroundDebounceTimer = new Timer(DEBOUNCE_DELAY_MS, e -> {
            if (pendingBackgroundUpdate) {
                persistBackgroundAdjustments();
                notifyLayersChanged();
                pendingBackgroundUpdate = false;
            }
        });
        backgroundDebounceTimer.setRepeats(false);
        
        // Controles de transformaciones de imagen (con debounce para mejor rendimiento)
        ChangeListener imageTransformListener = e -> {
            pendingLayerUpdate = true;
            debounceTimer.restart();
        };
        brightnessSlider.addChangeListener(imageTransformListener);
        contrastSlider.addChangeListener(imageTransformListener);
        saturationSlider.addChangeListener(imageTransformListener);
        gammaSlider.addChangeListener(imageTransformListener);
        sharpnessSlider.addChangeListener(imageTransformListener);
        scaleSlider.addChangeListener(imageTransformListener);
        
        // Listener para entrada directa en spinner de escala
        scaleSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = ((Integer) scaleSpinner.getValue()).intValue();
            value = Math.max(1, Math.min(300, value)); // Limitar entre 1% y 300%
            scaleSlider.setValue(value);
        });
        
        // Listeners para entrada directa en los demás spinners (nuevos rangos -100..100)
        brightnessSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = ((Integer) brightnessSpinner.getValue()).intValue();
            value = Math.max(-100, Math.min(100, value)); // Limitar entre -100% y 100%
            brightnessSlider.setValue(value);
        });
        
        contrastSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = ((Integer) contrastSpinner.getValue()).intValue();
            value = Math.max(-100, Math.min(100, value)); // Limitar entre -100% y 100%
            contrastSlider.setValue(value);
        });
        
        saturationSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = ((Integer) saturationSpinner.getValue()).intValue();
            value = Math.max(-100, Math.min(100, value)); // Limitar entre -100% y 100%
            saturationSlider.setValue(value);
        });
        
        gammaSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            double value = ((Double) gammaSpinner.getValue()).doubleValue();
            value = Math.max(0.5, Math.min(2.0, value)); // Limitar entre 0.5 y 2.0
            // Convertir a valor de slider (50-200)
            int sliderValue = (int)(value * 100);
            gammaSlider.setValue(sliderValue);
        });
        
        sharpnessSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            int value = ((Integer) sharpnessSpinner.getValue()).intValue();
            value = Math.max(-100, Math.min(100, value)); // Limitar entre -100% y 100%
            sharpnessSlider.setValue(value);
        });
        
        // Añadir listener de rueda de ratón para el spinner de escala
        scaleSpinner.addMouseWheelListener(e -> {
            if (!scaleSpinner.isEnabled()) return;
            
            // Obtener el modelo del spinner
            SpinnerNumberModel model = (SpinnerNumberModel) scaleSpinner.getModel();
            int currentValue = (Integer) model.getValue();
            int stepSize = model.getStepSize().intValue();
            
            // Calcular nuevo valor basado en la dirección de la rueda
            int multiplier = 1;
            if (e.isShiftDown()) multiplier = 10; // mover más rápido con Shift
            if (e.isControlDown()) multiplier = 100; // muy rápido con Ctrl

            int rotation = e.getWheelRotation();
            if (rotation > 0) rotation = 1;
            else if (rotation < 0) rotation = -1;

            int delta = rotation * stepSize * -1 * multiplier; // Invertir para que sea más intuitivo
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
            
            model.setValue(newValue);
        });
        
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
     * NUEVO: Configura listener para actualizar spinners cuando cambie el zoom
     */
    private void setupZoomListener() {
        try {
            if (brickedView != null && brickedView.getMosaicZoomController() != null) {
                // Añadir listener para cambios de zoom
                brickedView.getMosaicZoomController().addZoomListener(new mosaic.controllers.MosaicZoomController.MosaicZoomListener() {
                    @Override
                    public void zoomChanged(double zoomFactor, int zoomIndex) {
                        // Actualizar los spinners de posición para reflejar el nuevo zoom
                        SwingUtilities.invokeLater(() -> {
                            if (!updatingControls) {
                                updateLayerControls(); // Recalcular posiciones en pantalla
                            }
                        });
                    }
                });
            }
        } catch (Exception ex) {
            io.Log.log("WARN: Error configurando listener de zoom: " + ex.getMessage());
        }
    }
    
    /**
     * Añade una nueva capa desde un archivo
     */
    private void addLayer() {
    io.Log.log("DEBUG: IntegratedImageLayerPanel - Iniciando addLayer()");
        JFileChooser fileChooser = new JFileChooser();
        
        // MEJORADO: Establecer directorio inicial basado en capas existentes o imagen principal
        File initialDirectory = null;
        
        // 1. Intentar usar el directorio de las capas ya cargadas
        if (layerManager.getLayers().size() > 0) {
            Layer firstLayer = layerManager.getLayers().get(0);
            if (firstLayer.getImageFilePath() != null) {
                File layerFile = new File(firstLayer.getImageFilePath());
                File layerDir = layerFile.getParentFile();
                if (layerDir != null && layerDir.exists()) {
                    initialDirectory = layerDir;
                    io.Log.log("DEBUG: IntegratedImageLayerPanel - Directorio inicial establecido desde capas: " + layerDir.getAbsolutePath());
                }
            }
        }
        
        // 2. Si no hay capas, usar el directorio de la imagen principal
        if (initialDirectory == null) {
            File mainImageDir = mainController.getMainImageDirectory();
            if (mainImageDir != null && mainImageDir.exists()) {
                initialDirectory = mainImageDir;
                io.Log.log("DEBUG: IntegratedImageLayerPanel - Directorio inicial establecido desde imagen principal: " + mainImageDir.getAbsolutePath());
            }
        }
        
        // Aplicar el directorio inicial encontrado
        if (initialDirectory != null) {
            fileChooser.setCurrentDirectory(initialDirectory);
        }
        
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Images (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            io.Log.log("DEBUG: IntegratedImageLayerPanel - Archivo seleccionado: " + selectedFile.getAbsolutePath());
            try {
                Layer layer = layerManager.addLayerFromFile(
                    selectedFile.getAbsolutePath(), 
                    null
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
                    "Error loading image: " + ex.getMessage(), 
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
     * Actualiza la posición de la capa seleccionada usando coordenadas directas del mosaico
     */
    private void updateLayerPosition() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            io.Log.log("DEBUG: updateLayerPosition - INICIADO para capa: " + selected.getName());
            
            // PRESERVAR MODIFICACIONES DEL USUARIO ANTES DEL MOVIMIENTO
            java.util.Map<String, colors.LEGOColor> baseModifications = null;
            if (layerManager.getModificationManager() != null) {
                baseModifications = layerManager.getModificationManager().getAllModifications();
                io.Log.log("DEBUG: updateLayerPosition - preserving " + baseModifications.size() + " base grid modifications");
            }
            
            // Obtener valores de spinner en unidades de mosaico (studs)
            int mosaicUnitsX = (Integer) positionXSpinner.getValue();
            int mosaicUnitsY = (Integer) positionYSpinner.getValue();

            Point pixelPosition = convertMosaicUnitsToPixelPosition(mosaicUnitsX, mosaicUnitsY);
            selected.setPosition(pixelPosition);
            
            // RESTAURAR MODIFICACIONES DESPUÉS DEL CAMBIO
            if (baseModifications != null && layerManager.getModificationManager() != null) {
                boolean visible = layerManager.getModificationManager().areModificationsVisible();
                colors.LEGOColorGrid colorGrid = layerManager.getColorGrid();
                if (visible && colorGrid != null) {
                    // Camino directo: aplicar modificaciones directamente al ColorGrid
                    layerManager.getModificationManager().restoreModificationsFromCopyAndApply(baseModifications, colorGrid);
                    io.Log.log("DEBUG: updateLayerPosition - restored and applied " + baseModifications.size() + " base grid modifications (directo)");
					
                    // Forzar actualización visual inmediata
                    if (brickedView != null) {
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            brickedView.repaint();
                            io.Log.log("DEBUG: updateLayerPosition - Forzado repaint() del BrickedView (camino directo)");
                        });
                    }
                } else {
                    layerManager.getModificationManager().restoreModificationsFromCopy(baseModifications);
                    io.Log.log("DEBUG: updateLayerPosition - Pintado oculto o ColorGrid null, solo se restauran " + baseModifications.size() + " modificaciones sin aplicarlas");
                }
            }
            
            notifyLayersChanged();
        }
    }
    
    /**
     * Convierte coordenadas de pantalla a coordenadas de mosaico considerando el zoom
     */
    private Point screenToMosaicCoordinates(int screenX, int screenY) {
        try {
            if (brickedView != null && brickedView.getMosaicZoomController() != null) {
                double zoomFactor = brickedView.getMosaicZoomController().getCurrentZoomFactor();
                // Para convertir de pantalla a mosaico: dividir por zoom
                int mosaicX = (int) Math.round(screenX / zoomFactor);
                int mosaicY = (int) Math.round(screenY / zoomFactor);
                return new Point(mosaicX, mosaicY);
            }
        } catch (Exception ex) {
            io.Log.log("WARN: Error convirtiendo coordenadas de pantalla: " + ex.getMessage());
        }
        
        // Fallback: asumir zoom 1.0 (sin conversión)
        return new Point(screenX, screenY);
    }
    
    /**
     * Convierte coordenadas de mosaico a coordenadas de pantalla considerando el zoom
     */
    private Point mosaicToScreenCoordinates(int mosaicX, int mosaicY) {
        try {
            if (brickedView != null && brickedView.getMosaicZoomController() != null) {
                double zoomFactor = brickedView.getMosaicZoomController().getCurrentZoomFactor();
                // Para convertir de mosaico a pantalla: multiplicar por zoom
                int screenX = (int) Math.round(mosaicX * zoomFactor);
                int screenY = (int) Math.round(mosaicY * zoomFactor);
                return new Point(screenX, screenY);
            }
        } catch (Exception ex) {
            io.Log.log("WARN: Error convirtiendo coordenadas de mosaico: " + ex.getMessage());
        }
        
        // Fallback: asumir zoom 1.0 (sin conversión)
        return new Point(mosaicX, mosaicY);
    }
    
    /**
     * Actualiza las transformaciones de imagen de la capa seleccionada
     */
    private void updateLayerImageTransforms() {
        if (updatingControls) return;
        
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            // Convertir valores de slider (-100 a 100) a factores apropiados
            float brightness = (brightnessSlider.getValue() + 100) / 100.0f; // -100..100 -> 0.0..2.0
            float contrast = (contrastSlider.getValue() + 100) / 100.0f; // -100..100 -> 0.0..2.0
            float saturation = (saturationSlider.getValue() + 100) / 100.0f; // -100..100 -> 0.0..2.0
            float gamma = gammaSlider.getValue() / 100.0f; // 50..200 -> 0.5..2.0 (sin cambio)
            float sharpness = (sharpnessSlider.getValue() + 100) / 100.0f; // -100..100 -> 0.0..2.0
            float scale = scaleSlider.getValue() / 100.0f;
            
            // Aplicar transformaciones de forma batch para mejor rendimiento
            selected.setBrightness(brightness);
            selected.setContrast(contrast);
            selected.setSaturation(saturation);
            selected.setGamma(gamma);
            selected.setSharpness(sharpness);
            selected.setScale(scale);
            
            // Actualizar etiquetas de valores (optimizado)
            SwingUtilities.invokeLater(() -> {
                if (!updatingControls) {
                    brightnessLabel.setText(String.format("%d%%", brightnessSlider.getValue()));
                    contrastLabel.setText(String.format("%d%%", contrastSlider.getValue()));
                    saturationLabel.setText(String.format("%d%%", saturationSlider.getValue()));
                    gammaLabel.setText(String.format("%.2f", gamma));
                    sharpnessLabel.setText(String.format("%d%%", sharpnessSlider.getValue()));
                    
                    // Actualizar spinners para mantener sincronización (sin causar eventos)
                    updatingControls = true;
                    try {
                        brightnessSpinner.setValue(brightnessSlider.getValue());
                        contrastSpinner.setValue(contrastSlider.getValue());
                        saturationSpinner.setValue(saturationSlider.getValue());
                        gammaSpinner.setValue((double) gamma); // Convertir float a double
                        sharpnessSpinner.setValue(sharpnessSlider.getValue());
                        scaleSpinner.setValue(scaleSlider.getValue());
                    } finally {
                        updatingControls = false;
                    }
                }
            });
            
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
                // Restablecer valores por defecto (valores neutros - 0 para la mayoría)
                opacitySlider.setValue(100);
                // NO resetear: positionXSpinner.setValue(0);
                // NO resetear: positionYSpinner.setValue(0);
                brightnessSlider.setValue(0); // Valor neutro: 0
                contrastSlider.setValue(0); // Valor neutro: 0
                saturationSlider.setValue(0); // Valor neutro: 0
                gammaSlider.setValue(100); // Valor neutro: 100 (representa 1.0)
                sharpnessSlider.setValue(0); // Valor neutro: 0
                // NO resetear: scaleSlider.setValue(100);
                
                // Resetear también los spinners
                brightnessSpinner.setValue(0); // Valor neutro: 0
                contrastSpinner.setValue(0); // Valor neutro: 0
                saturationSpinner.setValue(0); // Valor neutro: 0
                gammaSpinner.setValue(1.0); // Valor neutro: 1.0
                sharpnessSpinner.setValue(0); // Valor neutro: 0
                
                // Restablecer etiquetas a valores por defecto (valores neutros)
                opacityLabel.setText("100%");
                brightnessLabel.setText("0%");
                contrastLabel.setText("0%");
                saturationLabel.setText("0%");
                gammaLabel.setText("1.00");
                sharpnessLabel.setText("0%");
                scaleSpinner.setValue((int) Math.round(currentScale * 100)); // CAMBIADO: scaleField -> scaleSpinner, Mantener escala actual
                
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
            // Habilitar/deshabilitar spinners también
            brightnessSpinner.setEnabled(hasSelection);
            contrastSpinner.setEnabled(hasSelection);
            saturationSpinner.setEnabled(hasSelection);
            gammaSpinner.setEnabled(hasSelection);
            sharpnessSpinner.setEnabled(hasSelection);
            resetAdjustmentsButton.setEnabled(hasSelection);
            removeLayerButton.setEnabled(hasSelection);
            duplicateButton.setEnabled(hasSelection);
            // Habilitar/deshabilitar flechas según posición
            if (hasSelection) {
                int idx = layerManager.getLayers().indexOf(selected);
                int lastIdx = layerManager.getLayers().size() - 1;
                moveUpButton.setEnabled(idx < lastIdx);
                moveDownButton.setEnabled(idx > 0);
                // Actualizar valores de controles
                opacitySlider.setValue((int) (selected.getOpacity() * 100));
                visibilityCheckBox.setSelected(selected.isVisible());
                blendModeCombo.setSelectedItem(selected.getBlendMode());
                
                // Coord. en unidades del mosaico (studs) para la UI
                Point mosaicUnits = convertPixelPositionToMosaicUnits(selected.getPosition());
                positionXSpinner.setValue(mosaicUnits.x);
                positionYSpinner.setValue(mosaicUnits.y);
                
                // Actualizar controles de transformación de imagen (convertir 0.0-2.0 a rango -100..100)
                brightnessSlider.setValue((int) (selected.getBrightness() * 100 - 100)); // 0.0-2.0 -> -100..100
                contrastSlider.setValue((int) (selected.getContrast() * 100 - 100)); // 0.0-2.0 -> -100..100
                saturationSlider.setValue((int) (selected.getSaturation() * 100 - 100)); // 0.0-2.0 -> -100..100
                gammaSlider.setValue((int) (selected.getGamma() * 100)); // 0.5-2.0 -> 50..200 (sin cambio)
                sharpnessSlider.setValue((int) (selected.getSharpness() * 100 - 100)); // 0.0-2.0 -> -100..100
                scaleSlider.setValue((int) (selected.getScale() * 100));
                
                // Actualizar spinners con valores actuales (mismo rango -100..100)
                brightnessSpinner.setValue((int) (selected.getBrightness() * 100 - 100));
                contrastSpinner.setValue((int) (selected.getContrast() * 100 - 100));
                saturationSpinner.setValue((int) (selected.getSaturation() * 100 - 100));
                gammaSpinner.setValue((double) selected.getGamma()); // Convertir float a double
                sharpnessSpinner.setValue((int) (selected.getSharpness() * 100 - 100));
                
                // Actualizar etiquetas con valores de los sliders (mostrar -100..100)
                opacityLabel.setText(String.format("%.0f%%", selected.getOpacity() * 100));
                brightnessLabel.setText(String.format("%d%%", brightnessSlider.getValue()));
                contrastLabel.setText(String.format("%d%%", contrastSlider.getValue()));
                saturationLabel.setText(String.format("%d%%", saturationSlider.getValue()));
                gammaLabel.setText(String.format("%.2f", selected.getGamma()));
                sharpnessLabel.setText(String.format("%d%%", sharpnessSlider.getValue()));
                scaleSpinner.setValue((int) Math.round(selected.getScale() * 100)); // CAMBIADO: scaleField -> scaleSpinner
            }
        } finally {
            updatingControls = false;
        }
    }
    
    /**
     * Obtiene el índice visual de una capa en la lista invertida
     */
    private int getVisualIndexForLayer(Layer layer) {
        List<Layer> layers = layerManager.getLayers();
        int realIndex = layers.indexOf(layer);
        if (realIndex == -1) return -1;
        // En lista invertida: índice visual = (size - 1) - índice real
        return layers.size() - 1 - realIndex;
    }
    
    /**
     * Actualiza la lista de capas
     */
    private void updateLayerList() {
        // OPTIMIZACIÓN: Solo log si hay error, eliminar logs de operación normal
        if (listModel == null) {
            io.Log.log("ERROR: updateLayerList() - listModel es null");
            return;
        }
        
        listModel.clear();
        
        List<Layer> layers = layerManager.getLayers();
        
        // Mostrar capas en orden invertido: índice más alto arriba
        for (int i = layers.size() - 1; i >= 0; i--) {
            listModel.addElement(layers.get(i));
        }
        
        // Mantener selección
        Layer selected = layerManager.getSelectedLayer();
        if (selected != null) {
            layerList.setSelectedValue(selected, true);
        }
        
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
        layerInfoLabel.setText("Layers: " + layerCount);
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
            // io.Log.log("DEBUG: IntegratedImageLayerPanel - Ejecutando callback"); // COMENTADO PARA RENDIMIENTO
            onLayersChangedCallback.run();
            // io.Log.log("DEBUG: IntegratedImageLayerPanel - Callback ejecutado"); // COMENTADO PARA RENDIMIENTO
        } else {
            io.Log.log("DEBUG: IntegratedImageLayerPanel - WARNING: Callback es null!");
        }
    }
    
    /**
     * Obtiene los valores actuales de transformación de imagen de fondo
     */
    public float getBackgroundBrightness() {
        // io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundBrightness() devuelve: " + backgroundBrightness); // COMENTADO PARA RENDIMIENTO
        return backgroundBrightness;
    }
    
    public float getBackgroundContrast() {
        // io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundContrast() devuelve: " + backgroundContrast); // COMENTADO PARA RENDIMIENTO
        return backgroundContrast;
    }
    
    public float getBackgroundSaturation() {
        // io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundSaturation() devuelve: " + backgroundSaturation); // COMENTADO PARA RENDIMIENTO
        return backgroundSaturation;
    }
    
    public float getBackgroundGamma() {
        // io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundGamma() devuelve: " + backgroundGamma); // COMENTADO PARA RENDIMIENTO
        return backgroundGamma;
    }
    
    public float getBackgroundSharpness() {
        // io.Log.log("DEBUG: IntegratedImageLayerPanel - getBackgroundSharpness() devuelve: " + backgroundSharpness); // COMENTADO PARA RENDIMIENTO
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
            int multiplier = 1;
            if (e.isShiftDown()) multiplier = 10; // mover más rápido con Shift
            if (e.isControlDown()) multiplier = 100; // muy rápido con Ctrl

            int rotation = e.getWheelRotation();
            if (rotation > 0) rotation = 1;
            else if (rotation < 0) rotation = -1;

            int delta = rotation * stepSize * -1 * multiplier; // Invertir para que sea más intuitivo
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

    private Point convertMosaicUnitsToPixelPosition(int mosaicUnitsX, int mosaicUnitsY) {
        double pxPerUnitX = getPixelsPerMosaicUnitX();
        double pxPerUnitY = getPixelsPerMosaicUnitY();
        int pixelX = (int) Math.round(mosaicUnitsX * pxPerUnitX);
        int pixelY = (int) Math.round(mosaicUnitsY * pxPerUnitY);
        return new Point(pixelX, pixelY);
    }

    private Point convertPixelPositionToMosaicUnits(Point pixelPoint) {
        if (pixelPoint == null) {
            return new Point(0, 0);
        }
        double pxPerUnitX = getPixelsPerMosaicUnitX();
        double pxPerUnitY = getPixelsPerMosaicUnitY();
        int mosaicX = pxPerUnitX != 0 ? (int) Math.round(pixelPoint.x / pxPerUnitX) : pixelPoint.x;
        int mosaicY = pxPerUnitY != 0 ? (int) Math.round(pixelPoint.y / pxPerUnitY) : pixelPoint.y;
        return new Point(mosaicX, mosaicY);
    }

    private static final double MIN_VALID_PIXELS_PER_UNIT = 1.0;
    private double lastLoggedPixelsPerUnitX = Double.NaN;
    private double lastLoggedPixelsPerUnitY = Double.NaN;
    private String lastPixelsPerUnitSourceX = "";
    private String lastPixelsPerUnitSourceY = "";

    private double getPixelsPerMosaicUnitX() {
        return getPixelsPerMosaicUnit(true);
    }

    private double getPixelsPerMosaicUnitY() {
        return getPixelsPerMosaicUnit(false);
    }

    private double getPixelsPerMosaicUnit(boolean horizontal) {
        LEGOColorGrid grid = layerManager != null ? layerManager.getColorGrid() : null;
        if (grid == null && brickedView != null) {
            grid = brickedView.getColorGrid();
        }

        int gridSize = 0;
        if (grid != null) {
            gridSize = horizontal ? grid.getWidth() : grid.getHeight();
        }

        if (gridSize <= 0) {
            maybeLogPixelsPerUnit(1.0, horizontal, -1, 0, "Default");
            return 1.0;
        }

        double ratio = Double.NaN;
        String source = "";
        int axisSize = -1;

        if (layerManager != null) {
            int baseSize = horizontal ? layerManager.getBaseCanvasWidth() : layerManager.getBaseCanvasHeight();
            if (baseSize > 0) {
                double candidate = (double) baseSize / gridSize;
                if (isPixelsPerUnitValid(candidate)) {
                    ratio = candidate;
                    source = "BaseCanvas";
                    axisSize = baseSize;
                }
            }
        }

        if (!isPixelsPerUnitValid(ratio) && brickedView != null) {
            Dimension mosaicSize = brickedView.getBrickedSize();
            if (mosaicSize != null) {
                int mosaicAxisSize = horizontal ? mosaicSize.width : mosaicSize.height;
                if (mosaicAxisSize > 0) {
                    double candidate = (double) mosaicAxisSize / gridSize;
                    if (isPixelsPerUnitValid(candidate)) {
                        ratio = candidate;
                        source = "BrickedView";
                        axisSize = mosaicAxisSize;
                    }
                }
            }
        }

        if (!isPixelsPerUnitValid(ratio)) {
            int fallbackBase = layerManager != null
                    ? (horizontal ? layerManager.getBaseCanvasWidth() : layerManager.getBaseCanvasHeight())
                    : -1;
            double fallback = fallbackBase > 0 ? (double) fallbackBase / gridSize : MIN_VALID_PIXELS_PER_UNIT;
            ratio = Math.max(MIN_VALID_PIXELS_PER_UNIT, fallback);
            source = source.isEmpty() ? "FallbackClamp" : source + "(Clamp)";
            axisSize = fallbackBase;
        }

        maybeLogPixelsPerUnit(ratio, horizontal, axisSize, gridSize, source);
        return ratio;
    }

    private boolean isPixelsPerUnitValid(double ratio) {
        return !Double.isNaN(ratio) && ratio >= MIN_VALID_PIXELS_PER_UNIT;
    }

    /**
     * Returns a human-friendly label for each blend mode so the combo box stays readable.
     */
    private String getBlendModeDisplayName(Layer.BlendMode mode) {
        if (mode == null) {
            return "Normal";
        }
        return switch (mode) {
            case NORMAL -> "Normal";
            case MULTIPLY -> "Multiply";
            case OVERLAY -> "Overlay";
            case SCREEN -> "Screen";
            case SOFT_LIGHT -> "Soft Light";
        };
    }

    private void maybeLogPixelsPerUnit(double ratio, boolean horizontal, int axisSize, int gridSize, String source) {
        double lastRatio = horizontal ? lastLoggedPixelsPerUnitX : lastLoggedPixelsPerUnitY;
        String lastSource = horizontal ? lastPixelsPerUnitSourceX : lastPixelsPerUnitSourceY;
        if (Double.isNaN(lastRatio) || Math.abs(lastRatio - ratio) > 1e-3 || !source.equals(lastSource)) {
            String axis = horizontal ? "X" : "Y";
            io.Log.log(String.format(
                    "DEBUG: PixelsPerMosaicUnit%s = %.4f (grid=%d, axisSize=%d, source=%s)",
                    axis, ratio, gridSize, axisSize, source));
            if (horizontal) {
                lastLoggedPixelsPerUnitX = ratio;
                lastPixelsPerUnitSourceX = source;
            } else {
                lastLoggedPixelsPerUnitY = ratio;
                lastPixelsPerUnitSourceY = source;
            }
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
                String text = "📄 " + layer.getName(); // Emoji para hacer más visible
                
                if (!layer.isVisible()) {
                    text += " (hidden)";
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
                setOpaque(true);
            }
            
            return this;
        }
    }
    
    /**
     * TransferHandler personalizado para permitir drag and drop de capas
     * Permite reordenar las capas arrastrándolas en la lista
     */
    private class LayerTransferHandler extends TransferHandler {
        private static final String LAYER_INDEX_FLAVOR = "application/x-layer-index";
        private DataFlavor layerIndexFlavor;
        
        public LayerTransferHandler() {
            try {
                layerIndexFlavor = new DataFlavor(LAYER_INDEX_FLAVOR);
            } catch (ClassNotFoundException e) {
                io.Log.log("ERROR: No se pudo crear DataFlavor para capas: " + e.getMessage());
                layerIndexFlavor = DataFlavor.stringFlavor; // Fallback
            }
        }
        
        @Override
        public boolean canImport(TransferSupport support) {
            // Verificar que es drop y que soporta nuestro flavor o string
            return support.isDrop() && 
                   (support.isDataFlavorSupported(layerIndexFlavor) || 
                    support.isDataFlavorSupported(DataFlavor.stringFlavor));
        }
        
        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            
            try {
                // Obtener el índice de origen
                int sourceIndex = -1;
                
                if (support.isDataFlavorSupported(layerIndexFlavor)) {
                    String indexStr = (String) support.getTransferable().getTransferData(layerIndexFlavor);
                    sourceIndex = Integer.parseInt(indexStr);
                } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String indexStr = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    try {
                        sourceIndex = Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        // Si no es un número, buscar por nombre de capa
                        String layerName = indexStr;
                        for (int i = 0; i < layerManager.getLayers().size(); i++) {
                            if (layerManager.getLayers().get(i).getName().equals(layerName)) {
                                sourceIndex = i;
                                break;
                            }
                        }
                    }
                }
                
                if (sourceIndex < 0 || sourceIndex >= layerManager.getLayers().size()) {
                    return false;
                }
                
                // Obtener el índice de destino
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int targetIndex = dropLocation.getIndex();
                
                // Ajustar targetIndex si estamos insertando
                if (dropLocation.isInsert()) {
                    // Para inserts, el índice ya está correcto
                } else {
                    // Para reemplazos, usar el índice tal como está
                }
                
                io.Log.log("DEBUG: DnD - sourceIndex=" + sourceIndex + ", targetIndex=" + targetIndex);
                
                // Verificar que los índices son válidos y diferentes
                if (targetIndex >= 0 && sourceIndex != targetIndex) {
                    
                    // Obtener la capa a mover
                    Layer sourceLayer = layerManager.getLayers().get(sourceIndex);
                    
                    // Calcular cuántos pasos mover
                    boolean success = false;
                    
                    if (sourceIndex < targetIndex) {
                        // Mover hacia abajo - ajustar targetIndex si es un insert
                        int steps = targetIndex - sourceIndex;
                        if (dropLocation.isInsert() && targetIndex > sourceIndex) {
                            steps--; // Ajuste para insert
                        }
                        
                        for (int i = 0; i < steps; i++) {
                            if (!layerManager.moveLayerDown(sourceLayer)) {
                                break;
                            }
                        }
                        success = true;
                    } else {
                        // Mover hacia arriba
                        int steps = sourceIndex - targetIndex;
                        
                        for (int i = 0; i < steps; i++) {
                            if (!layerManager.moveLayerUp(sourceLayer)) {
                                break;
                            }
                        }
                        success = true;
                    }
                    
                    if (success) {
                        io.Log.log("DEBUG: DnD - Movimiento exitoso");
                        
                        // Actualizar la UI
                        updateUI();
                        
                        // Seleccionar la capa en su nueva posición
                        int newIndex = layerManager.getLayers().indexOf(sourceLayer);
                        if (newIndex >= 0) {
                            layerList.setSelectedIndex(newIndex);
                        }
                        
                        // Notificar cambios
                        notifyLayersChanged();
                        
                        return true;
                    }
                }
                
            } catch (Exception e) {
                io.Log.log("ERROR: Error durante drag and drop: " + e.getMessage());
                e.printStackTrace();
            }
            
            return false;
        }
        
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            @SuppressWarnings("unchecked")
            JList<Layer> list = (JList<Layer>) c;
            Layer selectedLayer = list.getSelectedValue();
            
            if (selectedLayer != null) {
                int sourceIndex = layerManager.getLayers().indexOf(selectedLayer);
                io.Log.log("DEBUG: DnD - Creando transferable para índice: " + sourceIndex);
                return new StringSelection(String.valueOf(sourceIndex));
            }
            
            return null;
        }
        
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            // No necesitamos hacer nada aquí ya que el movimiento se hace en importData
            io.Log.log("DEBUG: DnD - Export done con action: " + action);
        }
    }
}