package mosaic.ui.menu;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ui.*;
import colors.LEGOColor;
import mosaic.controllers.*;
import mosaic.io.MosaicIO;
import mosaic.ui.*;
import mosaic.ui.actions.*;
import mosaic.ui.components.BrushSizeSelector;

public class Ribbon extends JToolBar {
	public Ribbon(MainController mc, MainWindow mw) {
		setFloatable(true);
		setLayout(new WrapLayout(0, 0));

		ImagePreparingView imagePreparingView = mw.getImagePreparingView();

		// Load & Save:
		add(MosaicIO.createOpenAction(mc, mw));
		add(MosaicIO.createSaveAction(mc, mw));
		add(PrintController.createPrintAction(mc.getPrintDialog()));
		addSeparator();
		
		// Toggles:
		final UIController uiController = mc.getUIController();
		add(new ToggleFilters(imagePreparingView));
		add(new ToggleCrop(imagePreparingView));
		add(new ToggleColorChooser(mw.getColorChooser()));
		add(new ToggleMagnifier(uiController));
		add(new ToggleMagnifierLegend(uiController));		
		add(createHidingButton(new ToggleMagnifierTotals(uiController), new IHideButton() {			
			@Override
			public boolean hide() {
				return !uiController.showLegend();
			}
		}, uiController));
		
		// Add ToBricks buttons:
		mc.getToBricksController().addComponents(this, mc);
		
		// Add Edit Tools:
		addSeparator();
		final StudEditController studEditController = mc.getStudEditController();
		add(new JButton(new BrushToolAction(studEditController)));
		
		// Add color preview panel
		add(createColorPreviewPanel(studEditController));
		
		// Add brush size selector
		add(new BrushSizeSelector(studEditController));
		
		add(new JButton(new EyedropperToolAction(studEditController)));
		add(new JButton(new ResetToolAction(studEditController)));
		
		add(new JButton(new GlobalResetAction(studEditController, mw.getBrickedView())));
		
		// Force Paint Mode Toggle
		final JToggleButton forcePaintButton = new JToggleButton("Force Paint");
		forcePaintButton.setToolTipText("Toggle Force Paint mode - paint over same color pixels");
		
		// Configurar colores para estados activo/inactivo
		Color activeColor = new Color(255, 100, 100);     // Rojo claro
		Color inactiveColor = new Color(240, 240, 240);   // Gris claro
		Color activeTextColor = Color.WHITE;
		Color inactiveTextColor = Color.BLACK;
		
		// Estado inicial (inactivo)
		forcePaintButton.setBackground(inactiveColor);
		forcePaintButton.setForeground(inactiveTextColor);
		forcePaintButton.setOpaque(true);
		
		forcePaintButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean enabled = forcePaintButton.isSelected();
				studEditController.setForcePaintMode(enabled);
				
				// Cambiar apariencia visual según el estado
				if (enabled) {
					forcePaintButton.setText("🎯 FORCE ACTIVE");
					forcePaintButton.setBackground(activeColor);
					forcePaintButton.setForeground(activeTextColor);
					forcePaintButton.setToolTipText("Force Paint ACTIVE - Click to disable");
				} else {
					forcePaintButton.setText("Force Paint");
					forcePaintButton.setBackground(inactiveColor);
					forcePaintButton.setForeground(inactiveTextColor);
					forcePaintButton.setToolTipText("Force Paint mode - Click to enable painting over same colors");
				}
				forcePaintButton.repaint();
			}
		});
		add(forcePaintButton);

		// Toggle manual painting visibility
		final JCheckBox showManualPaintToggle = new JCheckBox("Show manual paint");
		showManualPaintToggle.setSelected(studEditController.isManualPaintingVisible());
		showManualPaintToggle.setToolTipText("Show or hide manual painting applied to the mosaic");
		showManualPaintToggle.addActionListener(e -> {
			boolean selected = showManualPaintToggle.isSelected();
			studEditController.setManualPaintingVisible(selected);
		});
		studEditController.addChangeListener(e -> {
			boolean visible = studEditController.isManualPaintingVisible();
			if (showManualPaintToggle.isSelected() != visible) {
				showManualPaintToggle.setSelected(visible);
			}
		});
		add(showManualPaintToggle);
		
		addSeparator();
		
		// Add magnifier buttons:
		final MagnifierController magnifierController = mc.getMagnifierController();
		IHideButton hideWhenMagnifierDisabled = new IHideButton() {			
			@Override
			public boolean hide() {
				return !uiController.showMagnifier();
			}
		};
		add(createHidingButton(new MagnifierTaller(magnifierController), hideWhenMagnifierDisabled, magnifierController));
		add(createHidingButton(new MagnifierShorter(magnifierController), hideWhenMagnifierDisabled, magnifierController));
		add(createHidingButton(new MagnifierWidener(magnifierController), hideWhenMagnifierDisabled, magnifierController));
		add(createHidingButton(new MagnifierSlimmer(magnifierController), hideWhenMagnifierDisabled, magnifierController));
		add(createHidingButton(new ToggleMagnifierColors(uiController), hideWhenMagnifierDisabled, magnifierController));
		add(createHidingButton(new TogglePositionDisplay(mc.getPrintController()), hideWhenMagnifierDisabled, magnifierController));
	}
	
	public static interface IHideButton {
		boolean hide();
	}

	/**
	 * Crea un panel de previsualización de color para el pincel.
	 */
	private JPanel createColorPreviewPanel(StudEditController studEditController) {
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(40, 32));
		panel.setBorder(BorderFactory.createLoweredBevelBorder());
		panel.setToolTipText("Color seleccionado para pincel");
		
		// Agregar listener para cambios de color
		studEditController.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				LEGOColor selectedColor = studEditController.getSelectedColor();
				if (selectedColor != null) {
					// Obtener el color RGB del color LEGO
					Color rgb = selectedColor.getRGB();
					panel.setBackground(rgb);
				} else {
					panel.setBackground(Color.LIGHT_GRAY);
				}
				panel.repaint();
			}
		});
		
		// Configurar color inicial
		LEGOColor initialColor = studEditController.getSelectedColor();
		if (initialColor != null) {
			panel.setBackground(initialColor.getRGB());
		} else {
			panel.setBackground(Color.LIGHT_GRAY);
		}
		
		return panel;
	}

	public static JButton createHidingButton(Action action, final IHideButton hideButton, IChangeMonitor monitor) {
		final JButton ret = new JButton(action);
		if(ret.getIcon() != null)
			ret.setText(null);
		monitor.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				ret.setVisible(!hideButton.hide());
			}
		});
		ret.setVisible(!hideButton.hide());
		return ret;
	}
}
