// Sin package - versión simplificada

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Versión simplificada de BrickGraphics con sistema de overlays integrado
 * Enfoque: funcional y directo, sin complicaciones
 */
public class BrickGraphicsConOverlays extends JFrame {
    
    private JPanel mosaicPanel;
    private JPanel overlayPanel;
    private java.util.List<BufferedImage> overlays;
    private java.util.List<String> overlayNames;
    private java.util.List<Float> overlayOpacities;
    private java.util.List<Point> overlayPositions;
    private JList<String> overlayList;
    private DefaultListModel<String> listModel;
    private JSlider opacitySlider;
    private JSpinner xSpinner, ySpinner;
    private int selectedOverlay = -1;
    private double globalScale = 1.0; // Escala global para todos los overlays
    private BufferedImage mosaicFinal = null; // Imagen final generada
    
    public BrickGraphicsConOverlays() {
        super("BrickGraphics con Overlays Múltiples - Versión Simple");
        
        overlays = new java.util.ArrayList<>();
        overlayNames = new java.util.ArrayList<>();
        overlayOpacities = new java.util.ArrayList<>();
        overlayPositions = new java.util.ArrayList<>();
        listModel = new DefaultListModel<>();
        
        setupUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Panel superior con botones principales
        JPanel toolbar = new JPanel(new FlowLayout());
        toolbar.setBackground(new Color(240, 240, 240));
        
        JButton loadImageBtn = new JButton("📁 Cargar Imagen Base");
        loadImageBtn.addActionListener(e -> cargarImagenBase());
        toolbar.add(loadImageBtn);
        
        JButton addOverlayBtn = new JButton("➕ Añadir Overlay");
        addOverlayBtn.addActionListener(e -> añadirOverlay());
        toolbar.add(addOverlayBtn);
        
        JButton removeOverlayBtn = new JButton("🗑️ Eliminar Overlay");
        removeOverlayBtn.addActionListener(e -> eliminarOverlay());
        toolbar.add(removeOverlayBtn);
        
        JButton clearAllBtn = new JButton("🔄 Limpiar Todo");
        clearAllBtn.addActionListener(e -> limpiarTodo());
        toolbar.add(clearAllBtn);
        
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        
        JButton scaleBtn = new JButton("🔍 Escala");
        scaleBtn.addActionListener(e -> cambiarEscala());
        toolbar.add(scaleBtn);
        
        JButton generateBtn = new JButton("🎨 Generar Mosaico Final");
        generateBtn.addActionListener(e -> generarMosaicoFinal());
        toolbar.add(generateBtn);
        
        JButton saveBtn = new JButton("💾 Guardar Imagen");
        saveBtn.addActionListener(e -> guardarImagen());
        toolbar.add(saveBtn);
        
        add(toolbar, BorderLayout.NORTH);
        
        // Panel central - área del mosaico
        mosaicPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                dibujarMosaicoConOverlays(g);
            }
        };
        mosaicPanel.setBackground(Color.WHITE);
        mosaicPanel.setBorder(BorderFactory.createTitledBorder("Vista del Mosaico con Overlays"));
        add(mosaicPanel, BorderLayout.CENTER);
        
        // Panel derecho - controles de overlays
        setupOverlayControls();
        
        // Añadir algunos overlays de ejemplo
        crearOverlaysDeEjemplo();
    }
    
    private void setupOverlayControls() {
        overlayPanel = new JPanel(new BorderLayout());
        overlayPanel.setBorder(BorderFactory.createTitledBorder("Control de Overlays"));
        overlayPanel.setPreferredSize(new Dimension(300, 600));
        
        // Lista de overlays
        overlayList = new JList<>(listModel);
        overlayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overlayList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedOverlay = overlayList.getSelectedIndex();
                actualizarControles();
            }
        });
        
        JScrollPane listScroll = new JScrollPane(overlayList);
        listScroll.setPreferredSize(new Dimension(280, 200));
        overlayPanel.add(listScroll, BorderLayout.NORTH);
        
        // Controles
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Opacidad
        gbc.gridx = 0; gbc.gridy = 0;
        controlsPanel.add(new JLabel("Opacidad:"), gbc);
        
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.addChangeListener(e -> cambiarOpacidad());
        gbc.gridx = 1; gbc.gridwidth = 2;
        controlsPanel.add(opacitySlider, gbc);
        
        // Posición X
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        controlsPanel.add(new JLabel("Posición X:"), gbc);
        
        xSpinner = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 10));
        xSpinner.addChangeListener(e -> cambiarPosicion());
        gbc.gridx = 1;
        controlsPanel.add(xSpinner, gbc);
        
        // Posición Y
        gbc.gridx = 0; gbc.gridy = 2;
        controlsPanel.add(new JLabel("Posición Y:"), gbc);
        
        ySpinner = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 10));
        ySpinner.addChangeListener(e -> cambiarPosicion());
        gbc.gridx = 1;
        controlsPanel.add(ySpinner, gbc);
        
        // Botones de reordenamiento
        JPanel orderPanel = new JPanel(new FlowLayout());
        JButton upBtn = new JButton("↑");
        upBtn.addActionListener(e -> moverOverlay(-1));
        JButton downBtn = new JButton("↓");
        downBtn.addActionListener(e -> moverOverlay(1));
        JButton frontBtn = new JButton("🔝");
        frontBtn.addActionListener(e -> alFrente());
        JButton backBtn = new JButton("🔻");
        backBtn.addActionListener(e -> alFondo());
        
        orderPanel.add(upBtn);
        orderPanel.add(downBtn);
        orderPanel.add(frontBtn);
        orderPanel.add(backBtn);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        controlsPanel.add(orderPanel, gbc);
        
        overlayPanel.add(controlsPanel, BorderLayout.CENTER);
        
        // Estado
        JLabel statusLabel = new JLabel("<html><center>✅ Sistema de overlays activo<br/>Selecciona un overlay para editarlo</center></html>");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(overlayPanel, BorderLayout.EAST);
    }
    
    private void cargarImagenBase() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(fc.getSelectedFile());
                overlays.add(0, img);
                overlayNames.add(0, "🖼️ Base: " + fc.getSelectedFile().getName());
                overlayOpacities.add(0, 1.0f);
                overlayPositions.add(0, new Point(0, 0));
                
                actualizarLista();
                mosaicPanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar imagen: " + ex.getMessage());
            }
        }
    }
    
    private void añadirOverlay() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png", "gif"));
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(fc.getSelectedFile());
                overlays.add(img);
                overlayNames.add("📄 " + fc.getSelectedFile().getName());
                overlayOpacities.add(0.7f);
                overlayPositions.add(new Point(50, 50));
                
                actualizarLista();
                mosaicPanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar overlay: " + ex.getMessage());
            }
        }
    }
    
    private void eliminarOverlay() {
        if (selectedOverlay >= 0 && selectedOverlay < overlays.size()) {
            overlays.remove(selectedOverlay);
            overlayNames.remove(selectedOverlay);
            overlayOpacities.remove(selectedOverlay);
            overlayPositions.remove(selectedOverlay);
            
            selectedOverlay = -1;
            actualizarLista();
            actualizarControles();
            mosaicPanel.repaint();
        }
    }
    
    private void limpiarTodo() {
        overlays.clear();
        overlayNames.clear();
        overlayOpacities.clear();
        overlayPositions.clear();
        
        selectedOverlay = -1;
        actualizarLista();
        actualizarControles();
        mosaicPanel.repaint();
        
        // Recrear ejemplos
        crearOverlaysDeEjemplo();
    }
    
    private void crearOverlaysDeEjemplo() {
        // Fondo azul
        BufferedImage fondo = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fondo.createGraphics();
        g.setColor(new Color(100, 150, 255, 150));
        g.fillRect(0, 0, 400, 300);
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(3));
        g.drawRect(2, 2, 396, 296);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("FONDO BASE", 140, 160);
        g.dispose();
        
        // Círculo rojo
        BufferedImage circulo = new BufferedImage(150, 150, BufferedImage.TYPE_INT_ARGB);
        g = circulo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 100, 100, 180));
        g.fillOval(10, 10, 130, 130);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.drawOval(10, 10, 130, 130);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("OVERLAY", 50, 70);
        g.drawString("CÍRCULO", 45, 90);
        g.dispose();
        
        // Añadir a las listas
        overlays.add(fondo);
        overlayNames.add("🟦 Fondo Base");
        overlayOpacities.add(0.6f);
        overlayPositions.add(new Point(50, 50));
        
        overlays.add(circulo);
        overlayNames.add("🔴 Círculo Rojo");
        overlayOpacities.add(0.8f);
        overlayPositions.add(new Point(150, 100));
        
        actualizarLista();
        mosaicPanel.repaint();
    }
    
    private void cambiarEscala() {
        String input = JOptionPane.showInputDialog(this, 
            "Ingresa la escala (ejemplo: 0.5 = 50%, 2.0 = 200%):", 
            String.valueOf(globalScale));
        
        if (input != null) {
            try {
                double newScale = Double.parseDouble(input);
                if (newScale > 0.1 && newScale <= 5.0) {
                    globalScale = newScale;
                    mosaicPanel.repaint();
                    JOptionPane.showMessageDialog(this, 
                        String.format("✅ Escala cambiada a %.1f%% (%.2fx)", globalScale * 100, globalScale));
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Escala debe estar entre 0.1 y 5.0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "❌ Formato inválido. Usa números como 0.5 o 2.0");
            }
        }
    }
    
    private void generarMosaicoFinal() {
        if (overlays.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❌ No hay overlays para generar mosaico");
            return;
        }
        
        // Calcular dimensiones del mosaico final
        int maxWidth = 0, maxHeight = 0;
        for (int i = 0; i < overlays.size(); i++) {
            BufferedImage img = overlays.get(i);
            Point pos = overlayPositions.get(i);
            int w = (int) (img.getWidth() * globalScale) + pos.x;
            int h = (int) (img.getHeight() * globalScale) + pos.y;
            maxWidth = Math.max(maxWidth, w);
            maxHeight = Math.max(maxHeight, h);
        }
        
        // Crear imagen final
        mosaicFinal = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mosaicFinal.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        // Fondo blanco
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, maxWidth, maxHeight);
        
        // Renderizar cada overlay como mosaico individual
        for (int i = 0; i < overlays.size(); i++) {
            BufferedImage overlay = overlays.get(i);
            Point pos = overlayPositions.get(i);
            float opacity = overlayOpacities.get(i);
            
            // Aplicar transformación de escala
            int scaledWidth = (int) (overlay.getWidth() * globalScale);
            int scaledHeight = (int) (overlay.getHeight() * globalScale);
            
            // Configurar transparencia
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g2.setComposite(ac);
            
            // Dibujar overlay escalado (cada uno como mosaico individual)
            g2.drawImage(overlay, pos.x, pos.y, scaledWidth, scaledHeight, null);
        }
        
        g2.dispose();
        
        // Mostrar información del mosaico generado
        String info = String.format(
            "🎨 MOSAICO FINAL GENERADO:\n\n" +
            "📐 Dimensiones: %d x %d píxeles\n" +
            "📊 Overlays combinados: %d\n" +
            "🔍 Escala aplicada: %.1f%% (%.2fx)\n" +
            "💾 Listo para guardar\n\n" +
            "Cada overlay representa un mosaico único\n" +
            "que se combina en la imagen final.",
            maxWidth, maxHeight, overlays.size(), globalScale * 100, globalScale
        );
        
        JOptionPane.showMessageDialog(this, info, "Mosaico Generado", JOptionPane.INFORMATION_MESSAGE);
        
        // Actualizar vista para mostrar el resultado
        mosaicPanel.repaint();
    }
    
    private void guardarImagen() {
        if (mosaicFinal == null) {
            int respuesta = JOptionPane.showConfirmDialog(this, 
                "¿Generar mosaico final antes de guardar?", 
                "Mosaico no generado", 
                JOptionPane.YES_NO_OPTION);
            
            if (respuesta == JOptionPane.YES_OPTION) {
                generarMosaicoFinal();
            } else {
                return;
            }
        }
        
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));
        fc.setSelectedFile(new File("mosaico_final.png"));
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                
                javax.imageio.ImageIO.write(mosaicFinal, "png", file);
                JOptionPane.showMessageDialog(this, 
                    "✅ Mosaico guardado exitosamente:\n" + file.getAbsolutePath());
                    
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "❌ Error al guardar: " + ex.getMessage());
            }
        }
    }
    
    private void dibujarMosaicoConOverlays(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo
        g2.setColor(new Color(250, 250, 250));
        g2.fillRect(0, 0, mosaicPanel.getWidth(), mosaicPanel.getHeight());
        
        // Dibujar overlays en orden (cada uno como mosaico individual)
        for (int i = 0; i < overlays.size(); i++) {
            BufferedImage overlay = overlays.get(i);
            Point pos = overlayPositions.get(i);
            float opacity = overlayOpacities.get(i);
            
            // Aplicar escala global
            int scaledWidth = (int) (overlay.getWidth() * globalScale);
            int scaledHeight = (int) (overlay.getHeight() * globalScale);
            
            // Configurar transparencia
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g2.setComposite(ac);
            
            // Dibujar overlay escalado (representa mosaico individual)
            g2.drawImage(overlay, pos.x, pos.y, scaledWidth, scaledHeight, null);
        }
        
        // Restaurar composición normal
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Info en esquina
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(10, 10, 250, 80, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("📊 Overlays activos: " + overlays.size(), 20, 30);
        g2.drawString("🔍 Escala: " + String.format("%.1f%% (%.2fx)", globalScale * 100, globalScale), 20, 45);
        g2.drawString("🎨 Cada overlay = mosaico único", 20, 60);
        g2.drawString("✅ Sistema funcionando", 20, 75);
    }
    
    private void cambiarOpacidad() {
        if (selectedOverlay >= 0 && selectedOverlay < overlayOpacities.size()) {
            float newOpacity = opacitySlider.getValue() / 100.0f;
            overlayOpacities.set(selectedOverlay, newOpacity);
            mosaicPanel.repaint();
        }
    }
    
    private void cambiarPosicion() {
        if (selectedOverlay >= 0 && selectedOverlay < overlayPositions.size()) {
            int x = (Integer) xSpinner.getValue();
            int y = (Integer) ySpinner.getValue();
            overlayPositions.set(selectedOverlay, new Point(x, y));
            mosaicPanel.repaint();
        }
    }
    
    private void moverOverlay(int direction) {
        if (selectedOverlay >= 0 && overlays.size() > 1) {
            int newIndex = selectedOverlay + direction;
            if (newIndex >= 0 && newIndex < overlays.size()) {
                // Intercambiar elementos
                java.util.Collections.swap(overlays, selectedOverlay, newIndex);
                java.util.Collections.swap(overlayNames, selectedOverlay, newIndex);
                java.util.Collections.swap(overlayOpacities, selectedOverlay, newIndex);
                java.util.Collections.swap(overlayPositions, selectedOverlay, newIndex);
                
                selectedOverlay = newIndex;
                actualizarLista();
                overlayList.setSelectedIndex(selectedOverlay);
                mosaicPanel.repaint();
            }
        }
    }
    
    private void alFrente() {
        if (selectedOverlay >= 0 && selectedOverlay < overlays.size() - 1) {
            // Mover al final (frente en el renderizado)
            BufferedImage img = overlays.remove(selectedOverlay);
            String name = overlayNames.remove(selectedOverlay);
            Float opacity = overlayOpacities.remove(selectedOverlay);
            Point pos = overlayPositions.remove(selectedOverlay);
            
            overlays.add(img);
            overlayNames.add(name);
            overlayOpacities.add(opacity);
            overlayPositions.add(pos);
            
            selectedOverlay = overlays.size() - 1;
            actualizarLista();
            overlayList.setSelectedIndex(selectedOverlay);
            mosaicPanel.repaint();
        }
    }
    
    private void alFondo() {
        if (selectedOverlay > 0) {
            // Mover al principio (fondo en el renderizado)
            BufferedImage img = overlays.remove(selectedOverlay);
            String name = overlayNames.remove(selectedOverlay);
            Float opacity = overlayOpacities.remove(selectedOverlay);
            Point pos = overlayPositions.remove(selectedOverlay);
            
            overlays.add(0, img);
            overlayNames.add(0, name);
            overlayOpacities.add(0, opacity);
            overlayPositions.add(0, pos);
            
            selectedOverlay = 0;
            actualizarLista();
            overlayList.setSelectedIndex(selectedOverlay);
            mosaicPanel.repaint();
        }
    }
    
    private void actualizarLista() {
        listModel.clear();
        for (int i = 0; i < overlayNames.size(); i++) {
            String name = overlayNames.get(i);
            float opacity = overlayOpacities.get(i);
            Point pos = overlayPositions.get(i);
            listModel.addElement(String.format("%s (%.0f%%, %d,%d)", name, opacity * 100, pos.x, pos.y));
        }
    }
    
    private void actualizarControles() {
        if (selectedOverlay >= 0 && selectedOverlay < overlays.size()) {
            opacitySlider.setValue((int) (overlayOpacities.get(selectedOverlay) * 100));
            Point pos = overlayPositions.get(selectedOverlay);
            xSpinner.setValue(pos.x);
            ySpinner.setValue(pos.y);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BrickGraphicsConOverlays().setVisible(true);
            
            System.out.println("🎯 BRICKGRAPHICS CON OVERLAYS - VERSIÓN SIMPLIFICADA");
            System.out.println("=====================================================");
            System.out.println("✅ Interfaz integrada y funcional");
            System.out.println("✅ Sistema de overlays múltiples");
            System.out.println("✅ Controles en tiempo real");
            System.out.println("✅ Sin complicaciones técnicas");
            System.out.println("");
            System.out.println("💡 Funcionalidades:");
            System.out.println("   • Cargar imagen base");
            System.out.println("   • Añadir overlays múltiples");
            System.out.println("   • Controlar opacidad y posición");
            System.out.println("   • Reordenar capas");
            System.out.println("   • Vista en tiempo real");
        });
    }
}