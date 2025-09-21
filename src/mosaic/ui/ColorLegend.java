package mosaic.ui;

import icon.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mosaic.controllers.*;
import mosaic.rendering.Pipeline;
import mosaic.rendering.PipelineMosaicListener;
import colors.LEGOColor;

public class ColorLegend extends JToolBar implements ChangeListener, PipelineMosaicListener {
	private BrickedView brickedView;
	private LEGOColor.CountingLEGOColor[] colors;
	private ColorController cc;
	private UIController uc;
	private JScrollPane scrollPane;
	private JList<LEGOColor.CountingLEGOColor> list;
	
	/**
	 * Determina si un color es oscuro basándose en su luminosidad.
	 * Usa la fórmula de luminosidad perceptual: 0.299*R + 0.587*G + 0.114*B
	 * @param color El color a evaluar
	 * @return true si el color es oscuro (luminosidad < 128)
	 */
	private static boolean isDarkColor(Color color) {
		// Calcular luminosidad perceptual
		double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
		return luminance < 128; // Si la luminosidad es menor a 128, consideramos el color como oscuro
	}
	
	/**
	 * Obtiene el color de texto que mejor contrasta con el color de fondo.
	 * @param backgroundColor El color de fondo
	 * @return Color.WHITE para fondos oscuros, Color.BLACK para fondos claros
	 */
	private static Color getContrastingTextColor(Color backgroundColor) {
		return isDarkColor(backgroundColor) ? Color.WHITE : Color.BLACK;
	}

	public ColorLegend(MainController mc, Pipeline pipeline) {
		super("Legend");
		cc = mc.getColorController();
		uc = mc.getUIController();
		uc.addChangeListener(this);
		pipeline.addMosaicListener(this);
		
		list = new JList<LEGOColor.CountingLEGOColor>();
		list.setAutoscrolls(true);
		list.setCellRenderer(new CellRenderer());
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setBackground(getBackground());
		list.setSelectionModel(new DefaultListSelectionModel() {
			@Override
		    public void setSelectionInterval(int ignore1, int ignore2) {
		        super.setSelectionInterval(-1, -1); // So the user can't click the items.
			}
		});
		mc.getMagnifierController().addChangeListener(this);

		setLayout(new BorderLayout());
		scrollPane = new JScrollPane(list);
		add(scrollPane, BorderLayout.CENTER);
	}

	private class CellRenderer extends JLabel implements ListCellRenderer<LEGOColor.CountingLEGOColor> {
		@Override
		public JComponent getListCellRendererComponent(JList<? extends LEGOColor.CountingLEGOColor> list, 
				final LEGOColor.CountingLEGOColor color, int index, boolean isSelected, boolean cellHasFocus) {
			String identifier = cc.getNormalIdentifier(color.c);
			String text = "";
			if(identifier != null)
				text = identifier + ".";
			if(uc.showTotals() && color.cnt > 0)
				text += " \u2211=" + color.cnt;
			setText(text);
			setIcon(new Icon() {
				@Override
				public int getIconHeight() {
					return Icons.SIZE_LARGE;
				}

				@Override
				public int getIconWidth() {
					return Icons.SIZE_LARGE;
				}

				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
					// Dibujar círculo inscrito en lugar de cuadrado
					g.setColor(color.c.getRGB());
					int diameter = Math.min(getIconWidth(), getIconHeight());
					int circleX = x + (getIconWidth() - diameter) / 2;
					int circleY = y + (getIconHeight() - diameter) / 2;
					g.fillOval(circleX, circleY, diameter, diameter);
					
					// Añadir contorno blanco para todos los círculos
					g.setColor(Color.WHITE);
					Graphics2D g2 = (Graphics2D) g.create();
					Stroke originalStroke = g2.getStroke();
					g2.setStroke(new BasicStroke(1.0f));
					g2.drawOval(circleX, circleY, diameter, diameter);
					g2.setStroke(originalStroke);
					g2.dispose();
				}

			});
			setToolTipText(ColorController.getLongIdentifier(color.c));	
			setFont(list.getFont());
			setOpaque(true);

			if(isSelected) {
				setBackground(list.getSelectionBackground());
			}
			else {
				setBackground(list.getBackground());
			}
			
			return this;
		}
	}
	
	public void setHighlightedColors(LEGOColor.CountingLEGOColor[] m) {
		if(!uc.showLegend() || colors == null || m.length == 0)
			return;
		LEGOColor.CountingLEGOColor[] highlights = new LEGOColor.CountingLEGOColor[LEGOColor.getMaxRebrickableId()+1];
		for(int i = 0; i < m.length; ++i)
			highlights[m[i].c.getIDRebrickable()] = m[i];
		
		// Update data:
		if(uc.showTotals()) {
			for(LEGOColor.CountingLEGOColor color : colors) {
				int id = color.c.getIDRebrickable();
				if(highlights[id] != null)
					color.cnt = highlights[id].cnt;
				else
					color.cnt = 0;
			}
			list.setListData(colors);			
		}

		// Remove already selected indices:
		int[] alreadySelected = list.getSelectedIndices();
		for(int index : alreadySelected) {
			int id = colors[index].c.getIDRebrickable();
			if(highlights[id] != null) {
				highlights[id] = null;
			}
			else {
				list.removeSelectionInterval(index,  index);
			}
		}		
		for(int i = 0; i < colors.length; i++) {
			int id = colors[i].c.getIDRebrickable();
			if(highlights[id] != null) {
				list.addSelectionInterval(i, i);
			}
		}
		//list.repaint();
	}
	
	public void setBrickedView(BrickedView bw) {
		brickedView = bw;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(brickedView == null)
			return;
		if(isVisible() != uc.showLegend())
			setVisible(uc.showLegend());
		mosaicChanged(null);
	}

	@Override
	public void mosaicChanged(Dimension ignore) {
		if(brickedView == null)
			return;
		colors = brickedView.getLegendColors();
		if(!uc.showMagnifier() || !uc.showTotals())
			list.setListData(colors);
	}
}
