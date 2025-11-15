# Instrucciones para probar la corrección del sistema de capas

## ¿Qué se ha corregido?

El problema principal era que el sistema de pintura en capas estaba comparando:
- Coordenadas de mosaico (por ejemplo: 1500x2000 píxeles)
- Con dimensiones del grid LEGO (por ejemplo: 150x200 studs)

Esto hacía que las coordenadas estuvieran siempre fuera de rango, por lo que NUNCA se añadían modificaciones a las capas.

## La corrección realizada:

✅ Ahora `applyBrushToLayer` usa `mosaicImageSize` (tamaño real del mosaico en píxeles) para validar coordenadas
✅ Agregados logs detallados para monitorear cada modificación añadida
✅ Sistema de coordenadas alineado perfectamente con el mosaico base

## Para probar:

1. **Selecciona una capa de pintado** (la capa "pintado (Pintado)" ya está seleccionada)
2. **Selecciona la herramienta pincel** (BRUSH)
3. **Elige un color** diferente al fondo
4. **Pinta algunos píxeles** sobre el mosaico
5. **Mueve la capa** para verificar que la pintura persiste

## Logs a monitorear:

```bash
tail -f debug_capas.log | grep -E "(applyBrushToLayer|Modificación añadida|mosaicImageSize|modificaciones en capa)"
```

## Lo que deberíamos ver ahora:

- `DEBUG: applyBrushToLayer - mosaicImageSize: [WIDTH]x[HEIGHT]`
- `DEBUG: applyBrushToLayer - Modificación añadida en (X,Y)`
- `DEBUG: applyBrushToLayer - Añadidas [N] modificaciones`
- `DEBUG: applyBrushToLayer - Total modificaciones en capa: [N]`

¡La pintura ya debería persistir al mover las capas!