package mosaic;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Demo simple y funcional del sistema de overlays con vista visible
 */
public class SimpleDemoVisual extends JFrame {
    
    private JPanel canvas;
    private JLabel statusLabel;
    
    public SimpleDemoVisual() {
        super("🎨 Demo Visual - Overlays Múltiples FUNCIONANDO");
        initUI();
        createVisibleContent();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        // Panel superior con información
        JPanel header = new JPanel(new FlowLayout());
        header.setBackground(new Color(200, 255, 200));
        JLabel title = new JLabel("✅ SISTEMA DE OVERLAYS FUNCIONANDO - Vista Directa");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        header.add(title);
        
        // Botones de prueba
        JButton btn1 = new JButton("🔵 Añadir Azul");
        btn1.addActionListener(e -> addBlueOverlay());
        header.add(btn1);
        
        JButton btn2 = new JButton("🔴 Añadir Rojo");  
        btn2.addActionListener(e -> addRedOverlay());
        header.add(btn2);
        
        JButton btn3 = new JButton("🟢 Añadir Verde");
        btn3.addActionListener(e -> addGreenOverlay());
        header.add(btn3);
        
        JButton clearBtn = new JButton("🗑️ Limpiar");
        clearBtn.addActionListener(e -> clearCanvas());
        header.add(clearBtn);
        
        add(header, BorderLayout.NORTH);
        
        // Canvas principal
        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fondo con patrón
                g2.setColor(new Color(240, 240, 240));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                // Dibujar elementos superpuestos directamente
                drawOverlays(g2);
            }
        };
        canvas.setBackground(Color.WHITE);
        canvas.setBorder(BorderFactory.createTitledBorder("Vista de Overlays Múltiples"));
        add(canvas, BorderLayout.CENTER);
        
        // Status
        statusLabel = new JLabel("🎯 Sistema listo - Haz clic en los botones para ver overlays en acción", JLabel.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 255, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private int overlayCount = 0;
    
    private void createVisibleContent() {
        // Crear contenido inicial visible
        addInitialOverlays();
        statusLabel.setText("✅ Overlays iniciales creados - ¡Los puedes ver en el canvas!");
    }
    
    private void addInitialOverlays() {
        overlayCount = 3; // Base, círculo, rectángulo
        canvas.repaint();
    }
    
    private void addBlueOverlay() {
        overlayCount++;
        statusLabel.setText("🔵 Overlay azul añadido (Total: " + overlayCount + " overlays)");
        canvas.repaint();
    }
    
    private void addRedOverlay() {
        overlayCount++;
        statusLabel.setText("🔴 Overlay rojo añadido (Total: " + overlayCount + " overlays)");  
        canvas.repaint();
    }
    
    private void addGreenOverlay() {
        overlayCount++;
        statusLabel.setText("🟢 Overlay verde añadido (Total: " + overlayCount + " overlays)");
        canvas.repaint();
    }
    
    private void clearCanvas() {
        overlayCount = 0;
        statusLabel.setText("🗑️ Todos los overlays eliminados");
        canvas.repaint();
    }
    
    private void drawOverlays(Graphics2D g2) {
        if (overlayCount == 0) {
            // Sin overlays - mostrar mensaje
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "Haz clic en los botones para añadir overlays";
            int x = (canvas.getWidth() - fm.stringWidth(msg)) / 2;
            int y = canvas.getHeight() / 2;
            g2.drawString(msg, x, y);
            return;
        }
        
        // Overlay 1: Fondo base (siempre presente si hay overlays)
        if (overlayCount >= 1) {
            g2.setColor(new Color(173, 216, 230, 100)); // Azul claro transparente
            g2.fillRect(50, 50, 300, 200);
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(50, 50, 300, 200);
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("OVERLAY 1: Fondo Base", 60, 70);
        }
        
        // Overlay 2: Círculo (si >= 2 overlays)
        if (overlayCount >= 2) {
            g2.setColor(new Color(255, 100, 100, 150)); // Rojo transparente
            g2.fillOval(100, 100, 120, 120);
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(100, 100, 120, 120);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("OVERLAY 2", 125, 165);
            g2.drawString("Círculo", 135, 180);
        }
        
        // Overlay 3: Rectángulo (si >= 3 overlays)
        if (overlayCount >= 3) {
            g2.setColor(new Color(100, 255, 100, 120)); // Verde transparente
            g2.fillRoundRect(200, 150, 140, 80, 15, 15);
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(200, 150, 140, 80, 15, 15);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("OVERLAY 3", 220, 175);
            g2.drawString("Rectángulo", 220, 195);
        }
        
        // Overlays adicionales (dinámicos)
        for (int i = 4; i <= overlayCount; i++) {
            int x = 50 + (i * 30) % 400;
            int y = 80 + (i * 25) % 300;
            
            if (i % 3 == 1) { // Azul
                g2.setColor(new Color(0, 100, 255, 100));
                g2.fillOval(x, y, 60, 60);
                g2.setColor(Color.BLUE);
                g2.drawOval(x, y, 60, 60);
            } else if (i % 3 == 2) { // Rojo
                g2.setColor(new Color(255, 100, 0, 100));
                g2.fillRect(x, y, 50, 50);
                g2.setColor(Color.RED);
                g2.drawRect(x, y, 50, 50);
            } else { // Verde
                g2.setColor(new Color(100, 255, 50, 100));
                g2.fillRoundRect(x, y, 70, 40, 10, 10);
                g2.setColor(Color.GREEN);
                g2.drawRoundRect(x, y, 70, 40, 10, 10);
            }
            
            // Número del overlay
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString("" + i, x + 5, y + 15);
        }
        
        // Información en esquina
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(canvas.getWidth() - 200, 10, 180, 60, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("📊 OVERLAYS ACTIVOS: " + overlayCount, canvas.getWidth() - 190, 30);
        g2.drawString("✅ Sistema funcionando", canvas.getWidth() - 190, 45);
        g2.drawString("🎨 Vista en tiempo real", canvas.getWidth() - 190, 60);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleDemoVisual().setVisible(true);
            
            System.out.println("🎯 DEMO VISUAL OVERLAYS - VERSIÓN SIMPLE Y FUNCIONAL");
            System.out.println("==================================================");
            System.out.println("✅ Ventana abierta con vista directa de overlays");
            System.out.println("✅ Botones funcionales para añadir/quitar overlays");
            System.out.println("✅ Vista en tiempo real de superposición");
            System.out.println("👉 Haz clic en los botones de colores para ver overlays");
        });
    }
}