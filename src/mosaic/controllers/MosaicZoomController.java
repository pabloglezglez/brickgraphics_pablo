package mosaic.controllers;

import io.Model;
import mosaic.io.BrickGraphicsState;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Controlador específico para zoom del mosaico con funcionalidad de selección y edición precisa.
 * Diferente del magnifier existente, este permite zoom in-place en el canvas principal.
 */
public class MosaicZoomController implements ChangeListener {
    
    // Niveles de zoom predefinidos
    public static final double[] ZOOM_LEVELS = {0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0};
    public static final int DEFAULT_ZOOM_INDEX = 3; // 1.0x (100%)
    
    private Model<BrickGraphicsState> model;
    private List<MosaicZoomListener> listeners;
    
    // Estado del zoom
    private int currentZoomIndex;
    private double currentZoomFactor;
    
    // Estado del viewport (área visible del mosaico)
    private Rectangle viewportBounds;
    private Point zoomCenter; // Centro del zoom para operaciones de zoom in/out
    private Dimension mosaicSize; // Tamaño completo del mosaico
    private Dimension viewportSize; // Tamaño del área de visualización
    
    // Selección de área para herramientas
    private Rectangle selectionArea;
    private boolean isSelecting;
    
    public MosaicZoomController(Model<BrickGraphicsState> model) {
        this.model = model;
        this.listeners = new ArrayList<>();
        this.currentZoomIndex = DEFAULT_ZOOM_INDEX;
        this.currentZoomFactor = ZOOM_LEVELS[currentZoomIndex];
        this.viewportBounds = new Rectangle();
        this.zoomCenter = new Point();
        this.selectionArea = new Rectangle();
        this.isSelecting = false;
    }
    
    // ===== GESTIÓN DE ZOOM =====
    
    public void zoomIn() {
        if (currentZoomIndex < ZOOM_LEVELS.length - 1) {
            setZoomIndex(currentZoomIndex + 1);
        }
    }
    
    public void zoomOut() {
        if (currentZoomIndex > 0) {
            setZoomIndex(currentZoomIndex - 1);
        }
    }
    
    public void zoomToFit() {
        if (mosaicSize == null || viewportSize == null) return;
        
        double scaleX = (double) viewportSize.width / mosaicSize.width;
        double scaleY = (double) viewportSize.height / mosaicSize.height;
        double fitZoom = Math.min(scaleX, scaleY);
        
        // Encontrar el nivel de zoom más cercano
        int bestIndex = 0;
        double bestDiff = Math.abs(ZOOM_LEVELS[0] - fitZoom);
        
        for (int i = 1; i < ZOOM_LEVELS.length; i++) {
            double diff = Math.abs(ZOOM_LEVELS[i] - fitZoom);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        }
        
        setZoomIndex(bestIndex);
        centerViewport();
    }
    
    public void zoomToActualSize() {
        setZoomIndex(DEFAULT_ZOOM_INDEX);
        centerViewport();
    }
    
    public void setZoomIndex(int index) {
        if (index < 0 || index >= ZOOM_LEVELS.length) return;
        
        Point oldCenter = getViewportCenter();
        
        currentZoomIndex = index;
        currentZoomFactor = ZOOM_LEVELS[index];
        updateViewportBounds();
        
        // Mantener el centro del zoom
        if (oldCenter != null) {
            centerViewportOn(oldCenter);
        }
        
        notifyZoomChanged();
    }
    
    // ===== GESTIÓN DEL VIEWPORT =====
    
    public void setViewportSize(Dimension size) {
        this.viewportSize = new Dimension(size);
        updateViewportBounds();
    }
    
    public void setMosaicSize(Dimension size) {
        this.mosaicSize = new Dimension(size);
        updateViewportBounds();
    }
    
    public void centerViewport() {
        if (mosaicSize == null) return;
        centerViewportOn(new Point(mosaicSize.width / 2, mosaicSize.height / 2));
    }
    
    public void centerViewportOn(Point mosaicPoint) {
        if (viewportSize == null) return;
        
        int scaledWidth = (int) (viewportSize.width / currentZoomFactor);
        int scaledHeight = (int) (viewportSize.height / currentZoomFactor);
        
        int x = mosaicPoint.x - scaledWidth / 2;
        int y = mosaicPoint.y - scaledHeight / 2;
        
        // Limitar a los bordes del mosaico
        if (mosaicSize != null) {
            x = Math.max(0, Math.min(x, mosaicSize.width - scaledWidth));
            y = Math.max(0, Math.min(y, mosaicSize.height - scaledHeight));
        }
        
        viewportBounds.setBounds(x, y, scaledWidth, scaledHeight);
        notifyViewportChanged();
    }
    
    private void updateViewportBounds() {
        if (viewportSize == null || mosaicSize == null) return;
        
        int scaledWidth = (int) (viewportSize.width / currentZoomFactor);
        int scaledHeight = (int) (viewportSize.height / currentZoomFactor);
        
        // Mantener la posición actual si es válida
        int x = viewportBounds.x;
        int y = viewportBounds.y;
        
        // Ajustar si se sale de los límites
        x = Math.max(0, Math.min(x, mosaicSize.width - scaledWidth));
        y = Math.max(0, Math.min(y, mosaicSize.height - scaledHeight));
        
        viewportBounds.setBounds(x, y, scaledWidth, scaledHeight);
    }
    
    // ===== CONVERSIÓN DE COORDENADAS =====
    
    /**
     * Convierte coordenadas del viewport (pantalla) a coordenadas del mosaico.
     */
    public Point viewportToMosaic(Point viewportPoint) {
        int mosaicX = viewportBounds.x + (int) (viewportPoint.x / currentZoomFactor);
        int mosaicY = viewportBounds.y + (int) (viewportPoint.y / currentZoomFactor);
        return new Point(mosaicX, mosaicY);
    }
    
    /**
     * Convierte coordenadas del mosaico a coordenadas del viewport (pantalla).
     */
    public Point mosaicToViewport(Point mosaicPoint) {
        int viewportX = (int) ((mosaicPoint.x - viewportBounds.x) * currentZoomFactor);
        int viewportY = (int) ((mosaicPoint.y - viewportBounds.y) * currentZoomFactor);
        return new Point(viewportX, viewportY);
    }
    
    // ===== SELECCIÓN DE ÁREA =====
    
    public void startSelection(Point startPoint) {
        Point mosaicStart = viewportToMosaic(startPoint);
        selectionArea.setBounds(mosaicStart.x, mosaicStart.y, 0, 0);
        isSelecting = true;
        notifySelectionChanged();
    }
    
    public void updateSelection(Point currentPoint) {
        if (!isSelecting) return;
        
        Point mosaicCurrent = viewportToMosaic(currentPoint);
        Point mosaicStart = new Point(selectionArea.x, selectionArea.y);
        
        int x = Math.min(mosaicStart.x, mosaicCurrent.x);
        int y = Math.min(mosaicStart.y, mosaicCurrent.y);
        int width = Math.abs(mosaicCurrent.x - mosaicStart.x);
        int height = Math.abs(mosaicCurrent.y - mosaicStart.y);
        
        selectionArea.setBounds(x, y, width, height);
        notifySelectionChanged();
    }
    
    public void endSelection() {
        isSelecting = false;
        notifySelectionChanged();
    }
    
    public void clearSelection() {
        selectionArea.setBounds(0, 0, 0, 0);
        isSelecting = false;
        notifySelectionChanged();
    }
    
    // ===== GETTERS =====
    
    public double getCurrentZoomFactor() { return currentZoomFactor; }
    public int getCurrentZoomIndex() { return currentZoomIndex; }
    public Rectangle getViewportBounds() { return new Rectangle(viewportBounds); }
    public Rectangle getSelectionArea() { return new Rectangle(selectionArea); }
    public boolean isSelecting() { return isSelecting; }
    public Point getViewportCenter() {
        if (viewportBounds.isEmpty()) return null;
        return new Point(viewportBounds.x + viewportBounds.width/2, 
                        viewportBounds.y + viewportBounds.height/2);
    }
    
    public String getCurrentZoomPercentage() {
        return Math.round(currentZoomFactor * 100) + "%";
    }
    
    public boolean canZoomIn() { return currentZoomIndex < ZOOM_LEVELS.length - 1; }
    public boolean canZoomOut() { return currentZoomIndex > 0; }
    
    // ===== PANEO (PANNING) =====
    
    /**
     * Desplaza el viewport por un delta específico en coordenadas de pantalla.
     * @param deltaX desplazamiento horizontal en pixels de pantalla
     * @param deltaY desplazamiento vertical en pixels de pantalla
     */
    public void panViewport(int deltaX, int deltaY) {
        if (viewportSize == null || mosaicSize == null) return;
        
        // Convertir el desplazamiento de pantalla a coordenadas de mosaico
        int mosaicDeltaX = (int) (deltaX / currentZoomFactor);
        int mosaicDeltaY = (int) (deltaY / currentZoomFactor);
        
        // Aplicar el desplazamiento
        int newX = viewportBounds.x - mosaicDeltaX; // Invertir para paneo natural
        int newY = viewportBounds.y - mosaicDeltaY;
        
        // Limitar a los bordes del mosaico
        int maxX = mosaicSize.width - viewportBounds.width;
        int maxY = mosaicSize.height - viewportBounds.height;
        
        newX = Math.max(0, Math.min(newX, maxX));
        newY = Math.max(0, Math.min(newY, maxY));
        
        // Aplicar solo si hay cambio
        if (newX != viewportBounds.x || newY != viewportBounds.y) {
            viewportBounds.setLocation(newX, newY);
            notifyViewportChanged();
        }
    }
    
    /**
     * Desplaza el viewport a una posición específica en coordenadas de mosaico.
     * @param mosaicX nueva posición X en coordenadas de mosaico
     * @param mosaicY nueva posición Y en coordenadas de mosaico
     */
    public void setViewportPosition(int mosaicX, int mosaicY) {
        if (viewportSize == null || mosaicSize == null) return;
        
        // Limitar a los bordes del mosaico
        int maxX = mosaicSize.width - viewportBounds.width;
        int maxY = mosaicSize.height - viewportBounds.height;
        
        mosaicX = Math.max(0, Math.min(mosaicX, maxX));
        mosaicY = Math.max(0, Math.min(mosaicY, maxY));
        
        viewportBounds.setLocation(mosaicX, mosaicY);
        notifyViewportChanged();
    }
    
    /**
     * Verifica si el paneo está disponible (hay contenido fuera del viewport actual).
     */
    public boolean isPanningAvailable() {
        if (viewportSize == null || mosaicSize == null) return false;
        return currentZoomFactor > 1.0 && 
               (viewportBounds.width < mosaicSize.width || 
                viewportBounds.height < mosaicSize.height);
    }
    
    /**
     * Obtiene los límites de paneo disponibles.
     * @return Rectangle con los límites máximos de paneo en coordenadas de mosaico
     */
    public Rectangle getPanningBounds() {
        if (viewportSize == null || mosaicSize == null) {
            return new Rectangle();
        }
        return new Rectangle(0, 0, 
                           mosaicSize.width - viewportBounds.width, 
                           mosaicSize.height - viewportBounds.height);
    }
    
    // ===== LISTENERS =====
    
    public void addZoomListener(MosaicZoomListener listener) {
        listeners.add(listener);
    }
    
    public void removeZoomListener(MosaicZoomListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyZoomChanged() {
        for (MosaicZoomListener listener : listeners) {
            listener.zoomChanged(currentZoomFactor, currentZoomIndex);
        }
    }
    
    private void notifyViewportChanged() {
        for (MosaicZoomListener listener : listeners) {
            listener.viewportChanged(viewportBounds);
        }
    }
    
    private void notifySelectionChanged() {
        for (MosaicZoomListener listener : listeners) {
            listener.selectionChanged(selectionArea, isSelecting);
        }
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        // Responder a cambios en el modelo si es necesario
        // Por ejemplo, cuando cambie el tamaño del mosaico
    }
    
    // ===== INTERFACE PARA LISTENERS =====
    
    public interface MosaicZoomListener {
        default void zoomChanged(double zoomFactor, int zoomIndex) {}
        default void viewportChanged(Rectangle viewport) {}
        default void selectionChanged(Rectangle selection, boolean isSelecting) {}
    }
}