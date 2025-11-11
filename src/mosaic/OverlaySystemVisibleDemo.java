package mosaic;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mosaic.ui.panels.OverlayControlPanel;

/**
 * Versión mejorada del sistema de overlays con imágenes de ejemplo visibles
 */
public class OverlaySystemVisibleDemo extends JFrame {
    
    private JTabbedPane mainTabs;
    private OverlayControlPanel overlayControlPanel;
    private JPanel previewPanel;
    private JLabel previewLabel;
    private JLabel statusLabel;
    private MosaicOverlayManager overlayManager;
    private BufferedImage compositeImage;
    
    public OverlaySystemVisibleDemo() {
        super("BrickGraphics - Sistema Overlays Múltiples (VERSIÓN VISIBLE)");
        initializeSystem();
        setupUI();
        createExampleOverlays();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        updatePreview();
    }
    
    private void initializeSystem() {
        // Inicializar el sistema de overlays
        overlayManager = new MosaicOverlayManager();
        
        // Crear panel de control
        overlayControlPanel = new OverlayControlPanel();
        
        // Configurar listener para actualizaciones
        overlayControlPanel.setUpdateListener(new Runnable() {
            @Override
            public void run() {
                updatePreview();
            }
        });
        
        // Panel de vista previa
        previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Vista Previa de Overlays"));
        previewPanel.setPreferredSize(new Dimension(500, 400));
        
        previewLabel = new JLabel("", JLabel.CENTER);
        previewLabel.setBackground(Color.WHITE);
        previewLabel.setOpaque(true);
        previewPanel.add(new JScrollPane(previewLabel), BorderLayout.CENTER);
        
        // Label de estado
        statusLabel = new JLabel("✅ Sistema inicializado - Añadiendo overlays de ejemplo...", JLabel.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(200, 255, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Panel superior
        JPanel headerPanel = new JPanel(new FlowLayout());
        headerPanel.setBackground(new Color(240, 240, 240));
        
        JLabel titleLabel = new JLabel("🎯 OVERLAYS MÚLTIPLES CON VISTA PREVIA");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(50, 100, 50));
        headerPanel.add(titleLabel);
        
        // Botones de acción
        JButton addImageButton = new JButton("🖼️ Añadir Imagen");
        addImageButton.addActionListener(e -> addImageOverlay());
        headerPanel.add(addImageButton);
        
        JButton clearAllButton = new JButton("🗑️ Limpiar Todo");
        clearAllButton.addActionListener(e -> clearAllOverlays());
        headerPanel.add(clearAllButton);
        
        JButton refreshButton = new JButton("🔄 Actualizar Vista");
        refreshButton.addActionListener(e -> updatePreview());
        headerPanel.add(refreshButton);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Panel principal con pestañas
        mainTabs = new JTabbedPane();
        mainTabs.addTab("🎛️ Panel de Control", overlayControlPanel);
        mainTabs.addTab("👁️ Vista Previa", previewPanel);
        
        // Panel lateral con controles rápidos
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBorder(BorderFactory.createTitledBorder("Controles Rápidos"));
        sidePanel.setPreferredSize(new Dimension(250, 400));
        
        JPanel quickControls = new JPanel(new GridLayout(0, 1, 5, 5));
        
        JButton exampleButton1 = new JButton("🔵 Añadir Círculo Azul");
        exampleButton1.addActionListener(e -> addColoredShape(Color.BLUE, "Círculo Azul"));
        quickControls.add(exampleButton1);
        
        JButton exampleButton2 = new JButton("🔴 Añadir Rectángulo Rojo");
        exampleButton2.addActionListener(e -> addColoredRect(Color.RED, "Rectángulo Rojo"));
        quickControls.add(exampleButton2);
        
        JButton exampleButton3 = new JButton("🟢 Añadir Círculo Verde");
        exampleButton3.addActionListener(e -> addColoredShape(Color.GREEN, "Círculo Verde"));
        quickControls.add(exampleButton3);
        
        JButton patternButton = new JButton("🎨 Añadir Patrón");
        patternButton.addActionListener(e -> addPattern());
        quickControls.add(patternButton);
        
        sidePanel.add(quickControls, BorderLayout.NORTH);
        
        // Layout principal
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainTabs, sidePanel);
        splitPane.setDividerLocation(600);
        
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void createExampleOverlays() {
        statusLabel.setText("🎨 Creando overlays de ejemplo...");
        
        // Overlay 1: Fondo azul claro
        BufferedImage bg = createColoredImage(300, 200, new Color(173, 216, 230, 150), "FONDO");
        overlayManager.addOverlay("Fondo Azul", bg, new Point(50, 50));
        
        // Overlay 2: Círculo rojo
        BufferedImage circle = createCircleImage(100, Color.RED, "CÍRCULO");
        overlayManager.addOverlay("Círculo Rojo", circle, new Point(100, 80));
        
        // Overlay 3: Rectángulo verde
        BufferedImage rect = createRectImage(150, 80, Color.GREEN, "RECTÁNGULO");
        overlayManager.addOverlay("Rectángulo Verde", rect, new Point(150, 120));
        
        // Configurar opacidades
        if (overlayManager.getOverlay("Fondo Azul") != null) {
            overlayManager.getOverlay("Fondo Azul").setOpacity(0.3f);
        }
        if (overlayManager.getOverlay("Círculo Rojo") != null) {
            overlayManager.getOverlay("Círculo Rojo").setOpacity(0.7f);
        }
        if (overlayManager.getOverlay("Rectángulo Verde") != null) {
            overlayManager.getOverlay("Rectángulo Verde").setOpacity(0.5f);
        }
        
        statusLabel.setText("✅ Overlays de ejemplo creados - ¡Cambiar a pestaña Vista Previa!");
    }
    
    private BufferedImage createColoredImage(int width, int height, Color color, String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
        
        // Borde
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(2, 2, width-4, height-4);
        
        // Texto
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = height / 2 + fm.getAscent() / 2;
        g2.drawString(text, textX, textY);
        
        g2.dispose();
        return img;
    }
    
    private BufferedImage createCircleImage(int size, Color color, String text) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Círculo
        g2.setColor(color);
        g2.fillOval(5, 5, size-10, size-10);
        
        // Borde
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(5, 5, size-10, size-10);
        
        // Texto
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (size - fm.stringWidth(text)) / 2;
        int textY = size / 2 + fm.getAscent() / 2;
        g2.drawString(text, textX, textY);
        
        g2.dispose();
        return img;
    }
    
    private BufferedImage createRectImage(int width, int height, Color color, String text) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Rectángulo
        g2.setColor(color);
        g2.fillRoundRect(5, 5, width-10, height-10, 10, 10);
        
        // Borde
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(5, 5, width-10, height-10, 10, 10);
        
        // Texto
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(text)) / 2;
        int textY = height / 2 + fm.getAscent() / 2;
        g2.drawString(text, textX, textY);
        
        g2.dispose();
        return img;
    }
    
    private void addColoredShape(Color color, String name) {
        BufferedImage circle = createCircleImage(80, color, "NUEVO");
        Point randomPos = new Point((int)(Math.random() * 200), (int)(Math.random() * 200));
        overlayManager.addOverlay(name + " " + System.currentTimeMillis(), circle, randomPos);
        updatePreview();
        statusLabel.setText("✅ " + name + " añadido en posición " + randomPos);
    }
    
    private void addColoredRect(Color color, String name) {
        BufferedImage rect = createRectImage(120, 60, color, "NUEVO");
        Point randomPos = new Point((int)(Math.random() * 200), (int)(Math.random() * 200));
        overlayManager.addOverlay(name + " " + System.currentTimeMillis(), rect, randomPos);
        updatePreview();
        statusLabel.setText("✅ " + name + " añadido en posición " + randomPos);
    }
    
    private void addPattern() {
        BufferedImage pattern = createPatternImage(100, 100);
        Point randomPos = new Point((int)(Math.random() * 200), (int)(Math.random() * 200));
        overlayManager.addOverlay("Patrón " + System.currentTimeMillis(), pattern, randomPos);
        updatePreview();
        statusLabel.setText("✅ Patrón añadido en posición " + randomPos);
    }
    
    private BufferedImage createPatternImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Patrón de cuadrados
        for (int x = 0; x < width; x += 10) {
            for (int y = 0; y < height; y += 10) {
                Color c = ((x + y) / 10) % 2 == 0 ? Color.MAGENTA : Color.CYAN;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
                g2.fillRect(x, y, 10, 10);
            }
        }
        
        g2.dispose();
        return img;
    }
    
    private void addImageOverlay() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes", "jpg", "jpeg", "png", "gif"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = javax.imageio.ImageIO.read(fileChooser.getSelectedFile());
                if (img != null) {
                    String name = "Imagen " + System.currentTimeMillis();
                    overlayManager.addOverlay(name, img, new Point(0, 0));
                    updatePreview();
                    statusLabel.setText("✅ Imagen cargada: " + fileChooser.getSelectedFile().getName());
                } else {
                    statusLabel.setText("❌ Error: No se pudo cargar la imagen");
                }
            } catch (Exception ex) {
                statusLabel.setText("❌ Error al cargar imagen: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    private void clearAllOverlays() {
        overlayManager.clearAll();
        updatePreview();
        statusLabel.setText("🗑️ Todos los overlays eliminados");
    }
    
    private void updatePreview() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Crear imagen compuesta
                compositeImage = overlayManager.renderComposite(600, 400);
                
                if (compositeImage != null) {
                    ImageIcon icon = new ImageIcon(compositeImage);
                    previewLabel.setIcon(icon);
                    previewLabel.setText("");
                } else {
                    previewLabel.setIcon(null);
                    previewLabel.setText("<html><center><h2>🖼️ Vista Previa</h2><p>Añade overlays para ver la composición</p></center></html>");
                }
                
                previewPanel.revalidate();
                previewPanel.repaint();
                
            } catch (Exception ex) {
                statusLabel.setText("⚠️ Error actualizando vista previa: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new OverlaySystemVisibleDemo().setVisible(true);
            
            System.out.println("🎯 SISTEMA OVERLAYS MÚLTIPLES - VERSIÓN VISIBLE");
            System.out.println("==============================================");
            System.out.println("✅ Overlays de ejemplo creados automáticamente");
            System.out.println("✅ Vista previa disponible en segunda pestaña");
            System.out.println("✅ Controles rápidos en panel derecho");
            System.out.println("👉 Cambiar a pestaña 'Vista Previa' para ver overlays");
        });
    }
}