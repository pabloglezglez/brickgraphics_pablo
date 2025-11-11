package mosaic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import mosaic.ui.panels.OverlayControlPanel;

/**
 * Aplicación demo completa que muestra el sistema de overlays múltiples
 * con interfaz gráfica para control interactivo.
 * 
 * @author BrickGraphics
 */
public class OverlaySystemGUI extends JFrame {
    
    private MosaicOverlayManager overlayManager;
    private OverlayControlPanel controlPanel;
    private OverlayCanvas canvas;
    private JMenuBar menuBar;
    
    public OverlaySystemGUI() {
        super("BrickGraphics - Sistema de Overlays Múltiples");
        
        // Inicializar componentes
        overlayManager = new MosaicOverlayManager();
        overlayManager.setCanvasSize(new Dimension(800, 600));
        
        initializeComponents();
        setupMenuBar();
        layoutComponents();
        setupEventHandlers();
        
        // Configuración de la ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Crear algunos overlays de ejemplo
        createExampleOverlays();
    }
    
    private void initializeComponents() {
        // Panel de control
        controlPanel = new OverlayControlPanel(overlayManager);
        controlPanel.setPreferredSize(new Dimension(350, 600));
        
        // Canvas de renderizado
        canvas = new OverlayCanvas();
        canvas.setPreferredSize(new Dimension(800, 600));
        canvas.setBackground(Color.WHITE);
    }
    
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // Menú Archivo
        JMenu fileMenu = new JMenu("Archivo");
        
        JMenuItem loadImageItem = new JMenuItem("Cargar Imagen como Overlay");
        loadImageItem.addActionListener(e -> loadImageAsOverlay());
        fileMenu.add(loadImageItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Menú Overlays
        JMenu overlayMenu = new JMenu("Overlays");
        
        JMenuItem addExampleItem = new JMenuItem("Añadir Overlay de Ejemplo");
        addExampleItem.addActionListener(e -> addExampleOverlay());
        overlayMenu.add(addExampleItem);
        
        JMenuItem clearAllItem = new JMenuItem("Limpiar Todos");
        clearAllItem.addActionListener(e -> clearAllOverlays());
        overlayMenu.add(clearAllItem);
        
        overlayMenu.addSeparator();
        
        JMenuItem optimizeMemoryItem = new JMenuItem("Optimizar Memoria");
        optimizeMemoryItem.addActionListener(e -> optimizeMemory());
        overlayMenu.add(optimizeMemoryItem);
        
        // Menú Ayuda
        JMenu helpMenu = new JMenu("Ayuda");
        
        JMenuItem aboutItem = new JMenuItem("Acerca de");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        JMenuItem helpItem = new JMenuItem("Guía de Uso");
        helpItem.addActionListener(e -> showHelpDialog());
        helpMenu.add(helpItem);
        
        menuBar.add(fileMenu);
        menuBar.add(overlayMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Panel izquierdo: controles
        add(controlPanel, BorderLayout.WEST);
        
        // Panel central: canvas
        JPanel canvasPanel = new JPanel(new BorderLayout());
        canvasPanel.setBorder(BorderFactory.createTitledBorder("Vista de Overlays"));
        canvasPanel.add(canvas, BorderLayout.CENTER);
        
        // Panel inferior: información de estado
        JPanel statusPanel = createStatusPanel();
        canvasPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(canvasPanel, BorderLayout.CENTER);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        JLabel statusLabel = new JLabel("Sistema de Overlays Múltiples - Listo");
        panel.add(statusLabel);
        
        // Actualizar estado periódicamente
        Timer timer = new Timer(2000, e -> {
            String status = overlayManager.getMemoryStatus();
            statusLabel.setText("Estado: " + status);
        });
        timer.start();
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Listener para cambios en los overlays
        controlPanel.setChangeListener(new OverlayControlPanel.OverlayChangeListener() {
            @Override
            public void onOverlaysChanged() {
                canvas.repaint();
            }
            
            @Override
            public void onOverlaySelected(MosaicOverlay overlay) {
                // Resaltar overlay seleccionado en el canvas
                canvas.setSelectedOverlay(overlay);
                canvas.repaint();
            }
        });
    }
    
    private void createExampleOverlays() {
        // Crear overlay base
        BufferedImage baseImage = createGradientImage(400, 300, Color.BLUE, Color.CYAN);
        MosaicOverlay baseOverlay = new MosaicOverlay("Base Azul", baseImage, new Point(50, 50));
        baseOverlay.setOpacity(0.8f);
        overlayManager.addOverlay(baseOverlay);
        
        // Crear overlay de acentos
        BufferedImage accentImage = createPatternImage(200, 150, Color.RED);
        MosaicOverlay accentOverlay = new MosaicOverlay("Acentos Rojos", accentImage, new Point(150, 100));
        accentOverlay.setOpacity(0.6f);
        overlayManager.addOverlay(accentOverlay);
        
        // Crear overlay de detalles
        BufferedImage detailImage = createCircleImage(120, Color.GREEN);
        MosaicOverlay detailOverlay = new MosaicOverlay("Detalles Verdes", detailImage, new Point(250, 180));
        detailOverlay.setOpacity(0.7f);
        overlayManager.addOverlay(detailOverlay);
        
        canvas.repaint();
    }
    
    private BufferedImage createGradientImage(int width, int height, Color startColor, Color endColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        GradientPaint gradient = new GradientPaint(0, 0, startColor, width, height, endColor);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
        
        g2d.dispose();
        return image;
    }
    
    private BufferedImage createPatternImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(color);
        
        // Crear patrón de cuadrados
        for (int x = 0; x < width; x += 20) {
            for (int y = 0; y < height; y += 20) {
                if ((x + y) % 40 == 0) {
                    g2d.fillRect(x, y, 15, 15);
                }
            }
        }
        
        g2d.dispose();
        return image;
    }
    
    private BufferedImage createCircleImage(int size, Color color) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo transparente
        g2d.setColor(new Color(255, 255, 255, 0));
        g2d.fillRect(0, 0, size, size);
        
        // Círculo de color
        g2d.setColor(color);
        g2d.fillOval(10, 10, size - 20, size - 20);
        
        // Borde
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(10, 10, size - 20, size - 20);
        
        g2d.dispose();
        return image;
    }
    
    // Acciones de menú
    
    private void loadImageAsOverlay() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Imágenes", "jpg", "jpeg", "png", "gif", "bmp"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                BufferedImage image = ImageIO.read(file);
                
                String name = file.getName();
                MosaicOverlay overlay = new MosaicOverlay(name, image, new Point(100, 100));
                overlay.setOpacity(0.8f);
                
                overlayManager.addOverlay(overlay);
                canvas.repaint();
                
                JOptionPane.showMessageDialog(this,
                    "Imagen cargada como overlay: " + name,
                    "Overlay Añadido",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error al cargar la imagen: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void addExampleOverlay() {
        // Crear overlay aleatorio
        Color randomColor = new Color(
            (int)(Math.random() * 256),
            (int)(Math.random() * 256),
            (int)(Math.random() * 256)
        );
        
        BufferedImage image = createGradientImage(150, 100, randomColor, randomColor.brighter());
        Point position = new Point(
            (int)(Math.random() * 400),
            (int)(Math.random() * 300)
        );
        
        String name = "Overlay " + (overlayManager.getOverlayCount() + 1);
        MosaicOverlay overlay = new MosaicOverlay(name, image, position);
        overlay.setOpacity(0.5f + (float)(Math.random() * 0.5f));
        
        overlayManager.addOverlay(overlay);
        canvas.repaint();
    }
    
    private void clearAllOverlays() {
        int result = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar todos los overlays?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            overlayManager.clearAllOverlays();
            canvas.repaint();
        }
    }
    
    private void optimizeMemory() {
        overlayManager.optimizeMemoryUsage();
        JOptionPane.showMessageDialog(this,
            "Memoria optimizada.\n" + overlayManager.getMemoryStatus(),
            "Optimización Completada",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAboutDialog() {
        String message = "Sistema de Overlays Múltiples para BrickGraphics\n\n" +
                        "Características:\n" +
                        "• Overlays ilimitados\n" +
                        "• Configuraciones independientes\n" +
                        "• Gestión automática de memoria\n" +
                        "• Control de opacidad y posición\n\n" +
                        "Versión: 1.0\n" +
                        "Autor: BrickGraphics Team";
        
        JOptionPane.showMessageDialog(this, message, "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showHelpDialog() {
        String help = "Guía de Uso del Sistema de Overlays:\n\n" +
                     "1. AÑADIR OVERLAYS:\n" +
                     "   • Use 'Archivo > Cargar Imagen' para añadir imágenes\n" +
                     "   • Use 'Overlays > Añadir Ejemplo' para crear overlays de prueba\n\n" +
                     "2. CONTROLAR OVERLAYS:\n" +
                     "   • Seleccione un overlay en la lista\n" +
                     "   • Ajuste opacidad con el slider\n" +
                     "   • Cambie posición con los spinners X,Y\n" +
                     "   • Use checkboxes para mostrar/ocultar\n\n" +
                     "3. GESTIONAR ORDEN:\n" +
                     "   • Use botones ↑↓ para reordenar\n" +
                     "   • 'Al Frente' y 'Al Fondo' para posición extrema\n\n" +
                     "4. MEMORIA:\n" +
                     "   • El sistema optimiza automáticamente\n" +
                     "   • Use 'Optimizar Memoria' si es necesario";
        
        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Guía de Uso", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Canvas personalizado para renderizar los overlays
     */
    private class OverlayCanvas extends JPanel {
        private MosaicOverlay selectedOverlay;
        
        public OverlayCanvas() {
            setDoubleBuffered(true);
        }
        
        public void setSelectedOverlay(MosaicOverlay overlay) {
            this.selectedOverlay = overlay;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Fondo cuadriculado
            drawGrid(g2d);
            
            // Renderizar todos los overlays
            overlayManager.renderAllOverlays(g2d);
            
            // Resaltar overlay seleccionado
            if (selectedOverlay != null && selectedOverlay.isVisible()) {
                drawSelectionBorder(g2d, selectedOverlay);
            }
            
            // Información en pantalla
            drawInfo(g2d);
        }
        
        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(new Color(240, 240, 240));
            Dimension size = getSize();
            
            for (int x = 0; x < size.width; x += 20) {
                g2d.drawLine(x, 0, x, size.height);
            }
            for (int y = 0; y < size.height; y += 20) {
                g2d.drawLine(0, y, size.width, y);
            }
        }
        
        private void drawSelectionBorder(Graphics2D g2d, MosaicOverlay overlay) {
            Point pos = overlay.getPosition();
            Dimension size = overlay.getSize();
            
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                                         0, new float[]{10, 5}, 0));
            g2d.drawRect(pos.x - 2, pos.y - 2, size.width + 4, size.height + 4);
            
            // Esquinas de redimensionamiento
            g2d.setStroke(new BasicStroke(1));
            g2d.setColor(Color.BLUE);
            int cornerSize = 6;
            g2d.fillRect(pos.x - cornerSize/2, pos.y - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(pos.x + size.width - cornerSize/2, pos.y - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(pos.x - cornerSize/2, pos.y + size.height - cornerSize/2, cornerSize, cornerSize);
            g2d.fillRect(pos.x + size.width - cornerSize/2, pos.y + size.height - cornerSize/2, cornerSize, cornerSize);
        }
        
        private void drawInfo(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 128));
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            
            String info = String.format("Overlays: %d | %s", 
                overlayManager.getOverlayCount(),
                overlayManager.getMemoryStatus());
            
            FontMetrics fm = g2d.getFontMetrics();
            int infoWidth = fm.stringWidth(info);
            int infoHeight = fm.getHeight();
            
            // Fondo semi-transparente
            g2d.fillRect(5, 5, infoWidth + 10, infoHeight + 5);
            
            // Texto
            g2d.setColor(Color.WHITE);
            g2d.drawString(info, 10, 15 + fm.getAscent());
        }
    }
    
    /**
     * Método principal para ejecutar la aplicación demo
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Usar look and feel por defecto
            }
            
            new OverlaySystemGUI().setVisible(true);
        });
    }
}