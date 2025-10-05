package mosaic.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import mosaic.controllers.MosaicZoomController;

/**
 * Panel de controles de zoom para el mosaico.
 * Incluye botones de zoom in/out, zoom to fit, porcentaje actual, etc.
 */
public class MosaicZoomPanel extends JPanel {
    
    private MosaicZoomController zoomController;
    private BrickedView brickedView;
    
    // Componentes UI
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton zoomToFitButton;
    private JButton zoomToActualButton;
    private JToggleButton zoomModeButton;
    private JLabel zoomLabel;
    private JComboBox<String> zoomComboBox;
    private JLabel panningIndicator;
    
    public MosaicZoomPanel(MosaicZoomController zoomController, BrickedView brickedView) {
        this.zoomController = zoomController;
        this.brickedView = brickedView;
        
        initializeComponents();
        layoutComponents();
        setupListeners();
        updateZoomDisplay();
    }
    
    private void initializeComponents() {
        // Botones de zoom
        zoomInButton = new JButton("+");
        zoomInButton.setToolTipText("Zoom In (Ctrl + Mouse wheel up)");
        zoomInButton.setPreferredSize(new Dimension(30, 25));
        
        zoomOutButton = new JButton("-");
        zoomOutButton.setToolTipText("Zoom Out (Ctrl + Mouse wheel down)");
        zoomOutButton.setPreferredSize(new Dimension(30, 25));
        
        zoomToFitButton = new JButton("Fit");
        zoomToFitButton.setToolTipText("Fit complete mosaic in window");
        
        zoomToActualButton = new JButton("100%");
        zoomToActualButton.setToolTipText("Zoom to actual size");
        
        zoomModeButton = new JToggleButton("Selection");
        zoomModeButton.setToolTipText("Enable selection mode for zoom (Ctrl + drag)");
        
        // Etiqueta y combo de zoom
        zoomLabel = new JLabel("Zoom:");
        
        // Crear opciones del combo box basadas en los niveles de zoom
        String[] zoomOptions = new String[MosaicZoomController.ZOOM_LEVELS.length];
        for (int i = 0; i < MosaicZoomController.ZOOM_LEVELS.length; i++) {
            zoomOptions[i] = Math.round(MosaicZoomController.ZOOM_LEVELS[i] * 100) + "%";
        }
        zoomComboBox = new JComboBox<>(zoomOptions);
        zoomComboBox.setSelectedIndex(MosaicZoomController.DEFAULT_ZOOM_INDEX);
        zoomComboBox.setPreferredSize(new Dimension(80, 25));
        zoomComboBox.setToolTipText("Select specific zoom level");
        
        // Indicador de paneo
        panningIndicator = new JLabel();
        panningIndicator.setToolTipText("Panning available: Middle click + drag or keyboard arrows");
        updatePanningIndicator();
    }
    
    private void layoutComponents() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
        setBorder(BorderFactory.createTitledBorder("Mosaic Zoom"));
        
        add(zoomLabel);
        add(zoomComboBox);
        add(Box.createHorizontalStrut(5));
        add(zoomOutButton);
        add(zoomInButton);
        add(Box.createHorizontalStrut(5));
        add(zoomToFitButton);
        add(zoomToActualButton);
        add(Box.createHorizontalStrut(5));
        add(zoomModeButton);
        add(Box.createHorizontalStrut(5));
        add(panningIndicator);
    }
    
    private void setupListeners() {
        // Listener para zoom in
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomController.zoomIn();
                updateZoomDisplay();
            }
        });
        
        // Listener para zoom out
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomController.zoomOut();
                updateZoomDisplay();
            }
        });
        
        // Listener para zoom to fit
        zoomToFitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomController.zoomToFit();
                updateZoomDisplay();
            }
        });
        
        // Listener para zoom to actual size
        zoomToActualButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomController.zoomToActualSize();
                updateZoomDisplay();
            }
        });
        
        // Listener para modo de selección
        zoomModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enabled = ((JToggleButton) e.getSource()).isSelected();
                brickedView.setZoomMode(enabled);
            }
        });
        
        // Listener para combo box de zoom
        zoomComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = zoomComboBox.getSelectedIndex();
                if (selectedIndex >= 0) {
                    zoomController.setZoomIndex(selectedIndex);
                    updateZoomDisplay();
                }
            }
        });
        
        // Listener para cambios en el zoom controller
        zoomController.addZoomListener(new MosaicZoomController.MosaicZoomListener() {
            @Override
            public void zoomChanged(double zoomFactor, int zoomIndex) {
                SwingUtilities.invokeLater(() -> updateZoomDisplay());
            }
        });
    }
    
    /**
     * Actualiza la visualización del zoom actual.
     */
    private void updateZoomDisplay() {
        // Actualizar combo box sin disparar eventos
        zoomComboBox.removeActionListener(zoomComboBox.getActionListeners()[0]);
        zoomComboBox.setSelectedIndex(zoomController.getCurrentZoomIndex());
        zoomComboBox.addActionListener(zoomComboBox.getActionListeners().length > 0 ? 
                                      zoomComboBox.getActionListeners()[0] : 
                                      e -> {});
        
        // Actualizar estado de botones
        zoomInButton.setEnabled(zoomController.canZoomIn());
        zoomOutButton.setEnabled(zoomController.canZoomOut());
        
        // Actualizar tooltip del combo con porcentaje exacto
        zoomComboBox.setToolTipText("Current zoom: " + zoomController.getCurrentZoomPercentage());
        
        // Actualizar indicador de paneo
        updatePanningIndicator();
    }
    
    /**
     * Actualiza el indicador de paneo basado en si está disponible o no.
     */
    private void updatePanningIndicator() {
        if (zoomController.isPanningAvailable()) {
            panningIndicator.setText("🔍");
            panningIndicator.setToolTipText("Panning available: Middle click + drag or keyboard arrows");
            panningIndicator.setForeground(Color.GREEN);
        } else {
            panningIndicator.setText("○");
            panningIndicator.setToolTipText("Panning not available (zoom at 100% or less)");
            panningIndicator.setForeground(Color.GRAY);
        }
    }
    
    /**
     * Establece si el panel está habilitado.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        zoomInButton.setEnabled(enabled && zoomController.canZoomIn());
        zoomOutButton.setEnabled(enabled && zoomController.canZoomOut());
        zoomToFitButton.setEnabled(enabled);
        zoomToActualButton.setEnabled(enabled);
        zoomModeButton.setEnabled(enabled);
        zoomComboBox.setEnabled(enabled);
    }
    
    /**
     * Obtiene el estado actual del modo de selección.
     */
    public boolean isZoomModeActive() {
        return zoomModeButton.isSelected();
    }
    
    /**
     * Establece el estado del modo de selección.
     */
    public void setZoomModeActive(boolean active) {
        zoomModeButton.setSelected(active);
    }
}