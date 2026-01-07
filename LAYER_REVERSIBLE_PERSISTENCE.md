# Reversible Layer Persistence System

## 🔄 **NEW FEATURE: REVERSIBLE SAVES**

### Problem Solved

**BEFORE:** Saving a project with layers burned the layers into the base image. Opening the project and deleting the layers still left their effect baked into the mosaic.

**NOW:** The system stores the original image without layers separately so it can be fully restored when every layer is removed.

---

## ⚙️ **How It Works**

### 1. **Automatic Original Image Save**
```java
// Inside MainController.save()
if (originalImage != null) {
    DataFile originalImageDataFile = new DataFile(originalImage);
    model.set(BrickGraphicsState.OriginalImageFile, originalImageDataFile);
}
```

### 2. **Automatic Layer Removal Detection**
```java
// Inside LayerManager
public boolean removeLayer(Layer layer) {
    if (layers.isEmpty() && onAllLayersRemovedCallback != null) {
        onAllLayersRemovedCallback.run(); // Restore original image
    }
}
```

### 3. **Automatic Restoration**
```java
// Inside MainController
layerManager.setOnAllLayersRemovedCallback(() -> {
    updateImageWithLayers(); // Apply empty layers over the original image
});
```

---

## 🎯 **Persistent KMV States**

### Newly Added Keys:
- `OriginalImageFile`: Base image with no layers
- `LayersEnabled`: Whether the layer system is active
- `LayerData`: Full JSON configuration for every layer

### Data Stored Per Layer:
```json
{
  "name": "Layer name",
  "file": "path/to/file.jpg",
  "visible": true,
  "x": 100,
  "y": 50,
  "opacity": 0.8,
  "brightness": 1.2,
  "contrast": 1.1,
  "saturation": 0.9,
  "gamma": 1.0,
  "sharpness": 1.0,
  "scale": 1.5
}
```

---

## 📋 **User Workflow**

### Sample Scenario:

1. **Load base image** → `image.jpg`
2. **Add layers** → `logo.png`, `texture.jpg`
3. **Adjust layers** → brightness, position, scale
4. **Save project** → `project.kmv`
   - ✅ Original image stored without layers
   - ✅ Layer definitions persisted
   - ✅ Rendered image (with layers) stored

5. **Close and reopen** → `project.kmv`
   - ✅ Layered image reappears
   - ✅ Individual layers remain editable
   - ✅ Original image preserved in the background

6. **Remove every layer**
   - ✅ **AUTOMATICALLY** restores the original image
   - ✅ No trace of deleted layers remains
   - ✅ Mosaic returns exactly to the pristine state

---

## 🔧 **Technical Implementation**

### Modified Files:

#### **BrickGraphicsState.java**
```java
// New state for the original image
OriginalImageFile(new DataFile()),
```

#### **MainController.java**
- `save()`: Persist the original image in the KMV file
- `handleModelChange()`: Reload the original image when present
- Callback wiring for automatic restoration

#### **LayerManager.java**
- `removeLayer()`: Detects when the last layer disappears
- `clearLayers()`: Detects full cleanup
- Callback notifies when removal completes

---

## ✅ **System Benefits**

### For Users:
- **Complete peace of mind**: Experiment without risk
- **Full reversibility**: Removing layers restores the original image
- **Natural workflow**: Everything happens automatically
- **Reliable persistence**: Projects save and load flawlessly

### For Development:
- **Clear separation**: Original image ≠ Layered result
- **Robust foundation**: Error handling and fallbacks
- **Compatibility**: Works with existing KMV files
- **Scalability**: Easy to extend later

---

## 🎉 **System Status**

**✅ FULLY IMPLEMENTED AND OPERATIONAL**

- ✅ Original image persistence in KMV
- ✅ Automatic detection when layers are removed  
- ✅ Automatic restoration of the original image
- ✅ Backwards compatible with existing files
- ✅ Smart reset preserving position and scale
- ✅ Centered scaling without offset

**The layer system is now completely reversible!**