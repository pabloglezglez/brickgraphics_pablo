package mosaic;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import bricks.ToBricksType;

/**
 * Demostración del sistema de overlays múltiples de mosaicos.
 * Muestra cómo crear y gestionar múltiples overlays con configuraciones independientes.
 * 
 * @author BrickGraphics
 */
public class MultipleOverlayDemo {
    
    /**
     * Ejemplo básico de uso del sistema de overlays múltiples
     */
    public static void demoBasicOverlays() {
        System.out.println("=== Demo: Sistema de Overlays Múltiples ===");
        
        // Crear el manager
        MosaicOverlayManager manager = new MosaicOverlayManager();
        manager.setCanvasSize(new Dimension(1000, 800));
        
        // Crear imágenes de ejemplo (normalmente serían cargadas desde archivos)
        BufferedImage baseImage = createExampleImage(200, 200, Color.BLUE);
        BufferedImage overlay1Image = createExampleImage(150, 150, Color.RED);
        BufferedImage overlay2Image = createExampleImage(100, 100, Color.GREEN);
        
        // Crear overlays con configuraciones independientes
        MosaicOverlay baseOverlay = new MosaicOverlay("Base Layer", baseImage, new Point(100, 100));
        baseOverlay.setOpacity(0.8f);
        baseOverlay.setBrickType(ToBricksType.STUD_FROM_TOP);
        
        MosaicOverlay redOverlay = new MosaicOverlay("Red Accent", overlay1Image, new Point(150, 150));
        redOverlay.setOpacity(0.9f);
        redOverlay.setBrickType(ToBricksType.BRICK_FROM_TOP);
        
        MosaicOverlay greenOverlay = new MosaicOverlay("Green Detail", overlay2Image, new Point(200, 200));
        greenOverlay.setOpacity(0.7f);
        greenOverlay.setBrickType(ToBricksType.STUD_FROM_TOP);
        
        // Añadir overlays al manager
        manager.addOverlay(baseOverlay);
        manager.addOverlay(redOverlay);
        manager.addOverlay(greenOverlay);
        
        // Mostrar información
        System.out.println("Overlays creados: " + manager.getOverlayCount());
        System.out.println("Estado de memoria: " + manager.getMemoryStatus());
        
        // Demostrar gestión de orden
        System.out.println("\n--- Gestión de orden de capas ---");
        listOverlayOrder(manager);
        
        manager.moveOverlayToFront(0); // Mover base al frente
        System.out.println("Después de mover 'Base Layer' al frente:");
        listOverlayOrder(manager);
        
        // Demostrar configuración global
        System.out.println("\n--- Configuración global ---");
        manager.setGlobalBrickType(ToBricksType.BRICK_FROM_TOP);
        manager.setGlobalOpacity(0.8f);
        
        // Cleanup
        manager.dispose();
        System.out.println("\nDemo completado.");
    }
    
    /**
     * Demostración de escalabilidad con muchos overlays
     */
    public static void demoScalability() {
        System.out.println("\n=== Demo: Escalabilidad con múltiples overlays ===");
        
        // Configuración para manejar muchos overlays
        MosaicOverlayManager manager = new MosaicOverlayManager(
            new Dimension(1200, 900),  // Canvas grande
            256 * 1024 * 1024,        // 256MB de memoria máxima
            15                        // Máximo 15 overlays procesados en memoria
        );
        
        // Crear múltiples overlays simulando una composición compleja
        System.out.println("Creando múltiples overlays...");
        
        for (int i = 0; i < 25; i++) {
            // Crear imagen de ejemplo con diferentes colores
            Color overlayColor = new Color(
                (i * 50) % 256,
                (i * 80) % 256,
                (i * 120) % 256
            );
            
            BufferedImage image = createExampleImage(80 + i * 5, 80 + i * 5, overlayColor);
            
            Point position = new Point(
                50 + (i % 5) * 100,  // Distribuir en grid 5x5
                50 + (i / 5) * 100
            );
            
            MosaicOverlay overlay = new MosaicOverlay("Overlay_" + (i + 1), image, position);
            overlay.setOpacity(0.3f + (i % 7) * 0.1f); // Opacidades variables
            overlay.setBrickType(i % 2 == 0 ? ToBricksType.STUD_FROM_TOP : ToBricksType.BRICK_FROM_TOP);
            
            manager.addOverlay(overlay);
            
            // Mostrar progreso cada 5 overlays
            if ((i + 1) % 5 == 0) {
                System.out.println("Overlays creados: " + (i + 1) + 
                                 " | " + manager.getMemoryStatus());
            }
        }
        
        // Demostrar optimización automática de memoria
        System.out.println("\nForzando optimización de memoria...");
        manager.optimizeMemoryUsage();
        System.out.println("Después de optimización: " + manager.getMemoryStatus());
        
        // Simular renderizado (sin Graphics2D real para este demo)
        System.out.println("\nSimulando renderizado de todos los overlays...");
        int visibleOverlays = 0;
        for (MosaicOverlay overlay : manager.getAllOverlays()) {
            if (overlay.isVisible() && overlay.intersectsCanvas(manager.getCanvasSize())) {
                visibleOverlays++;
            }
        }
        System.out.println("Overlays visibles en canvas: " + visibleOverlays + "/" + manager.getOverlayCount());
        
        // Cleanup
        manager.dispose();
        System.out.println("Demo de escalabilidad completado.");
    }
    
    /**
     * Demostración de configuraciones independientes por overlay
     */
    public static void demoIndependentConfigurations() {
        System.out.println("\n=== Demo: Configuraciones independientes ===");
        
        MosaicOverlayManager manager = new MosaicOverlayManager();
        
        // Crear overlays con configuraciones muy diferentes
        MosaicOverlay overlay1 = new MosaicOverlay("Estructura Principal");
        overlay1.setSourceImage(createExampleImage(300, 200, Color.GRAY));
        overlay1.setPosition(50, 50);
        overlay1.setOpacity(1.0f);
        overlay1.setBrickType(ToBricksType.BRICK_FROM_TOP);
        overlay1.setVisible(true);
        
        MosaicOverlay overlay2 = new MosaicOverlay("Detalles Rojos");
        overlay2.setSourceImage(createExampleImage(150, 150, Color.RED));
        overlay2.setPosition(100, 100);
        overlay2.setOpacity(0.6f);
        overlay2.setBrickType(ToBricksType.STUD_FROM_TOP);
        overlay2.setVisible(true);
        
        MosaicOverlay overlay3 = new MosaicOverlay("Acentos Azules");
        overlay3.setSourceImage(createExampleImage(100, 100, Color.BLUE));
        overlay3.setPosition(200, 150);
        overlay3.setOpacity(0.4f);
        overlay3.setBrickType(ToBricksType.BRICK_FROM_TOP);
        overlay3.setVisible(false); // Inicialmente oculto
        
        // Añadir al manager
        manager.addOverlay(overlay1);
        manager.addOverlay(overlay2);
        manager.addOverlay(overlay3);
        
        // Mostrar configuraciones
        System.out.println("Configuraciones independientes:");
        for (int i = 0; i < manager.getOverlayCount(); i++) {
            MosaicOverlay overlay = manager.getOverlay(i);
            System.out.println(String.format(
                "  %d. %s - Pos:(%d,%d) Opacity:%.1f Visible:%s Type:%s",
                i + 1,
                overlay.getName(),
                overlay.getPosition().x,
                overlay.getPosition().y,
                overlay.getOpacity(),
                overlay.isVisible() ? "Sí" : "No",
                overlay.getBrickType()
            ));
        }
        
        // Modificar configuraciones individualmente
        System.out.println("\nModificando overlay 3 (Acentos Azules)...");
        overlay3.setVisible(true);
        overlay3.setOpacity(0.8f);
        overlay3.setPosition(175, 175);
        
        System.out.println("Nueva configuración del overlay 3:");
        System.out.println(String.format(
            "  %s - Pos:(%d,%d) Opacity:%.1f Visible:%s",
            overlay3.getName(),
            overlay3.getPosition().x,
            overlay3.getPosition().y,
            overlay3.getOpacity(),
            overlay3.isVisible() ? "Sí" : "No"
        ));
        
        manager.dispose();
        System.out.println("Demo de configuraciones independientes completado.");
    }
    
    /**
     * Lista el orden actual de los overlays
     */
    private static void listOverlayOrder(MosaicOverlayManager manager) {
        List<MosaicOverlay> overlays = manager.getAllOverlays();
        for (int i = 0; i < overlays.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + overlays.get(i).getName() + 
                             " (z-index: " + i + ")");
        }
    }
    
    /**
     * Crea una imagen de ejemplo con el color especificado
     */
    private static BufferedImage createExampleImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Fondo del color especificado
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        
        // Añadir algunos detalles para hacer la imagen más interesante
        g2d.setColor(color.darker());
        for (int i = 0; i < 5; i++) {
            g2d.fillRect(i * 20, i * 15, 15, 10);
        }
        
        g2d.setColor(color.brighter());
        for (int i = 0; i < 3; i++) {
            g2d.fillOval(i * 30 + 10, i * 25 + 10, 20, 20);
        }
        
        g2d.dispose();
        return image;
    }
    
    /**
     * Método principal para ejecutar todas las demostraciones
     */
    public static void main(String[] args) {
        System.out.println("BrickGraphics - Sistema de Overlays Múltiples");
        System.out.println("===============================================");
        
        demoBasicOverlays();
        demoScalability();
        demoIndependentConfigurations();
        
        System.out.println("\n=== Todas las demostraciones completadas ===");
        
        // Información del sistema
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("\nEstado de memoria del sistema:");
        System.out.println("  Memoria total: " + (totalMemory / 1024 / 1024) + "MB");
        System.out.println("  Memoria usada: " + (usedMemory / 1024 / 1024) + "MB");
        System.out.println("  Memoria libre: " + (freeMemory / 1024 / 1024) + "MB");
    }
}