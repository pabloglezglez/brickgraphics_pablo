package mosaic;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import colors.LEGOColorGrid;
import bricks.ToBricksType;

/**
 * Gestiona múltiples overlays de mosaico permitiendo overlays ilimitados
 * con gestión de memoria y optimización de renderizado.
 * 
 * @author BrickGraphics
 */
public class MosaicOverlayManager {
    
    private List<MosaicOverlay> overlays;
    private Dimension canvasSize;
    private boolean autoOptimizeMemory;
    private long maxMemoryUsage; // en bytes
    private int maxOverlaysInMemory;
    
    // Estadísticas
    private long totalMemoryUsed;
    private int overlaysProcessed;
    private long lastOptimizationTime;
    
    /**
     * Constructor por defecto
     */
    public MosaicOverlayManager() {
        this.overlays = new ArrayList<>();
        this.canvasSize = new Dimension(800, 600);
        this.autoOptimizeMemory = true;
        this.maxMemoryUsage = 512 * 1024 * 1024; // 512MB por defecto
        this.maxOverlaysInMemory = 20; // máximo 20 overlays procesados en memoria
        this.totalMemoryUsed = 0;
        this.overlaysProcessed = 0;
        this.lastOptimizationTime = 0;
    }
    
    /**
     * Constructor con configuración personalizada
     */
    public MosaicOverlayManager(Dimension canvasSize, long maxMemoryUsage, int maxOverlaysInMemory) {
        this();
        this.canvasSize = new Dimension(canvasSize.width, canvasSize.height);
        this.maxMemoryUsage = maxMemoryUsage;
        this.maxOverlaysInMemory = maxOverlaysInMemory;
    }
    
    // Gestión de overlays
    
    /**
     * Añade un nuevo overlay
     */
    public void addOverlay(MosaicOverlay overlay) {
        if (overlay != null) {
            overlays.add(overlay);
            System.out.println("Overlay añadido: " + overlay.getName() + 
                             " (Total: " + overlays.size() + " overlays)");
            
            if (autoOptimizeMemory) {
                optimizeMemoryUsage();
            }
        }
    }
    
    /**
     * Remueve un overlay por índice
     */
    public boolean removeOverlay(int index) {
        if (index >= 0 && index < overlays.size()) {
            MosaicOverlay removed = overlays.remove(index);
            removed.dispose();
            System.out.println("Overlay removido: " + removed.getName());
            updateMemoryStats();
            return true;
        }
        return false;
    }
    
    /**
     * Remueve un overlay por nombre
     */
    public boolean removeOverlay(String name) {
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).getName().equals(name)) {
                return removeOverlay(i);
            }
        }
        return false;
    }
    
    /**
     * Remueve un overlay específico
     */
    public boolean removeOverlay(MosaicOverlay overlay) {
        boolean removed = overlays.remove(overlay);
        if (removed) {
            overlay.dispose();
            System.out.println("Overlay removido: " + overlay.getName());
            updateMemoryStats();
        }
        return removed;
    }
    
    /**
     * Obtiene un overlay por índice
     */
    public MosaicOverlay getOverlay(int index) {
        if (index >= 0 && index < overlays.size()) {
            return overlays.get(index);
        }
        return null;
    }
    
    /**
     * Obtiene un overlay por nombre
     */
    public MosaicOverlay getOverlay(String name) {
        for (MosaicOverlay overlay : overlays) {
            if (overlay.getName().equals(name)) {
                return overlay;
            }
        }
        return null;
    }
    
    /**
     * Obtiene todos los overlays
     */
    public List<MosaicOverlay> getAllOverlays() {
        return new ArrayList<>(overlays);
    }
    
    /**
     * Obtiene el número de overlays
     */
    public int getOverlayCount() {
        return overlays.size();
    }
    
    /**
     * Limpia todos los overlays
     */
    public void clearAllOverlays() {
        for (MosaicOverlay overlay : overlays) {
            overlay.dispose();
        }
        overlays.clear();
        totalMemoryUsed = 0;
        overlaysProcessed = 0;
        System.out.println("Todos los overlays han sido eliminados");
    }
    
    // Gestión de orden (z-index)
    
    /**
     * Mueve un overlay hacia arriba en el orden de renderizado
     */
    public boolean moveOverlayUp(int index) {
        if (index > 0 && index < overlays.size()) {
            Collections.swap(overlays, index, index - 1);
            return true;
        }
        return false;
    }
    
    /**
     * Mueve un overlay hacia abajo en el orden de renderizado
     */
    public boolean moveOverlayDown(int index) {
        if (index >= 0 && index < overlays.size() - 1) {
            Collections.swap(overlays, index, index + 1);
            return true;
        }
        return false;
    }
    
    /**
     * Mueve un overlay al frente (último en renderizar)
     */
    public boolean moveOverlayToFront(int index) {
        if (index >= 0 && index < overlays.size()) {
            MosaicOverlay overlay = overlays.remove(index);
            overlays.add(overlay);
            return true;
        }
        return false;
    }
    
    /**
     * Mueve un overlay al fondo (primero en renderizar)
     */
    public boolean moveOverlayToBack(int index) {
        if (index >= 0 && index < overlays.size()) {
            MosaicOverlay overlay = overlays.remove(index);
            overlays.add(0, overlay);
            return true;
        }
        return false;
    }
    
    // Renderizado
    
    /**
     * Renderiza todos los overlays visibles en el contexto gráfico
     */
    public void renderAllOverlays(Graphics2D g2d) {
        if (overlays.isEmpty()) {
            return;
        }
        
        // Renderizar en orden: los primeros en la lista se renderizan primero (fondo)
        for (MosaicOverlay overlay : overlays) {
            if (overlay.isVisible() && overlay.intersectsCanvas(canvasSize)) {
                overlay.render(g2d, canvasSize);
            }
        }
        
        // Optimización de memoria después del renderizado si es necesario
        if (autoOptimizeMemory && shouldOptimizeMemory()) {
            optimizeMemoryUsage();
        }
    }
    
    /**
     * Renderiza solo los overlays en el área especificada
     */
    public void renderOverlaysInArea(Graphics2D g2d, Rectangle area) {
        if (overlays.isEmpty()) {
            return;
        }
        
        for (MosaicOverlay overlay : overlays) {
            if (overlay.isVisible()) {
                Rectangle overlayBounds = new Rectangle(overlay.getPosition(), overlay.getSize());
                if (overlayBounds.intersects(area)) {
                    overlay.render(g2d, canvasSize);
                }
            }
        }
    }
    
    // Configuración global
    
    /**
     * Configura el tipo de ladrillos para todos los overlays
     */
    public void setGlobalBrickType(ToBricksType brickType) {
        for (MosaicOverlay overlay : overlays) {
            overlay.setBrickType(brickType);
        }
        System.out.println("Tipo de ladrillo global establecido: " + brickType);
    }
    
    /**
     * Configura la opacidad global (multiplica la opacidad individual de cada overlay)
     */
    public void setGlobalOpacity(float globalOpacity) {
        for (MosaicOverlay overlay : overlays) {
            float currentOpacity = overlay.getOpacity();
            overlay.setOpacity(currentOpacity * globalOpacity);
        }
        System.out.println("Opacidad global aplicada: " + globalOpacity);
    }
    
    /**
     * Configura la configuración de colores para todos los overlays
     */
    public void setGlobalColorConfiguration(LEGOColorGrid colorGrid, String setName) {
        for (MosaicOverlay overlay : overlays) {
            overlay.setColorConfiguration(colorGrid, setName);
        }
        System.out.println("Configuración de colores global establecida: " + setName);
    }
    
    // Gestión de memoria
    
    /**
     * Optimiza el uso de memoria liberando overlays menos usados
     */
    public void optimizeMemoryUsage() {
        updateMemoryStats();
        
        if (totalMemoryUsed > maxMemoryUsage || overlaysProcessed > maxOverlaysInMemory) {
            System.out.println("Optimizando memoria... Uso actual: " + 
                             (totalMemoryUsed / 1024 / 1024) + "MB");
            
            // Crear lista de overlays con sus estadísticas de uso
            List<OverlayMemoryInfo> overlayInfos = new ArrayList<>();
            for (int i = 0; i < overlays.size(); i++) {
                MosaicOverlay overlay = overlays.get(i);
                overlayInfos.add(new OverlayMemoryInfo(i, overlay));
            }
            
            // Ordenar por prioridad (visibles y en canvas primero)
            overlayInfos.sort((a, b) -> {
                // Los invisibles tienen menor prioridad
                if (a.overlay.isVisible() != b.overlay.isVisible()) {
                    return a.overlay.isVisible() ? -1 : 1;
                }
                
                // Los que intersectan el canvas tienen mayor prioridad
                boolean aIntersects = a.overlay.intersectsCanvas(canvasSize);
                boolean bIntersects = b.overlay.intersectsCanvas(canvasSize);
                if (aIntersects != bIntersects) {
                    return aIntersects ? -1 : 1;
                }
                
                // Ordenar por uso de memoria (mayor uso = menor prioridad para liberación)
                return Long.compare(b.memoryUsage, a.memoryUsage);
            });
            
            // Liberar overlays con menor prioridad
            int freed = 0;
            for (int i = overlayInfos.size() - 1; i >= 0 && 
                 (totalMemoryUsed > maxMemoryUsage * 0.8 || freed < overlaysProcessed - maxOverlaysInMemory); i--) {
                OverlayMemoryInfo info = overlayInfos.get(i);
                if (info.overlay.needsReprocessing() == false) { // Solo liberar los que están procesados
                    info.overlay.markForReprocessing(); // Esto liberará la memoria del mosaico procesado
                    freed++;
                }
            }
            
            updateMemoryStats();
            lastOptimizationTime = System.currentTimeMillis();
            
            System.out.println("Optimización completada. Overlays liberados: " + freed + 
                             ", Memoria actual: " + (totalMemoryUsed / 1024 / 1024) + "MB");
        }
    }
    
    /**
     * Verifica si se debe optimizar la memoria
     */
    private boolean shouldOptimizeMemory() {
        long currentTime = System.currentTimeMillis();
        return (totalMemoryUsed > maxMemoryUsage * 0.9) || 
               (overlaysProcessed > maxOverlaysInMemory) ||
               (currentTime - lastOptimizationTime > 30000); // 30 segundos
    }
    
    /**
     * Actualiza las estadísticas de memoria
     */
    private void updateMemoryStats() {
        totalMemoryUsed = 0;
        overlaysProcessed = 0;
        
        for (MosaicOverlay overlay : overlays) {
            totalMemoryUsed += overlay.getMemoryUsage();
            if (!overlay.needsReprocessing()) {
                overlaysProcessed++;
            }
        }
    }
    
    // Configuración
    
    public Dimension getCanvasSize() {
        return new Dimension(canvasSize.width, canvasSize.height);
    }
    
    public void setCanvasSize(Dimension canvasSize) {
        this.canvasSize = new Dimension(canvasSize.width, canvasSize.height);
    }
    
    public boolean isAutoOptimizeMemory() {
        return autoOptimizeMemory;
    }
    
    public void setAutoOptimizeMemory(boolean autoOptimizeMemory) {
        this.autoOptimizeMemory = autoOptimizeMemory;
    }
    
    public long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }
    
    public void setMaxMemoryUsage(long maxMemoryUsage) {
        this.maxMemoryUsage = maxMemoryUsage;
    }
    
    public int getMaxOverlaysInMemory() {
        return maxOverlaysInMemory;
    }
    
    public void setMaxOverlaysInMemory(int maxOverlaysInMemory) {
        this.maxOverlaysInMemory = maxOverlaysInMemory;
    }
    
    // Estadísticas
    
    public long getTotalMemoryUsed() {
        updateMemoryStats();
        return totalMemoryUsed;
    }
    
    public int getOverlaysProcessed() {
        updateMemoryStats();
        return overlaysProcessed;
    }
    
    public String getMemoryStatus() {
        updateMemoryStats();
        return String.format("Memoria: %dMB/%dMB, Overlays procesados: %d/%d, Total overlays: %d",
                totalMemoryUsed / 1024 / 1024,
                maxMemoryUsage / 1024 / 1024,
                overlaysProcessed,
                maxOverlaysInMemory,
                overlays.size());
    }
    
    /**
     * Libera todos los recursos
     */
    public void dispose() {
        clearAllOverlays();
        System.out.println("MosaicOverlayManager disposed");
    }
    
    /**
     * Clase auxiliar para gestión de memoria
     */
    private static class OverlayMemoryInfo {
        int index;
        MosaicOverlay overlay;
        long memoryUsage;
        
        OverlayMemoryInfo(int index, MosaicOverlay overlay) {
            this.index = index;
            this.overlay = overlay;
            this.memoryUsage = overlay.getMemoryUsage();
        }
    }
}